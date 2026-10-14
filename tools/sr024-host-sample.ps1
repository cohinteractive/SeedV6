param(
    [ValidateRange(5, 30)][int]$Seconds = 15,
    [Parameter(Mandatory = $true)][string]$Out
)

# Read-only, low-frequency experiment resource evidence. Never controls a process
# or reads game outcomes. Accessible process CPU deltas are logical-CPU equivalents,
# not physical-core occupancy; one observation cannot reconstruct an entire match.
$ErrorActionPreference = 'Stop'
if (Test-Path -LiteralPath $Out) { throw 'Never overwrite a host observation.' }
$first = @{}
$unreadableBefore = 0
Get-Process | ForEach-Object {
    try {
        $first[$_.Id] = @{ Cpu = [double]$_.CPU; Start = $_.StartTime.ToUniversalTime().Ticks }
    } catch { $unreadableBefore++ }
}
$started = [DateTime]::UtcNow
$timer = [Diagnostics.Stopwatch]::StartNew()
Start-Sleep -Seconds $Seconds
$elapsed = $timer.Elapsed.TotalSeconds
$unreadableAfter = 0
$rows = @(Get-Process | ForEach-Object {
    try {
        if ($first.ContainsKey($_.Id) -and
            $_.StartTime.ToUniversalTime().Ticks -eq $first[$_.Id].Start) {
            $cores = ([double]$_.CPU - $first[$_.Id].Cpu) / $elapsed
            if ($cores -ge .005) {
                [ordered]@{ name = $_.ProcessName; pid = $_.Id;
                    averageLogicalCpus = [math]::Round($cores, 4);
                    workingSetBytes = $_.WorkingSet64 }
            }
        }
    } catch { $unreadableAfter++ }
} | Sort-Object { $_.averageLogicalCpus } -Descending)
$report = [ordered]@{
    startedUtc = $started.ToString('yyyy-MM-ddTHH:mm:ssZ')
    completedUtc = [DateTime]::UtcNow.ToString('yyyy-MM-ddTHH:mm:ssZ')
    elapsedSeconds = $elapsed
    observerSha256 = (Get-FileHash -Algorithm SHA256 -LiteralPath $PSCommandPath).Hash.ToLowerInvariant()
    processes = $rows
    unreadableBefore = $unreadableBefore
    unreadableAfter = $unreadableAfter
    systemMemoryKiB = Get-CimInstance Win32_OperatingSystem |
        Select-Object TotalVisibleMemorySize, FreePhysicalMemory
    systemCpuInstant = Get-CimInstance Win32_PerfFormattedData_PerfOS_Processor -Filter "Name='_Total'" |
        Select-Object Name, PercentProcessorTime
    pagingInstant = Get-CimInstance Win32_PerfFormattedData_PerfOS_Memory |
        Select-Object PagesInputPersec, PagesOutputPersec, PageReadsPersec, PageWritesPersec
    limits = 'Read-only sampled accessible processes alive at both endpoints, with PID creation identity checked. Exited/new/unreadable processes are not reconstructed. CPU deltas are approximate; system CPU, memory and paging are endpoint observations. System CPU includes work whose process details are inaccessible. No outcome inspection or automatic process control.'
}
$json = $report | ConvertTo-Json -Depth 6
[IO.File]::WriteAllText([IO.Path]::GetFullPath($Out), $json + "`n", [Text.UTF8Encoding]::new($false))
$json
