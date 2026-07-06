param(
  [string]$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
)

$ErrorActionPreference = "Stop"

$failures = [System.Collections.Generic.List[string]]::new()
function Fail([string]$Message) {
  $script:failures.Add($Message) | Out-Null
}

function ReadText([string]$Path) {
  if (-not (Test-Path -LiteralPath $Path)) {
    Fail "missing required brand surface file: $Path"
    return ""
  }
  return Get-Content -LiteralPath $Path -Raw
}

function ContainsUnicodeRune([string]$Text) {
  foreach ($ch in $Text.ToCharArray()) {
    $code = [int][char]$ch
    if ($code -ge 0x16A0 -and $code -le 0x16FF) {
      return $true
    }
  }
  return $false
}

$scanRoots = @(
  (Join-Path $RepoRoot "dashboard\src"),
  (Join-Path $RepoRoot "discord\src"),
  (Join-Path $RepoRoot "brand")
)

foreach ($root in $scanRoots) {
  if (-not (Test-Path $root)) {
    Fail "missing brand scan root: $root"
    continue
  }
  Get-ChildItem -LiteralPath $root -Recurse -File |
    Where-Object { $_.Extension -in @(".ts", ".tsx", ".svg", ".md") } |
    ForEach-Object {
      $text = Get-Content -LiteralPath $_.FullName -Raw
      if (ContainsUnicodeRune $text) {
        Fail "real Unicode rune glyph found in source surface: $($_.FullName). Use the invented RuneGlyphs/forge renderer instead."
      }
    }
}

$recordPages = @(
  "dashboard\src\app\record\[slug]\page.tsx",
  "dashboard\src\app\record\archive\page.tsx",
  "dashboard\src\app\record\terminal\page.tsx"
)

foreach ($rel in $recordPages) {
  $path = Join-Path $RepoRoot $rel
  $text = ReadText $path
  if ($text -eq "") { continue }
  if ($text.IndexOf("RuneGlyphs", [System.StringComparison]::Ordinal) -lt 0) {
    Fail "$rel must use RuneGlyphs for Record header marks"
  }
  if ($text.IndexOf("from `"@/lib/RuneGlyphs`"", [System.StringComparison]::Ordinal) -lt 0 -and
      $text.IndexOf("from '@/lib/RuneGlyphs'", [System.StringComparison]::Ordinal) -lt 0) {
    Fail "$rel imports no RuneGlyphs renderer"
  }
}

$sigilPath = Join-Path $RepoRoot "brand\sigil.svg"
$sigil = ReadText $sigilPath
if ($sigil -ne "") {
  foreach ($required in @("#C8A24B", "#E8E2D4", "#0a0c0f", "The Keeper's Eye")) {
    if ($sigil.IndexOf($required, [System.StringComparison]::OrdinalIgnoreCase) -lt 0) {
      Fail "brand/sigil.svg missing expected brand mark: $required"
    }
  }

  $allowedSigilColors = [System.Collections.Generic.HashSet[string]]::new([System.StringComparer]::OrdinalIgnoreCase)
  foreach ($color in @("#0a0c0f", "#8f897b", "#cfc8b8", "#C8A24B", "#3a2f16", "#E8E2D4")) {
    [void]$allowedSigilColors.Add($color)
  }
  foreach ($match in [regex]::Matches($sigil, '#[0-9A-Fa-f]{6}')) {
    $color = $match.Value
    if (-not $allowedSigilColors.Contains($color)) {
      Fail "brand/sigil.svg uses non-approved color $color; keep the sigil on the ink/parchment/gilt palette"
    }
  }
}

$brandSource = ReadText (Join-Path $RepoRoot "discord\src\brand.ts")
foreach ($required in @("accent: '#C8A24B'", "parchment: '#E8E2D4'", "ink: '#0E1116'")) {
  if ($brandSource.IndexOf($required, [System.StringComparison]::Ordinal) -lt 0) {
    Fail "discord/src/brand.ts missing expected token: $required"
  }
}

$dashboardRuneSource = ReadText (Join-Path $RepoRoot "dashboard\src\lib\runes.ts")
$discordRuneSource = ReadText (Join-Path $RepoRoot "discord\src\forge\runes.ts")
foreach ($literal in @("const LETTER_BRANCHES", "const DIGIT_BRANCHES", "GLYPH_W", "GLYPH_H")) {
  if ($dashboardRuneSource.IndexOf($literal, [System.StringComparison]::Ordinal) -lt 0) {
    Fail "dashboard rune source missing $literal"
  }
  if ($discordRuneSource.IndexOf($literal, [System.StringComparison]::Ordinal) -lt 0) {
    Fail "discord rune source missing $literal"
  }
}

if ($failures.Count -gt 0) {
  Write-Host "brand surface check: FAILED"
  foreach ($failure in $failures) {
    Write-Host "  - $failure"
  }
  exit 1
}

Write-Host "brand surface check: OK - Record glyphs use the invented alphabet and sigil colors stay on-brand"
