"""Beta world -> 1.2.5 (Anvil) -> 1.12.2 -> Paper 1.21.11: the same 15-year path Theo's world took.

    python -m morrow.world.pipeline --synthetic          # build a stand-in world (testing only)
    python -m morrow.world.pipeline --beta <world-dir>    # the real Beta 1.5 world you built

Steps: copy the world, inject the 2011 cipher objects (inject.py), upgrade with Mojang's official
1.2.5 and 1.12.2 servers (Java 8), then hand it to the local Paper test server as "Mossfield".
Old server jars live in build/upgrade-chain/ (official Mojang downloads, SHA-1 checked).
"""

from __future__ import annotations

import gzip
import shutil
import subprocess
import sys
import time
from pathlib import Path

from morrow.canon import ciphers, layout
from morrow.world import inject, nbt
from morrow.world.selftest import make_world

ROOT = Path(__file__).resolve().parents[2]
CHAIN = ROOT / "build" / "upgrade-chain"
PAPER = ROOT / "build" / "mrrw-v2-test"
JAVA8 = Path(r"C:\Program Files\Java\jre1.8.0_451\bin\java.exe")
JAVA21 = "java"


def write_level_dat(world: Path) -> None:
    sx, sy, sz = layout.ANCHORS["spawn"]
    data = {
        "RandomSeed": nbt.Tag(nbt.LONG, ciphers.java_string_hash(layout.SEED_TEXT)),
        "SpawnX": nbt.i(sx), "SpawnY": nbt.i(sy + 1), "SpawnZ": nbt.i(sz),
        "Time": nbt.Tag(nbt.LONG, 6000), "LastPlayed": nbt.Tag(nbt.LONG, 1305700000000),
        "SizeOnDisk": nbt.Tag(nbt.LONG, 0), "LevelName": nbt.st("Mossfield"), "version": nbt.i(19132),
    }
    nbt.save_file(str(world / "level.dat"), "", {"Data": nbt.Tag(nbt.COMPOUND, data)})


def run_server(java: str, jar: Path, cwd: Path, extra: list[str] | None = None, timeout: int = 600) -> None:
    (cwd / "eula.txt").write_text("eula=true\n")
    props = cwd / "server.properties"
    if not props.exists():
        props.write_text("level-name=Mossfield\nonline-mode=false\nserver-port=25601\nspawn-protection=0\n")
    cmd = [java, "-Xmx2G", "-jar", str(jar), "nogui"] + (extra or [])
    print("  running", jar.name, "in", cwd)
    p = subprocess.Popen(cmd, cwd=cwd, stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True)
    start = time.time()
    assert p.stdout and p.stdin
    for line in p.stdout:
        if "Done (" in line or "For help" in line:
            p.stdin.write("stop\n")
            p.stdin.flush()
        if "Exception" in line or "ERROR" in line or "Convert" in line or "Upgrad" in line:
            print("   ", line.rstrip()[:160])
        if time.time() - start > timeout:
            p.kill()
            raise SystemExit("server timed out")
    p.wait()


def main(argv: list[str]) -> int:
    work = CHAIN / "work"
    if work.exists():
        shutil.rmtree(work)
    (work / "beta").mkdir(parents=True)
    world = work / "beta" / "Mossfield"
    if "--synthetic" in argv:
        world.mkdir()
        make_world(str(world))
        write_level_dat(world)
        print("synthetic Beta world built")
    else:
        src = Path(argv[argv.index("--beta") + 1])
        shutil.copytree(src, world)
        print("copied", src)
    for step in inject.inject(str(world)):
        print("  injected:", step)

    for name in ("1.2.5", "1.12.2"):
        stage = work / name
        shutil.copytree(world, stage / "Mossfield")
        run_server(str(JAVA8), CHAIN / ("server-%s.jar" % name), stage)
        world = stage / "Mossfield"
        print("upgraded through", name)

    target = PAPER / "Mossfield"
    if target.exists():
        shutil.rmtree(target)
    shutil.copytree(world, target)
    props = PAPER / "server.properties"
    text = props.read_text() if props.exists() else ""
    lines = [l for l in text.splitlines() if not l.startswith(("level-name=", "level-type="))]
    props.write_text("\n".join(lines + ["level-name=Mossfield"]) + "\n")
    for leftover in ("mossfield_test", "mossfield_test_nether", "mossfield_test_the_end"):
        shutil.rmtree(PAPER / leftover, ignore_errors=True)
    print("handed to Paper at", target, "(first start: java -jar paper.jar --forceUpgrade)")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
