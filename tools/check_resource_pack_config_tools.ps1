param(
  [string]$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
)

$ErrorActionPreference = "Stop"

function Add-Failure([string]$Message) {
  $script:failures.Add($Message) | Out-Null
}

function Run-Setter([string[]]$ArgsList) {
  $scriptPath = Join-Path $repoFull "tools\set_resource_pack_config.ps1"
  $oldErrorActionPreference = $ErrorActionPreference
  $ErrorActionPreference = "Continue"
  try {
    $output = & powershell -NoProfile -ExecutionPolicy Bypass -File $scriptPath @ArgsList 2>&1
    $exitCode = $LASTEXITCODE
    return [ordered]@{
      exitCode = $exitCode
      output = @($output | ForEach-Object { [string]$_ })
    }
  } finally {
    $ErrorActionPreference = $oldErrorActionPreference
  }
}

function Resource-Pack-Value([string]$Text, [string]$Key) {
  $pattern = "(?ms)^resource-pack:\s.*?^\s+$([regex]::Escape($Key)):\s+`"([^`"]*)`""
  $match = [regex]::Match($Text, $pattern)
  if ($match.Success) { return $match.Groups[1].Value }
  return ""
}

$repoFull = [System.IO.Path]::GetFullPath($RepoRoot)
$failures = [System.Collections.Generic.List[string]]::new()

$setter = Join-Path $repoFull "tools\set_resource_pack_config.ps1"
$sourceConfig = Join-Path $repoFull "plugin\src\main\resources\config.yml"
$resourceZip = Join-Path $repoFull "observance-resourcepack.zip"

foreach ($file in @($setter, $sourceConfig, $resourceZip)) {
  if (!(Test-Path $file)) {
    Add-Failure "Missing required file: $file"
  }
}

if ($failures.Count -eq 0) {
  $expectedSha1 = (Get-FileHash -LiteralPath $resourceZip -Algorithm SHA1).Hash.ToLowerInvariant()
  $originalConfig = Get-Content -LiteralPath $sourceConfig -Raw

  $dryRun = Run-Setter @("-Url", "https://example.com/observance-resourcepack.zip", "-DryRun", "-RepoRoot", $repoFull)
  if ($dryRun.exitCode -ne 0) {
    Add-Failure "set_resource_pack_config dry run failed: $($dryRun.output -join ' ')"
  }
  $afterDryRun = Get-Content -LiteralPath $sourceConfig -Raw
  if ($afterDryRun -ne $originalConfig) {
    Add-Failure "set_resource_pack_config dry run changed the real plugin config"
  }
  if (($dryRun.output -join "`n").IndexOf($expectedSha1, [System.StringComparison]::OrdinalIgnoreCase) -lt 0) {
    Add-Failure "set_resource_pack_config dry run did not print the current resource-pack SHA1"
  }

  $badUrl = Run-Setter @("-Url", "http://example.com/observance-resourcepack.zip", "-DryRun", "-RepoRoot", $repoFull)
  if ($badUrl.exitCode -eq 0) {
    Add-Failure "set_resource_pack_config accepted a non-HTTPS URL"
  }

  $badPath = Run-Setter @("-Url", "https://example.com/not-a-pack.txt", "-DryRun", "-RepoRoot", $repoFull)
  if ($badPath.exitCode -eq 0) {
    Add-Failure "set_resource_pack_config accepted a non-zip URL"
  }

  $smokeDir = Join-Path $repoFull "build\check-resource-pack-config-tools"
  New-Item -ItemType Directory -Force -Path $smokeDir | Out-Null
  $tempConfig = Join-Path $smokeDir "config.yml"
  Copy-Item -LiteralPath $sourceConfig -Destination $tempConfig -Force

  $writeRun = Run-Setter @(
    "-Url", "https://example.com/observance-resourcepack.zip",
    "-ConfigPath", $tempConfig,
    "-RepoRoot", $repoFull
  )
  if ($writeRun.exitCode -ne 0) {
    Add-Failure "set_resource_pack_config temp write failed: $($writeRun.output -join ' ')"
  } else {
    $tempText = Get-Content -LiteralPath $tempConfig -Raw
    $url = Resource-Pack-Value $tempText "url"
    $sha1 = Resource-Pack-Value $tempText "sha1"
    if ($url -ne "https://example.com/observance-resourcepack.zip") {
      Add-Failure "set_resource_pack_config wrote wrong resource-pack.url to temp config: $url"
    }
    if ($sha1 -ne $expectedSha1) {
      Add-Failure "set_resource_pack_config wrote wrong resource-pack.sha1 to temp config: $sha1"
    }
    $supabaseUrl = [regex]::Match($tempText, '(?ms)^supabase:\s.*?^\s+url:\s+"([^"]*)"').Groups[1].Value
    if ($supabaseUrl -ne "https://fdnmhbpxnodrnbrzrlqq.supabase.co/rest/v1") {
      Add-Failure "set_resource_pack_config changed the Supabase URL in temp config"
    }
  }
}

if ($failures.Count -gt 0) {
  Write-Host "resource-pack config tool check: FAILED"
  foreach ($failure in $failures) {
    Write-Host "  - $failure"
  }
  exit 1
}

Write-Host "resource-pack config tool check: OK - setter dry-run, URL validation, SHA calculation, and scoped config write hold"
