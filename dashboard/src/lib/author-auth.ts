import "server-only";

import { redirect } from "next/navigation";
import { createClient } from "@/lib/supabase/server";

function allowedEmails(): Set<string> {
  return new Set(
    (process.env.ADMIN_EMAILS ?? "")
      .split(",")
      .map((email) => email.trim().toLowerCase())
      .filter(Boolean),
  );
}

export function isAuthorEmail(email: string | null | undefined): boolean {
  if (!email) return false;
  return allowedEmails().has(email.trim().toLowerCase());
}

export async function getAuthor() {
  const supabase = await createClient();
  const { data, error } = await supabase.auth.getUser();
  if (error || !data.user || !isAuthorEmail(data.user.email)) return null;
  return data.user;
}

export async function requireAuthor() {
  const user = await getAuthor();
  if (!user) redirect("/author/login");
  return user;
}

export async function assertAuthor(): Promise<void> {
  const user = await getAuthor();
  if (!user) throw new Error("Author authorization required.");
}
