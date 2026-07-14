# The Observance V5 — Paper 1.21.11 Minecraft runbook

This is the operator procedure for the V5 plugin. The verified coordinate classes retain their
`V4` Java names only for code-history stability; every packaged authority and live command is V5.

## 1. Prerequisites

- Paper 1.21.11 and Java 21.
- A stopped-server backup of the world and `plugins/Observance/`.
- Exactly one plugin jar: `observance-0.5.0.jar`.
- `OBSERVANCE_SUPABASE_KEY` configured in the server environment.
- No players online during build or repair.
- The village well and its Unlit entrance preserved outside the Hold envelope.

Build with `gradlew.bat clean build`. The build rejects stale/multiple Observance jars.

## 2. Plan, build, and resume safely

The Hold has one orientation: world +Z. Player yaw is ignored.

```text
/obs placehold prepare
/obs placehold plan
/obs placehold build
```

Console coordinates are also supported:

```text
/obs placehold prepare <world> <x> <y> <z>
/obs placehold plan <world> <x> <y> <z>
/obs placehold build <world> <x> <y> <z> +z
```

`prepare` asynchronously generates/loads one footprint chunk at a time and tickets the complete
envelope. It changes no Hold blocks or site registrations. Tickets expire after 30 minutes if no
build starts. `plan` never loads or generates terrain: it is read-only and must print `PLAN PASS`.
Confirm its complete X/Y/Z envelope misses the well, portals, player builds, NPC anchors, and every
other protected site.

`build` first creates a deterministic operation plan, then applies no more than 6,000 writes or 12 ms
of work per server tick. It atomically checkpoints its cursor every second in
`plugins/Observance/hold-build-state.properties`. After a crash, run `prepare`, `plan`, and `build`
again at the same Mouth with the same authority hash; acknowledged writes resume and any newer writes
replay idempotently. A checkpoint from another Mouth/hash is refused. A completed receipt refuses
another fresh build; use repair instead. Preparation tickets are released after success or failure.

No site registration is published unless all 76 fixtures, 38 physical book mounts, seven Record
stations, eight gates, signs, frames, standing cells, and the reversible route pass the final readback.

## 3. Verify after build and restart

```text
/obs placehold prepare
/obs placehold audit
/obs placehold sync
/obs dialogueaudit
/obs preflight
```

After a restart, prepare the exact Mouth again before repeating the four verification commands; this
keeps every audit read asynchronous and ticketed instead of forcing unloaded chunks onto the server
thread. `preflight` must be completely clean; a warning about an
unimplemented V5 predicate handler is a launch blocker, not an allowable exception.

Use a non-op Adventure-mode account to traverse from the Mouth to every currently open district and
back through the same Mouth. Verify:

- every declared standing cell has solid floor, two-block headroom, and a clear sightline;
- signs face their approach, have backing, and do not face into blocks;
- exact authority books have correct title, author, pages, PDC id, facing, and unlock timing;
- all six Orin frames contain unique PDC-tagged compasses in the documented initial wrong state;
- chiseled shelves and affidavit barrels accept only the interactions required by their predicate;
- wrong, partial, duplicate, simultaneous, disconnect, and replay inputs never destroy or duplicate items;
- sealed gates have no side/over/teleport/fluid/piston bypass, and opened latches survive restart;
- ordinary players cannot break/place/bucket/burn/explode or remove lectern books in the Hold.

Record every proof in `design/V5-LIVE-TEST-MATRIX.csv`.

## 4. State-preserving repair

```text
/obs placehold prepare
/obs placehold repair <fixture-id>
/obs placehold repair all
/obs placehold audit
```

Run `prepare` at the exact persisted Mouth (or pass its console coordinates) before repair. Repair does
not re-excavate the shell. It preserves tile inventories, signs, shelves, frames, and gate
latches, and never invents a consumed artifact. If its final registration write fails, the pending
site batch is discarded and no readiness receipt is published.

## 5. Exact artifact recovery

```text
/obs item recover <artifact> [online-player]
```

Only the 21 IDs in `ARG-V5-ARTIFACT-MANIFEST.csv` are valid, including:

```text
/obs item recover orientation_key PlayerName
/obs item recover affidavit_iss PlayerName
/obs item recover protocol_bridge PlayerName
/obs item recover averyn_fragment_n PlayerName
```

Recovery first verifies the artifact's durable completion flag. It then scans online inventories,
ender chests, loaded containers, and loaded dropped items; an existing exact copy prevents issuance.
It never displaces or drops an item.

## 6. Unlit and NPC setup checks

Keep the existing well entrance. Use `/obs unlit site`, `/obs unlit clue`, `/obs unlit border`, and
`/obs unlit darken` only while `/obs unlit buildmode on`; turn build mode off before testing.

```text
/obs unlit audit
/obs unlit ready
/obs townsfolk spawn <id>
/obs wren spawn
/obs dialogueaudit
```

NPC spawn commands persist their exact anchors. Rejoin after restart and verify every line changes only
on its V5 evidence flags.

### Player Discord handoff

Confirm `server.properties` has `online-mode=true` and restart Paper after any correction. Production
preflight must reject offline mode. As a normal non-op player, join once so the authenticated
`players` row exists, then run:

```text
/obslink
```

The reply must show one private 4-4-4 code and state that it expires in five minutes. In Discord use
`/link <exact Minecraft name> <callback> <code>`. Verify a wrong, expired, reused, or other player's
code changes no identity row; an exact retry is idempotent. If the same Discord account deliberately
recovers to another Minecraft identity, Paper must add the new hand and revoke the old hand on an
authoritative five-second refresh without requiring a restart. Supabase outages preserve the last
known-good binding and never issue an unrecorded code. On a disposable offline-mode rehearsal clone,
verify `/obslink` says no code was issued and makes no challenge RPC or database row.

## 7. Finale rehearsal and production ending

Rehearse only on restored clones. Verify controls and infrastructure first:

```text
/obs finale markers
/obs finale status
/obs placehold audit
/obs preflight
```

The operator cannot choose an ending. After the exact C10 prerequisites exist, arm the players'
already-persisted Wren outcome, name treatment, and conduct verdict:

```text
/obs finale arm
/obs finale arm 120
/obs finale cancel
```

The default window is 120 seconds; valid range is 15–600. ARMED never auto-commits. One player must
sneak in the marked +Z confirmation cell for 60 ticks and operate the PDC-tagged `SEVER RECORD`
control. The schema-2 COMMITTED record is fsynced and atomically replaced before any title, sound,
light change, kick, or shutdown.

The theater composes the exact name-treatment clause, one of three Wren clauses, the independently
derived solo/unanimous/divided/persistent conduct clause, and Averyn's universal goodbye. It then
durably enters CODA, opens all eight gates, mounts exactly one branch Coda receipt, saves players and
worlds, kicks everyone with the full goodbye, and requests a Paper shutdown. A CODA persistence or
world-projection failure retries every five seconds and cannot publish C10 or shut down early.

After restart, `/obs finale status` must report `phase=coda`; ordinary pollers and story systems stay
asleep, and joins receive the same immutable Coda. Restore a pre-finale clone to repeat a rehearsal.

## 8. Launch rule

Do not admit players until the post-restart commands, non-op traversal, all required live-test rows,
website/Discord checks, Unlit well route, NPC dialogue, artifact recovery, and finale clone rehearsals
all pass against the same production configuration. `NOT READY` is literal.
