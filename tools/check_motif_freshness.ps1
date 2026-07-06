param(
  [string]$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
)

$ErrorActionPreference = "Stop"

$failures = [System.Collections.Generic.List[string]]::new()
function Fail([string]$Message) {
  $script:failures.Add($Message) | Out-Null
}

function ReadRequired([string]$Path) {
  if (-not (Test-Path -LiteralPath $Path)) {
    Fail "missing required freshness source: $Path"
    return ""
  }
  return Get-Content -LiteralPath $Path -Raw
}

function RelPath([string]$Path) {
  $full = [System.IO.Path]::GetFullPath($Path)
  $root = [System.IO.Path]::GetFullPath($RepoRoot).TrimEnd('\', '/')
  if ($full.StartsWith($root, [System.StringComparison]::OrdinalIgnoreCase)) {
    return $full.Substring($root.Length).TrimStart('\', '/')
  }
  return $full
}

$runtimeFiles = @(
  "discord\src\voice.ts",
  "discord\src\voice.archive.ts",
  "discord\supabase\seeds\hints_seed.sql",
  "discord\supabase\seeds\metapuzzle_seed.sql",
  "discord\supabase\seeds\progression_seed.sql",
  "discord\supabase\seeds\puzzles_seed.sql",
  "discord\supabase\seeds\seventh_seed.sql",
  "discord\supabase\seeds\side_quests.sql",
  "discord\supabase\seeds\thread_cards.sql"
)

$documentRoot = Join-Path $RepoRoot "arc\lore\documents"
if (-not (Test-Path -LiteralPath $documentRoot)) {
  Fail "missing lore document root: $documentRoot"
}

$scanFiles = [System.Collections.Generic.List[string]]::new()
foreach ($rel in $runtimeFiles) {
  $scanFiles.Add((Join-Path $RepoRoot $rel)) | Out-Null
}
if (Test-Path -LiteralPath $documentRoot) {
  Get-ChildItem -LiteralPath $documentRoot -Recurse -File -Filter *.md |
    ForEach-Object { $scanFiles.Add($_.FullName) | Out-Null }
}

$forbiddenPhrases = @(
  "thing you could do to a person",
  "keeping you was the only proof",
  "the count does not come out even",
  "not a hero or a monster",
  "i forgot that was a thing",
  "be scared now"
)

foreach ($path in $scanFiles) {
  $text = ReadRequired $path
  if ($text -eq "") { continue }
  foreach ($phrase in $forbiddenPhrases) {
    if ($text.IndexOf($phrase, [System.StringComparison]::OrdinalIgnoreCase) -ge 0) {
      $rel = RelPath $path
      Fail "$rel contains stale/stock phrase: $phrase"
    }
  }
}

$stockKeepPhrase = "we would keep you, if you would keep the ways"
$stockKeepHits = [System.Collections.Generic.List[string]]::new()
foreach ($path in $scanFiles) {
  $text = ReadRequired $path
  if ($text -eq "") { continue }
  if ($text.IndexOf($stockKeepPhrase, [System.StringComparison]::OrdinalIgnoreCase) -ge 0) {
    $stockKeepHits.Add((RelPath $path)) | Out-Null
  }
}
if ($stockKeepHits.Count -gt 1) {
  Fail "stock warmth phrase '$stockKeepPhrase' appears in $($stockKeepHits.Count) player-facing/runtime source(s): $($stockKeepHits -join ', '). Keep it rare or rewrite the echoes."
}

$wren = ReadRequired (Join-Path $RepoRoot "discord\src\voice.archive.ts")
foreach ($required in @(
  "you didn’t give me a clean name. you gave me a true one.",
  "not close. not taken. let out.",
  "the missing place is doing arithmetic of its own"
)) {
  if ($wren.IndexOf($required, [System.StringComparison]::Ordinal) -lt 0) {
    Fail "Wren reckoning rewrite missing expected line: $required"
  }
}

$finale = ReadRequired (Join-Path $RepoRoot "discord\src\voice.ts")
foreach ($required in @(
  "a hand on the ledger after the hand was gone",
  "i am sorry for every name i held too tightly"
)) {
  if ($finale.IndexOf($required, [System.StringComparison]::Ordinal) -lt 0) {
    Fail "finale release rewrite missing expected line: $required"
  }
}

if ($failures.Count -gt 0) {
  Write-Host "motif freshness check: FAILED"
  foreach ($failure in $failures) {
    Write-Host "  - $failure"
  }
  exit 1
}

Write-Host "motif freshness check: OK - stale Wren/finale stock phrases stay out of player-facing prose"
