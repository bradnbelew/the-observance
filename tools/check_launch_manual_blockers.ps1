param(
  [string]$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path,
  [switch]$Launch,
  [string]$CaptureCsv = "",
  [string]$RehearsalPacket = ""
)

$ErrorActionPreference = "Stop"

function Resolve-UnderRepo([string]$Root, [string]$Path) {
  if ([string]::IsNullOrWhiteSpace($Path)) { return "" }
  if ([System.IO.Path]::IsPathRooted($Path)) {
    return [System.IO.Path]::GetFullPath($Path)
  }
  return [System.IO.Path]::GetFullPath((Join-Path $Root $Path))
}

function Add-Blocker([string]$Message) {
  $script:blockers.Add($Message) | Out-Null
}

function Add-Manual([string]$Message) {
  $script:manual.Add($Message) | Out-Null
}

function Clean([string]$Value) {
  if ($null -eq $Value) { return "" }
  return ($Value -replace '\s+#.*$', '').Trim().Trim('"')
}

function SectionValue([string]$Text, [string]$Section, [string]$Key) {
  $pattern = "(?ms)^$([regex]::Escape($Section)):\s.*?^\s+$([regex]::Escape($Key)):\s+`"([^`"]*)`""
  $match = [regex]::Match($Text, $pattern)
  if ($match.Success) { return Clean $match.Groups[1].Value }
  return ""
}

function Run-Check([string]$Name, [string]$ScriptPath, [string[]]$CheckArgs) {
  $output = & powershell -NoProfile -ExecutionPolicy Bypass -File $ScriptPath @CheckArgs 2>&1
  $exit = $LASTEXITCODE
  if ($exit -ne 0) {
    $lines = @($output | ForEach-Object { [string]$_ } | Where-Object { -not [string]::IsNullOrWhiteSpace($_) })
    if ($lines.Count -eq 0) {
      Add-Blocker "$Name failed with exit code $exit"
      return
    }
    Add-Blocker "$Name failed: $($lines[0].Trim())"
    foreach ($line in ($lines | Select-Object -Skip 1)) {
      $trimmed = $line.Trim()
      if ($trimmed.StartsWith("- ")) {
        Add-Blocker "${Name}: $($trimmed.Substring(2))"
      }
    }
  }
}

$repoFull = [System.IO.Path]::GetFullPath($RepoRoot)
$blockers = [System.Collections.Generic.List[string]]::new()
$manual = [System.Collections.Generic.List[string]]::new()

$configFile = Join-Path $repoFull "plugin\src\main\resources\config.yml"
$resourceZip = Join-Path $repoFull "observance-resourcepack.zip"
$worldCheck = Join-Path $repoFull "tools\check_world_build_readiness.ps1"
$coordCheck = Join-Path $repoFull "tools\check_launch_coord_quality.ps1"
$rehearsalCheck = Join-Path $repoFull "tools\check_rehearsal_packet.ps1"
$runbookFile = Join-Path $repoFull "design\RUNBOOK.md"
$manualPlanFile = Join-Path $repoFull "design\MANUAL-LAUNCH-PLAN.md"
$sessionZeroFile = Join-Path $repoFull "design\SESSION-ZERO.md"
$readinessVerdictFile = Join-Path $repoFull "design\CURRENT-READINESS-VERDICT.md"
$resourcePackSetter = Join-Path $repoFull "tools\set_resource_pack_config.ps1"
$launchBundlePackager = Join-Path $repoFull "tools\package_launch_bundle.ps1"

foreach ($file in @($configFile, $resourceZip, $worldCheck, $coordCheck, $rehearsalCheck, $runbookFile, $manualPlanFile, $sessionZeroFile, $readinessVerdictFile, $resourcePackSetter, $launchBundlePackager)) {
  if (-not (Test-Path $file)) {
    throw "launch manual blocker check: missing required file: $file"
  }
}

$config = Get-Content -LiteralPath $configFile -Raw
$resourceUrl = SectionValue $config "resource-pack" "url"
$resourceSha = SectionValue $config "resource-pack" "sha1"
$actualSha = (Get-FileHash -Algorithm SHA1 -LiteralPath $resourceZip).Hash.ToLowerInvariant()

if ([string]::IsNullOrWhiteSpace($resourceUrl)) {
  Add-Blocker "Resource pack is built but not launch-configured: host observance-resourcepack.zip, then run tools\set_resource_pack_config.ps1 -Url <hosted-https-zip-url>."
} else {
  $resourceUri = $null
  $validUri = [System.Uri]::TryCreate($resourceUrl, [System.UriKind]::Absolute, [ref]$resourceUri)
  if (-not $validUri -or $resourceUri.Scheme -ne "https") {
    Add-Blocker "Resource pack URL must be an absolute HTTPS URL to the hosted observance-resourcepack.zip; config has '$resourceUrl'."
  }
}
if ([string]::IsNullOrWhiteSpace($resourceSha)) {
  Add-Blocker "Resource pack SHA1 is blank: run tools\set_resource_pack_config.ps1 -Url <hosted-https-zip-url> so resource-pack.sha1 is set to $actualSha from the exact zip."
} elseif ($resourceSha -notmatch '^[0-9a-f]{40}$') {
  Add-Blocker "Resource pack SHA1 must be the 40-character lowercase hex SHA1 of observance-resourcepack.zip; config has '$resourceSha'."
} elseif ($resourceSha.ToLowerInvariant() -ne $actualSha) {
  Add-Blocker "Resource pack SHA1 mismatch: config has $resourceSha, but observance-resourcepack.zip is $actualSha."
}

$worldArgs = @("-RepoRoot", $repoFull)
if ($Launch) { $worldArgs += "-Launch" }
Run-Check "world build readiness" $worldCheck $worldArgs

if ([string]::IsNullOrWhiteSpace($CaptureCsv)) {
  if ($Launch) {
    Add-Blocker "No launch coords capture CSV was supplied. Run tools\new_launch_placement_packet.ps1, fill coords-capture.csv from live server placement, then rerun this check with -CaptureCsv."
  } else {
    Add-Blocker "Launch coordinate proof CSV not supplied; audit mode did not validate proof-shot fields. Use -CaptureCsv for a real placement packet."
  }
} else {
  $captureFull = Resolve-UnderRepo $repoFull $CaptureCsv
  if (-not $captureFull.StartsWith($repoFull, [System.StringComparison]::OrdinalIgnoreCase)) {
    Add-Blocker "Capture CSV is outside the repo: $captureFull"
  } elseif (-not (Test-Path $captureFull)) {
    Add-Blocker "Capture CSV not found: $captureFull"
  } else {
    $coordArgs = @("-RepoRoot", $repoFull, "-CaptureCsv", $captureFull)
    if ($Launch) { $coordArgs += "-Launch" }
    Run-Check "launch coordinate quality" $coordCheck $coordArgs
  }
}

if ([string]::IsNullOrWhiteSpace($RehearsalPacket)) {
  if ($Launch) {
    Add-Blocker "No completed live rehearsal packet was supplied. Generate one with tools\new_rehearsal_packet.ps1, fill 00-notes.md and launch-attestations.md from the actual server/client pass, then rerun with -RehearsalPacket."
  } else {
    Add-Blocker "Completed live rehearsal packet not supplied; audit mode did not validate 00-notes.md, screenshots/clips, fixes.md, or launch-attestations.md. Use -RehearsalPacket for a real rehearsal packet."
  }
} else {
  $packetFull = Resolve-UnderRepo $repoFull $RehearsalPacket
  if (-not $packetFull.StartsWith($repoFull, [System.StringComparison]::OrdinalIgnoreCase)) {
    Add-Blocker "Rehearsal packet is outside the repo: $packetFull"
  } elseif (-not (Test-Path $packetFull)) {
    Add-Blocker "Rehearsal packet not found: $packetFull"
  } else {
    Run-Check "live rehearsal packet" $rehearsalCheck @("-RepoRoot", $repoFull, "-PacketDir", $packetFull)
  }
}

Add-Manual "Apply discord/supabase/apply-all.sql to the live Supabase project and verify /observance status shows db true, last call ok true, queued writes 0."
Add-Manual "Build the deploy bundle with tools\package_launch_bundle.ps1, then load the plugin, datapack, and resource pack together on the exact Paper 1.21.11 server build; record the current plugin jar SHA1 and resource-pack SHA1 from observance-deploy-manifest.json in launch-attestations.md; confirm no console load errors or pack/datapack compatibility warnings."
Add-Manual "Join with a real Minecraft client and verify books, signs, item lore, titles/actionbars, bossbars, custom rune font glyphs, sounds, particles, NPC lines, and resource-pack fallback behavior in situ; record this in the rehearsal packet's launch-attestations.md."
Add-Manual "Run /observance preflight, /observance visualaudit, /observance dialogueaudit, /obs unlit audit, and /obs unlit ready on the live server after placement."
Add-Manual "Complete design/SESSION-ZERO.md before enabling observer_capture or voice_capture; record capture switch state and any observer_opt_out choices in launch-attestations.md."
Add-Manual "Rotate any previously exposed service_role or Discord bot credentials before public/friend launch; record proof in launch-attestations.md."

if ($blockers.Count -gt 0) {
  Write-Host "launch manual blocker check: NOT READY"
  foreach ($blocker in $blockers) {
    Write-Host "  - $blocker"
  }
} else {
  $mode = if ($Launch) { "launch" } else { "audit" }
  Write-Host "launch manual blocker check: OK ($mode mode) - repo-verifiable blockers are clear"
}

Write-Host "manual attestations still required:"
foreach ($item in $manual) {
  Write-Host "  - $item"
}
Write-Host "manual completion plan: design/MANUAL-LAUNCH-PLAN.md"

if ($Launch -and $blockers.Count -gt 0) {
  exit 1
}
