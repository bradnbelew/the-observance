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
  $items = @()
  foreach ($name in @("pack.mcmeta", "pack.png", "assets", "data")) {
    $path = Join-Path $SourceDir $name
    if (Test-Path $path) {
      $items += Get-Item -LiteralPath $path
    }
  }
  if ($items.Count -eq 0) {
    throw "$Label source has no runtime pack files: $SourceDir"
  }
  Compress-Archive -LiteralPath $items.FullName -DestinationPath $ZipPath -CompressionLevel Optimal
  $sha1 = (Get-FileHash -LiteralPath $ZipPath -Algorithm SHA1).Hash.ToLowerInvariant()
  Write-Host "$Label packaged: $ZipPath"
  Write-Host "$Label sha1: $sha1"
}

$datapackSource = Join-Path $RepoRoot "datapack\observance"
$resourcepackSource = Join-Path $RepoRoot "resourcepack"

Write-PackZip $datapackSource (Join-Path $RepoRoot "observance-datapack.zip") "datapack"
Write-PackZip $resourcepackSource (Join-Path $RepoRoot "observance-resourcepack.zip") "resourcepack"

& powershell -NoProfile -ExecutionPolicy Bypass -File (Join-Path $RepoRoot "tools\write_deploy_manifest.ps1") -RepoRoot $RepoRoot
if ($LASTEXITCODE -ne 0) {
  exit $LASTEXITCODE
}
