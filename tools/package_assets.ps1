param(
  [string]$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
)

$ErrorActionPreference = "Stop"

function Assert-WithinRepo([string]$Path, [string]$RepoRoot) {
  $repoFull = [System.IO.Path]::GetFullPath($RepoRoot).TrimEnd('\', '/')
  $pathFull = [System.IO.Path]::GetFullPath($Path)
  $prefix = $repoFull + [System.IO.Path]::DirectorySeparatorChar
  if (!$pathFull.Equals($repoFull, [System.StringComparison]::OrdinalIgnoreCase) -and
      !$pathFull.StartsWith($prefix, [System.StringComparison]::OrdinalIgnoreCase)) {
    throw "Refusing to write outside repo: $pathFull"
  }
}

function Write-PackZip([string]$SourceDir, [string]$ZipPath, [string]$Label) {
  if (!(Test-Path (Join-Path $SourceDir "pack.mcmeta"))) {
    throw "$Label source missing pack.mcmeta: $SourceDir"
  }
  Assert-WithinRepo $ZipPath $RepoRoot
  $zipFull = [System.IO.Path]::GetFullPath($ZipPath)
  $zipParent = Split-Path -Parent $zipFull
  New-Item -ItemType Directory -Force -Path $zipParent | Out-Null
  $tempZip = Join-Path $zipParent ("." + [System.IO.Path]::GetFileName($zipFull) + "." + [Guid]::NewGuid().ToString("N") + ".tmp")
  Assert-WithinRepo $tempZip $RepoRoot
  $files = [System.Collections.Generic.List[System.IO.FileInfo]]::new()
  foreach ($name in @("pack.mcmeta", "pack.png", "assets", "data")) {
    $path = Join-Path $SourceDir $name
    if (Test-Path $path) {
      $item = Get-Item -LiteralPath $path
      if ($item.PSIsContainer) {
        foreach ($file in Get-ChildItem -LiteralPath $item.FullName -Recurse -File) {
          $files.Add($file) | Out-Null
        }
      } else {
        $files.Add($item) | Out-Null
      }
    }
  }
  if ($files.Count -eq 0) {
    throw "$Label source has no runtime pack files: $SourceDir"
  }
  Add-Type -AssemblyName System.IO.Compression
  Add-Type -AssemblyName System.IO.Compression.FileSystem
  $sourceFull = [System.IO.Path]::GetFullPath($SourceDir).TrimEnd('\', '/')
  $fixedTimestamp = [System.DateTimeOffset]::new(2026, 1, 1, 0, 0, 0, [System.TimeSpan]::Zero)
  $zipStream = [System.IO.File]::Open($tempZip, [System.IO.FileMode]::CreateNew, [System.IO.FileAccess]::ReadWrite)
  try {
    $archive = [System.IO.Compression.ZipArchive]::new($zipStream, [System.IO.Compression.ZipArchiveMode]::Create, $false)
    try {
      foreach ($file in ($files | Sort-Object FullName)) {
        $fileFull = [System.IO.Path]::GetFullPath($file.FullName)
        $relative = $fileFull.Substring($sourceFull.Length).TrimStart('\', '/').Replace('\', '/')
        $entry = $archive.CreateEntry($relative, [System.IO.Compression.CompressionLevel]::Optimal)
        $entry.LastWriteTime = $fixedTimestamp
        $entryStream = $entry.Open()
        try {
          $input = [System.IO.File]::OpenRead($fileFull)
          try {
            $input.CopyTo($entryStream)
          } finally {
            $input.Dispose()
          }
        } finally {
          $entryStream.Dispose()
        }
      }
    } finally {
      $archive.Dispose()
    }
  } finally {
    $zipStream.Dispose()
  }

  try {
    # Publish only a complete archive. File.Replace keeps the previous good artifact intact until
    # the deterministic temporary zip has closed successfully.
    if (Test-Path -LiteralPath $zipFull -PathType Leaf) {
      $backupZip = $tempZip + ".previous"
      [System.IO.File]::Replace($tempZip, $zipFull, $backupZip)
      if (Test-Path -LiteralPath $backupZip -PathType Leaf) {
        Remove-Item -LiteralPath $backupZip -Force
      }
    } else {
      [System.IO.File]::Move($tempZip, $zipFull)
    }
  } finally {
    if (Test-Path -LiteralPath $tempZip -PathType Leaf) {
      Remove-Item -LiteralPath $tempZip -Force
    }
    if ($null -ne $backupZip -and (Test-Path -LiteralPath $backupZip -PathType Leaf)) {
      Remove-Item -LiteralPath $backupZip -Force
    }
  }

  $sha1 = (Get-FileHash -LiteralPath $zipFull -Algorithm SHA1).Hash.ToLowerInvariant()
  $sha256 = (Get-FileHash -LiteralPath $zipFull -Algorithm SHA256).Hash.ToLowerInvariant()
  Write-Host "$Label packaged: $zipFull"
  Write-Host "$Label sha1: $sha1"
  Write-Host "$Label sha256: $sha256"
}

$datapackSource = Join-Path $RepoRoot "datapack\observance"
$resourcepackSource = Join-Path $RepoRoot "resourcepack"

Write-PackZip $datapackSource (Join-Path $RepoRoot "observance-datapack.zip") "datapack"
Write-PackZip $resourcepackSource (Join-Path $RepoRoot "observance-resourcepack.zip") "resourcepack"

& powershell -NoProfile -ExecutionPolicy Bypass -File (Join-Path $RepoRoot "tools\write_deploy_manifest.ps1") -RepoRoot $RepoRoot
if ($LASTEXITCODE -ne 0) {
  exit $LASTEXITCODE
}
