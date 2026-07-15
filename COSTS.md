# The Observance V5 operating-cost notes

Status: current. Prices are intentionally not pinned here because hosting plans change; confirm them
with each provider before launch.

The V5 campaign is deterministic and does **not** require an Anthropic, OpenAI, transcription, or
other metered AI API during play. Answers, hints, NPC dialogue, books, media routing, state changes,
and the finale are authored in the repository. Do not add an AI key to production merely to run the
ARG.

The production services are:

- a Paper 1.21.11 server with enough memory and disk for the existing world plus snapshots;
- Supabase for durable campaign state;
- a persistent Railway worker for Discord, plus its recovery cron;
- Vercel for the Copperline/Record website;
- direct HTTPS hosting for the resource-pack ZIP;
- Discord and the existing media hosts.

Before choosing paid tiers, verify that free/sleeping limits meet the response-time requirements in
`design/V5-PRODUCTION-LAUNCH-RUNBOOK.md`. The Minecraft finale is local and does not wait for a paid
external request, but website/Discord state should still be monitored and reconciled.

Budget separately for backups. Keep at least one pre-V5 world snapshot, one pre-launch database
export, and one Coda snapshot. Never trade away recovery copies to reduce storage cost.
