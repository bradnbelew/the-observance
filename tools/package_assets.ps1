param(
  [string]$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
)

$ErrorActionPreference = "Stop"

function Assert-WithinRepo([string]$Path, [string]$RepoRoot) {
  $repoFull = [System.IO.Path]::GetFullPath($RepoRoot)
  $pathFull = [System.IO.Path]::GetFullPath($Path)
  if (!$pathFull.StartsWith($repoFull, [System.StringComparison]::OrdinalIgnoreCase)) {
    throw "Refusing to write outside repo: $pathFull"
  }
}

function Write-PackZip([string]$SourceDir, [string]$ZipPath, [string]$Label) {
  if (!(Test-Path (Join-Path $SourceDir "pack.mcmeta"))) {
    throw "$Label source missing pack.mcmeta: $SourceDir"
  }
  Assert-WithinRepo $ZipPath $RepoRoot
  if (Test-Path $ZipPath) {
    Remove-Item -LiteralPath $ZipPath -Force
  }
  $items = Get-ChildItem -LiteralPath $SourceDir -Force
  Compress-Archive -LiteralPath $items.FullName -DestinationPath $ZipPath -CompressionLevel Optimal
  $sha1 = (Get-FileHash -LiteralPath $ZipPath -Algorithm SHA1).Hash.ToLowerInvariant()
  Write-Host "$Label packaged: $ZipPath"
  Write-Host "$Label sha1: $sha1"
}

$datapackSource = Join-Path $RepoRoot "datapack\observance"
$resourcepackSource = Join-Path $RepoRoot "resourcepack"

Write-PackZip $datapackSource (Join-Path $RepoRoot "observance-datapack.zip") "datapack"
Write-PackZip $resourcepackSource (Join-Path $RepoRoot "observance-resourcepack.zip") "resourcepack"
