"""mrrw bridge client: call the plugin's signed HTTP bridge (the same scheme the website and bot use).

    python bridge_client.py state
    python bridge_client.py event unmask [--reply-at 2026-10-20T21:00:00-04:00] [--voice T-04a] [--subject "Re: ..."]
    python bridge_client.py corpus "is anyone there" [--persona rookery] [--seed 7]
    python bridge_client.py sign GET /state 1700000000 ""      (print a signature, for cross-checks)

Options (before or after the command):
    --url URL        bridge base URL (default http://127.0.0.1:25588, or env MRRW_BRIDGE_URL)
    --secret S       the stop secret (default env MRRW_STOP_SECRET, else read from --config)
    --config PATH    plugins/mrrw/config.yml to read stop-secret from (default ../build/... not assumed)

Signature: X-Mrrw-Ts = unix seconds; X-Mrrw-Sig = lowercase hex HMAC-SHA256(key = stop secret,
METHOD + "\\n" + PATH_WITH_QUERY + "\\n" + TS + "\\n" + BODY). Standard library only.
"""

from __future__ import annotations

import argparse
import hashlib
import hmac
import json
import os
import re
import sys
import time
import urllib.error
import urllib.parse
import urllib.request


def sign(secret: str, method: str, path_with_query: str, ts: str, body: bytes) -> str:
    msg = f"{method}\n{path_with_query}\n{ts}\n".encode("utf-8") + body
    return hmac.new(secret.encode("utf-8"), msg, hashlib.sha256).hexdigest()


def fingerprint(secret: str) -> str:
    return hmac.new(secret.encode("utf-8"), b"mrrw-fingerprint", hashlib.sha256).hexdigest()[:8]


def secret_from_config(path: str) -> str | None:
    try:
        with open(path, encoding="utf-8") as f:
            for line in f:
                m = re.match(r"^stop-secret:\s*['\"]?(.*?)['\"]?\s*$", line)
                if m:
                    return m.group(1)
    except OSError:
        return None
    return None


def call(base: str, secret: str, method: str, path_with_query: str, payload: dict | None = None) -> tuple[int, dict | str]:
    body = b"" if payload is None else json.dumps(payload, separators=(",", ":")).encode("utf-8")
    ts = str(int(time.time()))
    headers = {"X-Mrrw-Ts": ts, "X-Mrrw-Sig": sign(secret, method, path_with_query, ts, body)}
    if payload is not None:
        headers["Content-Type"] = "application/json; charset=utf-8"
    req = urllib.request.Request(base.rstrip("/") + path_with_query, data=body if method == "POST" else None,
                                 headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=20) as r:
            status, text = r.status, r.read().decode("utf-8")
    except urllib.error.HTTPError as e:
        status, text = e.code, e.read().decode("utf-8", "replace")
    try:
        return status, json.loads(text)
    except ValueError:
        return status, text


def main(argv: list[str]) -> int:
    ap = argparse.ArgumentParser(description="mrrw bridge client")
    ap.add_argument("--url", default=os.environ.get("MRRW_BRIDGE_URL", "http://127.0.0.1:25588"))
    ap.add_argument("--secret", default=os.environ.get("MRRW_STOP_SECRET"))
    ap.add_argument("--config", default=None, help="plugins/mrrw/config.yml to read stop-secret from")
    sub = ap.add_subparsers(dest="cmd", required=True)
    sub.add_parser("state")
    ev = sub.add_parser("event")
    ev.add_argument("ref")
    ev.add_argument("--reply-at", default=None)
    ev.add_argument("--voice", default=None)
    ev.add_argument("--subject", default=None)
    co = sub.add_parser("corpus")
    co.add_argument("heard")
    co.add_argument("--persona", default=None)
    co.add_argument("--seed", type=int, default=None)
    sg = sub.add_parser("sign")
    sg.add_argument("method")
    sg.add_argument("path")
    sg.add_argument("ts")
    sg.add_argument("body", nargs="?", default="")
    a = ap.parse_args(argv)

    secret = a.secret or (secret_from_config(a.config) if a.config else None)
    if a.cmd == "sign":
        if not secret:
            print("need --secret (or MRRW_STOP_SECRET / --config)", file=sys.stderr)
            return 2
        print(sign(secret, a.method.upper(), a.path, a.ts, a.body.encode("utf-8")))
        return 0
    if not secret:
        print("need --secret (or MRRW_STOP_SECRET / --config)", file=sys.stderr)
        return 2

    if a.cmd == "state":
        status, out = call(a.url, secret, "GET", "/state")
    elif a.cmd == "event":
        status, out = call(a.url, secret, "POST", "/event", {
            "kind": "desk", "ref": a.ref, "reply_at": a.reply_at, "voice": a.voice, "subject": a.subject})
    else:
        q = {"heard": a.heard}
        if a.persona:
            q["persona"] = a.persona
        if a.seed is not None:
            q["seed"] = str(a.seed)
        status, out = call(a.url, secret, "GET", "/corpus?" + urllib.parse.urlencode(q, quote_via=urllib.parse.quote))
    print(status)
    print(json.dumps(out, indent=2, ensure_ascii=False) if isinstance(out, dict) else out)
    if isinstance(out, dict) and "fingerprint" in out:
        mine = fingerprint(secret)
        print(f"local fingerprint {mine}: {'match' if mine == out['fingerprint'] else 'MISMATCH'}")
    return 0 if 200 <= status < 300 else 1


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
