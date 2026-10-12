import { NextResponse } from "next/server";

export const dynamic = "force-dynamic";

/** Never echoes or stores what was typed; always reports the panel as locked/invalid. */
export async function POST(request: Request) {
  await request.formData();
  return NextResponse.redirect(new URL("/tcadmin/legacy?tried=1", request.url), 303);
}
