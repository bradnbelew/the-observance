[CmdletBinding()]
param(
  [ValidateSet("local")]
  [string]$Target = "local",
  [string]$ProjectRef = "",
  [string]$ReceiptPath = "",
  [switch]$KeepWorkspace,
  [switch]$SelfTest
)

$ErrorActionPreference = "Stop"
$script:ProductionProjectRef = "fdnmhbpxnodrnbrzrlqq"
$script:Root = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$script:ProposalDir = Join-Path $script:Root "design\m2\sql"
$script:HarnessDir = Join-Path $script:ProposalDir "tests"

function Assert-True([bool]$Condition, [string]$Message) {
  if (-not $Condition) { throw $Message }
}

function Assert-SafeTarget {
  Assert-True ($Target -eq "local") "Only a local Supabase target is supported by this harness."
  Assert-True ([string]::IsNullOrWhiteSpace($ProjectRef)) "Local validation refuses every project ref; do not link a hosted project."
  Assert-True ($ProjectRef -ne $script:ProductionProjectRef) "Production project $script:ProductionProjectRef is forbidden."
}

function Invoke-Cli([string[]]$Arguments, [string]$WorkingDirectory) {
  $rendered = "supabase " + ($Arguments -join " ")
  Write-Host "> $rendered"
  Push-Location $WorkingDirectory
  try {
    $output = & supabase @Arguments 2>&1
    $code = $LASTEXITCODE
  } finally {
    Pop-Location
  }
  $text = ($output | Out-String).TrimEnd()
  if ($text) { Write-Host $text }
  if ($code -ne 0) { throw "$rendered failed with exit code $code" }
  return $text
}

function Assert-Help([string[]]$Arguments, [string]$RequiredText = "") {
  $output = Invoke-Cli $Arguments $script:Root
  Assert-True (-not [string]::IsNullOrWhiteSpace($output)) "CLI help for '$($Arguments -join ' ')' was empty."
  if (-not [string]::IsNullOrWhiteSpace($RequiredText)) {
    Assert-True ($output -match [regex]::Escape($RequiredText)) "CLI help for '$($Arguments -join ' ')' did not advertise '$RequiredText'."
  }
}

function New-Migration([string]$Workspace, [string]$Name, [string]$Source) {
  $migrationDir = Join-Path $Workspace "supabase\migrations"
  $before = @((Get-ChildItem -LiteralPath $migrationDir -Filter "*.sql" -ErrorAction SilentlyContinue).FullName)
  [void](Invoke-Cli @("migration", "new", $Name) $Workspace)
  $after = @(Get-ChildItem -LiteralPath $migrationDir -Filter "*.sql")
  $created = @($after | Where-Object { $_.FullName -notin $before })
  Assert-True ($created.Count -eq 1) "Supabase CLI did not create exactly one migration for $Name."
  Copy-Item -LiteralPath $Source -Destination $created[0].FullName -Force
  return $created[0].Name
}

function Invoke-SelfTest {
  Assert-SafeTarget
  foreach ($name in @(
    "contract-v1.up.sql", "contract-v1.rollback.sql", "contract-v1.forward.sql"
  )) {
    Assert-True (Test-Path -LiteralPath (Join-Path $script:ProposalDir $name)) "missing reviewed proposal: $name"
  }
  foreach ($name in @(
    "local-baseline.sql", "lifecycle-seed.sql", "assert-forward.sql", "assert-rollback.sql",
    "assert-final.sql", "contract-v1.test.sql"
  )) {
    Assert-True (Test-Path -LiteralPath (Join-Path $script:HarnessDir $name)) "missing validation input: $name"
  }
  $blocked = $false
  try {
    $script:ProjectRef = $script:ProductionProjectRef
    Assert-SafeTarget
  } catch { $blocked = $true }
  Assert-True $blocked "production-ref guard did not fail closed"
  $script:ProjectRef = ""
  Write-Host "validate_m2_supabase self-test: PASS - local-only target guard and validation inputs"
}

if ($SelfTest) {
  Invoke-SelfTest
  exit 0
}

Assert-SafeTarget
Assert-True ($null -ne (Get-Command supabase -ErrorAction SilentlyContinue)) "Supabase CLI is required."
Assert-True ($null -ne (Get-Command docker -ErrorAction SilentlyContinue)) "Docker is required for local Supabase validation."

# Discover the installed CLI surface before using it. This intentionally fails when a
# changed CLI no longer advertises the commands/flags this local-only harness relies on.
Assert-Help @("--help") "migration"
Assert-Help @("init", "--help")
Assert-Help @("migration", "new", "--help")
Assert-Help @("db", "reset", "--help") "--local"
Assert-Help @("test", "db", "--help")
Assert-Help @("db", "advisors", "--help") "--local"
Assert-Help @("db", "advisors", "--help") "--type"
Assert-Help @("stop", "--help") "--no-backup"

$cliVersion = Invoke-Cli @("--version") $script:Root
$gitCommit = (& git -C $script:Root rev-parse HEAD).Trim()
$tempRoot = [System.IO.Path]::GetFullPath([System.IO.Path]::GetTempPath())
$workspace = Join-Path $tempRoot ("observance-m2-supabase-" + [guid]::NewGuid().ToString("N"))
New-Item -ItemType Directory -Path $workspace | Out-Null
$startedAt = [DateTimeOffset]::UtcNow
$migrationIds = [System.Collections.Generic.List[string]]::new()

try {
  [void](Invoke-Cli @("init") $workspace)
  $migrationIds.Add((New-Migration $workspace "observance_m2_local_baseline" (Join-Path $script:HarnessDir "local-baseline.sql")))
  $migrationIds.Add((New-Migration $workspace "observance_m2_contract_v1" (Join-Path $script:ProposalDir "contract-v1.up.sql")))
  $migrationIds.Add((New-Migration $workspace "observance_m2_lifecycle_seed" (Join-Path $script:HarnessDir "lifecycle-seed.sql")))
  $migrationIds.Add((New-Migration $workspace "observance_m2_forward_first" (Join-Path $script:ProposalDir "contract-v1.forward.sql")))
  $migrationIds.Add((New-Migration $workspace "observance_m2_assert_forward" (Join-Path $script:HarnessDir "assert-forward.sql")))
  $migrationIds.Add((New-Migration $workspace "observance_m2_rollback" (Join-Path $script:ProposalDir "contract-v1.rollback.sql")))
  $migrationIds.Add((New-Migration $workspace "observance_m2_assert_rollback" (Join-Path $script:HarnessDir "assert-rollback.sql")))
  $migrationIds.Add((New-Migration $workspace "observance_m2_forward_recovery" (Join-Path $script:ProposalDir "contract-v1.forward.sql")))
  $migrationIds.Add((New-Migration $workspace "observance_m2_forward_idempotency" (Join-Path $script:ProposalDir "contract-v1.forward.sql")))
  $migrationIds.Add((New-Migration $workspace "observance_m2_assert_final" (Join-Path $script:HarnessDir "assert-final.sql")))

  $databaseTests = Join-Path $workspace "supabase\tests\database"
  New-Item -ItemType Directory -Path $databaseTests -Force | Out-Null
  Copy-Item -LiteralPath (Join-Path $script:HarnessDir "contract-v1.test.sql") -Destination $databaseTests

  $startOutput = Invoke-Cli @("start") $workspace
  $resetOutput = Invoke-Cli @("db", "reset", "--local") $workspace
  $testOutput = Invoke-Cli @("test", "db") $workspace
  $securityOutput = Invoke-Cli @("db", "advisors", "--local", "--type", "security") $workspace
  $performanceOutput = Invoke-Cli @("db", "advisors", "--local", "--type", "performance") $workspace

  if ([string]::IsNullOrWhiteSpace($ReceiptPath)) {
    $stamp = [DateTimeOffset]::UtcNow.ToString("yyyyMMddTHHmmssZ")
    $ReceiptPath = Join-Path $script:Root "design\m2\receipts\supabase-local-$stamp.json"
  }
  $receiptCandidate = if ([System.IO.Path]::IsPathRooted($ReceiptPath)) {
    $ReceiptPath
  } else {
    Join-Path $script:Root $ReceiptPath
  }
  $receiptFull = [System.IO.Path]::GetFullPath($receiptCandidate)
  $rootPrefix = $script:Root.TrimEnd('\', '/') + [System.IO.Path]::DirectorySeparatorChar
  Assert-True ($receiptFull.StartsWith($rootPrefix, [StringComparison]::OrdinalIgnoreCase)) "Receipt must stay inside the repository."
  New-Item -ItemType Directory -Path (Split-Path $receiptFull -Parent) -Force | Out-Null
  [ordered]@{
    status = "passed"
    target_kind = "local"
    hosted_project_ref = $null
    production_project_ref_forbidden = $script:ProductionProjectRef
    cli_version = $cliVersion
    git_commit = $gitCommit
    started_at = $startedAt.ToString("o")
    completed_at = [DateTimeOffset]::UtcNow.ToString("o")
    migration_ids = @($migrationIds)
    database_tests = $testOutput
    security_advisor = $securityOutput
    performance_advisor = $performanceOutput
    start_receipt = $startOutput
    reset_receipt = $resetOutput
  } | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath $receiptFull -Encoding utf8
  Write-Host "M2 local Supabase validation: PASS"
  Write-Host "receipt: $receiptFull"
} finally {
  if (Test-Path -LiteralPath $workspace) {
    try { [void](Invoke-Cli @("stop", "--no-backup") $workspace) } catch { Write-Warning $_ }
    if ($KeepWorkspace) {
      Write-Host "kept local validation workspace: $workspace"
    } else {
      $resolvedWorkspace = [System.IO.Path]::GetFullPath($workspace)
      Assert-True ($resolvedWorkspace.StartsWith($tempRoot, [StringComparison]::OrdinalIgnoreCase)) "Refusing cleanup outside the temp root."
      Assert-True ((Split-Path $resolvedWorkspace -Leaf) -match '^observance-m2-supabase-[0-9a-f]{32}$') "Refusing cleanup of an unexpected temp path."
      Remove-Item -LiteralPath $workspace -Recurse -Force
    }
  }
}
