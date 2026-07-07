param(
  [string]$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
)

$ErrorActionPreference = "Stop"

$pluginFile = Join-Path $RepoRoot "plugin\src\main\java\com\observance\watcher\ObservancePlugin.java"
$commandFile = Join-Path $RepoRoot "plugin\src\main\java\com\observance\watcher\command\ObservanceCommand.java"
$townsfolkFile = Join-Path $RepoRoot "plugin\src\main\java\com\observance\watcher\signal\listener\TownsfolkNpcListener.java"
$paintedLineFile = Join-Path $RepoRoot "plugin\src\main\java\com\observance\watcher\signal\listener\PaintedLineListener.java"
$customComplianceFile = Join-Path $RepoRoot "plugin\src\main\java\com\observance\watcher\signal\listener\CustomComplianceListener.java"
$darkHoursFile = Join-Path $RepoRoot "plugin\src\main\java\com\observance\watcher\signal\listener\DarkHoursListener.java"
$locationSamplerFile = Join-Path $RepoRoot "plugin\src\main\java\com\observance\watcher\signal\LocationSampler.java"
$sitesFile = Join-Path $RepoRoot "plugin\src\main\resources\sites.yml"
$configFile = Join-Path $RepoRoot "plugin\src\main\resources\config.yml"
$dialogueDocFile = Join-Path $RepoRoot "design\DIALOGUE-WORLD-AUDIT.md"
$travelDestFile = Join-Path $RepoRoot "design\content\travel-destinations.md"
$sideQuestFile = Join-Path $RepoRoot "discord\supabase\seeds\side_quests.sql"
$archiveVoiceFile = Join-Path $RepoRoot "discord\src\voice.archive.ts"

foreach ($file in @($pluginFile, $commandFile, $townsfolkFile, $paintedLineFile, $customComplianceFile, $darkHoursFile, $locationSamplerFile, $sitesFile, $configFile, $dialogueDocFile, $travelDestFile, $sideQuestFile, $archiveVoiceFile)) {
  if (-not (Test-Path $file)) {
    throw "dialogue contract check: missing required file: $file"
  }
}

$plugin = Get-Content -LiteralPath $pluginFile -Raw
$command = Get-Content -LiteralPath $commandFile -Raw
$townsfolk = Get-Content -LiteralPath $townsfolkFile -Raw
$paintedLine = Get-Content -LiteralPath $paintedLineFile -Raw
$customCompliance = Get-Content -LiteralPath $customComplianceFile -Raw
$darkHours = Get-Content -LiteralPath $darkHoursFile -Raw
$locationSampler = Get-Content -LiteralPath $locationSamplerFile -Raw
$sites = Get-Content -LiteralPath $sitesFile -Raw
$config = Get-Content -LiteralPath $configFile -Raw
$dialogueDoc = Get-Content -LiteralPath $dialogueDocFile -Raw
$travelDest = Get-Content -LiteralPath $travelDestFile -Raw
$sideQuests = Get-Content -LiteralPath $sideQuestFile -Raw
$archiveVoice = Get-Content -LiteralPath $archiveVoiceFile -Raw

$failures = [System.Collections.Generic.List[string]]::new()

function Fail([string] $message) {
  $script:failures.Add($message)
}

function RequireContains([string] $label, [string] $text, [string] $needle) {
  if (-not $text.Contains($needle)) {
    Fail "$label missing expected text: $needle"
  }
}

function RequireRegex([string] $label, [string] $text, [string] $pattern) {
  if (-not [regex]::IsMatch($text, $pattern, [System.Text.RegularExpressions.RegexOptions]::Singleline)) {
    Fail "$label missing expected pattern: $pattern"
  }
}

function SiteBody([string] $id) {
  $pattern = "(?ms)^\s{2}$([regex]::Escape($id)):\s*(?<body>.*?)(?=^\s{2}[A-Za-z0-9_-]+:\s*(?:#.*)?$|\z)"
  $m = [regex]::Match($sites, $pattern)
  if (-not $m.Success) {
    Fail "site '$id' is missing from sites.yml"
    return $null
  }
  return $m.Groups["body"].Value
}

function RequireSite([string] $id, [string] $type) {
  $body = SiteBody $id
  if ($null -eq $body) { return }
  $typePattern = '(?m)^\s*type:\s*"' + [regex]::Escape($type) + '"\s*(?:#.*)?$'
  if (-not [regex]::IsMatch($body, $typePattern)) {
    Fail "site '$id' must have type '$type'"
  }
  if (-not [regex]::IsMatch($body, '(?m)^\s*enabled:\s*true\s*(?:#.*)?$')) {
    Fail "site '$id' must be enabled for rehearsal/launch proof"
  }
}

function RequireDialogueKeys([string] $contract, [string[]] $keys) {
  foreach ($key in $keys) {
    RequireContains "$contract dialogue key" $townsfolk ('"' + $key + '"')
  }
}

# Contract 0: archive-authored townsfolk reaction texture must actually reach Minecraft.
RequireDialogueKeys "surface conduct texture" @(
  "aro.greet.again",
  "aro.greet.warm",
  "aro.greet.cold",
  "aro.greet.iss_cold",
  "wenna.greet.again",
  "wenna.greet.warm",
  "wenna.greet.cold",
  "coll.greet.warm",
  "coll.greet.cold",
  "old-pell.greet.warm",
  "old-pell.greet.cold",
  "dob.react.bad"
)
RequireContains "surface conduct texture archive" $archiveVoice "'aro.greet.iss_cold'"
RequireContains "surface conduct texture archive" $archiveVoice "'wenna.greet.warm'"
RequireContains "surface conduct texture archive" $archiveVoice "'coll.greet.cold'"
RequireContains "surface conduct texture arc echo" $townsfolk 'String issEchoKey = id + ".greet.iss_cold";'

# Contract 1: Aro/Coll/Dob's painted-line claims must point to a real route and a real consequence.
RequireDialogueKeys "painted-line" @(
  "aro.rumor.line",
  "aro.lie.cross",
  "coll.truth.line",
  "dob.chatter.line"
)
RequireSite "lampworks_stair" "lampworks_stair"
RequireSite "painted_line" "painted_line"
RequireContains "painted-line staging" $command "placeDescentProofChain"
RequireContains "painted-line fixture" $command "placePaintedLineFixture"
RequireContains "painted-line visit coverage" $command '"painted_line"'
RequireContains "painted-line flag constant" $paintedLine 'FLAG_CROSSED = "painted_line_crossed"'
RequireContains "painted-line typed site search" $paintedLine 'nearestPlacedOfType(sites, LINE_TYPE'
RequireContains "painted-line private cue" $paintedLine "playPressureCue(player)"
RequireContains "painted-line durable flag" $paintedLine "supabase.mergeArcFlags(flags)"
RequireContains "painted-line listener registration" $plugin "new com.observance.watcher.signal.listener.PaintedLineListener"
RequireRegex "painted-line config" $config '(?ms)^\s{2}painted-line:\s*\r?\n\s{4}enabled:\s*true\b'

# Contract 2: Coll's third-lamp errand must have a countable stand and a physical light interaction.
RequireDialogueKeys "third-lamp" @(
  "coll.rumor.lampworks",
  "coll.quest.offer",
  "coll.quest.done"
)
RequireSite "third_lamp_stand" "lamp_stand"
RequireContains "third-lamp fixture" $command "placeLampStand"
RequireRegex "third-lamp quest mapping" $townsfolk '"coll\.quest\.offer",\s*new Quest\("coll_lamp",\s*"coll\.quest\.offer",\s*"coll\.quest\.done",\s*"third_lamp_stand",\s*false\)'
RequireContains "third-lamp block-place hook" $townsfolk "onThirdLampLit"
RequireContains "third-lamp interact hook" $townsfolk "onThirdLampInteract"
RequireContains "third-lamp completion" $townsfolk 'completeQuestByKey(player, "coll_lamp"'

# Contract 3: Wenna's crust/dead-stall errand must have a memorable site and a drop action payoff.
RequireDialogueKeys "dead-stall" @(
  "wenna.quest.offer",
  "wenna.quest.done"
)
RequireSite "dead_stall" "dead_stall"
RequireContains "dead-stall fixture" $command "buildDeadStall"
RequireRegex "dead-stall quest mapping" $townsfolk '"wenna\.quest\.offer",\s*new Quest\("wenna_crust",\s*"wenna\.quest\.offer",\s*"wenna\.quest\.done",\s*"dead_stall",\s*false\)'
RequireContains "dead-stall drop hook" $townsfolk "onCrustDrop"
RequireContains "dead-stall crust filter" $townsfolk "isCrust"
RequireContains "dead-stall completion" $townsfolk 'completeQuestByKey(player, "wenna_crust"'

# Contract 3b: Aro's deep-bird/coops rumor must point to a visible place and an archive/side payoff.
RequireDialogueKeys "bird-coops" @(
  "aro.rumor.bird"
)
RequireSite "deep_bird_coops" "bird_coops"
RequireContains "bird-coops fixture" $command "buildBirdCoops"
RequireContains "bird-coops route registration" $command '"deep_bird_coops", "bird_coops"'
RequireContains "bird-coops launch list" $command '"deep_bird_coops"'
RequireContains "bird-coops coverage" $command '"deep_bird_coops"'
RequireContains "bird-coops visual audit" $command '"bird_coops"'
RequireContains "bird-coops visual proof" $command "expected visible cage bars"
RequireContains "bird-coops sidequest" $sideQuests "'dest-bird-coops'"
RequireContains "bird-coops sidequest reward" $sideQuests "the_sacred_beast; seeds deep-bird vigil"
RequireContains "bird-coops archive payoff" $archiveVoice "'voice.dest.coops.find'"

# Contract 3c: Sella's far-water side path must be a built mirror/count destination, not a generic marker.
RequireSite "the_far_water" "far_water"
RequireContains "far-water fixture" $command "placeFarWater"
RequireContains "far-water compact staging" $command "placeFarWaterProof"
RequireContains "far-water launch list" $command '"the_far_water"'
RequireContains "far-water coverage" $command '"the_far_water"'
RequireContains "far-water visual audit" $command '"far_water"'
RequireContains "far-water visual proof" $command "expected far-water mirror pool, count stones, copybook shelf, and shoreline signs"
RequireContains "far-water sidequest" $sideQuests "'dest-far-water'"
RequireContains "far-water sidequest reward" $sideQuests "KEYED (face-the-water mirror): Sella''s copybook drawings"
RequireContains "far-water travel spec find" $travelDest 'voice.dest.farWater.find'
RequireContains "far-water travel spec mirror" $travelDest 'voice.dest.farWater.mirror'
RequireContains "far-water archive find" $archiveVoice "'voice.dest.farWater.find'"
RequireContains "far-water archive mirror" $archiveVoice "'voice.dest.farWater.mirror'"

# Contract 3d: Ambient/rumored side destinations must have built proof, not just travel-copy payoffs.
RequireSite "school_stand" "school_stand"
RequireContains "school-stand fixture" $command "buildSchoolStand"
RequireContains "school-stand compact staging" $command "placeSchoolStandProof"
RequireContains "school-stand launch list" $command '"school_stand"'
RequireContains "school-stand coverage" $command '"school_stand"'
RequireContains "school-stand visual audit" $command '"school_stand"'
RequireContains "school-stand visual proof" $command "expected school slate, copybook shelf, six stones, grey seventh marker"
RequireContains "school-stand sidequest" $sideQuests "'dest-school-stand'"
RequireContains "school-stand sidequest reward" $sideQuests "domestic were-they-human detail"
RequireContains "school-stand travel spec" $travelDest 'voice.dest.school.find'
RequireContains "school-stand archive find" $archiveVoice "'voice.dest.school.find'"

RequireSite "markers_row" "markers_row"
RequireContains "markers-row fixture" $command "buildMarkersRow"
RequireContains "markers-row compact staging" $command "placeMarkersRowProof"
RequireContains "markers-row launch list" $command '"markers_row"'
RequireContains "markers-row coverage" $command '"markers_row"'
RequireContains "markers-row visual audit" $command '"markers_row"'
RequireContains "markers-row visual proof" $command "expected six bow-stones, worn bow marks, grey seventh hollow"
RequireContains "markers-row sidequest" $sideQuests "'dest-markers-row'"
RequireContains "markers-row sidequest reward" $sideQuests "the_bow taught; seventh-mark surplus"
RequireContains "markers-row travel spec" $travelDest 'voice.dest.markers.find'
RequireContains "markers-row archive find" $archiveVoice "'voice.dest.markers.find'"

RequireSite "cistern_7" "cistern_7"
RequireContains "cistern-7 fixture" $command "buildCisternSeven"
RequireContains "cistern-7 compact staging" $command "placeCisternProof"
RequireContains "cistern-7 launch list" $command '"cistern_7"'
RequireContains "cistern-7 coverage" $command '"cistern_7"'
RequireContains "cistern-7 visual audit" $command '"cistern_7"'
RequireContains "cistern-7 visual proof" $command "expected black water, pale arch, good-oil jars"
RequireContains "cistern-7 sidequest" $sideQuests "'dest-cistern-7'"
RequireContains "cistern-7 sidequest reward" $sideQuests "the lamp-in-water-lies spook"
RequireContains "cistern-7 travel spec" $travelDest 'voice.dest.cistern.find'
RequireContains "cistern-7 archive find" $archiveVoice "'voice.dest.cistern.find'"

RequireSite "watch_floor" "watch_floor"
RequireContains "watch-floor fixture" $command "buildWatchFloor"
RequireContains "watch-floor compact staging" $command "placeWatchFloorProof"
RequireContains "watch-floor launch list" $command '"watch_floor"'
RequireContains "watch-floor coverage" $command '"watch_floor"'
RequireContains "watch-floor visual audit" $command '"watch_floor"'
RequireContains "watch-floor visual proof" $command "expected watch-log lectern, black-moon lights, finished-log signs"
RequireContains "watch-floor sidequest" $sideQuests "'dest-watch-floor'"
RequireContains "watch-floor sidequest reward" $sideQuests "the_dark_hours; proves the watch ended"
RequireContains "watch-floor travel spec" $travelDest 'voice.dest.watchFloor.find'
RequireContains "watch-floor archive find" $archiveVoice "'voice.dest.watchFloor.find'"

# Contract 3e: Entry five, Orin's seal, and the way-up rumor must have physical side destinations.
RequireSite "set_apart_shelf" "set_apart_shelf"
RequireContains "set-apart fixture" $command "buildSetApartShelf"
RequireContains "set-apart compact staging" $command "placeSetApartProof"
RequireContains "set-apart launch list" $command '"set_apart_shelf"'
RequireContains "set-apart visual proof" $command "expected entry-5 shelf"
RequireContains "set-apart sidequest" $sideQuests "'dest-set-apart'"
RequireContains "set-apart travel spec" $travelDest 'voice.dest.setApart.find'
RequireContains "set-apart archive find" $archiveVoice "'voice.dest.setApart.find'"

RequireSite "undercroft_seal" "undercroft_seal"
RequireContains "undercroft-seal fixture" $command "buildUndercroftSeal"
RequireContains "undercroft-seal compact staging" $command "placeUndercroftSealProof"
RequireContains "undercroft-seal launch list" $command '"undercroft_seal"'
RequireContains "undercroft-seal visual proof" $command "expected sealed door"
RequireContains "undercroft-seal sidequest" $sideQuests "'dest-undercroft-seal'"
RequireContains "undercroft-seal travel spec" $travelDest 'voice.dest.undercroftSeal.find'
RequireContains "undercroft-seal archive find" $archiveVoice "'voice.dest.undercroftSeal.find'"

RequireSite "forgotten_mouth" "forgotten_mouth"
RequireContains "forgotten-mouth fixture" $command "buildForgottenMouth"
RequireContains "forgotten-mouth compact staging" $command "placeForgottenMouthProof"
RequireContains "forgotten-mouth launch list" $command '"forgotten_mouth"'
RequireContains "forgotten-mouth visual proof" $command "expected true way-up mouth"
RequireContains "forgotten-mouth sidequest" $sideQuests "'dest-way-up'"
RequireContains "forgotten-mouth travel spec" $travelDest 'voice.dest.wayUp.find'
RequireContains "forgotten-mouth archive find" $archiveVoice "'voice.dest.wayUp.find'"

# Contract 3f: Deep civic side destinations must connect to lore value instead of empty scenery.
RequireSite "deep_market" "deep_market"
RequireContains "deep-market fixture" $command "buildDeepMarket"
RequireContains "deep-market compact staging" $command "placeDeepMarketProof"
RequireContains "deep-market launch list" $command '"deep_market"'
RequireContains "deep-market coverage" $command '"deep_market"'
RequireContains "deep-market visual audit" $command '"deep_market"'
RequireContains "deep-market visual proof" $command "expected market stalls, lectern-shelf books, and market board"
RequireContains "deep-market sidequest" $sideQuests "'dest-deep-market'"
RequireContains "deep-market sidequest reward" $sideQuests "the warmth they grieve"
RequireContains "deep-market travel spec" $travelDest 'voice.dest.market.find'
RequireContains "deep-market archive find" $archiveVoice "'voice.dest.market.find'"

RequireSite "ration_table" "ration_table"
RequireContains "ration-table fixture" $command "buildRationTable"
RequireContains "ration-table compact staging" $command "placeRationTableProof"
RequireContains "ration-table launch list" $command '"ration_table"'
RequireContains "ration-table coverage" $command '"ration_table"'
RequireContains "ration-table visual audit" $command '"ration_table"'
RequireContains "ration-table visual proof" $command "expected ration table, half-loaf marker, and R14/child-line signs"
RequireContains "ration-table sidequest" $sideQuests "'dest-ration-table'"
RequireContains "ration-table sidequest reward" $sideQuests "were-they-human hottest"
RequireContains "ration-table travel spec" $travelDest 'voice.dest.rationTable.find'
RequireContains "ration-table archive find" $archiveVoice "'voice.dest.rationTable.find'"

RequireSite "third_bay_breach" "third_bay_breach"
RequireContains "third-bay fixture" $command "buildThirdBayBreach"
RequireContains "third-bay compact staging" $command "placeThirdBayProof"
RequireContains "third-bay launch list" $command '"third_bay_breach"'
RequireContains "third-bay coverage" $command '"third_bay_breach"'
RequireContains "third-bay visual audit" $command '"third_bay_breach"'
RequireContains "third-bay visual proof" $command "expected broken Deep Line, downward breach, and third-bay warning signs"
RequireContains "third-bay sidequest" $sideQuests "'dest-third-bay'"
RequireContains "third-bay sidequest reward" $sideQuests "DANGEROUS PROOF"
RequireContains "third-bay travel spec" $travelDest 'voice.dest.thirdBay.find'
RequireContains "third-bay archive find" $archiveVoice "'voice.dest.thirdBay.find'"

# Contract 3g: Aro's warm-town lie must be a physical contradiction, not a wasted walk.
RequireDialogueKeys "warm-town false lead" @(
  "aro.rumor.town"
)
RequireSite "warm_town_collapse" "warm_town_collapse"
RequireContains "warm-town fixture" $command "buildWarmTownCollapse"
RequireContains "warm-town compact staging" $command "placeWarmTownProof"
RequireContains "warm-town launch list" $command '"warm_town_collapse"'
RequireContains "warm-town coverage" $command '"warm_town_collapse"'
RequireContains "warm-town visual audit" $command '"warm_town_collapse"'
RequireContains "warm-town visual proof" $command "expected collapse rubble and WARDEN-3 notice"
RequireContains "warm-town sidequest" $sideQuests "'dest-warm-town'"
RequireContains "warm-town sidequest reward" $sideQuests "FALSE LEAD WITH TEETH"
RequireContains "warm-town travel spec" $travelDest 'voice.dest.warmTown.find'

# Contract 4: Wenna's bowing-stones line must teach a measurable in-world custom.
RequireDialogueKeys "bowing-stones" @("wenna.truth.bow")
RequireSite "bow_marker_01" "bow_marker"
RequireContains "bow custom listener registration" $plugin "new CustomComplianceListener"
RequireContains "bow listener event" $customCompliance "PlayerToggleSneakEvent"
RequireContains "bow typed site" $customCompliance 'TYPE_BOW_MARKER = "bow_marker"'
RequireContains "bow honored custom" $customCompliance "TrackerConfig.CUSTOM_BOW"
RequireContains "bow site in rehearsal route" $command '"bow_marker_01"'

# Contract 5: Wenna/Aro's black-moon sleep lines must connect to the Dark Hours tracker.
RequireDialogueKeys "black-moon sleep" @(
  "wenna.rumor.moon",
  "aro.lie.moon"
)
RequireRegex "dark-hours config" $config '(?ms)^\s{2}dark-hours:\s*\r?\n\s{4}enabled:\s*true\b'
RequireContains "dark-hours listener registration" $plugin "new DarkHoursListener"
RequireContains "dark-hours bed event" $darkHours "PlayerBedEnterEvent"
RequireContains "dark-hours taboo phase" $darkHours "isTabooMoonPhase"
RequireContains "dark-hours violation custom" $darkHours "TrackerConfig.CUSTOM_DARK_HOURS"

# Contract 6: Wenna/Coll's light lines must connect to a placed light zone and scanner.
RequireDialogueKeys "kept-light" @(
  "wenna.truth.light",
  "coll.truth.twolamps"
)
RequireSite "kept_light_home_01" "kept_light"
RequireContains "kept-light scanner scheduled" $plugin "locationSampler.sampleTick()"
RequireContains "kept-light typed scan" $locationSampler 'sites.placedOfType("kept_light")'
RequireContains "kept-light custom" $locationSampler "TrackerConfig.CUSTOM_KEPT_LIGHT"

# Contract 7: Wenna's give-back teaching must connect to an offering cairn and drop custom.
RequireSite "offering_cairn_01" "offering_cairn"
RequireContains "offering typed site" $customCompliance 'TYPE_OFFERING_CAIRN = "offering_cairn"'
RequireContains "offering drop event" $customCompliance "PlayerDropItemEvent"
RequireContains "offering honored custom" $customCompliance "TrackerConfig.CUSTOM_OFFERING"
RequireContains "offering site in rehearsal route" $command '"offering_cairn_01"'

# Contract 8: The executable guard itself must stay documented as the launch standard.
RequireContains "dialogue audit doc" $dialogueDoc 'No `BUILD`, `WIRE`, `REWRITE`, or `CUT` item is allowed'
RequireContains "dialogue audit doc" $dialogueDoc 'A player crossing the painted line sets `painted_line_crossed`'
RequireContains "dialogue audit doc" $dialogueDoc 'Aro''s bird/coops rumor resolves to `deep_bird_coops`'
RequireContains "dialogue audit doc" $dialogueDoc 'Far-water mirror: `the_far_water`'
RequireContains "dialogue audit doc" $dialogueDoc 'Aro''s warm-town lie resolves to `warm_town_collapse`'
RequireContains "dialogue audit doc" $dialogueDoc 'Set-apart shelf: `set_apart_shelf`'
RequireContains "dialogue audit doc" $dialogueDoc 'Undercroft seal: `undercroft_seal`'
RequireContains "dialogue audit doc" $dialogueDoc 'Forgotten Mouth: `forgotten_mouth`'
RequireContains "dialogue audit doc" $dialogueDoc 'Deep Market: `deep_market`'
RequireContains "dialogue audit doc" $dialogueDoc 'Ration table: `ration_table`'
RequireContains "dialogue audit doc" $dialogueDoc 'Third-bay breach: `third_bay_breach`'

if ($failures.Count -gt 0) {
  foreach ($failure in $failures) {
    [Console]::Error.WriteLine($failure)
  }
  exit 1
}

Write-Host "dialogue contract check: OK - NPC claims and major side destinations have world/mechanic proof"
