param(
  [string]$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
)

$ErrorActionPreference = "Stop"

$puzzlesFile = Join-Path $RepoRoot "discord\supabase\seeds\puzzles_seed.sql"
$hintsFile = Join-Path $RepoRoot "discord\supabase\seeds\hints_seed.sql"
$threadTagsFile = Join-Path $RepoRoot "discord\supabase\seeds\thread_tags.sql"
$customsGuideFile = Join-Path $RepoRoot "design\CUSTOMS-FIELD-GUIDE.md"

$failures = [System.Collections.Generic.List[string]]::new()
function Fail([string]$Message) {
  $script:failures.Add($Message) | Out-Null
}

foreach ($file in @($puzzlesFile, $hintsFile, $threadTagsFile, $customsGuideFile)) {
  if (-not (Test-Path -LiteralPath $file)) {
    Fail "missing customs/Rosetta source: $file"
  }
}

if ($failures.Count -eq 0) {
  $puzzles = Get-Content -LiteralPath $puzzlesFile -Raw
  $hints = Get-Content -LiteralPath $hintsFile -Raw
  $hintLines = Get-Content -LiteralPath $hintsFile
  $threadTags = Get-Content -LiteralPath $threadTagsFile -Raw
  $customsGuide = Get-Content -LiteralPath $customsGuideFile -Raw

  function RequireContains([string]$Label, [string]$Text, [string]$Needle) {
    if ($Text.IndexOf($Needle, [System.StringComparison]::OrdinalIgnoreCase) -lt 0) {
      Fail "$Label missing expected text: $Needle"
    }
  }

  function RequireAbsent([string]$Label, [string]$Text, [string]$Needle) {
    if ($Text.IndexOf($Needle, [System.StringComparison]::OrdinalIgnoreCase) -ge 0) {
      Fail "$Label still contains retired text: $Needle"
    }
  }

  RequireContains "puzzles_seed.sql rosetta accepted answer" $puzzles "bow offering kept light deep line unspoken sacred beast"
  RequireContains "puzzles_seed.sql rosetta spacing variant" $puzzles "bow offering keptlight deepline unspoken sacred beast"
  RequireContains "hints_seed.sql rosetta fairness hint" $hints "the ring is asking for the seven way names"
  RequireContains "hints_seed.sql rosetta ordering hint" $hints "give the names in ring order: bow, offering, kept light, deep line, unspoken, sacred beast"

  $rosettaBlock = ""
  for ($i = 0; $i -lt $hintLines.Count; $i++) {
    if ($hintLines[$i].IndexOf("rosetta-ring", [System.StringComparison]::OrdinalIgnoreCase) -ge 0) {
      $last = [Math]::Min($hintLines.Count - 1, $i + 2)
      $rosettaBlock = ($hintLines[$i..$last] -join "`n")
      break
    }
  }
  if ([string]::IsNullOrWhiteSpace($rosettaBlock)) {
    Fail "hints_seed.sql missing rosetta-ring hint block"
  }

  foreach ($customKey in @(
    "the_bow",
    "the_offering",
    "the_kept_light",
    "the_dark_hours",
    "the_deep_line",
    "the_unspoken",
    "the_sacred_beast"
  )) {
    RequireContains "thread_tags.sql custom coverage" $threadTags $customKey
  }

  foreach ($requiredText in @(
    "## Customs Standard",
    "Folk version",
    "Practical reason",
    "Physical proof",
    "Consequence",
    "False version",
    "Late use",
    "## The Bow",
    'Custom key: `the_bow`',
    "## The Offering",
    'Custom key: `the_offering`',
    "## The Kept Light",
    'Custom key: `the_kept_light`',
    "## The Deep Line",
    'Custom key: `the_deep_line`',
    "## The Unspoken",
    'Custom key: `the_unspoken`',
    "## The Sacred Beast",
    'Custom key: `the_sacred_beast`',
    "## The Dark Hours",
    'Custom key: `the_dark_hours`',
    "bow_marker_01",
    "offering_cairn_01",
    "kept_light_home_01",
    "painted_line",
    "third_bay_breach",
    "deep_bird_coops",
    "watch_floor",
    "brann_toll_tower",
    "The seven ways should not feel like seven passwords"
  )) {
    RequireContains "CUSTOMS-FIELD-GUIDE.md contract" $customsGuide $requiredText
  }

  RequireAbsent "hints_seed.sql rosetta hint" $rosettaBlock "six ways"
  RequireAbsent "hints_seed.sql rosetta hint" $rosettaBlock "ward"
  RequireAbsent "hints_seed.sql rosetta hint" $rosettaBlock "covering"
}

if ($failures.Count -gt 0) {
  Write-Host "customs Rosetta check: FAILED"
  foreach ($failure in $failures) {
    Write-Host "  - $failure"
  }
  exit 1
}

Write-Host "customs Rosetta check: OK - seven-way answer shape, hint rail, custom coverage, and field-guide six faces stay aligned"
