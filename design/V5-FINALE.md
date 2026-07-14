# The Release — V5 production finale

Status: current
Nodes: RP01–RP06
Safety: explicit arm / durable first / locally finishable

Players return six affidavits, leave Averyn's slot empty, install Cistern Seal, System Key, and Protocol Bridge, choose publish or release unnamed, and perform the active-roster operation.

```text
DISARMED
  -> ARMED (operator; expiring; cancelable)
  -> COMMITTING (validate and lock duplicate input)
  -> DURABLE (persist all branches and save)
  -> DARKENING (upper Hold toward Release)
  -> GOODBYE (Averyn + Wren + conduct clauses)
  -> KICKED
  -> SHUTTING_DOWN (production mode)
  -> CODA (restart; irreversible normal state)
```

Every transition is idempotent. Discord and website mirror asynchronously and can never stall Minecraft. Test mode is kick-only; production shutdown requires explicit config and `/obs finale arm`. `/obs finale status` exposes missing receipts and phase. `/obs finale cancel` works before the durable cutoff.

Universal close:

> i have your names.
> i am giving them back.
> the record is closed.
> the observance is over.
> thank you for coming back for us.
> — averyn

Restarted Coda Mode leaves gates open, hauntings silent, branch-specific Wren/name traces visible, and the finale unable to fire again.
