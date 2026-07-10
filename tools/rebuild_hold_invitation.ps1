param(
  [string]$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path,
  [string]$InputZip = "",
  [string]$OutZip = "",
  [switch]$NoBackup
)

$ErrorActionPreference = "Stop"

$repoFull = [System.IO.Path]::GetFullPath($RepoRoot)
if ([string]::IsNullOrWhiteSpace($InputZip)) {
  $InputZip = Join-Path $repoFull "dashboard\public\the-hold\the-hold.zip"
}
if ([string]::IsNullOrWhiteSpace($OutZip)) {
  $OutZip = $InputZip
}

$inputFull = [System.IO.Path]::GetFullPath($InputZip)
$outFull = [System.IO.Path]::GetFullPath($OutZip)
if (-not (Test-Path -LiteralPath $inputFull)) {
  throw "hold rebuild: input zip not found: $inputFull"
}

$workRoot = Join-Path ([System.IO.Path]::GetTempPath()) ("observance-rebuild-hold-" + [System.Guid]::NewGuid().ToString("N"))
New-Item -ItemType Directory -Path $workRoot | Out-Null
try {
  Expand-Archive -LiteralPath $inputFull -DestinationPath $workRoot -Force

  $worldRoot = Join-Path $workRoot "the-hold"
  if (-not (Test-Path -LiteralPath $worldRoot)) {
    throw "hold rebuild: expected extracted world folder 'the-hold'"
  }

  $functionRoot = Join-Path $worldRoot "datapacks\the_hold\data\the_hold\function"
  $handoffBookFile = Join-Path $functionRoot "generated\line_0515.mcfunction"
  $paperFile = Join-Path $functionRoot "generated\line_0517.mcfunction"
  $compassFile = Join-Path $functionRoot "generated\line_0518.mcfunction"
  $finalTriggerFile = Join-Path $functionRoot "triggers\final.mcfunction"

  foreach ($file in @($handoffBookFile, $paperFile, $compassFile, $finalTriggerFile)) {
    if (-not (Test-Path -LiteralPath $file)) {
      throw "hold rebuild: expected function file missing: $file"
    }
  }

  $handoffBook = 'data merge block 0 241 334 {Book:{id:"minecraft:written_book",count:1,components:{"minecraft:written_book_content":{title:"handoff",author:"m.kept",pages:[''{"text":"this copy does not connect to anything."}'',''{"text":"the rest is kept elsewhere. do not read this as a place yet."}'',''{"text":"three pieces were kept apart.\n\nrecord / the-record-keeps\n\ngate name: SNOIKERZ\nending: common web"}'',''{"text":"small gate number:\n25500 + (six marked x 11) + the third room\n\nbring the hands. say kept."}'']}}},Page:0}'
  $paper = 'item replace block 0 241 346 container.0 with minecraft:paper[minecraft:item_name=''"record / the-record-keeps"''] 1'
  $compass = 'item replace block 0 241 346 container.1 with minecraft:compass[minecraft:item_name=''"gate name + common ending"''] 1'
  $finalTrigger = @(
    "scoreboard players set @s hold_stage 5",
    "title @s times 20 60 40",
    'title @s subtitle {"text":"the rest is kept in pieces","color":"gray"}',
    'title @s title {"text":"","color":"gray"}',
    "playsound minecraft:block.respawn_anchor.deplete master @s 0 241 334 0.25 0.55"
  )

  [System.IO.File]::WriteAllText($handoffBookFile, $handoffBook + "`n", [System.Text.UTF8Encoding]::new($false))
  [System.IO.File]::WriteAllText($paperFile, $paper + "`n", [System.Text.UTF8Encoding]::new($false))
  [System.IO.File]::WriteAllText($compassFile, $compass + "`n", [System.Text.UTF8Encoding]::new($false))
  [System.IO.File]::WriteAllLines($finalTriggerFile, $finalTrigger, [System.Text.UTF8Encoding]::new($false))

  $allText = Get-ChildItem -LiteralPath $worldRoot -Recurse -File |
    ForEach-Object {
      try {
        [System.Text.Encoding]::UTF8.GetString([System.IO.File]::ReadAllBytes($_.FullName))
      } catch {
        ""
      }
    } | Out-String

  if ($allText -match 'snoikerz\.com\s*:\s*25569') {
    throw "hold rebuild: raw endpoint still present after rewrite"
  }

  $stagingZip = Join-Path $workRoot "the-hold.zip"
  if (Test-Path -LiteralPath $stagingZip) {
    Remove-Item -LiteralPath $stagingZip -Force
  }
  Compress-Archive -LiteralPath $worldRoot -DestinationPath $stagingZip -Force

  if ((-not $NoBackup) -and (Test-Path -LiteralPath $outFull)) {
    $backup = $outFull + ".bak-" + (Get-Date -Format "yyyyMMddHHmmss")
    Copy-Item -LiteralPath $outFull -Destination $backup -Force
    Write-Host "hold rebuild: backup written to $backup"
  }

  Copy-Item -LiteralPath $stagingZip -Destination $outFull -Force
  $item = Get-Item -LiteralPath $outFull
  $sha1 = (Get-FileHash -LiteralPath $outFull -Algorithm SHA1).Hash.ToLowerInvariant()
  Write-Host "hold rebuild: wrote $outFull"
  Write-Host "hold rebuild: size $($item.Length)"
  Write-Host "hold rebuild: sha1 $sha1"
  Write-Host "hold rebuild: destination grammar = route + gate name + common ending + port arithmetic"
} finally {
  if (Test-Path -LiteralPath $workRoot) {
    Remove-Item -LiteralPath $workRoot -Recurse -Force -ErrorAction SilentlyContinue
  }
}

