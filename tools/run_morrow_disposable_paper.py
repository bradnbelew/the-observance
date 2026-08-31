#!/usr/bin/env python3
"""Create-only, loopback-only disposable Paper lifecycle proof for the Morrow reboot."""

from __future__ import annotations

import argparse
import base64
import hashlib
import hmac
import http.server
import ipaddress
import json
import queue
import re
import shutil
import socket
import ssl
import stat
import subprocess
import threading
import time
from datetime import datetime, timedelta, timezone
from pathlib import Path
from typing import Any

from cryptography import x509
from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import rsa
from cryptography.x509.oid import NameOID


ROOT = Path(__file__).resolve().parents[1]
PAPER_SHA256 = "5ffef465eeeb5f2a3c23a24419d97c51afd7dbb4923ff42df9a3f58bba1ccfba"
PAPER_VERSION = "1.21.11"
PAPER_BUILD = 132
RELEASE = "morrow.rehearsal.c40f916.paper.v1"
CAMPAIGN = "05e13064-0569-5ac9-8652-0d1ca03c0a2d"
SECRET = "morrow-disposable-loopback-secret-v1-000000000000"
WORLD = "morrow_rehearsal"
GENESIS = "0" * 64


def sha256_bytes(value: bytes) -> str:
    return hashlib.sha256(value).hexdigest()


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def write_text(path: Path, body: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(body, encoding="utf-8", newline="\n")


def inventory(root: Path) -> dict[str, str]:
    return {path.relative_to(root).as_posix(): sha256(path)
            for path in sorted(root.rglob("*")) if path.is_file()}


def tree_hash(files: dict[str, str]) -> str:
    body = "".join(f"{name}\n{digest}\n" for name, digest in sorted(files.items()))
    return sha256_bytes(body.encode())


def candidate_inventory(root: Path) -> list[dict[str, Any]]:
    candidates = sorted(root.rglob("paper.jar"))
    if not candidates:
        raise FileNotFoundError(f"no paper.jar candidates beneath {root}")
    return [{"path": str(path.resolve()), "bytes": path.stat().st_size, "sha256": sha256(path),
             "exact_build_132_match": sha256(path) == PAPER_SHA256} for path in candidates]


def copy_runtime_inputs(target: Path, source_root: Path) -> dict[str, Any]:
    source_root = source_root.resolve()
    sections: dict[str, Any] = {}
    for name in ("cache", "libraries", "versions"):
        source = source_root / name
        if not source.is_dir():
            raise FileNotFoundError(f"bootstrap section is missing: {source}")
        if any(path.is_symlink() for path in source.rglob("*")):
            raise RuntimeError(f"bootstrap section contains a symlink: {name}")
        before = inventory(source)
        if not before:
            raise RuntimeError(f"bootstrap section is empty: {name}")
        destination = target / name
        if destination.exists():
            raise FileExistsError(f"fresh target already contains bootstrap section: {destination}")
        shutil.copytree(source, destination, copy_function=shutil.copyfile)
        for path in destination.rglob("*"):
            if path.is_file():
                path.chmod(path.stat().st_mode | stat.S_IWUSR)
        after = inventory(destination)
        if after != before:
            raise RuntimeError(f"bootstrap section changed during copy: {name}")
        sections[name] = {
            "source": str(source), "file_count": len(before),
            "bytes": sum((source / relative).stat().st_size for relative in before),
            "tree_sha256": tree_hash(before), "files": before,
        }
    required = target / "cache" / f"mojang_{PAPER_VERSION}.jar"
    version = target / "versions" / PAPER_VERSION / f"paper-{PAPER_VERSION}.jar"
    if not required.is_file() or not version.is_file():
        raise RuntimeError("copied bootstrap inputs lack the exact Mojang cache or Paper version artifact")
    return sections


def append_record(records: list[dict[str, Any]], key: str, event: str, payload: dict[str, Any]) -> None:
    payload_bytes = json.dumps(payload, sort_keys=True, separators=(",", ":")).encode()
    sequence = len(records) + 1
    previous = records[-1]["event_hash"] if records else GENESIS
    payload_hash = sha256_bytes(payload_bytes)
    event_hash = sha256_bytes(f"{sequence}\n{previous}\n{key}\n{event}\n{payload_hash}".encode())
    records.append({"sequence": sequence, "previous": previous, "event_hash": event_hash,
                    "key": key, "event": event, "payload": payload_bytes})


def seed_journal(path: Path) -> dict[str, Any]:
    records: list[dict[str, Any]] = []
    append_record(records, "morrow:release:initialized", "morrow_release_initialized", {"release": RELEASE})
    # Initialization payload is the raw release string in the production authority.
    records.clear()
    raw = RELEASE.encode()
    sequence = 1
    payload_hash = sha256_bytes(raw)
    event_hash = sha256_bytes(f"{sequence}\n{GENESIS}\nmorrow:release:initialized\nmorrow_release_initialized\n{payload_hash}".encode())
    records.append({"sequence": 1, "previous": GENESIS, "event_hash": event_hash,
                    "key": "morrow:release:initialized", "event": "morrow_release_initialized", "payload": raw})
    append_record(records, "paper-fixture:case", "morrow.act0.case_chain_authenticated",
                  {"fixture": "disposable-paper", "custody": "authenticated"})
    append_record(records, "paper-fixture:handoff", "morrow.act0.server_handoff_recovered",
                  {"fixture": "disposable-paper", "handoff": "recovered"})
    append_record(records, "paper:room04:greeting:v1", "morrow.act1.room04_witnessed",
                  {"action": "acknowledge_room", "fixture": "non-player-console-seed", "scope": "group"})
    lines = []
    for record in records:
        encoded = base64.urlsafe_b64encode(record["payload"]).decode().rstrip("=")
        lines.append("\t".join((str(record["sequence"]), record["previous"], record["event_hash"],
                                record["key"], record["event"], encoded)))
    write_text(path, "\n".join(lines) + "\n")
    return {"sequence": records[-1]["sequence"], "head_sha256": records[-1]["event_hash"],
            "projected_sequence": 4, "journal_sha256": sha256(path)}


def configure(target: Path, paper: Path, plugin: Path, cache_root: Path,
              paper_port: int, ingest_port: int) -> tuple[dict[str, Any], dict[str, Any]]:
    if target.exists():
        raise FileExistsError(f"refusing to reuse disposable target: {target}")
    (target / "plugins" / "Observance").mkdir(parents=True)
    shutil.copy2(paper, target / "paper.jar")
    shutil.copy2(plugin, target / "plugins" / plugin.name)
    cache = copy_runtime_inputs(target, cache_root)
    write_text(target / ".observance-disposable-morrow", "loopback-private-create-only\n")
    write_text(target / "eula.txt", "eula=true\n")
    write_text(target / "server.properties", "\n".join([
        "accepts-transfers=false", "allow-flight=false", "allow-nether=false", "difficulty=peaceful",
        "enable-command-block=false", "enable-query=false", "enable-rcon=false", "enable-status=true",
        "enforce-secure-profile=false", "enforce-whitelist=false", "force-gamemode=true", "gamemode=adventure", "generate-structures=false",
        "hardcore=false", f"level-name={WORLD}", "level-seed=40404", "level-type=minecraft:flat",
        "max-players=6", "motd=MORROW DISPOSABLE PAPER REHEARSAL", "online-mode=false",
        "prevent-proxy-connections=true", "server-ip=127.0.0.1", f"server-port={paper_port}",
        "spawn-animals=false", "spawn-monsters=false", "spawn-npcs=false", "spawn-protection=0",
        "sync-chunk-writes=true", "view-distance=5", "simulation-distance=4", "white-list=false", "",
    ]))
    config = (ROOT / "plugin" / "src" / "main" / "resources" / "config.yml").read_text(encoding="utf-8")
    marker = "morrow-reboot:\n"
    start = config.index(marker)
    config = config[:start] + "\n".join([
        "morrow-reboot:", "  enabled: true", f'  release-id: "{RELEASE}"',
        f'  campaign-id: "{CAMPAIGN}"', f'  world: "{WORLD}"',
        f'  ingest-url: "https://127.0.0.1:{ingest_port}/api/runtime/minecraft/events"',
        '  ingest-secret-env: "MORROW_MINECRAFT_INGEST_SECRET"', f'  ingest-secret: "{SECRET}"',
        "  projector:", "    connect-timeout-ms: 500", "    request-timeout-ms: 1000",
        "    initial-retry-ms: 100", "    maximum-retry-ms: 500", "  room04:",
        "    build-enabled: true", "    origin-x: 0", "    origin-y: 80", "    origin-z: 0",
        "  version-rooms:", "    build-enabled: true", "    origin-x: 32",
        "    origin-y: 80", "    origin-z: 0", "  consensus-audit:",
        "    build-enabled: true", "    origin-x: 64", "    origin-y: 80", "    origin-z: 0", "",
    ])
    write_text(target / "plugins" / "Observance" / "config.yml", config)
    journal = seed_journal(target / "plugins" / "Observance" / "morrow-reboot.journal")
    return cache, journal


def generate_tls(target: Path) -> tuple[Path, Path, Path, str]:
    key = rsa.generate_private_key(public_exponent=65537, key_size=2048)
    subject = issuer = x509.Name([x509.NameAttribute(NameOID.COMMON_NAME, "Morrow loopback rehearsal")])
    now = datetime.now(timezone.utc)
    cert = (x509.CertificateBuilder().subject_name(subject).issuer_name(issuer)
            .public_key(key.public_key()).serial_number(x509.random_serial_number())
            .not_valid_before(now - timedelta(minutes=5)).not_valid_after(now + timedelta(days=2))
            .add_extension(x509.SubjectAlternativeName([
                x509.DNSName("localhost"), x509.IPAddress(ipaddress.ip_address("127.0.0.1"))]), False)
            .sign(key, hashes.SHA256()))
    tls = target / "loopback-tls"
    tls.mkdir()
    cert_path, key_path, truststore = tls / "certificate.pem", tls / "private-key.pem", tls / "truststore.p12"
    cert_path.write_bytes(cert.public_bytes(serialization.Encoding.PEM))
    key_path.write_bytes(key.private_bytes(serialization.Encoding.PEM,
                                           serialization.PrivateFormat.TraditionalOpenSSL,
                                           serialization.NoEncryption()))
    subprocess.run(["keytool", "-importcert", "-noprompt", "-alias", "morrow-loopback",
                    "-file", str(cert_path), "-keystore", str(truststore), "-storetype", "PKCS12",
                    "-storepass", "morrow-local-only"], check=True, capture_output=True, text=True)
    return cert_path, key_path, truststore, sha256(cert_path)


class MockState:
    def __init__(self) -> None:
        self.attempts: list[dict[str, Any]] = []
        self.committed = threading.Event()


def start_ingest(port: int, cert: Path, key: Path) -> tuple[http.server.ThreadingHTTPServer, MockState]:
    state = MockState()

    class Handler(http.server.BaseHTTPRequestHandler):
        def do_POST(self) -> None:  # noqa: N802 - stdlib handler contract
            body = self.rfile.read(int(self.headers.get("content-length", "0")))
            timestamp = self.headers.get("x-morrow-timestamp", "")
            signature = self.headers.get("x-morrow-signature", "")
            expected = "v1=" + hmac.new(SECRET.encode(), timestamp.encode() + b"." + body,
                                         hashlib.sha256).hexdigest()
            valid = hmac.compare_digest(signature, expected)
            decoded = json.loads(body) if valid else {}
            attempt = len(state.attempts) + 1
            status = 503 if valid and attempt <= 2 else 200 if valid else 401
            state.attempts.append({
                "attempt": attempt, "body_sha256": sha256_bytes(body),
                "campaign_id": decoded.get("campaignId"), "event_key": decoded.get("eventKey"),
                "idempotency_key": decoded.get("idempotencyKey"), "release_id": decoded.get("releaseId"),
                "signature_valid": valid, "response_status": status,
            })
            response = ({"status": "committed", "releaseId": RELEASE} if status == 200
                        else {"status": "unavailable", "releaseId": RELEASE} if status == 503
                        else {"status": "rejected"})
            encoded = json.dumps(response, separators=(",", ":")).encode()
            self.send_response(status)
            self.send_header("content-type", "application/json")
            self.send_header("content-length", str(len(encoded)))
            self.end_headers()
            self.wfile.write(encoded)
            if status == 200:
                state.committed.set()

        def log_message(self, *_: Any) -> None:
            return

    server = http.server.ThreadingHTTPServer(("127.0.0.1", port), Handler)
    context = ssl.SSLContext(ssl.PROTOCOL_TLS_SERVER)
    context.load_cert_chain(certfile=cert, keyfile=key)
    server.socket = context.wrap_socket(server.socket, server_side=True)
    threading.Thread(target=server.serve_forever, daemon=True).start()
    return server, state


class PaperProcess:
    def __init__(self, target: Path, java: str, truststore: Path) -> None:
        self.lines: list[str] = []
        self.events: queue.Queue[str] = queue.Queue()
        command = [java, "-Xms1G", "-Xmx2G", f"-Djavax.net.ssl.trustStore={truststore}",
                   "-Djavax.net.ssl.trustStorePassword=morrow-local-only",
                   "-Dhttps.proxyHost=127.0.0.1", "-Dhttps.proxyPort=1",
                   "-Dhttp.proxyHost=127.0.0.1", "-Dhttp.proxyPort=1",
                   "-Dhttp.nonProxyHosts=localhost|127.*|[::1]", "-jar", "paper.jar", "--nogui"]
        self.process = subprocess.Popen(command, cwd=target, stdin=subprocess.PIPE, stdout=subprocess.PIPE,
                                        stderr=subprocess.STDOUT, text=True, encoding="utf-8",
                                        errors="replace", bufsize=1)
        self.reader = threading.Thread(target=self._read, daemon=True)
        self.reader.start()

    def _read(self) -> None:
        assert self.process.stdout is not None
        for line in self.process.stdout:
            clean = line.rstrip("\r\n")
            self.lines.append(clean)
            self.events.put(clean)

    def wait_for(self, token: str, timeout: float = 300) -> str:
        deadline = time.monotonic() + timeout
        while time.monotonic() < deadline:
            if self.process.poll() is not None:
                raise RuntimeError(f"Paper exited before {token!r}; exit={self.process.returncode}; "
                                   f"tail={self.lines[-12:]}")
            try:
                line = self.events.get(timeout=.5)
            except queue.Empty:
                continue
            if token in line:
                return line
        raise TimeoutError(f"timed out waiting for Paper output: {token}")

    def batch(self, name: str, command: str) -> list[str]:
        begin, end = f"MORROW_{name}_BEGIN", f"MORROW_{name}_END"
        start = len(self.lines)
        assert self.process.stdin is not None
        self.process.stdin.write(f"say {begin}\n{command}\nsay {end}\n")
        self.process.stdin.flush()
        self.wait_for(end, 60)
        return self.lines[start:]

    def command(self, command: str, token: str) -> str:
        assert self.process.stdin is not None
        self.process.stdin.write(command + "\n")
        self.process.stdin.flush()
        return self.wait_for(token, 120)

    def stop(self) -> None:
        if self.process.poll() is None:
            assert self.process.stdin is not None
            self.process.stdin.write("stop\n")
            self.process.stdin.flush()
        self.process.wait(timeout=180)
        self.reader.join(timeout=5)
        if self.process.returncode != 0:
            raise RuntimeError(f"Paper exited nonzero: {self.process.returncode}")


def entity_audit(runtime_ready: str, label: str) -> dict[str, int]:
    match = re.search(
        r"body_entities=(\d+) static_entities=(\d+) replay_entities=(\d+) version_entities=(\d+) consensus_entities=(\d+)",
        runtime_ready)
    if match is None:
        raise RuntimeError(f"{label} runtime receipt omitted PDC-owned entity counts")
    result = {"body": int(match.group(1)), "static_restore": int(match.group(2)),
              "entity_replay": int(match.group(3)), "version_rooms": int(match.group(4)),
              "consensus_audit": int(match.group(5))}
    expected = {"body": 2, "static_restore": 26, "entity_replay": 6,
                "version_rooms": 3, "consensus_audit": 8}
    if result != expected:
        raise RuntimeError(f"{label} PDC-owned entity counts {result}, expected {expected}")
    return result


def port_open(port: int) -> bool:
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as probe:
        probe.settimeout(.25)
        return probe.connect_ex(("127.0.0.1", port)) == 0


def lifecycle(target: Path, java: str, truststore: Path, ingest: MockState) -> dict[str, Any]:
    logs: dict[str, Any] = {}
    first = PaperProcess(target, java, truststore)
    try:
        first_room = first.wait_for("MORROW_ROOM04_READY status=BUILT", 300)
        first_versions = first.wait_for("MORROW_VERSION_ROOMS_READY status=BUILT", 300)
        first_consensus = first.wait_for("MORROW_CONSENSUS_AUDIT_READY status=BUILT", 300)
        first_ready = first.wait_for("MORROW_RUNTIME_READY", 300)
        first.wait_for("Done (", 300)
        if not ingest.committed.wait(30):
            raise TimeoutError("Morrow projector did not recover through the loopback HTTPS ingest")
        first_entities = entity_audit(first_ready, "FIRST")
        first.command("save-all flush", "Saved the game")
    finally:
        try:
            first.stop()
        finally:
            write_text(target / "morrow-first-start.log", "\n".join(first.lines) + "\n")
    if not any("MORROW_RUNTIME_CLOSED" in line for line in first.lines):
        raise RuntimeError("first Paper stop omitted Morrow cleanup receipt")

    attempts_before_restart = len(ingest.attempts)
    second = PaperProcess(target, java, truststore)
    try:
        restart_room = second.wait_for("MORROW_ROOM04_READY status=ALREADY_PRESENT", 300)
        restart_versions = second.wait_for("MORROW_VERSION_ROOMS_READY status=ALREADY_PRESENT", 300)
        restart_consensus = second.wait_for("MORROW_CONSENSUS_AUDIT_READY status=ALREADY_PRESENT", 300)
        restart_ready = second.wait_for("MORROW_RUNTIME_READY", 300)
        second.wait_for("Done (", 300)
        time.sleep(1.5)
        restart_entities = entity_audit(restart_ready, "RESTART")
        second.command("save-all flush", "Saved the game")
    finally:
        try:
            second.stop()
        finally:
            write_text(target / "morrow-restart.log", "\n".join(second.lines) + "\n")
    if not any("MORROW_RUNTIME_CLOSED" in line for line in second.lines):
        raise RuntimeError("restart Paper stop omitted Morrow cleanup receipt")
    if len(ingest.attempts) != attempts_before_restart:
        raise RuntimeError("durable cursor allowed a duplicate projection after restart")
    logs.update({"first_room": first_room, "first_version_rooms": first_versions,
                 "first_consensus_audit": first_consensus,
                 "first_ready": first_ready, "first_entities": first_entities,
                 "restart_room": restart_room, "restart_version_rooms": restart_versions,
                 "restart_consensus_audit": restart_consensus,
                 "restart_ready": restart_ready,
                 "restart_entities": restart_entities, "projection_attempts_before_restart": attempts_before_restart,
                 "projection_attempts_after_restart": len(ingest.attempts)})
    return logs


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--candidate-root", type=Path, required=True)
    parser.add_argument("--paper-jar", type=Path, required=True)
    parser.add_argument("--bootstrap-root", type=Path, required=True)
    parser.add_argument("--plugin-jar", type=Path, required=True)
    parser.add_argument("--target", type=Path, required=True)
    parser.add_argument("--receipt-dir", type=Path, required=True)
    parser.add_argument("--commit", required=True)
    parser.add_argument("--paper-port", type=int, default=25589)
    parser.add_argument("--ingest-port", type=int, default=29443)
    parser.add_argument("--java", default="java")
    args = parser.parse_args()
    target, receipt_dir = args.target.resolve(), args.receipt_dir.resolve()
    if receipt_dir.exists():
        raise FileExistsError(f"refusing to reuse receipt directory: {receipt_dir}")
    if port_open(args.paper_port) or port_open(args.ingest_port):
        raise RuntimeError("one or more requested loopback ports are already occupied")
    candidates = candidate_inventory(args.candidate_root.resolve())
    paper, plugin = args.paper_jar.resolve(), args.plugin_jar.resolve()
    if sha256(paper) != PAPER_SHA256:
        raise RuntimeError("selected Paper JAR is not the exact pinned build-132 artifact")
    if not any(row["path"] == str(paper) and row["exact_build_132_match"] for row in candidates):
        raise RuntimeError("selected Paper JAR is absent from the candidate inventory")

    cache, seeded = configure(target, paper, plugin, args.bootstrap_root.resolve(),
                              args.paper_port, args.ingest_port)
    cert, key, truststore, cert_hash = generate_tls(target)
    server, ingest = start_ingest(args.ingest_port, cert, key)
    try:
        evidence = lifecycle(target, args.java, truststore, ingest)
    finally:
        server.shutdown()
        server.server_close()
    if port_open(args.paper_port) or port_open(args.ingest_port):
        raise RuntimeError("a disposable loopback listener remained after shutdown")

    data = target / "plugins" / "Observance"
    required = [data / "morrow-reboot.journal", data / "morrow-reboot.projector.cursor",
                data / "morrow-room04.rollback.snapshot", data / "morrow-room04.install.receipt",
                data / "morrow-version-rooms.install.receipt",
                data / "morrow-consensus-audit.install.receipt"]
    if any(not path.is_file() for path in required):
        raise RuntimeError("one or more durable Morrow runtime artifacts are missing")
    cursor = (data / "morrow-reboot.projector.cursor").read_text(encoding="utf-8")
    if f"sequence={seeded['projected_sequence']}" not in cursor or seeded["head_sha256"] not in cursor:
        raise RuntimeError("durable projector cursor does not bind to the seeded owned event")
    cache_after = {name: inventory(target / name) for name in ("cache", "libraries", "versions")}
    for name, receipt in cache.items():
        if cache_after[name] != receipt["files"]:
            raise RuntimeError(f"Paper altered or downloaded into copied {name} inputs")

    receipt_dir.mkdir(parents=True)
    shutil.copy2(target / "morrow-first-start.log", receipt_dir / "morrow-first-start.log")
    shutil.copy2(target / "morrow-restart.log", receipt_dir / "morrow-restart.log")
    receipt = {
        "schema_version": "1.0.0-morrow-disposable-paper-runtime",
        "source_commit": args.commit,
        "runner_sha256": sha256(Path(__file__)),
        "target": str(target), "scope": "create-only loopback disposable Paper; never production",
        "paper": {"version": PAPER_VERSION, "build": PAPER_BUILD, "sha256": sha256(paper),
                  "selected_source": str(paper)},
        "paper_candidates": candidates,
        "plugin_sha256": sha256(plugin), "plugin_name": plugin.name,
        "bootstrap": cache,
        "release_id": RELEASE, "campaign_id": CAMPAIGN,
        "server": {"bind": "127.0.0.1", "port": args.paper_port, "online_mode": False,
                   "ingest": f"https://127.0.0.1:{args.ingest_port}/api/runtime/minecraft/events",
                   "jvm_non_loopback_proxy": "127.0.0.1:1", "production_credentials_loaded": False},
        "tls_certificate_sha256": cert_hash,
        "seeded_journal": seeded,
        "projection": {"attempts": ingest.attempts, "outage_responses": 2, "recovery_responses": 1,
                       "cursor_sha256": sha256(data / "morrow-reboot.projector.cursor"),
                       "no_restart_redelivery": True},
        "runtime": evidence,
        "durable_artifacts": {path.name: sha256(path) for path in required},
        "logs": {"first_sha256": sha256(receipt_dir / "morrow-first-start.log"),
                 "restart_sha256": sha256(receipt_dir / "morrow-restart.log")},
        "proof": {"exact_paper_match": True, "bootstrap_copy_hash_preserved": True,
                   "no_bootstrap_download_or_mutation": True, "room04_built_and_readback_audited": True,
                   "version_rooms_built_and_readback_audited": True,
                   "consensus_audit_built_and_readback_audited": True,
                  "restart_already_present_audit": True, "projector_outage_recovery": True,
                  "cursor_prevented_restart_duplicate": True, "entity_counts_stable_across_restart": True,
                  "graceful_cleanup_logged": True, "listeners_closed": True,
                  "graphical_client_automated": False, "production_mutated": False},
        "status": "pass",
    }
    write_text(receipt_dir / "paper-runtime-receipt.json", json.dumps(receipt, indent=2, sort_keys=True) + "\n")
    print(json.dumps({"status": "pass", "receipt": str(receipt_dir / "paper-runtime-receipt.json"),
                      "plugin_sha256": receipt["plugin_sha256"],
                      "paper_sha256": receipt["paper"]["sha256"],
                      "projection_attempts": len(ingest.attempts)}, indent=2))


if __name__ == "__main__":
    main()
