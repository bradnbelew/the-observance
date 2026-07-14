param(
  [string]$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path,
  [switch]$Live
)

$ErrorActionPreference = "Stop"
$failures = [System.Collections.Generic.List[string]]::new()
function Fail([string]$Message) { $script:failures.Add($Message) | Out-Null }

$root = [System.IO.Path]::GetFullPath($RepoRoot)
$manifestPath = Join-Path $root "arc\v5\media-manifest.json"
$receiptPath = Join-Path $root "design\V5-EXTERNAL-MEDIA-RECEIPT.md"
if (-not (Test-Path -LiteralPath $manifestPath)) {
  Fail "missing V5 media manifest: $manifestPath"
  $manifest = $null
} else {
  try { $manifest = Get-Content -LiteralPath $manifestPath -Raw | ConvertFrom-Json }
  catch { Fail "invalid V5 media manifest: $($_.Exception.Message)"; $manifest = $null }
}

$expectedPayloads = @(
  "ASH-13",
  "WHERE THE REEDS FOLD BACK",
  "STAY AWAKE",
  "SIX RETURN, ONE IS NOT KEPT",
  "I WAS NOT KEPT"
)

if ($null -ne $manifest) {
  if ($manifest.policy.allRequired -ne $true -or $manifest.policy.automaticReveal -ne $true -or
      $manifest.policy.operatorStorySwitchesForbidden -ne $true) {
    Fail "V5 media policy must require every asset, reveal automatically, and forbid operator story switches"
  }
  $assets = @($manifest.assets)
  if ($assets.Count -ne 5) { Fail "expected exactly five required media assets; found $($assets.Count)" }
  $ids = @($assets | ForEach-Object { [string]$_.id })
  if (($ids | Sort-Object -Unique).Count -ne $ids.Count) { Fail "media ids are not unique" }
  $payloads = @($assets | ForEach-Object { ([string]$_.expectedPayload).ToUpperInvariant() } | Sort-Object)
  if (($payloads -join "|") -ne (($expectedPayloads | Sort-Object) -join "|")) {
    Fail "media payload set does not match the five canonical V5 payloads"
  }
  foreach ($asset in $assets) {
    $url = if (-not [string]::IsNullOrWhiteSpace([string]$asset.url)) { [string]$asset.url } else { [string]$asset.archiveUrl }
    if (-not $url.StartsWith("https://")) { Fail "$($asset.id) has no HTTPS source" }
    if ([string]$asset.sourceSha1 -notmatch '^[0-9a-f]{40}$') { Fail "$($asset.id) has invalid source SHA-1" }
    if ([long]$asset.sourceBytes -le 0) { Fail "$($asset.id) has invalid source byte count" }
    if ([string]::IsNullOrWhiteSpace([string]$asset.revealPrerequisite) -or
        [string]::IsNullOrWhiteSpace([string]$asset.completionFlag)) {
      Fail "$($asset.id) has no prerequisite/completion contract"
    }
  }
}

if (-not (Test-Path -LiteralPath $receiptPath)) {
  Fail "missing dated V5 external media receipt: $receiptPath"
} elseif ($null -ne $manifest) {
  $receipt = Get-Content -LiteralPath $receiptPath -Raw
  foreach ($asset in @($manifest.assets)) {
    if (-not $receipt.Contains([string]$asset.id)) { Fail "media receipt omits $($asset.id)" }
  }
}

if ($Live -and $null -ne $manifest) {
  foreach ($asset in @($manifest.assets | Where-Object { -not [string]::IsNullOrWhiteSpace([string]$_.url) })) {
    try {
      $response = Invoke-WebRequest -Uri ([string]$asset.url) -TimeoutSec 45 -UseBasicParsing
      if ($response.StatusCode -ne 200) { Fail "$($asset.id) returned HTTP $($response.StatusCode)"; continue }
      if ($response.Content -notmatch '"playabilityStatus":\{"status":"OK"') {
        Fail "$($asset.id) does not report YouTube playabilityStatus OK"
      }
      $expectedId = ([uri]([string]$asset.url)).AbsolutePath.Trim('/')
      if (-not $response.Content.Contains('"videoId":"' + $expectedId + '"')) {
        Fail "$($asset.id) landing page does not identify expected video $expectedId"
      }
    } catch {
      Fail "$($asset.id) live request failed: $($_.Exception.Message)"
    }
  }

  $archive = @($manifest.assets | Where-Object { $_.id -eq "spectrogram_averyn_voice" }) | Select-Object -First 1
  if ($null -eq $archive) {
    Fail "missing spectrogram_averyn_voice asset"
  } else {
    try {
      Add-Type -AssemblyName System.IO.Compression.FileSystem
      $zipPath = Join-Path ([System.IO.Path]::GetTempPath()) "observance-v5-media-live-check.zip"
      $downloadUrl = ([string]$archive.archiveUrl) -replace 'dl=0', 'dl=1'
      Invoke-WebRequest -Uri $downloadUrl -OutFile $zipPath -TimeoutSec 60 -UseBasicParsing
      $zip = [System.IO.Compression.ZipFile]::OpenRead($zipPath)
      try {
        $entry = $zip.Entries | Where-Object { $_.FullName -eq "field_audio_03.wav" } | Select-Object -First 1
        if ($null -eq $entry) {
          Fail "Dropbox archive does not contain field_audio_03.wav"
        } else {
          $wavPath = Join-Path ([System.IO.Path]::GetTempPath()) "observance-v5-field_audio_03.wav"
          [System.IO.Compression.ZipFileExtensions]::ExtractToFile($entry, $wavPath, $true)
          $bytes = (Get-Item -LiteralPath $wavPath).Length
          $sha1 = (Get-FileHash -LiteralPath $wavPath -Algorithm SHA1).Hash.ToLowerInvariant()
          if ($bytes -ne [long]$archive.sourceBytes) { Fail "field_audio_03.wav byte count drifted: $bytes" }
          if ($sha1 -ne [string]$archive.sourceSha1) { Fail "field_audio_03.wav SHA-1 drifted: $sha1" }
        }
      } finally { $zip.Dispose() }
    } catch {
      Fail "spectrogram archive live check failed: $($_.Exception.Message)"
    }
  }
}

if ($failures.Count -gt 0) {
  Write-Host "V5 external media readiness: FAILED"
  foreach ($failure in $failures) { Write-Host "  - $failure" }
  exit 1
}

$mode = if ($Live) { "manifest + public live sources" } else { "manifest + dated receipt" }
Write-Host "V5 external media readiness: PASS ($mode; five required assets)"
