param(
  [string]$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path,
  [string]$PacketDir = "",
  [switch]$AllowOpenItems
)

$ErrorActionPreference = "Stop"

function Resolve-UnderRepo([string] $Root, [string] $Path) {
  if ([System.IO.Path]::IsPathRooted($Path)) {
    return [System.IO.Path]::GetFullPath($Path)
  }
  return [System.IO.Path]::GetFullPath((Join-Path $Root $Path))
}

$repoFull = [System.IO.Path]::GetFullPath($RepoRoot)

if ([string]::IsNullOrWhiteSpace($PacketDir)) {
  $rehearsals = Join-Path $repoFull "rehearsals"
  if (-not (Test-Path $rehearsals)) {
    throw "rehearsal packet check: no PacketDir supplied and no rehearsals/ folder exists"
  }
  $latest = Get-ChildItem -LiteralPath $rehearsals -Directory | Sort-Object Name -Descending | Select-Object -First 1
  if ($null -eq $latest) {
    throw "rehearsal packet check: no PacketDir supplied and rehearsals/ has no run folders"
  }
  $packetFull = $latest.FullName
} else {
  $packetFull = Resolve-UnderRepo $repoFull $PacketDir
}

if (-not $packetFull.StartsWith($repoFull, [System.StringComparison]::OrdinalIgnoreCase)) {
  throw "Refusing to validate rehearsal packet outside repo: $packetFull"
}

if (-not (Test-Path $packetFull)) {
  throw "rehearsal packet check: packet folder not found: $packetFull"
}

$notesFile = Join-Path $packetFull "00-notes.md"
$fixesFile = Join-Path $packetFull "fixes.md"
$screenshotsDir = Join-Path $packetFull "screenshots"
$clipsDir = Join-Path $packetFull "clips"

$failures = [System.Collections.Generic.List[string]]::new()
function Fail([string] $message) {
  $script:failures.Add($message)
}

foreach ($path in @($notesFile, $fixesFile, $screenshotsDir, $clipsDir)) {
  if (-not (Test-Path $path)) {
    Fail "missing required packet path: $path"
  }
}

if ($failures.Count -eq 0) {
  $notes = Get-Content -LiteralPath $notesFile -Raw
  $fixes = Get-Content -LiteralPath $fixesFile -Raw

  if (-not $AllowOpenItems) {
    if ([regex]::IsMatch($notes, '^\s*-\s+\[\s\]', [System.Text.RegularExpressions.RegexOptions]::Multiline)) {
      Fail "00-notes.md still contains unchecked required items; complete them or run with -AllowOpenItems for a draft review"
    }
    if ([regex]::IsMatch($fixes, '\|\s*P0\s*\|\s*\|\s*\|\s*\|\s*\|\s*\[\s\]\s*\|')) {
      Fail "fixes.md still contains the blank template P0 row"
    }
    if ([regex]::IsMatch($fixes, '\[\s\]')) {
      Fail "fixes.md still contains unresolved checklist items"
    }
  }

  foreach ($required in @("Evidence Lanes", "First-Hour Pacing", "Major Site Visual Shots", "Side Path Value Matrix", "NPC/World Contracts", "Puzzle Fairness Matrix", "Scare Review", "Unlit Expedition Proof", "Stop/Launch Decision")) {
    if ($notes.IndexOf($required, [System.StringComparison]::OrdinalIgnoreCase) -lt 0) {
      Fail "00-notes.md missing section: $required"
    }
  }

  $screenshotFiles = @(Get-ChildItem -LiteralPath $screenshotsDir -File -Recurse |
    Where-Object { $_.Name -notmatch '^README\.md$' })
  $clipFiles = @(Get-ChildItem -LiteralPath $clipsDir -File -Recurse |
    Where-Object { $_.Name -notmatch '^README\.md$' })

  $firstHourBeats = @(
    "spawn / join read",
    "first wrongness",
    "first social signal",
    "first meaningful choice",
    "first puzzle action",
    "first side pull",
    "first scare pressure",
    "handoff to live route"
  )

  $firstHourSectionMatch = [regex]::Match($notes, '(?is)##\s+First-Hour Pacing(?<body>.*?)(?=##\s+Major Site Visual Shots|\z)')
  $firstHourSection = if ($firstHourSectionMatch.Success) { $firstHourSectionMatch.Groups["body"].Value } else { "" }
  foreach ($beat in $firstHourBeats) {
    $pattern = '(?is)^\s*-\s+\[[xX ]\]\s+' + [regex]::Escape($beat) + '\s*(?<block>.*?)(?=^\s*-\s+\[[xX ]\]\s+|\z)'
    $match = [regex]::Match($firstHourSection, $pattern, [System.Text.RegularExpressions.RegexOptions]::Multiline)
    if (-not $match.Success) {
      Fail "First-Hour Pacing missing '$beat'"
      continue
    }
    $block = $match.Groups["block"].Value
    $verdictMatch = [regex]::Match($block, '(?im)^\s*verdict:\s*(?<verdict>.+?)\s*$')
    if (-not $verdictMatch.Success) {
      Fail "first-hour beat '$beat' missing verdict line"
    } else {
      $verdict = $verdictMatch.Groups["verdict"].Value.Trim()
      if (-not $AllowOpenItems -and $verdict -ne "KEEP") {
        Fail "first-hour beat '$beat' verdict must be KEEP for launch packet; found '$verdict'"
      }
    }
    foreach ($field in @("timestamp", "player action", "world evidence", "friction", "operator leak", "fix")) {
      $fieldMatch = [regex]::Match($block, '(?im)^\s*' + [regex]::Escape($field) + ':\s*(?<value>.*?)\s*$')
      if (-not $fieldMatch.Success) {
        Fail "first-hour beat '$beat' missing field: $field"
        continue
      }
      $value = $fieldMatch.Groups["value"].Value.Trim()
      if (-not $AllowOpenItems -and (
          [string]::IsNullOrWhiteSpace($value) -or
          $value -match '^audit-placeholder$' -or
          $value -match 'operator explains')) {
        Fail "first-hour beat '$beat' needs concrete pacing proof for: $field"
      }
    }
  }

  $majorSites = @(
    "rune_rosetta",
    "stone_vaun",
    "stone_mara",
    "stone_sella",
    "stone_orin",
    "stone_brann",
    "stone_iss",
    "school_stand",
    "the_far_water",
    "markers_row",
    "cistern_7",
    "watch_floor",
    "set_apart_shelf",
    "undercroft_seal",
    "forgotten_mouth",
    "stone_of_reckoning",
    "the_cold_hearth",
    "unbroken_light",
    "the_threshold",
    "the_unwriting",
    "threshold_vault",
    "nether_forge",
    "end_seventh_shrine",
    "lampworks_stair",
    "third_lamp_stand",
    "painted_line",
    "dead_stall",
    "deep_bird_coops",
    "deep_market",
    "ration_table",
    "third_bay_breach",
    "warm_town_collapse",
    "dread_route_start",
    "dread_route_elsewhere",
    "dread_route_figure",
    "dread_route_exit",
    "unlit_entry",
    "unlit_spawn_mirror",
    "unlit_exit",
    "unlit_house_lamp",
    "unlit_house_cairn",
    "unlit_house_coop",
    "unlit_house_well",
    "unlit_house_watch",
    "unlit_house_warm",
    "unlit_house_threshold",
    "unlit_house_base"
  )

  $visualSectionMatch = [regex]::Match($notes, '(?is)##\s+Major Site Visual Shots(?<body>.*?)(?=##\s+Side Path Value Matrix|\z)')
  $visualSection = if ($visualSectionMatch.Success) { $visualSectionMatch.Groups["body"].Value } else { "" }
  foreach ($site in $majorSites) {
    $pattern = '(?is)`' + [regex]::Escape($site) + '`(?<block>.*?)(?=^\s*-\s+\[[xX ]\]\s+`|\z)'
    $match = [regex]::Match($visualSection, $pattern, [System.Text.RegularExpressions.RegexOptions]::Multiline)
    if (-not $match.Success) {
      Fail "Major Site Visual Shots missing '$site'"
      continue
    }
    $block = $match.Groups["block"].Value
    $statusMatch = [regex]::Match($block, '(?im)^\s*status:\s*(?<status>.+?)\s*$')
    if (-not $statusMatch.Success) {
      Fail "major site '$site' missing visual status line"
    } else {
      $status = $statusMatch.Groups["status"].Value.Trim()
      if (-not $AllowOpenItems -and $status -ne "KEEP") {
        Fail "major site '$site' visual status must be KEEP for launch packet; found '$status'"
      }
    }

    foreach ($field in @("silhouette", "palette", "lighting", "body verb", "action/answer legibility", "evidence", "fix")) {
      $fieldMatch = [regex]::Match($block, '(?im)^\s*' + [regex]::Escape($field) + ':\s*(?<value>.*?)\s*$')
      if (-not $fieldMatch.Success) {
        Fail "major site '$site' missing visual field: $field"
        continue
      }
      $value = $fieldMatch.Groups["value"].Value.Trim()
      if (-not $AllowOpenItems -and [string]::IsNullOrWhiteSpace($value)) {
        Fail "major site '$site' needs concrete visual proof for: $field"
      }
    }
  }

  if (-not $AllowOpenItems) {
    foreach ($site in $majorSites) {
      $siteFiles = @($screenshotFiles | Where-Object { $_.BaseName.IndexOf($site, [System.StringComparison]::OrdinalIgnoreCase) -ge 0 })
      if ($siteFiles.Count -lt 4) {
        Fail "site '$site' needs at least 4 screenshot files named with the site id (approach, focal, action/answer, exit)"
        continue
      }
      foreach ($shot in @("approach", "focal")) {
        if (-not ($siteFiles | Where-Object { $_.BaseName.IndexOf($shot, [System.StringComparison]::OrdinalIgnoreCase) -ge 0 })) {
          Fail "site '$site' missing '$shot' screenshot filename"
        }
      }
      if (-not ($siteFiles | Where-Object {
        $_.BaseName.IndexOf("answer", [System.StringComparison]::OrdinalIgnoreCase) -ge 0 -or
        $_.BaseName.IndexOf("action", [System.StringComparison]::OrdinalIgnoreCase) -ge 0
      })) {
        Fail "site '$site' missing answer/action screenshot filename"
      }
      if (-not ($siteFiles | Where-Object {
        $_.BaseName.IndexOf("exit", [System.StringComparison]::OrdinalIgnoreCase) -ge 0 -or
        $_.BaseName.IndexOf("return", [System.StringComparison]::OrdinalIgnoreCase) -ge 0
      })) {
        Fail "site '$site' missing exit/return screenshot filename"
      }
    }
  }

  $sidePaths = @(
    "school_stand",
    "the_far_water",
    "markers_row",
    "cistern_7",
    "watch_floor",
    "set_apart_shelf",
    "undercroft_seal",
    "forgotten_mouth",
    "deep_bird_coops",
    "deep_market",
    "ration_table",
    "third_bay_breach",
    "warm_town_collapse"
  )

  $puzzleFamilies = @(
    "rune literacy / answer sign",
    "keeper ciphers",
    "side cipher / mirror read",
    "custom actions: bow / offering / kept light / dark hours",
    "NPC errands: third lamp / dead stall",
    "co-op plate / threshold vault",
    "Record web jump / oracle inscription",
    "Accepting rite / Seventh choice",
    "Nether and End deepening lanes"
  )

  $sideSectionMatch = [regex]::Match($notes, '(?is)##\s+Side Path Value Matrix(?<body>.*?)(?=##\s+NPC/World Contracts|\z)')
  $sideSection = if ($sideSectionMatch.Success) { $sideSectionMatch.Groups["body"].Value } else { "" }
  foreach ($site in $sidePaths) {
    $pattern = '(?is)`' + [regex]::Escape($site) + '`(?<block>.*?)(?=^\s*-\s+\[[xX ]\]\s+`|\z)'
    $match = [regex]::Match($sideSection, $pattern, [System.Text.RegularExpressions.RegexOptions]::Multiline)
    if (-not $match.Success) {
      Fail "Side Path Value Matrix missing '$site'"
      continue
    }
    $block = $match.Groups["block"].Value
    $valueMatch = [regex]::Match($block, '(?im)^\s*value:\s*(?<value>.+?)\s*$')
    if (-not $valueMatch.Success) {
      Fail "side path '$site' missing value line"
      continue
    }
    $value = $valueMatch.Groups["value"].Value.Trim()
    if (-not $AllowOpenItems -and (
        [string]::IsNullOrWhiteSpace($value) -or
        $value -match 'belief\s*/\s*dread\s*/\s*confirmation\s*/\s*motif' -or
        $value -match '^audit-placeholder$')) {
      Fail "side path '$site' needs a concrete value: belief change, dread, confirmation, motif, or useful contradiction"
    }
  }

  $fairnessSectionMatch = [regex]::Match($notes, '(?is)##\s+Puzzle Fairness Matrix(?<body>.*?)(?=##\s+Scare Review|\z)')
  $fairnessSection = if ($fairnessSectionMatch.Success) { $fairnessSectionMatch.Groups["body"].Value } else { "" }
  foreach ($family in $puzzleFamilies) {
    $pattern = '(?is)^\s*-\s+\[[xX ]\]\s+' + [regex]::Escape($family) + '\s*(?<block>.*?)(?=^\s*-\s+\[[xX ]\]\s+|\z)'
    $match = [regex]::Match($fairnessSection, $pattern, [System.Text.RegularExpressions.RegexOptions]::Multiline)
    if (-not $match.Success) {
      Fail "Puzzle Fairness Matrix missing '$family'"
      continue
    }
    $block = $match.Groups["block"].Value
    $verdictMatch = [regex]::Match($block, '(?im)^\s*verdict:\s*(?<verdict>.+?)\s*$')
    if (-not $verdictMatch.Success) {
      Fail "puzzle family '$family' missing verdict line"
    } else {
      $verdict = $verdictMatch.Groups["verdict"].Value.Trim()
      if (-not $AllowOpenItems -and $verdict -ne "KEEP") {
        Fail "puzzle family '$family' verdict must be KEEP for launch packet; found '$verdict'"
      }
    }
    foreach ($field in @("first guess", "failed attempt", "retraceable clue", "rescue path", "too easy risk", "impossible risk", "fix")) {
      $fieldMatch = [regex]::Match($block, '(?im)^\s*' + [regex]::Escape($field) + ':\s*(?<value>.*?)\s*$')
      if (-not $fieldMatch.Success) {
        Fail "puzzle family '$family' missing field: $field"
        continue
      }
      $value = $fieldMatch.Groups["value"].Value.Trim()
      if (-not $AllowOpenItems -and (
          [string]::IsNullOrWhiteSpace($value) -or
          $value -match '^audit-placeholder$' -or
          $value -match '^\?$')) {
        Fail "puzzle family '$family' needs concrete fairness proof for: $field"
      }
    }
  }

  $scareSectionMatch = [regex]::Match($notes, '(?is)##\s+Scare Review(?<body>.*?)(?=##\s+Unlit Expedition Proof|\z)')
  $scareSection = if ($scareSectionMatch.Success) { $scareSectionMatch.Groups["body"].Value } else { "" }
  foreach ($scare in @("ambient", "directed", "dread route", "Wren/companion", "Tier-0 implication")) {
    $pattern = '(?is)^\s*-\s+\[[xX ]\]\s+' + [regex]::Escape($scare) + '\s*(?<block>.*?)(?=^\s*-\s+\[[xX ]\]\s+|\z)'
    $match = [regex]::Match($scareSection, $pattern, [System.Text.RegularExpressions.RegexOptions]::Multiline)
    if (-not $match.Success) {
      Fail "Scare Review missing '$scare'"
      continue
    }
    $block = $match.Groups["block"].Value
    $verdictMatch = [regex]::Match($block, '(?im)^\s*verdict:\s*(?<verdict>.+?)\s*$')
    if (-not $verdictMatch.Success) {
      Fail "scare '$scare' missing verdict line"
    } else {
      $verdict = $verdictMatch.Groups["verdict"].Value.Trim()
      if (-not $AllowOpenItems -and $verdict -ne "KEEP") {
        Fail "scare '$scare' verdict must be KEEP for launch packet; found '$verdict'"
      }
    }

    foreach ($field in @("trigger", "lore hook", "body", "source", "restraint", "aftertaste", "fix")) {
      $fieldMatch = [regex]::Match($block, '(?im)^\s*' + [regex]::Escape($field) + ':\s*(?<value>.*?)\s*$')
      if (-not $fieldMatch.Success) {
        Fail "scare '$scare' missing field: $field"
        continue
      }
      $value = $fieldMatch.Groups["value"].Value.Trim()
      if (-not $AllowOpenItems -and (
          [string]::IsNullOrWhiteSpace($value) -or
          $value -match '^audit-placeholder$' -or
          $value -match 'be scared now')) {
        Fail "scare '$scare' needs concrete $field proof tied to lore, source, restraint, and aftertaste"
      }
    }
  }

  $unlitSectionMatch = [regex]::Match($notes, '(?is)##\s+Unlit Expedition Proof(?<body>.*?)(?=##\s+First-Hour Notes|\z)')
  $unlitSection = if ($unlitSectionMatch.Success) { $unlitSectionMatch.Groups["body"].Value } else { "" }
  foreach ($required in @(
    "/obs unlit site entry",
    "/obs unlit site spawn",
    "/obs unlit site exit",
    "/obs unlit buildmode off",
    "/obs unlit darken all",
    "/obs unlit border",
    "/obs unlit audit",
    "/obs unlit ready",
    "/obs unlit pass light",
    "/obs unlit pass stalker",
    "/obs unlit pass extinguish",
    "/obs unlit pass house",
    "/obs unlit pass extract",
    "fixture proof",
    "stray light OK",
    "border OK",
    "borrowed lantern",
    "lantern is broken",
    "failed-cheese",
    "non-linear"
  )) {
    if ($unlitSection.IndexOf($required, [System.StringComparison]::OrdinalIgnoreCase) -lt 0) {
      Fail "Unlit Expedition Proof missing '$required'"
    }
  }

  foreach ($house in @(
    "unlit_house_lamp",
    "unlit_house_cairn",
    "unlit_house_coop",
    "unlit_house_well",
    "unlit_house_watch",
    "unlit_house_warm",
    "unlit_house_threshold",
    "unlit_house_base"
  )) {
    $pattern = '(?is)`' + [regex]::Escape($house) + '`(?<block>.*?)(?=^\s*-\s+\[[xX ]\]\s+`|\z)'
    $match = [regex]::Match($unlitSection, $pattern, [System.Text.RegularExpressions.RegexOptions]::Multiline)
    if (-not $match.Success) {
      Fail "Unlit Expedition Proof missing '$house'"
      continue
    }
    $block = $match.Groups["block"].Value
    $verdictMatch = [regex]::Match($block, '(?im)^\s*verdict:\s*(?<verdict>.+?)\s*$')
    if (-not $verdictMatch.Success) {
      Fail "Unlit house '$house' missing verdict line"
    } else {
      $verdict = $verdictMatch.Groups["verdict"].Value.Trim()
      if (-not $AllowOpenItems -and $verdict -ne "KEEP") {
        Fail "Unlit house '$house' verdict must be KEEP for launch packet; found '$verdict'"
      }
    }
    foreach ($field in @("approach", "borrowed lantern route", "light radius", "clue readable", "exit route", "lantern break / retreat pressure", "failed-cheese attempt", "fixture proof", "fix")) {
      $fieldMatch = [regex]::Match($block, '(?im)^\s*' + [regex]::Escape($field) + ':\s*(?<value>.*?)\s*$')
      if (-not $fieldMatch.Success) {
        Fail "Unlit house '$house' missing field: $field"
        continue
      }
      $value = $fieldMatch.Groups["value"].Value.Trim()
      if (-not $AllowOpenItems -and [string]::IsNullOrWhiteSpace($value)) {
        Fail "Unlit house '$house' needs concrete proof for: $field"
      }
    }
  }

  if (-not $AllowOpenItems) {
    foreach ($clip in @(
      @("first hour", "first[-_ ]?hour"),
      @("ambient scare", "ambient"),
      @("directed scare", "directed"),
      @("dread route", "dread"),
      @("Wren/companion", "wren|companion"),
      @("Tier-0 implication", "tier[-_ ]?0"),
      @("Unlit expedition", "unlit"),
      @("Record/web", "record|web"),
      @("finale", "finale|release")
    )) {
      $label = $clip[0]
      $pattern = $clip[1]
      if (-not ($clipFiles | Where-Object { $_.BaseName -match $pattern })) {
        Fail "missing required clip filename for $label"
      }
    }
  }
}

if ($failures.Count -gt 0) {
  foreach ($failure in $failures) {
    [Console]::Error.WriteLine($failure)
  }
  exit 1
}

if ($AllowOpenItems) {
  Write-Host "rehearsal packet check: OK - draft packet has required sections and site matrices"
} else {
  Write-Host "rehearsal packet check: OK - packet has completed notes plus named visual, scare, Record/web, and finale evidence"
}
