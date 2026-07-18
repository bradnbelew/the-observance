param(
  [string]$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
)

$ErrorActionPreference = "Stop"
$dashboard = Join-Path $RepoRoot "dashboard\src"
$failures = [System.Collections.Generic.List[string]]::new()

function Read-Source([string]$Relative) {
  $path = Join-Path $dashboard $Relative
  if (-not (Test-Path -LiteralPath $path)) {
    $script:failures.Add("missing route/source: $Relative") | Out-Null
    return ""
  }
  return Get-Content -LiteralPath $path -Raw -Encoding utf8
}

function Require([string]$Label, [string]$Text, [string]$Needle) {
  if ($Text.IndexOf($Needle, [System.StringComparison]::Ordinal) -lt 0) {
    $script:failures.Add("$Label missing: $Needle") | Out-Null
  }
}

function Forbid([string]$Label, [string]$Text, [string]$Needle) {
  if ($Text.IndexOf($Needle, [System.StringComparison]::OrdinalIgnoreCase) -ge 0) {
    $script:failures.Add("$Label contains retired/broken copy: $Needle") | Out-Null
  }
}

$publicRoutes = @(
  "app\page.tsx",
  "app\game-servers.php\page.tsx",
  "app\server-list.php\page.tsx",
  "app\server.php\page.tsx",
  "app\announcements.php\page.tsx",
  "app\clientarea.php\page.tsx",
  "app\status\page.tsx",
  "app\support\index.php\page.tsx",
  "app\support\ticket.php\page.tsx",
  "app\community\index.php\page.tsx",
  "app\community\2011\02\08\world-backup\page.tsx",
  "app\not-found.tsx"
)

foreach ($route in $publicRoutes) {
  $source = Read-Source $route
  Require $route $source "LegacyShell"
}

$shell = Read-Source "components\legacy\LegacyShell.tsx"
foreach ($needle in @(
  "Preserved customer-site snapshot",
  "interactive account functions disabled",
  "Billing authentication retired",
  "Powered by WHMCS 5",
  "TCAdmin 1"
)) { Require "LegacyShell" $shell $needle }

$listing = Read-Source "app\server-list.php\page.tsx"
Require "server list" $listing "Directory snapshot:"
Require "server list" $listing 'server.listingLabel ?? `Listing #${server.id}`'
Forbid "server list" $listing "<button type=\"button\">Filter"

$legacyContent = Read-Source "lib\legacy-content.ts"
Require "server directory data" $legacyContent "chi-ret-7f0a"
Require "server directory data" $legacyContent "Docket reference damaged"
Forbid "server directory data" $legacyContent 'id: "1842"'

$serverDetail = Read-Source "app\server.php\page.tsx"
Require "server detail" $serverDetail "removed from directory export"
Require "server detail" $serverDetail "LS02_DOCKET_CIPHER"
Require "server detail" $serverDetail "LS02_TEACHING_STRIP"
Require "server detail" $serverDetail "ServiceDocketForm"
Require "server detail" $serverDetail "v5_ls02_service_1842"
Require "server detail" $serverDetail "/support/ticket.php?id=9137"
Forbid "server detail" $serverDetail "['LS02']"
Forbid "server detail" $serverDetail "answer: '1842'"
Forbid "server detail" $serverDetail "NEXT_PUBLIC_OBSERVANCE_SERVER_ADDRESS"
Forbid "server detail" $serverDetail "snoikerz.com:25569"

$docketAction = Read-Source "app\server.php\actions.ts"
foreach ($needle in @(
  "formData.get('serviceDocket')",
  "isCorrectLs02DocketAnswer",
  "recordV5WebSequence",
  "['LS02']",
  "copperline_service_1842",
  "docket_verified: true"
)) { Require "LS02 dedicated action" $docketAction $needle }
foreach ($forbidden in @("formData.get('node", "formData.get('completion", "SUPABASE_SERVICE_ROLE_KEY")) {
  Forbid "LS02 dedicated action" $docketAction $forbidden
}

$docketForm = Read-Source "app\server.php\ServiceDocketForm.tsx"
Require "LS02 client form" $docketForm "useActionState"
Require "LS02 client form" $docketForm "resolveLs02ServiceDocket"
foreach ($forbidden in @("recordV5WebSequence", "SUPABASE", "1842", "ONE EIGHT FOUR TWO")) {
  Forbid "LS02 client form" $docketForm $forbidden
}

$status = Read-Source "app\status\page.tsx"
Require "status" $status "Retired Minecraft accounts"
Forbid "status" $status "1842"

$support = Read-Source "app\support\index.php\page.tsx"
foreach ($anchor in @("#ftp", "#backups", "#jars", "#packs", "#billing", "#tickets")) {
  Require "support" $support $anchor
}
Forbid "support" $support "<Link href=\"/support/index.php\">Connecting"

$post = Read-Source "app\community\2011\02\08\world-backup\page.tsx"
foreach ($needle in @(
  "Java Edition 1.21.11 world",
  "V5_HOLD_ARCHIVE_DOWNLOAD_PATH",
  "readValidatedV5HoldArchive",
  "hasCampaignEvent('p2.artifact_authenticated')",
  "Download world"
)) { Require "world backup post" $post $needle }
foreach ($retired in @("included text file", "walk north", "first stone stair", "plain mirror page", "It does not contain a playable world", "Do not install this archive into Minecraft")) {
  Forbid "world backup post" $post $retired
}

$openingArchive = Read-Source "app\community\archive.php\page.tsx"
foreach ($needle in @(
  "params.locker === undefined",
  "eventKey: 'p1.attachment_history_restored'",
  "idempotencyKey: 'copperline:p1:service-1842-ticket-9137-history'",
  "hasCampaignEvent('p9.company_biographies_restored')",
  "params.locker === '13'",
  "['A06']",
  "['A07']"
)) { Require "opening/P9 archive split" $openingArchive $needle }
Forbid "opening/P9 archive split" $openingArchive "copperline:p1:service-1842-ticket-9137-locker-13"

$record = Read-Source "app\record\[slug]\page.tsx"
foreach ($needle in @(
  "mirror-site",
  "static user mirror",
  "/home/mkept/public_html/record/",
  "recovery-roster.txt",
  "uploader-note.txt",
  "record-system-header",
  "recordsrv/0.9",
  "record-error-site",
  "RuneGlyphs",
  "v5_ls03_directory_trail"
)) { Require "record slug" $record $needle }
Forbid "record slug" $record "mirror fragment / uploader copy"

$archive = Read-Source "app\record\archive\page.tsx"
$terminal = Read-Source "app\record\terminal\page.tsx"
foreach ($pair in @(@("archive", $archive), @("terminal", $terminal))) {
  Require $pair[0] $pair[1] "record-system-header"
  Require $pair[0] $pair[1] "recordsrv/0.9"
  Require $pair[0] $pair[1] "RuneGlyphs"
}

$bareRecord = Read-Source "app\record\page.tsx"
Require "bare record" $bareRecord "400: record key required"

$login = Read-Source "app\author\login\page.tsx"
$author = Read-Source "app\author\page.tsx"
Require "operator login" $login "Operator sign-in"
Require "operator login" $login "configured operator allowlist"
Require "operator console" $author "Observance Operations Console"

$layout = Read-Source "app\layout.tsx"
Require "root metadata" $layout "Copperline Hosting Archive"
Forbid "root metadata" $layout 'title: "The Observance"'

$css = Read-Source "app\globals.css"
foreach ($class in @(
  ".legacy-preservation",
  ".mirror-site",
  ".mirror-page",
  ".record-system-header",
  ".record-error-site",
  ".operator-login-shell",
  ".director-frame"
)) { Require "global surface CSS" $css $class }

$allPages = ($publicRoutes | ForEach-Object { Read-Source $_ }) -join [Environment]::NewLine
$allPages += $record + $archive + $terminal + $bareRecord + $login + $author
foreach ($forbidden in @("Lorem ipsum", "TODO", "TBD", "placeholder text", "Minecraft ARG")) {
  Forbid "website pages" $allPages $forbidden
}

if ($failures.Count -gt 0) {
  Write-Host "web surface coherence: FAILED"
  foreach ($failure in $failures) { Write-Host "  - $failure" }
  exit 1
}

Write-Host "web surface coherence: OK - 12 Copperline surfaces, static file mirror, 3 Record states, global errors, and operator tools follow their visual contracts"
