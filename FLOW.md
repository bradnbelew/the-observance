# The Observance V5 — flow and progression

This document is the concise operator-facing flow. Exact nodes live in `design/ARG-V5-NODE-MANIFEST.csv`; spoilers live in `arc/WORLD-BIBLE.md`.

## End-to-end route

```text
C01 Lost Server
  Copperline traces -> rebuilt archive -> Discord bind -> Surface Mouth key

C02 Long Cold
  construction -> rations -> ordinary life -> safety script -> founding motive -> Survey Seal
  -> G1

C03 Keeper Dossiers
  Vaun / Mara / Sella / Orin / Brann / Iss (three required nodes each)
  -> six affidavits -> G2

C04 Cistern Winter
  Nessa complaints -> samples -> valves -> rota -> counterfeit invoice
  -> fixed reeds footage -> cache -> clear Nessa -> Cistern Seal

C05 Break Inquest / Unlit
  leave Hold by Mouth -> village well -> all eight copied houses
  -> multi-cause synthesis -> Breach Plate -> return by well and Mouth -> G3

C06 Restoring the Hold
  filter -> lamps -> pressure shelf -> survey dials -> painted line
  -> System Key -> synchronization -> Deep Access Plate -> G4

C07 ASH-13 Company
  locate Camp Ash -> four stations -> barrel order -> notebook -> overlay
  -> Copperline archive -> fixed footage -> ASH-13 -> Locker 13 -> Witness Spool

C08 Wren
  quotations -> transmissions -> confrontation -> Rook route -> Bridge
  -> condemn / understand / free -> G5

C09 Averyn
  spectrogram -> six affidavit extractions -> AVERYN -> Record truth -> G6

C10 Release
  fixed release footage -> six affidavits returned / Averyn slot empty
  -> install Seal + Key + Bridge -> name treatment -> group operation
  -> durable sever -> lights -> goodbye -> kick -> shutdown -> Coda
```

## Gate rules

Major gates are linear; investigation within an unlocked district may be parallel. Open is monotonic. Database false/missing state cannot reseal a locally latched gate. Reset is an explicit destructive operator action, never normal synchronization.

| Gate | Condition |
| --- | --- |
| G1 | C02 complete |
| G2 | all six affidavits earned |
| G3 | C04 and C05 complete |
| G4 | C06 complete |
| Camp subgate | A01 camp location proved |
| Dread relay gate | C07 complete |
| G5 | C08 Wren reckoning complete |
| G6 | C09 AVERYN synthesis complete |

## Session rhythm

The campaign is evidence-paced, not day-timer-paced. Players can stop after any durable node or district and walk back through the Mouth. A resumed group enters by the same route and sees opened gates.

Recommended sessions:

1. C01–C02: discovery and first gate.
2. C03: parallel Keeper dossiers.
3. C04: Cistern Winter.
4. C05: full Unlit expedition.
5. C06: systems restoration.
6. C07: Camp Ash / Copperline / footage loop.
7. C08–C09: Wren and Averyn.
8. C10: release night.

This is pacing guidance, not an artificial lock. If the group moves faster, the story remains coherent.

## Answer and hint flow

- Minecraft physical mechanisms resolve locally and write durable receipts asynchronously.
- Discord handles cross-source conclusions and media payloads.
- Editable signs are used only as plausible filing/maintenance inputs.
- `/whisper` returns authored tier 1/2/3 hints for the selected open node.
- Selecting a puzzle constrains answer resolution; it is never ignored.
- Wrong physical deposits are preserved/returned.
- Critical artifacts are recoverable from durable state.

## Finale flow

The finale is explicitly armed. State is durable before theater. Website/Discord mirroring is asynchronous. Local Minecraft proceeds even if an external service is unavailable. Test mode kicks only; production mode saves and cleanly stops Paper after the cancel window. Restart enters branch-specific Coda Mode.
