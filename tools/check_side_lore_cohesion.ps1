param(
  [string]$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
)

$ErrorActionPreference = "Stop"
$failures = New-Object System.Collections.Generic.List[string]

function ReadText([string]$Path) {
  if (!(Test-Path $Path)) {
    $failures.Add("missing file: $Path")
    return ""
  }
  return Get-Content -Raw -Path $Path
}

function RequireText([string]$Label, [string]$Text, [string]$Needle) {
  if ($Text.IndexOf($Needle, [System.StringComparison]::Ordinal) -lt 0) {
    $failures.Add("$Label missing required text: $Needle")
  }
}

function ForbidText([string]$Label, [string]$Text, [string]$Needle) {
  if ($Text.IndexOf($Needle, [System.StringComparison]::OrdinalIgnoreCase) -ge 0) {
    $failures.Add("$Label contains forbidden stale/slop text: $Needle")
  }
}

$command = ReadText (Join-Path $RepoRoot "plugin\src\main\java\com\observance\watcher\command\ObservanceCommand.java")
$templates = ReadText (Join-Path $RepoRoot "plugin\src\main\java\com\observance\watcher\structure\StructureTemplates.java")
$applyAll = ReadText (Join-Path $RepoRoot "discord\supabase\apply-all.sql")
$experience = ReadText (Join-Path $RepoRoot "tools\check_experience_coherence.py")
$dashboard = ReadText (Join-Path $RepoRoot "dashboard\src\components\author\SideProofProgress.tsx")
$directorReport = ReadText (Join-Path $RepoRoot "dashboard\src\components\author\DirectorStateReport.tsx")
$simplification = ReadText (Join-Path $RepoRoot "design\DIRECTOR-SIMPLIFICATION.md")
$serverPrep = ReadText (Join-Path $RepoRoot "tools\prepare_server_test.ps1")

$sideSites = @(
  "school_stand",
  "the_far_water",
  "markers_row",
  "cistern_7",
  "watch_floor",
  "set_apart_shelf",
  "undercroft_seal",
  "forgotten_mouth",
  "deep_market",
  "ration_table",
  "third_bay_breach",
  "warm_town_collapse",
  "deep_bird_coops"
)

foreach ($site in $sideSites) {
  RequireText "apply-all.sql" $applyAll $site
}

foreach ($site in @(
  "school_stand",
  "markers_row",
  "cistern_7",
  "watch_floor",
  "set_apart_shelf",
  "undercroft_seal",
  "forgotten_mouth",
  "deep_market",
  "ration_table",
  "third_bay_breach",
  "warm_town_collapse",
  "deep_bird_coops"
)) {
  RequireText "check_experience_coherence.py" $experience ("site_seen_" + $site)
  RequireText "SideProofProgress.tsx" $dashboard ("site_seen_" + $site)
  RequireText "DirectorStateReport.tsx" $directorReport ("site_seen_" + $site)
}

foreach ($flag in @(
  "npc_wenna_crust_done",
  "npc_coll_lamp_done"
)) {
  RequireText "SideProofProgress.tsx" $dashboard $flag
  RequireText "DirectorStateReport.tsx" $directorReport $flag
}

foreach ($required in @(
  'seedFixtureLore(loc, "far_water")',
  'seedFixtureLore(loc, "school_stand")',
  'seedFixtureLore(loc, "markers_row")',
  'seedFixtureLore(loc, "cistern_7")',
  'seedFixtureLore(loc, "watch_floor")',
  'seedFixtureLore(loc, "set_apart_shelf")',
  'seedFixtureLore(loc, "undercroft_seal")',
  'seedFixtureLore(loc, "forgotten_mouth")',
  'seedFixtureLore(loc, "warm_town_collapse")',
  'seedFixtureLore(loc, "deep_market")',
  'seedFixtureLore(loc, "ration_table")',
  'seedFixtureLore(loc, "third_bay_breach")'
)) {
  RequireText "ObservanceCommand.java" $command $required
}

foreach ($required in @(
  "the water is only useful after the land count is honest",
  "proves the grey seventh was seen before anyone called it rumor",
  "This is before the far-water mistake",
  "His hall repeats the count lower",
  "The first dark was managed, not weather",
  "it changes who failed the watch",
  "this is where the warm story starts to split",
  "This is the bow before the door",
  "This is not a wall. It is a cover story",
  "Compare this to his safe-road notice",
  "This is before WARDEN-3 closed the warm road",
  "The table ties people to the market, not to myth",
  "the deep line is evidence, not scenery"
)) {
  RequireText "ObservanceCommand.java side lore" $command $required
}

foreach ($required in @(
  "Store ledger, lower room",
  "Shelf order card",
  "School copybook",
  "Mason order",
  "Night watch log",
  "Public notice",
  "Door account, older copy",
  "Archive repair ticket"
)) {
  RequireText "StructureTemplates.java keeper lore" $templates $required
}

foreach ($required in @(
  "decorative chiseled bookshelves are visibly occupied",
  "mechanic-owned shelves are not randomly pre-filled"
)) {
  RequireText "DIRECTOR-SIMPLIFICATION.md" $simplification $required
  RequireText "prepare_server_test.ps1" $serverPrep $required
}

foreach ($forbidden in @(
  "chiseled bookshelves are left empty unless",
  "random filler",
  "just lore",
  "lore only, does not matter",
  "mystery slop"
)) {
  ForbidText "side lore sources" ($command + $templates + $simplification + $serverPrep) $forbidden
}

if ($failures.Count -gt 0) {
  Write-Host "side lore cohesion check: FAILED"
  foreach ($failure in $failures) {
    Write-Host "  - $failure"
  }
  exit 1
}

Write-Host "side lore cohesion check: OK - side sites carry evidence, contradiction, route literacy, or later archive value across Minecraft, dashboard, and SQL surfaces"
