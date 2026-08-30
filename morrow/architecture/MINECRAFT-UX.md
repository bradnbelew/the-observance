# Minecraft Interaction and Presentation Specification

Target: Minecraft Java and Paper 1.21.11, Java 21, one to six non-op players.

## Recovery Room 04

The room is the repeated social/mechanical hub. It must feel like a real Copperline restoration bay,
not a dialogue lobby or puzzle chamber.

- Entry has an obvious physical route, stable landmark, and readable exit.
- The copper terminal is visible from the threshold but does not force a menu open.
- Morrow begins as sound/text emitted by the terminal.
- A resource-pack blocky body is assembled from display entities around an interaction entity.
- The body, lighting, exit treatment, echo position, and props change only at authored relationship
  revisions.
- Every change has an in-world cause and an audit receipt; no decorative randomization.

## Dialogue

- Right-clicking the terminal/body opens a native Paper dialog.
- Normal exchange: one to three short spoken lines, then zero to four concrete actions.
- Long evidence remains in books, tickets, images, or physical layouts; dialogue never becomes a novel.
- Choices state the action, not its moral label: `Preserve both copies`, not `Choose the good option`.
- Text input is reserved for short IDs, names, coordinates, hashes, or decoded tokens.
- Freeform theory notes may be saved as optional player notes but never graded as the progression key.
- Morrow's response is selected from authored state, prior response receipts, and current evidence.
- Escape/cancel behavior is intentional; leaving a dialog never silently accepts a choice.

## Capability authorization

- Solving the prerequisite investigation only makes a capability available; it does not unlock it.
- The terminal then shows a native confirmation dialog naming the exact capability, bounded use,
  stored data, rollback behavior, and how to stop it.
- Confirmation writes a separate durable `*_authorized` event. Relationship progression requires
  that receipt as well as the evidence events, so dialogue cannot silently infer consent.
- Live capture additionally requires per-player opt-in. A group authorization never enrolls a player
  who declined, disconnected, or joined later.
- Cancelling or declining preserves the current state and offers the authored non-capture review path;
  it never fabricates an authorization receipt.

## Text surfaces

- Signs label places, machinery, warnings, or short instructions; they do not carry multi-step lore.
- Lecterns/books contain short, plain-English records with visible author/date/custody context.
- Item names identify the object; lore supplies provenance, not puzzle instructions.
- Action bars/titles communicate immediate feedback only.
- Chat is not a mandatory input channel and is never continuously harvested.
- All player-facing text must pass the existing fit/readability tooling or a Morrow-specific successor.

## Morrow body

- Use display entities and an interaction hitbox, not a fake online player.
- Body poses are authored: idle, attending, mirroring, retaining, fractured, and negotiated.
- Head tracking follows the active speaker at a restrained update rate.
- Personal visual elements are shown only to the relevant player unless the reveal is group evidence.
- If the resource pack is unavailable, the body degrades to a clearly labeled vanilla copper/display
  form; required interaction remains functional.
- Morrow's pack handshake is optional by contract even if an older campaign config marks its pack
  required. `morrow-reboot.resource-pack.required: true` fails startup, because kicking a player who
  declines would make the promised vanilla fallback unreachable. The shipped Morrow pack gate stays
  disabled until accepted/declined parity is proven.

## Movement replay

- Recording begins only after an explicit in-fiction action and visible cue.
- Default vertical-slice clip: maximum 45 seconds, sampled every two ticks, maximum six tracked players.
- Store location, yaw/pitch, pose, selected slot, swing, jump/crouch, drop, and designated interactions.
- Compress redundant samples and write locally before any derivative is projected.
- Replay uses a translucent resource-pack model with display interpolation.
- Replays cannot place/break real blocks, hold permissions, appear in the tab list, or impersonate a
  connected account.
- A visible provenance effect distinguishes recorded, reconstructed, and live players.

## Restoration and version rooms

- Every mutable scene is a pre-authored bounded region with manifest, block counts, protected volume,
  standing cells, entry/exit routes, and rollback snapshot.
- Static restoration occurs in small visible passes with sound, particles, and audit lighting.
- Critical version differences are physically inspectable and represented in Copperline diffs.
- Split-backup scenes use cloned rooms, never critical client-only block illusions.
- Structure application is idempotent and re-audited after restart.

## Input verbs

Use a varied but teachable vocabulary:

- inspect a physical difference
- arrange or preserve evidence
- select a version/action in a native dialog
- enter a short decoded token
- walk or perform a bounded test pattern
- route signals/items between separated players
- authenticate a witness anchor
- compare Minecraft against Copperline/media
- deliberately refuse a false choice

No act may repeat the exact same input grammar more than twice in a row.

## Accessibility and recovery

- Required audio has subtitles/transcript and a visual timing equivalent.
- Color-coded provenance also uses shape, material, label, or position.
- Timed group scenes provide a reset/replay affordance and catch-up receipt.
- Any one-to-six-player subset can finish required content; asymmetric scenes assign roles dynamically.
- Disconnect/reconnect returns the player to a safe authored state without duplicating an event.
- A stuck player receives authored escalating hints based on observed evidence, never an unexplained gate
  opening.
