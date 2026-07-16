-- Disposable local baseline only. It supplies the two pre-M2 production-owned
-- relations referenced by the reviewed proposal without copying production data.
create table public.players (
  id uuid primary key
);

create table public.settings (
  key text primary key,
  value jsonb not null,
  updated_at timestamptz not null default now()
);

insert into public.settings (key, value)
values (
  'v5_physical_authority_sha256',
  to_jsonb('37020e754a8048d96e853cc7711f94656b4e66bc183783b9f903947bab585a9b'::text)
);
