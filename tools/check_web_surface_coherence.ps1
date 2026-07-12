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
Forbid "server list" $listing "<button type=\"button\">Filter"

$support = Read-Source "app\support\index.php\page.tsx"
foreach ($anchor in @("#ftp", "#backups", "#jars", "#packs", "#billing", "#tickets")) {
  Require "support" $support $anchor
}
Forbid "support" $support "<Link href=\"/support/index.php\">Connecting"

$post = Read-Source "app\community\2011\02\08\world-backup\page.tsx"
foreach ($needle in @(
  "Do not install the zip as a datapack",
  "do not merge it into an older copy",
  "static file mirror"
)) { Require "world backup post" $post $needle }
foreach ($retired in @("included text file", "walk north", "first stone stair", "plain mirror page")) {
  Forbid "world backup post" $post $retired
}

$record = Read-Source "app\record\[slug]\page.tsx"
foreach ($needle in @(
  "mirror-site",
  "static user mirror",
  "/home/mkept/public_html/record/",
  "copy-register.txt",
  "uploader-note.txt",
  "record-system-header",
  "recordsrv/0.7",
  "record-error-site"
)) { Require "record slug" $record $needle }
Forbid "record slug" $record "RuneGlyphs"
Forbid "record slug" $record "mirror fragment / uploader copy"

$archive = Read-Source "app\record\archive\page.tsx"
$terminal = Read-Source "app\record\terminal\page.tsx"
foreach ($pair in @(@("archive", $archive), @("terminal", $terminal))) {
  Require $pair[0] $pair[1] "record-system-header"
  Require $pair[0] $pair[1] "recordsrv/0.7"
  Forbid $pair[0] $pair[1] "RuneGlyphs"
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
