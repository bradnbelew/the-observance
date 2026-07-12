param(
  [string]$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
)

$ErrorActionPreference = "Stop"

$commandSource = Join-Path $RepoRoot "plugin\src\main\java\com\observance\watcher\command\ObservanceCommand.java"
$ignitionSource = Join-Path $RepoRoot "plugin\src\main\java\com\observance\watcher\signal\listener\IgnitionListener.java"
$dashboardReadme = Join-Path $RepoRoot "dashboard\README.md"
$dashboardChrome = Join-Path $RepoRoot "dashboard\src\app\site-chrome.tsx"
$launchReadinessPath = Join-Path $RepoRoot "design\LAUNCH-READINESS.md"
$nextSessionPath = Join-Path $RepoRoot "design\NEXT-SESSION.md"
$personalPlaytestPath = Join-Path $RepoRoot "design\PERSONAL-PLAYTEST-SCRIPT.md"
$manualLaunchPlanPath = Join-Path $RepoRoot "design\MANUAL-LAUNCH-PLAN.md"
$currentReadinessVerdictPath = Join-Path $RepoRoot "design\CURRENT-READINESS-VERDICT.md"
$sessionZeroPath = Join-Path $RepoRoot "design\SESSION-ZERO.md"
$launchBlockerPath = Join-Path $RepoRoot "tools\check_launch_manual_blockers.ps1"
$friendLaunchPrepPath = Join-Path $RepoRoot "tools\prepare_friend_launch.ps1"
$externalMediaCheckPath = Join-Path $RepoRoot "tools\check_external_media_readiness.ps1"
$hostedResourcePackCheckPath = Join-Path $RepoRoot "tools\check_hosted_resource_pack.ps1"
$resourcepackZipPath = Join-Path $RepoRoot "observance-resourcepack.zip"
$resourcePackSetterPath = Join-Path $RepoRoot "tools\set_resource_pack_config.ps1"
$docs = @(
  "plugin\src\main\resources\plugin.yml",
  "design\RUNBOOK.md",
  "design\DIRECTOR-SETUP-GUIDE.md",
  "design\MANUAL-LAUNCH-PLAN.md",
  "design\CURRENT-READINESS-VERDICT.md",
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
if (-not (Test-Path $launchReadinessPath)) {
  throw "operator docs check: missing launch readiness doc: $launchReadinessPath"
}
if (-not (Test-Path $nextSessionPath)) {
  throw "operator docs check: missing next-session doc: $nextSessionPath"
}
if (-not (Test-Path $personalPlaytestPath)) {
  throw "operator docs check: missing personal playtest script: $personalPlaytestPath"
}
if (-not (Test-Path $manualLaunchPlanPath)) {
  throw "operator docs check: missing manual launch plan: $manualLaunchPlanPath"
}
if (-not (Test-Path $currentReadinessVerdictPath)) {
  throw "operator docs check: missing current readiness verdict: $currentReadinessVerdictPath"
}
if (-not (Test-Path $sessionZeroPath)) {
  throw "operator docs check: missing session-zero consent script: $sessionZeroPath"
}
if (-not (Test-Path $launchBlockerPath)) {
  throw "operator docs check: missing launch blocker gate: $launchBlockerPath"
}
if (-not (Test-Path $friendLaunchPrepPath)) {
  throw "operator docs check: missing friend launch prep helper: $friendLaunchPrepPath"
}
if (-not (Test-Path $externalMediaCheckPath)) {
  throw "operator docs check: missing external media readiness gate: $externalMediaCheckPath"
}
if (-not (Test-Path $hostedResourcePackCheckPath)) {
  throw "operator docs check: missing hosted resource-pack checker: $hostedResourcePackCheckPath"
}
if (-not (Test-Path $resourcepackZipPath)) {
  throw "operator docs check: missing packaged resource pack zip: $resourcepackZipPath"
}
if (-not (Test-Path $resourcePackSetterPath)) {
  throw "operator docs check: missing resource-pack config setter: $resourcePackSetterPath"
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

$launchReadinessText = Get-Content -LiteralPath $launchReadinessPath -Raw
$hasHistoricalLaunchClaim = $launchReadinessText.IndexOf("code-complete and launch-ready", [System.StringComparison]::OrdinalIgnoreCase) -ge 0 -or
                            $launchReadinessText.IndexOf("every surface green", [System.StringComparison]::OrdinalIgnoreCase) -ge 0
if ($hasHistoricalLaunchClaim) {
  foreach ($required in @("CURRENT STATUS OVERRIDE", "not launch-ready", "check_launch_manual_blockers.ps1", "generated Deep Hold proof", "outside-Hold")) {
    if ($launchReadinessText.IndexOf($required, [System.StringComparison]::OrdinalIgnoreCase) -lt 0) {
      Write-Error "operator docs check: LAUNCH-READINESS has historical launch-ready claims but lacks current override text: $required"
      exit 1
    }
  }
}

$nextSessionText = Get-Content -LiteralPath $nextSessionPath -Raw
$hasHistoricalNextSessionClaim = $nextSessionText.IndexOf("complete and green", [System.StringComparison]::OrdinalIgnoreCase) -ge 0 -or
                                 $nextSessionText.IndexOf("code-complete end to end", [System.StringComparison]::OrdinalIgnoreCase) -ge 0 -or
                                 $nextSessionText.IndexOf("build is DONE + green", [System.StringComparison]::OrdinalIgnoreCase) -ge 0
if ($hasHistoricalNextSessionClaim) {
  foreach ($required in @("CURRENT STATUS OVERRIDE", "not launch-ready", "check_launch_manual_blockers.ps1", "generated Deep Hold proof", "outside-Hold")) {
    if ($nextSessionText.IndexOf($required, [System.StringComparison]::OrdinalIgnoreCase) -lt 0) {
      Write-Error "operator docs check: NEXT-SESSION has historical launch-ready claims but lacks current override text: $required"
      exit 1
    }
  }
}

$manualLaunchPlanText = Get-Content -LiteralPath $manualLaunchPlanPath -Raw
foreach ($required in @(
  "prepare_friend_launch.ps1",
  "ResourcePackUrl",
  "friend-launch-quickstart.md",
  "launch-blockers.md",
  "manual-media-checklist.md",
  "supabase-apply-card.md",
  "live-server-command-sheet.md",
  "friend-launch-todo.md",
  "Supabase SQL SHA1",
  "check_launch_manual_blockers.ps1 -Launch",
  "Supabase SQL",
  "launch-attestations.md",
  "resource-pack.url",
  "set_resource_pack_config.ps1",
  "check_hosted_resource_pack.ps1",
  "Build/proof the Deep Hold",
  "LIVE-REHEARSAL-EVIDENCE.md",
    "SESSION-ZERO.md",
    "observer_opt_out",
    "Credential rotation",
  "Player-discovery requirement",
  "Launch Verdict Template"
)) {
  if ($manualLaunchPlanText.IndexOf($required, [System.StringComparison]::OrdinalIgnoreCase) -lt 0) {
    Write-Error "operator docs check: MANUAL-LAUNCH-PLAN is missing required launch handoff text: $required"
    exit 1
  }
}

$sessionZeroText = Get-Content -LiteralPath $sessionZeroPath -Raw
foreach ($required in @(
  "Voice tracking is opt-in",
  "Capture switch rule",
  "observer_capture",
  "voice_capture",
  "players.observer_opt_out",
  "launch-attestations.md"
)) {
  if ($sessionZeroText.IndexOf($required, [System.StringComparison]::OrdinalIgnoreCase) -lt 0) {
    Write-Error "operator docs check: SESSION-ZERO is missing required consent/capture handoff text: $required"
    exit 1
  }
}

$operatorControlsText = Get-Content -LiteralPath (Join-Path $RepoRoot "design\OPERATOR-LIVE-CONTROLS.md") -Raw
foreach ($required in @(
  "prepare_friend_launch.ps1",
  "ResourcePackUrl",
  "friend-launch-quickstart.md",
  "launch-blockers.md",
  "manual-media-checklist.md",
  "supabase-apply-card.md",
  "live-server-command-sheet.md",
  "friend-launch-todo.md",
  "Supabase SQL SHA1",
  "Applied SQL SHA1",
  "package_launch_bundle.ps1",
  "set_resource_pack_config.ps1",
  "check_hosted_resource_pack.ps1",
  "observance-deploy-manifest.json",
  "pack readiness"
)) {
  if ($operatorControlsText.IndexOf($required, [System.StringComparison]::OrdinalIgnoreCase) -lt 0) {
    Write-Error "operator docs check: OPERATOR-LIVE-CONTROLS is missing required resource-pack handoff text: $required"
    exit 1
  }
}

$currentReadinessVerdictText = Get-Content -LiteralPath $currentReadinessVerdictPath -Raw
foreach ($required in @(
  "repo-ready but not launch-ready",
  "High-Level Verdict",
  "Ready",
  "Not Ready",
  "Contradictions Or Mismatches",
  "Stale, Placeholder, Unfinished, Forgotten, Or Weak Content",
  "Systems Needing Verification Or Re-Testing",
  "Manual Tasks Still Required",
  "Manual Task Detail",
  "Step-By-Step Manual Completion Plan",
  "Launch/Readiness Checklist",
  "Extra Suggestions",
  "Looked Built But Was Not Truly Finished",
  "Easy-To-Miss Items Now Explicitly Called Out",
  "generated Deep Hold proof",
  "launch-attestations.md",
  "check_launch_manual_blockers.ps1 -Launch",
  "observance-deploy-manifest.json"
)) {
  if ($currentReadinessVerdictText.IndexOf($required, [System.StringComparison]::OrdinalIgnoreCase) -lt 0) {
    Write-Error "operator docs check: CURRENT-READINESS-VERDICT is missing required readiness verdict text: $required"
    exit 1
  }
}

$launchBlockerText = Get-Content -LiteralPath $launchBlockerPath -Raw
foreach ($required in @(
  "prepare_friend_launch.ps1",
  "ResourcePackUrl",
  "Applied SQL SHA1",
  "Completed live rehearsal packet not supplied",
  "00-notes.md",
  "launch-attestations.md",
  "Resource pack is built but not launch-configured",
  "Resource pack URL must be an absolute HTTPS URL",
  "Resource pack SHA1 must be the 40-character lowercase hex SHA1",
  "set_resource_pack_config.ps1",
  "check_hosted_resource_pack.ps1",
  "package_launch_bundle.ps1",
  "Launch coordinate/proof CSV not supplied",
  "check_external_media_readiness.ps1",
  "friend-launch-quickstart.md",
  "launch-blockers.md",
  "manual-media-checklist.md",
  "supabase-apply-card.md",
  "live-server-command-sheet.md",
  "friend-launch-todo.md",
  "design\SESSION-ZERO.md",
  "observer_opt_out",
  "manual completion plan: design/MANUAL-LAUNCH-PLAN.md"
)) {
  if ($launchBlockerText.IndexOf($required, [System.StringComparison]::OrdinalIgnoreCase) -lt 0) {
    Write-Error "operator docs check: launch blocker gate is missing required readiness blocker text: $required"
    exit 1
  }
}

# Deployment instructions must follow the plugin manifest version. A stale literal jar name is a
# launch-breaking defect even when every command in the document is valid.
$pluginManifestPath = Join-Path $RepoRoot "plugin\src\main\resources\plugin.yml"
$pluginManifestText = Get-Content -LiteralPath $pluginManifestPath -Raw
$versionMatch = [regex]::Match($pluginManifestText, '(?m)^version:\s*["'']?([^"''\s]+)')
if (-not $versionMatch.Success) {
  Write-Error "operator docs check: could not read version from plugin.yml"
  exit 1
}
$currentPluginVersion = $versionMatch.Groups[1].Value
$versionSurfaces = @($docs) + @(
  "plugin\README.md",
  "design\LIVE-LAUNCH-RUNBOOK.md",
  "design\UNLIT-PREARG-STARTUP.md",
  "tools\new_rehearsal_packet.ps1"
)
foreach ($rel in $versionSurfaces | Select-Object -Unique) {
  $text = Get-Content -LiteralPath (Join-Path $RepoRoot $rel) -Raw
  foreach ($match in [regex]::Matches($text, 'observance-(\d+\.\d+\.\d+)\.jar')) {
    if ($match.Groups[1].Value -ne $currentPluginVersion) {
      Write-Error "operator docs check: $rel names stale plugin jar $($match.Value); current manifest is $currentPluginVersion"
      exit 1
    }
  }
}

$directorSetupGuidePath = Join-Path $RepoRoot "design\DIRECTOR-SETUP-GUIDE.md"
if (-not (Test-Path $directorSetupGuidePath)) {
  throw "operator docs check: missing director setup guide: $directorSetupGuidePath"
}
$directorSetupGuideText = Get-Content -LiteralPath $directorSetupGuidePath -Raw
foreach ($required in @(
  "repo-ready is not launch-ready",
  "audit_all.ps1",
  "prepare_friend_launch.ps1",
  "ResourcePackUrl",
  "observance-deploy-manifest.json",
  "set_resource_pack_config.ps1",
  "check_hosted_resource_pack.ps1",
  "/obs director state",
  "/obs director progress",
  "stuck-player hint view",
  "/obs site next <lane>",
  "/obs preflight",
  "/obs rehearse start",
  "Structure Proof Standard",
  "Website, Discord, And Media Discipline",
  "check_external_media_readiness.ps1",
  "check_launch_manual_blockers.ps1 -Launch",
  "GeneratedProof",
  "launch-attestations.md",
  "decision: LAUNCH",
  "Session Zero",
  "credentials were rotated"
)) {
  if ($directorSetupGuideText.IndexOf($required, [System.StringComparison]::OrdinalIgnoreCase) -lt 0) {
    Write-Error "operator docs check: DIRECTOR-SETUP-GUIDE is missing required setup handoff text: $required"
    exit 1
  }
}

$friendLaunchPrepText = Get-Content -LiteralPath $friendLaunchPrepPath -Raw
foreach ($required in @(
  "ResourcePackUrl",
  "friend-launch-quickstart.md",
  "Apply-all SHA1",
  "Ordered bundle files",
  "package_launch_bundle.ps1",
  "check_deploy_manifest.ps1",
  "Verify existing deploy bundle",
  "new_launch_placement_packet.ps1",
  "new_rehearsal_packet.ps1",
  "check_launch_manual_blockers.ps1",
  "launch-blockers.md",
  "manual-media-checklist.md",
  "supabase-apply-card.md",
  "live-server-command-sheet.md",
  "friend-launch-todo.md",
  "discord\supabase\apply-all.sql",
  "Apply-all SHA1",
  "Ordered bundle files",
  "set_resource_pack_config.ps1",
  "check_hosted_resource_pack.ps1",
  "Hosted resource-pack verification",
  "coords-capture.csv",
  "launch-blockers.md",
  "manual-media-checklist.md",
  "supabase-apply-card.md",
  "live-server-command-sheet.md",
  "friend-launch-todo.md",
  "Do not paste loose migration files",
  "Paper 1.21.11",
  "Deep Hold GeneratedProof",
  "outside-Hold anchors",
  "media_clip_01_ready",
  "ASH-13",
  "WHERE THE REEDS FOLD BACK",
  "STAY AWAKE",
  "SIX RETURN, ONE IS NOT KEPT",
  "I WAS NOT KEPT",
  "launch-attestations.md"
)) {
  if ($friendLaunchPrepText.IndexOf($required, [System.StringComparison]::OrdinalIgnoreCase) -lt 0) {
    Write-Error "operator docs check: friend launch prep helper is missing required handoff text: $required"
    exit 1
  }
}

foreach ($path in @(
  (Join-Path $RepoRoot "README.md"),
  (Join-Path $RepoRoot "plugin\README.md"),
  (Join-Path $RepoRoot "design\RUNBOOK.md"),
  $nextSessionPath,
  $personalPlaytestPath,
  $manualLaunchPlanPath,
  $currentReadinessVerdictPath
)) {
  $text = Get-Content -LiteralPath $path -Raw
  foreach ($staleGradle in @("D:\_gradle", "gradle-8.10.2\bin\gradle", "gradle-8.10.2/bin/gradle")) {
    if ($text.IndexOf($staleGradle, [System.StringComparison]::OrdinalIgnoreCase) -ge 0) {
      Write-Error "operator docs check: active docs still reference stale external Gradle path: $path :: $staleGradle"
      exit 1
    }
  }
}

Write-Host "operator docs check: OK - $($seen.Count) documented /obs subcommands match the plugin command handler"
