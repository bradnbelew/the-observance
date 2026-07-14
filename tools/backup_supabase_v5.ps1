<#
.SYNOPSIS
Creates a paged, checksummed backup of every public table declared by discord/supabase/apply-all.sql.

.EXAMPLE
  powershell -NoProfile -ExecutionPolicy Bypass -File tools\backup_supabase_v5.ps1 `
    -SupabaseUrl "https://project-ref.supabase.co" `
    -ServiceKeyEnv "OBSERVANCE_SUPABASE_KEY"

.EXAMPLE
  powershell -NoProfile -ExecutionPolicy Bypass -File tools\backup_supabase_v5.ps1 -SelfTest

.NOTES
Prefer -ServiceKeyEnv or the gitignored dashboard/.env.local file. Although -ServiceKey is
supported for automation, putting a secret directly on a command line can expose it through shell
history or process inspection. This script never prints the key and never writes it to a backup.
#>
[CmdletBinding()]
param(
  [string]$RepoRoot = "",
  [string]$SupabaseUrl = "",
  [string]$ServiceKey = "",
  [string]$ServiceKeyEnv = "OBSERVANCE_SUPABASE_KEY",
  [string]$EnvFile = "dashboard\.env.local",
  [ValidateRange(1, 10000)]
  [int]$PageSize = 1000,
  [switch]$SyntaxCheck,
  [switch]$SelfTest
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"
if ([string]::IsNullOrWhiteSpace($RepoRoot)) {
  $RepoRoot = Split-Path -Parent $PSScriptRoot
}
$script:Utf8NoBom = New-Object System.Text.UTF8Encoding($false)
$script:DefaultProjectUrl = "https://fdnmhbpxnodrnbrzrlqq.supabase.co"
$script:FutureV5Tables = @(
  "investigations",
  "investigation_nodes",
  "evidence_receipts",
  "required_media"
)

function Assert-True {
  param([bool]$Condition, [string]$Message)
  if (-not $Condition) {
    throw "self-test failed: $Message"
  }
}

function Invoke-ScriptSyntaxCheck {
  $tokens = $null
  $parseErrors = $null
  [void][System.Management.Automation.Language.Parser]::ParseFile(
    $PSCommandPath,
    [ref]$tokens,
    [ref]$parseErrors
  )
  if (@($parseErrors).Count -gt 0) {
    $summary = @($parseErrors | ForEach-Object { $_.Message }) -join "; "
    throw "syntax check failed: $summary"
  }
}

function Get-CanonicalRoot {
  param([string]$Path)
  if ([string]::IsNullOrWhiteSpace($Path)) {
    throw "repository root is blank"
  }
  $full = [System.IO.Path]::GetFullPath($Path)
  if (-not (Test-Path -LiteralPath $full -PathType Container)) {
    throw "repository root does not exist"
  }
  return $full.TrimEnd([System.IO.Path]::DirectorySeparatorChar, [System.IO.Path]::AltDirectorySeparatorChar)
}

function Get-FullPathWithin {
  param(
    [string]$Root,
    [string]$Path,
    [string]$Label
  )
  $candidate = if ([System.IO.Path]::IsPathRooted($Path)) {
    [System.IO.Path]::GetFullPath($Path)
  } else {
    [System.IO.Path]::GetFullPath((Join-Path $Root $Path))
  }
  $prefix = $Root + [System.IO.Path]::DirectorySeparatorChar
  $inside = $candidate.Equals($Root, [System.StringComparison]::OrdinalIgnoreCase) -or
    $candidate.StartsWith($prefix, [System.StringComparison]::OrdinalIgnoreCase)
  if (-not $inside) {
    throw "$Label must stay inside the repository"
  }
  return $candidate
}

function Assert-NoReparseAncestors {
  param([string]$Root, [string]$Path, [string]$Label)
  $candidate = Get-FullPathWithin $Root $Path $Label
  $cursor = $candidate
  while (-not $cursor.Equals($Root, [System.StringComparison]::OrdinalIgnoreCase)) {
    if (Test-Path -LiteralPath $cursor) {
      $item = Get-Item -LiteralPath $cursor -Force
      if (($item.Attributes -band [System.IO.FileAttributes]::ReparsePoint) -ne 0) {
        throw "$Label must not traverse a symlink or reparse point"
      }
    }
    $parent = [System.IO.Path]::GetDirectoryName($cursor)
    if ([string]::IsNullOrWhiteSpace($parent) -or $parent -eq $cursor) {
      throw "$Label ancestry could not be validated"
    }
    $cursor = $parent
  }
}

function Read-DotEnv {
  param([string]$Path)
  $values = @{}
  if (-not (Test-Path -LiteralPath $Path -PathType Leaf)) {
    return $values
  }
  foreach ($line in Get-Content -LiteralPath $Path) {
    if ($line -notmatch '^\s*(?:export\s+)?([A-Za-z_][A-Za-z0-9_]*)\s*=\s*(.*?)\s*$') {
      continue
    }
    $name = $matches[1]
    $value = $matches[2]
    if ($value.Length -ge 2) {
      $first = $value.Substring(0, 1)
      $last = $value.Substring($value.Length - 1, 1)
      if (($first -eq '"' -and $last -eq '"') -or ($first -eq "'" -and $last -eq "'")) {
        $value = $value.Substring(1, $value.Length - 2)
      }
    }
    $values[$name] = $value
  }
  return $values
}

function Normalize-SupabaseUrl {
  param([string]$Value)
  if ([string]::IsNullOrWhiteSpace($Value)) {
    throw "Supabase URL is blank"
  }
  $uri = $null
  if (-not [System.Uri]::TryCreate($Value.Trim(), [System.UriKind]::Absolute, [ref]$uri)) {
    throw "Supabase URL must be absolute"
  }
  if ($uri.Scheme -ne "https") {
    throw "Supabase URL must use HTTPS"
  }
  if (-not [string]::IsNullOrEmpty($uri.UserInfo)) {
    throw "Supabase URL must not contain credentials"
  }
  if (-not $uri.IsDefaultPort) {
    throw "Supabase URL must use the default HTTPS port"
  }
  $hostName = $uri.DnsSafeHost.ToLowerInvariant()
  if ($hostName -notmatch '^[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?\.supabase\.co$') {
    throw "Supabase URL host must be a project subdomain of supabase.co"
  }
  return "https://$hostName"
}

function Assert-ServiceKeyShape {
  param([string]$Value)
  if ([string]::IsNullOrWhiteSpace($Value) -or $Value.Length -lt 24) {
    throw "Supabase service-role key is missing or malformed"
  }
  if ($Value -match '\s' -or $Value.StartsWith("sb_publishable_", [System.StringComparison]::OrdinalIgnoreCase)) {
    throw "Supabase service-role key is missing or malformed"
  }
  $segments = $Value.Split('.')
  if ($segments.Count -eq 3 -and $Value.StartsWith("eyJ", [System.StringComparison]::Ordinal)) {
    try {
      $payload = $segments[1].Replace('-', '+').Replace('_', '/')
      while (($payload.Length % 4) -ne 0) { $payload += "=" }
      $json = [System.Text.Encoding]::UTF8.GetString([System.Convert]::FromBase64String($payload))
      $claims = $json | ConvertFrom-Json
      if (-not ($claims.PSObject.Properties.Name -contains "role") -or [string]$claims.role -ne "service_role") {
        throw "not service role"
      }
    } catch {
      throw "Supabase service-role key is missing or malformed"
    }
  }
}

function Resolve-ServiceKey {
  param(
    [string]$ExplicitValue,
    [string]$EnvironmentName,
    [hashtable]$DotEnv
  )
  if ($EnvironmentName -notmatch '^[A-Za-z_][A-Za-z0-9_]*$') {
    throw "service-key environment variable name is invalid"
  }
  $value = $ExplicitValue
  if ([string]::IsNullOrWhiteSpace($value)) {
    $value = [System.Environment]::GetEnvironmentVariable($EnvironmentName, "Process")
  }
  if ([string]::IsNullOrWhiteSpace($value) -and $DotEnv.ContainsKey($EnvironmentName)) {
    $value = [string]$DotEnv[$EnvironmentName]
  }
  if ([string]::IsNullOrWhiteSpace($value) -and $DotEnv.ContainsKey("SUPABASE_SERVICE_ROLE_KEY")) {
    $value = [string]$DotEnv["SUPABASE_SERVICE_ROLE_KEY"]
  }
  Assert-ServiceKeyShape $value
  return $value
}

function Protect-Message {
  param([string]$Message, [string]$Secret)
  $safe = [string]$Message
  if (-not [string]::IsNullOrEmpty($Secret)) {
    $safe = $safe.Replace($Secret, "[REDACTED]")
  }
  $safe = $safe -replace '[\r\n\t]+', ' '
  if ($safe.Length -gt 400) {
    $safe = $safe.Substring(0, 400) + "..."
  }
  return $safe
}

function Get-DeclaredPublicTables {
  param([string]$SqlPath)
  if (-not (Test-Path -LiteralPath $SqlPath -PathType Leaf)) {
    throw "apply-all.sql is missing"
  }
  $sql = Get-Content -LiteralPath $SqlPath -Raw
  $pattern = '(?im)^\s*create\s+table(?:\s+if\s+not\s+exists)?\s+(?:public\.)?"?([a-zA-Z_][a-zA-Z0-9_]*)"?\s*\('
  $names = @()
  foreach ($match in [System.Text.RegularExpressions.Regex]::Matches($sql, $pattern)) {
    $name = $match.Groups[1].Value.ToLowerInvariant()
    if ($names -notcontains $name) {
      $names += $name
    }
  }
  if ($names.Count -eq 0) {
    throw "apply-all.sql declares no public tables"
  }
  return $names
}

function Get-JsonArrayCount {
  param([string]$Json)
  $trimmed = $Json.Trim()
  if (-not ($trimmed.StartsWith("[") -and $trimmed.EndsWith("]"))) {
    throw "PostgREST returned a non-array payload"
  }
  if ($trimmed -eq "[]") {
    return 0
  }
  try {
    $parsed = $trimmed | ConvertFrom-Json
  } catch {
    throw "PostgREST returned invalid JSON"
  }
  return @($parsed).Count
}

function Get-JsonArrayInnerText {
  param([string]$Json)
  $trimmed = $Json.Trim()
  if (-not ($trimmed.StartsWith("[") -and $trimmed.EndsWith("]"))) {
    throw "PostgREST returned a non-array payload"
  }
  return $trimmed.Substring(1, $trimmed.Length - 2).Trim()
}

function Get-ContentRangeTotal {
  param([string]$ContentRange)
  if ([string]::IsNullOrWhiteSpace($ContentRange)) {
    return $null
  }
  if ($ContentRange -match '^\s*(?:\d+-\d+|\*)/(\d+)\s*$') {
    return [long]$matches[1]
  }
  throw "PostgREST returned an invalid Content-Range header"
}

function Test-MissingTableResponse {
  param([int]$StatusCode, [string]$Body)
  if ($StatusCode -ne 404) {
    return $false
  }
  return $Body -match '(?i)PGRST205|could not find the table|relation\s+.+\s+does not exist|42P01'
}

function Get-PostgrestTableUrl {
  param([string]$BaseUrl, [string]$Table)
  $encodedTable = [System.Uri]::EscapeDataString($Table)
  return "${BaseUrl}/rest/v1/${encodedTable}?select=*"
}

function Invoke-PostgrestPage {
  param(
    [System.Net.Http.HttpClient]$Client,
    [string]$BaseUrl,
    [string]$Table,
    [long]$Start,
    [long]$End,
    [string]$Key
  )
  $request = New-Object System.Net.Http.HttpRequestMessage(
    [System.Net.Http.HttpMethod]::Get,
    (Get-PostgrestTableUrl $BaseUrl $Table)
  )
  try {
    [void]$request.Headers.TryAddWithoutValidation("apikey", $Key)
    [void]$request.Headers.TryAddWithoutValidation("Authorization", "Bearer $Key")
    [void]$request.Headers.TryAddWithoutValidation("Range-Unit", "items")
    [void]$request.Headers.TryAddWithoutValidation("Range", "$Start-$End")
    [void]$request.Headers.TryAddWithoutValidation("Prefer", "count=exact")
    $response = $Client.SendAsync($request).GetAwaiter().GetResult()
    try {
      $body = $response.Content.ReadAsStringAsync().GetAwaiter().GetResult()
      $contentRange = $null
      if ($null -ne $response.Content.Headers.ContentRange) {
        $contentRange = $response.Content.Headers.ContentRange.ToString()
      }
      return [pscustomobject]@{
        StatusCode = [int]$response.StatusCode
        Body = $body
        ContentRange = $contentRange
      }
    } finally {
      $response.Dispose()
    }
  } finally {
    $request.Dispose()
  }
}

function New-BackupHttpClient {
  Add-Type -AssemblyName System.Net.Http
  $handler = New-Object System.Net.Http.HttpClientHandler
  $handler.AllowAutoRedirect = $false
  $client = New-Object System.Net.Http.HttpClient($handler)
  $client.Timeout = [System.TimeSpan]::FromSeconds(60)
  return $client
}

function New-BackupManifest {
  param(
    [string]$BaseUrl,
    [string]$MigrationPath,
    [string[]]$Tables,
    [int]$ManifestPageSize,
    [System.Collections.IEnumerable]$Results
  )
  $missingCount = 0
  $exportedCount = 0
  $exportedRows = [long]0
  $tableEntries = New-Object System.Collections.Generic.List[object]
  foreach ($entry in $Results) {
    if (-not ($entry -is [System.Collections.IDictionary])) {
      throw "backup manifest received a non-dictionary table result"
    }
    foreach ($requiredKey in @("name", "status", "rowCount")) {
      if (-not $entry.Contains($requiredKey)) {
        throw "backup manifest table result lacks '$requiredKey'"
      }
    }
    $entryName = [string]$entry["name"]
    $entryStatus = [string]$entry["status"]
    $entryRows = [long]$entry["rowCount"]
    if ($entryRows -lt 0) {
      throw "backup manifest table '$entryName' has a negative row count"
    }
    switch ($entryStatus) {
      "exported" {
        $exportedCount++
        $exportedRows += $entryRows
      }
      "missing_future_v5" {
        if ($script:FutureV5Tables -notcontains $entryName) {
          throw "backup manifest cannot classify non-V5 table '$entryName' as optional"
        }
        $missingCount++
      }
      default {
        throw "backup manifest table '$entryName' has unknown status '$entryStatus'"
      }
    }
    $tableEntries.Add($entry) | Out-Null
  }
  if ($tableEntries.Count -ne $Tables.Count) {
    throw "backup manifest result count does not match discovered table count"
  }

  # Results are OrderedDictionary values. Always use dictionary indexers above; dot-property
  # aggregation is not portable across Windows PowerShell and PowerShell 7.
  $manifest = [ordered]@{
    schemaVersion = 1
    status = if ($missingCount -gt 0) { "complete_with_missing_future_v5" } else { "complete" }
    createdAtUtc = [DateTime]::UtcNow.ToString("o")
    projectHost = ([Uri]$BaseUrl).DnsSafeHost
    source = [ordered]@{
      migration = "discord/supabase/apply-all.sql"
      migrationSha256 = (Get-FileHash -LiteralPath $MigrationPath -Algorithm SHA256).Hash.ToLowerInvariant()
      discoveredTableCount = $Tables.Count
    }
    paging = [ordered]@{
      pageSize = $ManifestPageSize
      exactCountRequired = $true
      consistency = "Per-table Content-Range total must remain stable for the full paged export."
    }
    totals = [ordered]@{
      exportedTables = $exportedCount
      missingFutureV5Tables = $missingCount
      exportedRows = $exportedRows
    }
    tables = @($tableEntries.ToArray())
  }
  return $manifest
}

function Invoke-OfflineSelfTest {
  Invoke-ScriptSyntaxCheck
  $root = Get-CanonicalRoot $RepoRoot
  $good = Normalize-SupabaseUrl "https://abc123.supabase.co/rest/v1"
  Assert-True ($good -eq "https://abc123.supabase.co") "valid project URL was not normalized"

  foreach ($bad in @(
    "http://abc123.supabase.co",
    "https://supabase.co",
    "https://abc123.supabase.co.evil.example",
    "https://user:pass@abc123.supabase.co"
  )) {
    $rejected = $false
    try { [void](Normalize-SupabaseUrl $bad) } catch { $rejected = $true }
    Assert-True $rejected "unsafe URL was accepted"
  }

  $sqlPath = Get-FullPathWithin $root "discord\supabase\apply-all.sql" "migration path"
  $tables = @(Get-DeclaredPublicTables $sqlPath)
  Assert-True ($tables.Count -ge 29) "migration table discovery is incomplete"
  foreach ($required in $script:FutureV5Tables) {
    Assert-True ($tables -contains $required) "V5 table discovery missed $required"
  }

  $fakeSecret = "sb_secret_SELF_TEST_ONLY_1234567890"
  Assert-ServiceKeyShape $fakeSecret
  $protected = Protect-Message "response echoed $fakeSecret" $fakeSecret
  Assert-True (-not $protected.Contains($fakeSecret)) "secret redaction failed"
  Assert-True ($protected.Contains("[REDACTED]")) "secret redaction marker is absent"

  $outsideRejected = $false
  try { [void](Get-FullPathWithin $root (Join-Path (Split-Path $root -Parent) "outside") "test path") } catch { $outsideRejected = $true }
  Assert-True $outsideRejected "repository path escape was accepted"
  Assert-True (Test-MissingTableResponse 404 '{"code":"PGRST205","message":"Could not find the table"}') "missing-table classification failed"
  Assert-True (-not (Test-MissingTableResponse 500 '{"code":"XX000"}')) "non-missing failure was misclassified"
  $tableUrl = Get-PostgrestTableUrl $good "investigation_nodes"
  Assert-True ($tableUrl -eq "https://abc123.supabase.co/rest/v1/investigation_nodes?select=*") "PostgREST query URL interpolation failed"

  $mixedResults = New-Object System.Collections.Generic.List[object]
  $mixedResults.Add([ordered]@{
    name = "players"
    status = "exported"
    rowCount = 7
    pages = 1
    file = "players.json"
    sha256 = ("a" * 64)
    note = $null
  }) | Out-Null
  $mixedResults.Add([ordered]@{
    name = "investigations"
    status = "missing_future_v5"
    rowCount = 0
    pages = 0
    file = $null
    sha256 = $null
    note = "self-test"
  }) | Out-Null
  $testManifest = New-BackupManifest `
    -BaseUrl $good `
    -MigrationPath $sqlPath `
    -Tables @("players", "investigations") `
    -ManifestPageSize 1000 `
    -Results $mixedResults
  Assert-True ($testManifest -is [System.Collections.IDictionary]) "manifest builder did not return a dictionary"
  Assert-True ([string]$testManifest["status"] -eq "complete_with_missing_future_v5") "mixed manifest status is incorrect"
  Assert-True ([long]$testManifest["totals"]["exportedRows"] -eq 7) "mixed manifest exported-row sum is incorrect"
  Assert-True ([int]$testManifest["totals"]["exportedTables"] -eq 1) "mixed manifest exported-table count is incorrect"
  Assert-True ([int]$testManifest["totals"]["missingFutureV5Tables"] -eq 1) "mixed manifest missing-table count is incorrect"
  $manifestJson = $testManifest | ConvertTo-Json -Depth 12
  $manifestRoundTrip = $manifestJson | ConvertFrom-Json
  Assert-True ([long]$manifestRoundTrip.totals.exportedRows -eq 7) "mixed manifest JSON round trip lost totals"
  Write-Host "backup_supabase_v5 self-test: PASS - syntax, table discovery, host/path guards, key redaction, and mixed ordered-dictionary manifest"
}

function Invoke-Backup {
  $root = Get-CanonicalRoot $RepoRoot
  $envPath = Get-FullPathWithin $root $EnvFile "environment file"
  $dotEnv = Read-DotEnv $envPath

  $urlValue = $SupabaseUrl
  if ([string]::IsNullOrWhiteSpace($urlValue)) {
    $urlValue = [System.Environment]::GetEnvironmentVariable("NEXT_PUBLIC_SUPABASE_URL", "Process")
  }
  if ([string]::IsNullOrWhiteSpace($urlValue) -and $dotEnv.ContainsKey("NEXT_PUBLIC_SUPABASE_URL")) {
    $urlValue = [string]$dotEnv["NEXT_PUBLIC_SUPABASE_URL"]
  }
  if ([string]::IsNullOrWhiteSpace($urlValue)) {
    $urlValue = $script:DefaultProjectUrl
  }
  $baseUrl = Normalize-SupabaseUrl $urlValue

  $migrationPath = Get-FullPathWithin $root "discord\supabase\apply-all.sql" "migration path"
  $tables = @(Get-DeclaredPublicTables $migrationPath)
  $buildRoot = Get-FullPathWithin $root "build" "build root"
  Assert-NoReparseAncestors $root $buildRoot "build root"
  New-Item -ItemType Directory -Path $buildRoot -Force | Out-Null
  Assert-NoReparseAncestors $root $buildRoot "build root"
  $backupRoot = Get-FullPathWithin $buildRoot "supabase-v5-backups" "backup root"
  New-Item -ItemType Directory -Path $backupRoot -Force | Out-Null
  Assert-NoReparseAncestors $root $backupRoot "backup root"
  $stamp = [DateTime]::UtcNow.ToString("yyyyMMddTHHmmssZ")
  $outputDir = Get-FullPathWithin $root (Join-Path "build\supabase-v5-backups" $stamp) "backup directory"
  if (Test-Path -LiteralPath $outputDir) {
    $stamp += "-" + [Guid]::NewGuid().ToString("N").Substring(0, 8)
    $outputDir = Get-FullPathWithin $root (Join-Path "build\supabase-v5-backups" $stamp) "backup directory"
  }
  New-Item -ItemType Directory -Path $outputDir | Out-Null
  [System.IO.File]::WriteAllText((Join-Path $outputDir "INCOMPLETE"), "Backup is not complete.`n", $script:Utf8NoBom)

  $resolvedKey = ""
  $client = $null
  $results = New-Object System.Collections.Generic.List[object]
  try {
    $resolvedKey = Resolve-ServiceKey $ServiceKey $ServiceKeyEnv $dotEnv
    $client = New-BackupHttpClient
    foreach ($table in $tables) {
      $fileName = "$table.json"
      $finalPath = Get-FullPathWithin $root (Join-Path $outputDir $fileName) "table export"
      $partialPath = Get-FullPathWithin $root ($finalPath + ".partial") "partial table export"
      $writer = New-Object System.IO.StreamWriter($partialPath, $false, $script:Utf8NoBom)
      $rowCount = [long]0
      $pageCount = 0
      $expectedTotal = $null
      $firstRowWritten = $false
      $missingFuture = $false
      try {
        $writer.Write("[")
        while ($true) {
          $end = $rowCount + $PageSize - 1
          $page = Invoke-PostgrestPage $client $baseUrl $table $rowCount $end $resolvedKey
          if ($page.StatusCode -notin @(200, 206)) {
            if ($rowCount -eq 0 -and ($script:FutureV5Tables -contains $table) -and
                (Test-MissingTableResponse $page.StatusCode $page.Body)) {
              $missingFuture = $true
              break
            }
            $safeBody = Protect-Message $page.Body $resolvedKey
            throw "table '$table' read failed with HTTP $($page.StatusCode): $safeBody"
          }

          $pageRows = Get-JsonArrayCount $page.Body
          $pageTotal = Get-ContentRangeTotal $page.ContentRange
          if ($null -ne $pageTotal) {
            if ($null -eq $expectedTotal) {
              $expectedTotal = [long]$pageTotal
            } elseif ([long]$expectedTotal -ne [long]$pageTotal) {
              throw "table '$table' changed row count during paged backup; retry from a quiet database"
            }
          }
          if ($pageRows -eq 0) {
            if ($null -ne $expectedTotal -and $rowCount -lt [long]$expectedTotal) {
              throw "table '$table' returned an empty page before its declared row count"
            }
            break
          }

          $inner = Get-JsonArrayInnerText $page.Body
          if (-not [string]::IsNullOrWhiteSpace($inner)) {
            if ($firstRowWritten) { $writer.Write(",") }
            $writer.Write($inner)
            $firstRowWritten = $true
          }
          $rowCount += $pageRows
          $pageCount++
          if ($null -ne $expectedTotal) {
            if ($rowCount -ge [long]$expectedTotal) { break }
          } elseif ($pageRows -lt $PageSize) {
            break
          }
        }
        $writer.Write("]")
      } finally {
        $writer.Dispose()
      }

      if ($missingFuture) {
        Remove-Item -LiteralPath $partialPath -Force
        $results.Add([ordered]@{
          name = $table
          status = "missing_future_v5"
          rowCount = 0
          pages = 0
          file = $null
          sha256 = $null
          note = "Additive V5 table is not present in the live PostgREST schema cache."
        }) | Out-Null
        Write-Host "backup: $table - noted missing additive V5 table"
        continue
      }

      if ($null -ne $expectedTotal -and $rowCount -ne [long]$expectedTotal) {
        throw "table '$table' export count does not match Content-Range"
      }
      Move-Item -LiteralPath $partialPath -Destination $finalPath
      $sha256 = (Get-FileHash -LiteralPath $finalPath -Algorithm SHA256).Hash.ToLowerInvariant()
      $results.Add([ordered]@{
        name = $table
        status = "exported"
        rowCount = $rowCount
        pages = $pageCount
        file = $fileName
        sha256 = $sha256
        note = $null
      }) | Out-Null
      Write-Host "backup: $table - $rowCount rows"
    }

    $manifest = New-BackupManifest `
      -BaseUrl $baseUrl `
      -MigrationPath $migrationPath `
      -Tables $tables `
      -ManifestPageSize $PageSize `
      -Results $results
    $manifestPath = Get-FullPathWithin $root (Join-Path $outputDir "manifest.json") "backup manifest"
    [System.IO.File]::WriteAllText(
      $manifestPath,
      ($manifest | ConvertTo-Json -Depth 12),
      $script:Utf8NoBom
    )
    Remove-Item -LiteralPath (Join-Path $outputDir "INCOMPLETE") -Force
    Write-Host "Supabase V5 backup complete: $outputDir"
    Write-Host "manifest: $manifestPath"
  } catch {
    $safeError = Protect-Message $_.Exception.Message $resolvedKey
    throw "Supabase V5 backup failed (INCOMPLETE marker retained): $safeError"
  } finally {
    if ($null -ne $client) { $client.Dispose() }
    $resolvedKey = ""
  }
}

if ($SyntaxCheck -or $SelfTest) {
  Invoke-ScriptSyntaxCheck
  if ($SelfTest) {
    Invoke-OfflineSelfTest
  } else {
    Write-Host "backup_supabase_v5 syntax check: PASS"
  }
  return
}

Invoke-Backup
