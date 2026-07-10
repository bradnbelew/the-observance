param(
  [string]$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path,
  [switch]$Launch
)

$ErrorActionPreference = "Stop"

$ledgerFile = Join-Path $RepoRoot "design\CLUE-LEDGER.md"
$failures = [System.Collections.Generic.List[string]]::new()

function Fail([string]$Message) {
  $script:failures.Add($Message) | Out-Null
}

if (-not (Test-Path -LiteralPath $ledgerFile)) {
  throw "clue ledger check: missing design\CLUE-LEDGER.md"
}

$ledger = Get-Content -LiteralPath $ledgerFile -Raw

foreach ($requiredText in @(
  "Ledger Schema",
  "Launch Rules",
  "truth_state",
  "surface_owner",
  "players_see",
  "players_infer",
  "invited_action",
  "traversal_vector",
  "later_proof",
  "audit_status",
  "Every required row needs a traversal vector",
  "Every mandatory destination needs at least two vectors"
)) {
  if ($ledger.IndexOf($requiredText, [System.StringComparison]::OrdinalIgnoreCase) -lt 0) {
    Fail "CLUE-LEDGER.md missing required ledger contract text: $requiredText"
  }
}

$requiredIds = @(
  "hold-address-reconstruction",
  "first-report-mouth",
  "rune-literacy",
  "reckoning-literacy",
  "vaun-audit",
  "mara-editions",
  "sella-seven-count",
  "orin-posture",
  "brann-dark-hours",
  "iss-forgery",
  "custom-bow",
  "custom-offering",
  "custom-kept-light",
  "custom-deep-line",
  "custom-unspoken",
  "custom-sacred-beast",
  "custom-dark-hours",
  "media-ash-13",
  "media-reeds",
  "media-stay-awake",
  "media-six-return",
  "archive-not-kept",
  "unlit-custom-proof",
  "wren-evidence-loop",
  "accepting-convergence"
)

foreach ($id in $requiredIds) {
  if ($ledger.IndexOf("| $id |", [System.StringComparison]::OrdinalIgnoreCase) -lt 0) {
    Fail "CLUE-LEDGER.md missing required row: $id"
  }
}

$allowedStatuses = @("required", "evidence", "deepening", "treasure", "rewrite", "merge", "cut")
$allowedTruth = @("true", "false", "partial", "forged", "stale", "misunderstood")
$allowedAudit = @("ready", "needs_rewrite", "needs_build", "needs_live_proof", "cut_candidate")
$rows = [System.Collections.Generic.List[object]]::new()

foreach ($line in ($ledger -split "`r?`n")) {
  if (-not $line.StartsWith("| ")) { continue }
  if ($line -match '^\|\s*-+\s*\|') { continue }
  if ($line -match '^\|\s*id\s*\|') { continue }

  $cells = @($line.Trim().Trim("|").Split("|") | ForEach-Object { $_.Trim() })
  if ($cells.Count -ne 12) { continue }
  if ($requiredIds -notcontains $cells[0]) { continue }

  $row = [pscustomobject]@{
    Id = $cells[0]
    Expedition = $cells[1]
    Status = $cells[2]
    TruthState = $cells[3]
    SurfaceOwner = $cells[4]
    PlayersSee = $cells[5]
    PlayersInfer = $cells[6]
    InvitedAction = $cells[7]
    TraversalVector = $cells[8]
    LaterProof = $cells[9]
    Implementation = $cells[10]
    AuditStatus = $cells[11]
  }
  $rows.Add($row) | Out-Null
}

if ($rows.Count -lt $requiredIds.Count) {
  Fail "CLUE-LEDGER.md parsed $($rows.Count) required row(s), expected $($requiredIds.Count)"
}

foreach ($row in $rows) {
  if ($allowedStatuses -notcontains $row.Status) {
    Fail "$($row.Id) has unsupported status '$($row.Status)'"
  }
  if ($allowedTruth -notcontains $row.TruthState) {
    Fail "$($row.Id) has unsupported truth_state '$($row.TruthState)'"
  }
  if ($allowedAudit -notcontains $row.AuditStatus) {
    Fail "$($row.Id) has unsupported audit_status '$($row.AuditStatus)'"
  }

  foreach ($field in @("Expedition", "SurfaceOwner", "PlayersSee", "PlayersInfer", "InvitedAction", "TraversalVector", "LaterProof", "Implementation")) {
    if ([string]::IsNullOrWhiteSpace([string]$row.$field)) {
      Fail "$($row.Id) has blank required ledger field: $field"
    }
  }

  if ($row.Status -eq "required") {
    foreach ($field in @("InvitedAction", "TraversalVector", "LaterProof")) {
      if ([string]::IsNullOrWhiteSpace([string]$row.$field)) {
        Fail "$($row.Id) is required but lacks $field"
      }
    }
  }
}

foreach ($customId in @("custom-bow", "custom-offering", "custom-kept-light", "custom-deep-line", "custom-unspoken", "custom-sacred-beast", "custom-dark-hours")) {
  $row = $rows | Where-Object { $_.Id -eq $customId } | Select-Object -First 1
  if ($null -eq $row) {
    Fail "missing custom row: $customId"
    continue
  }
  if ($row.LaterProof.IndexOf("proof", [System.StringComparison]::OrdinalIgnoreCase) -lt 0 -and
      $row.LaterProof.IndexOf("prove", [System.StringComparison]::OrdinalIgnoreCase) -lt 0 -and
      $row.LaterProof.IndexOf("explain", [System.StringComparison]::OrdinalIgnoreCase) -lt 0 -and
      $row.LaterProof.IndexOf("resolve", [System.StringComparison]::OrdinalIgnoreCase) -lt 0 -and
      $row.LaterProof.IndexOf("use", [System.StringComparison]::OrdinalIgnoreCase) -lt 0) {
    Fail "$customId does not name a late proof/use in later_proof"
  }
}

if ($Launch) {
  foreach ($row in $rows) {
    if ($row.AuditStatus -notin @("ready", "needs_live_proof")) {
      Fail "$($row.Id) is not repo/setup-ready for launch audit; audit_status is '$($row.AuditStatus)'"
    }
  }
}

if ($failures.Count -gt 0) {
  Write-Host "clue ledger check: FAILED"
  foreach ($failure in $failures) {
    Write-Host "  - $failure"
  }
  exit 1
}

$rewriteCount = @($rows | Where-Object { $_.AuditStatus -eq "needs_rewrite" }).Count
$buildCount = @($rows | Where-Object { $_.AuditStatus -eq "needs_build" }).Count
$liveCount = @($rows | Where-Object { $_.AuditStatus -eq "needs_live_proof" }).Count
Write-Host "clue ledger check: OK - $($rows.Count) required control rows parsed; debt: $rewriteCount rewrite, $buildCount build, $liveCount live-proof"

