param(
  [string]$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path,
  [string]$OutPath = "observance-deploy-manifest.json"
)

$ErrorActionPreference = "Stop"

function Resolve-UnderRepo([string]$Root, [string]$Path) {
  if ([System.IO.Path]::IsPathRooted($Path)) {
    return [System.IO.Path]::GetFullPath($Path)
  }
  return [System.IO.Path]::GetFullPath((Join-Path $Root $Path))
}

function Relative-Path([string]$Root, [string]$Path) {
  $rootUri = [System.Uri]::new(([System.IO.Path]::GetFullPath($Root).TrimEnd("\") + "\"))
  $pathUri = [System.Uri]::new([System.IO.Path]::GetFullPath($Path))
  return [System.Uri]::UnescapeDataString($rootUri.MakeRelativeUri($pathUri).ToString()).Replace("/", "\")
}

function File-Entry([string]$Root, [string]$Path) {
  $full = [System.IO.Path]::GetFullPath($Path)
  if (!(Test-Path $full)) {
    return [ordered]@{
      path = Relative-Path $Root $full
      exists = $false
      sha1 = ""
      bytes = 0
      lastWriteUtc = ""
    }
  }
  $item = Get-Item -LiteralPath $full
  return [ordered]@{
    path = Relative-Path $Root $item.FullName
    exists = $true
    sha1 = (Get-FileHash -LiteralPath $item.FullName -Algorithm SHA1).Hash.ToLowerInvariant()
    bytes = $item.Length
    lastWriteUtc = $item.LastWriteTimeUtc.ToString("o")
  }
}

function Config-Value([string]$Text, [string]$Section, [string]$Key) {
  $pattern = "(?ms)^$([regex]::Escape($Section)):\s.*?^\s+$([regex]::Escape($Key)):\s+`"([^`"]*)`""
  $match = [regex]::Match($Text, $pattern)
  if ($match.Success) { return $match.Groups[1].Value.Trim() }
  return ""
}

function Config-Bool([string]$Text, [string]$Section, [string]$Key) {
  $pattern = "(?ms)^$([regex]::Escape($Section)):\s.*?^\s+$([regex]::Escape($Key)):\s+(true|false)\s*$"
  $match = [regex]::Match($Text, $pattern)
  if ($match.Success) { return [bool]::Parse($match.Groups[1].Value) }
  return $false
}

$repoFull = [System.IO.Path]::GetFullPath($RepoRoot)
$outFull = Resolve-UnderRepo $repoFull $OutPath
if (!$outFull.StartsWith($repoFull, [System.StringComparison]::OrdinalIgnoreCase)) {
  throw "Refusing to write deploy manifest outside repo: $outFull"
}

$buildFile = Join-Path $repoFull "plugin\build.gradle"
$version = "UNKNOWN"
if (Test-Path $buildFile) {
  $versionMatch = [regex]::Match((Get-Content -LiteralPath $buildFile -Raw), "(?m)^version\s*=\s*'([^']+)'")
  if ($versionMatch.Success) {
    $version = $versionMatch.Groups[1].Value
  }
}

$configFile = Join-Path $repoFull "plugin\src\main\resources\config.yml"
$configText = if (Test-Path $configFile) { Get-Content -LiteralPath $configFile -Raw } else { "" }

$manifest = [ordered]@{
  generatedUtc = (Get-Date).ToUniversalTime().ToString("o")
  minecraftTarget = "Paper 1.21.11"
  pluginVersion = $version
  artifacts = [ordered]@{
    pluginJar = File-Entry $repoFull (Join-Path $repoFull "plugin\build\libs\observance-$version.jar")
    datapackZip = File-Entry $repoFull (Join-Path $repoFull "observance-datapack.zip")
    resourcepackZip = File-Entry $repoFull (Join-Path $repoFull "observance-resourcepack.zip")
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
[System.IO.File]::WriteAllText($outFull, ($json + [Environment]::NewLine), [System.Text.Encoding]::UTF8)
Write-Host "deploy manifest written: $outFull"
