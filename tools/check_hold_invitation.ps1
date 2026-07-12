param(
  [string]$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path,
  [string]$WorkRoot = ""
)

$ErrorActionPreference = "Stop"

$zipFile = Join-Path $RepoRoot "dashboard\public\the-hold\the-hold.zip"
$stagingFile = Join-Path $RepoRoot "design\MANUAL-MEDIA-STAGING.md"
$failures = [System.Collections.Generic.List[string]]::new()

function Fail([string]$Message) {
  $script:failures.Add($Message) | Out-Null
}

if (-not (Test-Path -LiteralPath $zipFile)) {
  throw "hold invitation check: missing dashboard\public\the-hold\the-hold.zip"
}

if (-not (Test-Path -LiteralPath $stagingFile)) {
  throw "hold invitation check: missing design\MANUAL-MEDIA-STAGING.md"
}

$workRoot = if ([string]::IsNullOrWhiteSpace($WorkRoot)) {
  Join-Path ([System.IO.Path]::GetTempPath()) ("observance-check-hold-invitation-" + [System.Guid]::NewGuid().ToString("N"))
} else {
  [System.IO.Path]::GetFullPath($WorkRoot)
}
if (Test-Path -LiteralPath $workRoot) {
  Remove-Item -LiteralPath $workRoot -Recurse -Force
}
New-Item -ItemType Directory -Path $workRoot | Out-Null
Expand-Archive -LiteralPath $zipFile -DestinationPath $workRoot -Force

$rawEndpointPatterns = @(
  'snoikerz\.com\s*:\s*25569',
  '\b[a-z0-9.-]+\.(com|net|org|gg|io)\s*:\s*[0-9]{2,5}\b',
  '\b(?:[0-9]{1,3}\.){3}[0-9]{1,3}\s*:\s*[0-9]{2,5}\b'
)

$scanned = 0
$hits = [System.Collections.Generic.List[string]]::new()
foreach ($file in Get-ChildItem -LiteralPath $workRoot -Recurse -File) {
  $bytes = [System.IO.File]::ReadAllBytes($file.FullName)
  $views = @(
    [System.Text.Encoding]::UTF8.GetString($bytes),
    [System.Text.Encoding]::Unicode.GetString($bytes),
    [System.Text.Encoding]::ASCII.GetString($bytes)
  )
  $scanned++
  foreach ($view in $views) {
    foreach ($pattern in $rawEndpointPatterns) {
      if ([regex]::IsMatch($view, $pattern, [System.Text.RegularExpressions.RegexOptions]::IgnoreCase)) {
        $relative = $file.FullName.Substring($workRoot.TrimEnd('\', '/').Length).TrimStart('\', '/')
        $hits.Add($relative) | Out-Null
      }
    }
  }
}

if ($hits.Count -gt 0) {
  $uniqueHits = @($hits | Sort-Object -Unique)
  Fail "the-hold.zip exposes a raw server endpoint in extracted file(s): $($uniqueHits -join ', ')"
}

$joinedExtractedText = Get-ChildItem -LiteralPath $workRoot -Recurse -File |
  ForEach-Object {
    try {
      [System.Text.Encoding]::UTF8.GetString([System.IO.File]::ReadAllBytes($_.FullName))
    } catch {
      ""
    }
  } | Out-String

foreach ($requiredText in @(
  "front door: SNOIKERZ",
  "ending: common web",
  "path: /",
  "look for mirror 03",
  "0 / 7",
  "no staff listed",
  "the server address is not in this file",
  "mirror 03 keeps the old listing"
)) {
  if ($joinedExtractedText.IndexOf($requiredText, [System.StringComparison]::OrdinalIgnoreCase) -lt 0) {
    Fail "the-hold.zip missing rebuilt invitation evidence: $requiredText"
  }
}

$staging = Get-Content -LiteralPath $stagingFile -Raw
foreach ($requiredText in @(
  "the-hold.zip",
  "dashboard/public/the-hold/the-hold.zip",
  "SHA1",
  "payload"
)) {
  if ($staging.IndexOf($requiredText, [System.StringComparison]::OrdinalIgnoreCase) -lt 0) {
    Fail "MANUAL-MEDIA-STAGING.md missing Hold staging receipt text: $requiredText"
  }
}

foreach ($requiredText in @(
  "front door + common web + root path + mirror 03; no server port",
  "no raw server endpoint"
)) {
  if ($staging.IndexOf($requiredText, [System.StringComparison]::OrdinalIgnoreCase) -lt 0) {
    Fail "MANUAL-MEDIA-STAGING.md missing rebuilt Hold receipt text: $requiredText"
  }
}

if ($failures.Count -gt 0) {
  Write-Host "hold invitation check: FAILED"
  foreach ($failure in $failures) {
    Write-Host "  - $failure"
  }
  exit 1
}

Write-Host "hold invitation check: OK - scanned $scanned extracted file(s); no raw server endpoint exposed"
