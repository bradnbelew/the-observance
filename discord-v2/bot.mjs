// Copperline Recovery: the Discord bridge Theo set up in 2026 (Morrow v2).
//
// It runs next to the Minecraft server (it reads the server log and the mrrw plugin's data folder),
// relays Mossfield chat into #bridge, sends the Act 7 messages, and goes silent after the stop.
// It only ever speaks authored lines from canon.json, or quotes a player's own in-game line verbatim.
//
//   node bot.mjs            run (needs .env, see README.md)
//   node bot.mjs --dry-run  print what it would do for every act, no Discord connection
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const HERE = path.dirname(fileURLToPath(import.meta.url));
const CANON = JSON.parse(fs.readFileSync(path.join(HERE, "canon.json"), "utf8"));
const DRY = process.argv.includes("--dry-run");

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
  try { return JSON.parse(fs.readFileSync(STATE_FILE, "utf8")); } catch { return { done: [] }; }
}
function saveState(s) { fs.writeFileSync(STATE_FILE, JSON.stringify(s, null, 1)); }

function voiceFile(id) {
  if (!id) return null;
  for (const ext of ["ogg", "mp3", "m4a", "wav"]) {
    const f = path.join(VOICE_DIR, `${id}.${ext}`);
    if (fs.existsSync(f)) return f;
  }
  return null;
}

// ---------------------------------------------------------------- the plan: what happens when
/** Act 7 messages, spread over the act. Each resident gets a different mix; Theo's real ones are among them. */
export function act7Plan(residentNames) {
  const plan = [];
  const quotes = playerLines();
  residentNames.forEach((name, i) => {
    const morrow = CANON.morrow_dms.filter((_, k) => (k + i) % 2 === 0).slice(0, 2);
    morrow.forEach((dm, k) => plan.push({ to: name, at: 2 + k * 9 + i, kind: "morrow", text: `${dm.as}: ${dm.text}` }));
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
  console.log("act 1+ #bridge relays chat lines and [Recovery] lines from", MC_LOG);
  console.log("act 7  DM plan (hours after the act starts):");
  for (const m of act7Plan(names)) console.log(`   +${String(m.at).padStart(2)}h  to ${m.to.padEnd(12)} [${m.kind}] ${m.text}${m.voice ? `  (+ voice ${m.voice}: ${voiceFile(m.voice) ? "found" : "not recorded yet"})` : ""}`);
  console.log("act 9  DM to every resident:", CANON.fake_theo.text, `(voice ${CANON.fake_theo.voice}: ${voiceFile(CANON.fake_theo.voice) ? "found" : "not made yet"})`);
  console.log("stop   #general:", CANON.bridge_cut.text, `(voice ${CANON.bridge_cut.voice}: ${voiceFile(CANON.bridge_cut.voice) ? "found" : "not recorded yet"})`);
  console.log("stop   then: no relay, no messages, ever");
  console.log("current plugin act:", readAct(), "| stopped:", stopped());
  process.exit(0);
}

// ---------------------------------------------------------------- live
const { Client, GatewayIntentBits, Partials, AttachmentBuilder } = await import("discord.js");
for (const k of ["DISCORD_TOKEN", "GUILD_ID", "GENERAL_CHANNEL_ID", "BRIDGE_CHANNEL_ID", "RESIDENT_ROLE_ID"]) {
  if (!ENV[k]) { console.error(`missing ${k} in discord-v2/.env (see README.md)`); process.exit(1); }
}
const client = new Client({ intents: [GatewayIntentBits.Guilds, GatewayIntentBits.GuildMembers], partials: [Partials.Channel] });
const state = loadState();
const once = async (flag, fn) => {
  if (state.done.includes(flag)) return;
  await fn();
  state.done.push(flag);
  saveState(state);
};

async function send(channel, text, voice) {
  const file = voiceFile(voice);
  await channel.send({ content: text, files: file ? [new AttachmentBuilder(file, { name: path.basename(file) })] : [] });
}

client.once("clientReady", async () => {
  console.log("mrrw bridge online as", client.user.tag);
  const guild = await client.guilds.fetch(ENV.GUILD_ID);
  const general = await guild.channels.fetch(ENV.GENERAL_CHANNEL_ID);
  const bridge = await guild.channels.fetch(ENV.BRIDGE_CHANNEL_ID);

  await once("welcome", async () => {
    const msg = await general.send(CANON.welcome);
    await msg.pin().catch(() => {});
  });

  // Relay: tail the server log for chat and Morrow's lines.
  let offset = fs.existsSync(MC_LOG) ? fs.statSync(MC_LOG).size : 0;
  setInterval(async () => {
    if (stopped() || !fs.existsSync(MC_LOG)) return;
    const size = fs.statSync(MC_LOG).size;
    if (size < offset) offset = 0;  // rotated
    if (size === offset) return;
    const fd = fs.openSync(MC_LOG, "r");
    const buf = Buffer.alloc(size - offset);
    fs.readSync(fd, buf, 0, buf.length, offset);
    fs.closeSync(fd);
    offset = size;
    for (const line of buf.toString("utf8").split(/\r?\n/)) {
      const chat = line.match(/\[Async Chat Thread[^\]]*\/INFO\]: <([^>]+)> (.*)$/) || line.match(/INFO\]: <([^>]+)> (.*)$/);
      const morrow = line.match(/INFO\]: \[Recovery\] (.*)$/);
      if (chat) await bridge.send(`**${chat[1]}**: ${chat[2]}`).catch(() => {});
      else if (morrow) await bridge.send(`*[Recovery] ${morrow[1]}*`).catch(() => {});
    }
  }, 4000);

  // Act 7: the messages, scheduled from the moment the bot first sees Act 7.
  setInterval(async () => {
    if (stopped()) return;
    const act = readAct();
    if (act >= 7 && !state.act7Start) { state.act7Start = Date.now(); saveState(state); }
    if (act < 7 || act > 9 || !state.act7Start) return;
    const members = (await guild.members.fetch()).filter((m) => m.roles.cache.has(ENV.RESIDENT_ROLE_ID) && !m.user.bot);
    const names = members.map((m) => m.displayName);
    const hours = (Date.now() - state.act7Start) / 3_600_000 * Number(ENV.TIME_SCALE || 1);
    for (const m of act7Plan(names)) {
      if (hours < m.at) continue;
      await once(`dm:${m.to}:${m.at}:${m.kind}`, async () => {
        const member = members.find((x) => x.displayName === m.to);
        if (!member) return;
        const dm = await member.createDM();
        await send(dm, m.text, m.voice);
      });
    }
  }, 60_000);

  // Act 9: Morrow's spliced "Theo" voice note (phone-band, says "mate"): the authentication test.
  setInterval(async () => {
    if (stopped() || readAct() < 9) return;
    await once("act9:fake_theo", async () => {
      const members = (await guild.members.fetch()).filter((m) => m.roles.cache.has(ENV.RESIDENT_ROLE_ID) && !m.user.bot);
      for (const m of members.values()) await send(await m.createDM(), CANON.fake_theo.text, CANON.fake_theo.voice);
    });
  }, 60_000);

  // The stop: one last line from Theo, then silence forever.
  const watch = setInterval(async () => {
    if (!stopped()) return;
    clearInterval(watch);
    await once("bridge_cut", () => send(general, CANON.bridge_cut.text, CANON.bridge_cut.voice));
    console.log("stopped: the bridge is cut. Exiting.");
    setTimeout(() => process.exit(0), 3000);
  }, 5000);
});

client.login(ENV.DISCORD_TOKEN);
