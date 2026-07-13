param(
  [string]$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
)

$ErrorActionPreference = "Stop"

$discord = Join-Path $RepoRoot "discord"
$dashboard = Join-Path $RepoRoot "dashboard"
$pluginCheck = Join-Path $RepoRoot "tools\check_plugin_compile.ps1"
$pluginPackage = Join-Path $RepoRoot "tools\package_plugin.ps1"
$pluginDbContractCheck = Join-Path $RepoRoot "tools\check_plugin_db_contracts.ps1"
$companionArcContractCheck = Join-Path $RepoRoot "tools\check_companion_arc_contracts.ps1"
$pluginJarCheck = Join-Path $RepoRoot "tools\check_plugin_jar.ps1"
$assetCheck = Join-Path $RepoRoot "tools\check_assets.ps1"
$deployManifestCheck = Join-Path $RepoRoot "tools\check_deploy_manifest.ps1"
$resourcePackConfigToolCheck = Join-Path $RepoRoot "tools\check_resource_pack_config_tools.ps1"
$friendLaunchPrepCheck = Join-Path $RepoRoot "tools\check_friend_launch_prep.ps1"
$serverTestPrepCheck = Join-Path $RepoRoot "tools\check_server_test_prep.ps1"
$brandSurfaceCheck = Join-Path $RepoRoot "tools\check_brand_surfaces.ps1"
$mediaCheck = Join-Path $RepoRoot "tools\check_media_readiness.ps1"
$externalMediaCheck = Join-Path $RepoRoot "tools\check_external_media_readiness.ps1"
$holdInvitationCheck = Join-Path $RepoRoot "tools\check_hold_invitation.ps1"
$operatorDocsCheck = Join-Path $RepoRoot "tools\check_operator_docs.ps1"
$minecraftTextCheck = Join-Path $RepoRoot "tools\check_minecraft_text_surfaces.ps1"
$rehearsalCheck = Join-Path $RepoRoot "tools\check_rehearsal_consistency.ps1"
$dialogueContractCheck = Join-Path $RepoRoot "tools\check_dialogue_contracts.ps1"
$scareImmersionCheck = Join-Path $RepoRoot "tools\check_scare_immersion.ps1"
$motifFreshnessCheck = Join-Path $RepoRoot "tools\check_motif_freshness.ps1"
$clueLedgerCheck = Join-Path $RepoRoot "tools\check_clue_ledger.ps1"
$structureQualityCheck = Join-Path $RepoRoot "tools\check_structure_quality.ps1"
$structureSurfaceIntegrityCheck = Join-Path $RepoRoot "tools\check_structure_surface_integrity.ps1"
$sideLoreCohesionCheck = Join-Path $RepoRoot "tools\check_side_lore_cohesion.ps1"
$keeperInvestigationCheck = Join-Path $RepoRoot "tools\check_keeper_investigations.ps1"
$customsRosettaCheck = Join-Path $RepoRoot "tools\check_customs_rosetta.ps1"
$directorToolsCheck = Join-Path $RepoRoot "tools\check_director_tools.ps1"
$directorConcernClosureCheck = Join-Path $RepoRoot "tools\check_director_concern_closure.ps1"
$liveRehearsalCheck = Join-Path $RepoRoot "tools\check_live_rehearsal_evidence.ps1"
$launchPlacementCheck = Join-Path $RepoRoot "tools\check_launch_placement_packet.ps1"
$launchCoordQualityCheck = Join-Path $RepoRoot "tools\check_launch_coord_quality.ps1"
$worldBuildCheck = Join-Path $RepoRoot "tools\check_world_build_readiness.ps1"
$unlitReadinessCheck = Join-Path $RepoRoot "tools\check_unlit_readiness.ps1"
$launchManualBlockerCheck = Join-Path $RepoRoot "tools\check_launch_manual_blockers.ps1"
$repositoryIntegrityCheck = Join-Path $RepoRoot "tools\check_repository_integrity.py"

python $repositoryIntegrityCheck
if ($LASTEXITCODE -ne 0) {
  exit $LASTEXITCODE
}

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
  npm.cmd run typecheck
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
  # Build before the standalone compiler pass so Next regenerates `.next/types`
  # from the current route tree. Otherwise routes removed since the previous build
  # can leave stale generated imports that make an unchanged source tree fail.
  npm.cmd run selftest
  if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
  }
  npm.cmd run build
  if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
  }
  npx.cmd tsc --noEmit --pretty false
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

# Several downstream launch/rehearsal validators inspect the deployable JAR.
# Build it before those checks so a clean checkout can pass the audit without a
# stale or manually prebuilt artifact.
& powershell -NoProfile -ExecutionPolicy Bypass -File $pluginPackage -RepoRoot $RepoRoot
if ($LASTEXITCODE -ne 0) {
  exit $LASTEXITCODE
}

& powershell -NoProfile -ExecutionPolicy Bypass -File $pluginDbContractCheck -RepoRoot $RepoRoot
if ($LASTEXITCODE -ne 0) {
  exit $LASTEXITCODE
}

& powershell -NoProfile -ExecutionPolicy Bypass -File $companionArcContractCheck -RepoRoot $RepoRoot
if ($LASTEXITCODE -ne 0) {
  exit $LASTEXITCODE
}

& powershell -NoProfile -ExecutionPolicy Bypass -File $operatorDocsCheck -RepoRoot $RepoRoot
if ($LASTEXITCODE -ne 0) {
  exit $LASTEXITCODE
}

& powershell -NoProfile -ExecutionPolicy Bypass -File $externalMediaCheck -RepoRoot $RepoRoot
if ($LASTEXITCODE -ne 0) {
  exit $LASTEXITCODE
}

& powershell -NoProfile -ExecutionPolicy Bypass -File $holdInvitationCheck -RepoRoot $RepoRoot
if ($LASTEXITCODE -ne 0) {
  exit $LASTEXITCODE
}

& powershell -NoProfile -ExecutionPolicy Bypass -File $minecraftTextCheck -RepoRoot $RepoRoot
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

& powershell -NoProfile -ExecutionPolicy Bypass -File $clueLedgerCheck -RepoRoot $RepoRoot
if ($LASTEXITCODE -ne 0) {
  exit $LASTEXITCODE
}

& powershell -NoProfile -ExecutionPolicy Bypass -File $structureQualityCheck -RepoRoot $RepoRoot
if ($LASTEXITCODE -ne 0) {
  exit $LASTEXITCODE
}

& powershell -NoProfile -ExecutionPolicy Bypass -File $structureSurfaceIntegrityCheck -RepoRoot $RepoRoot
if ($LASTEXITCODE -ne 0) {
  exit $LASTEXITCODE
}

& powershell -NoProfile -ExecutionPolicy Bypass -File $sideLoreCohesionCheck -RepoRoot $RepoRoot
if ($LASTEXITCODE -ne 0) {
  exit $LASTEXITCODE
}

& powershell -NoProfile -ExecutionPolicy Bypass -File $keeperInvestigationCheck -RepoRoot $RepoRoot
if ($LASTEXITCODE -ne 0) {
  exit $LASTEXITCODE
}

& powershell -NoProfile -ExecutionPolicy Bypass -File $customsRosettaCheck -RepoRoot $RepoRoot
if ($LASTEXITCODE -ne 0) {
  exit $LASTEXITCODE
}

& powershell -NoProfile -ExecutionPolicy Bypass -File $directorToolsCheck -RepoRoot $RepoRoot
if ($LASTEXITCODE -ne 0) {
  exit $LASTEXITCODE
}

& powershell -NoProfile -ExecutionPolicy Bypass -File $directorConcernClosureCheck -RepoRoot $RepoRoot
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

& powershell -NoProfile -ExecutionPolicy Bypass -File $launchManualBlockerCheck -RepoRoot $RepoRoot
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

& powershell -NoProfile -ExecutionPolicy Bypass -File $deployManifestCheck -RepoRoot $RepoRoot
if ($LASTEXITCODE -ne 0) {
  exit $LASTEXITCODE
}

& powershell -NoProfile -ExecutionPolicy Bypass -File $resourcePackConfigToolCheck -RepoRoot $RepoRoot
if ($LASTEXITCODE -ne 0) {
  exit $LASTEXITCODE
}

& powershell -NoProfile -ExecutionPolicy Bypass -File $friendLaunchPrepCheck -RepoRoot $RepoRoot
if ($LASTEXITCODE -ne 0) {
  exit $LASTEXITCODE
}

& powershell -NoProfile -ExecutionPolicy Bypass -File $serverTestPrepCheck -RepoRoot $RepoRoot
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

Write-Host "observance audit: OK - automated repo checks passed; see the launch manual blocker section above for live-server tasks that still prevent a true launch-ready verdict"
