$ErrorActionPreference = 'Stop'
$taskRoot = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
$evidenceRoot = Join-Path $taskRoot 'docs/spikes/road-ahead'
$adbPath = (Get-Command adb -ErrorAction Stop).Source
$utf8 = New-Object System.Text.UTF8Encoding($false)
$report = New-Object System.Collections.Generic.List[string]

function Set-TestState([string]$gear, [int]$speed) {
    Invoke-RestMethod -Method Put -Uri 'http://127.0.0.1:8088/vehicle/controls?speed=0&accelerator=0&brake=0' | Out-Null
    Invoke-RestMethod -Method Put -Uri "http://127.0.0.1:8088/vehicle/gear?gear=$gear" | Out-Null
    Invoke-RestMethod -Method Put -Uri "http://127.0.0.1:8088/vehicle/controls?speed=$speed&accelerator=0&brake=0" | Out-Null
}

function Save-Screen([string]$path) {
    $info = New-Object System.Diagnostics.ProcessStartInfo
    $info.FileName = $adbPath
    $info.Arguments = '-s emulator-5556 exec-out screencap -p'
    $info.UseShellExecute = $false
    $info.CreateNoWindow = $true
    $info.RedirectStandardOutput = $true
    $info.RedirectStandardError = $true
    $process = [System.Diagnostics.Process]::Start($info)
    $stream = [System.IO.File]::Create($path)
    try { $process.StandardOutput.BaseStream.CopyTo($stream) } finally { $stream.Dispose() }
    $errorText = $process.StandardError.ReadToEnd()
    $process.WaitForExit()
    if ($process.ExitCode -ne 0) { throw "screencap failed: $errorText" }
    $bytes = [System.IO.File]::ReadAllBytes($path)
    if ($bytes.Length -lt 1000 -or [BitConverter]::ToString($bytes[0..7]) -ne '89-50-4E-47-0D-0A-1A-0A') {
        throw "Invalid PNG: $path"
    }
}

try {
    foreach ($test in @(@('P0', 'Park', 0), @('D0', 'Drive', 0), @('D30', 'Drive', 30))) {
        $label, $gear, $speed = $test
        Set-TestState $gear $speed
        & $adbPath -s emulator-5556 shell am start -n com.roadahead.spike/.MainActivity --es op state
        if ($LASTEXITCODE -ne 0) { throw 'Activity launch failed' }
        Start-Sleep -Seconds 3
        $state = Invoke-RestMethod -Uri 'http://127.0.0.1:8088/vehicle/state'
        $json = $state | ConvertTo-Json -Depth 30
        [System.IO.File]::WriteAllText((Join-Path $evidenceRoot "ux-$label-state.json"), $json, $utf8)
        if ($state.gear.actual.value -ne $gear -or [Math]::Abs($state.controls.speed_kph - $speed) -gt 0.1) {
            throw "State mismatch: $label actual=$($state.gear.actual.value)/$($state.controls.speed_kph)"
        }
        Save-Screen (Join-Path $evidenceRoot "ux-$label.png")
        $ux = (& $adbPath -s emulator-5556 logcat -d -v threadtime -s T14:I) | Select-String '07 UX' | Select-Object -Last 3
        $report.Add("$(Get-Date -Format o) $label gear=$gear speed=$speed PNG/state assertions passed")
        $report.Add(($ux -join "`n"))
        Write-Output $report[$report.Count - 2]
        Write-Output $ux
    }
} finally {
    Set-TestState 'Park' 0
    [System.IO.File]::WriteAllText((Join-Path $evidenceRoot 'ux-capture.txt'), ($report -join "`n"), $utf8)
}
