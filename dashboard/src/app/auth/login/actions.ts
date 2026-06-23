"use server";

import { headers } from "next/headers";
import { createClient } from "@/lib/supabase/server";

export type LoginState = {
  status: "idle" | "sent" | "error";
  message?: string;
};

/**
 * Server action: send a Supabase magic-link (OTP) email.
 *
 * The link routes back through /auth/callback to exchange the code for a
 * session. Authorization (admin vs not) is decided later by isAdmin() on the
 * Author pages — this only establishes a session.
 */
export async function signInWithMagicLink(
  _prev: LoginState,
  formData: FormData,
): Promise<LoginState> {
  const email = String(formData.get("email") ?? "").trim();

  if (!email || !email.includes("@")) {
    return { status: "error", message: "Enter a valid email address." };
  }

  const origin = (await headers()).get("origin") ?? "";
  const supabase = await createClient();

  const { error } = await supabase.auth.signInWithOtp({
    email,
    options: {
      emailRedirectTo: `${origin}/auth/callback`,
    },
  });

  if (error) {
    return { status: "error", message: error.message };
  }

  return {
    status: "sent",
    message: "Check your email for the sign-in link.",
  };
}
