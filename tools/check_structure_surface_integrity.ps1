param(
  [string]$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
)

$ErrorActionPreference = "Stop"
$failures = New-Object System.Collections.Generic.List[string]

function ReadText([string]$Path) {
  if (!(Test-Path $Path)) {
    $failures.Add("missing file: $Path")
    return ""
  }
  return Get-Content -Raw -Path $Path
}

function RequireText([string]$Label, [string]$Text, [string]$Needle) {
  if ($Text.IndexOf($Needle, [System.StringComparison]::Ordinal) -lt 0) {
    $failures.Add("$Label missing required text: $Needle")
  }
}

function ForbidText([string]$Label, [string]$Text, [string]$Needle) {
  if ($Text.IndexOf($Needle, [System.StringComparison]::Ordinal) -ge 0) {
    $failures.Add("$Label contains forbidden text: $Needle")
  }
}

$templates = ReadText (Join-Path $RepoRoot "plugin\src\main\java\com\observance\watcher\structure\StructureTemplates.java")
$command = ReadText (Join-Path $RepoRoot "plugin\src\main\java\com\observance\watcher\command\ObservanceCommand.java")
$plugin = ReadText (Join-Path $RepoRoot "plugin\src\main\java\com\observance\watcher\ObservancePlugin.java")
$sites = ReadText (Join-Path $RepoRoot "plugin\src\main\resources\sites.yml")

ForbidText "StructureTemplates.java" $templates "Material.BEACON"
ForbidText "StructureTemplates.java" $templates "keptLightBeacon"
ForbidText "ObservanceCommand.java" $command "keptLightBeacon"
ForbidText "ObservanceCommand.java" $command "beaconTint"
ForbidText "ObservanceCommand.java" $command "beam projecting"
ForbidText "sites.yml" $sites "visual_beacon:"
ForbidText "sites.yml" $sites "visual-beacon:"

RequireText "ObservancePlugin.java" $plugin 'sec.set("visual_beacon", null)'
RequireText "ObservancePlugin.java" $plugin 'sec.set("visual-beacon", null)'
RequireText "ObservanceCommand.java" $command "removeRetiredBeaconNear(loc)"
RequireText "ObservanceCommand.java" $command "retired beacon block still present near site"
RequireText "ObservanceCommand.java" $command "if (beacon.getType() != Material.BEACON) continue"

RequireText "StructureTemplates.java" $templates 'pen.putBook(cx, cy + 1, cz, "the rosetta"'
RequireText "StructureTemplates.java" $templates "this page back to them"
RequireText "StructureTemplates.java" $templates 'pen.runeCribPair(px + out.getModX(), cy + 1, pz + out.getModZ(), out'

RequireText "StructureTemplates.java" $templates "pen.topSlab(cx, cy + 1, cz, Material.POLISHED_DEEPSLATE_SLAB)"
RequireText "StructureTemplates.java" $templates "void topSlab(int x, int y, int z, Material mat)"
RequireText "StructureTemplates.java" $templates "slab.setType(org.bukkit.block.data.type.Slab.Type.TOP)"
RequireText "StructureTemplates.java" $templates "Wall banners need a solid backing behind the banner block"
RequireText "StructureTemplates.java" $templates "pen.set(cx - 2, cy + 1, z, Material.DEEPSLATE_BRICKS)"
RequireText "StructureTemplates.java" $templates "pen.set(cx + 2, cy + 1, z, Material.DEEPSLATE_BRICKS)"
RequireText "StructureTemplates.java" $templates "pen.wallBanner(cx - 1, cy + 1, cz - 1, BlockFace.EAST"
RequireText "StructureTemplates.java" $templates "pen.wallBanner(cx + 1, cy + 1, cz - 1, BlockFace.WEST"

RequireText "StructureTemplates.java" $templates "try { sign.setWaxed(false); }"
RequireText "StructureTemplates.java" $templates "try { sign.setWaxed(true); }"
RequireText "StructureTemplates.java" $templates "TextFit.paginate(body)"
RequireText "StructureTemplates.java" $templates "prepareTemplateVolume(pen, base, id)"
RequireText "StructureTemplates.java" $templates "void clearBox(int cx, int y, int cz, int radius, int height)"
RequireText "StructureTemplates.java" $templates "seedLoreStorage(base, id)"
RequireText "StructureTemplates.java" $templates "if (b.getType() == Material.CHISELED_BOOKSHELF) continue"
RequireText "StructureTemplates.java" $templates "void chiseledShelf(int x, int y, int z)"
RequireText "StructureTemplates.java" $templates "shelf.setSlotOccupied(slot, occupied)"
RequireText "StructureTemplates.java" $templates 'pen.putBook(cx + 2, cy, cz - 1, "the missing volume"'
RequireText "StructureTemplates.java" $templates "South wall shelf inventory"
RequireText "ObservanceCommand.java" $command 'seedFixtureLore(loc, "far_water")'
RequireText "ObservanceCommand.java" $command "private void seedFixtureLore(Location base, String id)"
RequireText "ObservanceCommand.java" $command "int[][] targets = fixtureLoreTargets(key);"
RequireText "ObservanceCommand.java" $command "private int[][] fixtureLoreTargets(String id)"
RequireText "ObservanceCommand.java" $command "private void placeDecorativeBookshelf(Block block, int seed)"
RequireText "ObservanceCommand.java" $command "placeDecorativeBookshelf(world.getBlockAt(bx + 4, by, bz + 5), 31)"
RequireText "ObservanceCommand.java" $command "fixtureLoreFragments"
RequireText "ObservanceCommand.java" $command "WARDEN-3 closure"

if ($failures.Count -gt 0) {
  Write-Host "structure surface integrity check: FAILED"
  foreach ($failure in $failures) {
    Write-Host "  - $failure"
  }
  exit 1
}

Write-Host "structure surface integrity check: OK - no active beacons, Rosetta/Orin surfaces are mechanically guarded, and sign/book helpers remain safe"
