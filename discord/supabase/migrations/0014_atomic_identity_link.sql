-- The Observance — compatibility retirement for the former claim-first identity RPC
-- 0014_atomic_identity_link.sql
--
-- An earlier working-tree draft introduced a three-argument claim function that knew the shared
-- Copperline callback but had no per-player Minecraft proof. It must never be published or granted,
-- even transiently. 0015 creates the only production identity flow with a one-time hashed proof.

begin;

drop function if exists public.observance_claim_identity_handoff(text,text,text);

commit;
