# V5 production toolchain

Status: current tooling authority.

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
