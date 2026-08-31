# Morrow graphical-client rehearsal

This is the fail-closed procedure for all twelve Morrow investigations plus the six cross-cutting
client lanes. It never enables production. The operator runs the complete M01–M12 journey; a second
pseudonymous observer reviews the retained evidence. A written claim, screenshot, or server log by
itself cannot pass the validator.

## Create a run

From repository root, while the exact disposable Paper target is stopped:

```text
python tools/new_morrow_client_rehearsal.py --run-id <lowercase-id> --operator-id <operator> --observer-id <different-observer>
```

The command creates `build/morrow-client-rehearsal/<run-id>` once and refuses reuse. It binds the
packet to the current Git commit, current retained Paper/runtime receipt, exact Paper JAR, matching
plugin JAR, release, campaign, Room 04 manifest, canonical M01–M12 rehearsal contract, and the
receipt's loopback address. It refuses packet creation when the built plugin no longer matches the
retained Paper run. Do not edit those bindings.

If the Microsoft launcher UI is unavailable to automation, the repository can prepare or start the
already-installed vanilla 1.21.11 client without reading account files:

```text
python tools/run_morrow_offline_client.py --run-id <lowercase-id> --server <packet-server> --prepare-only
python tools/run_morrow_offline_client.py --run-id <different-lowercase-id> --server <packet-server>
python tools/run_morrow_offline_client.py --run-id <bounded-id> --server <packet-server> --wait-seconds 120 --terminate-after-wait
python tools/run_morrow_offline_client.py --run-id <silent-id> --server <packet-server> --audio-disabled
```

This helper requires every local library and asset index to match the vanilla manifest, extracts only
the matching Windows natives into a fresh directory, uses a deterministic dummy offline identity,
routes HTTP(S) through a closed loopback proxy, and quick-connects only to the packet's loopback
server address.
The bounded form retains a finalized receipt and client-log hash after stopping its own disposable
client process; it does not shut down Paper.
The `--audio-disabled` profile writes every vanilla sound category to zero before launch and binds the
exact options hash into the receipt; it supports the audio-accessibility lane but does not replace the
human visual-equivalence review.

For the optional resource-pack lane, run the create-only loopback harness once per decision. It serves
the exact ZIP from loopback, writes a fresh saved-server preference into an isolated game directory,
waits for Paper's real `LOADED` or `DECLINED` event, stops its owned client, and retains request/log
hashes. The visible-parity and independent-observer requirements remain open until matching media is
reviewed:

```text
python tools/run_morrow_resource_pack_rehearsal.py <pinned inputs> --expected-status loaded --launch-client --launch-acknowledgement launch-visible-minecraft-resource-pack:loaded
python tools/run_morrow_resource_pack_rehearsal.py <pinned inputs> --expected-status declined --launch-client --launch-acknowledgement launch-visible-minecraft-resource-pack:declined
python tools/run_morrow_resource_pack_rehearsal.py <pinned inputs> --expected-status loaded --audio-disabled --launch-client --launch-acknowledgement launch-visible-minecraft-resource-pack:loaded
python tools/test_morrow_resource_pack_rehearsal.py
python tools/check_morrow_resource_pack_receipts.py
```

The client fixture uses the exact uncompressed NBT format that Minecraft 1.21.11 reads from
`servers.dat`: omitted `acceptTextures` means prompt, byte `1` means enabled, and byte `0` means
disabled. This controls only the fresh disposable server entry; it never reads or edits the player's
normal server list.

Prepare distinct vanilla identities and isolated game directories for a complete cohort before the
server starts:

```text
python tools/prepare_morrow_offline_cohort.py --run-id <cohort-id> --server <packet-server> --cohort-size 1
python tools/prepare_morrow_offline_cohort.py --run-id <cohort-id> --server <packet-server> --cohort-size 2 --audio-disabled
python tools/prepare_morrow_offline_cohort.py --run-id <cohort-id> --server <packet-server> --cohort-size 6 --max-memory-mib 1024
```

Preparation starts no GUI. An actual local launch additionally requires `--launch` and the exact
`--launch-acknowledgement` value printed by the helper. Each client gets a unique offline UUID,
pseudonym, create-only game directory, bounded heap, loopback target, and hash-bound child receipt.
This removes cohort setup ambiguity but does not turn six synthetic clients into six human players;
the complete operator-free media and independent-observer gate remains mandatory.

On a Windows 10 host where the normal window-capture API fails before returning a frame, take a bounded
still only after selecting exactly one visible Java window and recording its exact title and process ID:

```text
python tools/capture_windows_window.py --title "Minecraft 1.21.11 - Multiplayer (3rd-party Server)" --process-id <pid> --output <frame.png> --receipt <frame.json>
```

The fallback refuses missing, ambiguous, minimized, invalid, or partially off-screen targets and never
injects input. Because it captures visible desktop pixels inside the exact window bounds, keep the target
unobscured and inspect every frame. A fallback still may support a bounded finding, but it never replaces
the continuous synchronized media and independent-observer evidence required for a complete lane.

## Run order

Repeat the complete 60–90 minute M01–M12 journey for fresh one-, two-, and six-player cohorts:

1. Begin synchronized capture, authenticate M01 in Copperline, and retain the matching private
   Discord custody/handoff receipt before using the recovered server handoff.
2. Start the exact disposable Paper target and retain the complete console log.
3. Join using Minecraft Java 1.21.11. No player may be operator and the human operator may not
   narrate an affordance, answer, or recovery step.
4. Capture continuous video around every interaction plus readable stills at each required state.
   Preserve original files; do not transcode or overwrite them after the run.
5. Verify Morrow's six authored poses and recorded/reconstructed/live provenance are distinguishable.
   Include the exact 37-second replay and deliberate-movement echo.
6. Exercise each native dialog with mouse, keyboard, and Escape. Closing, declining, and reviewing
   evidence must not fabricate a progress receipt.
7. Run once with the resource pack accepted and once declined. The vanilla display/interaction body,
   readable text, and puzzle inputs must remain equivalent.
8. Disable Minecraft music and sound. Every required cue must retain its captioned/pulsed visual
   equivalent; no solution may depend on hearing.
9. Fill every player's inventory before entering Room 04. Prove safe spawn/exit, no item grant or
   removal, disconnect/rejoin recovery, and exact before/after inventory hashes.
10. Complete M01 through one supported M12 ending without operator intervention. Record elapsed time
    per cohort; valid pacing is 3,600–5,400 seconds.
11. Stop Paper gracefully. Export the journal, exact world read-backs, inventory receipts, and
    entity/task cleanup receipt before reviewing the media.

## Investigation contract

`client-rehearsal-contract.json` is the machine-readable authority for the run. It covers M01–M12,
all 26 canonical events, each investigation's required surfaces, and the four supported M12 endings.
For every investigation the packet must independently retain:

- native player input, not an operator command or prose claim
- the successful path
- its authored recoverable failure path without accidental progression
- its no-audio/readable accessibility equivalent
- the promised callback or cross-surface payoff
- at least one visual file plus a separate synchronized receipt, reviewed by the independent observer

Evidence for one investigation or lane cannot be silently reused as another proof. Long continuous
recordings may be preserved as originals, but each finding must cite a distinct retained clip or still
and its matching machine receipt.

## Evidence format

Fill `client-rehearsal.json` only with relative paths inside its create-only run directory. Every file
row contains `file`, `bytes`, and `sha256`. Each of the twelve investigations and six lanes needs
at least two synchronized files and at least one visual-media file. Each cohort additionally needs:

- complete server lifecycle log containing `MORROW_RUNTIME_READY` and `MORROW_RUNTIME_CLOSED`
- native six-field TSV Morrow journal containing all 26 canonical Act 0–7 events; the validator
  recomputes its sequence and SHA-256 hash chain
- explicit ordered `completed_investigations: M01…M12` and one canonical M12 ending
- JSON world read-back with the canonical Room 04 hash, safe route, and zero occupied-cell conflicts,
  plus the investigation-specific read-backs referenced by the per-investigation receipts
- before/after inventory JSON with identical per-player aggregate hashes
- JSON cleanup receipt with zero leaked entities and tasks
- client media covering the cohort, disconnect/rejoin, failure recovery, and final state

The observer sets an investigation or lane to `pass` only after reviewing its media alongside the
matching machine receipts. The operator and observer identifiers must differ.

Validate with:

```text
python tools/check_morrow_client_rehearsal.py build/morrow-client-rehearsal/<run-id>
```

The command rejects omitted investigations or surfaces, incomplete lanes, stale files, changed hashes,
non-loopback targets, mismatched artifacts, screenshot-only proof, missing canonical events, missing
failure/accessibility/callback proof, non-canonical endings, inventory changes, leaked runtime
ownership, operator narration, or incomplete 1/2/6-player coverage.

The validator's dependency-free positive fixture and evidence-reuse refusal test run with:

```text
python tools/test_morrow_client_rehearsal.py
```
