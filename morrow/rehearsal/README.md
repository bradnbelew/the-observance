# Morrow disposable rehearsal

This directory retains the fail-closed P0 vertical-slice rehearsal. It is local-only, create-only,
and does not enable the shipped Morrow feature gates.

Run the deterministic authority lane from repository root:

```text
python tools/run_morrow_p0_rehearsal.py
python tools/check_morrow_rehearsal.py
```

The runner binds every receipt to source commit `fa1b80b84959dc62012c209c4749c6e31abde8ec`,
release `morrow.rehearsal.fa1b80b.p0-10.v1`, the cohort identities, campaign identity, and the
SHA-256 set of all exercised authorities. It refuses non-loopback network access. Regeneration is
byte-for-byte deterministic at the retained timestamp.

The `launch-matrix.json` receipt is authoritative about proof limits. A headless contract result is
not a substitute for an actual Paper boot or a graphical client observation. The Paper lane now has
an actual runtime receipt. A bounded graphical 1.21.11 client has now proven Room 04 safe-entry recovery
without suffocation, but Windows visual capture remains unavailable. The six complete graphical lanes
remain required and use the create-only protocol
in `CLIENT-REHEARSAL.md`; its validator refuses screenshots or prose without matching journal, world,
inventory, cleanup, and complete server-lifecycle evidence.
