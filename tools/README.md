# V5 production toolchain

Status: current tooling authority.

`check_phase1_architecture.py` is the non-live Phase 1 rebuild-architecture gate. It validates the
approved spine prerequisite, twelve-arc/26.5-hour experience map, subset/catch-up and hint/automation
contracts, preservation of every currently manifested media receipt, single-Crafty-runtime topology,
M0–M5 migration sequence, and future-agent authority routing. It performs no network or live mutation.

`build_m2_contracts.py` deterministically derives the approved P1-P12 logical manifests and all 82
legacy import contracts. `check_m2_contracts.py` verifies their hashes, the exact historical predicate
byte reconstruction, schema/security/rollback proposals, cross-surface parity, and approval boundaries.
Both are non-live; neither applies a database migration or claims a production receipt.

`validate_m2_supabase.ps1` is the fail-closed disposable database harness for the reviewed M2 SQL. It
accepts only a local target, rejects every hosted project ref (including production
`fdnmhbpxnodrnbrzrlqq`), discovers the installed CLI surface through `--help`, creates every executable
migration with `supabase migration new`, and copies the reviewed up/rollback/forward bytes only into a
temporary local project. It runs reset, pgTAP database tests, lifecycle assertions, repeated-forward
idempotency, and both security/performance advisors, then writes exact local CLI/migration receipts.
Docker and an installed official Supabase CLI are prerequisites. `-SelfTest` exercises only its guards
and committed inputs and never starts or contacts a database.

`check_continuation_lineage.py` validates the canonical M3 continuation ledger: both completed sibling
evidence commits and their linear incorporation commits, preserved unresolved gaps and supersession,
the failed v1/v2 Brad visual decisions, the clean v2 stop, the current v3 Paper/restart receipts, and the
still-closed M4 gate. `check_m3_v3_revision.py` is the focused v3 authority/implementation/receipt gate;
it runs alongside the preserved v2 checker so historical evidence is verified rather than rewritten.
The matching `run_m3_v3_disposable_paper.py` imports the preserved v2 process harness but owns v3
authority, journal, package, and receipt identities; `run_m3_disposable_paper.py` remains byte-identical
to its v2 package provenance.

The production entry point is `tools/audit_all.ps1`. It is fail-closed: source authorities are
validated before generation, every project is built, release artifacts are rebuilt and read back,
the hosted resource-pack bytes and external media are checked, and release-tool self-tests run. A passing tool run is still not a substitute for the real Paper,
client, world, credential, and service receipts in `design/V5-LIVE-TEST-MATRIX.csv`.

## Production entry points

| Tool | Purpose |
| --- | --- |
| `audit_all.ps1` | strict V5 source/build/package/media aggregate |
| `check_repository_integrity.py` | read every project-owned file and reject damaged/unsafe content |
| `check_v5_freshness.py` | enforce current/archive separation and disable pre-V5 generators |
| `check_v5_content.py --runtime` | validate the 82-node canon and scan live surfaces for retired claims |
| `check_v5_physical_predicates.py` | validate exact executable Minecraft success predicates |
| `render_v5_map_art.py` | verify the nine hashed 128×128 map clues and canonical solved-view sheet (`--write` regenerates them) |
| `check_deep_hold_layout.py` | validate spatial manifests, generated plan, protection, and runtime wiring |
| `simulate_v5_scenarios.py` | deterministic model-level failure/replay/recovery/finale scenarios |
| `package_plugin.ps1` / `check_plugin_jar.ps1` | Gradle-only reproducible V5 JAR build and exact authority readback |
| `package_assets.ps1` / `check_assets.ps1` | deterministic 1.21.11 datapack/resource-pack packaging and readback |
| `check_external_media_readiness.ps1 -Live` | verify all five immutable public media sources |
| `set_resource_pack_config.ps1` | atomically set the hosted pack URL and matching SHA-1 |
| `package_launch_bundle.ps1` | regenerate SQL, then package and verify the V5 deploy artifacts |
| `write_deploy_manifest.ps1` / `check_deploy_manifest.ps1` | bind plugin/datapack/resource-pack/SQL/Hold-archive bytes and service inputs into one receipt |

Run a production candidate from PowerShell:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File tools\audit_all.ps1
```

`-SkipLiveExternalMedia` and `-SkipLiveHostedResourcePack` exist only for offline development. The
script prints that a run using either switch is not a launch receipt.

## Retired tools

The following filenames remain as fail-fast tombstones so an old note or shell history cannot
silently generate a believable but wrong launch packet:

- `apply_launch_coords.ps1`
- `build_storymap.py`
- `build_viz.py`
- `new_director_packet.ps1`
- `new_launch_placement_packet.ps1`
- `new_rehearsal_packet.ps1`
- `prepare_friend_launch.ps1`
- `prepare_server_test.ps1`
- `rebuild_hold_invitation.ps1`

Do not remove their guards without replacing the workflow with a V5 manifest-driven equivalent and
adding it to `check_v5_freshness.py`. Old `check_*` scripts not listed as production entry points are
historical diagnostics; `audit_all.ps1` intentionally does not call them.

## Version invariant

Production is Paper/Minecraft `1.21.11`, Java 21, and Observance plugin `0.5.0`. Older JAR names may
appear only inside unmistakably labeled historical archives. `gradlew clean check build --no-daemon`
is the sole production compiler/packager; reproducible archive settings plus the JAR readback check
ensure exactly one deployable `observance-0.5.0.jar` survives in `plugin/build/libs` with every exact
V5 authority and no unowned authority entry.
