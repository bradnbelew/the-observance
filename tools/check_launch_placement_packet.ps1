param(
  [string]$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
)

$ErrorActionPreference = "Stop"

$generatorFile = Join-Path $RepoRoot "tools\new_launch_placement_packet.ps1"
$applyFile = Join-Path $RepoRoot "tools\apply_launch_coords.ps1"
$qualityFile = Join-Path $RepoRoot "tools\check_launch_coord_quality.ps1"
$commandFile = Join-Path $RepoRoot "plugin\src\main\java\com\observance\watcher\command\ObservanceCommand.java"
$sitesFile = Join-Path $RepoRoot "plugin\src\main\resources\sites.yml"
$applyAllSqlFile = Join-Path $RepoRoot "discord\supabase\apply-all.sql"
$deployManifestFile = Join-Path $RepoRoot "observance-deploy-manifest.json"
$datapackZipFile = Join-Path $RepoRoot "observance-datapack.zip"
$resourcepackZipFile = Join-Path $RepoRoot "observance-resourcepack.zip"
$runbookFile = Join-Path $RepoRoot "design\RUNBOOK.md"
$directorFile = Join-Path $RepoRoot "design\DIRECTOR-SIMPLIFICATION.md"

foreach ($file in @($generatorFile, $applyFile, $qualityFile, $commandFile, $sitesFile, $applyAllSqlFile, $deployManifestFile, $datapackZipFile, $resourcepackZipFile, $runbookFile, $directorFile)) {
  if (-not (Test-Path $file)) {
    throw "launch placement packet check: missing required file: $file"
  }
}

$pluginJars = @(Get-ChildItem -LiteralPath (Join-Path $RepoRoot "plugin\build\libs") -Filter "*.jar" -File -ErrorAction SilentlyContinue | Sort-Object LastWriteTimeUtc -Descending)
if ($pluginJars.Count -eq 0) {
  throw "launch placement packet check: missing built plugin jar under plugin\build\libs"
}
$pluginJarFile = $pluginJars[0].FullName

$commandSource = Get-Content -LiteralPath $commandFile -Raw
$applySource = Get-Content -LiteralPath $applyFile -Raw
$qualitySource = Get-Content -LiteralPath $qualityFile -Raw
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

function FileSha1([string]$Path) {
  return (Get-FileHash -LiteralPath $Path -Algorithm SHA1).Hash.ToLowerInvariant()
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
  foreach ($column in @("Lane", "PlacementMethod", "SurveyCommand", "AfterSurvey")) {
    if (-not ($rows | Get-Member -Name $column -MemberType NoteProperty)) {
      Fail "launch-sites.csv missing placement column '$column'"
    }
  }
  foreach ($id in $launchSites) {
    if (-not ($rows | Where-Object { $_.SiteId -eq $id })) {
      Fail "launch-sites.csv missing launch site '$id'"
    }
  }
  foreach ($lane in @("prologue", "keepers", "customs", "human", "deep", "dread", "dimensions")) {
    if (-not ($rows | Where-Object { $_.Lane -eq $lane })) {
      Fail "launch-sites.csv has no rows for placement lane '$lane'"
    }
  }
  foreach ($id in @("rune_rosetta", "stone_vaun", "school_stand", "deep_market", "threshold_vault", "dread_route_start")) {
    $row = @($rows | Where-Object { $_.SiteId -eq $id }) | Select-Object -First 1
    if ($null -eq $row) {
      Fail "launch-sites.csv missing Hold-owned site '$id'"
    } elseif ($row.PlacementMethod -ne "Deep Hold generated") {
      Fail "launch-sites.csv marks Hold-owned site '$id' as '$($row.PlacementMethod)', expected Deep Hold generated"
    } elseif ($row.SurveyCommand -notmatch "placehold build") {
      Fail "launch-sites.csv Hold-owned site '$id' does not point at /obs placehold build"
    } elseif ($row.PlaceRule -match "scatter away") {
      Fail "launch-sites.csv Hold-owned site '$id' still carries a manual scatter place rule"
    }
  }
  foreach ($id in @("first_report_lectern_01", "nether_forge", "end_seventh_shrine")) {
    $row = @($rows | Where-Object { $_.SiteId -eq $id }) | Select-Object -First 1
    if ($null -ne $row -and $row.PlacementMethod -eq "Deep Hold generated") {
      Fail "launch-sites.csv incorrectly marks outside-Hold site '$id' as Deep Hold generated"
    }
  }
}

if (Test-Path $capturePath) {
  $captureRows = @(Import-Csv -LiteralPath $capturePath)
  if ($captureRows.Count -ne $launchSites.Count) {
    Fail "coords-capture.csv has $($captureRows.Count) row(s), expected $($launchSites.Count)"
  }
  foreach ($column in @("Lane", "PlacementMethod", "GeneratedProof", "PlaceworldReceipt", "ApproachShot", "FocalShot", "ActionShot", "ExitShot", "CohesionNotes")) {
    if (-not ($captureRows | Get-Member -Name $column -MemberType NoteProperty)) {
      Fail "coords-capture.csv missing quality-proof column '$column'"
    }
  }
  foreach ($row in $captureRows) {
    if (-not [string]::IsNullOrWhiteSpace([string]$row.GeneratedProof)) {
      Fail "coords-capture.csv prefilled GeneratedProof for '$($row.SiteId)'; proof cells must start blank"
    }
    if (-not [string]::IsNullOrWhiteSpace([string]$row.PlaceworldReceipt)) {
      Fail "coords-capture.csv prefilled PlaceworldReceipt for '$($row.SiteId)'; receipt cells must start blank"
    }
  }
}

if (Test-Path $mdPath) {
  $md = Get-Content -LiteralPath $mdPath -Raw
  foreach ($required in @(
    "Launch Receipt",
    "Supabase SQL",
    "discord\supabase\apply-all.sql",
    "Apply-all SHA1",
    (FileSha1 $applyAllSqlFile),
    "Deploy manifest",
    "observance-deploy-manifest.json",
    "Plugin jar",
    "Plugin jar SHA1",
    (FileSha1 $pluginJarFile),
    "Datapack zip",
    "observance-datapack.zip",
    "Datapack SHA1",
    (FileSha1 $datapackZipFile),
    "Resource pack zip",
    "observance-resourcepack.zip",
    "Resource pack SHA1",
    (FileSha1 $resourcepackZipFile),
    "resource-pack.url",
    "set_resource_pack_config.ps1",
    "check_hosted_resource_pack.ps1",
    "friend-launch-quickstart.md",
    "launch-blockers.md",
    "manual-media-checklist.md",
    "supabase-apply-card.md",
    "live-server-command-sheet.md",
    "friend-launch-todo.md",
    "launch-attestations.md",
    "Remaining launch proof rows",
    "surveyed-stamp-pending",
    "Placement Lanes",
    "PlacementMethod",
    "Deep Hold generated",
    "GeneratedProof",
    "PlaceworldReceipt",
    "nether_forge_placed",
    "end_seventh_shrine_placed",
    "/obs placehold build",
    "/obs placehold audit",
    "generated-room proof",
    "/obs site plan lanes",
    "/obs site next <lane>",
    "/obs site plan",
    "/obs site set",
    "/obs placeworld",
    "check_launch_coord_quality.ps1",
    "apply_launch_coords.ps1",
    "silhouette / palette / lighting / body verb / action-answer legibility",
    "Visual status is KEEP",
    "four proof-shot columns",
    "CohesionNotes",
    "prologue - Prologue / first literacy",
    "human - Human-history side proof web",
    "deep - Deep route, market, and finale"
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
RequireContains "check_launch_coord_quality.ps1" $qualitySource "PlaceworldReceipt"
RequireContains "check_launch_coord_quality.ps1" $qualitySource "has no /obs placeworld receipt"
RequireContains "check_launch_coord_quality.ps1" $qualitySource "nether_forge_placed"
RequireContains "check_launch_coord_quality.ps1" $qualitySource "end_seventh_shrine_placed"

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
SiteId,Lane,ChosenWorld,X,Y,Z,SurveyedBy,VisualStatus,ApproachShot,FocalShot,ActionShot,ExitShot,CohesionNotes,Notes
first_report_lectern_01,prologue,world,11,64,-22,audit,KEEP,approach.png,focal.png,action.png,exit.png,near the rune teaching route,synthetic apply smoke
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
