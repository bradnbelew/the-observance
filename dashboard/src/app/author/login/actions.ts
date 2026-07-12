"use server";

import { headers } from "next/headers";
import { redirect } from "next/navigation";
import { createClient } from "@/lib/supabase/server";
import { isAuthorEmail } from "@/lib/author-auth";

export type LoginState = { message: string; kind: "idle" | "sent" | "error" };

export async function requestAuthorLink(
  _previous: LoginState,
  formData: FormData,
): Promise<LoginState> {
  const email = String(formData.get("email") ?? "").trim().toLowerCase();
  if (!isAuthorEmail(email)) {
    return { kind: "sent", message: "If that hand is recognized, a sign-in link has been sent." };
  }

  const headerStore = await headers();
  const origin = headerStore.get("origin") ?? `https://${headerStore.get("host")}`;
  const supabase = await createClient();
  const { error } = await supabase.auth.signInWithOtp({
    email,
    options: { emailRedirectTo: `${origin}/auth/callback?next=/author` },
  });

  if (error) return { kind: "error", message: "The control channel did not answer. Try again." };
  return { kind: "sent", message: "A one-time sign-in link has been sent." };
}

export async function signOutAuthor() {
  const supabase = await createClient();
  await supabase.auth.signOut();
  redirect("/author/login");
}
