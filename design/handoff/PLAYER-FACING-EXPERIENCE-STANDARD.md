# Player-facing experience, copy, artifact, and room-composition standard

Status: **BINDING CROSS-PHASE AUTHORITY**

Brad issued this standard after the M3 v3 review on 2026-07-16. It applies to M3 v4 and every later
player-facing implementation in M4 and M5. It sits immediately after the locked spine and spine
conformance authorities: it changes neither canon nor the approved evidence model, but governs how
that canon and evidence are embodied, written, placed, and understood by players.

The machine-readable companion is `PLAYER-FACING-EXPERIENCE-STANDARD.json`. The routed static gate is
`python tools/check_player_facing_experience_standard.py`.

## The human acceptance question

A room must first read as a believable place where people did a specific job. Its architecture,
furniture, circulation, storage, lighting, work residue, and empty space must support that job. Evidence
must appear in the forms those people would actually have made, received, amended, stored, or left
behind. A deduction may then emerge from the record. A room that chiefly reads as a puzzle chamber,
an array of lecterns, or a container for exposition fails even if every automated interaction works.

## Binding composition rules

1. **Function justifies scale.** Every room declares its ordinary institutional job and why its
   dimensions are needed. Furnishing density, circulation width, staff/public separation, storage,
   sightlines, and purposeful negative space must support that explanation. Large empty volume is not
   grandeur by default.
2. **Furniture forms compositions, not stations.** Desks, counters, cabinet ranks, shelving, lamps,
   records, tools, seating, and service routes are authored in coherent clusters. Repeated fixtures need
   a fictionally credible reason and visible variation. A row of equivalent interaction stations fails.
3. **Lecterns are exceptional.** A lectern is used only when a standing, bound volume is the natural
   artifact and the location gives it a specific job. Records may instead live on desks, counters,
   shelves, notice boards, map tables, cabinets, walls, machinery, floors, or environmental
   compositions. Reducing lectern count is not enough if substitute props remain repetitive stations.
4. **Circulation teaches without labels.** Entrances, public counters, staff thresholds, filing routes,
   controlled gates, and intentionally sealed stubs are legible through form, material, light, facing,
   clearance, and sightline. Meta labels such as “entrance,” “exit,” “puzzle,” “clue,” or “submit here”
   may not compensate for ambiguous architecture.
5. **Minecraft craft is exact.** Block states, attachment, support, collision, standing cells,
   eye-height readability, palette rhythm, massing, silhouettes, and negative space are part of
   authorship. Every decorative cluster has a declared purpose; no unclassified floating or misplaced
   block is acceptable.

## Binding copy and artifact rules

1. **Write people, not ARG voice.** Player-facing prose is realistic, grounded, specific, and human.
   Its diction follows the writer, recipient, workplace, date, medium, and pressure of the moment. Avoid
   purple portent, cryptic-for-cryptic's-sake phrasing, conspicuous mystery language, and text that
   sounds like a puzzle setter addressing a player.
2. **Use the medium truthfully.** A ledger tabulates; a personal letter assumes a relationship; a
   workplace note is abbreviated by shared context; a notice states policy; minutes preserve a meeting;
   marginalia argues with or corrects a source; a transcription records speech and uncertainty; a
   diagram carries spatial or mechanical information; environmental evidence is inferred from physical
   arrangement. Do not pour the same docket paragraph into different containers.
3. **Vary format, voice, and evidentiary work.** The campaign may use conversations between people,
   workplace notes, personal letters, official records, ledgers, marginalia, transcriptions, notices,
   diagrams, physical environmental evidence, and genuine lore or written books. These are examples,
   not a quota. Each selected form must earn its place and do work its alternatives could not do as well.
4. **Let records be partial.** Artifacts should differ in knowledge, motive, register, and reliability.
   Avoid monotonous exposition, repeated docket prose, and a single institutional narrator explaining
   the whole case. Players should compare situated records and physical evidence, not assemble a
   serialized lore dump.
5. **Keep instructions diegetic and parsimonious.** The ordinary workflow may establish the civic
   question and how a finding is filed, but may not reveal the conclusion or become tutorial signage.
   Chat scaffolding, subtitles that explain controls, and immersion-breaking handholding are not
   acceptable substitutes for a legible physical affordance.

## Inventory and static gate

Every new player-facing room and artifact must be represented in a machine-readable implementation
inventory. A room entry records its job, scale rationale, circulation, negative-space purpose,
furniture clusters, lectern purposes, repeated-fixture rationale, evidence formats, and intended
cold-read result. An artifact entry records its physical format, exact placement, human voice and
relationship, register, evidentiary role, provenance, canon references, medium fit, and human editorial
review.

The static gate rejects missing purpose or provenance, unexplained repeated fixtures, duplicate text
disguised as format variety, missing medium-fit review, meta-puzzle copy, and absent human cold-read
receipts. It may report counts and diversity as diagnostic evidence, but **writing quality is never
reduced to a minimum number of formats, voices, or props**. Passing a quota cannot make implausible
writing or a puzzle-chamber room acceptable.

## Human cold-read gates

Before Brad is invited to another M3 review, a cold player who has not received external explanation
must be able to:

- state the room's ordinary civic or clerical job and the disputed civic question;
- distinguish readable records from the physical act of filing a conclusion;
- explain why the evidence artifacts plausibly exist where they are placed;
- recognize materially different people, formats, and points of view rather than repeated docket prose;
- derive and submit one defensible finding from content rather than from touching every surface;
- explain how the four findings combine into the final synthesis;
- navigate the public and staff route without chat prompts or tutorial labels; and
- describe the space as a workplace or civic facility, not as a puzzle room or room of lecterns.

The observer also records whether the prose sounds like plausible human/workplace writing and whether
any interaction required out-of-world explanation. A static declaration cannot replace this human
receipt.

## M3 v4 demonstration obligation

V4 must preserve v3's native book UI, directional/support fixes, state/persistence/security behavior,
and improved controlled gate. It must use a smaller set of purposeful lecterns and multiple naturally
placed evidence formats and voices. Each P4.F1–P4.F4 finding must require a specific defensible
conclusion derived from paired records, and the final synthesis must combine those conclusions. A naive
read-everything/use-everything pass and bounded blind submissions must leave unsupported findings
uncommitted and the gate closed. Any-subset, replay, restart, catch-up, idempotency, accessibility, the
locked spine, and the approved evidence model remain mandatory.

Brad's v3 decision is **NOT APPROVED / REVISION REQUIRED**. Visual approval is null and M4 remains
closed until Brad explicitly approves a later M3 revision in game.
