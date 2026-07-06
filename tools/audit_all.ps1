param(
  [string]$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
)

$ErrorActionPreference = "Stop"

$discord = Join-Path $RepoRoot "discord"
$dashboard = Join-Path $RepoRoot "dashboard"
$pluginCheck = Join-Path $RepoRoot "tools\check_plugin_compile.ps1"
$pluginJarCheck = Join-Path $RepoRoot "tools\check_plugin_jar.ps1"
$assetCheck = Join-Path $RepoRoot "tools\check_assets.ps1"
$brandSurfaceCheck = Join-Path $RepoRoot "tools\check_brand_surfaces.ps1"
$mediaCheck = Join-Path $RepoRoot "tools\check_media_readiness.ps1"
$operatorDocsCheck = Join-Path $RepoRoot "tools\check_operator_docs.ps1"
$rehearsalCheck = Join-Path $RepoRoot "tools\check_rehearsal_consistency.ps1"
$dialogueContractCheck = Join-Path $RepoRoot "tools\check_dialogue_contracts.ps1"
$scareImmersionCheck = Join-Path $RepoRoot "tools\check_scare_immersion.ps1"
$motifFreshnessCheck = Join-Path $RepoRoot "tools\check_motif_freshness.ps1"
$liveRehearsalCheck = Join-Path $RepoRoot "tools\check_live_rehearsal_evidence.ps1"
$launchPlacementCheck = Join-Path $RepoRoot "tools\check_launch_placement_packet.ps1"
$launchCoordQualityCheck = Join-Path $RepoRoot "tools\check_launch_coord_quality.ps1"
$worldBuildCheck = Join-Path $RepoRoot "tools\check_world_build_readiness.ps1"
$unlitReadinessCheck = Join-Path $RepoRoot "tools\check_unlit_readiness.ps1"

Push-Location $discord
try {
  npm.cmd run audit
  if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
  }
  npm.cmd run runtimecheck
  if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
  }
} finally {
Pop-Location
}

Push-Location $dashboard
try {
  npm.cmd run lint
  if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
  }
  npx.cmd tsc --noEmit --pretty false
  if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
  }
  npm.cmd run selftest
  if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
  }
  npm.cmd run build
  if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
  }
} finally {
  Pop-Location
}

& powershell -NoProfile -ExecutionPolicy Bypass -File $pluginCheck -RepoRoot $RepoRoot
if ($LASTEXITCODE -ne 0) {
  exit $LASTEXITCODE
}

& powershell -NoProfile -ExecutionPolicy Bypass -File $operatorDocsCheck -RepoRoot $RepoRoot
if ($LASTEXITCODE -ne 0) {
  exit $LASTEXITCODE
}

& powershell -NoProfile -ExecutionPolicy Bypass -File $dialogueContractCheck -RepoRoot $RepoRoot
if ($LASTEXITCODE -ne 0) {
  exit $LASTEXITCODE
}

& powershell -NoProfile -ExecutionPolicy Bypass -File $scareImmersionCheck -RepoRoot $RepoRoot
if ($LASTEXITCODE -ne 0) {
  exit $LASTEXITCODE
}

& powershell -NoProfile -ExecutionPolicy Bypass -File $motifFreshnessCheck -RepoRoot $RepoRoot
if ($LASTEXITCODE -ne 0) {
  exit $LASTEXITCODE
}

& powershell -NoProfile -ExecutionPolicy Bypass -File $rehearsalCheck -RepoRoot $RepoRoot
if ($LASTEXITCODE -ne 0) {
  exit $LASTEXITCODE
}

& powershell -NoProfile -ExecutionPolicy Bypass -File $liveRehearsalCheck -RepoRoot $RepoRoot
if ($LASTEXITCODE -ne 0) {
  exit $LASTEXITCODE
}

& powershell -NoProfile -ExecutionPolicy Bypass -File $launchPlacementCheck -RepoRoot $RepoRoot
if ($LASTEXITCODE -ne 0) {
  exit $LASTEXITCODE
}

& powershell -NoProfile -ExecutionPolicy Bypass -File $launchCoordQualityCheck -RepoRoot $RepoRoot
if ($LASTEXITCODE -ne 0) {
  exit $LASTEXITCODE
}

& powershell -NoProfile -ExecutionPolicy Bypass -File $worldBuildCheck -RepoRoot $RepoRoot
if ($LASTEXITCODE -ne 0) {
  exit $LASTEXITCODE
}

& powershell -NoProfile -ExecutionPolicy Bypass -File $unlitReadinessCheck -RepoRoot $RepoRoot
if ($LASTEXITCODE -ne 0) {
  exit $LASTEXITCODE
}

& powershell -NoProfile -ExecutionPolicy Bypass -File $pluginJarCheck -RepoRoot $RepoRoot
if ($LASTEXITCODE -ne 0) {
  exit $LASTEXITCODE
}

& powershell -NoProfile -ExecutionPolicy Bypass -File $assetCheck -RepoRoot $RepoRoot
if ($LASTEXITCODE -ne 0) {
  exit $LASTEXITCODE
}

& powershell -NoProfile -ExecutionPolicy Bypass -File $brandSurfaceCheck -RepoRoot $RepoRoot
if ($LASTEXITCODE -ne 0) {
  exit $LASTEXITCODE
}

& powershell -NoProfile -ExecutionPolicy Bypass -File $mediaCheck -RepoRoot $RepoRoot
if ($LASTEXITCODE -ne 0) {
  exit $LASTEXITCODE
}

Write-Host "observance audit: OK - story/data, showrunner, dashboard selftests/build, plugin source/jar, operator docs, dialogue contracts, scare immersion, motif freshness, rehearsal wiring/evidence, launch placement packet, launch coordinate quality, world-build gate, Unlit readiness, assets, brand surfaces, and media checks passed"
