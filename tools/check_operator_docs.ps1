param(
  [string]$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
)

$ErrorActionPreference = "Stop"

$commandSource = Join-Path $RepoRoot "plugin\src\main\java\com\observance\watcher\command\ObservanceCommand.java"
$ignitionSource = Join-Path $RepoRoot "plugin\src\main\java\com\observance\watcher\signal\listener\IgnitionListener.java"
$dashboardReadme = Join-Path $RepoRoot "dashboard\README.md"
$dashboardChrome = Join-Path $RepoRoot "dashboard\src\app\site-chrome.tsx"
$docs = @(
  "plugin\src\main\resources\plugin.yml",
  "design\RUNBOOK.md",
  "design\DIRECTOR-SIMPLIFICATION.md",
  "design\OPERATOR-LIVE-CONTROLS.md",
  "design\DIALOGUE-WORLD-AUDIT.md",
  "design\VISUAL-RESCUE.md"
)

foreach ($sourceFile in @($commandSource, $ignitionSource)) {
  if (-not (Test-Path $sourceFile)) {
    throw "operator docs check: missing source: $sourceFile"
  }
}
foreach ($file in @($dashboardReadme, $dashboardChrome)) {
  if (-not (Test-Path $file)) {
    throw "operator docs check: missing dashboard auth-removal guard file: $file"
  }
}

$source = Get-Content -LiteralPath $commandSource -Raw
$known = [System.Collections.Generic.HashSet[string]]::new([System.StringComparer]::OrdinalIgnoreCase)
[regex]::Matches($source, 'case\s+"([^"]+)"\s*->') | ForEach-Object {
  [void]$known.Add($_.Groups[1].Value)
}

if ($known.Count -eq 0) {
  throw "operator docs check: no Observance subcommands found in $commandSource"
}

$pattern = [regex]'/(?:obs|observance)\s+([a-z][a-z0-9_-]*)'
$failures = New-Object System.Collections.Generic.List[string]
$seen = [System.Collections.Generic.HashSet[string]]::new([System.StringComparer]::OrdinalIgnoreCase)

foreach ($rel in $docs) {
  $path = Join-Path $RepoRoot $rel
  if (-not (Test-Path $path)) {
    throw "operator docs check: missing doc: $path"
  }
  $lineNo = 0
  foreach ($line in Get-Content -LiteralPath $path) {
    $lineNo += 1
    foreach ($match in $pattern.Matches($line)) {
      $cmd = $match.Groups[1].Value
      [void]$seen.Add($cmd)
      if (-not $known.Contains($cmd)) {
        $failures.Add("${rel}:${lineNo}: /obs $cmd is documented but not handled by ObservanceCommand")
      }
    }
  }
}

if ($failures.Count -gt 0) {
  $failures | ForEach-Object { Write-Error $_ }
  exit 1
}

$dashboardText = (Get-Content -LiteralPath $dashboardReadme -Raw) + "`n" + (Get-Content -LiteralPath $dashboardChrome -Raw)
foreach ($pattern in @("legacy auth", "auth callback", "login-gated", "route stays login", "is sign-in gated", "is sign in gated")) {
  if ($dashboardText.IndexOf($pattern, [System.StringComparison]::OrdinalIgnoreCase) -ge 0) {
    Write-Error "operator docs check: dashboard still contains stale auth-removal wording: $pattern"
    exit 1
  }
}

$runbookText = Get-Content -LiteralPath (Join-Path $RepoRoot "design\RUNBOOK.md") -Raw
$ignitionText = Get-Content -LiteralPath $ignitionSource -Raw
if ($ignitionText.Contains("player.isSneaking()") -and
    $runbookText.IndexOf("sneak-right-click", [System.StringComparison]::OrdinalIgnoreCase) -lt 0) {
  Write-Error "operator docs check: RUNBOOK ignition instructions must mention sneak-right-click when IgnitionListener requires sneaking"
  exit 1
}

foreach ($rel in @("design\FIRST-PLAYTEST.md", "design\GO-LIVE-TONIGHT.md", "plugin\TODO-GOLIVE.md")) {
  $path = Join-Path $RepoRoot $rel
  if (-not (Test-Path $path)) {
    throw "operator docs check: missing retired guide: $path"
  }
  $text = Get-Content -LiteralPath $path -Raw
  $lineCount = ($text -split "`r?`n").Count
  if ($lineCount -gt 12 -or
      $text.IndexOf("retired", [System.StringComparison]::OrdinalIgnoreCase) -lt 0 -or
      $text.IndexOf("RUNBOOK.md", [System.StringComparison]::OrdinalIgnoreCase) -lt 0) {
    Write-Error "operator docs check: $rel must stay a short retired-guide pointer, not a competing launch checklist"
    exit 1
  }
  foreach ($staleMarker in @("Historical below", "## 0.", "## 1.", "Option A", "Option B", "PebbleHost", "0004_oracle")) {
    if ($text.IndexOf($staleMarker, [System.StringComparison]::OrdinalIgnoreCase) -ge 0) {
      Write-Error "operator docs check: $rel still contains stale launch-playbook wording: $staleMarker"
      exit 1
    }
  }
}

$buildPlanPath = Join-Path $RepoRoot "design\BUILD-PLAN.md"
if (Test-Path $buildPlanPath) {
  $buildPlanText = Get-Content -LiteralPath $buildPlanPath -Raw
  $hasHistoricalStatus = $buildPlanText.IndexOf("PENDING apply", [System.StringComparison]::OrdinalIgnoreCase) -ge 0 -or
                         $buildPlanText.IndexOf("new, not yet written", [System.StringComparison]::OrdinalIgnoreCase) -ge 0 -or
                         $buildPlanText.IndexOf("the stopgap", [System.StringComparison]::OrdinalIgnoreCase) -ge 0
  if ($hasHistoricalStatus -and
      ($buildPlanText.IndexOf("CURRENT STATUS OVERRIDE", [System.StringComparison]::OrdinalIgnoreCase) -lt 0 -or
       $buildPlanText.IndexOf("historical build skeleton", [System.StringComparison]::OrdinalIgnoreCase) -lt 0)) {
    Write-Error "operator docs check: BUILD-PLAN has historical status claims but lacks a current-status override"
    exit 1
  }
}

Write-Host "operator docs check: OK - $($seen.Count) documented /obs subcommands match the plugin command handler"
