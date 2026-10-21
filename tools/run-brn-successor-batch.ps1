param(
    [Parameter(Mandatory=$true)][string]$Plan,
    [Parameter(Mandatory=$true)][string]$Output,
    [Parameter(Mandatory=$true)][string]$RuntimeManifest,
    [Parameter(Mandatory=$true)][string]$LogPrefix
)
$ErrorActionPreference='Stop'
$launcherSha256=(Get-FileHash -LiteralPath $PSCommandPath -Algorithm SHA256).Hash.ToLowerInvariant()
# One bounded runner invocation. No loop, training, cleanup, or store writes.
# A worktree lease prevents cooperating launches racing. The process check also
# catches a runner left alive after its supervising shell was interrupted.
$leaseDirectory=Join-Path $PWD 'build/research'
[IO.Directory]::CreateDirectory($leaseDirectory) | Out-Null
$lease=[IO.File]::Open((Join-Path $leaseDirectory 'brn-successor-batch.lock'),
    [IO.FileMode]::OpenOrCreate,[IO.FileAccess]::ReadWrite,[IO.FileShare]::None)
try {
$active=@(Get-CimInstance Win32_Process -Filter "Name='java.exe'" | Where-Object {
    $_.CommandLine -and ($_.CommandLine.Contains('com.ohinteractive.seedv6.tools.eval.BrnSuccessorMatch') `
        -or $_.CommandLine.Contains('com.ohinteractive.seedv6.tools.eval.BrnGatedCacheProbe') `
        -or $_.CommandLine.Contains('com.ohinteractive.seedv6.tools.eval.BrnHybridProbe') `
        -or $_.CommandLine.Contains('com.ohinteractive.seedv6.tools.eval.BrnCubicLayoutProbe'))
})
if ($active.Count -ne 0) {
    throw 'An existing BRN successor match or diagnostic is active; preserve it and wait before another launch.'
}
if (Test-Path -LiteralPath 'build/research/brn-successor-batches.pause') {
    throw 'Research batches intentionally paused before launch; preserve receipts and resume with a new log prefix.'
}
$binding=Get-Content -LiteralPath $RuntimeManifest -Raw -Encoding UTF8 | ConvertFrom-Json
$configuration=Get-Content -LiteralPath $Plan -Raw -Encoding UTF8 | ConvertFrom-Json
if (($configuration.nodes -gt 0) -eq ($configuration.moveMillis -gt 0)) {
    throw 'Choose exactly one positive node or clock budget; node safety is internal to the runner.'
}
if (@($configuration.models | Where-Object { $null -ne $_.hybridBase }).Count -ne 0 `
        -and @($binding.modelFeatures) -notcontains 'brn-hybrid-raw-v1') {
    throw 'This plan requires explicit hybrid component binding; the frozen runtime does not declare that capability.'
}
if (@($configuration.models | Where-Object { $_.packedCubicCache -eq $true }).Count -ne 0 `
        -and @($binding.modelFeatures) -notcontains 'brn-cubic-packed-v1') {
    throw 'This plan requires the packed cubic cache; the frozen runtime does not declare that capability.'
}
if (@($configuration.models | Where-Object { $_.compiledUnary -eq $true }).Count -ne 0 `
        -and @($binding.modelFeatures) -notcontains 'brn-unary-compiled-v1') {
    throw 'This plan requires compiled unary binding; the frozen runtime does not declare that capability.'
}
if (@($configuration.models | Where-Object { $_.compiledConstantGates -eq $true }).Count -ne 0 `
        -and @($binding.modelFeatures) -notcontains 'brn-constant-gates-compiled-v1') {
    throw 'This plan requires compiled constant-gate binding; the frozen runtime does not declare that capability.'
}
if ($configuration.batchGames -gt 12 -or $configuration.batchSeconds -gt 240) {
    throw 'Batch exceeds the admitted resource envelope.'
}
foreach ($jar in $binding.jars.PSObject.Properties) {
    $file=Join-Path $binding.runtime $jar.Name
    if ((Get-FileHash -LiteralPath $file -Algorithm SHA256).Hash.ToLowerInvariant() -ne $jar.Value) {
        throw "Frozen runtime changed: $file"
    }
}
if (Test-Path -LiteralPath $Output) {
    if (Get-ChildItem -LiteralPath $Output -Filter '*.failed') {
        throw 'Prior failed game requires investigation; preserve evidence.'
    }
}
foreach ($suffix in @('.stdout','.stderr','.process.json')) {
    if (Test-Path -LiteralPath ($LogPrefix+$suffix)) { throw 'Preserve prior batch logs.' }
}
$started=[DateTime]::UtcNow
$process=Start-Process -FilePath 'C:/java/jdk-21/bin/java.exe' -ArgumentList @(
    '-Xmx768m','-XX:ActiveProcessorCount=1','--add-modules=jdk.incubator.vector',
    '-cp',($binding.runtime+'/*'),'com.ohinteractive.seedv6.tools.eval.BrnSuccessorMatch',
    $Plan,$Output
) -WorkingDirectory $PWD -WindowStyle Hidden -PassThru `
    -RedirectStandardOutput ($LogPrefix+'.stdout') -RedirectStandardError ($LogPrefix+'.stderr')
$handle=$process.Handle
$process.PriorityClass='BelowNormal'
$peakWorkingSet=0L
while (-not $process.WaitForExit(1000)) {
    $process.Refresh()
    if (-not $process.HasExited) { $peakWorkingSet=[Math]::Max($peakWorkingSet,$process.WorkingSet64) }
}
$process.Refresh()
$record=@{
    startedUtc=$started.ToString('o'); completedUtc=[DateTime]::UtcNow.ToString('o');
    processId=$process.Id; exitCode=$process.ExitCode; cpuSeconds=$process.TotalProcessorTime.TotalSeconds;
    sampledPeakWorkingSetBytes=$peakWorkingSet; plan=$Plan; runtimeManifest=$RuntimeManifest;
    heapMiB=768; activeProcessors=1; priority='BelowNormal'; concurrentGames=1; workersPerEngine=1; pondering=$false
    launcherSha256=$launcherSha256
}
$record | ConvertTo-Json | Set-Content -LiteralPath ($LogPrefix+'.process.json') -Encoding UTF8
if ($process.ExitCode -ne 0) { throw "Research batch failed: $($process.ExitCode). See $LogPrefix.stderr" }
Get-Content -LiteralPath ($LogPrefix+'.stdout') -Tail 2
} finally {
    $lease.Dispose()
}
