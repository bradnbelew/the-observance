# The Observance V5 datapack

Target: Minecraft Java / Paper **1.21.11**. The pack is deliberately small and non-narrative.

V5 builds the Deep Hold through the Paper plugin and keeps the Unlit as a protected copy of the real
village reached only through its well. The retired custom Undercroft dimension and V4 reward toasts
have been removed; installing this pack cannot create a contradictory second deep world or expose old
progression.

On load, `observance:v5/load` writes only a deployment marker:

```text
storage observance:runtime version = 5
scoreboard runtime obs_v5 = 5
```

Verify after restart:

```text
/datapack list enabled
/data get storage observance:runtime version
/scoreboard players get runtime obs_v5
```

Expected version is `5`. No answer, gate, book, NPC, artifact, or finale state is stored here; those
remain authoritative in the plugin and Supabase. This keeps datapack reloads unable to reset or advance
the ARG.

Package it only with `tools/package_assets.ps1`; install the resulting `observance-datapack.zip` at the
world datapack root and run `tools/check_assets.ps1` against the exact release bytes. Any source tree
containing an Observance custom dimension, worldgen, or advancement directory is pre-V5 and must be
rejected rather than merged into this pack.
