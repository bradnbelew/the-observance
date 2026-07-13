param(
  [string]$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
)

$ErrorActionPreference = "Stop"

$failures = [System.Collections.Generic.List[string]]::new()
function Fail([string]$Message) { $script:failures.Add($Message) | Out-Null }
function RequireFile([string]$Path) {
  if (-not (Test-Path -LiteralPath $Path)) {
    Fail "missing file: $Path"
    return ""
  }
  return Get-Content -LiteralPath $Path -Raw
}
function RequireContains([string]$Label, [string]$Text, [string]$Needle) {
  if ($Text.IndexOf($Needle, [System.StringComparison]::OrdinalIgnoreCase) -lt 0) {
    Fail "$Label missing: $Needle"
  }
}
function RequireAbsent([string]$Label, [string]$Text, [string]$Needle) {
  if ($Text.IndexOf($Needle, [System.StringComparison]::OrdinalIgnoreCase) -ge 0) {
    Fail "$Label must not contain: $Needle"
  }
}

$repoFull = [System.IO.Path]::GetFullPath($RepoRoot)
$mediaGuideFile = Join-Path $repoFull "design\MEDIA-GUIDE.md"
$mediaPacketFile = Join-Path $repoFull "design\MANUAL-MEDIA-PACKET.md"
$mediaStagingFile = Join-Path $repoFull "design\MANUAL-MEDIA-STAGING.md"
$contentGuideFile = Join-Path $repoFull "design\CONTENT-GUIDELINE.md"
$manualPlanFile = Join-Path $repoFull "design\MANUAL-LAUNCH-PLAN.md"
$manifestFile = Join-Path $repoFull "design\EXPERIENCE-MANIFEST.md"
$recordPageFile = Join-Path $repoFull "dashboard\src\app\record\[slug]\page.tsx"
$authorPageFile = Join-Path $repoFull "dashboard\src\app\author\page.tsx"
$manualMediaComponentFile = Join-Path $repoFull "dashboard\src\components\author\ManualMediaProgress.tsx"
$webAuditFile = Join-Path $repoFull "discord\src\oracle\web-audit.ts"
$metapuzzleFile = Join-Path $repoFull "discord\supabase\seeds\metapuzzle_seed.sql"
$puzzlesFile = Join-Path $repoFull "discord\supabase\seeds\puzzles_seed.sql"
$launchBlockerFile = Join-Path $repoFull "tools\check_launch_manual_blockers.ps1"
$holdZip = Join-Path $repoFull "dashboard\public\the-hold\the-hold.zip"

$mediaGuide = RequireFile $mediaGuideFile
$mediaPacket = RequireFile $mediaPacketFile
$mediaStaging = RequireFile $mediaStagingFile
$contentGuide = RequireFile $contentGuideFile
$manualPlan = RequireFile $manualPlanFile
$manifest = RequireFile $manifestFile
$recordPage = RequireFile $recordPageFile
$authorPage = RequireFile $authorPageFile
$manualMediaComponent = RequireFile $manualMediaComponentFile
$webAudit = RequireFile $webAuditFile
$metapuzzle = RequireFile $metapuzzleFile
$puzzles = RequireFile $puzzlesFile
$launchBlocker = RequireFile $launchBlockerFile

foreach ($needle in @(
  "B1",
  "Found-footage",
  "produced, hosted on YouTube",
  "B2",
  "the-hold.zip",
  "produced, in dashboard public",
  "B3",
  "hosted on Dropbox",
  "Drive folder",
  "spectrogram",
  "do NOT plant the in-world clue",
  "dashboard/public/the-hold/the-hold.zip"
)) {
  RequireContains "design/MEDIA-GUIDE.md" $mediaGuide $needle
}

foreach ($needle in @(
  "Handoff Format",
  "MANUAL-MEDIA-STAGING.md",
  "produced and local-staged",
  "hosted folder",
  "reachable, but not automatically launch-live",
  "current actionable packet",
  "found footage should be recorded in Minecraft",
  "whisper.ogg",
  "keeper_iss.ogg",
  "Video evidence grammar",
  "Hotbar items are evidence",
  "Video titles and descriptions are evidence",
  "clip_01_prior_base.mp4",
  "ASH-13",
  "media-prior-base",
  "base_check_06.mp4",
  "kept elsewhere",
  "clip_02_far_water_count.mp4",
  "WHERE THE REEDS FOLD BACK",
  "media-far-water",
  "sella counted",
  "shore_copy_unlisted.mp4",
  "clip_03_black_moon_toll.mp4",
  "STAY AWAKE",
  "media-black-moon-toll",
  "watch_floor_9_lit.mp4",
  "clip_04_release_room_late.mp4",
  "SIX RETURN, ONE IS NOT KEPT",
  "media-release-room",
  "room_below_noaudio.mp4",
  "https://youtu.be/du-qp_clP7c",
  "https://youtu.be/iKqvPMHjR74",
  "https://youtu.be/pSPhBYMGIRc",
  "https://youtu.be/DtZizx5QIEs",
  "hosting state: reachable, but not automatically launch-live",
  "I WAS NOT KEPT",
  "spectrogram-key.txt",
  "intentionally omitted",
  "dashboard/public/the-hold/the-hold.zip",
  "8a4986422a4af6c65b47f76c61a1e75421b568d4",
  "host fragments I-IV + common-web ending + service digits 25569; no assembled raw server endpoint",
  "/record/the-record-keeps",
  "spine-recovered-archive requires seventh_suspected and recovered_archive_ready",
  "Google Drive",
  "Audacity spectrogram",
  "unlisted YouTube",
  "Do not make the spectrogram the only proof"
)) {
  RequireContains "design/MANUAL-MEDIA-PACKET.md" $mediaPacket $needle
}

foreach ($needle in @(
  "MANUAL MEDIA STAGING RECEIPT",
  "the-hold.zip",
  "8a4986422a4af6c65b47f76c61a1e75421b568d4",
  "host fragments I-IV + common-web ending + service digits 25569; no assembled raw server endpoint",
  "no README/manifest/spoiler-style entries found",
  "base_check_06.mp4",
  "844c2aaf8fb51836add4b59e81abe4131c8d6d0a",
  "shore_copy_unlisted.mp4",
  "9b979e349c7a0d7497fd0fe76d0450e744dc39d0",
  "watch_floor_9_lit.mp4",
  "9b6552e21ec01e6f046027247a689c8dd78b8ce1",
  "room_below_noaudio.mp4",
  "1cb3e600d3e16e9bb1434fa65ddbdff04f512fbd",
  "recovered-archive-packet.zip",
  "783ecde5685abdb601e4a659fc947c32964f70b3",
  "https://www.dropbox.com/scl/fo/72dz7n8lpa1gtiymtkyjl/AMbzcJsSm0x2_TkUq1Bzkv4",
  "Hosted found footage reachability check: HTTP 200",
  "Hosted archive reachability check: HTTP 200",
  "https://youtu.be/du-qp_clP7c",
  "https://youtu.be/iKqvPMHjR74",
  "https://youtu.be/pSPhBYMGIRc",
  "https://youtu.be/DtZizx5QIEs",
  "operator-checked; flag still dormant",
  "field_audio_03.wav",
  "2003f0151c1ba643c649b5ed0e19d1b31bb68319",
  "I WAS NOT KEPT",
  "spectrogram-key.txt",
  "intentionally omitted",
  "Operator media verification",
  "correct files",
  "Do not flip any media-ready flag automatically",
  "intended story gate"
)) {
  RequireContains "design/MANUAL-MEDIA-STAGING.md" $mediaStaging $needle
}

foreach ($needle in @(
  "Real, named systems only",
  "Self-confirming",
  "Retrace-fair",
  "Never make one artifact the SOLE path",
  "Audio spectrogram",
  "EXIF"
)) {
  RequireContains "design/CONTENT-GUIDELINE.md" $contentGuide $needle
}

foreach ($needle in @(
  "media-required",
  "found footage is produced/local-staged",
  "the-hold.zip",
  "is produced and present",
  "recovered archive/spectrogram is produced/local-staged and hosted on Dropbox",
  "found footage is hosted on YouTube and HTTP-reachable",
  "operator-checked",
  "do not plant the live in-world lure",
  "the-hold.zip",
  "Drive folder",
  "waveform/spectrogram"
)) {
  RequireContains "design/EXPERIENCE-MANIFEST.md" $manifest $needle
}

foreach ($needle in @(
  'const HOLD_ZIP_PUBLIC_PATH = "/the-hold/the-hold.zip"',
  "function holdZipAvailable()",
  "file not yet recovered",
  "hasHoldZip ?"
)) {
  RequireContains "record lure page" $recordPage $needle
}

foreach ($needle in @(
  "ManualMediaProgress",
  "the-hold.zip",
  "ASH-13",
  "where the reeds fold back",
  "stay awake",
  "six return, one is not kept",
  "i was not kept",
  "media_clip_01_ready",
  "media_prior_base_read",
  "recovered_archive_ready",
  "seventh_suspected",
  "no placeholder trails",
  "verify live"
)) {
  RequireContains "author manual media surface" ($authorPage + "`n" + $manualMediaComponent) $needle
}

foreach ($needle in @(
  "dashboard/public/the-hold/the-hold.zip",
  "safely withheld",
  "file not yet recovered",
  "hasHoldZip ?"
)) {
  RequireContains "web-audit.ts" $webAudit $needle
}

foreach ($needle in @(
  "spine-recovered-archive",
  "media-prior-base",
  "media-far-water",
  "media-black-moon-toll",
  "media-release-room",
  "media_clip_01_ready",
  "media_clip_02_ready",
  "media_clip_03_ready",
  "media_clip_04_ready",
  "recovered_archive_ready",
  "seventh_suspected"
)) {
  RequireContains "metapuzzle external archive gate" $metapuzzle $needle
}

if ($metapuzzle -notmatch "(?s)jsonb_build_object\('seventh_suspected',\s*true,\s*'recovered_archive_ready',\s*true\)\s*where puzzle_key = 'spine-recovered-archive'") {
  Fail "spine-recovered-archive must require both seventh_suspected and recovered_archive_ready"
}

foreach ($needle in @(
  "OPTIONAL external surface",
  "spine never depends on it",
  "media-prior-base",
  "ash 13",
  "media-far-water",
  "where the reeds fold back",
  "media-black-moon-toll",
  "stay awake",
  "media-release-room",
  "six return one is not kept",
  "i was not kept",
  "media_spectrogram_read"
)) {
  RequireContains "puzzles_seed spine-recovered-archive" $puzzles $needle
}

foreach ($needle in @(
  "Verify external media choices",
  "MANUAL-MEDIA-STAGING.md",
  "B1 found-footage is produced, hosted on YouTube, reachable, and operator-checked",
  "B2",
  "the-hold.zip",
  'is present at `dashboard/public/the-hold/the-hold.zip`',
  "B3 recovered archive is hosted on Dropbox, reachable, correctly populated, and spectrogram-checked",
  "do not plant the in-world lure clue",
  "missing optional media is either withheld or clearly not planted",
  "ASH-13",
  "where the reeds fold back",
  "stay awake",
  "six return one is not kept",
  "i was not kept"
)) {
  RequireContains "design/MANUAL-LAUNCH-PLAN.md media task" $manualPlan $needle
}

RequireContains "launch blocker external media check" $launchBlocker "check_external_media_readiness.ps1"

if (Test-Path -LiteralPath $holdZip) {
  $zip = Get-Item -LiteralPath $holdZip
  if ($zip.Length -lt 4096) {
    Fail "the-hold.zip exists but is suspiciously small ($($zip.Length) bytes): $holdZip"
  }
} else {
  RequireContains "record lure page absent-zip state" $recordPage "file not yet recovered"
}

RequireAbsent "external media plans" ($mediaGuide + "`n" + $mediaPacket + "`n" + $manualPlan) "plant the in-world clue until the file exists"

if ($failures.Count -gt 0) {
  Write-Host "external media readiness check: FAILED"
  foreach ($failure in $failures) {
    Write-Host "  - $failure"
  }
  exit 1
}

$holdState = if (Test-Path -LiteralPath $holdZip) { "the-hold.zip present" } else { "the-hold.zip absent and withheld" }
Write-Host "external media readiness check: OK - media guide, external archive gate, record lure withholding, and launch handoff are aligned ($holdState)"
