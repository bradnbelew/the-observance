# V5 canonical content sources

This directory contains player-facing and machine-readable V5 source material. It is governed by:

1. `arc/WORLD-BIBLE.md` for spoiler truth;
2. `design/ARG-V5-NODE-MANIFEST.csv` for the exact 82-node graph;
3. `design/EXPERIENCE-MANIFEST.md` for player order and surface ownership.

Runtime copies in SQL, Java resources, the dashboard, Discord, build artifacts, or generated archives must be derived from or parity-checked against these sources. Old lore documents outside this directory are not allowed to override V5.

`media-manifest.json` records the five fixed external artifacts, their immutable payloads, production receipts, reveal prerequisites, and new narrative jobs. All five are required.

`generated-image-manifest.json` records the two restrained Copperline frame stills used by A07/A08. Both are required, served only from the earned archive page, and checked byte-for-byte against their production PNGs.

`minecraft-books.json` is the exact 44-book lectern/artifact corpus. `evidence-item-text.json` is
the separate exact 7-book authority for movable WR01 quotation cards, BI01 wick layers, and the CW07
Drafts A/B exhibit. The installer must never replace either
corpus with generated filler text.

`evidence-item-appearance.json` is the exact 108-item title/lore authority for every remaining
player-facing tagged evidence item, plus the A02 station and CW02/KV02 filing labels. Internal PDC
IDs and solution classes are implementation details and must never be shown as substitute copy.

`map-art-manifest.json` and `map-art/*.png` are the exact nine 128×128 map clues for A05, KS02,
BI04, and AR04. Their hashes, component bindings, and solved ItemFrame rotations are verified in the
release audit; a default or blank Minecraft map is never an acceptable fallback.
