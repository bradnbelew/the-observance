# THE OBSERVANCE — CONTENT GUIDELINE (Ethan's field guide for making real artifacts)

> **SUPERSEDED PRE-V5 ARCHIVE — DO NOT USE AS A PRODUCTION CONTENT LIST.** The five fixed media assets and exact V5 content live under `arc/v5/`.

> **SUPERSEDED PRE-V5 ARCHIVE — DO NOT USE AS A PRODUCTION CONTENT LIST.** The five fixed media assets and exact V5 content live under `arc/v5/`.

> Your guide for producing the *real* content that makes the ARG land — found documents, videos, images,
> audio, screenshots — the stuff that rewards digging because the data is genuinely hidden *in the file*.
> The rule of thumb the whole project runs on: **AI-perfect content reads as a game asset; a real,
> slightly-degraded artifact with real hidden data reads as recovered.** You make the hero artifacts; I
> wire them into the world (the haunt that points at it → the payload check → the flag that advances a
> thread). Hand me the file + "here's what's hidden and where it should point," and I do the rest.

---

## 1. THE VOICE (never break this — one register across every surface)
The Record / the keepers / the Watcher speak ONE way (Set-B): **lowercase, sparse, declarative. States what
is and stops. No warmth, no second person, no named emotion, no exclamation, no adjectives-as-drama, no
"not just X but Y," no em-dash theatrics.** It never says "haunted," "eerie," "scary," "AI," "game,"
"server," "puzzle." It says what is true and goes quiet. The *silence around a line* is the dread.
- **Good:** `a fire was kept here. it is out. it was not put out by any hand that is still here.`
- **Bad:** `The eerie, long-dead hearth sends a chill down your spine — something is watching!`

The **surface townsfolk** (Aro/Wenna/Coll/Dob/Old Pell) are the ONE exception — ordinary modern humans:
casual, hedged, contractions, they can be warm or rude. They are people, not the Record. (Everything else
is the cold register.)

## 2. THE SPOILER LAW (what an artifact may and may NOT contain)
A found artifact is a **subset** of the truth, never the sealed core. It may carry: the surface facts (a
record opens, the living are counted, a hold was kept and left), atmosphere, a hidden coordinate/word, one
recognition token. It may NOT carry: a keeper's full fate spelled out, a cipher KEY, the Seventh's name, a
server flag, a database URL. **Test:** if someone cracks the file open in a text/NBT/EXIF tool, they should
find only *what they already saw* + the payload you meant them to find. Nothing sealed leaks early.

## 3. THE FAIRNESS LAW (hard, never unfair)
- **Real, named systems only.** Morse, a spectrogram, EXIF, an acrostic, a book-cipher with a *named* book,
  a substitution with an in-world crib. NEVER a homemade unnamed cipher or a random encrypted blob.
- **Self-confirming.** When they solve it they should *know* — a coordinate that leads somewhere real, a
  word that's obviously the answer. No "is this right?" ambiguity.
- **Retrace-fair.** After they know the answer, the trail that forces it must be traceable. If the only way
  to get it is to already know it, it's broken.
- **≥3 ways in (redundancy).** Never make one artifact the SOLE path to a conclusion — I'll pair it with an
  in-world clue and a record line so a missed file doesn't dead-end anyone. You make the artifact; tell me
  the conclusion it supports and I build the other two paths.

## 4. HOW TO HIDE DATA (the toolbox — pick per artifact)
| Technique | How | Feels like | Good for |
|---|---|---|---|
| **Acrostic** | first letter of each line/entry spells it | "wait — read down the margin" | documents, journals, record entries |
| **Margin coordinate** | an XYZ written small, or split across pages | a real place to go | any doc/page |
| **EXIF / metadata** | GPS coords or a word in a photo's EXIF (don't strip it!) | rewards the datamine | photos, screenshots |
| **Filename / description payload** | a token in the filename or a video description | the careless find | any hosted file |
| **Zero-width characters** | invisible chars between letters (I can generate these) | "there's text hidden in the text" | a "recovered" digital doc |
| **LSB steganography** | a message in an image's least-significant bits (I can encode) | forensic reward | images |
| **Audio spectrogram** | a word/coords drawn into the audio, visible in a spectrogram | "what's a spectrogram?" → aha | audio, video soundtrack |
| **Backmasking** | a message reversed in audio | play-it-backwards | audio, video |
| **Visible-but-brief** | coords/word on screen for <1s | frame-by-frame reward | video |
| **Rune substitution** | text in the campaign rune font (or vanilla `illageralt`) — earned literacy decodes it | "these are letters" | signs, carvings, doc headers |

Rule: **one payload should have TWO independent extraction paths where you can** (e.g. coords visible in a
frame AND in the spectrogram) so a group that misses one still gets it. Tell me which technique(s) you used
and the plaintext; I author the check + the redundant clues.

## 5. THE FOUND-FOOTAGE VIDEO (your first hero artifact — spec)
- **Look:** found, not produced. Handheld or "screen-recording-of-a-recording." Heavy degradation — VHS
  tracking, dropped frames, dead-air audio, a wrong timestamp burn-in. **No music, no titles, no editing.**
  Silence + one wrong detail beats spectacle. ~30–90s.
- **Director's recommended content:** the **finale lead-in** — *the Seventh, in the dark, far down, waiting.*
  A slow push or a static hold; a figure barely resolved; a held breath; it cuts. (Alt: an early "recovered
  recording of a prior group's last session" — mundane base footage, then one impossible frame, then cut.)
- **Recommended payload (functional, not-too-easy, two paths):** **visible coordinates in one <1s frame**
  (on a sign/carving/F3 readout) **+** the **same coords as a spectrogram** in the audio. Frame-by-frame
  finds one; "run the audio through a spectrogram" finds the other. Both point to the same in-world place.
- **Delivery:** unlisted YouTube or a hosted file; it surfaces on the record as one recovered-file entry.
- **Assist:** if you want, I'll generate a HyperFrames DRAFT clip so the beat is wired + testable now; your
  real one drops in when ready. Say the word.

## 6. THE OTHER ARTIFACTS (make as you like; each slots into a thread)
- **A found journal / recovered page** (photo/scan/screenshot) — the highest-value early artifact; hide an
  acrostic or margin coord that points to the first place. Register: the keeper whose hand it is (or Mara,
  the archivist).
- **A steganographic image** — a "recovered" screenshot/photo with LSB text or a spectrogram in an embedded
  audio note. Slots as a record/Drive attachment.
- **An audio artifact** — a voicemail / distorted recording with a spectrogram or backmask. (Google Voice
  line optional.)
- **Screenshots** — a corrupted terminal, a half-deleted post, a "recovered" chat log.
- **The recovered-archive (Google Drive) folder** — a small set of the above, framed as a salvaged archive;
  one file carries the payload, the rest are lore/texture.

## 6b. THE DISCOVERY WORKFLOW (how players FIND the scattered sites — the vibe engine)
Sites are now scattered far apart + terrain-integrated (W2: `/observance site set <keeper>` marks a spot,
`placeworld` stamps it). They are **not** handed to players on a map — the investigation *points* to them.
The primary, most-on-vibe path is **coords-in-artifacts**, and it's YOUR workflow because you survey the spots:
1. **Survey a site** where it fits the world (Vaun's treasury in a cave, Brann's watch on a hill you can see
   from afar, Sella's pool on a shore): stand there, `/observance site set brann`. Note the **XYZ**.
2. **Hide those coords** in the artifact that should lead there (§4 toolbox — a margin coordinate, a
   spectrogram, an acrostic that spells a bearing, a frame in the found-footage). The artifact is found
   *through the haunt/investigation*, cracked, and its payload is *the place*.
3. **Hand me** the artifact + "these coords → Brann's watch." I wire the haunt that surfaces the artifact,
   the arrival check at the site, and the redundant clues (so a missed artifact doesn't dead-end anyone).
Reliable fallbacks I build so nothing is ever un-findable: **legible-geography landmarks** (tall sites like
Brann's watch are visible from range — a sightline pulls players out to them) and a **late-earned
recovery-compass** whose needle settles toward the Seventh once earned (a reward, not an early crutch).
Rule: **every site reachable ≥3 ways** (its artifact-coords · a landmark or a second clue · the salience
drip that surfaces its thread) so the scatter is *mysterious*, never *lost*.

## 7. THE WORKFLOW (how we hand off)
1. You make an artifact + decide the plaintext payload + roughly where it should point (a keeper? the deep?
   the first place?).
2. You hand me: the file (or URL), the technique(s) used, the plaintext, and the intended "leads to."
3. I wire: the in-world haunt that surfaces it → the payload check (answer_kind coords/phrase/token) → the
   redundant clues (so it's ≥3-clue fair) → the flag that advances the thread → the record/story callback.
4. We playtest that beat; tune difficulty.

**One line to remember:** make it look *found and a little broken*, hide something *real* inside it, and
tell me what it means — the world will do the haunting that makes them look.
