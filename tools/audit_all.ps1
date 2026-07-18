param(
  [string]$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path,
  [switch]$SkipLiveExternalMedia,
  [switch]$SkipLiveHostedResourcePack
)

$ErrorActionPreference = "Stop"

function Invoke-External(
  [string]$Name,
  [string]$WorkingDirectory,
  [string]$Executable,
  [string[]]$Arguments
) {
  Write-Host ""
  Write-Host "== $Name =="
  Push-Location $WorkingDirectory
  try {
    & $Executable @Arguments
    $code = $LASTEXITCODE
  } finally {
    Pop-Location
  }
  if ($code -ne 0) {
    throw "$Name failed with exit code $code"
  }
}

$root = [System.IO.Path]::GetFullPath($RepoRoot)
$tools = Join-Path $root "tools"
$discord = Join-Path $root "discord"
$dashboard = Join-Path $root "dashboard"
$plugin = Join-Path $root "plugin"

foreach ($required in @(
  (Join-Path $tools "check_repository_integrity.py"),
  (Join-Path $tools "check_v5_freshness.py"),
  (Join-Path $tools "check_v5_content.py"),
  (Join-Path $tools "check_v5_physical_predicates.py"),
  (Join-Path $tools "check_m3_vertical_slice.py"),
  (Join-Path $tools "check_m3_v3_revision.py"),
  (Join-Path $tools "check_m3_v4_revision.py"),
  (Join-Path $tools "check_player_facing_experience_standard.py"),
  (Join-Path $tools "check_supabase_security_proposal.py"),
  (Join-Path $tools "check_arg_experience_authority.py"),
  (Join-Path $tools "test_arg_experience_negative_contracts.py"),
  (Join-Path $tools "check_arg_vertical_slice.py"),
  (Join-Path $tools "check_p5_p12_authored_candidate.py"),
  (Join-Path $tools "check_p5_p12_projection.py"),
  (Join-Path $tools "check_p5_p12_minecraft_bindings.py"),
  (Join-Path $tools "simulate_p5_p12_campaign.py"),
  (Join-Path $tools "sim_m3_vertical_slice.py"),
  (Join-Path $tools "check_hold_invitation.ps1"),
  (Join-Path $tools "render_v5_map_art.py"),
  (Join-Path $tools "check_deep_hold_layout.py"),
  (Join-Path $tools "simulate_v5_scenarios.py"),
  (Join-Path $tools "package_assets.ps1"),
  (Join-Path $tools "check_assets.ps1"),
  (Join-Path $tools "check_resource_pack_config_tools.ps1"),
  (Join-Path $tools "check_hosted_resource_pack.ps1"),
  (Join-Path $tools "publish_resource_pack_to_supabase.ps1"),
  (Join-Path $tools "backup_supabase_v5.ps1"),
  (Join-Path $tools "check_external_media_readiness.ps1"),
  (Join-Path $plugin "gradlew.bat")
)) {
  if (-not (Test-Path -LiteralPath $required -PathType Leaf)) {
    throw "V5 audit prerequisite is missing: $required"
  }
}

# Repository and authority checks run before any build so malformed or stale inputs
# cannot be copied into SQL, JAR, datapack, or resource-pack outputs.
Invoke-External "repository integrity (source)" $root "python" @((Join-Path $tools "check_repository_integrity.py"))
Invoke-External "V5 freshness and supersession" $root "python" @((Join-Path $tools "check_v5_freshness.py"))
Invoke-External "V5 canonical/runtime content" $root "python" @((Join-Path $tools "check_v5_content.py"), "--runtime")
Invoke-External "V5 executable physical predicates" $root "python" @((Join-Path $tools "check_v5_physical_predicates.py"))
Invoke-External "M3 private-slice authority/security" $root "python" @((Join-Path $tools "check_m3_vertical_slice.py"))
Invoke-External "M3 v3 authored revision/receipt gate" $root "python" @((Join-Path $tools "check_m3_v3_revision.py"))
Invoke-External "M3 v4 content-dependent revision/receipt gate" $root "python" @((Join-Path $tools "check_m3_v4_revision.py"))
Invoke-External "cross-phase player-facing experience authority" $root "python" @((Join-Path $tools "check_player_facing_experience_standard.py"))
Invoke-External "Supabase production-baseline hardening proposal" $root "python" @((Join-Path $tools "check_supabase_security_proposal.py"))
Invoke-External "research-based P1-P12 ARG experience authority" $root "python" @((Join-Path $tools "check_arg_experience_authority.py"))
Invoke-External "ARG experience/input negative contract mutations" $root "python" @((Join-Path $tools "test_arg_experience_negative_contracts.py"))
Invoke-External "P4-P5 real-input ARG vertical slice" $root "python" @((Join-Path $tools "check_arg_vertical_slice.py"))
Invoke-External "P5-P12 authored content scaffolding" $root "python" @((Join-Path $tools "check_p5_p12_authored_candidate.py"))
Invoke-External "P5-P12 Paper/web/Discord projection parity" $root "python" @((Join-Path $tools "check_p5_p12_projection.py"))
Invoke-External "P5-P12 authored-to-Deep-Hold Minecraft bindings" $root "python" @((Join-Path $tools "check_p5_p12_minecraft_bindings.py"))
Invoke-External "P5-P12 deterministic campaign simulation" $root "python" @((Join-Path $tools "simulate_p5_p12_campaign.py"))
Invoke-External "M3 private-slice block reachability" $root "python" @((Join-Path $tools "sim_m3_vertical_slice.py"))
Invoke-External "playable Hold and private Discord handoff" $root "powershell" @(
  "-NoProfile", "-ExecutionPolicy", "Bypass", "-File",
  (Join-Path $tools "check_hold_invitation.ps1"), "-RepoRoot", $root
)
Invoke-External "V5 exact Minecraft map art" $root "python" @((Join-Path $tools "render_v5_map_art.py"))
Invoke-External "V5 Deep Hold layout/runtime integration" $root "python" @((Join-Path $tools "check_deep_hold_layout.py"))
Invoke-External "V5 deterministic failure simulation" $root "python" @((Join-Path $tools "simulate_v5_scenarios.py"))

# Generate the database bundle from source before testing it. Never audit a hand-edited
# or stale apply-all.sql file.
Invoke-External "Discord V5 SQL bundle" $discord "npm.cmd" @("run", "db:seed")
Invoke-External "Discord V5 audit" $discord "npm.cmd" @("run", "audit")
Invoke-External "Discord V5 runtime tests" $discord "npm.cmd" @("run", "runtimecheck")
Invoke-External "Discord TypeScript" $discord "npm.cmd" @("run", "typecheck")
Invoke-External "resource-pack glyph generation" $discord "npm.cmd" @("run", "pack:build")
Invoke-External "resource-pack glyph proof" $discord "npm.cmd" @("run", "pack:proof")

Invoke-External "dashboard lint" $dashboard "npm.cmd" @("run", "lint")
Invoke-External "dashboard V5 self-tests" $dashboard "npm.cmd" @("run", "selftest")
Invoke-External "dashboard production build" $dashboard "npm.cmd" @("run", "build")

# Gradle clean is deliberate: it prevents an older Observance JAR from surviving in
# build/libs and being mistaken for the V5 deployable.
Invoke-External "Paper plugin clean/check/build" $plugin (Join-Path $plugin "gradlew.bat") @("clean", "check", "build", "--no-daemon")
Invoke-External "V5 plugin JAR contents" $root "powershell" @(
  "-NoProfile", "-ExecutionPolicy", "Bypass", "-File",
  (Join-Path $tools "check_plugin_jar.ps1"), "-RepoRoot", $root
)

# Package only after every source/project build succeeds, then read the exact ZIP/JAR
# bytes back through the release validators.
Invoke-External "deterministic datapack/resource-pack packaging" $root "powershell" @(
  "-NoProfile", "-ExecutionPolicy", "Bypass", "-File",
  (Join-Path $tools "package_assets.ps1"), "-RepoRoot", $root
)
Invoke-External "V5 datapack/resource-pack assets" $root "powershell" @(
  "-NoProfile", "-ExecutionPolicy", "Bypass", "-File",
  (Join-Path $tools "check_assets.ps1"), "-RepoRoot", $root
)
Invoke-External "V5 deploy manifest" $root "powershell" @(
  "-NoProfile", "-ExecutionPolicy", "Bypass", "-File",
  (Join-Path $tools "check_deploy_manifest.ps1"), "-RepoRoot", $root
)
Invoke-External "resource-pack config tools" $root "powershell" @(
  "-NoProfile", "-ExecutionPolicy", "Bypass", "-File",
  (Join-Path $tools "check_resource_pack_config_tools.ps1"), "-RepoRoot", $root
)
Invoke-External "resource-pack publisher self-test" $root "powershell" @(
  "-NoProfile", "-ExecutionPolicy", "Bypass", "-File",
  (Join-Path $tools "publish_resource_pack_to_supabase.ps1"), "-RepoRoot", $root, "-SelfTest"
)
Invoke-External "Supabase backup self-test" $root "powershell" @(
  "-NoProfile", "-ExecutionPolicy", "Bypass", "-File",
  (Join-Path $tools "backup_supabase_v5.ps1"), "-RepoRoot", $root, "-SelfTest"
)

if (-not $SkipLiveHostedResourcePack) {
  Invoke-External "hosted resource-pack exact bytes" $root "powershell" @(
    "-NoProfile", "-ExecutionPolicy", "Bypass", "-File",
    (Join-Path $tools "check_hosted_resource_pack.ps1"), "-RepoRoot", $root
  )
} else {
  Write-Warning "Hosted resource-pack verification was explicitly skipped. This run is not a production launch receipt."
}

Invoke-External "V5 external media manifest/receipt" $root "powershell" @(
  "-NoProfile", "-ExecutionPolicy", "Bypass", "-File",
  (Join-Path $tools "check_external_media_readiness.ps1"), "-RepoRoot", $root
)
if (-not $SkipLiveExternalMedia) {
  Invoke-External "V5 external media live sources" $root "powershell" @(
    "-NoProfile", "-ExecutionPolicy", "Bypass", "-File",
    (Join-Path $tools "check_external_media_readiness.ps1"), "-RepoRoot", $root, "-Live"
  )
} else {
  Write-Warning "Live external-media verification was explicitly skipped. This run is not a production launch receipt."
}

Invoke-External "repository integrity (release outputs)" $root "python" @((Join-Path $tools "check_repository_integrity.py"))

Write-Host ""
if ($SkipLiveExternalMedia -or $SkipLiveHostedResourcePack) {
  Write-Host "V5 audit: PASS (static/build checks passed; one or more live checks deliberately skipped)"
} else {
  Write-Host "V5 audit: PASS (repository, 82-node content, predicates, Hold, simulation, all projects, deploy artifacts, hosted pack, and live media)"
}
Write-Host "This does not replace the Paper/client/world/service receipts in design/V5-LIVE-TEST-MATRIX.csv."
