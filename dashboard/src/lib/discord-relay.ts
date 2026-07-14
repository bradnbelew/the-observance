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
