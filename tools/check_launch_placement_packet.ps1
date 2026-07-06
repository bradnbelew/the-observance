param(
  [string]$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
)

$ErrorActionPreference = "Stop"

$generatorFile = Join-Path $RepoRoot "tools\new_launch_placement_packet.ps1"
$applyFile = Join-Path $RepoRoot "tools\apply_launch_coords.ps1"
$qualityFile = Join-Path $RepoRoot "tools\check_launch_coord_quality.ps1"
$commandFile = Join-Path $RepoRoot "plugin\src\main\java\com\observance\watcher\command\ObservanceCommand.java"
$sitesFile = Join-Path $RepoRoot "plugin\src\main\resources\sites.yml"
$runbookFile = Join-Path $RepoRoot "design\RUNBOOK.md"
$directorFile = Join-Path $RepoRoot "design\DIRECTOR-SIMPLIFICATION.md"

foreach ($file in @($generatorFile, $applyFile, $qualityFile, $commandFile, $sitesFile, $runbookFile, $directorFile)) {
  if (-not (Test-Path $file)) {
    throw "launch placement packet check: missing required file: $file"
  }
}

$commandSource = Get-Content -LiteralPath $commandFile -Raw
$applySource = Get-Content -LiteralPath $applyFile -Raw
$runbook = Get-Content -LiteralPath $runbookFile -Raw
$director = Get-Content -LiteralPath $directorFile -Raw
$failures = [System.Collections.Generic.List[string]]::new()

function Fail([string]$Message) {
  $script:failures.Add($Message) | Out-Null
}

function RequireContains([string]$Label, [string]$Text, [string]$Needle) {
  if ($Text.IndexOf($Needle, [System.StringComparison]::OrdinalIgnoreCase) -lt 0) {
    Fail "$Label missing expected text: $Needle"
  }
}

function QuotedStrings([string]$Text) {
  return [string[]]([regex]::Matches($Text, '"([^"]+)"') | ForEach-Object { $_.Groups[1].Value })
}

function Extract-StringArray([string]$Name) {
  $pattern = "private\s+static\s+final\s+String\[\]\s+$([regex]::Escape($Name))\s*=\s*\{(?<body>.*?)\};"
  $m = [regex]::Match($commandSource, $pattern, [System.Text.RegularExpressions.RegexOptions]::Singleline)
  if (-not $m.Success) {
    throw "launch placement packet check: could not find String[] $Name"
  }
  return QuotedStrings $m.Groups["body"].Value
}

$launchSites = Extract-StringArray "LAUNCH_REQUIRED_SITES"

$outRoot = Join-Path $RepoRoot "build\check-launch-placement"
& powershell -NoProfile -ExecutionPolicy Bypass -File $generatorFile -RepoRoot $RepoRoot -OutRoot $outRoot -Date "audit" -Force | Out-Null
if ($LASTEXITCODE -ne 0) {
  Fail "new_launch_placement_packet.ps1 failed its audit dry-run"
}

$packetDir = Join-Path $outRoot "audit"
$mdPath = Join-Path $packetDir "00-placement.md"
$csvPath = Join-Path $packetDir "launch-sites.csv"
$capturePath = Join-Path $packetDir "coords-capture.csv"
foreach ($file in @($mdPath, $csvPath, $capturePath)) {
  if (-not (Test-Path $file)) {
    Fail "launch placement packet missing generated file: $file"
  }
}

if (Test-Path $csvPath) {
  $rows = @(Import-Csv -LiteralPath $csvPath)
  if ($rows.Count -ne $launchSites.Count) {
    Fail "launch-sites.csv has $($rows.Count) row(s), expected $($launchSites.Count)"
  }
  foreach ($id in $launchSites) {
    if (-not ($rows | Where-Object { $_.SiteId -eq $id })) {
      Fail "launch-sites.csv missing launch site '$id'"
    }
  }
}

if (Test-Path $capturePath) {
  $captureRows = @(Import-Csv -LiteralPath $capturePath)
  if ($captureRows.Count -ne $launchSites.Count) {
    Fail "coords-capture.csv has $($captureRows.Count) row(s), expected $($launchSites.Count)"
  }
  foreach ($column in @("ApproachShot", "FocalShot", "ActionShot", "ExitShot", "CohesionNotes")) {
    if (-not ($captureRows | Get-Member -Name $column -MemberType NoteProperty)) {
      Fail "coords-capture.csv missing quality-proof column '$column'"
    }
  }
}

if (Test-Path $mdPath) {
  $md = Get-Content -LiteralPath $mdPath -Raw
  foreach ($required in @(
    "launch-required coordinate anchors",
    "/obs site plan",
    "/obs site set",
    "/obs placeworld",
    "check_launch_coord_quality.ps1",
    "apply_launch_coords.ps1",
    "silhouette / palette / lighting / body verb / action-answer legibility",
    "Visual status is KEEP",
    "four proof-shot columns",
    "CohesionNotes"
  )) {
    RequireContains "00-placement.md" $md $required
  }
  foreach ($id in $launchSites) {
    RequireContains "00-placement.md" $md ('`' + $id + '`')
  }
}

RequireContains "RUNBOOK.md" $runbook "tools\new_launch_placement_packet.ps1"
RequireContains "RUNBOOK.md" $runbook "coords-capture.csv"
RequireContains "RUNBOOK.md" $runbook "tools\check_launch_coord_quality.ps1"
RequireContains "RUNBOOK.md" $runbook "tools\apply_launch_coords.ps1"
RequireContains "DIRECTOR-SIMPLIFICATION.md" $director "new_launch_placement_packet.ps1"
RequireContains "DIRECTOR-SIMPLIFICATION.md" $director "check_launch_coord_quality.ps1"
RequireContains "DIRECTOR-SIMPLIFICATION.md" $director "apply_launch_coords.ps1"

& powershell -NoProfile -ExecutionPolicy Bypass -File $qualityFile -RepoRoot $RepoRoot -CaptureCsv $capturePath | Out-Null
if ($LASTEXITCODE -ne 0) {
  Fail "check_launch_coord_quality.ps1 failed generated packet audit"
}

foreach ($required in @(
  "preview only",
  "-Apply",
  "VisualStatus must be KEEP",
  "Refusing to update sites file outside repo"
)) {
  RequireContains "apply_launch_coords.ps1" $applySource $required
}

$applySmokeSites = Join-Path $packetDir "sites-apply-smoke.yml"
Copy-Item -LiteralPath $sitesFile -Destination $applySmokeSites -Force
$applySmokeCsv = Join-Path $packetDir "coords-apply-smoke.csv"
@"
SiteId,ChosenWorld,X,Y,Z,SurveyedBy,VisualStatus,ApproachShot,FocalShot,ActionShot,ExitShot,CohesionNotes,Notes
first_report_lectern_01,world,11,64,-22,audit,KEEP,approach.png,focal.png,action.png,exit.png,near the rune teaching route,synthetic apply smoke
"@ | Set-Content -LiteralPath $applySmokeCsv -Encoding UTF8

& powershell -NoProfile -ExecutionPolicy Bypass -File $applyFile -RepoRoot $RepoRoot -SitesFile $applySmokeSites -CaptureCsv $applySmokeCsv | Out-Null
if ($LASTEXITCODE -ne 0) {
  Fail "apply_launch_coords.ps1 failed preview smoke test"
}

& powershell -NoProfile -ExecutionPolicy Bypass -File $applyFile -RepoRoot $RepoRoot -SitesFile $applySmokeSites -CaptureCsv $applySmokeCsv -Apply | Out-Null
if ($LASTEXITCODE -ne 0) {
  Fail "apply_launch_coords.ps1 failed apply smoke test"
}
$appliedSmoke = Get-Content -LiteralPath $applySmokeSites -Raw
foreach ($required in @(
  'first_report_lectern_01:',
  'world: "world"',
  'x: 11',
  'y: 64',
  'z: -22'
)) {
  RequireContains "apply smoke sites.yml" $appliedSmoke $required
}

if ($failures.Count -gt 0) {
  foreach ($failure in $failures) {
    [Console]::Error.WriteLine($failure)
  }
  exit 1
}

Write-Host "launch placement packet check: OK - generator covers $($launchSites.Count) launch-required site anchors"
