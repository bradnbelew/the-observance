"""check_voice_register.py — Watcher/Keeper voice-register lint guardrail.

WHY: prose register violations in the Watcher/Keeper voice — capitalized lines
where the rule is lowercase-only, named emotions stated outright ("she felt
afraid") where the rule is oblique/shown-not-told, and the specific overused
chiasmus device "it was never X, it was Y" — have been flagged, fixed by hand,
and recurred across multiple authoring waves. The only prior check was a
self-reported footnote convention (writers noting "slop fixed" in a margin
note) that nobody mechanically enforced. This script enforces it.

SOURCE OF THE RULES (do not re-derive by guessing — read this file first if
the rules ever seem to need updating): `arc/corpus/npc-and-watcher-voice.md`.
  - SET A (surface NPCs) is explicitly modern-rough: contractions, capitals,
    exclamations, and named feelings are ALLOWED there. It is intentionally
    NOT linted by this script.
  - SET B / SET B-NEW (the Watcher, `voice.ts` register) is lowercase-only,
    no contractions, no exclamation, no emoji, no named feeling (file header,
    "## SET B", and the `must:` list in the SCHEMA block at the foot of the
    file).
  - SET C (the presiding Keeper) is "lowercase like the Watcher" but
    second-person and permitted the half-veiled "we" — still: no exclamation,
    no named feeling, no capital letter (`must_not:` list in the SCHEMA C
    block).
  - The chiasmus device "it was never X, it was Y" is explicitly called out
    as banned/cut in-file (e.g. the `docketReread` note: "slop A3: the
    chiasmus is CUT").
Only the spoken voice LINES themselves are linted — the backtick-quoted
bullet lines (`` - `...` ``) inside SET B / SET B-NEW / SET C. Surrounding
author commentary (the *(...)* notes, headers, schema block) is prose ABOUT
the rules, not voice output, and is not linted.

WHAT IT CHECKS (per Watcher/Keeper voice line):
  1. Capitalization — any capital letter anywhere in the line (Set B/C convention
     is lowercase-only, including names and line-starts; see file header "no
     capital letter, ever, including names and line-starts").
  2. Named-emotion phrases — a banned-phrase list derived from the actual voice
     rule ("no feeling named" / "no named feeling"), e.g. "felt afraid", "was
     terrified", "grew sad". See BANNED_EMOTION_PHRASES below for the derivation
     of each entry.
  3. The chiasmus pattern: "it was never X, it was Y" / "was not X, it was Y"
     (and close variants), via regex.

USAGE:
    python tools/check_voice_register.py

EXIT CODE:
    0 — no violations found (prints a clean summary count)
    1 — one or more violations found (prints file:line + line text + rule broken)

This script only REPORTS. It does not rewrite any prose — fixing a flagged
line is a content-judgment call left to a human authoring pass.
"""

from __future__ import annotations

import pathlib
import re
import sys

if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")

ROOT = pathlib.Path(__file__).resolve().parent.parent
VOICE_FILE = ROOT / "arc" / "corpus" / "npc-and-watcher-voice.md"

# Section headings that bound the Watcher/Keeper voice registers we lint.
# SET A (surface NPCs) is explicitly NOT linted -- it is allowed capitals,
# contractions, exclamations, and named feelings by its own rule.
LINT_SECTION_START = re.compile(r"^## SET B\b")  # first linted section
LINT_SECTION_END = re.compile(r"^## SCHEMA\b")   # end of all linted sections
SKIP_SECTION_HEADINGS = re.compile(r"^## SET A\b")

# A backtick-quoted bullet line, e.g.:  - `the record is not angry...`
# This is how every Set B / Set B-NEW / Set C spoken line is delimited in the
# corpus (verified against the live file: every spoken line in those sections
# is wrapped `- `...``; author commentary uses *(...)* or plain prose instead).
VOICE_LINE_RE = re.compile(r"^-\s*`(?P<text>.*)`\s*$")

# ---------------------------------------------------------------------------
# Check 1: capitalization
# ---------------------------------------------------------------------------
# The motif "▒" and template placeholders like ${name} / ${n} are not letters
# and are ignored automatically by \b[A-Z] matching on real capital letters.
CAPITAL_LETTER_RE = re.compile(r"[A-Z]")


def find_capital_violations(text: str) -> list[str]:
    if CAPITAL_LETTER_RE.search(text):
        return ["capital letter in lowercase-only voice line"]
    return []


# ---------------------------------------------------------------------------
# Check 2: named-emotion phrases
# ---------------------------------------------------------------------------
# Derived directly from the rule "no feeling named" / "no named feeling" in
# arc/corpus/npc-and-watcher-voice.md (file header + SCHEMA `must`/`must_not`
# lists). This is a bounded, non-overzealous list of the concrete "stated
# outright" forms the rule prohibits: <be-verb> + emotion-adjective, and
# <feel/grow/become> + emotion-adjective/noun. It deliberately does NOT flag
# emotion words used as nouns describing OTHER people/events in an oblique,
# third-party, or negated way (e.g. "it does not plead" or "warmth under
# dread" survive; "was afraid" or "felt sadness" do not).
EMOTION_WORDS = [
    "afraid", "scared", "terrified", "frightened", "fearful",
    "sad", "sadness", "sorrowful", "grief-stricken", "grieving",
    "angry", "anger", "furious", "rage", "wrathful",
    "happy", "happiness", "joyful", "joy", "elated",
    "lonely", "loneliness",
    "anxious", "anxiety", "nervous", "worried", "worry",
    "ashamed", "shame", "guilty", "guilt",
    "hopeful", "hopeless", "despair",
    "excited", "excitement",
    "relieved", "relief",
    "proud", "pride",
    "disgusted", "disgust",
    "hateful", "hatred",
    "loving", "love",
]
_emotion_alt = "|".join(EMOTION_WORDS)

BANNED_EMOTION_PATTERNS = [
    # "felt afraid", "felt a deep sadness", "felt the fear"
    re.compile(rf"\bfelt\b(?:\s+\w+){{0,3}}\s+(?:{_emotion_alt})\b", re.IGNORECASE),
    # "was afraid", "were terrified", "is anxious"
    re.compile(rf"\b(?:was|were|is|are|am)\s+(?:so\s+|very\s+|deeply\s+)?(?:{_emotion_alt})\b", re.IGNORECASE),
    # "grew sad", "grew angry", "grew afraid"
    re.compile(rf"\bgrew\s+(?:{_emotion_alt})\b", re.IGNORECASE),
    # "became afraid", "becomes anxious"
    re.compile(rf"\bbecame?s?\s+(?:{_emotion_alt})\b", re.IGNORECASE),
    # explicit naming: "the fear in me", "her sadness", "his anger" as a
    # stated internal state noun directly following a possessive
    re.compile(rf"\b(?:my|your|her|his|their|our|its)\s+(?:{_emotion_alt})\b", re.IGNORECASE),
]


def find_emotion_violations(text: str) -> list[str]:
    hits = []
    for pat in BANNED_EMOTION_PATTERNS:
        m = pat.search(text)
        if m:
            hits.append(f'named emotion stated outright ("{m.group(0)}")')
    return hits


# ---------------------------------------------------------------------------
# Check 3: the chiasmus device
# ---------------------------------------------------------------------------
# "it was never X, it was Y" / "it was not X, it was Y" / "was never X, was Y"
# Explicitly called out as a banned device in-file (docketReread note: "slop
# A3: the chiasmus is CUT").
CHIASMUS_RE = re.compile(
    r"\b(?:it\s+)?was\s+(?:never|not)\b[^.?!`]{0,80}?,\s*(?:it\s+)?was\b",
    re.IGNORECASE,
)


def find_chiasmus_violations(text: str) -> list[str]:
    m = CHIASMUS_RE.search(text)
    if m:
        return [f'"it was never X, it was Y" chiasmus device ("{m.group(0)}")']
    return []


# ---------------------------------------------------------------------------
# Scan
# ---------------------------------------------------------------------------

def iter_voice_lines(path: pathlib.Path):
    """Yields (lineno, line_text) for every backtick-quoted voice line found
    inside SET B / SET B-NEW / SET C (i.e. between the first '## SET B' and
    '## SCHEMA' headings), explicitly skipping SET A."""
    text = path.read_text(encoding="utf-8")
    in_lint_zone = False
    in_skip_zone = False
    for lineno, line in enumerate(text.splitlines(), start=1):
        if LINT_SECTION_START.match(line):
            in_lint_zone = True
            in_skip_zone = False
            continue
        if LINT_SECTION_END.match(line):
            in_lint_zone = False
            continue
        if SKIP_SECTION_HEADINGS.match(line):
            in_skip_zone = True
            in_lint_zone = False
            continue
        if line.startswith("## ") and in_skip_zone:
            # left SET A into some other heading; re-arm on next SET B match
            in_skip_zone = False
        if not in_lint_zone:
            continue
        m = VOICE_LINE_RE.match(line.strip())
        if m:
            yield lineno, m.group("text")


def main() -> int:
    if not VOICE_FILE.exists():
        print(f"ERROR: expected voice file not found at {VOICE_FILE}")
        return 1

    rel = VOICE_FILE.relative_to(ROOT).as_posix()
    violations = []  # list of (lineno, line_text, [reasons])
    line_count = 0

    for lineno, voice_text in iter_voice_lines(VOICE_FILE):
        line_count += 1
        reasons = []
        reasons += find_capital_violations(voice_text)
        reasons += find_emotion_violations(voice_text)
        reasons += find_chiasmus_violations(voice_text)
        if reasons:
            violations.append((lineno, voice_text, reasons))

    print("=" * 78)
    print("VOICE/REGISTER LINT — Watcher (SET B/B-NEW) + Keeper (SET C)")
    print("=" * 78)
    print(f"\nScanned: {rel}")
    print(f"Voice lines checked: {line_count}")

    if violations:
        print(f"\nVIOLATIONS ({len(violations)}):\n")
        for lineno, voice_text, reasons in violations:
            print(f"  {rel}:{lineno}")
            snippet = voice_text if len(voice_text) <= 140 else voice_text[:137] + "..."
            print(f"    line: `{snippet}`")
            for r in reasons:
                print(f"    rule broken: {r}")
            print()
    else:
        print("\nNo violations found.")

    print("=" * 78)
    if violations:
        print(f"RESULT: FAIL -- {len(violations)} voice-register violation(s) found.")
        print("(This script only reports. Rewriting flagged lines is a content-pass judgment call.)")
    else:
        print("RESULT: CLEAN -- no capitalization / named-emotion / chiasmus violations found.")
    print("=" * 78)

    return 1 if violations else 0


if __name__ == "__main__":
    sys.exit(main())
