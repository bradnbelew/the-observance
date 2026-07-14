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

function Test-WithinRepo([string]$Root, [string]$Path) {
  $rootFull = [System.IO.Path]::GetFullPath($Root).TrimEnd('\', '/')
  $pathFull = [System.IO.Path]::GetFullPath($Path)
  $prefix = $rootFull + [System.IO.Path]::DirectorySeparatorChar
  return $pathFull.Equals($rootFull, [System.StringComparison]::OrdinalIgnoreCase) -or
    $pathFull.StartsWith($prefix, [System.StringComparison]::OrdinalIgnoreCase)
}

function Add-Failure([string]$Message) {
  $script:failures.Add($Message) | Out-Null
}

function Actual-Hash([string]$Root, [string]$RelativePath, [string]$Algorithm) {
  if ([string]::IsNullOrWhiteSpace($RelativePath)) { return "" }
  $full = Resolve-UnderRepo $Root $RelativePath
  if (!(Test-WithinRepo $Root $full)) {
    Add-Failure "Manifest path escapes repo: $RelativePath"
    return ""
  }
  if (!(Test-Path -LiteralPath $full -PathType Leaf)) {
    Add-Failure "Manifest artifact missing on disk: $RelativePath"
    return ""
  }
  return (Get-FileHash -LiteralPath $full -Algorithm $Algorithm).Hash.ToLowerInvariant()
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
$manifestFull = Resolve-UnderRepo $repoFull $ManifestPath
$failures = [System.Collections.Generic.List[string]]::new()

if (!(Test-WithinRepo $repoFull $manifestFull)) {
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
    if ([int]$manifest.schemaVersion -ne 2) {
      Add-Failure "Deploy manifest schemaVersion must be 2"
    }
    if ([string]$manifest.minecraftTarget -ne "Paper 1.21.11") {
      Add-Failure "Deploy manifest Minecraft target must be Paper 1.21.11"
    }
    if ([string]$manifest.pluginVersion -ne "0.5.0") {
      Add-Failure "Deploy manifest plugin version must be 0.5.0"
    }
    $currentCommit = (& git -C $repoFull rev-parse HEAD 2>$null | Out-String).Trim()
    if ($LASTEXITCODE -ne 0 -or [string]$manifest.source.gitCommit -ne $currentCommit) {
      Add-Failure "Deploy manifest source commit does not match the current checkout"
    }
    $currentBranch = (& git -C $repoFull branch --show-current 2>$null | Out-String).Trim()
    if ($LASTEXITCODE -ne 0 -or [string]$manifest.source.gitBranch -ne $currentBranch) {
      Add-Failure "Deploy manifest source branch does not match the current checkout"
    }
    foreach ($entry in @(
      @("plugin jar", $manifest.artifacts.pluginJar),
      @("datapack zip", $manifest.artifacts.datapackZip),
      @("resourcepack zip", $manifest.artifacts.resourcepackZip),
      @("Supabase SQL bundle", $manifest.artifacts.supabaseSql),
      @("gated Hold archive", $manifest.artifacts.holdArchive),
      @("Hold archive checksum", $manifest.deploymentInputs.holdArchiveSha1),
      @("Supabase verification SQL", $manifest.deploymentInputs.supabaseVerificationSql),
      @("Render Blueprint", $manifest.deploymentInputs.renderBlueprint),
      @("Vercel ignore policy", $manifest.deploymentInputs.vercelIgnore),
      @("dashboard lockfile", $manifest.deploymentInputs.dashboardLockfile),
      @("Discord lockfile", $manifest.deploymentInputs.discordLockfile)
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
      if ([string]$artifact.sha1 -notmatch '^[0-9a-f]{40}$' -or
          [string]$artifact.sha256 -notmatch '^[0-9a-f]{64}$' -or
          [long]$artifact.bytes -le 0) {
        Add-Failure "Deploy manifest has invalid hash/size metadata for ${label}"
      }
      $actualSha1 = Actual-Hash $repoFull ([string]$artifact.path) "SHA1"
      $recordedSha1 = ([string]$artifact.sha1).ToLowerInvariant()
      if ($actualSha1 -ne "" -and $recordedSha1 -ne $actualSha1) {
        Add-Failure "Deploy manifest stale for ${label} SHA-1: manifest has $recordedSha1, disk has $actualSha1"
      }
      $actualSha256 = Actual-Hash $repoFull ([string]$artifact.path) "SHA256"
      $recordedSha256 = ([string]$artifact.sha256).ToLowerInvariant()
      if ($actualSha256 -ne "" -and $recordedSha256 -ne $actualSha256) {
        Add-Failure "Deploy manifest stale for ${label} SHA-256: manifest has $recordedSha256, disk has $actualSha256"
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
        $configRequired = Config-Bool $configText "resource-pack" "required"
        $configPrompt = Config-Value $configText "resource-pack" "prompt"
        if ($manifest.resourcePackConfig.url -ne $configUrl) {
          Add-Failure "Deploy manifest stale for resource-pack.url"
        }
        if ($manifest.resourcePackConfig.sha1 -ne $configSha) {
          Add-Failure "Deploy manifest stale for resource-pack.sha1"
        }
        if ($configSha -ne "" -and $configSha -ne $resourceSha) {
          Add-Failure "Deploy manifest resource-pack.sha1 does not match resourcepack zip"
        }
        $configUri = $null
        if (-not [System.Uri]::TryCreate($configUrl, [System.UriKind]::Absolute, [ref]$configUri) -or
            $configUri.Scheme -ne "https" -or $configUri.AbsolutePath -notmatch '(?i)\.zip$' -or
            -not [string]::IsNullOrEmpty($configUri.UserInfo)) {
          Add-Failure "Production resource-pack.url must be a credential-free direct HTTPS .zip URL"
        }
        if ($configSha -notmatch '^[0-9a-f]{40}$') {
          Add-Failure "Production resource-pack.sha1 must be 40 lowercase hexadecimal characters"
        }
        if (-not $configRequired -or $manifest.resourcePackConfig.required -ne $true) {
          Add-Failure "Production resource pack must be required"
        }
        if ([string]::IsNullOrWhiteSpace($configPrompt)) {
          Add-Failure "Production resource-pack prompt is blank"
        }
      }
    }

    $archive = $manifest.artifacts.holdArchive
    $archiveReceipt = $manifest.deploymentInputs.holdArchiveSha1
    if ($null -ne $archive -and $null -ne $archiveReceipt -and
        $archive.exists -eq $true -and $archiveReceipt.exists -eq $true) {
      $receiptPath = Resolve-UnderRepo $repoFull ([string]$archiveReceipt.path)
      $receiptText = Get-Content -LiteralPath $receiptPath -Raw
      $declared = [regex]::Match($receiptText, '(?i)^\s*([0-9a-f]{40})(?:\s+\*?the-hold\.zip)?\s*$').Groups[1].Value.ToLowerInvariant()
      if ($declared -eq "" -or $declared -ne ([string]$archive.sha1).ToLowerInvariant()) {
        Add-Failure "Gated Hold archive checksum receipt does not match the archived bytes"
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

Write-Host "deploy manifest check: OK - plugin, packs, SQL, gated archive, service inputs, source commit, and required-pack config match"
