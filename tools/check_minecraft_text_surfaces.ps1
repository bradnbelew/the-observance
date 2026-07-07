param(
  [string]$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
)

$ErrorActionPreference = "Stop"

$failures = New-Object System.Collections.Generic.List[string]

function Add-Failure([string]$Message) {
  $script:failures.Add($Message) | Out-Null
}

function Read-Utf8([string]$Path) {
  return [System.IO.File]::ReadAllText($Path, [System.Text.UTF8Encoding]::new($false))
}

function Read-Utf8Lines([string]$Path) {
  return [System.IO.File]::ReadAllLines($Path, [System.Text.UTF8Encoding]::new($false))
}

function Line-Of([string]$Text, [int]$Index) {
  if ($Index -le 0) { return 1 }
  return (($Text.Substring(0, $Index) -split "`n").Count)
}

function Convert-JavaString([string]$Token) {
  if ($Token.Length -lt 2) { return "" }
  $body = $Token.Substring(1, $Token.Length - 2)
  $sb = [System.Text.StringBuilder]::new()
  for ($i = 0; $i -lt $body.Length; $i++) {
    $ch = $body[$i]
    if ($ch -ne '\') {
      [void]$sb.Append($ch)
      continue
    }
    if ($i + 1 -ge $body.Length) {
      [void]$sb.Append($ch)
      continue
    }
    $i += 1
    switch ($body[$i]) {
      'n' { [void]$sb.Append("`n") }
      'r' { [void]$sb.Append("`r") }
      't' { [void]$sb.Append("`t") }
      '"' { [void]$sb.Append('"') }
      '\' { [void]$sb.Append('\') }
      default { [void]$sb.Append($body[$i]) }
    }
  }
  return $sb.ToString()
}

function Java-Strings([string]$Snippet) {
  [regex]::Matches($Snippet, '"(?:\\.|[^"\\])*"') | ForEach-Object {
    Convert-JavaString $_.Value
  }
}

function Find-Calls([string]$Text, [string]$Name) {
  $calls = New-Object System.Collections.Generic.List[object]
  $needle = "$Name("
  $idx = 0
  while (($start = $Text.IndexOf($needle, $idx, [System.StringComparison]::Ordinal)) -ge 0) {
    $pos = $start + $needle.Length - 1
    $depth = 0
    $inString = $false
    $escape = $false
    for ($i = $pos; $i -lt $Text.Length; $i++) {
      $c = $Text[$i]
      if ($inString) {
        if ($escape) {
          $escape = $false
        } elseif ($c -eq '\') {
          $escape = $true
        } elseif ($c -eq '"') {
          $inString = $false
        }
        continue
      }
      if ($c -eq '"') {
        $inString = $true
      } elseif ($c -eq '(') {
        $depth += 1
      } elseif ($c -eq ')') {
        $depth -= 1
        if ($depth -eq 0) {
          $calls.Add([pscustomobject]@{
            Start = $start
            Line = Line-Of $Text $start
            Text = $Text.Substring($start, $i - $start + 1)
          }) | Out-Null
          $idx = $i + 1
          break
        }
      }
    }
    if ($idx -le $start) { $idx = $start + $needle.Length }
  }
  return $calls
}

function Check-Length([string]$Label, [string]$Value, [int]$Max, [string]$Where) {
  if ($Value.Length -gt $Max) {
    Add-Failure "$Where $Label is $($Value.Length) chars, limit ${Max}: '$Value'"
  }
}

$signLineLimit = 15
$hudLineLimit = 64
$tooltipLineLimit = 50
$bookTitleLimit = 32
$bookPageLimit = 240
$bookHardLineLimit = 32

$structurePath = Join-Path $RepoRoot "plugin\src\main\java\com\observance\watcher\structure\StructureTemplates.java"
$textFitPath = Join-Path $RepoRoot "plugin\src\main\java\com\observance\watcher\util\TextFit.java"

foreach ($path in @($structurePath, $textFitPath)) {
  if (-not (Test-Path $path)) {
    throw "minecraft text surface check: missing required source: $path"
  }
}

$structure = Read-Utf8 $structurePath
$textFit = Read-Utf8 $textFitPath
foreach ($required in @("BOOK_PAGE_CHARS = 240", "SIGN_LINE_CHARS = 15", "HUD_LINE_CHARS = 64", "TOOLTIP_LINE_CHARS = 50")) {
  if ($textFit.IndexOf($required, [System.StringComparison]::Ordinal) -lt 0) {
    Add-Failure "TextFit no longer exposes expected client-facing limit: $required"
  }
}

foreach ($method in @("labelWallSign", "hangingSign")) {
  foreach ($call in Find-Calls $structure $method) {
    $strings = @(Java-Strings $call.Text)
    for ($i = 0; $i -lt $strings.Count; $i++) {
      Check-Length "$method line" $strings[$i] $signLineLimit "StructureTemplates.java:$($call.Line)"
    }
  }
}

foreach ($method in @("runeCrib", "runeCribPair")) {
  foreach ($call in Find-Calls $structure $method) {
    $strings = @(Java-Strings $call.Text)
    if ($strings.Count -eq 0) { continue }
    $word = $strings[$strings.Count - 1]
    Check-Length "$method word" $word $signLineLimit "StructureTemplates.java:$($call.Line)"
    if ($word -notmatch '^[A-Za-z0-9]+$') {
      Add-Failure "StructureTemplates.java:$($call.Line) $method word must be ASCII A-Z/0-9 for the rune font: '$word'"
    }
  }
}

foreach ($call in Find-Calls $structure "putBook") {
  $strings = @(Java-Strings $call.Text)
  if ($strings.Count -lt 2) { continue }
  $title = $strings[0]
  $page = ($strings | Select-Object -Skip 1) -join ""
  Check-Length "book title" $title $bookTitleLimit "StructureTemplates.java:$($call.Line)"
  Check-Length "book page" $page $bookPageLimit "StructureTemplates.java:$($call.Line)"
  foreach ($hardLine in ($page -split "`r?`n")) {
    Check-Length "book hard line" $hardLine $bookHardLineLimit "StructureTemplates.java:$($call.Line)"
  }
}

$javaRoot = Join-Path $RepoRoot "plugin\src\main\java"
Get-ChildItem -LiteralPath $javaRoot -Recurse -Filter *.java | ForEach-Object {
  $rel = $_.FullName
  $rootPrefix = $RepoRoot.TrimEnd('\') + '\'
  if ($rel.StartsWith($rootPrefix, [System.StringComparison]::OrdinalIgnoreCase)) {
    $rel = $rel.Substring($rootPrefix.Length)
  }
  $text = Read-Utf8 $_.FullName
  $lineNo = 0
  foreach ($line in Read-Utf8Lines $_.FullName) {
    $lineNo += 1
    foreach ($m in [regex]::Matches($line, 'sendActionBar\s*\(\s*Component\.text\s*\(\s*("(?:\\.|[^"\\])*")')) {
      $s = Convert-JavaString $m.Groups[1].Value
      Check-Length "actionbar literal" $s $hudLineLimit "${rel}:${lineNo}"
    }
    foreach ($m in [regex]::Matches($line, 'displayName\s*\(\s*Component\.text\s*\(\s*("(?:\\.|[^"\\])*")')) {
      $s = Convert-JavaString $m.Groups[1].Value
      Check-Length "tooltip display name" $s $tooltipLineLimit "${rel}:${lineNo}"
    }
    foreach ($m in [regex]::Matches($line, 'Component\.text\s*\(\s*("(?:\\.|[^"\\])*")')) {
      $s = Convert-JavaString $m.Groups[1].Value
      if ($line -match 'meta\.lore|List\.of|lore\(') {
        Check-Length "tooltip lore literal" $s $tooltipLineLimit "${rel}:${lineNo}"
      }
    }
  }
}

foreach ($required in @(
  "BookAppearsBeat.java:TextFit.paginate",
  "LecternFillBeat.java:TextFit.paginate",
  "SignWriteBeat.java:TextFit.SIGN_LINE_CHARS",
  "PrivateMessageBeat.java:TextFit.HUD_LINE_CHARS",
  "BossBarBeat.java:TextFit.HUD_LINE_CHARS",
  "ItemRelabelBeat.java:TextFit.TOOLTIP_LINE_CHARS",
  "KeptNeedleBeat.java:TextFit.TOOLTIP_LINE_CHARS"
)) {
  $parts = $required.Split(":")
  $file = Join-Path $javaRoot ("com\observance\watcher\beats\lib\" + $parts[0])
  if (-not (Test-Path $file)) {
    Add-Failure "missing text-fit guarded beat source: $($parts[0])"
    continue
  }
  $source = Read-Utf8 $file
  if ($source.IndexOf($parts[1], [System.StringComparison]::Ordinal) -lt 0) {
    Add-Failure "$($parts[0]) no longer uses expected text-fit guard: $($parts[1])"
  }
}

if ($failures.Count -gt 0) {
  Write-Host "minecraft text surface check: FAILED"
  foreach ($failure in $failures) {
    Write-Host "  - $failure"
  }
  exit 1
}

Write-Host "minecraft text surface check: OK - authored signs, structure books, actionbars, and text-fit guards stay within static client-facing limits"
