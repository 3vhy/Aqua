# Aqua Client: reads what Windows' media controls show (the "now playing" box next to the volume
# flyout) and prints one JSON line per update. Commands (toggle, next, previous, seek <ms>) arrive
# one per line on standard input. Nothing is sent anywhere else.
$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'
[Console]::OutputEncoding = New-Object System.Text.UTF8Encoding $false
Add-Type -AssemblyName System.Runtime.WindowsRuntime

$asTaskOperation = [System.WindowsRuntimeSystemExtensions].GetMethods() | Where-Object {
    $_.Name -eq 'AsTask' -and $_.GetParameters().Count -eq 1 -and $_.GetParameters()[0].ParameterType.Name -eq 'IAsyncOperation`1'
} | Select-Object -First 1

function Await($operation, [Type]$resultType) {
    $task = $asTaskOperation.MakeGenericMethod($resultType).Invoke($null, @($operation))
    if (-not $task.Wait(4000)) { throw 'Windows media did not answer in time' }
    return $task.Result
}

function Emit($object) {
    [Console]::Out.WriteLine(($object | ConvertTo-Json -Compress))
    [Console]::Out.Flush()
}

$null = [Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager, Windows.Media.Control, ContentType = WindowsRuntime]
$null = [Windows.Storage.Streams.IRandomAccessStreamWithContentType, Windows.Storage.Streams, ContentType = WindowsRuntime]
$null = [Windows.Storage.Streams.DataReader, Windows.Storage.Streams, ContentType = WindowsRuntime]
$managerType = [Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager]
$propertiesType = [Windows.Media.Control.GlobalSystemMediaTransportControlsSessionMediaProperties]
$streamType = [Windows.Storage.Streams.IRandomAccessStreamWithContentType]
$manager = Await ($managerType::RequestAsync()) $managerType

$anyApp = $env:AQUA_MEDIA_FILTER -eq 'any'
$coverDir = $env:AQUA_COVER_DIR
$coverSlot = 0
$lastKey = $null
$coverPath = ''
# [Console]::In reads synchronously even through ReadLineAsync, so read the raw input stream instead:
# its ReadLineAsync really runs in the background and the loop below never waits for a command.
$stdin = New-Object System.IO.StreamReader([Console]::OpenStandardInput(), (New-Object System.Text.UTF8Encoding $false))
$pendingLine = $stdin.ReadLineAsync()

function Find-Session {
    if ($anyApp) {
        $current = $manager.GetCurrentSession()
        if ($null -ne $current) { return $current }
    }
    foreach ($session in @($manager.GetSessions())) {
        if ([string]$session.SourceAppUserModelId -match 'spotify') { return $session }
    }
    if ($anyApp) {
        foreach ($session in @($manager.GetSessions())) { return $session }
    }
    return $null
}

function Invoke-AquaCommand([string]$line) {
    $session = Find-Session
    if ($null -eq $session) { return }
    $parts = $line.Trim().Split(' ')
    switch ($parts[0]) {
        'toggle' { $null = Await ($session.TryTogglePlayPauseAsync()) ([bool]) }
        'next' { $null = Await ($session.TrySkipNextAsync()) ([bool]) }
        'previous' { $null = Await ($session.TrySkipPreviousAsync()) ([bool]) }
        'seek' { $null = Await ($session.TryChangePlaybackPositionAsync([long]$parts[1] * 10000)) ([bool]) }
    }
}

while ($true) {
    while ($pendingLine.IsCompleted) {
        $line = $pendingLine.Result
        if ($null -eq $line) { exit 0 }
        try { Invoke-AquaCommand $line } catch { }
        $pendingLine = $stdin.ReadLineAsync()
    }
    try {
        $session = Find-Session
        if ($null -eq $session) {
            Emit @{ none = $true }
        } else {
            $props = Await ($session.TryGetMediaPropertiesAsync()) $propertiesType
            $timeline = $session.GetTimelineProperties()
            $playback = $session.GetPlaybackInfo()
            $key = [string]$session.SourceAppUserModelId + "`n" + $props.Artist + "`n" + $props.Title
            if ($key -ne $lastKey) {
                $lastKey = $key
                $coverPath = ''
                if ($null -ne $props.Thumbnail -and $coverDir) {
                    $coverSlot = 1 - $coverSlot
                    $file = Join-Path $coverDir ('windows_cover_' + $coverSlot + '.img')
                    try {
                        # Read the thumbnail with a WinRT DataReader (works from Windows PowerShell 5.1).
                        $stream = Await ($props.Thumbnail.OpenReadAsync()) $streamType
                        $size = [uint32]$stream.Size
                        if ($size -gt 0 -and $size -lt 8MB) {
                            $reader = [Windows.Storage.Streams.DataReader]::new($stream.GetInputStreamAt(0))
                            $null = Await ($reader.LoadAsync($size)) ([uint32])
                            $bytes = New-Object byte[] $size
                            $reader.ReadBytes($bytes)
                            $reader.Dispose()
                            [System.IO.File]::WriteAllBytes($file, $bytes)
                            $coverPath = $file
                        }
                        $stream.Dispose()
                    } catch {
                        try {
                            $stream = Await ($props.Thumbnail.OpenReadAsync()) $streamType
                            $source = [System.IO.WindowsRuntimeStreamExtensions]::AsStreamForRead($stream)
                            $target = [System.IO.File]::Create($file)
                            try { $source.CopyTo($target) } finally { $target.Dispose(); $source.Dispose() }
                            $coverPath = $file
                        } catch { $coverPath = '' }
                    }
                }
            }
            $playing = $playback.PlaybackStatus.ToString() -eq 'Playing'
            $position = $timeline.Position.TotalMilliseconds
            if ($playing -and $timeline.LastUpdatedTime.Year -gt 2000) {
                $position += ([DateTimeOffset]::UtcNow - $timeline.LastUpdatedTime).TotalMilliseconds
            }
            Emit ([ordered]@{
                app = [string]$session.SourceAppUserModelId
                title = [string]$props.Title
                artist = [string]$props.Artist
                album = [string]$props.AlbumTitle
                durationMs = [long]$timeline.EndTime.TotalMilliseconds
                positionMs = [long]$position
                playing = [bool]$playing
                cover = [string]$coverPath
            })
        }
    } catch {
        Emit @{ error = [string]$_.Exception.Message }
    }
    Start-Sleep -Milliseconds 750
}
