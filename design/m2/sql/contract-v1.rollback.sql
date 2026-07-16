-- M2 NON-DESTRUCTIVE ROLLBACK PROPOSAL. Retain every receipt and new table.
begin;
update public.predicate_authority_versions set status = 'rolled_back'
where raw_sha256 = '16de527496a6c4e3ae0fc093db07b74754be55193059f1c8d3fe9ab0c29a595a';
update public.predicate_authority_versions set status = 'active', activated_at = now()
where raw_sha256 = '37020e754a8048d96e853cc7711f94656b4e66bc183783b9f903947bab585a9b';
update public.settings set
  value = to_jsonb('37020e754a8048d96e853cc7711f94656b4e66bc183783b9f903947bab585a9b'::text),
  updated_at = now()
where key = 'v5_physical_authority_sha256';
update public.campaign_manifest_versions set status = 'rolled_back'
where manifest_version = '2.0.0-m2' and status in ('active','staged');
commit;
