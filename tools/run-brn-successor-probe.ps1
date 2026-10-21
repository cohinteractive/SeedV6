param(
    [Parameter(Mandatory=$true)][string]$RuntimeManifest,
    [Parameter(Mandatory=$true)][string]$EntryPoint,
    [Parameter(Mandatory=$true)][string[]]$ProbeArguments,
    [Parameter(Mandatory=$true)][string]$LogPrefix,
    [string[]]$RequiredFeatures=@()
)
$ErrorActionPreference='Stop'
# Read-only diagnostics only. No training, model preparation, or automatic fallback.
$allowed=@('BrnHybridProbe','BrnGatedCacheProbe','BrnCubicLayoutProbe')
if($allowed -notcontains $EntryPoint){throw 'Unapproved diagnostic entry point'}
$launcherHash=(Get-FileHash -LiteralPath $PSCommandPath).Hash.ToLowerInvariant()
$directory=Join-Path $PWD 'build/research'
[IO.Directory]::CreateDirectory($directory) | Out-Null
$lease=[IO.File]::Open((Join-Path $directory 'brn-successor-batch.lock'),
    [IO.FileMode]::OpenOrCreate,[IO.FileAccess]::ReadWrite,[IO.FileShare]::None)
try {
    $active=@(Get-CimInstance Win32_Process -Filter "Name='java.exe'" | Where-Object {
        $_.CommandLine -and ($_.CommandLine.Contains('com.ohinteractive.seedv6.tools.eval.BrnSuccessorMatch') `
            -or $_.CommandLine.Contains('com.ohinteractive.seedv6.tools.eval.BrnGatedCacheProbe') `
            -or $_.CommandLine.Contains('com.ohinteractive.seedv6.tools.eval.BrnHybridProbe') `
            -or $_.CommandLine.Contains('com.ohinteractive.seedv6.tools.eval.BrnCubicLayoutProbe'))
    })
    if($active.Count -ne 0){throw 'A BRN diagnostic or match is already active; preserve it and wait'}
    if(Test-Path -LiteralPath 'build/research/brn-successor-batches.pause'){throw 'Research is intentionally paused'}
    $binding=Get-Content -LiteralPath $RuntimeManifest -Raw -Encoding UTF8 | ConvertFrom-Json
    foreach($feature in $RequiredFeatures){
        if(@($binding.modelFeatures) -notcontains $feature){throw "Frozen runtime lacks required capability: $feature"}
    }
    if($EntryPoint -in @('BrnGatedCacheProbe','BrnHybridProbe')) {
        $plan=Get-Content -LiteralPath $ProbeArguments[0] -Raw -Encoding UTF8 | ConvertFrom-Json
        if(@($plan.models | Where-Object {$_.correctionAblation -eq 'cubic-only'}).Count -ne 0 `
                -and @($binding.modelFeatures) -notcontains 'brn-cubic-only-ablation-v1'){throw 'Plan requires cubic-only capability'}
        if(@($plan.models | Where-Object {$null -ne $_.hybridBase}).Count -ne 0 `
                -and @($binding.modelFeatures) -notcontains 'brn-hybrid-raw-v1'){throw 'Plan requires hybrid capability'}
        if(@($plan.models | Where-Object {$_.packedCubicCache -eq $true}).Count -ne 0 `
                -and @($binding.modelFeatures) -notcontains 'brn-cubic-packed-v1'){throw 'Plan requires packed capability'}
        if(@($plan.models | Where-Object {$_.compiledUnary -eq $true}).Count -ne 0 `
                -and @($binding.modelFeatures) -notcontains 'brn-unary-compiled-v1'){throw 'Plan requires compiled unary capability'}
        if(@($plan.models | Where-Object {$_.compiledConstantGates -eq $true}).Count -ne 0 `
                -and @($binding.modelFeatures) -notcontains 'brn-constant-gates-compiled-v1'){throw 'Plan requires compiled constant-gate capability'}
    }
    foreach($jar in $binding.jars.PSObject.Properties){
        if((Get-FileHash -LiteralPath (Join-Path $binding.runtime $jar.Name)).Hash.ToLowerInvariant() -ne $jar.Value){throw 'Frozen runtime hash changed'}
    }
    foreach($suffix in @('.stdout','.stderr','.process.json')){
        if(Test-Path -LiteralPath ($LogPrefix+$suffix)){throw 'Preserve prior diagnostic logs'}
    }
    $arguments=@('-Xmx768m','-XX:ActiveProcessorCount=1','--add-modules=jdk.incubator.vector',
        '-cp',($binding.runtime+'/*'),('com.ohinteractive.seedv6.tools.eval.'+$EntryPoint))+$ProbeArguments
    # Start-Process joins its array; quote every token and refuse embedded quotes.
    if(@($arguments | Where-Object {$_ -match '["\r\n]'}).Count -ne 0){throw 'Unsupported argument quoting'}
    $quoted=$arguments | ForEach-Object {'"'+$_+'"'}
    $started=[DateTime]::UtcNow
    $process=Start-Process -FilePath $binding.java -ArgumentList $quoted -WorkingDirectory $PWD `
        -WindowStyle Hidden -PassThru -RedirectStandardOutput ($LogPrefix+'.stdout') -RedirectStandardError ($LogPrefix+'.stderr')
    $handle=$process.Handle
    $process.PriorityClass='BelowNormal'
    $timedOut=$false
    $peakWorkingSet=0L
    while(-not $process.WaitForExit(1000)) {
        $process.Refresh()
        if(-not $process.HasExited){$peakWorkingSet=[Math]::Max($peakWorkingSet,$process.WorkingSet64)}
        if(([DateTime]::UtcNow-$started).TotalSeconds -gt 300) {
            $timedOut=$true
            if(-not $process.HasExited){Stop-Process -Id $process.Id}
            $process.WaitForExit()
            break
        }
    }
    @{exitCode=$process.ExitCode;startedUtc=$started.ToString('o');completedUtc=[DateTime]::UtcNow.ToString('o');
        arguments=$arguments;priority='BelowNormal';cpuSeconds=$process.TotalProcessorTime.TotalSeconds;
        runtime=$RuntimeManifest;runtimeSha256=(Get-FileHash -LiteralPath $RuntimeManifest).Hash.ToLowerInvariant();
        launcherSha256=$launcherHash;requiredFeatures=$RequiredFeatures;timedOut=$timedOut;
        sampledPeakWorkingSetBytes=$peakWorkingSet;activeProcessors=1;heapMiB=768} | ConvertTo-Json -Depth 8 |
        Set-Content -LiteralPath ($LogPrefix+'.process.json') -Encoding UTF8
    if($timedOut -or $process.ExitCode -ne 0){throw 'Diagnostic failed; preserve logs and investigate'}
    Get-Content -LiteralPath ($LogPrefix+'.stdout') -Tail 4
} finally {$lease.Dispose()}
