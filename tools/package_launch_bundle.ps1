param(
  [string]$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
)

$ErrorActionPreference = "Stop"

function Invoke-Step([string]$Name, [string]$ScriptPath, [string[]]$ArgsList) {
  Write-Host ""
  Write-Host "== $Name =="
  & powershell -NoProfile -ExecutionPolicy Bypass -File $ScriptPath @ArgsList
  if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
  }
}

function Read-Manifest([string]$Path) {
  if (!(Test-Path $Path)) {
    throw "Deploy manifest was not created: $Path"
  }
  return Get-Content -LiteralPath $Path -Raw | ConvertFrom-Json
}

$repoFull = [System.IO.Path]::GetFullPath($RepoRoot)
$tools = Join-Path $repoFull "tools"

Invoke-Step "package plugin jar" (Join-Path $tools "package_plugin.ps1") @("-RepoRoot", $repoFull)
Invoke-Step "package datapack and resource pack" (Join-Path $tools "package_assets.ps1") @("-RepoRoot", $repoFull)
Invoke-Step "verify plugin jar" (Join-Path $tools "check_plugin_jar.ps1") @("-RepoRoot", $repoFull)
Invoke-Step "verify datapack/resourcepack assets" (Join-Path $tools "check_assets.ps1") @("-RepoRoot", $repoFull)
Invoke-Step "verify deploy manifest" (Join-Path $tools "check_deploy_manifest.ps1") @("-RepoRoot", $repoFull)

$manifestPath = Join-Path $repoFull "observance-deploy-manifest.json"
$manifest = Read-Manifest $manifestPath

Write-Host ""
Write-Host "launch bundle ready:"
Write-Host "  plugin jar:      $($manifest.artifacts.pluginJar.path)"
Write-Host "  plugin sha1:     $($manifest.artifacts.pluginJar.sha1)"
Write-Host "  datapack zip:    $($manifest.artifacts.datapackZip.path)"
Write-Host "  datapack sha1:   $($manifest.artifacts.datapackZip.sha1)"
Write-Host "  resource zip:    $($manifest.artifacts.resourcepackZip.path)"
Write-Host "  resource sha1:   $($manifest.artifacts.resourcepackZip.sha1)"
Write-Host "  deploy receipt:  observance-deploy-manifest.json"
Write-Host ""
Write-Host "next: upload the plugin jar, install the datapack, host the resource zip, set resource-pack.url/SHA1, then run tools\check_hosted_resource_pack.ps1."
