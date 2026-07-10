param(
  [string]$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
)

$ErrorActionPreference = "Stop"

function Fail([string]$Message) {
  $script:failures.Add($Message) | Out-Null
}

function RequireContains([string]$Label, [string]$Text, [string]$Needle) {
  if ($Text.IndexOf($Needle, [System.StringComparison]::OrdinalIgnoreCase) -lt 0) {
    Fail "$Label missing expected text: $Needle"
  }
}

function RequireNoBadControlChars([string]$Label, [string]$Text) {
  if ($Text -match "[\x00-\x08\x0B\x0C\x0E-\x1F]") {
    Fail "$Label contains non-printing control characters; check PowerShell backtick quoting in generated markdown"
  }
}

$repoFull = [System.IO.Path]::GetFullPath($RepoRoot)
$script = Join-Path $repoFull "tools\prepare_server_test.ps1"
$date = "audit-server-test-prep"
$placementDir = Join-Path $repoFull "build\launch-placement\$date"
$rehearsalDir = Join-Path $repoFull "rehearsals\$date"
$guideFile = Join-Path $rehearsalDir "server-test-guide.md"
$failures = [System.Collections.Generic.List[string]]::new()

if (-not (Test-Path $script)) {
  throw "server test prep check: missing helper: $script"
}

try {
  $output = @(& powershell -NoProfile -ExecutionPolicy Bypass -File $script -RepoRoot $repoFull -Date $date -SkipBundle -Force 2>&1)
  if ($LASTEXITCODE -ne 0) {
    Fail "prepare_server_test.ps1 failed smoke generation"
  }
  $outText = [string]::Join("`n", ($output | ForEach-Object { [string]$_ }))
  RequireContains "prepare_server_test.ps1 output" $outText "server test prep ready"
  RequireContains "prepare_server_test.ps1 output" $outText "Test Depths"

  if (-not (Test-Path $guideFile)) {
    Fail "missing generated server-test-guide.md: $guideFile"
  } else {
    $guide = Get-Content -LiteralPath $guideFile -Raw
    RequireNoBadControlChars "server-test-guide.md" $guide
    foreach ($needle in @(
      "Server Test Guide",
      "Test Depths",
      "Smoke test",
      "Vertical slice",
      "Full rehearsal",
      "Launch go/no-go",
      "TECHNICAL-SMOKE-PASS",
      "PLAYABLE-SLICE-PASS",
      "SERVER-TEST-READY",
      "DO NOT LAUNCH",
      "Generated Packet Paths",
      "discord\supabase\apply-all.sql",
      "observance-resourcepack.zip",
      "Server Install Checklist",
      "/observance status",
      "/observance preflight",
      "/observance prepworld",
      "/observance townsfolk spawn",
      "/observance wren spawn",
      "Inspect the Rosetta as a normal player",
      "the rune/plaintext crib signs must render",
      "Inspect Orin's crouch/banner site",
      "standing should feel blocked or awkward at the lintel",
      "there should be no beacon beams",
      "operator knowledge or a sky marker",
      "/obs unlit audit",
      "coords-capture.csv",
      "/observance site next <lane>",
      "/observance site set <siteId>",
      "manual-media-checklist.md",
      "media_clip_01_ready",
      "recovered_archive_ready",
      "Scenario Stress List",
      "A required movement has no player-facing pointer",
      "check_rehearsal_packet.ps1",
      "check_launch_manual_blockers.ps1 -Launch"
    )) {
      RequireContains "server-test-guide.md" $guide $needle
    }
  }
} finally {
  foreach ($path in @($placementDir, $rehearsalDir)) {
    $full = [System.IO.Path]::GetFullPath($path)
    if ($full.StartsWith($repoFull, [System.StringComparison]::OrdinalIgnoreCase) -and (Test-Path -LiteralPath $full)) {
      Remove-Item -LiteralPath $full -Recurse -Force
    }
  }
}

if ($failures.Count -gt 0) {
  Write-Host "server test prep check: FAILED"
  foreach ($failure in $failures) {
    Write-Host "  - $failure"
  }
  exit 1
}

Write-Host "server test prep check: OK - generated guide preserves smoke, vertical-slice, full-rehearsal, and launch go/no-go distinctions"
