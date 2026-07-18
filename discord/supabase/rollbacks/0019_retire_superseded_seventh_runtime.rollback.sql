-- Exact rollback for disposable/private validation. Production remains unapplied.
begin;

update public.puzzles
set active = true
where puzzle_key in (
  'seventh-shrine',
  'seventh-unwriting',
  'seventh-cause',
  'seventh-choice',
  'seventh-name'
);

commit;
