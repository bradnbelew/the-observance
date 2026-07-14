param(
  [string]$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path,
  [string]$OutDir = "",
  [switch]$SkipChecks
)

$ErrorActionPreference = "Stop"

# RETIRED_PRE_V5_TOOL: the generated director packet describes the previous
# campaign state and is not a production source.
throw "RETIRED PRE-V5 TOOL: new_director_packet.ps1 is disabled. Use the V5 setup guide, launch runbook, and live-test matrix."

function Code([string]$Value) {
  return [string]([char]0x60) + $Value + [string]([char]0x60)
}

function Add-Line([System.Collections.Generic.List[string]]$Lines, [string]$Value = "") {
  $Lines.Add($Value) | Out-Null
}

function Run-Check([string]$Name, [string]$ScriptPath, [string[]]$ArgsList) {
  $result = [ordered]@{
    Name = $Name
    ExitCode = 0
    Output = @()
  }

  if (-not (Test-Path -LiteralPath $ScriptPath)) {
    $result.ExitCode = 127
    $result.Output = @("missing check script: $ScriptPath")
    return [pscustomobject]$result
  }

  $output = @(& powershell -NoProfile -ExecutionPolicy Bypass -File $ScriptPath @ArgsList 2>&1)
  $result.ExitCode = $LASTEXITCODE
  $result.Output = @($output | ForEach-Object { [string]$_ })
  return [pscustomobject]$result
}

function Parse-LedgerDebt([string]$LedgerPath) {
  $counts = [ordered]@{
    ready = 0
    needs_rewrite = 0
    needs_build = 0
    needs_live_proof = 0
    cut_candidate = 0
    unknown = 0
  }

  if (-not (Test-Path -LiteralPath $LedgerPath)) {
    $counts.unknown = 1
    return [pscustomobject]$counts
  }

  foreach ($line in Get-Content -LiteralPath $LedgerPath) {
    if ($line -notmatch '^\| ') { continue }
    if ($line -match '^\| id \|') { continue }
    if ($line -match '^\| ---') { continue }
    $parts = $line.Trim().Trim('|').Split('|') | ForEach-Object { $_.Trim() }
    if ($parts.Count -lt 12) { continue }
    $status = $parts[11]
    if ($counts.Contains($status)) {
      $counts[$status]++
    } else {
      $counts.unknown++
    }
  }

  return [pscustomobject]$counts
}

function FileReceipt([string]$Path) {
  if (-not (Test-Path -LiteralPath $Path)) {
    return "missing"
  }
  $item = Get-Item -LiteralPath $Path
  $sha = (Get-FileHash -LiteralPath $Path -Algorithm SHA1).Hash.ToLowerInvariant()
  return "$($item.Length) bytes, sha1 $sha"
}

$repoFull = [System.IO.Path]::GetFullPath($RepoRoot)
if ([string]::IsNullOrWhiteSpace($OutDir)) {
  $stamp = Get-Date -Format "yyyyMMdd-HHmmss"
  $OutDir = Join-Path $repoFull "build\director-packets\$stamp"
}
$outFull = [System.IO.Path]::GetFullPath($OutDir)

if (-not $outFull.StartsWith($repoFull, [System.StringComparison]::OrdinalIgnoreCase)) {
  throw "director packet output must stay inside repo: $outFull"
}

New-Item -ItemType Directory -Force -Path $outFull | Out-Null

$checks = @()
if (-not $SkipChecks) {
  $commonArgs = @("-RepoRoot", $repoFull)
  $checks = @(
    Run-Check "Hold invitation" (Join-Path $repoFull "tools\check_hold_invitation.ps1") $commonArgs
    Run-Check "Clue ledger" (Join-Path $repoFull "tools\check_clue_ledger.ps1") $commonArgs
    Run-Check "Keeper investigations" (Join-Path $repoFull "tools\check_keeper_investigations.ps1") $commonArgs
    Run-Check "Customs Rosetta" (Join-Path $repoFull "tools\check_customs_rosetta.ps1") $commonArgs
    Run-Check "Director concern closure" (Join-Path $repoFull "tools\check_director_concern_closure.ps1") $commonArgs
    Run-Check "Structure quality" (Join-Path $repoFull "tools\check_structure_quality.ps1") $commonArgs
    Run-Check "World build readiness" (Join-Path $repoFull "tools\check_world_build_readiness.ps1") $commonArgs
    Run-Check "External media readiness" (Join-Path $repoFull "tools\check_external_media_readiness.ps1") $commonArgs
  )
}

$ledgerDebt = Parse-LedgerDebt (Join-Path $repoFull "design\CLUE-LEDGER.md")
$holdReceipt = FileReceipt (Join-Path $repoFull "dashboard\public\the-hold\the-hold.zip")

$lines = [System.Collections.Generic.List[string]]::new()
Add-Line $lines "# Observance Director Packet"
Add-Line $lines ""
Add-Line $lines "Generated: $(Get-Date -Format "yyyy-MM-dd HH:mm:ss")"
Add-Line $lines "Repo: $(Code $repoFull)"
Add-Line $lines ""
Add-Line $lines "## Current Director Read"
Add-Line $lines ""
Add-Line $lines "- The Hold no longer hands over the raw endpoint; it now requires route/gate/port reconstruction."
Add-Line $lines "- The keeper stones are no longer the open primary path; Vaun, Mara, Orin, and Brann are gated behind evidence clusters."
Add-Line $lines "- The six keeper investigations now have dossier contracts for player behavior, non-sign surfaces, old-stone role, fair answer shape, and rehearsal failure."
Add-Line $lines "- The Rosetta/customs answer now has a fair hint rail that teaches seven way names and ring order."
Add-Line $lines "- The seven customs now have a field-guide contract: folk version, practical reason, physical proof, consequence, false version, and late use."
Add-Line $lines "- Wren now has an evidence-loop contract: independent proof before reveal, comparison instead of exposition, one reckoning choice, and a finale callback that does not overrule the group."
Add-Line $lines "- The original director concerns now have a concern-closure contract and a 220-scenario concern audit: variety, nonlinearity, cohesion, difficulty fairness, structure immersion, and side-content weight stay visible."
Add-Line $lines "- The project is still not launch-ready until live placement, live server proof, and rehearsal attestations are filled."
Add-Line $lines ""
Add-Line $lines "## Check Results"
Add-Line $lines ""
if ($SkipChecks) {
  Add-Line $lines "- Checks skipped by request."
} else {
  foreach ($check in $checks) {
    $status = if ($check.ExitCode -eq 0) { "PASS" } else { "FAIL ($($check.ExitCode))" }
    Add-Line $lines "- $($check.Name): $status"
    foreach ($line in $check.Output) {
      Add-Line $lines "  - $line"
    }
  }
}
Add-Line $lines ""
Add-Line $lines "## Clue Debt"
Add-Line $lines ""
Add-Line $lines "- ready: $($ledgerDebt.ready)"
Add-Line $lines "- needs_rewrite: $($ledgerDebt.needs_rewrite)"
Add-Line $lines "- needs_build: $($ledgerDebt.needs_build)"
Add-Line $lines "- needs_live_proof: $($ledgerDebt.needs_live_proof)"
Add-Line $lines "- cut_candidate: $($ledgerDebt.cut_candidate)"
Add-Line $lines "- unknown: $($ledgerDebt.unknown)"
Add-Line $lines ""
Add-Line $lines "## Receipts"
Add-Line $lines ""
Add-Line $lines "- the-hold.zip: $(Code $holdReceipt)"
Add-Line $lines "- active ledger: $(Code "design\CLUE-LEDGER.md")"
Add-Line $lines "- flow bible: $(Code "design\DIRECTOR-FLOW-BIBLE.md")"
Add-Line $lines "- setup guide: $(Code "design\DIRECTOR-SETUP-GUIDE.md")"
Add-Line $lines "- customs field guide: $(Code "design\CUSTOMS-FIELD-GUIDE.md")"
Add-Line $lines "- keeper investigation dossiers: $(Code "design\KEEPER-INVESTIGATION-DOSSIERS.md")"
Add-Line $lines "- Wren evidence loop: $(Code "design\WREN-EVIDENCE-LOOP.md")"
Add-Line $lines "- concern closure: $(Code "design\DIRECTOR-CONCERN-CLOSURE.md")"
Add-Line $lines "- structure doctrine: $(Code "design\structures.md")"
Add-Line $lines "- setup goal prompt: $(Code "outputs\observance-setup-ready-goal-prompt.md")"
Add-Line $lines "- break/speedrun scenario audit: $(Code "outputs\observance-200-scenario-break-speedrun-audit.md")"
Add-Line $lines "- concern-closure scenario audit: $(Code "outputs\observance-200-concern-closure-scenario-audit.md")"
Add-Line $lines ""
Add-Line $lines "## Setup Commands"
Add-Line $lines ""
Add-Line $lines "Run these from the repo root when you are preparing a live server:"
Add-Line $lines ""
Add-Line $lines '```powershell'
Add-Line $lines "powershell -NoProfile -ExecutionPolicy Bypass -File tools\prepare_friend_launch.ps1 -ResourcePackUrl <hosted-https-zip-url>"
Add-Line $lines "powershell -NoProfile -ExecutionPolicy Bypass -File tools\new_launch_placement_packet.ps1"
Add-Line $lines "powershell -NoProfile -ExecutionPolicy Bypass -File tools\apply_launch_coords.ps1 -CaptureCsv <coords-capture.csv> -Apply"
Add-Line $lines "powershell -NoProfile -ExecutionPolicy Bypass -File tools\check_world_build_readiness.ps1 -Launch"
Add-Line $lines "powershell -NoProfile -ExecutionPolicy Bypass -File tools\check_launch_manual_blockers.ps1 -Launch -CaptureCsv <coords-capture.csv> -RehearsalPacket <rehearsal-dir>"
Add-Line $lines '```'
Add-Line $lines ""
Add-Line $lines "## Director Pass Order"
Add-Line $lines ""
Add-Line $lines "1. Place and proof the world anchors."
Add-Line $lines "2. Run the structure audit in-game with approach, focal, exit, and solved-state proof."
Add-Line $lines "3. Rehearse Hold, Rosetta, one keeper cluster, one custom toll, one website/archive reveal, and one Unlit transition."
Add-Line $lines "4. Run the speedrun rehearsal: fastest legal path from Hold to Threshold must still need keeper theory, side proof, Wren/Iss context, media proof, Unlit correction, and physical convergence."
Add-Line $lines "5. Run the side-proof retelling: every side site must change the case in the testers' own words."
Add-Line $lines "6. Run the copy read-aloud and sign-room pass; rewrite fantasy-gibberish lines and reshape any major site described as a sign room, book room, or answer room."
Add-Line $lines "7. Rewrite any clue-ledger row still marked needs_rewrite before calling the experience ready."
Add-Line $lines "8. Treat every player confusion as either a clue-surface problem, a traversal problem, or a pacing problem. Do not patch it by adding an out-of-character instruction."

$packetPath = Join-Path $outFull "director-status.md"
[System.IO.File]::WriteAllLines($packetPath, $lines, [System.Text.UTF8Encoding]::new($false))

Write-Host "director packet: wrote $packetPath"
