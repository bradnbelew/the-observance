// Copperline Recovery: the Discord bridge Theo set up in 2026 (Morrow v2).
//
// It follows the Minecraft server only through the mrrw plugin's signed bridge (MORROW_BRIDGE_URL),
// so it can run anywhere (Railway: see README.md). It relays Mossfield chat into #bridge, sends the
// Act 7 messages, and goes silent after the stop. It only ever speaks authored lines from
// canon.json, quotes a player's own in-game line verbatim, or (Act 7, a resident DMs it back) passes
// on what the plugin's corpus answers: words someone said.
//
// Group moments: Theo's big desk replies are mirrored into #general when the plugin releases them
// (GET /feed "theo.reply"), and in Act 10 the group votes in #general on what goes on the USB stick
// (buttons; Resident role only; each press is POST /usb, the plugin keeps and counts the votes).
//
//   node bot.mjs            run (needs .env or environment variables, see README.md)
//   node bot.mjs --dry-run  print what it would do for every act and test it against a fake plugin
import fs from "node:fs";
import http from "node:http";
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
const VOICE_DIR = ENV.VOICE_DIR || path.join(HERE, "..", "dashboard", "private-media", "theo");
const STATE_FILE = path.join(ENV.STATE_DIR || HERE, "state.json");  // on Railway: a volume, so a redeploy remembers
const NO_PINGS = { parse: [] };  // nothing the bot posts can ping @everyone, a role or a user
const BRIDGE = { url: (ENV.MORROW_BRIDGE_URL || "").trim().replace(/\/+$/, ""), secret: (ENV.MORROW_STOP_SECRET || "").trim() };

function loadState() {
  try {
    return { done: [], failed: {}, ...JSON.parse(fs.readFileSync(STATE_FILE, "utf8")) };
  } catch {
    return { done: [], failed: {} };
  }
}
/** Atomic: write a temp file, then rename, so a crash mid-write never loses what was already sent. */
function saveState(s) {
  fs.mkdirSync(path.dirname(STATE_FILE), { recursive: true });
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

/** A signed request to the plugin. Resolves to the JSON, or null on any failure (unset, down, refused, slow). */
async function bridgeCall(bridge, method, pathWithQuery, body, timeoutMs) {
  if (!bridge?.url || !bridge?.secret) return null;
  try {
    const url = new URL(bridge.url + pathWithQuery);
    const ts = String(Math.floor(Date.now() / 1000));
    const text = body === undefined ? "" : JSON.stringify(body);
    const res = await fetch(url, {
      method,
      headers: {
        "X-Mrrw-Ts": ts, "X-Mrrw-Sig": bridgeSignature(bridge.secret, method, url.pathname + url.search, ts, text), Accept: "application/json",
        ...(body === undefined ? {} : { "Content-Type": "application/json" }),
      },
      body: body === undefined ? undefined : text,
      signal: AbortSignal.timeout(timeoutMs),
    });
    return res.ok ? await res.json() : null;
  } catch {
    return null;
  }
}
export const bridgeGet = (bridge, pathWithQuery, timeoutMs = 2500) => bridgeCall(bridge, "GET", pathWithQuery, undefined, timeoutMs);
export const bridgePost = (bridge, pathOnly, body, timeoutMs = 4000) => bridgeCall(bridge, "POST", pathOnly, body, timeoutMs);

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
  const res = await bridgeGet(bridge, corpusPath(String(text).slice(0, 500), persona, seed ?? now));
  const reply = typeof res?.reply === "string" ? res.reply.trim() : "";
  if (!reply) return null;
  return (CANON.persona_reply_template || "{as}: {text}").replace("{as}", () => persona).replace("{text}", () => reply.slice(0, 1800));
}

// ---------------------------------------------------------------- Act 10: the USB vote
export const USB_KEYS = ["clean", "recovered", "corpus"];

/** {clean, recovered, corpus} counts from the plugin's usb.votes. */
export const tallyFrom = (votes) => Object.fromEntries(USB_KEYS.map((k) => [k, Number(votes?.[k] ?? 0)]));

/**
 * One button press. Residents only; the plugin keeps the ballots (POST /usb: pressing again changes
 * your vote) and refuses while the vote is closed. Returns what to tell them and the new counts
 * (tally null = nothing changed). If the bridge doesn't answer, nothing is said and nothing changes.
 */
export async function castVote({ bridge, userId, choice, isResident }) {
  if (!USB_KEYS.includes(choice)) return { said: null, tally: null };
  if (!isResident) return { said: CANON.usb_vote.residents_only, tally: null };
  const res = await bridgePost(bridge, "/usb", { voter: String(userId), choice });
  if (!res) return { said: null, tally: null };
  if (!res.accepted) return { said: CANON.usb_vote.closed, tally: null };
  return { said: CANON.usb_vote.ack.replace("{label}", CANON.usb_vote.options[choice].label), tally: tallyFrom(res.votes) };
}

/** The #general post: the prompt, what each choice costs, and the count so far. */
export function voteContent(tally, closed = false) {
  const v = CANON.usb_vote;
  const options = USB_KEYS.map((k) => `**${v.options[k].label}.** ${v.options[k].cost}`).join("\n\n");
  const counts = USB_KEYS.map((k) => `${v.options[k].label} ${tally[k]}`).join(", ");
  return `${v.prompt}\n\n${options}\n\n${v.tally.replace("{counts}", counts)}${closed ? `\n\n${v.closed}` : ""}`;
}

export const voteButtons = (tally) => USB_KEYS.map((k) => ({ id: `usb:${k}`, label: `${CANON.usb_vote.options[k].label} (${tally[k]})` }));

// ---------------------------------------------------------------- the feed: chat, [Recovery] lines, Theo's replies
/** What the bot posts for one released Theo reply (null if canon has no text for it). */
export function theoPost(entry) {
  const text = CANON.theo_mirror?.replies?.[entry.id];
  if (!text) return null;
  const header = CANON.theo_mirror.header.replace("{subject}", () => entry.subject || "case 6118");
  return { content: `${header}\n${text}`.slice(0, 2000), voice: entry.id };
}

/**
 * The posts for a page of GET /feed entries: chat and [Recovery] lines to #bridge, Theo's released
 * replies to #general with the voice note. Players' words are quoted, never obeyed.
 */
export function feedPosts(entries) {
  const out = [];
  for (const e of entries ?? []) {
    if (e?.kind === "chat" && e.name && e.text) out.push({ to: "bridge", content: `**${e.name}**: ${e.text}` });
    else if (e?.kind === "recovery" && e.text) out.push({ to: "bridge", content: `*[Recovery] ${e.text}*` });
    else if (e?.kind === "theo.reply" && e.id) {
      const post = theoPost({ id: String(e.id), subject: String(e.subject ?? "") });
      if (post) out.push({ to: "general", content: post.content, voice: post.voice, flag: `theo:${e.id}:${e.at ?? ""}` });
      else console.error(new Date().toISOString(), "theo.reply with no canon text:", e.id);
    }
  }
  return out;
}

// ---------------------------------------------------------------- the plan: what happens when
/** Act 7 messages, spread over the act. Each resident gets a different mix; Theo's real ones are among them. */
export function act7Plan(residentNames, quotes = []) {
  const plan = [];
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
  console.log("act 1+ #bridge relays chat lines and [Recovery] lines from the plugin's feed (GET /feed), no pings");
  console.log("act 7  DM plan (hours after the act starts; each DM recorded per member):");
  const quotes = [{ name: names[1] ?? "PlayerTwo", text: "the rail looks better unfinished" }];
  for (const m of act7Plan(names, quotes)) console.log(`   +${String(m.at).padStart(2)}h  to ${m.to.padEnd(12)} [${m.kind}] ${m.text}${m.voice ? `  (+ voice ${m.voice}: ${voiceFile(m.voice) ? "found" : "not recorded yet"})` : ""}`);
  console.log("act 9  DM to every resident:", CANON.fake_theo.text, `(voice ${CANON.fake_theo.voice}: ${voiceFile(CANON.fake_theo.voice) ? "found" : "not made yet"})`);
  console.log("stop   #general:", CANON.bridge_cut.text, `(voice ${CANON.bridge_cut.voice}: ${voiceFile(CANON.bridge_cut.voice) ? "found" : "not recorded yet"})`);
  console.log("stop   then: no relay, no messages, ever (the bot exits, and exits again if restarted)");
  if (BRIDGE.url) {  // the real plugin: read-only, except a vote when DRY_VOTE=<choice> (closed before Act 10: refused)
    const st = await bridgeGet(BRIDGE, "/state");
    console.log("plugin bridge:", BRIDGE.url, st ? `act ${st.act}, stopped ${st.stopped}, usb ${JSON.stringify(st.usb?.votes)}` : "not answering (or wrong secret)");
    const feed = await bridgeGet(BRIDGE, "/feed?after=0&max=200");
    if (feed) for (const p of feedPosts(feed.entries)) console.log(`   feed -> #${p.to}: ${p.content.split("\n")[0]}${p.voice ? `  (voice ${p.voice})` : ""}`);
    console.log("   feed:", feed ? `${feed.entries.length} entries, next ${feed.next}, end ${feed.end}` : "no answer");
    const lines = await bridgeGet(BRIDGE, "/lines");
    console.log("   lines:", lines ? `${lines.lines.length} residents' lines` : "no answer");
    if (ENV.DRY_VOTE) console.log("   vote:", JSON.stringify(await castVote({ bridge: BRIDGE, userId: "dry-run", choice: ENV.DRY_VOTE, isResident: true })));
  }
  const checks = await dryBridge();
  for (const [ok, what] of checks) console.log(`   ${ok ? "ok  " : "FAIL"} ${what}`);
  // let fetch's sockets finish closing first: exiting mid-close aborts Node on Windows (libuv UV_HANDLE_CLOSING)
  await new Promise((r) => setTimeout(r, 250));
  process.exit(checks.some(([ok]) => !ok) ? 1 : 0);
}

/** Dry run of everything the bot does through the bridge, against a fake plugin that checks signatures. */
async function dryBridge() {
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

  // a fake plugin: verifies the signature (body included), answers like the real bridge
  const seen = [];
  const plugin = { act: 10, stopped: false, ballots: {}, feed: [
    { n: 1, kind: "chat", name: "ash", text: "@everyone where is the rail going" },
    { n: 2, kind: "recovery", text: "3 block(s) restored (cairn storehouse)" },
    { n: 3, kind: "theo.reply", id: "T-04a", subject: "who runs this", at: "2026-10-17T23:41:00Z" },
    { n: 4, kind: "theo.reply", id: "T-99", subject: "", at: "2026-10-18T00:00:00Z" },
  ] };
  const fake = http.createServer((req, res) => {
    let body = "";
    req.on("data", (c) => { body += c; });
    req.on("end", () => {
      const ts = String(req.headers["x-mrrw-ts"] || "");
      const want = Buffer.from(bridgeSignature(SECRET, req.method, req.url, ts, body));
      const got = Buffer.from(String(req.headers["x-mrrw-sig"] || ""));
      const ok = got.length === want.length && timingSafeEqual(got, want) && Math.abs(Date.now() / 1000 - Number(ts)) <= 120;
      const u = new URL(req.url, "http://x");
      seen.push({ ok, method: req.method, path: u.pathname, query: u.searchParams, body });
      const reply = (status, o) => { res.writeHead(status, { "Content-Type": "application/json" }); res.end(JSON.stringify(o)); };
      if (!ok) return reply(401, { error: "sig" });
      if (u.pathname === "/corpus") return reply(200, { reply: "the bed is gone", sources: ["cairn, chat, 2011-05-12 06:51:44"], obeys_law: true });
      if (u.pathname === "/feed") {
        const after = Number(u.searchParams.get("after") ?? -1);
        const max = Number(u.searchParams.get("max") ?? 100);
        const entries = after < 0 ? [] : plugin.feed.filter((e) => e.n > after).slice(0, max);
        return reply(200, { entries, next: after < 0 ? plugin.feed.length : (entries.at(-1)?.n ?? after), end: plugin.feed.length, act: plugin.act, stopped: plugin.stopped });
      }
      if (u.pathname === "/usb" && req.method === "POST") {
        const v = JSON.parse(body);
        const open = plugin.act >= 10 && !plugin.stopped;
        if (open && USB_KEYS.includes(v.choice)) plugin.ballots[v.voter] = v.choice;
        const counts = tallyFrom(Object.fromEntries(USB_KEYS.map((k) => [k, Object.values(plugin.ballots).filter((c) => c === k).length])));
        return reply(200, { votes: counts, winner: null, open, accepted: open && USB_KEYS.includes(v.choice) });
      }
      return reply(404, { error: "no such endpoint" });
    });
  });
  await new Promise((r) => fake.listen(0, "127.0.0.1", r));
  const bridge = { url: `http://127.0.0.1:${fake.address().port}`, secret: SECRET };

  // Act 7 replies
  const limiter = makeLimiter();
  const t0 = 1_000_000;
  const ask = (o) => personaReply({ act: 7, userId: "u1", lastPersona: "cairn", limiter, bridge, seed: 1, now: t0, ...o });
  const r1 = await ask({ text: "where did you put the bed" });
  checks.push([r1 === "cairn: the bed is gone", `Act 7 DM: Morrow answers as the persona it last wrote as, from the corpus (${r1})`]);
  const c1 = seen.at(-1);
  checks.push([c1.ok && c1.path === "/corpus" && c1.query.get("persona") === "cairn" && c1.query.get("heard") === "where did you put the bed",
    "the /corpus request is signed and carries heard + persona"]);
  const n = seen.length;
  checks.push([await ask({ text: "again?", now: t0 + 5_000 }) === null && seen.length === n, "rate limit: nothing within 20 s (and no bridge call)"]);
  checks.push([(await ask({ text: "again?", now: t0 + 20_000 }))?.startsWith("cairn: "), "rate limit: answers again after 20 s"]);
  const r2 = await ask({ text: "juni_lantern are you there", userId: "u2" });
  checks.push([r2 === "juni_lantern: the bed is gone" && seen.at(-1).query.get("persona") === "juni_lantern", "naming a persona makes Morrow answer as it"]);
  checks.push([await ask({ text: "hello", userId: "u3", act: 6 }) === null, "outside Act 7: no reply"]);
  checks.push([await ask({ text: "hello", userId: "u4", lastPersona: null }) === null, "before Morrow has written to them as anyone: no reply"]);
  checks.push([await ask({ text: "hello", userId: "u5", bridge: { ...bridge, secret: "wrong" } }) === null, "a refused signature: no reply"]);

  // the feed
  const first = await bridgeGet(bridge, "/feed?after=-1&max=100");
  checks.push([first?.entries.length === 0 && first.next === 4, "a new bot starts at the end of the feed (no backlog flood)"]);
  const page = await bridgeGet(bridge, "/feed?after=0&max=100");
  const posts = feedPosts(page.entries);
  checks.push([page.next === 4 && posts.length === 3, "a page relays chat, [Recovery] and Theo's reply; an unknown voice id posts nothing"]);
  checks.push([posts[0].to === "bridge" && posts[0].content === "**ash**: @everyone where is the rail going"
    && posts[1].content === "*[Recovery] 3 block(s) restored (cairn storehouse)*", "chat and [Recovery] lines go to #bridge, quoted"]);
  checks.push([posts[2].to === "general" && posts[2].content.includes("re: who runs this") && posts[2].content.includes("it's me.  Theo")
    && posts[2].voice === "T-04a" && posts[2].flag === "theo:T-04a:2026-10-17T23:41:00Z", "Theo's reply goes to #general with the desk header, canon text and voice note"]);
  checks.push([(await bridgeGet(bridge, "/feed?after=2&max=1"))?.entries.map((e) => e.n).join() === "3", "the cursor pages from where it stopped"]);

  // the USB vote
  let v = await castVote({ bridge, userId: "111", choice: "clean", isResident: true });
  checks.push([v.said.includes(CANON.usb_vote.options.clean.label) && v.tally.clean === 1, "a resident votes (ack names the choice)"]);
  checks.push([JSON.parse(seen.at(-1).body).voter === "111" && seen.at(-1).ok && seen.at(-1).method === "POST", "the vote is a signed POST /usb (body signed too)"]);
  v = await castVote({ bridge, userId: "111", choice: "corpus", isResident: true });
  checks.push([v.tally.corpus === 1 && v.tally.clean === 0, "pressing again changes the vote, never adds one"]);
  const before = seen.length;
  v = await castVote({ bridge, userId: "222", choice: "recovered", isResident: false });
  checks.push([v.said === CANON.usb_vote.residents_only && v.tally === null && seen.length === before, "no Resident role: no vote (and no bridge call)"]);
  plugin.stopped = true;
  v = await castVote({ bridge, userId: "333", choice: "recovered", isResident: true });
  checks.push([v.said === CANON.usb_vote.closed && v.tally === null, "a closed vote takes nothing"]);
  const tally = { clean: 0, recovered: 1, corpus: 1 };
  const content = voteContent(tally);
  checks.push([USB_KEYS.every((k) => content.includes(CANON.usb_vote.options[k].cost)) && content.includes("Every word 1"),
    "the #general post names every choice's cost and the count so far"]);
  checks.push([voteButtons(tally).map((b) => b.label).join("|") === "The April copy (0)|The finished town (1)|Every word (1)", "three buttons with counts"]);
  console.log("act 10 #general vote:\n" + content.split("\n").map((l) => `   | ${l}`).join("\n"));

  await new Promise((r) => fake.close(r));
  checks.push([await ask({ text: "hello", userId: "u6" }) === null, "bridge unreachable: no reply"]);
  checks.push([(await castVote({ bridge, userId: "111", choice: "clean", isResident: true })).tally === null, "bridge unreachable: a vote changes nothing"]);

  for (const id of CANON.theo_mirror?.replies ? Object.keys(CANON.theo_mirror.replies) : []) {
    console.log(`theo   #general mirror ${id}: voice ${voiceFile(id) ? "found" : "not recorded yet"}`);
  }
  checks.push([["T-04a", "T-06a", "T-08a", "T-09a", "T-09b"].every((id) => CANON.theo_mirror?.replies?.[id]), "canon has text for every mirrored reply"]);
  return checks;
}

// ---------------------------------------------------------------- live
const state = loadState();
if (state.stopped) {  // after the stop the bridge stays cut, however often a supervisor restarts the bot
  console.log("the bridge was cut at the stop; nothing to do. Exiting.");
  process.exit(0);
}
const { Client, GatewayIntentBits, Partials, AttachmentBuilder, ActionRowBuilder, ButtonBuilder, ButtonStyle, MessageFlags } = await import("discord.js");
for (const k of ["DISCORD_TOKEN", "GUILD_ID", "GENERAL_CHANNEL_ID", "BRIDGE_CHANNEL_ID", "RESIDENT_ROLE_ID", "MORROW_BRIDGE_URL", "MORROW_STOP_SECRET"]) {
  if (!ENV[k]) { console.error(`missing ${k} (environment or discord-v2/.env, see README.md)`); process.exit(1); }
}
const client = new Client({
  // DirectMessages: Act 7 replies to residents' DMs (DM text needs no Message Content intent). Buttons need no intent.
  intents: [GatewayIntentBits.Guilds, GatewayIntentBits.GuildMembers, GatewayIntentBits.DirectMessages],
  partials: [Partials.Channel],
  allowedMentions: NO_PINGS,
});
const once = async (flag, fn) => {
  if (state.done.includes(flag)) return;
  await fn();
  state.done.push(flag);
  saveState(state);
};

// What the plugin last said about the campaign (GET /state and every GET /feed). Until the bridge first
// answers, nothing act-based happens: act 0 is below every threshold.
const live = { act: 0, stopped: false, votes: tallyFrom({}), seen: false };
const note = (o) => {
  if (!o) return;
  if (Number.isFinite(o.act)) live.act = o.act;
  if (typeof o.stopped === "boolean") live.stopped = o.stopped;
  if (o.usb?.votes) live.votes = tallyFrom(o.usb.votes);
  if (!live.seen) { live.seen = true; console.log(new Date().toISOString(), "bridge answering: act", live.act, "stopped", live.stopped); }
};
const stopped = () => live.stopped;

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
  console.log("mrrw bridge online as", client.user.tag, "| plugin bridge", BRIDGE.url);
  const guild = await client.guilds.fetch(ENV.GUILD_ID);
  const general = await guild.channels.fetch(ENV.GENERAL_CHANNEL_ID);
  const bridgeChannel = await guild.channels.fetch(ENV.BRIDGE_CHANNEL_ID);

  await once("welcome", async () => {
    const msg = await general.send({ content: CANON.welcome, allowedMentions: NO_PINGS });
    await msg.pin().catch(() => {});
  }).catch((err) => console.error("welcome failed:", err?.message || err));

  // The campaign's act, the stop and the vote counts, from the plugin.
  let quiet = 0;
  every(5000, "state", async () => {
    const st = await bridgeGet(BRIDGE, "/state");
    if (st) { note(st); quiet = 0; } else if (++quiet === 12) console.error(new Date().toISOString(), "bridge: no answer for a minute (server down, port 25588 closed, or a secret mismatch)");
  });

  // Relay: chat and Morrow's lines into #bridge, Theo's released replies into #general. The cursor is
  // saved in state.json, so a restart carries on where it stopped; a new bot starts at the end.
  every(4000, "feed", async () => {
    if (stopped()) return;
    const page = await bridgeGet(BRIDGE, `/feed?after=${state.feed ?? -1}&max=100`);
    if (!page) return;
    note(page);
    for (const p of feedPosts(page.entries)) {
      if (p.flag && state.done.includes(p.flag)) continue;
      const channel = p.to === "general" ? general : bridgeChannel;
      await send(channel, p.content, p.voice).catch((e) => console.error("relay send failed:", e?.message || e));
      if (p.flag) { state.done.push(p.flag); console.log(new Date().toISOString(), "theo mirror posted", p.voice); }
    }
    if (state.feed !== page.next) { state.feed = page.next; saveState(state); }
  });

  // Act 7: the messages, scheduled from the moment the bot first sees Act 7.
  every(60_000, "act7", async () => {
    if (stopped()) return;
    if (live.act >= 7 && !state.act7Start) { state.act7Start = Date.now(); saveState(state); }
    if (live.act < 7 || live.act > 9 || !state.act7Start) return;
    const members = (await guild.members.fetch()).filter((m) => m.roles.cache.has(ENV.RESIDENT_ROLE_ID) && !m.user.bot);
    const names = members.map((m) => m.displayName);
    const quotes = (await bridgeGet(BRIDGE, "/lines"))?.lines ?? [];
    const hours = (Date.now() - state.act7Start) / 3_600_000 * Number(ENV.TIME_SCALE || 1);
    for (const m of act7Plan(names, quotes)) {
      if (hours < m.at) continue;
      const member = members.find((x) => x.displayName === m.to);
      if (member) await dmOnce(`dm:${m.to}:${m.at}:${m.kind}`, member, m.text, m.voice, m.as);
    }
  });

  // Act 9: Morrow's spliced "Theo" voice note (phone-band, says "mate"): the authentication test.
  // Recorded per member, so one closed DM never re-sends it to everyone else.
  every(60_000, "act9", async () => {
    if (stopped() || live.act < 9) return;
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

  // Act 10: the group's USB vote in #general. Opens when the plugin reaches Act 10 (the window can only
  // be booked then); each press is POST /usb, and the plugin counts it (the console takes the winner).
  const voteRow = (tally) => new ActionRowBuilder().addComponents(
    voteButtons(tally).map((b) => new ButtonBuilder().setCustomId(b.id).setLabel(b.label).setStyle(ButtonStyle.Secondary)));
  every(30_000, "usb-vote", async () => {
    if (stopped() || live.act < 10 || state.vote) return;
    const msg = await general.send({ content: voteContent(live.votes), components: [voteRow(live.votes)], allowedMentions: NO_PINGS });
    state.vote = { messageId: msg.id };
    saveState(state);
    console.log(new Date().toISOString(), "usb vote opened in #general");
  });

  client.on("interactionCreate", async (i) => {
    try {
      if (!i.isButton() || !i.customId.startsWith("usb:")) return;
      await i.deferReply({ flags: MessageFlags.Ephemeral });  // the bridge can take a moment; Discord wants an answer in 3 s
      const r = await castVote({ bridge: BRIDGE, userId: i.user.id, choice: i.customId.slice(4), isResident: await isResident(i.user.id) });
      if (r.said) await i.editReply({ content: r.said, allowedMentions: NO_PINGS });
      else await i.deleteReply();  // the bridge didn't answer: say nothing (the bot only speaks authored lines)
      if (r.tally) {
        live.votes = r.tally;
        await i.message.edit({ content: voteContent(r.tally), components: [voteRow(r.tally)], allowedMentions: NO_PINGS });
        console.log(new Date().toISOString(), "usb vote", i.user.id, i.customId.slice(4), JSON.stringify(r.tally));
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
      if (message.guild || message.author.bot) return;
      if (!(await isResident(message.author.id))) return;
      const reply = await personaReply({
        act: live.act, stoppedNow: stopped(), text: message.content, userId: message.author.id,
        lastPersona: state.persona?.[message.author.id], limiter, bridge: BRIDGE, seed: message.id,
      });
      if (reply) await message.channel.send({ content: reply, allowedMentions: NO_PINGS });
    } catch (err) {
      console.error(new Date().toISOString(), "dm reply failed:", err?.message || err);
    }
  });

  // The stop: one last line from Theo, then silence forever.
  const watch = every(5000, "stop-watch", async () => {
    if (!stopped()) return;
    clearInterval(watch);
    try {
      if (state.vote) {
        const msg = await general.messages.fetch(state.vote.messageId);
        await msg.edit({ content: voteContent(live.votes, true), components: [], allowedMentions: NO_PINGS });
      }
    } catch (err) {
      console.error("closing the vote failed:", err?.message || err);
    }
    try {
      await once("bridge_cut", () => send(general, CANON.bridge_cut.text, CANON.bridge_cut.voice));
    } catch (err) {
      console.error("bridge_cut failed:", err?.message || err);
    }
    state.stopped = true;
    saveState(state);
    console.log("stopped: the bridge is cut. Exiting.");
    setTimeout(() => process.exit(0), 3000);
  });
});

client.login(ENV.DISCORD_TOKEN);
