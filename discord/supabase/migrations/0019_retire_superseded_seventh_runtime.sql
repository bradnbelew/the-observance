-- Reversible private/staging proposal only. The current spine preserves a category error:
-- six Keepers, seven practical Ways, and Averyn as a human registrar/witness. These five older
-- puzzles hard-coded a literal Seventh category and a name-as-release trigger, so they must never
-- remain active beside the P11 identity and physical RP06 release runtime.
begin;

update public.puzzles
set active = false
where puzzle_key in (
  'seventh-shrine',
  'seventh-unwriting',
  'seventh-cause',
  'seventh-choice',
  'seventh-name'
);

commit;
