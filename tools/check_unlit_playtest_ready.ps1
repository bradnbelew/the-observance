param(
  [string]$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path,
  [string]$PacketDir = ""
)

$ErrorActionPreference = "Stop"

function Resolve-UnderRepo([string] $Root, [string] $Path) {
  if ([System.IO.Path]::IsPathRooted($Path)) {
    return [System.IO.Path]::GetFullPath($Path)
  }
  return [System.IO.Path]::GetFullPath((Join-Path $Root $Path))
}

$repoFull = [System.IO.Path]::GetFullPath($RepoRoot)
$auditAll = Join-Path $repoFull "tools\audit_all.ps1"
$rehearsalCheck = Join-Path $repoFull "tools\check_rehearsal_packet.ps1"

if (-not (Test-Path -LiteralPath $auditAll)) {
  throw "playtest readiness: missing tools\audit_all.ps1"
}
if (-not (Test-Path -LiteralPath $rehearsalCheck)) {
  throw "playtest readiness: missing tools\check_rehearsal_packet.ps1"
}
if ([string]::IsNullOrWhiteSpace($PacketDir)) {
  throw "playtest readiness: pass -PacketDir rehearsals\<date> after the live Unlit evidence packet is filled"
}

$packetFull = Resolve-UnderRepo $repoFull $PacketDir
if (-not $packetFull.StartsWith($repoFull, [System.StringComparison]::OrdinalIgnoreCase)) {
  throw "playtest readiness: refusing packet outside repo: $packetFull"
}
if (-not (Test-Path -LiteralPath $packetFull)) {
  throw "playtest readiness: packet folder not found: $packetFull"
}

& powershell -NoProfile -ExecutionPolicy Bypass -File $auditAll -RepoRoot $repoFull
if ($LASTEXITCODE -ne 0) {
  exit $LASTEXITCODE
}

& powershell -NoProfile -ExecutionPolicy Bypass -File $rehearsalCheck -RepoRoot $repoFull -PacketDir $packetFull
if ($LASTEXITCODE -ne 0) {
  exit $LASTEXITCODE
}

$notesFile = Join-Path $packetFull "00-notes.md"
$notes = Get-Content -LiteralPath $notesFile -Raw
foreach ($required in @(
  "## Unlit Expedition Proof",
  "/obs unlit buildmode off",
  "/obs unlit darken all",
  "/obs unlit audit",
  "/obs unlit ready",
  "Gate: READY",
  "fixture proof",
  "stray light OK",
  "border OK",
  "borrowed lantern",
  "lantern is broken",
  "failed-cheese",
  "non-linear",
  "unlit_house_lamp",
  "unlit_house_cairn",
  "unlit_house_coop",
  "unlit_house_well",
  "unlit_house_watch",
  "unlit_house_warm",
  "unlit_house_threshold",
  "unlit_house_base"
)) {
  if ($notes.IndexOf($required, [System.StringComparison]::OrdinalIgnoreCase) -lt 0) {
    throw "playtest readiness: completed packet missing Unlit proof text: $required"
  }
}

Write-Host "unlit playtest readiness: OK - stop building and hand this to Nano for playtest"
