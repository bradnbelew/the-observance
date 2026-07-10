param(
  [string]$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
)

$ErrorActionPreference = "Stop"

$puzzlesFile = Join-Path $RepoRoot "discord\supabase\seeds\puzzles_seed.sql"
$metapuzzleFile = Join-Path $RepoRoot "discord\supabase\seeds\metapuzzle_seed.sql"
$threadCardsFile = Join-Path $RepoRoot "discord\supabase\seeds\thread_cards.sql"
$structuresFile = Join-Path $RepoRoot "design\structures.md"
$ledgerFile = Join-Path $RepoRoot "design\CLUE-LEDGER.md"
$keeperDossiersFile = Join-Path $RepoRoot "design\KEEPER-INVESTIGATION-DOSSIERS.md"

$failures = [System.Collections.Generic.List[string]]::new()
function Fail([string]$Message) {
  $script:failures.Add($Message) | Out-Null
}

foreach ($file in @($puzzlesFile, $metapuzzleFile, $threadCardsFile, $structuresFile, $ledgerFile, $keeperDossiersFile)) {
  if (-not (Test-Path -LiteralPath $file)) {
    Fail "missing keeper-investigation source: $file"
  }
}

if ($failures.Count -eq 0) {
  $puzzles = Get-Content -LiteralPath $puzzlesFile -Raw
  $metapuzzle = Get-Content -LiteralPath $metapuzzleFile -Raw
  $threadCards = Get-Content -LiteralPath $threadCardsFile -Raw
  $structures = Get-Content -LiteralPath $structuresFile -Raw
  $ledger = Get-Content -LiteralPath $ledgerFile -Raw
  $keeperDossiers = Get-Content -LiteralPath $keeperDossiersFile -Raw

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

  RequireContains "puzzles_seed.sql" $puzzles "'vaun-bookshelf-tally'"
  RequireContains "puzzles_seed.sql vaun-bookshelf-tally" $puzzles "'set_flags', jsonb_build_object('vaun_tally_read', true)"
  RequireAbsent "puzzles_seed.sql stone-orin answers" $puzzles "'threshold',"

  foreach ($contract in @(
    "where puzzle_key = 'stone-vaun'",
    "jsonb_build_object('vaun_tally_read', true)",
    "where puzzle_key = 'stone-mara'",
    "jsonb_build_object('mara_walked', true)",
    "where puzzle_key = 'stone-orin'",
    "jsonb_build_object('orin_key_found', true)",
    "where puzzle_key in ('stone-brann', 'stone-brann-cipher')",
    "jsonb_build_object('brann_toll_heard', true, 'brann_corridor_passed', true)"
  )) {
    RequireContains "metapuzzle_seed.sql keeper-gating contract" $metapuzzle $contract
  }

  foreach ($retiredReveal in @(
    "'stone-vaun', null",
    "'stone-mara', null",
    "'stone-orin', null",
    "'stone-brann', null"
  )) {
    RequireAbsent "thread_cards.sql primary keeper-card reveal" $threadCards $retiredReveal
  }

  foreach ($investigationReveal in @(
    "'vaun-bookshelf-tally', null",
    "'vaun-hoard-sorted', null",
    "'mara-lectern-lock', null",
    "'orin-frame-dials', null",
    "'brann-black-moon-toll', null",
    "'brann-silence-corridor', null",
    "'stone-brann-cipher', null"
  )) {
    RequireContains "thread_cards.sql investigation-card reveal" $threadCards $investigationReveal
  }

  foreach ($keeperSpec in @(
    "vaun - Audit Of What Was Kept",
    "mara - Editions That Disagree",
    "sella - The Count That Water Refuses",
    "orin - Posture And Sealed Sightlines",
    "brann - Dark Hours And Listening",
    "iss - Comfort That Does Not Match The Land"
  )) {
    RequireContains "structures.md keeper investigation spec" $structures $keeperSpec
  }

  foreach ($row in @(
    "vaun-audit",
    "mara-editions",
    "sella-seven-count",
    "orin-posture",
    "brann-dark-hours",
    "iss-forgery"
  )) {
    RequireContains "CLUE-LEDGER.md keeper row" $ledger "| $row |"
  }

  foreach ($requiredText in @(
    "## Keeper Standard",
    "## Vaun - Audit Of What Was Kept",
    'Ledger row: `vaun-audit`',
    "## Mara - Editions That Disagree",
    'Ledger row: `mara-editions`',
    "## Sella - The Count That Water Refuses",
    'Ledger row: `sella-seven-count`',
    "## Orin - Posture And Sealed Sightlines",
    'Ledger row: `orin-posture`',
    "## Brann - Dark Hours And Listening",
    'Ledger row: `brann-dark-hours`',
    "## Iss - Comfort That Does Not Match The Land",
    'Ledger row: `iss-forgery`',
    "### Player behavior",
    "### Non-sign surfaces",
    "### Side evidence with weight",
    "### Old stone role",
    "### Fair answer shape",
    "### Rehearsal failure",
    "The six investigations should feel different in hand",
    "If every keeper looks like"
  )) {
    RequireContains "KEEPER-INVESTIGATION-DOSSIERS.md contract" $keeperDossiers $requiredText
  }
}

if ($failures.Count -gt 0) {
  Write-Host "keeper investigations check: FAILED"
  foreach ($failure in $failures) {
    Write-Host "  - $failure"
  }
  exit 1
}

Write-Host "keeper investigations check: OK - stone ciphers are gated behind evidence clusters, keeper dossiers, and obvious shortcuts stay retired"
