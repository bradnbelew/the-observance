import { safeDiscordInvite } from "./discord-relay";

function check(label: string, condition: boolean): void {
  if (!condition) throw new Error(`discord relay selftest failed: ${label}`);
}

check("discord.gg accepted", safeDiscordInvite("https://discord.gg/example") !== null);
check("discord invite accepted", safeDiscordInvite("https://discord.com/invite/example") !== null);
check("arbitrary redirect rejected", safeDiscordInvite("https://example.com/discord.gg/example") === null);
check("non-https rejected", safeDiscordInvite("http://discord.gg/example") === null);

console.log("discord relay selftest: OK");
