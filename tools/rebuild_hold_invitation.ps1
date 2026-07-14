param(
  [string]$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path,
  [string]$InputZip = "",
  [string]$OutZip = "",
  [string]$PublicSiteHost = "copperlinehosting.com",
  [switch]$NoBackup
)

$ErrorActionPreference = "Stop"

# RETIRED_PRE_V5_TOOL: the downloadable six-room prologue is not the Deep Hold
# and must never overwrite the V5 diagnostic web artifact.
throw "RETIRED PRE-V5 TOOL: rebuild_hold_invitation.ps1 is disabled. The V5 Hold is built only by the Paper plugin."

$repoFull = [System.IO.Path]::GetFullPath($RepoRoot)
$builder = Join-Path $repoFull "tools\build_hold_prologue.py"
if (-not (Test-Path -LiteralPath $builder)) {
  throw "hold rebuild: production builder is missing: $builder"
}

if ([string]::IsNullOrWhiteSpace($InputZip)) {
  $InputZip = Join-Path $repoFull "dashboard\public\the-hold\the-hold.zip"
}
if ([string]::IsNullOrWhiteSpace($OutZip)) {
  $OutZip = $InputZip
}

$arguments = @(
  $builder,
  "--input", [System.IO.Path]::GetFullPath($InputZip),
  "--output", [System.IO.Path]::GetFullPath($OutZip)
)
if (-not $NoBackup) {
  $arguments += "--backup"
}

& python @arguments
if ($LASTEXITCODE -ne 0) {
  throw "hold rebuild: production builder exited with code $LASTEXITCODE"
}

$siteHost = ($PublicSiteHost -replace '^https?://', '').TrimEnd('/')
Write-Host "hold rebuild: public listing = https://$siteHost/"
Write-Host "hold rebuild: route = six contained rooms; five controlled gates; one lamp interaction"
Write-Host "hold rebuild: destination grammar = host fragments I-IV + common-web ending + service digits 25569; no assembled raw server endpoint"
