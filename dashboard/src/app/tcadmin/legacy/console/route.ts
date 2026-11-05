import { NextResponse } from "next/server";
import { cookies } from "next/headers";
import { createHash } from "node:crypto";
import { bundle, currentAct } from "@/lib/morrow/canon";
import { deskFast, seal, unseal } from "@/lib/morrow/campaign";
import { remember, ticketLink, type Ticket } from "@/lib/morrow/desk";
import { morrowStatus, ping, stopCode, STOP_CHOICES } from "@/lib/morrow/slp";
import { bridgeState, postHandIn } from "@/lib/morrow/bridge";
import { after } from "next/server";

export const dynamic = "force-dynamic";

const md5 = (s: string) => createHash("md5").update(s).digest("hex");

/**
 * The stop console (Act 10). Needs Iona's command (HALTMRRW, from the annex wall) and her key
 * (from the buried chest). It then asks the node itself, over the server-list ping, whether
 * Morrow reports "maint=on sessions=0" (the waxed MAINT sign and lowpath's session ended).
 * Only then does it issue the stop code that players enter in game.
 *
 * What goes on the USB stick is the group's call: with the bridge on, the console only accepts the
 * winner of the #general vote (the plugin counts it); without a bridge, the panel's own choice.
 */
export async function POST(request: Request) {
  const back = (r: object) => NextResponse.redirect(new URL(`/tcadmin/legacy?r=${seal(r)}`, request.url), 303);
  const b = await bundle();
  const act = await currentAct();
  if (act < 6 || !unseal((await cookies()).get("tcadmin")?.value)) {
    return NextResponse.redirect(new URL("/tcadmin/legacy", request.url), 303);
  }
  const form = await request.formData();
  const command = String(form.get("command") ?? "").trim().toUpperCase();
  const key = String(form.get("key") ?? "").trim();
  // Acts 6-9: the console takes Iona's key (Act 7's hand-in) and refuses the stop: a session is still open
  if (act < 10 || !b.console) {
    if (!b.console_key_md5) return back({ error: "console: read only (recovery job running)" });
    // the plugin applies the hand-in at Act 7 only; an earlier correct key must read as rejected, or it is "accepted" and lost
    if (act < 7 || md5(key) !== b.console_key_md5) return back({ error: key ? "key rejected (watchdog still armed)" : "key required" });
    console.log(`MORROW_CONSOLE key accepted at act ${act}`);
    after(() => postHandIn("console_key", "iona's key"));
    return back({ status: "key accepted. stop refused: maint=off sessions=1 (lowpath)" });
  }
  let choice = String(form.get("choice") ?? "") as keyof typeof STOP_CHOICES;
  if (md5(command) !== b.console.cmd_md5) return back({ error: `${command || "(blank)"}: unknown command` });
  if (md5(key) !== b.console.key_md5) return back({ error: "key rejected (watchdog still armed)" });
  const bridge = await bridgeState();
  if (bridge) {
    if (!bridge.usb.winner) {
      console.log(`MORROW_CONSOLE refused: no USB winner yet (${JSON.stringify(bridge.usb.votes)})`);
      return back({ error: b.usb_vote?.undecided ?? "The group hasn't decided.  Ask in #general." });
    }
    choice = bridge.usb.winner;
  }
  if (!(choice in STOP_CHOICES)) return back({ error: "choose what to retain first" });

  const reply = await ping();
  const status = morrowStatus(reply);
  const shown = reply === null ? "unreachable (is MORROW_MC_HOST set?)" : status || "(no heartbeat)";
  console.log(`MORROW_CONSOLE node=${shown}`);
  if (status !== b.ready_status) {
    return back({ error: reply !== null && !status ? "nothing to stop: the process is silent" : "stop refused: node is not ready", status: shown });
  }
  const now = new Date();
  const t: Ticket = {
    tid: 400000 + (now.getTime() % 100000), at: now.toISOString(), name: "rookery", subject: "what goes on the stick",
    message: `Retain: ${b.usb_choices?.[choice]?.line ?? choice}`, kind: "choice", ref: choice,
    replyAt: (deskFast() ? now : new Date(now.getTime() + 10 * 60_000)).toISOString(),
  };
  await remember(t);
  console.log(`MORROW_CONSOLE stop issued choice=${choice}`);
  return back({ code: stopCode(choice, now), ticket: ticketLink(t), status });
}
