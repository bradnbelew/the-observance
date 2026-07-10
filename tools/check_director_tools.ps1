param(
  [string]$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
)

$ErrorActionPreference = "Stop"

$flowBibleFile = Join-Path $RepoRoot "design\DIRECTOR-FLOW-BIBLE.md"
$setupGuideFile = Join-Path $RepoRoot "design\DIRECTOR-SETUP-GUIDE.md"
$directorPacketTool = Join-Path $RepoRoot "tools\new_director_packet.ps1"
$authorPageFile = Join-Path $RepoRoot "dashboard\src\app\author\page.tsx"
$directorStateFile = Join-Path $RepoRoot "dashboard\src\components\author\DirectorStateReport.tsx"
$directorProgressFile = Join-Path $RepoRoot "dashboard\src\components\author\DirectorProgressReport.tsx"
$observanceCommandFile = Join-Path $RepoRoot "plugin\src\main\java\com\observance\watcher\command\ObservanceCommand.java"

$failures = [System.Collections.Generic.List[string]]::new()
function Fail([string]$Message) {
  $script:failures.Add($Message) | Out-Null
}

foreach ($file in @($flowBibleFile, $setupGuideFile, $directorPacketTool, $authorPageFile, $directorStateFile, $directorProgressFile, $observanceCommandFile)) {
  if (-not (Test-Path -LiteralPath $file)) {
    Fail "missing director tool source: $file"
  }
}

if ($failures.Count -eq 0) {
  $flowBible = Get-Content -LiteralPath $flowBibleFile -Raw
  $setupGuide = Get-Content -LiteralPath $setupGuideFile -Raw
  $packetTool = Get-Content -LiteralPath $directorPacketTool -Raw
  $authorPage = Get-Content -LiteralPath $authorPageFile -Raw
  $directorState = Get-Content -LiteralPath $directorStateFile -Raw
  $directorProgress = Get-Content -LiteralPath $directorProgressFile -Raw
  $observanceCommand = Get-Content -LiteralPath $observanceCommandFile -Raw

  function RequireContains([string]$Label, [string]$Text, [string]$Needle) {
    if ($Text.IndexOf($Needle, [System.StringComparison]::OrdinalIgnoreCase) -lt 0) {
      Fail "$Label missing expected text: $Needle"
    }
  }

  foreach ($phase in @(
    "Phase 0 - Hold Copy",
    "Phase 1 - First Server Wrongness",
    "Phase 2 - Rosetta And Record",
    "Phase 4 - Keeper Field",
    "Phase 8 - Media And Recovered Archive",
    "Phase 9 - Unlit Expedition",
    "Phase 10 - Threshold And Accepting",
    "Setup-Readiness Boundary"
  )) {
    RequireContains "DIRECTOR-FLOW-BIBLE.md" $flowBible $phase
  }

  foreach ($contract in @(
    "what players experience",
    "how they know where to pull next",
    "A required destination needs a pointer",
    "DIRECTOR-SETUP-GUIDE.md",
    "The keeper field is now six investigations",
    "The repo can be setup-ready while the launch is not"
  )) {
    RequireContains "DIRECTOR-FLOW-BIBLE.md contract" $flowBible $contract
  }

  foreach ($guideText in @(
    "repo-ready is not launch-ready",
    "Setup Chain",
    "/obs director state",
    "/obs director progress",
    "Structure Proof Standard",
    "Website, Discord, And Media Discipline",
    "Final Go/No-Go",
    "67 launch-required coordinates",
    "decision: LAUNCH"
  )) {
    RequireContains "DIRECTOR-SETUP-GUIDE.md" $setupGuide $guideText
  }

  RequireContains "new_director_packet.ps1" $packetTool "Director Pass Order"
  RequireContains "new_director_packet.ps1" $packetTool "DIRECTOR-FLOW-BIBLE.md"
  RequireContains "new_director_packet.ps1" $packetTool "DIRECTOR-SETUP-GUIDE.md"
  RequireContains "new_director_packet.ps1" $packetTool "check_keeper_investigations.ps1"
  RequireContains "new_director_packet.ps1" $packetTool "check_customs_rosetta.ps1"

  RequireContains "author page" $authorPage "DirectorStateReport"
  RequireContains "author page" $authorPage "DirectorProgressReport"
  RequireContains "author page" $authorPage "answer_attempts"
  RequireContains "author page" $authorPage "solves"
  RequireContains "author page" $authorPage "hints"
  RequireContains "DirectorStateReport.tsx" $directorState "Open player leads"
  RequireContains "DirectorStateReport.tsx" $directorState "Customs heat"
  RequireContains "DirectorStateReport.tsx" $directorState "Next operator move"
  RequireContains "DirectorStateReport.tsx" $directorState "bowed_as_one"
  RequireContains "DirectorProgressReport.tsx" $directorProgress "Player pace and stuck hints"
  RequireContains "DirectorProgressReport.tsx" $directorProgress "Stuck-player hint view"
  RequireContains "DirectorProgressReport.tsx" $directorProgress "Recent wrong inputs"
  RequireContains "DirectorProgressReport.tsx" $directorProgress "mirrors /obs director progress"

  RequireContains "ObservanceCommand.java" $observanceCommand "handleDirectorState"
  RequireContains "ObservanceCommand.java" $observanceCommand "handleDirectorProgress"
  RequireContains "ObservanceCommand.java" $observanceCommand "director <state|progress|world|lab>"
  RequireContains "ObservanceCommand.java" $observanceCommand "Open player leads"
  RequireContains "ObservanceCommand.java" $observanceCommand "Media gates"
  RequireContains "ObservanceCommand.java" $observanceCommand "Finale readiness"
  RequireContains "ObservanceCommand.java" $observanceCommand "Next operator move"
  RequireContains "ObservanceCommand.java" $observanceCommand "Player progress summary"
  RequireContains "ObservanceCommand.java" $observanceCommand "Stuck-player hint view"

  foreach ($rowFile in @(
    "plugin\src\main\java\com\observance\watcher\data\rows\HintRow.java",
    "plugin\src\main\java\com\observance\watcher\data\rows\SolveReadRow.java",
    "plugin\src\main\java\com\observance\watcher\data\rows\AnswerAttemptReadRow.java"
  )) {
    if (-not (Test-Path -LiteralPath (Join-Path $RepoRoot $rowFile))) {
      Fail "missing director progress data row: $rowFile"
    }
  }
}

if ($failures.Count -gt 0) {
  Write-Host "director tools check: FAILED"
  foreach ($failure in $failures) {
    Write-Host "  - $failure"
  }
  exit 1
}

Write-Host "director tools check: OK - flow bible, packet generator, author dashboard, and in-game director state command are wired"
