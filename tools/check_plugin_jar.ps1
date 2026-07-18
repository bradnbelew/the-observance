param(
  [string]$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
)

$ErrorActionPreference = "Stop"

Add-Type -AssemblyName System.IO.Compression.FileSystem

$pluginRoot = Join-Path $RepoRoot "plugin"
$buildFile = Join-Path $pluginRoot "build.gradle"
$srcRoot = Join-Path $pluginRoot "src"
$libsDir = Join-Path $pluginRoot "build\libs"

$authoritySources = [ordered]@{
  "v5/authority/ARG-V5-NODE-MANIFEST.csv" = (Join-Path $RepoRoot "design\ARG-V5-NODE-MANIFEST.csv")
  "v5/authority/ARG-V5-RUNTIME-BINDINGS.csv" = (Join-Path $RepoRoot "design\ARG-V5-RUNTIME-BINDINGS.csv")
  "v5/authority/ARG-V5-PHYSICAL-PREDICATES.json" = (Join-Path $RepoRoot "design\ARG-V5-PHYSICAL-PREDICATES.json")
  "v5/authority/ARG-V5-ROOM-ASSIGNMENTS.csv" = (Join-Path $RepoRoot "design\ARG-V5-ROOM-ASSIGNMENTS.csv")
  "v5/authority/ARG-V5-FIXTURE-OWNERSHIP.csv" = (Join-Path $RepoRoot "design\ARG-V5-FIXTURE-OWNERSHIP.csv")
  "v5/authority/DEEP-HOLD-GATE-MANIFEST.csv" = (Join-Path $RepoRoot "design\DEEP-HOLD-GATE-MANIFEST.csv")
  "v5/authority/ARG-V5-ARTIFACT-MANIFEST.csv" = (Join-Path $RepoRoot "design\ARG-V5-ARTIFACT-MANIFEST.csv")
  "v5/authority/ARG-V5-BOOK-PLACEMENT.csv" = (Join-Path $RepoRoot "design\ARG-V5-BOOK-PLACEMENT.csv")
  "v5/authority/ARG-V5-RECORD-OWNERSHIP.csv" = (Join-Path $RepoRoot "design\ARG-V5-RECORD-OWNERSHIP.csv")
  "v5/authority/DEEP-HOLD-RECORD-STATION-MANIFEST.csv" = (Join-Path $RepoRoot "design\DEEP-HOLD-RECORD-STATION-MANIFEST.csv")
  "v5/authority/minecraft-books.json" = (Join-Path $RepoRoot "arc\v5\minecraft-books.json")
  "v5/authority/npc-dialogue.json" = (Join-Path $RepoRoot "arc\v5\npc-dialogue.json")
  "v5/authority/media-manifest.json" = (Join-Path $RepoRoot "arc\v5\media-manifest.json")
  "v5/authority/evidence-item-text.json" = (Join-Path $RepoRoot "arc\v5\evidence-item-text.json")
  "v5/authority/evidence-item-appearance.json" = (Join-Path $RepoRoot "arc\v5\evidence-item-appearance.json")
  "v5/authority/map-art-manifest.json" = (Join-Path $RepoRoot "arc\v5\map-art-manifest.json")
}
$retiredResourceNames = @("deep-hold-d05-shelf.json", "deep-hold-lock-books.json")
$campaignProjectionPath = Join-Path $RepoRoot "plugin\src\main\resources\campaign\p5-p12.json"
$campaignBindingPath = Join-Path $RepoRoot "plugin\src\main\resources\campaign\p5-p12-minecraft-bindings.json"

$mapManifestPath = [string]$authoritySources["v5/authority/map-art-manifest.json"]
if (!(Test-Path -LiteralPath $mapManifestPath -PathType Leaf)) {
  throw "V5 map-art manifest is missing: $mapManifestPath"
}
$mapManifest = Get-Content -LiteralPath $mapManifestPath -Raw | ConvertFrom-Json
$mapRows = @($mapManifest.maps)
if ($mapRows.Count -ne 9) {
  throw "V5 map-art manifest must name exactly 9 maps; found $($mapRows.Count)"
}
$mapSourceDir = [System.IO.Path]::GetFullPath((Join-Path $RepoRoot "arc\v5\map-art"))
$manifestMapNames = New-Object System.Collections.Generic.List[string]
foreach ($map in $mapRows) {
  $manifestFile = [string]$map.file
  if ($manifestFile -notmatch '^arc/v5/map-art/([^/\\]+\.png)$') {
    throw "Unsafe or non-canonical V5 map-art path in manifest: $manifestFile"
  }
  $fileName = $Matches[1]
  if ($manifestMapNames.Contains($fileName)) {
    throw "Duplicate V5 map-art filename in manifest: $fileName"
  }
  $manifestMapNames.Add($fileName) | Out-Null
  $sourcePath = [System.IO.Path]::GetFullPath((Join-Path $mapSourceDir $fileName))
  if (!(Test-Path -LiteralPath $sourcePath -PathType Leaf) -or
      !([System.IO.Path]::GetDirectoryName($sourcePath)).Equals($mapSourceDir, [System.StringComparison]::OrdinalIgnoreCase)) {
    throw "Manifest-listed V5 map art is missing or escaped its source directory: $manifestFile"
  }
  $authoritySources["v5/authority/map-art/$fileName"] = $sourcePath
}
$diskMapNames = @(Get-ChildItem -LiteralPath $mapSourceDir -Filter "*.png" -File | ForEach-Object { $_.Name } | Sort-Object)
$expectedMapNames = @($manifestMapNames | Sort-Object)
if (($diskMapNames -join "`n") -ne ($expectedMapNames -join "`n")) {
  throw "V5 map-art directory differs from manifest. Manifest: $($expectedMapNames -join ', '); disk: $($diskMapNames -join ', ')"
}

$failures = New-Object System.Collections.Generic.List[string]
function Add-Failure([string]$Message) {
  $script:failures.Add($Message) | Out-Null
}

& python (Join-Path $RepoRoot "tools\check_v5_physical_predicates.py")
if ($LASTEXITCODE -ne 0) {
  Add-Failure "V5 physical predicate authority validation failed"
}

if (!(Test-Path $buildFile)) {
  Add-Failure "Plugin build.gradle not found: $buildFile"
} else {
  $versionMatch = [regex]::Match((Get-Content -LiteralPath $buildFile -Raw), "(?m)^version\s*=\s*'([^']+)'")
  if (!$versionMatch.Success) {
    Add-Failure "Could not read plugin version from $buildFile"
  } else {
    $version = $versionMatch.Groups[1].Value
    if ($version -ne "0.5.0") {
      Add-Failure "Production jar version must be 0.5.0; build.gradle declares $version"
    }
    $jarPath = Join-Path $libsDir "observance-$version.jar"
    $allDeployJars = @(Get-ChildItem -LiteralPath $libsDir -Filter "observance-*.jar" -File -ErrorAction SilentlyContinue)
    if ($allDeployJars.Count -ne 1 -or $allDeployJars[0].Name -ne "observance-0.5.0.jar") {
      Add-Failure "Expected exactly one deploy jar observance-0.5.0.jar; found $($allDeployJars.Name -join ', ')"
    }
    if (!(Test-Path $jarPath)) {
      Add-Failure "Plugin jar missing: $jarPath"
    } else {
      $jar = Get-Item -LiteralPath $jarPath
      $latestSource = Get-ChildItem -LiteralPath $srcRoot -Recurse -File |
        Sort-Object LastWriteTimeUtc -Descending |
        Select-Object -First 1
      foreach ($entryName in $authoritySources.Keys) {
        $authorityPath = [string]$authoritySources[$entryName]
        if (!(Test-Path -LiteralPath $authorityPath -PathType Leaf)) {
          Add-Failure "V5 authority source missing for ${entryName}: $authorityPath"
          continue
        }
        $authorityItem = Get-Item -LiteralPath $authorityPath
        if ($null -eq $latestSource -or $authorityItem.LastWriteTimeUtc -gt $latestSource.LastWriteTimeUtc) {
          $latestSource = $authorityItem
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
        $configEntry = $zip.Entries | Where-Object { $_.FullName -eq "config.yml" } | Select-Object -First 1
        if ($null -eq $configEntry) {
          Add-Failure "Plugin jar does not contain config.yml"
        } else {
          $reader = New-Object System.IO.StreamReader($configEntry.Open())
          try {
            $packagedConfig = $reader.ReadToEnd()
          } finally {
            $reader.Dispose()
          }
          if ($packagedConfig -notmatch '(?im)^\s{2}discord-invite-url:\s*["'']https://(?:discord\.gg|discord\.com/invite)/[A-Za-z0-9_-]+["'']\s*(?:#.*)?$') {
            Add-Failure "Plugin jar config.yml does not contain a valid nonblank HTTPS Discord invitation"
          }
        }
        if ($null -eq ($zip.Entries | Where-Object { $_.FullName -eq "sites.yml" } | Select-Object -First 1)) {
          Add-Failure "Plugin jar does not contain sites.yml"
        }
        $campaignEntry = $zip.Entries | Where-Object { $_.FullName -eq "campaign/p5-p12.json" } | Select-Object -First 1
        if ($null -eq $campaignEntry) {
          Add-Failure "Plugin jar does not contain campaign/p5-p12.json"
        } elseif (!(Test-Path -LiteralPath $campaignProjectionPath -PathType Leaf)) {
          Add-Failure "Plugin campaign projection source is missing: $campaignProjectionPath"
        } else {
          $entryStream = $campaignEntry.Open()
          try {
            $sha = [System.Security.Cryptography.SHA256]::Create()
            try { $entryHash = ([BitConverter]::ToString($sha.ComputeHash($entryStream))).Replace("-", "").ToLowerInvariant() }
            finally { $sha.Dispose() }
          } finally { $entryStream.Dispose() }
          $sourceHash = (Get-FileHash -LiteralPath $campaignProjectionPath -Algorithm SHA256).Hash.ToLowerInvariant()
          if ($entryHash -ne $sourceHash) { Add-Failure "Plugin jar campaign projection differs from source" }
        }
        $bindingEntry = $zip.Entries | Where-Object { $_.FullName -eq "campaign/p5-p12-minecraft-bindings.json" } | Select-Object -First 1
        if ($null -eq $bindingEntry) {
          Add-Failure "Plugin jar does not contain campaign/p5-p12-minecraft-bindings.json"
        } elseif (!(Test-Path -LiteralPath $campaignBindingPath -PathType Leaf)) {
          Add-Failure "Plugin campaign Minecraft binding source is missing: $campaignBindingPath"
        } else {
          $entryStream = $bindingEntry.Open()
          try {
            $sha = [System.Security.Cryptography.SHA256]::Create()
            try { $entryHash = ([BitConverter]::ToString($sha.ComputeHash($entryStream))).Replace("-", "").ToLowerInvariant() }
            finally { $sha.Dispose() }
          } finally { $entryStream.Dispose() }
          $sourceHash = (Get-FileHash -LiteralPath $campaignBindingPath -Algorithm SHA256).Hash.ToLowerInvariant()
          if ($entryHash -ne $sourceHash) { Add-Failure "Plugin jar campaign Minecraft bindings differ from source" }
        }
        $entryNames = @($zip.Entries | ForEach-Object { $_.FullName })
        # Gradle/JAR may emit structural directory records. Authority parity is exact over files;
        # folders carry no bytes and are neither owned resources nor permissible file extras.
        $actualAuthorityEntries = @($zip.Entries |
          Where-Object {
            !$_.FullName.EndsWith("/", [System.StringComparison]::Ordinal) -and
            $_.FullName.StartsWith("v5/authority/", [System.StringComparison]::Ordinal)
          } |
          ForEach-Object { $_.FullName } |
          Sort-Object)
        $expectedAuthorityEntries = @($authoritySources.Keys | Sort-Object)
        $unexpectedAuthorityEntries = @($actualAuthorityEntries | Where-Object { $_ -notin $expectedAuthorityEntries })
        $missingAuthorityEntries = @($expectedAuthorityEntries | Where-Object { $_ -notin $actualAuthorityEntries })
        if ($unexpectedAuthorityEntries.Count -gt 0) {
          Add-Failure "Plugin jar contains unowned V5 authority entries: $($unexpectedAuthorityEntries -join ', ')"
        }
        if ($missingAuthorityEntries.Count -gt 0) {
          Add-Failure "Plugin jar is missing V5 authority entries: $($missingAuthorityEntries -join ', ')"
        }
        foreach ($entryName in $authoritySources.Keys) {
          $matchingAuthorityEntries = @($zip.Entries | Where-Object { $_.FullName -eq $entryName })
          if ($matchingAuthorityEntries.Count -ne 1) {
            if ($matchingAuthorityEntries.Count -gt 1) {
              Add-Failure "Plugin jar contains duplicate V5 authority entry: $entryName"
            }
            $authorityEntry = $null
          } else {
            $authorityEntry = $matchingAuthorityEntries[0]
          }
          if ($null -eq $authorityEntry) {
            Add-Failure "Plugin jar does not contain V5 authority $entryName"
          } else {
            $entryStream = $authorityEntry.Open()
            try {
              $sha = [System.Security.Cryptography.SHA256]::Create()
              try {
                $entryHash = ([System.BitConverter]::ToString($sha.ComputeHash($entryStream))).Replace("-", "").ToLowerInvariant()
              } finally {
                $sha.Dispose()
              }
            } finally {
              $entryStream.Dispose()
            }
            $sourceHash = (Get-FileHash -LiteralPath ([string]$authoritySources[$entryName]) -Algorithm SHA256).Hash.ToLowerInvariant()
            if ($entryHash -ne $sourceHash) {
              Add-Failure "Plugin jar V5 authority differs from source: $entryName"
            }
          }
        }
        foreach ($retiredName in $retiredResourceNames) {
          if ($entryNames | Where-Object { $_ -eq $retiredName -or $_ -like "*/$retiredName" }) {
            Add-Failure "Plugin jar contains retired V4 resource $retiredName"
          }
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
