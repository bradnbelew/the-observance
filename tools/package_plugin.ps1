param(
  [string]$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
)

$ErrorActionPreference = "Stop"

$pluginRoot = Join-Path $RepoRoot "plugin"
$buildFile = Join-Path $pluginRoot "build.gradle"
$classesDir = Join-Path $pluginRoot "build\check-plugin-classes"
$resourcesDir = Join-Path $pluginRoot "src\main\resources"
$holdD05Dir = Join-Path $RepoRoot "design"
$holdD05File = Join-Path $holdD05Dir "deep-hold-d05-shelf.json"
$libsDir = Join-Path $pluginRoot "build\libs"

if (!(Test-Path $buildFile)) {
  throw "Plugin build.gradle not found: $buildFile"
}
if (!(Test-Path (Join-Path $resourcesDir "plugin.yml"))) {
  throw "Plugin resources missing plugin.yml: $resourcesDir"
}
if (!(Test-Path -LiteralPath $holdD05File)) {
  throw "Plugin resources missing Deep Hold D05 shelf: $holdD05File"
}

$versionMatch = [regex]::Match((Get-Content -LiteralPath $buildFile -Raw), "(?m)^version\s*=\s*'([^']+)'")
if (!$versionMatch.Success) {
  throw "Could not read plugin version from $buildFile"
}
$version = $versionMatch.Groups[1].Value
$jarPath = Join-Path $libsDir "observance-$version.jar"

& powershell -NoProfile -ExecutionPolicy Bypass -File (Join-Path $RepoRoot "tools\check_plugin_compile.ps1") -RepoRoot $RepoRoot
if ($LASTEXITCODE -ne 0) {
  exit $LASTEXITCODE
}

New-Item -ItemType Directory -Force -Path $libsDir | Out-Null
if (Test-Path $jarPath) {
  Remove-Item -LiteralPath $jarPath -Force
}

Add-Type -AssemblyName System.IO.Compression
Add-Type -AssemblyName System.IO.Compression.FileSystem

function Write-DeterministicJar([string]$OutPath) {
  if (Test-Path $OutPath) {
    Remove-Item -LiteralPath $OutPath -Force
  }
  $fixedTimestamp = [System.DateTimeOffset]::new(2026, 1, 1, 0, 0, 0, [System.TimeSpan]::Zero)
  $entries = [ordered]@{}
  foreach ($root in @($classesDir, $resourcesDir)) {
    $rootFull = [System.IO.Path]::GetFullPath($root).TrimEnd('\', '/')
    foreach ($file in Get-ChildItem -LiteralPath $rootFull -Recurse -File) {
      $relative = $file.FullName.Substring($rootFull.Length).TrimStart('\', '/').Replace('\', '/')
      if ($entries.Contains($relative)) {
        throw "Duplicate plugin JAR entry '$relative' from $($file.FullName)"
      }
      $entries[$relative] = $file.FullName
    }
  }
  if ($entries.Contains("deep-hold-d05-shelf.json")) {
    throw "Duplicate plugin JAR entry 'deep-hold-d05-shelf.json'"
  }
  $entries["deep-hold-d05-shelf.json"] = $holdD05File

  $stream = [System.IO.File]::Open($OutPath, [System.IO.FileMode]::CreateNew, [System.IO.FileAccess]::ReadWrite)
  try {
    $archive = [System.IO.Compression.ZipArchive]::new($stream, [System.IO.Compression.ZipArchiveMode]::Create, $false)
    try {
      $manifestEntry = $archive.CreateEntry("META-INF/MANIFEST.MF", [System.IO.Compression.CompressionLevel]::Optimal)
      $manifestEntry.LastWriteTime = $fixedTimestamp
      $manifestStream = $manifestEntry.Open()
      try {
        $manifestBytes = [System.Text.Encoding]::UTF8.GetBytes("Manifest-Version: 1.0`r`nCreated-By: Observance deterministic packager`r`n`r`n")
        $manifestStream.Write($manifestBytes, 0, $manifestBytes.Length)
      } finally {
        $manifestStream.Dispose()
      }

      foreach ($relative in ($entries.Keys | Sort-Object)) {
        $entry = $archive.CreateEntry($relative, [System.IO.Compression.CompressionLevel]::Optimal)
        $entry.LastWriteTime = $fixedTimestamp
        $entryStream = $entry.Open()
        try {
          $input = [System.IO.File]::OpenRead([string]$entries[$relative])
          try { $input.CopyTo($entryStream) } finally { $input.Dispose() }
        } finally {
          $entryStream.Dispose()
        }
      }
    } finally {
      $archive.Dispose()
    }
  } finally {
    $stream.Dispose()
  }
}

Write-DeterministicJar $jarPath
$reproPath = "$jarPath.repro-check"
Write-DeterministicJar $reproPath
$sha1 = (Get-FileHash -LiteralPath $jarPath -Algorithm SHA1).Hash.ToLowerInvariant()
$reproSha1 = (Get-FileHash -LiteralPath $reproPath -Algorithm SHA1).Hash.ToLowerInvariant()
Remove-Item -LiteralPath $reproPath -Force
if ($sha1 -ne $reproSha1) {
  throw "Plugin packaging is not reproducible: $sha1 != $reproSha1"
}

Write-Host "plugin packaged: $jarPath"
Write-Host "plugin sha1: $sha1 (reproducible)"

& powershell -NoProfile -ExecutionPolicy Bypass -File (Join-Path $RepoRoot "tools\write_deploy_manifest.ps1") -RepoRoot $RepoRoot
if ($LASTEXITCODE -ne 0) {
  exit $LASTEXITCODE
}
