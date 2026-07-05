import { NextResponse, type NextRequest } from "next/server";
import { resolveInscription, type RecordOutcome } from "@/lib/record/resolve";

/**
 * POST /record/terminal/inscribe — the write-answers-in endpoint (INTEGRATION Layer 5: "write answers
 * INTO it"). A player inscribes { name, answer }; this SERVER route handler normalizes the answer with
 * the exact oracle algorithm, matches it against the OPEN puzzles, and records the solve — the same
 * closed loop the in-world AnswerSignListener and Discord #the-record scan run.
 *
 * SECURITY (the whole point). All DB access is server-side via the service-role oracle client, fenced
 * behind `server-only` in the resolver. The browser sends { name, answer } and receives ONLY the
 * neutral outcome kind — never which puzzle matched, never an answer, never a closeness tell, never
 * whether the name is a real keeper. The service-role key never leaves this process.
 *
 * The response is uniform and quiet: every non-'kept' outcome is a calm non-answer in the cold
 * register. Wrong, already-kept, rate-limited, and unknown-keeper are all deliberately
 * indistinguishable to anyone probing the endpoint.
 */

export const runtime = "nodejs"; // service-role client + server-only; never the edge with secrets in a bundle.
export const dynamic = "force-dynamic"; // an inscription is a write; never cached.

interface Body {
  name?: unknown;
  answer?: unknown;
}

export async function POST(request: NextRequest): Promise<NextResponse> {
  let body: Body;
  try {
    body = (await request.json()) as Body;
  } catch {
    // Malformed body → the same neutral non-answer. Never a 400 that tells a prober they hit the API.
    return NextResponse.json({ outcome: "unresolved" satisfies RecordOutcome }, { status: 200 });
  }

  const name = typeof body.name === "string" ? body.name : "";
  const answer = typeof body.answer === "string" ? body.answer : "";

  // Best-effort caller IP, ONLY to throttle unresolved-name submissions (see resolve.ts). Never a real
  // keeper identifier, never stored raw. x-forwarded-for's first hop is the original client on Vercel;
  // absent behind a different proxy, this is simply null and that submission just isn't IP-throttled.
  const clientIp =
    request.headers.get("x-forwarded-for")?.split(",")[0]?.trim() || request.headers.get("x-real-ip") || null;

  const outcome = await resolveInscription(name, answer, clientIp);

  // Cold-register lines the UI may show; the endpoint returns the kind + a line, nothing spoiler.
  const line = LINE[outcome];
  return NextResponse.json({ outcome, line }, { status: 200 });
}

/** The cold keeper register per neutral outcome. Lowercase, declarative, no warmth, no "you"-address,
 *  no closeness. 'kept' is the only acknowledgement of advance; everything else is a quiet non-answer. */
const LINE: Record<RecordOutcome, string> = {
  kept: "the mark is kept. the count holds.",
  already: "this is already kept. the record does not keep it twice.",
  unresolved: "the record holds nothing under that.",
  withheld: "the record is not reading. wait, and inscribe again.",
  empty: "nothing was inscribed.",
};
