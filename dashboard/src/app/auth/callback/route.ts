import { NextResponse } from "next/server";
import { createClient } from "@/lib/supabase/server";

export async function GET(request: Request) {
  const url = new URL(request.url);
  const code = url.searchParams.get("code");
  const requested = url.searchParams.get("next") ?? "/author";
  const next = requested.startsWith("/") && !requested.startsWith("//") ? requested : "/author";
  if (code) {
    const supabase = await createClient();
    const { error } = await supabase.auth.exchangeCodeForSession(code);
    if (!error) return NextResponse.redirect(new URL(next, url.origin));
  }
  const failure = next.startsWith('/support/')
    ? `/support/account?error=link&next=${encodeURIComponent(next)}`
    : '/author/login?error=link';
  return NextResponse.redirect(new URL(failure, url.origin));
}
