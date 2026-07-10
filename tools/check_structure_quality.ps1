param(
  [string]$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path,
  [switch]$Launch
)

$ErrorActionPreference = "Stop"

$structuresFile = Join-Path $RepoRoot "design\structures.md"
$visualRescueFile = Join-Path $RepoRoot "design\VISUAL-RESCUE.md"
$ledgerFile = Join-Path $RepoRoot "design\CLUE-LEDGER.md"
$sitesFile = Join-Path $RepoRoot "plugin\src\main\resources\sites.yml"
$placementPacketFile = Join-Path $RepoRoot "tools\new_launch_placement_packet.ps1"
$worldBuildCheckFile = Join-Path $RepoRoot "tools\check_world_build_readiness.ps1"

$failures = [System.Collections.Generic.List[string]]::new()
function Fail([string]$Message) {
  $script:failures.Add($Message) | Out-Null
}

foreach ($file in @($structuresFile, $visualRescueFile, $ledgerFile, $sitesFile, $placementPacketFile, $worldBuildCheckFile)) {
  if (-not (Test-Path -LiteralPath $file)) {
    Fail "missing required structure-quality source: $file"
  }
}

if ($failures.Count -eq 0) {
  $structures = Get-Content -LiteralPath $structuresFile -Raw
  $visual = Get-Content -LiteralPath $visualRescueFile -Raw
  $ledger = Get-Content -LiteralPath $ledgerFile -Raw
  $placement = Get-Content -LiteralPath $placementPacketFile -Raw
  $worldBuild = Get-Content -LiteralPath $worldBuildCheckFile -Raw

  foreach ($requiredText in @(
    "playable investigation spaces",
    "not clue billboards",
    "two non-sign clue surfaces",
    "architecture",
    "inventory",
    "item frames",
    "lecterns/books",
    "block counts",
    "sound",
    "light",
    "water/reflection",
    "entity behavior",
    "pathing",
    "redstone/comparator state",
    "map art",
    "time condition",
    "co-op action",
    "clear entrance",
    "clear exit",
    "softlock",
    "fallback",
    "text/book readability",
    "traversal vector",
    "no major structure is launch-ready if its only clue surface is a sign"
  )) {
    if ($structures.IndexOf($requiredText, [System.StringComparison]::OrdinalIgnoreCase) -lt 0) {
      Fail "structures.md missing structure-quality doctrine: $requiredText"
    }
  }

  foreach ($forbiddenText in @(
    "Answer surface: the stone IS a `keeper_stone` answer-site",
    "No extra build."
  )) {
    if ($structures.IndexOf($forbiddenText, [System.StringComparison]::OrdinalIgnoreCase) -ge 0) {
      Fail "structures.md still contains old signboard/answer-surface doctrine: $forbiddenText"
    }
  }

  foreach ($keeperId in @("vaun", "mara", "sella", "orin", "brann", "iss")) {
    if ($structures.IndexOf("### $keeperId", [System.StringComparison]::OrdinalIgnoreCase) -lt 0) {
      Fail "structures.md missing distinct keeper investigation spec for $keeperId"
    }
  }

  foreach ($requiredText in @(
    "KEEP",
    "RESHAPE",
    "REPLACE",
    "CUT",
    "approach silhouette",
    "palette",
    "focal object",
    "lighting",
    "player movement"
  )) {
    if ($visual.IndexOf($requiredText, [System.StringComparison]::OrdinalIgnoreCase) -lt 0) {
      Fail "VISUAL-RESCUE.md missing required visual audit term: $requiredText"
    }
  }

  foreach ($requiredText in @(
    "ApproachShot",
    "FocalShot",
    "ActionShot",
    "ExitShot",
    "CohesionNotes"
  )) {
    if ($placement.IndexOf($requiredText, [System.StringComparison]::OrdinalIgnoreCase) -lt 0) {
      Fail "new_launch_placement_packet.ps1 missing structure proof column: $requiredText"
    }
  }

  foreach ($requiredText in @(
    "structure quality",
    "non-sign",
    "clue surface",
    "CLUE-LEDGER"
  )) {
    if ($worldBuild.IndexOf($requiredText, [System.StringComparison]::OrdinalIgnoreCase) -lt 0) {
      Fail "check_world_build_readiness.ps1 does not reference new structure/ledger doctrine: $requiredText"
    }
  }

  foreach ($requiredText in @(
    "hold-address-reconstruction",
    "vaun-audit",
    "mara-editions",
    "sella-seven-count",
    "orin-posture",
    "brann-dark-hours",
    "iss-forgery",
    "accepting-convergence"
  )) {
    if ($ledger.IndexOf("| $requiredText |", [System.StringComparison]::OrdinalIgnoreCase) -lt 0) {
      Fail "CLUE-LEDGER.md missing structure-linked clue row: $requiredText"
    }
  }

  if ($Launch) {
    $siteText = Get-Content -LiteralPath $sitesFile -Raw
    foreach ($siteId in @(
      "stone_vaun",
      "stone_mara",
      "stone_sella",
      "stone_orin",
      "stone_brann",
      "stone_iss",
      "unbroken_light",
      "the_threshold",
      "the_far_water",
      "the_cold_hearth"
    )) {
      $pattern = "(?ms)^\s{2}$([regex]::Escape($siteId)):\s*(?<body>.*?)(?=^\s{2}[A-Za-z0-9_-]+:\s*|\z)"
      $m = [regex]::Match($siteText, $pattern)
      if (-not $m.Success) {
        Fail "sites.yml missing launch structure site: $siteId"
        continue
      }
      $body = $m.Groups["body"].Value
      foreach ($coord in @("x", "y", "z")) {
        if ($body -match "(?m)^\s{4}${coord}:\s*null\b") {
          Fail "$siteId still has placeholder $coord coordinate during launch structure-quality audit"
        }
      }
    }
  }
}

if ($failures.Count -gt 0) {
  Write-Host "structure quality check: FAILED"
  foreach ($failure in $failures) {
    Write-Host "  - $failure"
  }
  exit 1
}

Write-Host "structure quality check: OK - structure doctrine, visual proof packet, and clue-ledger links are present"
