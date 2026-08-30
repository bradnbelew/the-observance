# Morrow graphical-client rehearsal

This is the fail-closed procedure for the six client-only P0 lanes. It never enables production.
The operator runs the slice; a second pseudonymous observer reviews the retained evidence. A written
claim, screenshot, or server log by itself cannot pass the validator.

## Create a run

From repository root, while the exact disposable Paper target is stopped:

```text
python tools/new_morrow_client_rehearsal.py --run-id <lowercase-id> --operator-id <operator> --observer-id <different-observer>
```

The command creates `build/morrow-client-rehearsal/<run-id>` once and refuses reuse. It binds the
packet to the current Git commit, retained Paper/runtime receipt, Paper JAR, plugin JAR, release,
campaign, Room 04 manifest, and `127.0.0.1:25589`. Do not edit those bindings.

## Run order

Repeat the complete 60–90 minute slice for fresh one-, two-, and six-player cohorts:

1. Start the exact disposable Paper target and retain the complete console log.
2. Join using Minecraft Java 1.21.11. No player may be operator and the human operator may not
   narrate an affordance, answer, or recovery step.
3. Capture continuous video around every interaction plus readable stills at each required state.
   Preserve original files; do not transcode or overwrite them after the run.
4. Verify Morrow's six authored poses and recorded/reconstructed/live provenance are distinguishable.
   Include the exact 37-second replay and deliberate-movement echo.
5. Exercise each native dialog with mouse, keyboard, and Escape. Closing, declining, and reviewing
   evidence must not fabricate a progress receipt.
6. Run once with the resource pack accepted and once declined. The vanilla display/interaction body,
   readable text, and puzzle inputs must remain equivalent.
7. Disable Minecraft music and sound. Every required cue must retain its captioned/pulsed visual
   equivalent; no solution may depend on hearing.
8. Fill every player's inventory before entering Room 04. Prove safe spawn/exit, no item grant or
   removal, disconnect/rejoin recovery, and exact before/after inventory hashes.
9. Complete the slice without operator intervention. Record elapsed time per cohort; valid pacing is
   3,600–5,400 seconds.
10. Stop Paper gracefully. Export the journal, exact Room 04 world read-back, inventory receipts, and
    entity/task cleanup receipt before reviewing the media.

## Evidence format

Fill `client-rehearsal.json` only with relative paths inside its create-only run directory. Every file
row contains `file`, `bytes`, and `sha256`. Each of the six lanes needs at least two synchronized
files and at least one visual-media file. Each cohort additionally needs:

- complete server lifecycle log containing `MORROW_RUNTIME_READY` and `MORROW_RUNTIME_CLOSED`
- native six-field TSV Morrow journal containing all required Room 04, Static Restore, and Entity
  Replay events; the validator recomputes its sequence and SHA-256 hash chain
- JSON world read-back with the canonical Room 04 hash, safe route, and zero occupied-cell conflicts
- before/after inventory JSON with identical per-player aggregate hashes
- JSON cleanup receipt with zero leaked entities and tasks
- client media covering the cohort, disconnect/rejoin, failure recovery, and final state

The observer sets a lane to `pass` only after reviewing its media alongside the matching machine
receipts. The operator and observer identifiers must differ.

Validate with:

```text
python tools/check_morrow_client_rehearsal.py build/morrow-client-rehearsal/<run-id>
```

The command rejects incomplete lanes, stale files, changed hashes, non-loopback targets, mismatched
artifacts, screenshot-only proof, missing progression, inventory changes, leaked runtime ownership,
operator narration, or incomplete 1/2/6-player coverage.

The validator's dependency-free positive fixture and evidence-reuse refusal test run with:

```text
python tools/test_morrow_client_rehearsal.py
```
