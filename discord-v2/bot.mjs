// Copperline Recovery: the Discord bridge Theo set up in 2026 (Morrow v2).
//
// It runs next to the Minecraft server (it reads the server log and the mrrw plugin's data folder),
// relays Mossfield chat into #bridge, sends the Act 7 messages, and goes silent after the stop.
// It only ever speaks authored lines from canon.json, quotes a player's own in-game line verbatim,
// or (Act 7, a resident DMs it back) passes on what the plugin's corpus answers: words someone said.
//
// Group moments: Theo's big desk replies are mirrored into #general when the plugin's journal
// releases them ("theo.reply"), and in Act 10 the group votes in #general on what goes on the USB
// stick (buttons; Resident role only; the bot writes plugins/mrrw/usb-votes.json, the plugin counts).
//
//   node bot.mjs            run (needs .env, see README.md; run it under a supervisor)
//   node bot.mjs --dry-run  print what it would do for every act, no Discord connection
import fs from "node:fs";
import http from "node:http";
import os from "node:os";
import path from "node:path";
import { createHmac, timingSafeEqual } from "node:crypto";
import { fileURLToPath } from "node:url";

const HERE = path.dirname(fileURLToPath(import.meta.url));
const CANON = JSON.parse(fs.readFileSync(path.join(HERE, "canon.json"), "utf8"));
const DRY = process.argv.includes("--dry-run");

// A stray rejection must never kill the bot (a restart used to re-run the DM plan): log it instead.
process.on("unhandledRejection", (err) => console.error(new Date().toISOString(), "unhandledRejection:", err?.stack || err));
process.on("uncaughtException", (err) => console.error(new Date().toISOString(), "uncaughtException:", err?.stack || err));

// ---------------------------------------------------------------- config
function loadEnv() {
  const env = { ...process.env };
  const file = path.join(HERE, ".env");
  if (fs.existsSync(file)) {
    for (const line of fs.readFileSync(file, "utf8").split(/\r?\n/)) {
      const m = line.match(/^\s*([A-Z_]+)\s*=\s*(.*)\s*$/);
      if (m && !env[m[1]]) env[m[1]] = m[2];
    }
  }
  return env;
}
const ENV = loadEnv();
const MRRW = ENV.MRRW_DATA_DIR || path.join(HERE, "..", "build", "mrrw-v2-test", "plugins", "mrrw");
const MC_LOG = ENV.MC_LOG || path.join(HERE, "..", "build", "mrrw-v2-test", "logs", "latest.log");
const VOICE_DIR = ENV.VOICE_DIR || path.join(HERE, "..", "dashboard", "private-media", "theo");
const STATE_FILE = path.join(HERE, "state.json");
const NO_PINGS = { parse: [] };  // nothing the bot posts can ping @everyone, a role or a user
const BRIDGE_URL = (ENV.MORROW_BRIDGE_URL || "").trim().replace(/\/+$/, "");  // the plugin's bridge (Act 7 corpus replies)
const STOP_SECRET = (ENV.MORROW_STOP_SECRET || "").trim();                    // signs bridge requests (same as the site)
const VOTES_FILE = path.join(MRRW, "usb-votes.json");
const JOURNAL = path.join(MRRW, "journal.jsonl");

// ---------------------------------------------------------------- shared state with the plugin
function readAct() {
  try {
    const m = fs.readFileSync(path.join(MRRW, "state.yml"), "utf8").match(/^act:\s*(\d+)/m);
    return m ? Number(m[1]) : 1;
  } catch {
    return 1;
  }
}
const stopped = () => fs.existsSync(path.join(MRRW, "stopped.flag"));

/** Players' own in-game lines (roster.yml "lines": "name\u0000text"). Quoted verbatim only. */
function playerLines() {
  try {
    const raw = fs.readFileSync(path.join(MRRW, "roster.yml"), "utf8");
    const lines = [];
    let inLines = false;
    for (const l of raw.split(/\r?\n/)) {
      if (/^lines:/.test(l)) { inLines = true; continue; }
      if (inLines && /^\S/.test(l)) inLines = false;
      if (inLines) {
        const m = l.match(/^-\s+['"]?(.*?)['"]?$/);
        if (m) {
          const [name, text] = m[1].replace(/\\0|\u0000/g, "\u0000").split("\u0000");
          if (name && text) lines.push({ name, text });
        }
      }
    }
    return lines;
  } catch {
    return [];
  }
}

function loadState() {
  try {
    return { done: [], failed: {}, ...JSON.parse(fs.readFileSync(STATE_FILE, "utf8")) };
  } catch {
    return { done: [], failed: {} };
  }
}
/** Atomic: write a temp file, then rename, so a crash mid-write never loses what was already sent. */
function saveState(s) {
  const tmp = STATE_FILE + ".tmp";
  fs.writeFileSync(tmp, JSON.stringify(s, null, 1));
  fs.renameSync(tmp, STATE_FILE);
}

function voiceFile(id) {
  if (!id) return null;
  for (const ext of ["ogg", "mp3", "m4a", "wav"]) {
    const f = path.join(VOICE_DIR, `${id}.${ext}`);
    if (fs.existsSync(f)) return f;
  }
  return null;
}

// ---------------------------------------------------------------- the bridge (signed like the site: dashboard/src/lib/morrow/bridge-sign.ts)
/** Lowercase hex HMAC-SHA256(key = MORROW_STOP_SECRET, METHOD \n PATH_WITH_QUERY \n TS \n BODY). */
export function bridgeSignature(secret, method, target, ts, body = "") {
  return createHmac("sha256", secret).update(`${method.toUpperCase()}\n${target}\n${ts}\n${body}`).digest("hex");
}

/** A signed GET to the plugin. Resolves to the JSON, or null on any failure (unset, down, refused, slow). */
export async function bridgeGet(base, secret, pathWithQuery, timeoutMs = 2500) {
  if (!base || !secret) return null;
  try {
    const url = new URL(base + pathWithQuery);
    const ts = String(Math.floor(Date.now() / 1000));
    const res = await fetch(url, {
      headers: { "X-Mrrw-Ts": ts, "X-Mrrw-Sig": bridgeSignature(secret, "GET", url.pathname + url.search, ts), Accept: "application/json" },
      signal: AbortSignal.timeout(timeoutMs),
    });
    return res.ok ? await res.json() : null;
  } catch {
    return null;
  }
}

// ---------------------------------------------------------------- Act 7: Morrow answers DMs (the Turing test)
export const REPLY_EVERY_MS = 20_000;

/** Which persona Morrow answers as: the one the player names ("cairn, ..."), else the one it last wrote to them as. */
export function personaFor(text, last, personas = CANON.personas ?? []) {
  const t = String(text || "").toLowerCase();
  const named = personas.find((p) => new RegExp(`(^|[^a-z0-9_])@?${p.replace(/[^a-z0-9_]/gi, "")}($|[^a-z0-9_])`).test(t));
  return named || (personas.includes(last) ? last : null);
}

/** The /corpus request target, percent-encoded (%20 for spaces, never "+"); this exact string is signed. */
export const corpusPath = (heard, persona, seed) =>
  `/corpus?heard=${encodeURIComponent(heard)}&persona=${encodeURIComponent(persona)}&seed=${encodeURIComponent(String(seed))}`;

/** One reply per user per REPLY_EVERY_MS. */
export function makeLimiter(everyMs = REPLY_EVERY_MS) {
  const last = new Map();
  return (userId, now = Date.now()) => {
    const prev = last.get(userId);
    if (prev !== undefined && now - prev < everyMs) return false;
    last.set(userId, now);
    return true;
  };
}

/**
 * Morrow's answer to a resident's DM, or null for silence. Only in Act 7, only once it has written to
 * them as someone, at most one reply per 20 s each, and only what the plugin's corpus gives back
 * (GET /corpus): if the bridge doesn't answer, Morrow doesn't either.
 */
export async function personaReply({ act, stoppedNow = false, text, userId, lastPersona, limiter, bridge, seed, now = Date.now() }) {
  if (act < 7 || act > 9 || stoppedNow || !String(text || "").trim()) return null;  // while its DMs run
  const persona = personaFor(text, lastPersona);
  if (!persona) return null;
  if (!limiter(userId, now)) return null;
  const res = await bridgeGet(bridge.url, bridge.secret, corpusPath(String(text).slice(0, 500), persona, seed ?? now));
  const reply = typeof res?.reply === "string" ? res.reply.trim() : "";
  if (!reply) return null;
  return (CANON.persona_reply_template || "{as}: {text}").replace("{as}", () => persona).replace("{text}", () => reply.slice(0, 1800));
}

// ---------------------------------------------------------------- Act 10: the USB vote
export const USB_KEYS = ["clean", "recovered", "corpus"];

export function readVotes(file = VOTES_FILE) {
  try {
    const o = JSON.parse(fs.readFileSync(file, "utf8"));
    const votes = {};
    for (const [id, c] of Object.entries(o.votes ?? {})) if (USB_KEYS.includes(c)) votes[id] = c;
    return { open: o.open === true, votes };
  } catch {
    return { open: false, votes: {} };
  }
}

/** Atomic: temp file + rename, so the plugin never reads half a file. */
export function writeVotes(data, file = VOTES_FILE) {
  const tmp = `${file}.${process.pid}.tmp`;
  fs.writeFileSync(tmp, JSON.stringify({ open: data.open === true, votes: data.votes ?? {} }, null, 1));
  fs.renameSync(tmp, file);
}

/** One button press. Residents only; pressing again changes your vote. Returns the new data and what to tell them. */
export function castVote(data, userId, choice, isResident) {
  if (!data.open) return { data, said: CANON.usb_vote.closed };
  if (!isResident) return { data, said: CANON.usb_vote.residents_only };
  if (!USB_KEYS.includes(choice)) return { data, said: null };
  const next = { open: true, votes: { ...data.votes, [userId]: choice } };
  return { data: next, said: CANON.usb_vote.ack.replace("{label}", CANON.usb_vote.options[choice].label) };
}

export function tallyVotes(votes) {
  const n = { clean: 0, recovered: 0, corpus: 0 };
  for (const c of Object.values(votes)) if (c in n) n[c] += 1;
  return n;
}

/** The #general post: the prompt, what each choice costs, and the count so far. */
export function voteContent(tally, closed = false) {
  const v = CANON.usb_vote;
  const options = USB_KEYS.map((k) => `**${v.options[k].label}.** ${v.options[k].cost}`).join("\n\n");
  const counts = USB_KEYS.map((k) => `${v.options[k].label} ${tally[k]}`).join(", ");
  return `${v.prompt}\n\n${options}\n\n${v.tally.replace("{counts}", counts)}${closed ? `\n\n${v.closed}` : ""}`;
}

export const voteButtons = (tally) => USB_KEYS.map((k) => ({ id: `usb:${k}`, label: `${CANON.usb_vote.options[k].label} (${tally[k]})` }));

// ---------------------------------------------------------------- Theo's big replies, mirrored to #general
/** "theo.reply" entries in journal text: {id, subject, at}. Accepts the fields in detail or at the top level. */
export function theoReplies(journalText) {
  const out = [];
  for (const line of String(journalText).split(/\r?\n/)) {
    if (!line.trim()) continue;
    let o;
    try { o = JSON.parse(line); } catch { continue; }
    if (o?.kind !== "theo.reply") continue;
    const d = o.detail && typeof o.detail === "object" ? o.detail : o;
    const id = d.id || d.voice;
    if (id) out.push({ id: String(id), subject: String(d.subject ?? ""), at: String(d.at ?? o.at ?? "") });
  }
  return out;
}

/** What the bot posts for one released reply (null if canon has no text for it). */
export function theoPost(entry) {
  const text = CANON.theo_mirror?.replies?.[entry.id];
  if (!text) return null;
  const header = CANON.theo_mirror.header.replace("{subject}", () => entry.subject || "case 6118");
  return { content: `${header}\n${text}`.slice(0, 2000), voice: entry.id };
}

// ---------------------------------------------------------------- log tailing
/** Identity of the log file. Rotation shows up as a new inode/birth time, or a different first line. */
export function logIdentity(file) {
  const st = fs.statSync(file);
  let head = "";
  try {
    const fd = fs.openSync(file, "r");
    const buf = Buffer.alloc(Math.min(256, st.size));
    fs.readSync(fd, buf, 0, buf.length, 0);
    fs.closeSync(fd);
    head = buf.toString("utf8").split(/\r?\n/)[0];
  } catch { /* briefly unreadable: compare by stat only */ }
  return { ino: String(st.ino), birth: Math.round(st.birthtimeMs), head, size: st.size };
}

/**
 * Where to resume reading the log. A saved position is trusted only if it is the same file (same
 * inode and birth time where the filesystem has them, and the same first line) and not past its
 * end. Otherwise the log was rotated (Paper gzips latest.log at startup and at midnight) or
 * truncated, and the new file is read from the start. null = never tailed: start at the end.
 */
export function resumeOffset(saved, now) {
  if (!saved) return null;
  const hasIno = saved.ino && saved.ino !== "0" && now.ino !== "0";
  const sameFile = (hasIno ? saved.ino === now.ino && saved.birth === now.birth : true)
    && (!saved.head || !now.head || saved.head === now.head);
  if (!sameFile || now.size < saved.offset) return 0;
  return saved.offset;
}

// ---------------------------------------------------------------- the plan: what happens when
/** Act 7 messages, spread over the act. Each resident gets a different mix; Theo's real ones are among them. */
export function act7Plan(residentNames) {
  const plan = [];
  const quotes = playerLines();
  residentNames.forEach((name, i) => {
    const morrow = CANON.morrow_dms.filter((_, k) => (k + i) % 2 === 0).slice(0, 2);
    morrow.forEach((dm, k) => plan.push({ to: name, at: 2 + k * 9 + i, kind: "morrow", as: dm.as, text: `${dm.as}: ${dm.text}` }));
    const other = quotes.find((q) => q.name !== name);
    if (other) plan.push({ to: name, at: 14 + i, kind: "morrow", text: CANON.quote_template.replace("{other}", other.name).replace("{line}", other.text) });
    const theo = CANON.theo_dms[i % CANON.theo_dms.length];
    plan.push({ to: name, at: 6 + i * 3, kind: "theo", text: theo.text, voice: theo.voice });
  });
  return plan.sort((a, b) => a.at - b.at);
}

// ---------------------------------------------------------------- dry run
if (DRY) {
  const names = (ENV.DRY_PLAYERS || "PlayerOne,PlayerTwo,PlayerThree").split(",");
  console.log("act 1  pin in #general:", CANON.welcome);
  console.log("act 1+ #bridge relays chat lines and [Recovery] lines from", MC_LOG, "(no pings)");
  console.log("act 7  DM plan (hours after the act starts; each DM recorded per member):");
  for (const m of act7Plan(names)) console.log(`   +${String(m.at).padStart(2)}h  to ${m.to.padEnd(12)} [${m.kind}] ${m.text}${m.voice ? `  (+ voice ${m.voice}: ${voiceFile(m.voice) ? "found" : "not recorded yet"})` : ""}`);
  console.log("act 9  DM to every resident:", CANON.fake_theo.text, `(voice ${CANON.fake_theo.voice}: ${voiceFile(CANON.fake_theo.voice) ? "found" : "not made yet"})`);
  console.log("stop   #general:", CANON.bridge_cut.text, `(voice ${CANON.bridge_cut.voice}: ${voiceFile(CANON.bridge_cut.voice) ? "found" : "not recorded yet"})`);
  console.log("stop   then: no relay, no messages, ever");
  console.log("current plugin act:", readAct(), "| stopped:", stopped());
  // log tailing: the offset survives restarts; rotation is caught by file identity, not size alone
  const a = { ino: "11", birth: 100, head: "[10:00:00] [ServerMain/INFO]: Environment: A", size: 5000 };
  const b = { ino: "12", birth: 200, head: "[00:00:01] [ServerMain/INFO]: Environment: B", size: 9000 };
  const checks = [
    [resumeOffset(null, a) === null, "first run starts at the end of the log (no backlog flood)"],
    [resumeOffset({ ...a, offset: 4000 }, a) === 4000, "a restart resumes at the saved offset"],
    [resumeOffset({ ...a, offset: 4000 }, b) === 0, "rotated to a BIGGER new file: read from the start"],
    [resumeOffset({ ...a, offset: 4000 }, { ...a, size: 100 }) === 0, "truncated in place: read from the start"],
    [resumeOffset({ ...a, ino: "0", offset: 4000 }, { ...b, ino: "0" }) === 0, "no inode (some Windows shares): first line decides"],
  ];
  checks.push(...await dryGroupMoments());
  for (const [ok, what] of checks) console.log(`   ${ok ? "ok  " : "FAIL"} ${what}`);
  // let fetch's sockets finish closing first: exiting mid-close aborts Node on Windows (libuv UV_HANDLE_CLOSING)
  await new Promise((r) => setTimeout(r, 250));
  process.exit(checks.some(([ok]) => !ok) ? 1 : 0);
}

/** Dry run of the bridge pieces against a fake plugin: the corpus DMs, the USB vote, the Theo mirror. */
async function dryGroupMoments() {
  const checks = [];
  const SECRET = "dry-run-stop-secret";
  checks.push([bridgeSignature("test-stop-secret", "GET", "/state", "1700000000", "") === "568f9284666a5990ca2bebb4c800e07a26d3e792e627d51c0e7e1d860433c833",
    "bridge signature matches the shared test vector (site, plugin and e2e use the same one)"]);
  checks.push([bridgeSignature("test-stop-secret", "POST", "/event", "1700000000",
    `{"kind":"desk","ref":"unmask","reply_at":null,"voice":"T-04a","subject":"Re: who's Theo"}`) === "e3cbc7b422424af89abcfeefea80b6b97fc6447260fcf78b8c3103ab78dcaebb",
    "plugin vector: POST /event"]);
  checks.push([corpusPath("hello there", "rookery", 7) === "/corpus?heard=hello%20there&persona=rookery&seed=7"
    && bridgeSignature("test-stop-secret", "GET", corpusPath("hello there", "rookery", 7), "1700000000", "") === "c36897b35c62fd1830ac5b20ca8e66e551e439e42332f49a1d2c4cd494b6bd4c",
    "plugin vector: GET /corpus (raw %20-encoded target)"]);

  // a fake plugin: verifies the signature, answers /corpus with words someone said
  const seen = [];
  const fake = http.createServer((req, res) => {
    const ts = String(req.headers["x-mrrw-ts"] || "");
    const want = Buffer.from(bridgeSignature(SECRET, req.method, req.url, ts, ""));
    const got = Buffer.from(String(req.headers["x-mrrw-sig"] || ""));
    const ok = got.length === want.length && timingSafeEqual(got, want) && Math.abs(Date.now() / 1000 - Number(ts)) <= 120;
    const u = new URL(req.url, "http://x");
    seen.push({ ok, path: u.pathname, heard: u.searchParams.get("heard"), persona: u.searchParams.get("persona") });
    res.writeHead(ok ? 200 : 401, { "Content-Type": "application/json" });
    res.end(JSON.stringify(ok ? { reply: "the bed is gone", sources: ["cairn, chat, 2011-05-12 06:51:44"], obeys_law: true } : { error: "sig" }));
  });
  await new Promise((r) => fake.listen(0, "127.0.0.1", r));
  const bridge = { url: `http://127.0.0.1:${fake.address().port}`, secret: SECRET };
  const limiter = makeLimiter();
  const t0 = 1_000_000;
  const ask = (o) => personaReply({ act: 7, userId: "u1", lastPersona: "cairn", limiter, bridge, seed: 1, now: t0, ...o });
  const r1 = await ask({ text: "where did you put the bed" });
  checks.push([r1 === "cairn: the bed is gone", `Act 7 DM: Morrow answers as the persona it last wrote as, from the corpus (${r1})`]);
  checks.push([seen.length === 1 && seen[0].ok && seen[0].path === "/corpus" && seen[0].persona === "cairn" && seen[0].heard === "where did you put the bed",
    "the /corpus request is signed and carries heard + persona"]);
  checks.push([await ask({ text: "again?", now: t0 + 5_000 }) === null && seen.length === 1, "rate limit: nothing within 20 s (and no bridge call)"]);
  checks.push([(await ask({ text: "again?", now: t0 + 20_000 }))?.startsWith("cairn: "), "rate limit: answers again after 20 s"]);
  const r2 = await ask({ text: "juni_lantern are you there", userId: "u2" });
  checks.push([r2 === "juni_lantern: the bed is gone" && seen.at(-1).persona === "juni_lantern", "naming a persona makes Morrow answer as it"]);
  checks.push([await ask({ text: "hello", userId: "u3", act: 6 }) === null, "outside Act 7: no reply"]);
  checks.push([await ask({ text: "hello", userId: "u4", lastPersona: null }) === null, "before Morrow has written to them as anyone: no reply"]);
  checks.push([await ask({ text: "hello", userId: "u5", bridge: { ...bridge, secret: "wrong" } }) === null, "a refused signature: no reply"]);
  await new Promise((r) => fake.close(r));
  checks.push([await ask({ text: "hello", userId: "u6" }) === null, "bridge unreachable: no reply"]);

  // the USB vote
  const dir = fs.mkdtempSync(path.join(os.tmpdir(), "mrrw-vote-"));
  const file = path.join(dir, "usb-votes.json");
  let data = { open: true, votes: {} };
  writeVotes(data, file);
  let r = castVote(data, "111", "clean", true); data = r.data;
  checks.push([data.votes["111"] === "clean" && r.said.includes(CANON.usb_vote.options.clean.label), "a resident votes (ack names the choice)"]);
  r = castVote(data, "111", "corpus", true); data = r.data;
  checks.push([data.votes["111"] === "corpus" && Object.keys(data.votes).length === 1, "pressing again changes the vote, never adds one"]);
  r = castVote(data, "222", "recovered", false);
  checks.push([!("222" in r.data.votes) && r.said === CANON.usb_vote.residents_only, "no Resident role: no vote"]);
  data = castVote(data, "333", "recovered", true).data;
  writeVotes(data, file);
  const back = readVotes(file);
  checks.push([back.open && back.votes["111"] === "corpus" && back.votes["333"] === "recovered" && fs.readdirSync(dir).length === 1,
    "usb-votes.json is written atomically ({open, votes: {discord id: choice}}, no temp file left)"]);
  const closedR = castVote({ open: false, votes: {} }, "111", "clean", true);
  checks.push([Object.keys(closedR.data.votes).length === 0 && closedR.said === CANON.usb_vote.closed, "a closed vote takes nothing"]);
  const tally = tallyVotes(data.votes);
  const content = voteContent(tally);
  checks.push([USB_KEYS.every((k) => content.includes(CANON.usb_vote.options[k].cost)) && content.includes("Every word 1"),
    "the #general post names every choice's cost and the count so far"]);
  checks.push([voteButtons(tally).map((b) => b.label).join("|") === "The April copy (0)|The finished town (1)|Every word (1)", "three buttons with counts"]);
  fs.rmSync(dir, { recursive: true, force: true });
  console.log("act 10 #general vote:\n" + content.split("\n").map((l) => `   | ${l}`).join("\n"));

  // Theo's big replies, mirrored when the journal releases them
  const journal = [
    JSON.stringify({ at: "2026-10-17T19:00:00Z", kind: "desk.event", detail: { ref: "unmask" } }),
    JSON.stringify({ at: "2026-10-17T23:41:00Z", kind: "theo.reply", detail: { id: "T-04a", subject: "who runs this", at: "2026-10-17T23:41:00Z" } }),
    "not json",
    JSON.stringify({ at: "2026-10-18T00:10:00Z", kind: "theo.reply", id: "T-09b", subject: "case 6118 brief", reply_at: "x" }),
  ].join("\n");
  const rel = theoReplies(journal);
  checks.push([rel.length === 2 && rel[0].id === "T-04a" && rel[1].id === "T-09b", "theo.reply journal entries are read (detail or top level), other lines skipped"]);
  const post = theoPost(rel[0]);
  checks.push([!!post && post.content.includes("re: who runs this") && post.content.includes("it's me.  Theo") && post.voice === "T-04a",
    "the #general post carries the desk header, the canon text and the voice note id"]);
  checks.push([theoPost({ id: "T-99", subject: "" }) === null, "an unknown voice id posts nothing"]);
  for (const id of CANON.theo_mirror?.replies ? Object.keys(CANON.theo_mirror.replies) : []) {
    console.log(`theo   #general mirror ${id}: voice ${voiceFile(id) ? "found" : "not recorded yet"}`);
  }
  checks.push([["T-04a", "T-06a", "T-08a", "T-09a", "T-09b"].every((id) => CANON.theo_mirror?.replies?.[id]), "canon has text for every mirrored reply"]);
  return checks;
}

// ---------------------------------------------------------------- live
const { Client, GatewayIntentBits, Partials, AttachmentBuilder, ActionRowBuilder, ButtonBuilder, ButtonStyle, MessageFlags } = await import("discord.js");
for (const k of ["DISCORD_TOKEN", "GUILD_ID", "GENERAL_CHANNEL_ID", "BRIDGE_CHANNEL_ID", "RESIDENT_ROLE_ID"]) {
  if (!ENV[k]) { console.error(`missing ${k} in discord-v2/.env (see README.md)`); process.exit(1); }
}
const client = new Client({
  // DirectMessages: Act 7 replies to residents' DMs (DM text needs no Message Content intent). Buttons need no intent.
  intents: [GatewayIntentBits.Guilds, GatewayIntentBits.GuildMembers, GatewayIntentBits.DirectMessages],
  partials: [Partials.Channel],
  allowedMentions: NO_PINGS,
});
const state = loadState();
const once = async (flag, fn) => {
  if (state.done.includes(flag)) return;
  await fn();
  state.done.push(flag);
  saveState(state);
};

async function send(channel, text, voice) {
  const file = voiceFile(voice);
  await channel.send({ content: text, files: file ? [new AttachmentBuilder(file, { name: path.basename(file) })] : [], allowedMentions: NO_PINGS });
}

/**
 * One DM to one member, recorded per member. Success marks that member's flag done; a failure
 * (DMs closed, left the server) is recorded and retried on later passes up to MAX_DM_TRIES, then
 * given up. It never stops the others, and nobody who already got it gets it again.
 */
const MAX_DM_TRIES = 3;
async function dmOnce(flag, member, text, voice, as) {
  if (state.done.includes(flag)) return;
  const tries = state.failed[flag]?.tries ?? 0;
  if (tries >= MAX_DM_TRIES) return;
  try {
    await send(await member.createDM(), text, voice);
    state.done.push(flag);
    delete state.failed[flag];
    if (as) state.persona = { ...(state.persona ?? {}), [member.id]: as };  // who Morrow answers as if they write back
    console.log(new Date().toISOString(), "dm sent", flag);
  } catch (err) {
    state.failed[flag] = { tries: tries + 1, error: String(err?.message || err), at: new Date().toISOString() };
    console.error(new Date().toISOString(), "dm failed", flag, `(try ${tries + 1}/${MAX_DM_TRIES}):`, err?.message || err);
  }
  saveState(state);
}

/** setInterval for async work: never overlaps itself, never lets a rejection escape. */
function every(ms, name, fn) {
  let busy = false;
  return setInterval(async () => {
    if (busy) return;
    busy = true;
    try {
      await fn();
    } catch (err) {
      console.error(new Date().toISOString(), name, "failed:", err?.stack || err);
    } finally {
      busy = false;
    }
  }, ms);
}

client.on("error", (err) => console.error(new Date().toISOString(), "discord client error:", err?.message || err));

client.once("clientReady", async () => {
  console.log("mrrw bridge online as", client.user.tag);
  const guild = await client.guilds.fetch(ENV.GUILD_ID);
  const general = await guild.channels.fetch(ENV.GENERAL_CHANNEL_ID);
  const bridge = await guild.channels.fetch(ENV.BRIDGE_CHANNEL_ID);

  await once("welcome", async () => {
    const msg = await general.send({ content: CANON.welcome, allowedMentions: NO_PINGS });
    await msg.pin().catch(() => {});
  }).catch((err) => console.error("welcome failed:", err?.message || err));

  // Relay: tail the server log for chat and Morrow's lines. The position is saved in state.json,
  // so a restart carries on where it stopped; rotation is detected by file identity.
  let tail = null;  // { ino, birth, head, size, offset }
  if (fs.existsSync(MC_LOG)) {
    const id = logIdentity(MC_LOG);
    const resume = resumeOffset(state.log, id);
    tail = { ...id, offset: resume ?? id.size };
    console.log("log:", MC_LOG, resume === null ? "first run, starting at the end" : `resuming at ${resume} of ${id.size}`);
  }
  every(4000, "relay", async () => {
    if (stopped() || !fs.existsSync(MC_LOG)) return;
    const id = logIdentity(MC_LOG);
    const resume = resumeOffset(tail, id);
    if (tail && resume === 0 && tail.offset > 0) console.log(new Date().toISOString(), "log: rotated, reading the new file from the start");
    const from = resume ?? 0;
    tail = { ...id, offset: from };
    if (id.size > from) {
      const fd = fs.openSync(MC_LOG, "r");
      const buf = Buffer.alloc(id.size - from);
      fs.readSync(fd, buf, 0, buf.length, from);
      fs.closeSync(fd);
      const end = buf.lastIndexOf(0x0a);  // whole lines only; a half-written line waits for the next pass
      if (end >= 0) {
        tail.offset = from + end + 1;
        for (const line of buf.subarray(0, end).toString("utf8").split(/\r?\n/)) {
          const chat = line.match(/\[Async Chat Thread[^\]]*\/INFO\]: <([^>]+)> (.*)$/) || line.match(/INFO\]: <([^>]+)> (.*)$/);
          const morrow = line.match(/INFO\]: \[Recovery\] (.*)$/);
          // players' words are quoted, never obeyed: allowedMentions stops @everyone/role/user pings
          if (chat) await bridge.send({ content: `**${chat[1]}**: ${chat[2]}`, allowedMentions: NO_PINGS }).catch((e) => console.error("relay send failed:", e?.message || e));
          else if (morrow) await bridge.send({ content: `*[Recovery] ${morrow[1]}*`, allowedMentions: NO_PINGS }).catch((e) => console.error("relay send failed:", e?.message || e));
        }
      }
    }
    if (!state.log || state.log.offset !== tail.offset || state.log.ino !== tail.ino || state.log.head !== tail.head) {
      state.log = { ino: tail.ino, birth: tail.birth, head: tail.head, offset: tail.offset };
      saveState(state);
    }
  });

  // Act 7: the messages, scheduled from the moment the bot first sees Act 7.
  every(60_000, "act7", async () => {
    if (stopped()) return;
    const act = readAct();
    if (act >= 7 && !state.act7Start) { state.act7Start = Date.now(); saveState(state); }
    if (act < 7 || act > 9 || !state.act7Start) return;
    const members = (await guild.members.fetch()).filter((m) => m.roles.cache.has(ENV.RESIDENT_ROLE_ID) && !m.user.bot);
    const names = members.map((m) => m.displayName);
    const hours = (Date.now() - state.act7Start) / 3_600_000 * Number(ENV.TIME_SCALE || 1);
    for (const m of act7Plan(names)) {
      if (hours < m.at) continue;
      const member = members.find((x) => x.displayName === m.to);
      if (member) await dmOnce(`dm:${m.to}:${m.at}:${m.kind}`, member, m.text, m.voice, m.as);
    }
  });

  // Act 9: Morrow's spliced "Theo" voice note (phone-band, says "mate"): the authentication test.
  // Recorded per member, so one closed DM never re-sends it to everyone else.
  every(60_000, "act9", async () => {
    if (stopped() || readAct() < 9) return;
    if (state.done.includes("act9:fake_theo")) return;  // already sent to everyone by an older version
    const members = (await guild.members.fetch()).filter((m) => m.roles.cache.has(ENV.RESIDENT_ROLE_ID) && !m.user.bot);
    for (const m of members.values()) await dmOnce(`act9:fake_theo:${m.id}`, m, CANON.fake_theo.text, CANON.fake_theo.voice);
  });

  const isResident = async (userId) => {
    try {
      const m = await guild.members.fetch(userId);
      return m.roles.cache.has(ENV.RESIDENT_ROLE_ID) && !m.user.bot;
    } catch {
      return false;
    }
  };

  // Theo's big desk replies (T-04a, T-06a, T-08a, T-09a, T-09b): the plugin journals "theo.reply" when
  // the desk's reply time passes; the bot posts it to #general with the voice note. The journal offset is
  // kept in state.json; on the very first run only the last 6 hours are posted (no backlog flood).
  every(10_000, "theo-mirror", async () => {
    if (stopped() || !fs.existsSync(JOURNAL)) return;
    const size = fs.statSync(JOURNAL).size;
    const first = !state.journal;
    let from = state.journal?.offset ?? 0;
    if (size < from) from = 0;  // rotated or truncated
    if (size <= from) return;
    const fd = fs.openSync(JOURNAL, "r");
    const buf = Buffer.alloc(size - from);
    fs.readSync(fd, buf, 0, buf.length, from);
    fs.closeSync(fd);
    const end = buf.lastIndexOf(0x0a);
    if (end < 0) return;
    for (const entry of theoReplies(buf.subarray(0, end).toString("utf8"))) {
      const flag = `theo:${entry.id}:${entry.at}`;
      if (state.done.includes(flag)) continue;
      if (first && Date.now() - Date.parse(entry.at) > 6 * 3_600_000) { state.done.push(flag); continue; }
      const post = theoPost(entry);
      if (!post) { console.error(new Date().toISOString(), "theo.reply with no canon text:", entry.id); state.done.push(flag); continue; }
      await send(general, post.content, post.voice);
      state.done.push(flag);
      console.log(new Date().toISOString(), "theo mirror posted", entry.id);
    }
    state.journal = { offset: from + end + 1 };
    saveState(state);
  });

  // Act 10: the group's USB vote in #general. Opens when the plugin reaches Act 10 (the window can only
  // be booked then); the bot writes usb-votes.json and the plugin counts it (the console takes the winner).
  const voteRow = (tally) => new ActionRowBuilder().addComponents(
    voteButtons(tally).map((b) => new ButtonBuilder().setCustomId(b.id).setLabel(b.label).setStyle(ButtonStyle.Secondary)));
  every(30_000, "usb-vote", async () => {
    if (stopped() || readAct() < 10 || state.vote) return;
    const data = readVotes();
    writeVotes({ open: true, votes: data.votes });
    const msg = await general.send({ content: voteContent(tallyVotes(data.votes)), components: [voteRow(tallyVotes(data.votes))], allowedMentions: NO_PINGS });
    state.vote = { messageId: msg.id };
    saveState(state);
    console.log(new Date().toISOString(), "usb vote opened in #general");
  });

  client.on("interactionCreate", async (i) => {
    try {
      if (!i.isButton() || !i.customId.startsWith("usb:")) return;
      const choice = i.customId.slice(4);
      const resident = await isResident(i.user.id);
      // read, change and write with no await in between, so two presses at once can't drop a vote
      const before = readVotes();
      const cur = { ...before, open: before.open && !stopped() };
      const r = castVote(cur, i.user.id, choice, resident);
      const changed = r.data !== cur;
      if (changed) writeVotes(r.data);
      if (r.said) await i.reply({ content: r.said, flags: MessageFlags.Ephemeral, allowedMentions: NO_PINGS });
      else await i.deferUpdate();
      if (changed) {
        const tally = tallyVotes(r.data.votes);
        await i.message.edit({ content: voteContent(tally), components: [voteRow(tally)], allowedMentions: NO_PINGS });
        console.log(new Date().toISOString(), "usb vote", i.user.id, choice, JSON.stringify(tally));
      }
    } catch (err) {
      console.error(new Date().toISOString(), "vote failed:", err?.message || err);
    }
  });

  // Act 7: a resident DMs the bot back. Morrow answers as the persona it last wrote to them as (or the
  // one they name), only with what the plugin's corpus gives (GET /corpus): the Turing test. Theo's real
  // DMs stay scheduled as before; Morrow never answers outside Act 7 or without the bridge.
  const limiter = makeLimiter();
  client.on("messageCreate", async (message) => {
    try {
      if (message.guild || message.author.bot || !BRIDGE_URL) return;
      if (!(await isResident(message.author.id))) return;
      const reply = await personaReply({
        act: readAct(), stoppedNow: stopped(), text: message.content, userId: message.author.id,
        lastPersona: state.persona?.[message.author.id], limiter, bridge: { url: BRIDGE_URL, secret: STOP_SECRET }, seed: message.id,
      });
      if (reply) await message.channel.send({ content: reply, allowedMentions: NO_PINGS });
    } catch (err) {
      console.error(new Date().toISOString(), "dm reply failed:", err?.message || err);
    }
  });
  if (!BRIDGE_URL || !STOP_SECRET) console.log("bridge: MORROW_BRIDGE_URL / MORROW_STOP_SECRET not set, so Morrow won't answer DMs in Act 7");

  // The stop: one last line from Theo, then silence forever.
  const watch = every(5000, "stop-watch", async () => {
    if (!stopped()) return;
    clearInterval(watch);
    try {
      const data = readVotes();
      if (data.open) writeVotes({ open: false, votes: data.votes });
      if (state.vote) {
        const msg = await general.messages.fetch(state.vote.messageId);
        await msg.edit({ content: voteContent(tallyVotes(data.votes), true), components: [], allowedMentions: NO_PINGS });
      }
    } catch (err) {
      console.error("closing the vote failed:", err?.message || err);
    }
    try {
      await once("bridge_cut", () => send(general, CANON.bridge_cut.text, CANON.bridge_cut.voice));
    } catch (err) {
      console.error("bridge_cut failed:", err?.message || err);
    }
    console.log("stopped: the bridge is cut. Exiting.");
    setTimeout(() => process.exit(0), 3000);
  });
});

client.login(ENV.DISCORD_TOKEN);
