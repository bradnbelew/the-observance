param(
  [string]$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
)

$ErrorActionPreference = "Stop"

$failures = [System.Collections.Generic.List[string]]::new()
function Fail([string]$Message) {
  $script:failures.Add($Message) | Out-Null
}

function ReadRequired([string]$RelativePath) {
  $path = Join-Path $RepoRoot $RelativePath
  if (-not (Test-Path -LiteralPath $path)) {
    Fail "missing required concern-closure source: $RelativePath"
    return ""
  }
  return Get-Content -LiteralPath $path -Raw
}

function RequireContains([string]$Label, [string]$Text, [string]$Needle) {
  if ($Text.IndexOf($Needle, [System.StringComparison]::OrdinalIgnoreCase) -lt 0) {
    Fail "$Label missing expected concern-closure text: $Needle"
  }
}

$closure = ReadRequired "design\DIRECTOR-CONCERN-CLOSURE.md"
$flow = ReadRequired "design\DIRECTOR-FLOW-BIBLE.md"
$setup = ReadRequired "design\DIRECTOR-SETUP-GUIDE.md"
$packet = ReadRequired "tools\new_director_packet.ps1"

foreach ($text in @(
  "playtest-proven",
  "Hold Hands Over The Server Too Easily",
  "Too Much Content Feels Irrelevant",
  "Six Keeper Stones Are Repetitive",
  "Answers Are Weirdly Formatted",
  "Customs Need More To Work With",
  "Writing Sounds Like Fantasy Gibberish",
  "Traversal Is Unclear",
  "Expeditions Could Become New Monotony",
  "Structures Could Be Boring Or Broken",
  "Director Needs To Stay In The Loop",
  "Speedrun rehearsal",
  "Side-proof retelling",
  "Copy read-aloud",
  "Sign-room pass",
  "Traversal pass",
  "Vaun is audit and sort",
  "Mara is compare and walk",
  "Sella is reflect and count",
  "Orin is crouch and align",
  "Brann is wait and listen",
  "Iss is compare and accuse",
  "No major site is launch-ready if players call it a sign room"
)) {
  RequireContains "DIRECTOR-CONCERN-CLOSURE.md" $closure $text
}

foreach ($text in @(
  "survival/customs",
  "investigation/records",
  "exploration/sites",
  "media/archive",
  "social/Wren",
  "threshold/action"
)) {
  RequireContains "DIRECTOR-CONCERN-CLOSURE.md braided lanes" $closure $text
}

foreach ($text in @(
  "DIRECTOR-CONCERN-CLOSURE.md",
  "observance-200-scenario-break-speedrun-audit.md",
  "observance-200-concern-closure-scenario-audit.md"
)) {
  RequireContains "director setup guide concern handoff" $setup $text
}

RequireContains "flow bible concern handoff" $flow "DIRECTOR-CONCERN-CLOSURE.md"
RequireContains "director packet concern handoff" $packet "DIRECTOR-CONCERN-CLOSURE.md"
RequireContains "director packet concern scenario handoff" $packet "observance-200-concern-closure-scenario-audit.md"

if ($failures.Count -gt 0) {
  Write-Host "director concern-closure check: FAILED"
  foreach ($failure in $failures) {
    Write-Host "  - $failure"
  }
  exit 1
}

Write-Host "director concern-closure check: OK - original concern categories, residual risks, and rehearsal guards stay visible"
