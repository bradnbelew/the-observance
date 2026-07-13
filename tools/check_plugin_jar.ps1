param(
  [string]$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
)

$ErrorActionPreference = "Stop"

Add-Type -AssemblyName System.IO.Compression.FileSystem

$pluginRoot = Join-Path $RepoRoot "plugin"
$buildFile = Join-Path $pluginRoot "build.gradle"
$srcRoot = Join-Path $pluginRoot "src"
$libsDir = Join-Path $pluginRoot "build\libs"

$failures = New-Object System.Collections.Generic.List[string]
function Add-Failure([string]$Message) {
  $script:failures.Add($Message) | Out-Null
}

if (!(Test-Path $buildFile)) {
  Add-Failure "Plugin build.gradle not found: $buildFile"
} else {
  $versionMatch = [regex]::Match((Get-Content -LiteralPath $buildFile -Raw), "(?m)^version\s*=\s*'([^']+)'")
  if (!$versionMatch.Success) {
    Add-Failure "Could not read plugin version from $buildFile"
  } else {
    $version = $versionMatch.Groups[1].Value
    $jarPath = Join-Path $libsDir "observance-$version.jar"
    if (!(Test-Path $jarPath)) {
      Add-Failure "Plugin jar missing: $jarPath"
    } else {
      $jar = Get-Item -LiteralPath $jarPath
      $latestSource = Get-ChildItem -LiteralPath $srcRoot -Recurse -File |
        Sort-Object LastWriteTimeUtc -Descending |
        Select-Object -First 1
      $holdD05Source = Join-Path $RepoRoot "design\deep-hold-d05-shelf.json"
      if (Test-Path -LiteralPath $holdD05Source) {
        $d05Item = Get-Item -LiteralPath $holdD05Source
        if ($null -eq $latestSource -or $d05Item.LastWriteTimeUtc -gt $latestSource.LastWriteTimeUtc) {
          $latestSource = $d05Item
        }
      }
      if ($null -ne $latestSource -and $jar.LastWriteTimeUtc -lt $latestSource.LastWriteTimeUtc) {
        Add-Failure "Plugin jar is stale: $($jar.Name) older than $($latestSource.FullName)"
      }

      $zip = [System.IO.Compression.ZipFile]::OpenRead($jarPath)
      try {
        $pluginEntry = $zip.Entries | Where-Object { $_.FullName -eq "plugin.yml" } | Select-Object -First 1
        if ($null -eq $pluginEntry) {
          Add-Failure "Plugin jar does not contain plugin.yml at jar root"
        } else {
          $reader = New-Object System.IO.StreamReader($pluginEntry.Open())
          try {
            $pluginYml = $reader.ReadToEnd()
          } finally {
            $reader.Dispose()
          }
          if ($pluginYml -notmatch "(?m)^version:\s*['`"]?$([regex]::Escape($version))['`"]?\s*$") {
            Add-Failure "plugin.yml version inside jar does not match build.gradle version $version"
          }
        }
        if ($null -eq ($zip.Entries | Where-Object { $_.FullName -eq "config.yml" } | Select-Object -First 1)) {
          Add-Failure "Plugin jar does not contain config.yml"
        }
        if ($null -eq ($zip.Entries | Where-Object { $_.FullName -eq "sites.yml" } | Select-Object -First 1)) {
          Add-Failure "Plugin jar does not contain sites.yml"
        }
        if ($null -eq ($zip.Entries | Where-Object { $_.FullName -eq "deep-hold-lock-books.json" } | Select-Object -First 1)) {
          Add-Failure "Plugin jar does not contain deep-hold-lock-books.json"
        }
        if ($null -eq ($zip.Entries | Where-Object { $_.FullName -eq "deep-hold-d05-shelf.json" } | Select-Object -First 1)) {
          Add-Failure "Plugin jar does not contain deep-hold-d05-shelf.json"
        }
        if ($null -eq ($zip.Entries | Where-Object { $_.FullName -eq "com/observance/watcher/ObservancePlugin.class" } | Select-Object -First 1)) {
          Add-Failure "Plugin jar does not contain ObservancePlugin.class"
        }
      } finally {
        $zip.Dispose()
      }
    }
  }
}

if ($failures.Count -gt 0) {
  Write-Host "plugin jar check: FAILED"
  foreach ($failure in $failures) {
    Write-Host "  - $failure"
  }
  exit 1
}

$sha1 = (Get-FileHash -LiteralPath $jarPath -Algorithm SHA1).Hash.ToLowerInvariant()
Write-Host "plugin jar check: OK - $jarPath"
Write-Host "plugin jar sha1: $sha1"
