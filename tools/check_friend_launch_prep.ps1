param(
  [string]$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
)

$ErrorActionPreference = "Stop"

function Fail([string]$Message) {
  $script:failures.Add($Message) | Out-Null
}

function RequireFile([string]$Path) {
  if (-not (Test-Path -LiteralPath $Path)) {
    Fail "missing generated file: $Path"
    return ""
  }
  return Get-Content -LiteralPath $Path -Raw
}

function RequireContains([string]$Label, [string]$Text, [string]$Needle) {
  if ($Text.IndexOf($Needle, [System.StringComparison]::OrdinalIgnoreCase) -lt 0) {
    Fail "$Label missing expected text: $Needle"
  }
}

function RequireContainsAny([string]$Label, [string]$Text, [string[]]$Needles) {
  foreach ($needle in $Needles) {
    if ($Text.IndexOf($needle, [System.StringComparison]::OrdinalIgnoreCase) -ge 0) {
      return
    }
  }
  Fail "$Label missing one of expected texts: $($Needles -join ' | ')"
}

function RequireNotContains([string]$Label, [string]$Text, [string]$Needle) {
  if ($Text.IndexOf($Needle, [System.StringComparison]::OrdinalIgnoreCase) -ge 0) {
    Fail "$Label contains retired duplicated text: $Needle"
  }
}

function RequireNoBadControlChars([string]$Label, [string]$Text) {
  if ($Text -match "[\x00-\x08\x0B\x0C\x0E-\x1F]") {
    Fail "$Label contains non-printing control characters; check PowerShell backtick quoting in generated markdown"
  }
}

$repoFull = [System.IO.Path]::GetFullPath($RepoRoot)
$prepScript = Join-Path $repoFull "tools\prepare_friend_launch.ps1"
$date = "audit-friend-launch-prep"
$placementDir = Join-Path $repoFull "build\launch-placement\$date"
$rehearsalDir = Join-Path $repoFull "rehearsals\$date"
$quickstartFile = Join-Path $rehearsalDir "friend-launch-quickstart.md"
$blockerFile = Join-Path $rehearsalDir "launch-blockers.md"
$mediaFile = Join-Path $rehearsalDir "manual-media-checklist.md"
$todoFile = Join-Path $rehearsalDir "friend-launch-todo.md"
$commandSheetFile = Join-Path $rehearsalDir "live-server-command-sheet.md"
$supabaseApplyCardFile = Join-Path $rehearsalDir "supabase-apply-card.md"
$attestationsFile = Join-Path $rehearsalDir "launch-attestations.md"
$failures = [System.Collections.Generic.List[string]]::new()

if (-not (Test-Path -LiteralPath $prepScript)) {
  throw "friend launch prep check: missing helper: $prepScript"
}

try {
  $prepOutput = @(& powershell -NoProfile -ExecutionPolicy Bypass -File $prepScript -RepoRoot $repoFull -Date $date -SkipBundle -Force 2>&1)
  if ($LASTEXITCODE -ne 0) {
    Fail "prepare_friend_launch.ps1 failed smoke generation"
  }
  $prepText = [string]::Join("`n", ($prepOutput | ForEach-Object { [string]$_ }))
  RequireContains "prepare_friend_launch.ps1 output" $prepText "Verify existing deploy bundle"
  RequireContains "prepare_friend_launch.ps1 output" $prepText "deploy manifest check: OK"
  $prepSource = Get-Content -LiteralPath $prepScript -Raw
  RequireContains "prepare_friend_launch.ps1 source" $prepSource "rehearsal packet did not create expected command sheet"
  RequireNotContains "prepare_friend_launch.ps1 source" $prepSource "function Write-LiveServerCommandSheet"

  $quickstart = RequireFile $quickstartFile
  $blocker = RequireFile $blockerFile
  $media = RequireFile $mediaFile
  $todo = RequireFile $todoFile
  $commandSheet = RequireFile $commandSheetFile
  $supabaseApplyCard = RequireFile $supabaseApplyCardFile
  $attestations = RequireFile $attestationsFile
  $applyAllSha1 = (Get-FileHash -LiteralPath (Join-Path $repoFull "discord\supabase\apply-all.sql") -Algorithm SHA1).Hash.ToLowerInvariant()

  foreach ($entry in @(
    @("friend-launch-quickstart.md", $quickstart),
    @("launch-blockers.md", $blocker),
    @("manual-media-checklist.md", $media),
    @("friend-launch-todo.md", $todo),
    @("live-server-command-sheet.md", $commandSheet),
    @("supabase-apply-card.md", $supabaseApplyCard),
    @("launch-attestations.md", $attestations)
  )) {
    RequireNoBadControlChars $entry[0] $entry[1]
  }

  foreach ($needle in @(
    "Friend Launch Quickstart",
    "discord\supabase\apply-all.sql",
    "Apply-all SHA1",
    "Ordered bundle files",
    "observance-deploy-manifest.json",
    "Hosted resource-pack verification",
    "launch-blockers.md",
    "manual-media-checklist.md",
    "friend-launch-todo.md",
    "live-server-command-sheet.md",
    "supabase-apply-card.md",
    "Prove the Failed Accepting route",
    "check_launch_manual_blockers.ps1 -Launch"
  )) {
    RequireContains "friend-launch-quickstart.md" $quickstart $needle
  }

  foreach ($needle in @(
    "launch manual blocker check: NOT READY",
    "manual attestations still required",
    "discord/supabase/apply-all.sql"
  )) {
    RequireContains "launch-blockers.md" $blocker $needle
  }
  RequireContainsAny "launch-blockers.md" $blocker @(
    "Resource pack is built but not launch-configured",
    "hosted resource pack failed",
    "Resource pack URL must be an absolute HTTPS URL",
    "Resource pack SHA1 mismatch"
  )

  foreach ($needle in @(
    "clip_01_prior_base.mp4",
    "MANUAL-MEDIA-STAGING.md",
    "hosted is not automatically live",
    "media_clip_01_ready",
    "ASH-13",
    "https://youtu.be/du-qp_clP7c",
    "clip_02_far_water_count.mp4",
    "WHERE THE REEDS FOLD BACK",
    "https://youtu.be/iKqvPMHjR74",
    "clip_03_black_moon_toll.mp4",
    "STAY AWAKE",
    "https://youtu.be/pSPhBYMGIRc",
    "clip_04_release_room_late.mp4",
    "SIX RETURN, ONE IS NOT KEPT",
    "https://youtu.be/DtZizx5QIEs",
    "dashboard/public/the-hold/the-hold.zip",
    "69d227914501508c382952706c1f154e5e71152f",
    "copperline + hosting + common web + service 1842; no server port",
    "no raw server endpoint",
    "operator-checked on 2026-07-08",
    "recovered_archive_ready",
    "I WAS NOT KEPT",
    "check_external_media_readiness.ps1"
  )) {
    RequireContains "manual-media-checklist.md" $media $needle
  }

  foreach ($needle in @(
    "Friend Launch Todo",
    "Exact Receipts",
    "discord\supabase\apply-all.sql",
    'Apply `discord\supabase\apply-all.sql`',
    "Do not paste loose migration files",
    "Paper 1.21.11",
    "observance-resourcepack.zip",
    "prepare_friend_launch.ps1 -ResourcePackUrl",
    "media checklist, Supabase card, command sheet, launch todo, and attestations are refreshed together",
    "supabase-apply-card.md",
    "build/proof the Deep Hold rows",
    "Prove the Failed Accepting route",
    "no witness",
    "answers are not witness",
    "witness before accepting",
    "remaining outside-Hold anchors",
    "live-server-command-sheet.md",
    "media_clip_01_ready",
    "recovered_archive_ready",
    "SESSION-ZERO.md",
    "service_role",
    "check_launch_manual_blockers.ps1 -Launch",
    "decision: LAUNCH"
  )) {
    RequireContains "friend-launch-todo.md" $todo $needle
  }

  foreach ($needle in @(
    "Live Server Command Sheet",
    "/observance status",
    "/observance preflight",
    "/observance visualaudit",
    "/observance dialogueaudit",
    "/observance site launch",
    "/observance placehold build",
    "/observance placehold audit",
    "/observance placehold sync",
    "Failed Accepting gate",
    "no witness",
    "answers are not witness",
    "witness before accepting",
    "/observance site next",
    "/observance site set <siteId>",
    "/obs unlit audit",
    "/obs unlit ready",
    "/obs unlit pass light",
    "Normal Non-Op Pass",
    "real non-op player account",
    "/observance flag set media_clip_01_ready true",
    "/observance flag set recovered_archive_ready true",
    "launch-attestations.md"
  )) {
    RequireContains "live-server-command-sheet.md" $commandSheet $needle
  }

  foreach ($needle in @(
    "Supabase Apply Card",
    "fdnmhbpxnodrnbrzrlqq",
    "discord\supabase\apply-all.sql",
    $applyAllSha1,
    "Apply-all SHA1 to record",
    "Ordered bundle files",
    "discord\supabase\apply-tonight.sql",
    "Do not paste loose migration or seed files",
    "discord\src\db\build-apply-all.ts",
    "/observance status",
    "supabase configured: true",
    "last db call ok: true",
    "queued writes: 0",
    "Copy To Attestations"
  )) {
    RequireContains "supabase-apply-card.md" $supabaseApplyCard $needle
  }

  foreach ($needle in @(
    "No loose migration or seed files",
    "Normal Non-Op Player Pass",
    "Failed Accepting / Post-Keeper Gate",
    "prior_witness_ready",
    "real player account joined without operator status",
    "Applied SQL SHA1",
    "media_clip_01_ready",
    "media_clip_02_ready",
    "media_clip_03_ready",
    "media_clip_04_ready",
    "recovered_archive_ready",
    "manual-media-checklist.md",
    "decision: LAUNCH / DO NOT LAUNCH"
  )) {
    RequireContains "launch-attestations.md" $attestations $needle
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
  Write-Host "friend launch prep check: FAILED"
  foreach ($failure in $failures) {
    Write-Host "  - $failure"
  }
  exit 1
}

Write-Host "friend launch prep check: OK - quickstart, blocker report, manual media checklist, Supabase card, command sheet, launch todo, and launch attestations generate cleanly"
