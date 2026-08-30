#!/usr/bin/env python3
"""Run one bounded real-client Morrow resource-pack decision on loopback only."""

from __future__ import annotations

import argparse
import hashlib
import http.server
import json
import re
import shutil
import socket
import subprocess
import sys
import threading
import time
from datetime import datetime, timezone
from pathlib import Path
from typing import Any

import run_morrow_disposable_paper as paper_harness


ROOT = Path(__file__).resolve().parents[1]
EXPECTED = {"loaded": "LOADED", "declined": "DECLINED"}


def sha1(path: Path) -> str:
    digest = hashlib.sha1()  # noqa: S324 - Minecraft's resource-pack protocol requires SHA-1
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def require(condition: bool, message: str) -> None:
    if not condition:
        raise RuntimeError(message)


class PackState:
    def __init__(self, payload: bytes) -> None:
        self.payload = payload
        self.requests: list[dict[str, Any]] = []
        self.lock = threading.Lock()


def start_pack_server(port: int, pack: Path) -> tuple[http.server.ThreadingHTTPServer, PackState]:
    payload = pack.read_bytes()
    state = PackState(payload)

    class Handler(http.server.BaseHTTPRequestHandler):
        def do_HEAD(self) -> None:  # noqa: N802 - stdlib handler contract
            self._respond(False)

        def do_GET(self) -> None:  # noqa: N802 - stdlib handler contract
            self._respond(True)

        def _respond(self, include_body: bool) -> None:
            allowed = self.path.split("?", 1)[0] == "/morrow-resourcepack.zip"
            status = 200 if allowed else 404
            row = {
                "method": self.command,
                "path": self.path.split("?", 1)[0],
                "status": status,
                "range": self.headers.get("range"),
                "user_agent_sha256": hashlib.sha256(
                    self.headers.get("user-agent", "").encode()).hexdigest(),
            }
            with state.lock:
                state.requests.append(row)
            if not allowed:
                self.send_response(404)
                self.send_header("content-length", "0")
                self.end_headers()
                return
            self.send_response(200)
            self.send_header("content-type", "application/zip")
            self.send_header("content-length", str(len(payload)))
            self.send_header("cache-control", "no-store")
            self.end_headers()
            if include_body:
                self.wfile.write(payload)

        def log_message(self, *_: Any) -> None:
            return

    server = http.server.ThreadingHTTPServer(("127.0.0.1", port), Handler)
    threading.Thread(target=server.serve_forever, daemon=True).start()
    return server, state


def enable_pack(target: Path, port: int, pack_sha1: str) -> str:
    config_path = target / "plugins" / "Observance" / "config.yml"
    config = config_path.read_text(encoding="utf-8")
    url = f"http://127.0.0.1:{port}/morrow-resourcepack.zip"
    global_block = "\n".join([
        "resource-pack:",
        f'  url: "{url}"',
        f'  sha1: "{pack_sha1}"',
        "  required: false",
        '  prompt: "Morrow can add authored visuals and sound. The complete vanilla path remains available."',
        "  delay-ticks: 20",
        "",
    ])
    config, changed = re.subn(
        r"(?ms)^resource-pack:\n.*?(?=^handoff:)", global_block, config, count=1)
    require(changed == 1, "could not bind the disposable global resource-pack block")
    needle = "    initial-retry-ms: 100\n    maximum-retry-ms: 500\n  room04:"
    replacement = "\n".join([
        "    initial-retry-ms: 100",
        "    maximum-retry-ms: 500",
        "  resource-pack:",
        "    enabled: true",
        "    required: false",
        "  room04:",
    ])
    require(needle in config, "could not bind the disposable Morrow resource-pack gate")
    config = config.replace(needle, replacement, 1)
    paper_harness.write_text(config_path, config)
    return url


def stop_owned_client(client_root: Path, run_id: str, launcher: subprocess.Popen[str] | None) -> None:
    stop_file = client_root / run_id / "stop.request"
    if stop_file.parent.is_dir():
        paper_harness.write_text(stop_file, "stop owned rehearsal client\n")
    if launcher is None:
        return
    try:
        launcher.wait(timeout=30)
    except subprocess.TimeoutExpired:
        launcher.terminate()
        launcher.wait(timeout=15)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--candidate-root", type=Path, required=True)
    parser.add_argument("--paper-jar", type=Path, required=True)
    parser.add_argument("--bootstrap-root", type=Path, required=True)
    parser.add_argument("--plugin-jar", type=Path, required=True)
    parser.add_argument("--resource-pack", type=Path, required=True)
    parser.add_argument("--target", type=Path, required=True)
    parser.add_argument("--receipt-dir", type=Path, required=True)
    parser.add_argument("--commit", required=True)
    parser.add_argument("--expected-status", choices=sorted(EXPECTED), required=True)
    parser.add_argument("--run-id", required=True)
    parser.add_argument("--username", default="MorrowPackTest")
    parser.add_argument("--paper-port", type=int, default=25592)
    parser.add_argument("--ingest-port", type=int, default=29446)
    parser.add_argument("--pack-port", type=int, default=29447)
    parser.add_argument("--status-timeout-seconds", type=int, default=90)
    parser.add_argument("--post-status-hold-seconds", type=int, default=15)
    parser.add_argument("--java", default="java")
    parser.add_argument("--launch-client", action="store_true")
    parser.add_argument("--launch-acknowledgement", default="")
    args = parser.parse_args()

    require(re.fullmatch(r"[a-z0-9][a-z0-9-]{2,63}", args.run_id) is not None,
            "run-id must use lowercase letters, digits, and hyphens")
    require(re.fullmatch(r"[A-Za-z0-9_]{3,16}", args.username) is not None,
            "username must be a vanilla-safe local pseudonym")
    require(15 <= args.status_timeout_seconds <= 105,
            "status-timeout-seconds must be between 15 and 105")
    require(0 <= args.post_status_hold_seconds <= 30,
            "post-status-hold-seconds must be between 0 and 30")
    acknowledgement = f"launch-visible-minecraft-resource-pack:{args.expected_status}"
    require(not args.launch_client or args.launch_acknowledgement == acknowledgement,
            f"visible client launch requires --launch-acknowledgement {acknowledgement}")

    target = args.target.resolve()
    receipt_dir = args.receipt_dir.resolve()
    pack = args.resource_pack.resolve()
    require(pack.is_file() and pack.read_bytes()[:4] == b"PK\x03\x04",
            "resource pack must be an existing ZIP")
    require(not target.exists(), f"refusing to reuse disposable target: {target}")
    require(not receipt_dir.exists(), f"refusing to reuse receipt directory: {receipt_dir}")
    for port in (args.paper_port, args.ingest_port, args.pack_port):
        require(not paper_harness.port_open(port), f"loopback port is occupied: {port}")

    candidate_root = args.candidate_root.resolve()
    paper = args.paper_jar.resolve()
    plugin = args.plugin_jar.resolve()
    candidates = paper_harness.candidate_inventory(candidate_root)
    require(paper_harness.sha256(paper) == paper_harness.PAPER_SHA256,
            "selected Paper JAR is not exact pinned build 132")
    require(any(row["path"] == str(paper) and row["exact_build_132_match"] for row in candidates),
            "selected Paper JAR is absent from candidate inventory")

    cache, seeded = paper_harness.configure(
        target, paper, plugin, args.bootstrap_root.resolve(), args.paper_port, args.ingest_port)
    pack_sha1 = sha1(pack)
    pack_url = enable_pack(target, args.pack_port, pack_sha1)
    cert, key, truststore, cert_hash = paper_harness.generate_tls(target)
    ingest_server, ingest = paper_harness.start_ingest(args.ingest_port, cert, key)
    pack_server, pack_state = start_pack_server(args.pack_port, pack)
    paper_process: paper_harness.PaperProcess | None = None
    launcher: subprocess.Popen[str] | None = None
    client_root = target / "client"
    observed_line = ""
    client_stdout = ""
    started_at = datetime.now(timezone.utc)
    try:
        paper_process = paper_harness.PaperProcess(target, args.java, truststore)
        room_line = paper_process.wait_for("MORROW_ROOM04_READY status=BUILT", 300)
        ready_line = paper_process.wait_for("MORROW_RUNTIME_READY", 300)
        require("resource_pack=optional" in ready_line,
                "Morrow did not report the optional pack policy")
        paper_process.wait_for("Done (", 300)
        require(ingest.committed.wait(30), "Morrow projector did not reach loopback ingest")

        if args.launch_client:
            launcher_command = [
                sys.executable, str(ROOT / "tools" / "run_morrow_offline_client.py"),
                "--run-id", args.run_id,
                "--target-root", str(client_root),
                "--server", f"127.0.0.1:{args.paper_port}",
                "--username", args.username,
                "--server-resource-pack-policy",
                "enabled" if args.expected_status == "loaded" else "disabled",
                "--wait-seconds", "120",
                "--terminate-after-wait",
                "--stop-when-requested",
                "--max-memory-mib", "1536",
            ]
            launcher = subprocess.Popen(
                launcher_command, cwd=ROOT, stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
                text=True, encoding="utf-8", errors="replace")
        token = f"MORROW_RESOURCE_PACK player={args.username} status={EXPECTED[args.expected_status]}"
        observed_line = paper_process.wait_for(token, args.status_timeout_seconds)
        if args.post_status_hold_seconds:
            time.sleep(args.post_status_hold_seconds)
        stop_owned_client(client_root, args.run_id, launcher)
        if launcher is not None and launcher.stdout is not None:
            client_stdout = launcher.stdout.read()
        launcher = None
        paper_process.command("save-all flush", "Saved the game")
        paper_process.stop()
        paper_harness.write_text(target / "morrow-pack-paper.log", "\n".join(paper_process.lines) + "\n")
        require(any("MORROW_RUNTIME_CLOSED" in line for line in paper_process.lines),
                "Paper stop omitted Morrow cleanup receipt")
        paper_process = None

        with pack_state.lock:
            pack_requests = list(pack_state.requests)
        successful_gets = [row for row in pack_requests
                           if row["method"] == "GET" and row["status"] == 200]
        if args.expected_status == "loaded":
            require(successful_gets, "loaded status had no successful loopback pack GET")
        client_receipt = client_root / args.run_id / "offline-client-receipt.json"
        require(not args.launch_client or client_receipt.is_file(),
                "owned client emitted no final receipt")

        receipt_dir.mkdir(parents=True)
        paper_log = receipt_dir / "morrow-pack-paper.log"
        shutil.copy2(target / "morrow-pack-paper.log", paper_log)
        retained_client = None
        retained_options = None
        retained_servers = None
        client_data: dict[str, Any] = {}
        if client_receipt.is_file():
            client_data = json.loads(client_receipt.read_text(encoding="utf-8"))
            retained_client = receipt_dir / "client-receipt.json"
            retained_options = receipt_dir / "options.txt"
            retained_servers = receipt_dir / "servers.dat"
            shutil.copy2(client_receipt, retained_client)
            shutil.copy2(client_receipt.parent / "game" / "options.txt", retained_options)
            shutil.copy2(client_receipt.parent / "game" / "servers.dat", retained_servers)
        receipt = {
            "schema_version": "1.0.0-morrow-resource-pack-rehearsal",
            "status": "bounded_handshake_pass_human_visual_parity_open",
            "source_commit": args.commit,
            "runner_sha256": paper_harness.sha256(Path(__file__)),
            "started_at": started_at.replace(microsecond=0).isoformat().replace("+00:00", "Z"),
            "scope": "create-only loopback Paper, loopback pack bytes, one owned vanilla client decision",
            "expected_status": EXPECTED[args.expected_status],
            "observed_status_line": observed_line,
            "resource_pack": {
                "url": pack_url,
                "required": False,
                "sha1": pack_sha1,
                "sha256": paper_harness.sha256(pack),
                "bytes": pack.stat().st_size,
                "requests": pack_requests,
            },
            "server": {
                "bind": "127.0.0.1", "paper_port": args.paper_port,
                "ingest_port": args.ingest_port, "pack_port": args.pack_port,
                "online_mode": False, "production_credentials_loaded": False,
                "room_ready_line": room_line, "runtime_ready_line": ready_line,
                "seeded_journal": seeded, "tls_certificate_sha256": cert_hash,
                "bootstrap_sections": {name: {k: v for k, v in value.items() if k != "files"}
                                       for name, value in cache.items()},
            },
            "client": {
                "launched": args.launch_client,
                "username": args.username,
                "receipt": retained_client.name if retained_client is not None else None,
                "receipt_sha256": paper_harness.sha256(retained_client) if retained_client is not None else None,
                "options": retained_options.name if retained_options is not None else None,
                "options_sha256": paper_harness.sha256(retained_options) if retained_options is not None else None,
                "servers": retained_servers.name if retained_servers is not None else None,
                "servers_sha256": paper_harness.sha256(retained_servers) if retained_servers is not None else None,
                "server_resource_pack_policy": client_data.get("server_resource_pack_policy"),
                "launcher_output_sha256": hashlib.sha256(client_stdout.encode()).hexdigest(),
                "client_log_retained": False,
            },
            "paper_log": {"file": paper_log.name, "sha256": paper_harness.sha256(paper_log)},
            "proof": {
                "optional_policy_reported": True,
                "exact_pack_hash_bound": True,
                "status_observed_from_real_client": args.launch_client,
                "pack_bytes_fetched": bool(successful_gets),
                "vanilla_fallback_visual_equivalence": False,
                "human_media_reviewed": False,
                "listeners_closed": True,
                "production_mutated": False,
            },
        }
        paper_harness.write_text(
            receipt_dir / "resource-pack-rehearsal.json",
            json.dumps(receipt, indent=2, sort_keys=True) + "\n")
        print(json.dumps({
            "status": receipt["status"],
            "observed": EXPECTED[args.expected_status],
            "receipt": str(receipt_dir / "resource-pack-rehearsal.json"),
            "pack_gets": len(successful_gets),
        }, indent=2))
    finally:
        stop_owned_client(client_root, args.run_id, launcher)
        if paper_process is not None:
            try:
                paper_process.stop()
            finally:
                paper_harness.write_text(
                    target / "morrow-pack-paper.log", "\n".join(paper_process.lines) + "\n")
        pack_server.shutdown()
        pack_server.server_close()
        ingest_server.shutdown()
        ingest_server.server_close()
        for port in (args.paper_port, args.ingest_port, args.pack_port):
            require(not paper_harness.port_open(port), f"loopback listener remained open: {port}")


if __name__ == "__main__":
    try:
        main()
    except Exception as failure:  # noqa: BLE001 - one concise fail-closed harness error
        print(f"MORROW RESOURCE PACK REHEARSAL: FAIL: {failure}", file=sys.stderr)
        raise SystemExit(1)
