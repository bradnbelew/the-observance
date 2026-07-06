param(
  [string]$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path,
  [string]$SitesFile = "",
  [string]$CaptureCsv = "",
  [switch]$Apply,
  [switch]$RequireAll,
  [switch]$AllowOpenVisuals
)

$ErrorActionPreference = "Stop"

function Resolve-UnderRepo([string]$Root, [string]$Path) {
  if ([System.IO.Path]::IsPathRooted($Path)) {
    return [System.IO.Path]::GetFullPath($Path)
  }
  return [System.IO.Path]::GetFullPath((Join-Path $Root $Path))
}

function Fail([System.Collections.Generic.List[string]]$Failures, [string]$Message) {
  $Failures.Add($Message) | Out-Null
}

function IsBlank([object]$Value) {
  return $null -eq $Value -or [string]::IsNullOrWhiteSpace([string]$Value)
}

function Clean([object]$Value) {
  if ($null -eq $Value) { return "" }
  return ([string]$Value).Trim()
}

function IsNumber([string]$Value) {
  return $Value -match '^-?\d+(?:\.\d+)?$'
}

function FormatCoord([string]$Value) {
  $v = $Value.Trim()
  if ($v -match '^-?\d+\.0+$') {
    return $v.Substring(0, $v.IndexOf('.'))
  }
  return $v
}

function Update-LineValue([string]$Line, [string]$Value) {
  $comment = ""
  $prefix = $Line
  $commentIndex = $Line.IndexOf("#")
  if ($commentIndex -ge 0) {
    $prefix = $Line.Substring(0, $commentIndex).TrimEnd()
    $comment = " " + $Line.Substring($commentIndex)
  }
  $key = [regex]::Match($prefix, '^(\s*[A-Za-z0-9_-]+:\s*)').Groups[1].Value
  return $key + $Value + $comment
}

$repoFull = [System.IO.Path]::GetFullPath($RepoRoot)
if ([string]::IsNullOrWhiteSpace($SitesFile)) {
  $SitesFile = Join-Path $repoFull "plugin\src\main\resources\sites.yml"
}
$sitesFull = Resolve-UnderRepo $repoFull $SitesFile
if (-not $sitesFull.StartsWith($repoFull, [System.StringComparison]::OrdinalIgnoreCase)) {
  throw "Refusing to update sites file outside repo: $sitesFull"
}
if (-not (Test-Path $sitesFull)) {
  throw "launch coords apply: sites file not found: $sitesFull"
}

if ([string]::IsNullOrWhiteSpace($CaptureCsv)) {
  $placementRoot = Join-Path $repoFull "build\launch-placement"
  $latest = if (Test-Path $placementRoot) {
    Get-ChildItem -LiteralPath $placementRoot -Directory |
      Sort-Object LastWriteTime -Descending |
      Select-Object -First 1
  } else {
    $null
  }
  if ($null -eq $latest) {
    throw "launch coords apply: no CaptureCsv supplied and no build\launch-placement packet exists"
  }
  $CaptureCsv = Join-Path $latest.FullName "coords-capture.csv"
}
$captureFull = Resolve-UnderRepo $repoFull $CaptureCsv
if (-not $captureFull.StartsWith($repoFull, [System.StringComparison]::OrdinalIgnoreCase)) {
  throw "Refusing to read capture CSV outside repo: $captureFull"
}
if (-not (Test-Path $captureFull)) {
  throw "launch coords apply: capture CSV not found: $captureFull"
}

$rows = @(Import-Csv -LiteralPath $captureFull)
foreach ($requiredColumn in @("SiteId", "ChosenWorld", "X", "Y", "Z", "VisualStatus")) {
  if (-not ($rows | Get-Member -Name $requiredColumn -MemberType NoteProperty)) {
    throw "launch coords apply: coords-capture.csv missing column: $requiredColumn"
  }
}

$failures = [System.Collections.Generic.List[string]]::new()
$updates = [ordered]@{}
foreach ($row in $rows) {
  $id = (Clean $row.SiteId).ToLowerInvariant()
  if ([string]::IsNullOrWhiteSpace($id)) {
    Fail $failures "coords-capture.csv has a row with blank SiteId"
    continue
  }

  $hasAnyCoord = -not (IsBlank $row.ChosenWorld) -or -not (IsBlank $row.X) -or -not (IsBlank $row.Y) -or -not (IsBlank $row.Z)
  if (-not $hasAnyCoord) {
    if ($RequireAll) {
      Fail $failures "site '$id' has no captured coordinates"
    }
    continue
  }

  $world = Clean $row.ChosenWorld
  $x = FormatCoord (Clean $row.X)
  $y = FormatCoord (Clean $row.Y)
  $z = FormatCoord (Clean $row.Z)
  $visual = Clean $row.VisualStatus

  if ([string]::IsNullOrWhiteSpace($world)) {
    Fail $failures "site '$id' missing ChosenWorld"
  }
  foreach ($pair in @(@("X", $x), @("Y", $y), @("Z", $z))) {
    if ([string]::IsNullOrWhiteSpace($pair[1]) -or -not (IsNumber $pair[1])) {
      Fail $failures "site '$id' has invalid $($pair[0]) coordinate: '$($pair[1])'"
    }
  }
  if (-not $AllowOpenVisuals -and $visual -ne "KEEP") {
    Fail $failures "site '$id' VisualStatus must be KEEP before applying coordinates; found '$visual'"
  }
  if ($updates.Contains($id)) {
    Fail $failures "site '$id' appears more than once in coords-capture.csv"
    continue
  }
  $updates[$id] = [pscustomobject]@{ world = $world; x = $x; y = $y; z = $z; visual = $visual }
}

$lines = [System.Collections.Generic.List[string]]::new()
(Get-Content -LiteralPath $sitesFull) | ForEach-Object { $lines.Add($_) | Out-Null }

$seenSites = [System.Collections.Generic.HashSet[string]]::new([System.StringComparer]::OrdinalIgnoreCase)
$currentId = ""
for ($i = 0; $i -lt $lines.Count; $i++) {
  $siteMatch = [regex]::Match($lines[$i], '^\s{2}([A-Za-z0-9_-]+):\s*(?:#.*)?$')
  if ($siteMatch.Success) {
    $currentId = $siteMatch.Groups[1].Value.ToLowerInvariant()
    if ($updates.Contains($currentId)) {
      $seenSites.Add($currentId) | Out-Null
    }
    continue
  }
  if ([string]::IsNullOrWhiteSpace($currentId) -or -not $updates.Contains($currentId)) {
    continue
  }
  $u = $updates[$currentId]
  if ($lines[$i] -match '^\s{4}world:\s*') {
    $lines[$i] = Update-LineValue $lines[$i] ('"' + $u.world + '"')
  } elseif ($lines[$i] -match '^\s{4}x:\s*') {
    $lines[$i] = Update-LineValue $lines[$i] $u.x
  } elseif ($lines[$i] -match '^\s{4}y:\s*') {
    $lines[$i] = Update-LineValue $lines[$i] $u.y
  } elseif ($lines[$i] -match '^\s{4}z:\s*') {
    $lines[$i] = Update-LineValue $lines[$i] $u.z
  }
}

foreach ($id in $updates.Keys) {
  if (-not $seenSites.Contains($id)) {
    Fail $failures "site '$id' from coords-capture.csv does not exist in sites.yml"
  }
}

if ($failures.Count -gt 0) {
  foreach ($failure in $failures) {
    [Console]::Error.WriteLine($failure)
  }
  exit 1
}

if ($updates.Count -eq 0) {
  Write-Host "launch coords apply: no filled coordinate rows found in $captureFull"
  exit 0
}

foreach ($id in $updates.Keys) {
  $u = $updates[$id]
  Write-Host ("{0}: {1} {2},{3},{4} visual={5}" -f $id, $u.world, $u.x, $u.y, $u.z, $u.visual)
}

if (-not $Apply) {
  Write-Host "launch coords apply: preview only - rerun with -Apply to update sites.yml"
  exit 0
}

[System.IO.File]::WriteAllLines($sitesFull, $lines, [System.Text.UTF8Encoding]::new($false))
Write-Host "launch coords apply: updated $($updates.Count) site coordinate row(s) in $sitesFull"
