param(
  [string]$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path,
  [string]$OutPath = "observance-deploy-manifest.json"
)

$ErrorActionPreference = "Stop"

function Resolve-UnderRepo([string]$Root, [string]$Path) {
  $rootFull = [System.IO.Path]::GetFullPath($Root).TrimEnd('\', '/')
  $full = if ([System.IO.Path]::IsPathRooted($Path)) {
    [System.IO.Path]::GetFullPath($Path)
  } else {
    [System.IO.Path]::GetFullPath((Join-Path $rootFull $Path))
  }
  $prefix = $rootFull + [System.IO.Path]::DirectorySeparatorChar
  if (!$full.Equals($rootFull, [System.StringComparison]::OrdinalIgnoreCase) -and
      !$full.StartsWith($prefix, [System.StringComparison]::OrdinalIgnoreCase)) {
    throw "Path escapes repository: $full"
  }
  return $full
}

function Relative-Path([string]$Root, [string]$Path) {
  $rootUri = [System.Uri]::new(([System.IO.Path]::GetFullPath($Root).TrimEnd("\") + "\"))
  $pathUri = [System.Uri]::new([System.IO.Path]::GetFullPath($Path))
  return [System.Uri]::UnescapeDataString($rootUri.MakeRelativeUri($pathUri).ToString()).Replace("/", "\")
}

function File-Entry([string]$Root, [string]$Path) {
  $full = [System.IO.Path]::GetFullPath($Path)
  if (!(Test-Path -LiteralPath $full -PathType Leaf)) {
    return [ordered]@{
      path = Relative-Path $Root $full
      exists = $false
      sha1 = ""
      sha256 = ""
      bytes = 0
      lastWriteUtc = ""
    }
  }
  $item = Get-Item -LiteralPath $full
  return [ordered]@{
    path = Relative-Path $Root $item.FullName
    exists = $true
    sha1 = (Get-FileHash -LiteralPath $item.FullName -Algorithm SHA1).Hash.ToLowerInvariant()
    sha256 = (Get-FileHash -LiteralPath $item.FullName -Algorithm SHA256).Hash.ToLowerInvariant()
    bytes = $item.Length
    lastWriteUtc = $item.LastWriteTimeUtc.ToString("o")
  }
}

function Git-Value([string]$Root, [string[]]$Arguments) {
  $value = & git -C $Root @Arguments 2>$null
  if ($LASTEXITCODE -ne 0) { return "" }
  return (($value | Out-String).Trim())
}

function Config-Value([string]$Text, [string]$Section, [string]$Key) {
  $inSection = $false
  foreach ($line in ($Text -split "`r?`n")) {
    if ($line -match '^([^\s#][^:]*):\s*(?:#.*)?$') {
      $inSection = $matches[1].Trim() -eq $Section
      continue
    }
    if ($inSection -and $line -match ("^\s+" + [regex]::Escape($Key) + ":\s+`"([^`"]*)`"\s*(?:#.*)?$")) {
      return $matches[1].Trim()
    }
  }
  return ""
}

function Config-Bool([string]$Text, [string]$Section, [string]$Key) {
  $inSection = $false
  foreach ($line in ($Text -split "`r?`n")) {
    if ($line -match '^([^\s#][^:]*):\s*(?:#.*)?$') {
      $inSection = $matches[1].Trim() -eq $Section
      continue
    }
    if ($inSection -and $line -match ("^\s+" + [regex]::Escape($Key) + ":\s+(true|false)\s*(?:#.*)?$")) {
      return [bool]::Parse($matches[1])
    }
  }
  return $false
}

$repoFull = [System.IO.Path]::GetFullPath($RepoRoot)
$outFull = Resolve-UnderRepo $repoFull $OutPath

$buildFile = Join-Path $repoFull "plugin\build.gradle"
$version = "UNKNOWN"
if (Test-Path $buildFile) {
  $versionMatch = [regex]::Match((Get-Content -LiteralPath $buildFile -Raw), "(?m)^version\s*=\s*'([^']+)'")
  if ($versionMatch.Success) {
    $version = $versionMatch.Groups[1].Value
  }
}
if ($version -ne "0.5.0") {
  throw "Refusing to write a production deploy manifest for plugin version '$version'; V5 requires 0.5.0"
}

$configFile = Join-Path $repoFull "plugin\src\main\resources\config.yml"
$configText = if (Test-Path $configFile) { Get-Content -LiteralPath $configFile -Raw } else { "" }
$gitCommit = Git-Value $repoFull @("rev-parse", "HEAD")
$gitBranch = Git-Value $repoFull @("branch", "--show-current")
$gitStatus = Git-Value $repoFull @("status", "--porcelain=v1", "--untracked-files=all")

$manifest = [ordered]@{
  schemaVersion = 2
  generatedUtc = (Get-Date).ToUniversalTime().ToString("o")
  minecraftTarget = "Paper 1.21.11"
  pluginVersion = $version
  source = [ordered]@{
    gitCommit = $gitCommit
    gitBranch = $gitBranch
    workingTreeDirty = (-not [string]::IsNullOrWhiteSpace($gitStatus))
  }
  artifacts = [ordered]@{
    pluginJar = File-Entry $repoFull (Join-Path $repoFull "plugin\build\libs\observance-$version.jar")
    datapackZip = File-Entry $repoFull (Join-Path $repoFull "observance-datapack.zip")
    resourcepackZip = File-Entry $repoFull (Join-Path $repoFull "observance-resourcepack.zip")
    supabaseSql = File-Entry $repoFull (Join-Path $repoFull "discord\supabase\apply-all.sql")
    holdArchive = File-Entry $repoFull (Join-Path $repoFull "dashboard\content\the-hold-v5\the-hold.zip")
  }
  deploymentInputs = [ordered]@{
    holdArchiveSha1 = File-Entry $repoFull (Join-Path $repoFull "dashboard\content\the-hold-v5\the-hold.sha1")
    supabaseVerificationSql = File-Entry $repoFull (Join-Path $repoFull "tools\verify_supabase_v5.sql")
    renderBlueprint = File-Entry $repoFull (Join-Path $repoFull "render.yaml")
    vercelIgnore = File-Entry $repoFull (Join-Path $repoFull ".vercelignore")
    dashboardLockfile = File-Entry $repoFull (Join-Path $repoFull "dashboard\package-lock.json")
    discordLockfile = File-Entry $repoFull (Join-Path $repoFull "discord\package-lock.json")
  }
  resourcePackConfig = [ordered]@{
    url = Config-Value $configText "resource-pack" "url"
    sha1 = (Config-Value $configText "resource-pack" "sha1").ToLowerInvariant()
    required = Config-Bool $configText "resource-pack" "required"
    prompt = Config-Value $configText "resource-pack" "prompt"
  }
  packagingRules = [ordered]@{
    resourcepackRuntimeRoots = @("pack.mcmeta", "pack.png", "assets", "data")
    note = "Only runtime pack roots are zipped; docs and design files must not change the hosted resource-pack hash."
  }
}

$parent = Split-Path -Parent $outFull
if (![string]::IsNullOrWhiteSpace($parent)) {
  New-Item -ItemType Directory -Force -Path $parent | Out-Null
}

$json = $manifest | ConvertTo-Json -Depth 8
$tempOut = Join-Path $parent ("." + [System.IO.Path]::GetFileName($outFull) + "." + [Guid]::NewGuid().ToString("N") + ".tmp")
Resolve-UnderRepo $repoFull $tempOut | Out-Null
try {
  [System.IO.File]::WriteAllText($tempOut, ($json + [Environment]::NewLine), [System.Text.UTF8Encoding]::new($false))
  if (Test-Path -LiteralPath $outFull -PathType Leaf) {
    $backupOut = $tempOut + ".previous"
    [System.IO.File]::Replace($tempOut, $outFull, $backupOut)
    if (Test-Path -LiteralPath $backupOut -PathType Leaf) {
      Remove-Item -LiteralPath $backupOut -Force
    }
  } else {
    [System.IO.File]::Move($tempOut, $outFull)
  }
} finally {
  if (Test-Path -LiteralPath $tempOut -PathType Leaf) {
    Remove-Item -LiteralPath $tempOut -Force
  }
  if ($null -ne $backupOut -and (Test-Path -LiteralPath $backupOut -PathType Leaf)) {
    Remove-Item -LiteralPath $backupOut -Force
  }
}
Write-Host "deploy manifest written: $outFull"
