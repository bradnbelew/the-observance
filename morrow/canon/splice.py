"""Build Morrow's fake Theo voice note (Act 9) from the real 2011 voicemail.

    python -m morrow.canon.splice <theo_voicemail_2011.wav> <start-end> [<start-end> ...]

Each segment is seconds, e.g. 4.2-5.6. Pick the words "no worries at all", then "keep it
billable till someone comes home" from your recording. The segments are joined with hard cuts
and no crossfade, so the splice is audible: that's the point. Players catch it by the jumps in
room tone, by the phone-band sound (2026 Theo never sounds like that), and because every word
traces to the 2011 voicemail. Writes dashboard/private-media/theo/T-09-fake.wav.
Needs a WAV input (any sample rate); no other tools.
"""

from __future__ import annotations

import sys
import wave
from pathlib import Path

OUT = Path(__file__).resolve().parents[2] / "dashboard" / "private-media" / "theo" / "T-09-fake.wav"


def main(argv: list[str]) -> int:
    if len(argv) < 2:
        print(__doc__)
        return 2
    src = wave.open(argv[0], "rb")
    rate, width, chans = src.getframerate(), src.getsampwidth(), src.getnchannels()
    frames = src.readframes(src.getnframes())
    src.close()
    step = width * chans
    out = bytearray()
    for seg in argv[1:]:
        a, b = (float(x) for x in seg.split("-"))
        out += frames[int(a * rate) * step:int(b * rate) * step]
        out += bytes(int(0.08 * rate) * step)  # a tiny dead gap between cuts
    OUT.parent.mkdir(parents=True, exist_ok=True)
    with wave.open(str(OUT), "wb") as w:
        w.setnchannels(chans)
        w.setsampwidth(width)
        w.setframerate(rate)
        w.writeframes(bytes(out))
    print("wrote", OUT)
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
