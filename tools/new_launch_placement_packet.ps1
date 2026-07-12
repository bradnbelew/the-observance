param(
  [string]$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path,
  [string]$OutRoot = (Join-Path $RepoRoot "build\launch-placement"),
  [string]$Date = (Get-Date -Format "yyyy-MM-dd"),
  [switch]$Force
)

$ErrorActionPreference = "Stop"

$repoFull = (Resolve-Path $RepoRoot).Path
$packetDir = Join-Path $OutRoot $Date
$packetFull = [System.IO.Path]::GetFullPath($packetDir)
if (-not $packetFull.StartsWith($repoFull, [System.StringComparison]::OrdinalIgnoreCase)) {
  throw "Refusing to create launch placement packet outside repo: $packetFull"
}
if ((Test-Path $packetFull) -and -not $Force) {
  throw "Launch placement packet already exists: $packetFull (use -Force to replace)"
}

$sitesFile = Join-Path $repoFull "plugin\src\main\resources\sites.yml"
$commandFile = Join-Path $repoFull "plugin\src\main\java\com\observance\watcher\command\ObservanceCommand.java"
foreach ($file in @($sitesFile, $commandFile)) {
  if (-not (Test-Path $file)) {
    throw "launch placement packet: missing required file: $file"
  }
}

if (Test-Path $packetFull) {
  Remove-Item -LiteralPath $packetFull -Recurse -Force
}
New-Item -ItemType Directory -Force -Path $packetFull | Out-Null

$sitesText = Get-Content -LiteralPath $sitesFile -Raw
$commandSource = Get-Content -LiteralPath $commandFile -Raw

function Clean-Value([string]$Value) {
  $v = ($Value -replace '\s+#.*$', '').Trim()
  if ($v.StartsWith('"') -and $v.EndsWith('"')) {
    return $v.Substring(1, $v.Length - 2)
  }
  return $v
}

function Unescape-JavaString([string]$Value) {
  return $Value.Replace('\"', '"').Replace('\\', '\')
}

function QuotedStrings([string]$Text) {
  return [string[]]([regex]::Matches($Text, '"((?:\\.|[^"\\])*)"') | ForEach-Object {
    Unescape-JavaString $_.Groups[1].Value
  })
}

function Extract-StringArray([string]$Name) {
  $pattern = "private\s+static\s+final\s+String\[\]\s+$([regex]::Escape($Name))\s*=\s*\{(?<body>.*?)\};"
  $m = [regex]::Match($commandSource, $pattern, [System.Text.RegularExpressions.RegexOptions]::Singleline)
  if (-not $m.Success) {
    throw "launch placement packet: could not find String[] $Name"
  }
  return QuotedStrings $m.Groups["body"].Value
}

function Extract-FirstColumn2d([string]$Name) {
  $pattern = "private\s+static\s+final\s+String\[\]\[\]\s+$([regex]::Escape($Name))\s*=\s*\{(?<body>.*?)\};"
  $m = [regex]::Match($commandSource, $pattern, [System.Text.RegularExpressions.RegexOptions]::Singleline)
  if (-not $m.Success) {
    throw "launch placement packet: could not find String[][] $Name"
  }
  return [string[]]([regex]::Matches($m.Groups["body"].Value, '\{\s*"([^"]+)"') | ForEach-Object { $_.Groups[1].Value })
}

function Extract-HoldSiteIds {
  return [string[]]([regex]::Matches($commandSource, 'new\s+HoldSite\(\s*"([^"]+)"') | ForEach-Object {
    $_.Groups[1].Value
  })
}

function Extract-PlacementLanes {
  $pattern = 'new\s+PlacementLane\(\s*"(?<id>[^"]+)"\s*,\s*"(?<label>[^"]+)"\s*,\s*new\s+String\[\]\s*\{(?<body>.*?)\}\s*\)'
  $lanes = [System.Collections.Generic.List[object]]::new()
  foreach ($m in [regex]::Matches($commandSource, $pattern, [System.Text.RegularExpressions.RegexOptions]::Singleline)) {
    $lanes.Add([pscustomobject]@{
      id = $m.Groups["id"].Value
      label = $m.Groups["label"].Value
      sites = QuotedStrings $m.Groups["body"].Value
    }) | Out-Null
  }
  if ($lanes.Count -eq 0) {
    throw "launch placement packet: could not find PLACEMENT_LANES"
  }
  return $lanes
}

$sites = [ordered]@{}
$current = $null
foreach ($line in ($sitesText -split "`r?`n")) {
  $siteMatch = [regex]::Match($line, '^\s{2}([A-Za-z0-9_-]+):\s*(?:#.*)?$')
  if ($siteMatch.Success) {
    $id = $siteMatch.Groups[1].Value
    if ($id -notin @("radius", "protect", "vertical-radius")) {
      $current = [ordered]@{
        id = $id
        type = ""
        world = ""
        x = "null"
        y = "null"
        z = "null"
        enabled = "true"
        radius = ""
      }
      $sites[$id] = $current
    }
    continue
  }
  if ($null -ne $current) {
    $propMatch = [regex]::Match($line, '^\s{4}([A-Za-z0-9_-]+):\s*(.*?)\s*$')
    if ($propMatch.Success) {
      $current[$propMatch.Groups[1].Value] = Clean-Value $propMatch.Groups[2].Value
    }
  }
}

$launchSites = Extract-StringArray "LAUNCH_REQUIRED_SITES"
$surveyFixtures = Extract-StringArray "PLACEWORLD_SURVEY_FIXTURES"
$keeperSpine = Extract-FirstColumn2d "KEEPER_SPINE"
$holdSites = Extract-HoldSiteIds
$holdSiteSet = [System.Collections.Generic.HashSet[string]]::new([System.StringComparer]::OrdinalIgnoreCase)
foreach ($holdSite in $holdSites) { [void]$holdSiteSet.Add($holdSite) }
$placementLanes = Extract-PlacementLanes
$siteLane = @{}
foreach ($lane in $placementLanes) {
  foreach ($siteId in $lane.sites) {
    $siteLane[$siteId] = $lane.id
  }
}

$briefMatch = [regex]::Match(
  $commandSource,
  'private\s+PlacementBrief\s+placementBrief\s*\([^)]*\)\s*\{(?<body>.*?)\r?\n\s*private\s+PlacementBrief\s+keeperPlacementBrief',
  [System.Text.RegularExpressions.RegexOptions]::Singleline
)
if (-not $briefMatch.Success) {
  throw "launch placement packet: could not find placementBrief()"
}
$briefBody = $briefMatch.Groups["body"].Value

function Brief-For([string]$SiteId) {
  $casePattern = 'case\s+"' + [regex]::Escape($SiteId) + '"\s*->\s*(?<expr>.*?)(?=\r?\n\s*case\s+"|\r?\n\s*default\s*->)'
  $m = [regex]::Match($briefBody, $casePattern, [System.Text.RegularExpressions.RegexOptions]::Singleline)
  if (-not $m.Success) {
    return [pscustomobject]@{ intent = ""; place = ""; proof = "" }
  }
  $expr = $m.Groups["expr"].Value
  $parts = QuotedStrings $expr
  if ($expr -match 'keeperPlacementBrief' -and $parts.Count -ge 3) {
    return [pscustomobject]@{
      intent = "$($parts[0]) keeper evidence site; $($parts[1]) becomes a readable place"
      place = "$($parts[2]); scatter away from sibling evidence sites so it is found, not farmed"
      proof = "approach silhouette, keeper-specific focal object, answer surface, and route away"
    }
  }
  if ($expr -match 'dreadPlacementBrief' -and $parts.Count -ge 2) {
    return [pscustomobject]@{
      intent = "dread route $($parts[0]); scary because of space and timing, not text"
      place = "$($parts[1]); keep signs diegetic or absent"
      proof = "approach, focal pressure object, player action/body position, and exit/aftertaste"
    }
  }
  if ($parts.Count -ge 3) {
    return [pscustomobject]@{ intent = $parts[0]; place = $parts[1]; proof = $parts[2] }
  }
  return [pscustomobject]@{ intent = ""; place = ""; proof = "" }
}

function Is-Placeholder($Site) {
  if ($null -eq $Site) { return $true }
  return ([string]$Site.x -eq "null" -or [string]$Site.y -eq "null" -or [string]$Site.z -eq "null" -or
    [string]::IsNullOrWhiteSpace([string]$Site.x) -or
    [string]::IsNullOrWhiteSpace([string]$Site.y) -or
    [string]::IsNullOrWhiteSpace([string]$Site.z))
}

function After-Survey([string]$SiteId) {
  if ($holdSiteSet.Contains($SiteId)) {
    return "No hand survey first: run /obs placehold build, then /obs placehold audit/sync; capture proof from the generated Hold room."
  }
  if ($SiteId -eq "nether_forge" -or $SiteId -eq "end_seventh_shrine") {
    return "Run /obs placeworld while standing in the correct Nether/End world."
  }
  if ($keeperSpine -contains $SiteId) {
    return "Run /obs placeworld from the curated build world to stamp the spine set-piece."
  }
  if ($surveyFixtures -contains $SiteId) {
    return "Run /obs placeworld from this world to stamp the surveyed fixture."
  }
  return "No placeworld stamp required; the surveyed anchor is live."
}

function Get-RelativePath([string]$Path) {
  $full = [System.IO.Path]::GetFullPath($Path)
  if ($full.StartsWith($repoFull, [System.StringComparison]::OrdinalIgnoreCase)) {
    return $full.Substring($repoFull.Length).TrimStart('\', '/')
  }
  return $full
}

function Get-FileSha1([string]$Path) {
  if (-not (Test-Path $Path)) {
    return "MISSING"
  }
  return (Get-FileHash -LiteralPath $Path -Algorithm SHA1).Hash.ToLowerInvariant()
}

function Get-FirstFile([string]$Pattern) {
  $files = @(Get-ChildItem -LiteralPath (Split-Path $Pattern -Parent) -Filter (Split-Path $Pattern -Leaf) -File -ErrorAction SilentlyContinue | Sort-Object LastWriteTimeUtc -Descending)
  if ($files.Count -eq 0) {
    return $null
  }
  return $files[0].FullName
}

$applyAllSqlFile = Join-Path $repoFull "discord\supabase\apply-all.sql"
$deployManifestFile = Join-Path $repoFull "observance-deploy-manifest.json"
$pluginJarFile = Get-FirstFile (Join-Path $repoFull "plugin\build\libs\*.jar")
$datapackZipFile = Join-Path $repoFull "observance-datapack.zip"
$resourcepackZipFile = Join-Path $repoFull "observance-resourcepack.zip"

$rows = [System.Collections.Generic.List[object]]::new()
$order = 0
foreach ($id in $launchSites) {
  $order++
  $site = if ($sites.Contains($id)) { $sites[$id] } else { $null }
  $brief = Brief-For $id
  $isHoldOwned = $holdSiteSet.Contains($id)
  $placementMethod = if ($isHoldOwned) {
    "Deep Hold generated"
  } elseif ($id -eq "nether_forge" -or $id -eq "end_seventh_shrine") {
    "Dimension survey"
  } elseif ($keeperSpine -contains $id -or $surveyFixtures -contains $id) {
    "Bespoke survey + placeworld"
  } else {
    "Bespoke survey"
  }
  $status = if (Is-Placeholder $site) {
    "TODO"
  } elseif ($isHoldOwned) {
    "GENERATED_PROOF_PENDING"
  } elseif ($placementMethod -eq "Dimension survey" -or $placementMethod -eq "Bespoke survey + placeworld") {
    "SURVEYED_STAMP_PENDING"
  } else {
    "COORDS_CAPTURED"
  }
  $setupCommand = if ($isHoldOwned) { "/obs placehold build; /obs placehold audit; /obs placehold sync" } else { "/obs site set $id" }
  $placeRule = if ($isHoldOwned) {
    "Generated inside the Deep Hold district plan; use the intent as the room-proof lens, not as a manual scatter rule."
  } else {
    $brief.place
  }
  $rows.Add([pscustomobject]@{
    Order = $order
    Lane = if ($siteLane.ContainsKey($id)) { [string]$siteLane[$id] } else { "ungrouped" }
    SiteId = $id
    Status = $status
    PlacementMethod = $placementMethod
    Type = if ($null -ne $site) { [string]$site.type } else { "" }
    World = if ($null -ne $site) { [string]$site.world } else { "" }
    X = if ($null -ne $site) { [string]$site.x } else { "" }
    Y = if ($null -ne $site) { [string]$site.y } else { "" }
    Z = if ($null -ne $site) { [string]$site.z } else { "" }
    Radius = if ($null -ne $site) { [string]$site.radius } else { "" }
    Intent = $brief.intent
    PlaceRule = $placeRule
    ProofShots = $brief.proof
    PlanCommand = "/obs site plan $id"
    SurveyCommand = $setupCommand
    AfterSurvey = After-Survey $id
  }) | Out-Null
}

$csvPath = Join-Path $packetFull "launch-sites.csv"
$rows | Export-Csv -LiteralPath $csvPath -NoTypeInformation -Encoding UTF8

$captureRows = foreach ($row in $rows) {
  $isGenerated = $row.PlacementMethod -eq "Deep Hold generated"
  [pscustomobject]@{
    Order = $row.Order
    Lane = $row.Lane
    SiteId = $row.SiteId
    PlacementMethod = $row.PlacementMethod
    GeneratedProof = ""
    PlaceworldReceipt = ""
    ChosenWorld = ""
    X = ""
    Y = ""
    Z = ""
    SurveyedBy = ""
    VisualStatus = "KEEP / RESHAPE / REPLACE / CUT"
    ApproachShot = ""
    FocalShot = ""
    ActionShot = ""
    ExitShot = ""
    CohesionNotes = ""
    Notes = if ($isGenerated) {
      "GeneratedProof: paste the /obs placehold audit summary and room-proof note here, then move actual proof into GeneratedProof."
    } elseif ($row.PlacementMethod -eq "Dimension survey") {
      if ($row.SiteId -eq "nether_forge") { "PlaceworldReceipt must mention nether_forge_placed after stamping in the Nether." }
      elseif ($row.SiteId -eq "end_seventh_shrine") { "PlaceworldReceipt must mention end_seventh_shrine_placed after stamping in the End." }
      else { "PlaceworldReceipt must name the dimension stamp receipt." }
    } elseif ($row.PlacementMethod -eq "Bespoke survey + placeworld") {
      "PlaceworldReceipt: paste the stamped/occupied receipt from chat or /obs site launch."
    } else {
      ""
    }
  }
}
$captureRows | Export-Csv -LiteralPath (Join-Path $packetFull "coords-capture.csv") -NoTypeInformation -Encoding UTF8

$todoCount = @($rows | Where-Object { $_.Status -eq "TODO" }).Count
$stampPendingCount = @($rows | Where-Object { $_.Status -eq "SURVEYED_STAMP_PENDING" }).Count
$generatedPendingCount = @($rows | Where-Object { $_.Status -eq "GENERATED_PROOF_PENDING" }).Count
$coordsCapturedCount = @($rows | Where-Object { $_.Status -eq "COORDS_CAPTURED" }).Count
$md = [System.Collections.Generic.List[string]]::new()
$md.Add("# The Observance Launch Placement Packet - $Date")
$md.Add("")
$md.Add("This packet turns the in-game ``/obs site plan`` route into an external placement worksheet.")
$md.Add("It does not replace the server commands; it keeps the director from losing lane intent, proof shots, and coordinates during the sprint.")
$md.Add("")
$md.Add("Remaining launch proof rows: $todoCount todo, $stampPendingCount surveyed-stamp-pending, $generatedPendingCount generated-proof-pending. Coordinate/proof state: $coordsCapturedCount coord-captured.")
$md.Add("")
$md.Add("## Launch Receipt")
$md.Add("")
$md.Add("Record these receipts with the completed coordinate/proof packet so the placement pass can be matched to the exact launch build.")
$md.Add("")
$md.Add("- Supabase SQL: ``$(Get-RelativePath $applyAllSqlFile)``")
$md.Add("- Apply-all SHA1: ``$(Get-FileSha1 $applyAllSqlFile)``")
$md.Add("- Deploy manifest: ``$(Get-RelativePath $deployManifestFile)``")
$md.Add("- Plugin jar: ``$(if ($null -eq $pluginJarFile) { 'MISSING' } else { Get-RelativePath $pluginJarFile })``")
$md.Add("- Plugin jar SHA1: ``$(if ($null -eq $pluginJarFile) { 'MISSING' } else { Get-FileSha1 $pluginJarFile })``")
$md.Add("- Datapack zip: ``$(Get-RelativePath $datapackZipFile)``")
$md.Add("- Datapack SHA1: ``$(Get-FileSha1 $datapackZipFile)``")
$md.Add("- Resource pack zip: ``$(Get-RelativePath $resourcepackZipFile)``")
$md.Add("- Resource pack SHA1: ``$(Get-FileSha1 $resourcepackZipFile)``")
$md.Add("- Hosted resource pack: configure ``resource-pack.url`` with ``tools\set_resource_pack_config.ps1`` and verify the live HTTPS zip with ``tools\check_hosted_resource_pack.ps1`` before players join.")
$md.Add("- Rehearsal receipt: the same run should have ``friend-launch-quickstart.md``, ``launch-blockers.md``, ``manual-media-checklist.md``, ``supabase-apply-card.md``, ``live-server-command-sheet.md``, ``friend-launch-todo.md``, and ``launch-attestations.md`` from ``tools\prepare_friend_launch.ps1`` / ``tools\new_rehearsal_packet.ps1``.")
$md.Add("")
$md.Add("## How To Use")
$md.Add("")
$md.Add("1. Run ``/obs site todo`` in game.")
$md.Add("2. Run ``/obs placehold build`` and ``/obs placehold audit`` before surveying individual underground rooms; Hold-owned rows are generated together and need generated-room proof, not hand-surveyed static coordinates.")
$md.Add("3. Run ``/obs site plan lanes`` and choose one outside-Hold lane to finish before opening the next.")
$md.Add("4. For each TODO row in that lane, run ``/obs site next <lane>`` or ``/obs site plan <siteId>`` and read the intent/place/proof brief.")
$md.Add("5. If the row's PlacementMethod is ``Deep Hold generated``, proof the generated room in place instead of running ``/obs site set``. Otherwise stand at the chosen real anchor and run the row's SurveyCommand.")
$md.Add("6. Follow the row's after-survey instruction, usually ``/obs placeworld`` for bespoke or dimension sites.")
$md.Add("7. Capture approach, focal, action/answer, and exit screenshots. For Deep Hold generated rows, fill ``GeneratedProof`` with the ``/obs placehold audit`` result and the generated room proof note.")
$md.Add("8. For rows whose PlacementMethod includes ``placeworld`` or ``Dimension survey``, fill ``PlaceworldReceipt`` after the stamp. Dimension rows must name ``nether_forge_placed`` or ``end_seventh_shrine_placed``.")
$md.Add("9. Mark visual status only after checking silhouette, palette, lighting, body verb, and action/answer legibility.")
$md.Add("10. Fill the four proof-shot columns: approach, focal object, player action/answer surface, and exit/aftertaste.")
$md.Add("11. Fill ``CohesionNotes`` with the spacing/route reason: why this anchor belongs here and what nearby anchor it speaks to.")
$md.Add("12. Run ``tools\check_launch_coord_quality.ps1 -CaptureCsv <packet>\coords-capture.csv`` early and often; for the real launch pass, add ``-Launch``.")
$md.Add("13. If you are applying coordinates from the worksheet instead of relying on ``/obs site set`` persistence, preview with ``tools\apply_launch_coords.ps1 -CaptureCsv <packet>\coords-capture.csv``. Only after the preview is clean, rerun with ``-Apply``.")
$md.Add("14. Finish with ``/obs preflight``, then ``tools\check_world_build_readiness.ps1 -Launch`` and a live rehearsal packet.")
$md.Add("")
$md.Add("## Files")
$md.Add("")
$md.Add("- ``launch-sites.csv`` - current site state plus command/proof brief.")
$md.Add("- ``coords-capture.csv`` - capture sheet for outside-Hold chosen coordinates plus Deep Hold generated-room proof, placeworld receipts, placement lane, visual verdicts, four proof shots, and cohesion notes.")
$md.Add("- ``tools\check_launch_coord_quality.ps1`` - worksheet quality gate for duplicate outside-Hold anchors, wrong dimensions, generated Hold proof, placeworld receipts, spacing, route cohesion, KEEP verdicts, and proof shots.")
$md.Add("- ``tools\apply_launch_coords.ps1`` - guarded worksheet-to-``sites.yml`` updater; refuses non-KEEP visual verdicts unless explicitly overridden.")
$md.Add("")
$md.Add("## Placement Lanes")
$md.Add("")
foreach ($lane in $placementLanes) {
  $laneRows = @($rows | Where-Object { $_.Lane -eq $lane.id })
  $laneReady = @($laneRows | Where-Object { $_.Status -eq "COORDS_CAPTURED" }).Count
  $laneStampPending = @($laneRows | Where-Object { $_.Status -eq "SURVEYED_STAMP_PENDING" }).Count
  $laneGeneratedPending = @($laneRows | Where-Object { $_.Status -eq "GENERATED_PROOF_PENDING" }).Count
  $md.Add("### $($lane.id) - $($lane.label)")
  $md.Add("")
  $md.Add("Coord-captured: $laneReady/$($laneRows.Count); stamp-pending: $laneStampPending; generated-proof-pending: $laneGeneratedPending. Work this as one coherent pass with ``/obs site next $($lane.id)``.")
  $md.Add("")
  foreach ($row in $laneRows) {
    $md.Add("#### $($row.Order). ``$($row.SiteId)`` - $($row.Status)")
    $md.Add("")
    $md.Add("- type/world/radius: ``$($row.Type)`` / ``$($row.World)`` / ``$($row.Radius)``")
    $md.Add("- current coords: ``$($row.X),$($row.Y),$($row.Z)``")
    $md.Add("- placement method: $($row.PlacementMethod)")
    $md.Add("- intent: $($row.Intent)")
    $md.Add("- place: $($row.PlaceRule)")
    $md.Add("- proof shots: $($row.ProofShots)")
    $md.Add("- commands: ``$($row.PlanCommand)`` -> ``$($row.SurveyCommand)``")
    $md.Add("- after survey: $($row.AfterSurvey)")
    $md.Add("- visual proof: silhouette / palette / lighting / body verb / action-answer legibility")
    $md.Add("")
    $md.Add("Checklist:")
    $md.Add("- [ ] Intent still fits the chosen terrain.")
    if ($row.PlacementMethod -eq "Deep Hold generated") {
      $md.Add("- [ ] GeneratedProof captured in ``coords-capture.csv`` with clean ``/obs placehold audit`` result.")
    } else {
      $md.Add("- [ ] Coordinate captured in ``coords-capture.csv``.")
    }
    $md.Add("- [ ] Survey command run in game.")
    if ($row.PlacementMethod -eq "Dimension survey" -or $row.PlacementMethod -eq "Bespoke survey + placeworld") {
      $md.Add("- [ ] PlaceworldReceipt captured in ``coords-capture.csv`` after the stamp.")
    }
    $md.Add("- [ ] After-survey command run if needed.")
    $md.Add("- [ ] Four proof screenshots captured.")
    $md.Add("- [ ] Visual status is KEEP.")
    $md.Add("")
  }
  $md.Add("")
}

[System.IO.File]::WriteAllLines((Join-Path $packetFull "00-placement.md"), $md, [System.Text.UTF8Encoding]::new($false))

Write-Host "launch placement packet created: $packetFull"
Write-Host "remaining launch proof rows: $todoCount todo, $stampPendingCount stamp-pending, $generatedPendingCount generated-proof-pending"
