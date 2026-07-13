export const RELAY_TICKET_ID = "1842";
export const RELAY_CALLBACK = "9137";

export function normalizeRelayCallback(value: string | null | undefined): string {
  return (value ?? "").normalize("NFKC").replace(/[^0-9]/g, "");
}

export function relayCallbackMatches(value: string | null | undefined): boolean {
  return normalizeRelayCallback(value) === RELAY_CALLBACK;
}

/** The environment variable is operator-controlled, but validate it anyway so a bad deployment
 * cannot turn an earned in-fiction link into an arbitrary redirect. */
export function safeDiscordInvite(value: string | null | undefined): string | null {
  if (!value) return null;
  try {
    const url = new URL(value.trim());
    const validHost = url.hostname === "discord.gg" || url.hostname === "discord.com";
    const validPath = url.hostname === "discord.gg" || url.pathname.startsWith("/invite/");
    return url.protocol === "https:" && validHost && validPath ? url.toString() : null;
  } catch {
    return null;
  }
}
