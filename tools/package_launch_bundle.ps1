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

function Invoke-NativeStep([string]$Name, [string]$Executable, [string[]]$ArgsList) {
  Write-Host ""
  Write-Host "== $Name =="
  & $Executable @ArgsList
  if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
  }
}

function Invoke-ProjectStep([string]$Name, [string]$WorkingDirectory, [string]$Executable, [string[]]$ArgsList) {
  Write-Host ""
  Write-Host "== $Name =="
  Push-Location $WorkingDirectory
  try {
    & $Executable @ArgsList
    $code = $LASTEXITCODE
  } finally {
    Pop-Location
  }
  if ($code -ne 0) {
    exit $code
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
$discord = Join-Path $repoFull "discord"

Invoke-NativeStep "verify V5 freshness" "python" @((Join-Path $tools "check_v5_freshness.py"))
Invoke-NativeStep "verify P1-P12 ARG experience authority" "python" @((Join-Path $tools "check_arg_experience_authority.py"))
Invoke-NativeStep "verify ARG experience/input negative mutations" "python" @((Join-Path $tools "test_arg_experience_negative_contracts.py"))
Invoke-NativeStep "verify V5 content/runtime parity" "python" @((Join-Path $tools "check_v5_content.py"), "--runtime")
Invoke-NativeStep "verify V5 physical predicates" "python" @((Join-Path $tools "check_v5_physical_predicates.py"))
Invoke-NativeStep "verify V5 exact Minecraft map art" "python" @((Join-Path $tools "render_v5_map_art.py"))
Invoke-NativeStep "verify V5 Hold layout/runtime integration" "python" @((Join-Path $tools "check_deep_hold_layout.py"))
Invoke-NativeStep "simulate V5 failure/replay scenarios" "python" @((Join-Path $tools "simulate_v5_scenarios.py"))
Invoke-Step "verify V5 external media receipt" (Join-Path $tools "check_external_media_readiness.ps1") @("-RepoRoot", $repoFull)
Invoke-ProjectStep "regenerate V5 Supabase bundle" $discord "npm.cmd" @("run", "db:seed")
Invoke-ProjectStep "verify V5 Supabase bundle" $discord "npm.cmd" @("run", "db:bundlecheck")

Invoke-Step "package datapack and resource pack" (Join-Path $tools "package_assets.ps1") @("-RepoRoot", $repoFull)
Invoke-Step "verify datapack/resourcepack assets" (Join-Path $tools "check_assets.ps1") @("-RepoRoot", $repoFull)
Invoke-Step "package plugin jar after final resource-pack config" (Join-Path $tools "package_plugin.ps1") @("-RepoRoot", $repoFull)
Invoke-Step "verify plugin jar" (Join-Path $tools "check_plugin_jar.ps1") @("-RepoRoot", $repoFull)
Invoke-Step "verify deploy manifest" (Join-Path $tools "check_deploy_manifest.ps1") @("-RepoRoot", $repoFull)

$manifestPath = Join-Path $repoFull "observance-deploy-manifest.json"
$manifest = Read-Manifest $manifestPath

Write-Host ""
Write-Host "launch bundle ready:"
Write-Host "  plugin jar:      $($manifest.artifacts.pluginJar.path)"
Write-Host "  plugin sha1:     $($manifest.artifacts.pluginJar.sha1)"
Write-Host "  plugin sha256:   $($manifest.artifacts.pluginJar.sha256)"
Write-Host "  datapack zip:    $($manifest.artifacts.datapackZip.path)"
Write-Host "  datapack sha1:   $($manifest.artifacts.datapackZip.sha1)"
Write-Host "  datapack sha256: $($manifest.artifacts.datapackZip.sha256)"
Write-Host "  resource zip:    $($manifest.artifacts.resourcepackZip.path)"
Write-Host "  resource sha1:   $($manifest.artifacts.resourcepackZip.sha1)"
Write-Host "  resource sha256: $($manifest.artifacts.resourcepackZip.sha256)"
Write-Host "  Supabase SQL:    $($manifest.artifacts.supabaseSql.path)"
Write-Host "  SQL sha256:      $($manifest.artifacts.supabaseSql.sha256)"
Write-Host "  gated archive:   $($manifest.artifacts.holdArchive.path)"
Write-Host "  archive sha256:  $($manifest.artifacts.holdArchive.sha256)"
Write-Host "  deploy receipt:  observance-deploy-manifest.json"
Write-Host ""
Write-Host "next: verify the already-configured hosted resource bytes with tools\check_hosted_resource_pack.ps1, then upload this exact plugin JAR and install this exact datapack."
Write-Host "If the hosted URL or SHA-1 changes, run set_resource_pack_config.ps1 first and rerun this entire bundle; never upload the pre-update JAR."
