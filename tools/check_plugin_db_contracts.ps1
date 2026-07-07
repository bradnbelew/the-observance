param(
  [string]$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
)

$ErrorActionPreference = "Stop"

$failures = New-Object System.Collections.Generic.List[string]

function Add-Failure([string]$Message) {
  $script:failures.Add($Message) | Out-Null
}

function Read-Text([string]$Path) {
  if (!(Test-Path $Path)) {
    Add-Failure "Missing required file: $Path"
    return ""
  }
  return Get-Content -LiteralPath $Path -Raw
}

$supabaseClient = Read-Text (Join-Path $RepoRoot "plugin\src\main\java\com\observance\watcher\data\SupabaseClient.java")
$baseRow = Read-Text (Join-Path $RepoRoot "plugin\src\main\java\com\observance\watcher\data\rows\BaseRow.java")
$eventLogRow = Read-Text (Join-Path $RepoRoot "plugin\src\main\java\com\observance\watcher\data\rows\EventLogRow.java")
$settingsRow = Read-Text (Join-Path $RepoRoot "plugin\src\main\java\com\observance\watcher\data\rows\SettingsRow.java")
$beatPoller = Read-Text (Join-Path $RepoRoot "plugin\src\main\java\com\observance\watcher\beat\BeatQueuePoller.java")
$schemaRepair = Read-Text (Join-Path $RepoRoot "discord\supabase\schema-repair.sql")
$applyAll = Read-Text (Join-Path $RepoRoot "discord\supabase\apply-all.sql")
$oracleMigration = Read-Text (Join-Path $RepoRoot "discord\supabase\migrations\0004_oracle.sql")
$dashboardBeatStatus = Read-Text (Join-Path $RepoRoot "dashboard\supabase\migrations\0009_beat_queue_failed_status.sql")
$dbTypes = Read-Text (Join-Path $RepoRoot "discord\src\db\types.ts")

if (!$supabaseClient.Contains('return upsert("bases", "owner_uuid", row, "upsertBase")')) {
  Add-Failure "SupabaseClient.upsertBase must conflict on owner_uuid, not bigint id"
}
if (!$baseRow.Contains('@SerializedName("owner_uuid")') -or $baseRow -notmatch 'public\s+String\s+ownerUuid') {
  Add-Failure "BaseRow must serialize owner_uuid for plugin base upserts"
}
if (!$schemaRepair.Contains("create unique index if not exists bases_owner_uuid_key")) {
  Add-Failure "schema-repair.sql must create bases_owner_uuid_key for plugin base upserts"
}

foreach ($fragment in @(
  '@SerializedName("level")',
  '@SerializedName("source")',
  '@SerializedName("message")',
  '@SerializedName("created_at")'
)) {
  if (!$eventLogRow.Contains($fragment)) {
    Add-Failure "EventLogRow missing real event_log column mapping: $fragment"
  }
}
foreach ($bad in @('@SerializedName("type")', '@SerializedName("context")', '@SerializedName("mc_uuid")', '@SerializedName("detail")')) {
  if ($eventLogRow.Contains($bad)) {
    Add-Failure "EventLogRow must not serialize non-table column $bad"
  }
}

if (!$settingsRow.Contains("JsonElement value") -or !$settingsRow.Contains("public boolean asBoolean()")) {
  Add-Failure "SettingsRow must parse settings.value jsonb as JsonElement and expose asBoolean()"
}

foreach ($sql in @($schemaRepair, $applyAll)) {
  foreach ($needle in @(
    "add column if not exists type",
    "add column if not exists context",
    "add column if not exists mc_uuid",
    "add column if not exists detail",
    "create table if not exists public.world_paste_ledger",
    "create index if not exists idx_world_paste_ledger_site"
  )) {
    if (!$sql.Contains($needle)) {
      Add-Failure "SQL bundle/repair missing plugin DB contract fragment: $needle"
    }
  }
}

if (!$supabaseClient.Contains("public boolean claimBeatForFiring(String beatId)")) {
  Add-Failure "SupabaseClient must expose claimBeatForFiring for durable beat claims"
}
if (!$beatPoller.Contains("supabase.claimBeatForFiring(beat.id)")) {
  Add-Failure "BeatQueuePoller must claim approved beats before enacting them"
}
foreach ($sql in @($oracleMigration, $dashboardBeatStatus, $applyAll)) {
  if (!$sql.Contains("'pending','approved','firing','skipped','fired','failed'") -and
      !$sql.Contains("'pending', 'approved', 'firing', 'skipped', 'fired', 'failed'")) {
    Add-Failure "beat_queue status CHECK must include transient firing status"
  }
}
if (!$dbTypes.Contains("'pending' | 'approved' | 'firing' | 'skipped' | 'fired' | 'failed'")) {
  Add-Failure "Discord BeatStatus type must include firing"
}

if ($failures.Count -gt 0) {
  Write-Host "plugin DB contract check: FAILED"
  foreach ($failure in $failures) {
    Write-Host "  - $failure"
  }
  exit 1
}

Write-Host "plugin DB contract check: OK - plugin row shapes, schema repair, apply-all, paste ledger, and beat claim status match"
