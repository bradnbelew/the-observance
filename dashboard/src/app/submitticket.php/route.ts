import { after, NextResponse } from "next/server";
import { bundle } from "@/lib/morrow/canon";
import { deskEvent, newTicket, remember, ticketLink } from "@/lib/morrow/desk";
import { postDeskEvent } from "@/lib/morrow/bridge";

export const dynamic = "force-dynamic";

export async function POST(request: Request) {
  const form = await request.formData();
  const b = await bundle();
  const t = newTicket(b, String(form.get("name") ?? ""), String(form.get("subject") ?? ""), String(form.get("message") ?? ""));
  await remember(t);
  console.log(`MORROW_DESK ticket=${t.tid} kind=${t.kind} ref=${t.ref ?? "-"} act=${b.act}`);
  // tell the plugin (it advances the act and queues Theo's big replies); after the response, never blocking it
  const ev = deskEvent(b, t);
  if (ev) after(() => postDeskEvent(ev));
  return NextResponse.redirect(new URL(ticketLink(t), request.url), 303);
}

