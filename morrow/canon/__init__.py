"""Morrow canon generator.

Single source of truth for every authored 2011-2026 record in the Morrow ARG.
Every log, post, ticket, cipher artifact and checksum is generated from the
modules in this package so the records can never contradict each other.

Run `python -m morrow.canon.build` from the repository root to regenerate
`morrow/canon/out/` and the website data copy, and `python -m morrow.canon.verify`
to prove every cipher round-trips.
"""
