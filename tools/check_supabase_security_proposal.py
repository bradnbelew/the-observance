from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SQL = ROOT / "design" / "m2" / "sql"
UP = (SQL / "production-security-hardening-v2.up.sql").read_text(encoding="utf-8")
ROLLBACK = (SQL / "production-security-hardening-v2.rollback.sql").read_text(encoding="utf-8")
FORWARD = (SQL / "production-security-hardening-v2.forward.sql").read_text(encoding="utf-8")
ASSERT = (SQL / "tests" / "production-security-hardening-v2.assert.sql").read_text(encoding="utf-8")

views = {
    "v_archive", "v_required_media_delivery", "v_case_progress", "v_heatmap",
    "v_compliance_counts", "v_health", "v_record",
}
for view in views:
    if f"alter view public.{view} set (security_invoker = true)" not in UP:
        raise SystemExit(f"missing SECURITY INVOKER remediation for {view}")
    if f"alter view public.{view} set (security_invoker = false)" not in ROLLBACK:
        raise SystemExit(f"missing exact view rollback for {view}")

required_up = [
    "revoke all privileges on all tables in schema public from anon, authenticated",
    "grant select, insert, update, delete on all tables in schema public to service_role",
    "alter table public.dossiers add constraint dossiers_pkey primary key (id)",
    "security_hardening_v2_grants",
    "security_hardening_v2_indexes",
]
for token in required_up:
    if token not in UP:
        raise SystemExit(f"UP proposal missing: {token}")
for token in ["drop column id", "drop schema observance_migration", "where not existed_before"]:
    if token not in ROLLBACK:
        raise SystemExit(f"rollback proposal missing: {token}")
for token in required_up + ["alter view public.v_archive set (security_invoker = true)", "commit;"]:
    if token not in FORWARD:
        raise SystemExit(f"forward recovery is not a complete executable reapplication: {token}")
for token in ["unindexed public foreign key", "unexpected browser Data API grant", "dossiers has no primary key"]:
    if token not in ASSERT:
        raise SystemExit(f"assertion file missing: {token}")

print("Supabase security proposal check passed: views, grants, PK, FK indexes, rollback snapshot, executable forward")
