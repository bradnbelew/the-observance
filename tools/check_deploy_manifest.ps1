param(
  [string]$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path,
  [string]$ManifestPath = "observance-deploy-manifest.json"
)

$ErrorActionPreference = "Stop"

function Resolve-UnderRepo([string]$Root, [string]$Path) {
  if ([System.IO.Path]::IsPathRooted($Path)) {
    return [System.IO.Path]::GetFullPath($Path)
  }
  return [System.IO.Path]::GetFullPath((Join-Path $Root $Path))
}

function Add-Failure([string]$Message) {
  $script:failures.Add($Message) | Out-Null
}

function Actual-Sha1([string]$Root, [string]$RelativePath) {
  if ([string]::IsNullOrWhiteSpace($RelativePath)) { return "" }
  $full = Resolve-UnderRepo $Root $RelativePath
  if (!$full.StartsWith($Root, [System.StringComparison]::OrdinalIgnoreCase)) {
    Add-Failure "Manifest path escapes repo: $RelativePath"
    return ""
  }
  if (!(Test-Path $full)) {
    Add-Failure "Manifest artifact missing on disk: $RelativePath"
    return ""
  }
  return (Get-FileHash -LiteralPath $full -Algorithm SHA1).Hash.ToLowerInvariant()
}

function Config-Value([string]$Text, [string]$Section, [string]$Key) {
  $pattern = "(?ms)^$([regex]::Escape($Section)):\s.*?^\s+$([regex]::Escape($Key)):\s+`"([^`"]*)`""
  $match = [regex]::Match($Text, $pattern)
  if ($match.Success) { return $match.Groups[1].Value.Trim() }
  return ""
}

$repoFull = [System.IO.Path]::GetFullPath($RepoRoot)
$manifestFull = Resolve-UnderRepo $repoFull $ManifestPath
$failures = [System.Collections.Generic.List[string]]::new()

if (!$manifestFull.StartsWith($repoFull, [System.StringComparison]::OrdinalIgnoreCase)) {
  throw "Deploy manifest path is outside repo: $manifestFull"
}
if (!(Test-Path $manifestFull)) {
  Add-Failure "Deploy manifest missing: run tools\write_deploy_manifest.ps1 after packaging plugin/assets."
} else {
  try {
    $manifest = Get-Content -LiteralPath $manifestFull -Raw | ConvertFrom-Json
  } catch {
    Add-Failure "Deploy manifest is invalid JSON: $($_.Exception.Message)"
    $manifest = $null
  }

  if ($null -ne $manifest) {
    foreach ($entry in @(
      @("plugin jar", $manifest.artifacts.pluginJar),
      @("datapack zip", $manifest.artifacts.datapackZip),
      @("resourcepack zip", $manifest.artifacts.resourcepackZip)
    )) {
      $label = $entry[0]
      $artifact = $entry[1]
      if ($null -eq $artifact) {
        Add-Failure "Deploy manifest missing artifact entry: $label"
        continue
      }
      if ($artifact.exists -ne $true) {
        Add-Failure "Deploy manifest records missing artifact: $label"
        continue
      }
      $actual = Actual-Sha1 $repoFull ([string]$artifact.path)
      $recorded = ([string]$artifact.sha1).ToLowerInvariant()
      if ($actual -ne "" -and $recorded -ne $actual) {
        Add-Failure "Deploy manifest stale for ${label}: manifest has $recorded, disk has $actual"
      }
    }

    $resourcepackArtifact = $manifest.artifacts.resourcepackZip
    if ($null -ne $resourcepackArtifact -and $resourcepackArtifact.exists -eq $true) {
      $resourceSha = ([string]$resourcepackArtifact.sha1).ToLowerInvariant()
      $configFile = Join-Path $repoFull "plugin\src\main\resources\config.yml"
      if (Test-Path $configFile) {
        $configText = Get-Content -LiteralPath $configFile -Raw
        $configUrl = Config-Value $configText "resource-pack" "url"
        $configSha = (Config-Value $configText "resource-pack" "sha1").ToLowerInvariant()
        if ($manifest.resourcePackConfig.url -ne $configUrl) {
          Add-Failure "Deploy manifest stale for resource-pack.url"
        }
        if ($manifest.resourcePackConfig.sha1 -ne $configSha) {
          Add-Failure "Deploy manifest stale for resource-pack.sha1"
        }
        if ($configSha -ne "" -and $configSha -ne $resourceSha) {
          Add-Failure "Deploy manifest resource-pack.sha1 does not match resourcepack zip"
        }
      }
    }

    $roots = @($manifest.packagingRules.resourcepackRuntimeRoots | ForEach-Object { [string]$_ })
    foreach ($requiredRoot in @("pack.mcmeta", "assets")) {
      if ($requiredRoot -notin $roots) {
        Add-Failure "Deploy manifest packaging rules missing runtime root: $requiredRoot"
      }
    }
  }
}

if ($failures.Count -gt 0) {
  Write-Host "deploy manifest check: FAILED"
  foreach ($failure in $failures) {
    Write-Host "  - $failure"
  }
  exit 1
}

Write-Host "deploy manifest check: OK - artifact hashes and resource-pack config snapshot match current files"
