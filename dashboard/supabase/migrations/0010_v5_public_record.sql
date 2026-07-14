-- The Observance V5 public projections.
-- Every view is intentionally narrower than the service-role tables: no accepted answers, no hidden
-- prerequisites, no player identities, and no future node titles. Public rows appear only when their
-- case is unlocked or their evidence is already earned.

begin;

drop view if exists public.v_record;
create view public.v_record
with (security_invoker = false) as
with state as (
  select current_act, phase_key, coalesce(flags, '{}'::jsonb) flags
    from public.arc_state where id = 1
), totals as (
  select count(*)::int total_nodes
    from public.investigation_nodes where active and required
), done as (
  select count(*)::int nodes_completed
    from public.investigation_nodes n cross join state s
   where n.active and n.required
     and coalesce((s.flags ->> n.completion_flag)::boolean, false)
), cases as (
  select count(*)::int cases_completed
    from public.investigations i cross join state s
   where i.active and i.required
     and coalesce((s.flags ->> i.completion_flag)::boolean, false)
), current_case as (
  select
    i.case_key,
    case
      when coalesce((s.flags ->> i.completion_flag)::boolean, false) then i.title
      else 'Docket ' || i.case_key
    end as public_title
    from public.investigations i cross join state s
   where i.active and i.required
     and (i.ordinal = 1 or coalesce((s.flags ->> i.unlock_flag)::boolean, false))
   order by i.ordinal desc limit 1
)
select
  s.current_act as movement,
  s.phase_key,
  c.case_key as current_case_key,
  c.public_title as current_case_title,
  cases.cases_completed,
  done.nodes_completed,
  totals.total_nodes,
  coalesce((s.flags ->> 'v5_case_c10_complete')::boolean, false) as closed,
  case when coalesce((s.flags ->> 'v5_case_c10_complete')::boolean, false)
       then s.flags ->> 'v5_ending_branch' end as ending_branch,
  case when coalesce((s.flags ->> 'v5_case_c10_complete')::boolean, false)
       then s.flags ->> 'v5_name_treatment' end as name_treatment,
  case when coalesce((s.flags ->> 'v5_case_c10_complete')::boolean, false)
       then s.flags ->> 'v5_wren_outcome' end as wren_outcome
from state s cross join totals cross join done cross join cases
left join current_case c on true;

comment on view public.v_record is
  'V5 spoiler-safe Record muster: coarse act/phase, current unlocked case, mandatory case/node counts, and final choices only after closure.';
grant select on public.v_record to anon, authenticated;

create or replace view public.v_case_progress
with (security_invoker = false) as
select
  i.case_key,
  i.ordinal,
  case
    when coalesce((a.flags ->> i.completion_flag)::boolean, false) then i.title
    else 'Docket ' || i.case_key
  end as title,
  case
    when coalesce((a.flags ->> i.completion_flag)::boolean, false) then i.summary
    else 'Docket open. Recover and file evidence at its named surface.'
  end as summary,
  count(n.node_key)::int as total_nodes,
  count(n.node_key) filter (
    where coalesce((a.flags ->> n.completion_flag)::boolean, false)
  )::int as completed_nodes,
  coalesce((a.flags ->> i.completion_flag)::boolean, false) as complete
from public.investigations i
cross join public.arc_state a
join public.investigation_nodes n on n.case_key = i.case_key and n.active and n.required
where a.id = 1 and i.active and i.required
  and (i.ordinal = 1 or coalesce((a.flags ->> i.unlock_flag)::boolean, false))
group by i.case_key,i.ordinal,i.title,i.summary,i.completion_flag,a.flags
order by i.ordinal;

comment on view public.v_case_progress is
  'Only unlocked mandatory cases, with coarse completion counts. Incomplete cases expose a generic docket label; canonical titles and summaries appear only after completion.';
grant select on public.v_case_progress to anon, authenticated;

drop view if exists public.v_archive;
create view public.v_archive
with (security_invoker = false) as
select
  n.node_key,
  n.case_key,
  i.ordinal as case_ordinal,
  i.title as case_title,
  n.ordinal as node_ordinal,
  n.title,
  n.modality,
  n.reward,
  coalesce(
    (select min(r.received_at) from public.evidence_receipts r where r.node_key = n.node_key),
    a.updated_at
  ) as recovered_at
from public.investigation_nodes n
join public.investigations i on i.case_key = n.case_key
cross join public.arc_state a
where a.id = 1 and n.active and n.required and i.active and i.required
  and coalesce((a.flags ->> n.completion_flag)::boolean, false)
order by i.ordinal,n.ordinal;

comment on view public.v_archive is
  'V5 recovered evidence only. A node title and payoff leave the database only after its durable completion flag is true.';
grant select on public.v_archive to anon, authenticated;

create or replace view public.v_required_media_delivery
with (security_invoker = false) as
select
  m.media_key,
  m.case_key,
  m.node_key,
  m.media_kind,
  m.title,
  m.delivery_url,
  m.filename,
  m.delivery_state
from public.required_media m
cross join public.arc_state a
where a.id = 1 and m.active
  and not exists (
    select 1 from unnest(m.prerequisite_flags) as required_flag
     where not coalesce((a.flags ->> required_flag)::boolean, false)
  );

comment on view public.v_required_media_delivery is
  'Prerequisite-gated delivery routes for required V5 media. Expected payloads and checksums remain service-role only.';
grant select on public.v_required_media_delivery to anon, authenticated;

commit;
