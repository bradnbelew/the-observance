param(
  [string]$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
)

$ErrorActionPreference = "Stop"

$failures = New-Object System.Collections.Generic.List[string]

function Add-Failure([string]$Message) {
  $script:failures.Add($Message) | Out-Null
}

function Read-Json([string]$Path) {
  try {
    return Get-Content -LiteralPath $Path -Raw | ConvertFrom-Json
  } catch {
    Add-Failure "Invalid JSON: $Path ($($_.Exception.Message))"
    return $null
  }
}

function Find-BytePattern([byte[]]$Bytes, [byte[]]$Pattern) {
  if ($Bytes.Length -lt $Pattern.Length) { return -1 }
  for ($i = 0; $i -le $Bytes.Length - $Pattern.Length; $i++) {
    $ok = $true
    for ($j = 0; $j -lt $Pattern.Length; $j++) {
      if ($Bytes[$i + $j] -ne $Pattern[$j]) {
        $ok = $false
        break
      }
    }
    if ($ok) { return $i }
  }
  return -1
}

function Get-LastOggGranule([byte[]]$Bytes) {
  for ($i = $Bytes.Length - 27; $i -ge 0; $i--) {
    if (
      $Bytes[$i] -eq 0x4f -and
      $Bytes[$i + 1] -eq 0x67 -and
      $Bytes[$i + 2] -eq 0x67 -and
      $Bytes[$i + 3] -eq 0x53
    ) {
      $granule = [BitConverter]::ToInt64($Bytes, $i + 6)
      if ($granule -gt 0) { return $granule }
    }
  }
  return 0
}

function Get-VorbisInfo([string]$Path) {
  $bytes = [IO.File]::ReadAllBytes($Path)
  if ($bytes.Length -lt 64) {
    Add-Failure "OGG is implausibly small: $Path"
    return $null
  }
  if ($bytes[0] -ne 0x4f -or $bytes[1] -ne 0x67 -or $bytes[2] -ne 0x67 -or $bytes[3] -ne 0x53) {
    Add-Failure "OGG does not start with OggS capture pattern: $Path"
    return $null
  }

  $vorbis = [Text.Encoding]::ASCII.GetBytes("vorbis")
  $vorbisIndex = Find-BytePattern $bytes $vorbis
  if ($vorbisIndex -lt 1 -or $bytes[$vorbisIndex - 1] -ne 0x01) {
    Add-Failure "OGG is not a Vorbis identification packet: $Path"
    return $null
  }

  $channels = [int]$bytes[$vorbisIndex + 10]
  $sampleRate = [BitConverter]::ToInt32($bytes, $vorbisIndex + 11)
  $granule = Get-LastOggGranule $bytes
  $duration = if ($sampleRate -gt 0) { $granule / [double]$sampleRate } else { 0 }

  return [pscustomobject]@{
    Channels = $channels
    SampleRate = $sampleRate
    DurationSeconds = $duration
    Bytes = $bytes.Length
  }
}

$resourceNamespace = Join-Path $RepoRoot "resourcepack\assets\observance"
$soundsJson = Join-Path $resourceNamespace "sounds.json"
$sounds = Read-Json $soundsJson

$requiredKeys = @(
  "whisper",
  "drone_low",
  "stone_breath",
  "cold_toll",
  "keeper_voice",
  "keeper_voice.vaun",
  "keeper_voice.mara",
  "keeper_voice.sella",
  "keeper_voice.orin",
  "keeper_voice.brann",
  "keeper_voice.iss"
)

if ($null -ne $sounds) {
  foreach ($key in $requiredKeys) {
    if ($null -eq $sounds.PSObject.Properties[$key]) {
      Add-Failure "sounds.json missing required key: $key"
    }
  }

  $checked = New-Object System.Collections.Generic.List[string]
  foreach ($soundKey in $sounds.PSObject.Properties.Name) {
    $entry = $sounds.$soundKey
    foreach ($sound in @($entry.sounds)) {
      $name = if ($sound -is [string]) { $sound } else { $sound.name }
      if ([string]::IsNullOrWhiteSpace($name)) {
        Add-Failure "Sound entry '$soundKey' has no name"
        continue
      }

      $pathPart = $name
      if ($pathPart.StartsWith("observance:")) {
        $pathPart = $pathPart.Substring("observance:".Length)
      }
      $ogg = Join-Path (Join-Path $resourceNamespace "sounds") ($pathPart.Replace("/", "\") + ".ogg")
      if (!(Test-Path $ogg)) {
        Add-Failure "Sound entry '$soundKey' references missing OGG: $name -> $ogg"
        continue
      }

      $info = Get-VorbisInfo $ogg
      if ($null -eq $info) { continue }
      if ($info.Channels -ne 1) {
        Add-Failure "$soundKey must be mono for Minecraft positional audio, found $($info.Channels) channel(s): $ogg"
      }
      if ($info.SampleRate -lt 22050 -or $info.SampleRate -gt 48000) {
        Add-Failure "$soundKey has unexpected sample rate $($info.SampleRate) Hz: $ogg"
      }
      if ($info.DurationSeconds -lt 0.15 -or $info.DurationSeconds -gt 30) {
        Add-Failure "$soundKey duration looks wrong ($([math]::Round($info.DurationSeconds, 2))s): $ogg"
      }
      if ($info.Bytes -lt 8000) {
        Add-Failure "$soundKey is suspiciously tiny ($($info.Bytes) bytes): $ogg"
      }

      $checked.Add("$soundKey=$([math]::Round($info.DurationSeconds, 2))s") | Out-Null
    }
  }
}

if ($failures.Count -gt 0) {
  Write-Host "media readiness check: FAILED"
  foreach ($failure in $failures) {
    Write-Host "  - $failure"
  }
  exit 1
}

Write-Host "media readiness check: OK - $($checked.Count) OGG sounds are present, Vorbis, mono, and duration-checked"
Write-Host "media durations: $($checked -join ', ')"
