<#
.SYNOPSIS
Uploads the deterministic Observance resource-pack zip to Supabase Storage and verifies the public bytes.

.EXAMPLE
  powershell -NoProfile -ExecutionPolicy Bypass -File tools\publish_resource_pack_to_supabase.ps1 `
    -ZipPath observance-resourcepack.zip `
    -ExpectedSha1 aeadf5b566b435a80a5bdc8ace70090cf6959393 `
    -ServiceKeyEnv OBSERVANCE_SUPABASE_KEY

.EXAMPLE
  powershell -NoProfile -ExecutionPolicy Bypass -File tools\publish_resource_pack_to_supabase.ps1 -SelfTest

.NOTES
When URL, bucket, object, or expected SHA-1 is omitted, the resource-pack values in
plugin/src/main/resources/config.yml are used. Prefer -ServiceKeyEnv or the gitignored
dashboard/.env.local. Although -ServiceKey is supported, a command-line secret may be visible in
shell history or process inspection. The script never prints or writes the key.
#>
[CmdletBinding()]
param(
  [string]$RepoRoot = "",
  [string]$ZipPath = "observance-resourcepack.zip",
  [string]$ExpectedSha1 = "",
  [string]$SupabaseUrl = "",
  [string]$Bucket = "",
  [string]$ObjectPath = "",
  [string]$ServiceKey = "",
  [string]$ServiceKeyEnv = "OBSERVANCE_SUPABASE_KEY",
  [string]$EnvFile = "dashboard\.env.local",
  [switch]$SyntaxCheck,
  [switch]$SelfTest
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"
if ([string]::IsNullOrWhiteSpace($RepoRoot)) {
  $RepoRoot = Split-Path -Parent $PSScriptRoot
}
$script:Utf8NoBom = New-Object System.Text.UTF8Encoding($false)
$script:FallbackProjectUrl = "https://fdnmhbpxnodrnbrzrlqq.supabase.co"
$script:FallbackBucket = "observance-public"
$script:FallbackObject = "observance-resourcepack.zip"

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
    throw "$Label must stay inside $Root"
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

function Assert-NotReparseFile {
  param([string]$Path, [string]$Label)
  $item = Get-Item -LiteralPath $Path -Force
  if (($item.Attributes -band [System.IO.FileAttributes]::ReparsePoint) -ne 0) {
    throw "$Label must not be a symlink or reparse point"
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

function Get-ResourcePackConfigDefaults {
  param([string]$Root)
  $configPath = Get-FullPathWithin $Root "plugin\src\main\resources\config.yml" "plugin config"
  if (-not (Test-Path -LiteralPath $configPath -PathType Leaf)) {
    throw "plugin resource-pack config is missing"
  }
  $inSection = $false
  $configuredUrl = ""
  $configuredSha1 = ""
  foreach ($line in Get-Content -LiteralPath $configPath) {
    if ($line -match '^resource-pack:\s*$') {
      $inSection = $true
      continue
    }
    if ($inSection -and $line -match '^\S') {
      break
    }
    if (-not $inSection) { continue }
    if ($line -match '^\s+url:\s*["'']?([^"'']*)["'']?\s*$') {
      $configuredUrl = $matches[1].Trim()
    } elseif ($line -match '^\s+sha1:\s*["'']?([0-9A-Fa-f]*)["'']?\s*$') {
      $configuredSha1 = $matches[1].Trim().ToLowerInvariant()
    }
  }
  if ([string]::IsNullOrWhiteSpace($configuredUrl)) {
    throw "plugin resource-pack URL is blank"
  }
  $uri = $null
  if (-not [System.Uri]::TryCreate($configuredUrl, [System.UriKind]::Absolute, [ref]$uri)) {
    throw "plugin resource-pack URL is invalid"
  }
  $origin = Normalize-SupabaseUrl $configuredUrl
  $prefix = "/storage/v1/object/public/"
  if (-not $uri.AbsolutePath.StartsWith($prefix, [System.StringComparison]::Ordinal)) {
    throw "plugin resource-pack URL is not a Supabase public-storage object"
  }
  $relative = $uri.AbsolutePath.Substring($prefix.Length)
  $segments = @($relative.Split('/') | Where-Object { $_.Length -gt 0 } | ForEach-Object {
    [System.Uri]::UnescapeDataString($_)
  })
  if ($segments.Count -lt 2) {
    throw "plugin resource-pack URL lacks a bucket or object path"
  }
  return [pscustomobject]@{
    SupabaseUrl = $origin
    Bucket = $segments[0]
    ObjectPath = ($segments[1..($segments.Count - 1)] -join "/")
    ExpectedSha1 = $configuredSha1
  }
}

function Assert-StorageNames {
  param([string]$BucketName, [string]$StorageObject)
  if ($BucketName -notmatch '^[a-z0-9][a-z0-9._-]{0,62}$') {
    throw "storage bucket name is invalid"
  }
  if ([string]::IsNullOrWhiteSpace($StorageObject) -or $StorageObject.Length -gt 512 -or
      $StorageObject.Contains("\") -or $StorageObject.StartsWith("/") -or
      $StorageObject.Contains("?") -or $StorageObject.Contains("#")) {
    throw "storage object path is invalid"
  }
  foreach ($segment in $StorageObject.Split('/')) {
    if ([string]::IsNullOrWhiteSpace($segment) -or $segment -in @(".", "..")) {
      throw "storage object path is invalid"
    }
  }
}

function Get-EncodedObjectPath {
  param([string]$StorageObject)
  return (($StorageObject.Split('/') | ForEach-Object { [System.Uri]::EscapeDataString($_) }) -join "/")
}

function Get-StorageUrls {
  param([string]$BaseUrl, [string]$BucketName, [string]$StorageObject)
  Assert-StorageNames $BucketName $StorageObject
  $encodedBucket = [System.Uri]::EscapeDataString($BucketName)
  $encodedObject = Get-EncodedObjectPath $StorageObject
  return [pscustomobject]@{
    Upload = "${BaseUrl}/storage/v1/object/${encodedBucket}/${encodedObject}"
    Public = "${BaseUrl}/storage/v1/object/public/${encodedBucket}/${encodedObject}"
  }
}

function Assert-ZipSignature {
  param([string]$Path)
  if (-not (Test-Path -LiteralPath $Path -PathType Leaf)) {
    throw "resource-pack zip is missing"
  }
  $stream = [System.IO.File]::OpenRead($Path)
  try {
    if ($stream.Length -lt 4) { throw "resource-pack file is not a zip payload" }
    $signature = New-Object byte[] 4
    [void]$stream.Read($signature, 0, 4)
    $isZip = $signature[0] -eq 0x50 -and $signature[1] -eq 0x4b -and
      (($signature[2] -eq 0x03 -and $signature[3] -eq 0x04) -or
       ($signature[2] -eq 0x05 -and $signature[3] -eq 0x06) -or
       ($signature[2] -eq 0x07 -and $signature[3] -eq 0x08))
    if (-not $isZip) { throw "resource-pack file is not a zip payload" }
  } finally {
    $stream.Dispose()
  }
}

function Assert-ExpectedHash {
  param([string]$Path, [string]$Expected)
  if ($Expected -notmatch '^[0-9A-Fa-f]{40}$') {
    throw "expected resource-pack SHA-1 must be exactly 40 hexadecimal characters"
  }
  $actual = (Get-FileHash -LiteralPath $Path -Algorithm SHA1).Hash.ToLowerInvariant()
  if ($actual -ne $Expected.ToLowerInvariant()) {
    throw "local resource-pack SHA-1 does not match the expected SHA-1; upload refused"
  }
  return $actual
}

function New-StorageHttpClient {
  Add-Type -AssemblyName System.Net.Http
  $handler = New-Object System.Net.Http.HttpClientHandler
  $handler.AllowAutoRedirect = $false
  $client = New-Object System.Net.Http.HttpClient($handler)
  $client.Timeout = [System.TimeSpan]::FromSeconds(120)
  return $client
}

function Invoke-StorageUpload {
  param(
    [System.Net.Http.HttpClient]$Client,
    [string]$Url,
    [string]$Path,
    [string]$Key
  )
  $stream = [System.IO.File]::OpenRead($Path)
  $request = New-Object System.Net.Http.HttpRequestMessage([System.Net.Http.HttpMethod]::Post, $Url)
  try {
    $content = New-Object System.Net.Http.StreamContent($stream)
    $content.Headers.ContentType = New-Object System.Net.Http.Headers.MediaTypeHeaderValue("application/zip")
    $request.Content = $content
    [void]$request.Headers.TryAddWithoutValidation("apikey", $Key)
    [void]$request.Headers.TryAddWithoutValidation("Authorization", "Bearer $Key")
    [void]$request.Headers.TryAddWithoutValidation("x-upsert", "true")
    [void]$request.Headers.TryAddWithoutValidation("cache-control", "no-cache")
    $response = $Client.SendAsync($request).GetAwaiter().GetResult()
    try {
      $body = $response.Content.ReadAsStringAsync().GetAwaiter().GetResult()
      return [pscustomobject]@{
        StatusCode = [int]$response.StatusCode
        Body = $body
      }
    } finally {
      $response.Dispose()
    }
  } finally {
    $request.Dispose()
    $stream.Dispose()
  }
}

function Invoke-PublicDownload {
  param(
    [System.Net.Http.HttpClient]$Client,
    [string]$Url,
    [string]$Destination
  )
  $request = New-Object System.Net.Http.HttpRequestMessage([System.Net.Http.HttpMethod]::Get, $Url)
  try {
    [void]$request.Headers.TryAddWithoutValidation("Cache-Control", "no-cache")
    $response = $Client.SendAsync($request, [System.Net.Http.HttpCompletionOption]::ResponseHeadersRead).GetAwaiter().GetResult()
    try {
      if ([int]$response.StatusCode -ne 200) {
        $body = $response.Content.ReadAsStringAsync().GetAwaiter().GetResult()
        return [pscustomobject]@{ StatusCode = [int]$response.StatusCode; Body = $body }
      }
      $inputStream = $response.Content.ReadAsStreamAsync().GetAwaiter().GetResult()
      try {
        $outputStream = New-Object System.IO.FileStream(
          $Destination,
          [System.IO.FileMode]::Create,
          [System.IO.FileAccess]::Write,
          [System.IO.FileShare]::None
        )
        try {
          $inputStream.CopyTo($outputStream)
        } finally {
          $outputStream.Dispose()
        }
      } finally {
        $inputStream.Dispose()
      }
      return [pscustomobject]@{ StatusCode = 200; Body = "" }
    } finally {
      $response.Dispose()
    }
  } finally {
    $request.Dispose()
  }
}

function Test-FilesByteEqual {
  param([string]$Left, [string]$Right)
  $leftInfo = Get-Item -LiteralPath $Left
  $rightInfo = Get-Item -LiteralPath $Right
  if ($leftInfo.Length -ne $rightInfo.Length) { return $false }
  $leftStream = [System.IO.File]::OpenRead($Left)
  $rightStream = [System.IO.File]::OpenRead($Right)
  try {
    $leftBuffer = New-Object byte[] 65536
    $rightBuffer = New-Object byte[] 65536
    while ($true) {
      $leftRead = $leftStream.Read($leftBuffer, 0, $leftBuffer.Length)
      $rightRead = $rightStream.Read($rightBuffer, 0, $rightBuffer.Length)
      if ($leftRead -ne $rightRead) { return $false }
      if ($leftRead -eq 0) { return $true }
      for ($i = 0; $i -lt $leftRead; $i++) {
        if ($leftBuffer[$i] -ne $rightBuffer[$i]) { return $false }
      }
    }
  } finally {
    $leftStream.Dispose()
    $rightStream.Dispose()
  }
}

function Invoke-OfflineSelfTest {
  Invoke-ScriptSyntaxCheck
  $root = Get-CanonicalRoot $RepoRoot
  $defaults = Get-ResourcePackConfigDefaults $root
  Assert-True ($defaults.SupabaseUrl -eq $script:FallbackProjectUrl) "configured project URL differs from the release default"
  Assert-True ($defaults.Bucket -eq $script:FallbackBucket) "configured bucket differs from the release default"
  Assert-True ($defaults.ObjectPath -eq $script:FallbackObject) "configured object differs from the release default"

  $good = Normalize-SupabaseUrl "https://abc123.supabase.co/storage/v1/object/public/a/b.zip"
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
  $objectRejected = $false
  try { Assert-StorageNames "observance-public" "../escape.zip" } catch { $objectRejected = $true }
  Assert-True $objectRejected "unsafe object path was accepted"

  $fakeSecret = "sb_secret_SELF_TEST_ONLY_1234567890"
  Assert-ServiceKeyShape $fakeSecret
  $protected = Protect-Message "response echoed $fakeSecret" $fakeSecret
  Assert-True (-not $protected.Contains($fakeSecret)) "secret redaction failed"

  $buildRoot = Get-FullPathWithin $root "build" "build root"
  New-Item -ItemType Directory -Path $buildRoot -Force | Out-Null
  $testDir = Get-FullPathWithin $buildRoot "release-tool-selftest" "self-test directory"
  New-Item -ItemType Directory -Path $testDir -Force | Out-Null
  $testPath = Get-FullPathWithin $buildRoot (Join-Path "release-tool-selftest" ([Guid]::NewGuid().ToString("N") + ".zip")) "self-test file"
  try {
    [System.IO.File]::WriteAllBytes($testPath, [byte[]]@(0x50,0x4b,0x03,0x04,0x01,0x02,0x03,0x04))
    Assert-ZipSignature $testPath
    $actual = (Get-FileHash -LiteralPath $testPath -Algorithm SHA1).Hash.ToLowerInvariant()
    Assert-True ((Assert-ExpectedHash $testPath $actual) -eq $actual) "valid hash was rejected"
    $mismatchRejected = $false
    try { [void](Assert-ExpectedHash $testPath ("0" * 40)) } catch { $mismatchRejected = $true }
    Assert-True $mismatchRejected "SHA-1 mismatch guard did not refuse publication"

    $outsideRejected = $false
    try { [void](Get-FullPathWithin $root (Join-Path (Split-Path $root -Parent) "outside.zip") "zip") } catch { $outsideRejected = $true }
    Assert-True $outsideRejected "repository path escape was accepted"
    $urls = Get-StorageUrls $good "observance-public" "nested/pack file.zip"
    Assert-True ($urls.Public -match 'nested/pack%20file\.zip$') "object URL encoding failed"
  } finally {
    if (Test-Path -LiteralPath $testPath -PathType Leaf) {
      Remove-Item -LiteralPath $testPath -Force
    }
  }
  Write-Host "publish_resource_pack_to_supabase self-test: PASS - syntax, host/path guards, hash refusal, URL encoding, and key redaction"
}

function Invoke-Publish {
  $root = Get-CanonicalRoot $RepoRoot
  $defaults = Get-ResourcePackConfigDefaults $root
  $baseUrl = Normalize-SupabaseUrl $(if ([string]::IsNullOrWhiteSpace($SupabaseUrl)) { $defaults.SupabaseUrl } else { $SupabaseUrl })
  $bucketValue = if ([string]::IsNullOrWhiteSpace($Bucket)) { $defaults.Bucket } else { $Bucket }
  $objectValue = if ([string]::IsNullOrWhiteSpace($ObjectPath)) { $defaults.ObjectPath } else { $ObjectPath }
  $expectedValue = if ([string]::IsNullOrWhiteSpace($ExpectedSha1)) { $defaults.ExpectedSha1 } else { $ExpectedSha1 }
  Assert-StorageNames $bucketValue $objectValue
  $urls = Get-StorageUrls $baseUrl $bucketValue $objectValue

  $zipFull = Get-FullPathWithin $root $ZipPath "resource-pack zip"
  if (-not (Test-Path -LiteralPath $zipFull -PathType Leaf)) {
    throw "resource-pack zip is missing"
  }
  Assert-NotReparseFile $zipFull "resource-pack zip"
  Assert-NoReparseAncestors $root $zipFull "resource-pack zip"
  Assert-ZipSignature $zipFull
  # Deliberately validate the local artifact before resolving a secret or making any request.
  $actualSha1 = Assert-ExpectedHash $zipFull $expectedValue
  $sourceSize = (Get-Item -LiteralPath $zipFull).Length

  $envPath = Get-FullPathWithin $root $EnvFile "environment file"
  $dotEnv = Read-DotEnv $envPath
  $resolvedKey = ""
  $client = $null
  $tempPath = ""
  $tempOwnedByThisRun = $false
  try {
    $resolvedKey = Resolve-ServiceKey $ServiceKey $ServiceKeyEnv $dotEnv
    $buildRoot = Get-FullPathWithin $root "build" "build root"
    Assert-NoReparseAncestors $root $buildRoot "build root"
    New-Item -ItemType Directory -Path $buildRoot -Force | Out-Null
    Assert-NoReparseAncestors $root $buildRoot "build root"
    $publishRoot = Get-FullPathWithin $buildRoot "resource-pack-publish" "publish work directory"
    New-Item -ItemType Directory -Path $publishRoot -Force | Out-Null
    Assert-NoReparseAncestors $root $publishRoot "publish work directory"
    $tempName = [Guid]::NewGuid().ToString("N") + ".download.tmp"
    $tempPath = Get-FullPathWithin $publishRoot $tempName "download temp file"
    $claim = New-Object System.IO.FileStream(
      $tempPath,
      [System.IO.FileMode]::CreateNew,
      [System.IO.FileAccess]::Write,
      [System.IO.FileShare]::None
    )
    $claim.Dispose()
    $tempOwnedByThisRun = $true

    $client = New-StorageHttpClient
    $upload = Invoke-StorageUpload $client $urls.Upload $zipFull $resolvedKey
    if ($upload.StatusCode -notin @(200, 201)) {
      $safeBody = Protect-Message $upload.Body $resolvedKey
      throw "Supabase Storage upload failed with HTTP $($upload.StatusCode): $safeBody"
    }

    $cacheBustedPublicUrl = $urls.Public + "?observance_sha1=" + $actualSha1
    $download = Invoke-PublicDownload $client $cacheBustedPublicUrl $tempPath
    if ($download.StatusCode -ne 200) {
      $safeBody = Protect-Message $download.Body $resolvedKey
      throw "public resource-pack verification failed with HTTP $($download.StatusCode): $safeBody"
    }
    $downloadSize = (Get-Item -LiteralPath $tempPath).Length
    $downloadSha1 = (Get-FileHash -LiteralPath $tempPath -Algorithm SHA1).Hash.ToLowerInvariant()
    if ($downloadSize -ne $sourceSize -or $downloadSha1 -ne $actualSha1 -or
        -not (Test-FilesByteEqual $zipFull $tempPath)) {
      throw "public resource-pack bytes do not exactly match the local artifact"
    }

    $receipt = [ordered]@{
      schemaVersion = 1
      verifiedAtUtc = [DateTime]::UtcNow.ToString("o")
      publicUrl = $urls.Public
      bucket = $bucketValue
      object = $objectValue
      bytes = $sourceSize
      sha1 = $actualSha1
      sha256 = (Get-FileHash -LiteralPath $zipFull -Algorithm SHA256).Hash.ToLowerInvariant()
      verification = "downloaded public bytes matched size, SHA-1, and byte-for-byte comparison"
    }
    $receiptName = "publish-" + [DateTime]::UtcNow.ToString("yyyyMMddTHHmmssZ") + ".json"
    $receiptPath = Get-FullPathWithin $publishRoot $receiptName "publication receipt"
    [System.IO.File]::WriteAllText($receiptPath, ($receipt | ConvertTo-Json -Depth 6), $script:Utf8NoBom)

    Write-Host "Resource pack published and byte-verified."
    Write-Host "public URL: $($urls.Public)"
    Write-Host "SHA-1: $actualSha1"
    Write-Host "bytes: $sourceSize"
    Write-Host "receipt: $receiptPath"
  } catch {
    $safeError = Protect-Message $_.Exception.Message $resolvedKey
    throw "resource-pack publication failed: $safeError"
  } finally {
    if ($null -ne $client) { $client.Dispose() }
    if ($tempOwnedByThisRun -and -not [string]::IsNullOrWhiteSpace($tempPath) -and
        (Test-Path -LiteralPath $tempPath -PathType Leaf)) {
      $buildRootForCleanup = Get-FullPathWithin $root "build" "build root"
      [void](Get-FullPathWithin $buildRootForCleanup $tempPath "download temp file")
      if ([System.IO.Path]::GetExtension($tempPath) -eq ".tmp") {
        Remove-Item -LiteralPath $tempPath -Force
      }
    }
    $resolvedKey = ""
  }
}

if ($SyntaxCheck -or $SelfTest) {
  Invoke-ScriptSyntaxCheck
  if ($SelfTest) {
    Invoke-OfflineSelfTest
  } else {
    Write-Host "publish_resource_pack_to_supabase syntax check: PASS"
  }
  return
}

Invoke-Publish
