param(
  [string]$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path,
  [string]$VideoRoot,
  [string]$PacketRoot
)

$ErrorActionPreference = "Stop"
$receiptPath = Join-Path $RepoRoot "campaign\p5-p12\media-custody.json"
$receipt = Get-Content -LiteralPath $receiptPath -Raw | ConvertFrom-Json
if (-not $VideoRoot) { $VideoRoot = [string]$receipt.video_root }
if (-not $PacketRoot) { $PacketRoot = [string]$receipt.packet_root }

$videoIds = @("clip_01_ash_locker", "clip_02_reeds_cache", "clip_03_watch_correction", "clip_04_release_instruction")
foreach ($asset in $receipt.assets) {
  $root = if ($videoIds -contains [string]$asset.id) { $VideoRoot } else { $PacketRoot }
  $relative = ([string]$asset.relative_path).Replace('/', [IO.Path]::DirectorySeparatorChar)
  $path = Join-Path $root $relative
  if (-not (Test-Path -LiteralPath $path -PathType Leaf)) { throw "Missing custody asset: $($asset.id) at $path" }
  $item = Get-Item -LiteralPath $path
  $sha1 = (Get-FileHash -LiteralPath $path -Algorithm SHA1).Hash.ToLowerInvariant()
  $sha256 = (Get-FileHash -LiteralPath $path -Algorithm SHA256).Hash.ToLowerInvariant()
  if ($item.Length -ne [long]$asset.bytes -or $sha1 -ne [string]$asset.sha1 -or $sha256 -ne [string]$asset.sha256) {
    throw "Custody mismatch: $($asset.id)"
  }
  Write-Host "MEDIA CUSTODY PASS $($asset.id) bytes=$($item.Length) sha256=$sha256"
}

if ($receipt.originals_mutated -ne $false -or $receipt.bytes_committed_to_git -ne $false -or $receipt.deployment_claimed -ne $false) {
  throw "Media custody receipt weakens non-destructive/deployment boundary"
}
Write-Host "P5-P12 MEDIA CUSTODY: PASS (10 exact files; originals untouched; no deployment claim)"
