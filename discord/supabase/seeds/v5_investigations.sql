-- The Observance V5 canonical campaign seed.
-- Idempotent and intentionally last: legacy rows remain as audit history but cannot open, hint,
-- autocomplete, or appear in the public archive after this seed runs.

begin;

update public.puzzles set active = false;
update public.hints set active = false;
update public.thread_cards set active = false;
update public.side_quests set active = false;
update public.required_media set active = false;
update public.investigation_nodes set active = false;
update public.investigations set active = false;

-- One-time cutover: no pending V4 beat may survive into the first V5 boot. The settings marker makes
-- this safe to reapply later; legitimate V5 queue rows are never swept on subsequent seed runs.
do $$
begin
  if not exists (select 1 from public.settings where key = 'v5_queue_retirement_complete') then
    update public.beat_queue
       set status = 'skipped', decided_at = coalesce(decided_at, now())
     where status in ('pending','approved','firing');
    insert into public.settings (key,value,updated_at)
    values ('v5_queue_retirement_complete',jsonb_build_object('completed_at',now()),now());
  end if;
end;
$$;

-- Minecraft accepts remote prerequisite truths only when these exact metadata values match its
-- packaged physical authority. A stale deployment therefore fails closed instead of opening a
-- local gate from an unrelated/older campaign row.
insert into public.settings (key,value,updated_at) values
  ('v5_campaign_version', to_jsonb('v5'::text), now()),
  ('v5_physical_authority_sha256',
   to_jsonb('85e5db1d8b72e9bcc29f53177b85d6c730564a4b2dfa1878144ed5383b736a90'::text), now())
on conflict (key) do update set value=excluded.value, updated_at=excluded.updated_at;

insert into public.investigations
  (case_key, ordinal, title, summary, phase_key, unlock_flag, completion_flag, expected_nodes, active)
values
  ('C01',1,'The Lost Server','Recover the ordinary hosting trail, bind an identity, and reach the Surface Mouth.','c01-lost-server',null,'v5_case_c01_complete',6,true),
  ('C02',2,'The Long Cold','Reconstruct why the refuge was built and how ordinary life worked underground.','c02-long-cold','v5_case_c01_complete','v5_case_c02_complete',6,true),
  ('C03',3,'Keeper Dossiers','Investigate six distinct contemporaries through audits, contradictions, and private affidavits.','c03-keeper-dossiers','v5_case_c02_complete','v5_case_c03_complete',18,true),
  ('C04',4,'Cistern Winter','Clear Nessa by proving the civic record concealed diverted supplies and counterfeit filters.','c04-cistern-winter','v5_case_c03_complete','v5_case_c04_complete',8,true),
  ('C05',5,'Break Inquest','Use the eight houses of the Unlit to prove that the Break had multiple causes.','c05-break-inquest','v5_case_c04_complete','v5_case_c05_complete',8,true),
  ('C06',6,'Restoring the Hold','Repair the refuge systems and earn safe access to the lower works.','c06-restoring-hold','v5_case_c05_complete','v5_case_c06_complete',7,true),
  ('C07',7,'ASH-13 Company','Find the prior camp, reconstruct four real investigators, and recover Ash''s locker key.','c07-ash-13-company','v5_case_c06_complete','v5_case_c07_complete',10,true),
  ('C08',8,'Wren''s Betrayal','Prove what Wren gave the Record and make a finishable moral judgment.','c08-wren-betrayal','v5_case_c07_complete','v5_case_c08_complete',5,true),
  ('C09',9,'Averyn and the Unwriting','Restore Averyn''s identity and understand the human voice constrained inside the Record.','c09-averyn-unwriting','v5_case_c08_complete','v5_case_c09_complete',8,true),
  ('C10',10,'Release Protocol','Return the six affidavits without binding a seventh, sever the Record, and close the server.','c10-release-protocol','v5_case_c09_complete','v5_case_c10_complete',6,true)
on conflict (case_key) do update set
  ordinal=excluded.ordinal, title=excluded.title, summary=excluded.summary,
  phase_key=excluded.phase_key, unlock_flag=excluded.unlock_flag,
  completion_flag=excluded.completion_flag, expected_nodes=excluded.expected_nodes,
  required=true, active=true, updated_at=now();

-- Discord/media conclusions use the shared exact-match oracle. Physical answer signs, tagged items,
-- frames, shelves, routes, and NPC choices are resolved by the Paper runtime and still receive the
-- same evidence receipts through observance_record_evidence().
insert into public.puzzles
  (puzzle_key,title,accepted_answers,outcome_type,outcome_payload,movement,active,max_attempts,answer_kind,requires_flags)
values
  ('v5-lc05-motive','C02 / founding motive',array['heat water cover','heat water and cover','heat water shelter','heat water and shelter','stable heat stable water and cover','stable heat stable water and shelter'],'main_beat','{"voice_key":"oracleMainBeat","set_flags":{"v5_lc05_motive":true},"node_key":"LC05"}'::jsonb,1,true,8,'phrase','{"v5_lc04_safety_script":true}'::jsonb),
  ('v5-ko03-crack-map','C03 / Orin certificate conclusion',array['conditional certificate load not reduced and brace failed','the certificate was conditional the heat load was not reduced and the brace failed','heat load not reduced and brace failed'],'main_beat','{"voice_key":"oracleMainBeat","set_flags":{"v5_ko03_affidavit":true},"node_key":"KO03"}'::jsonb,2,true,8,'phrase','{"v5_ko02_dials":true}'::jsonb),
  ('v5-kb01-stay-awake','C03 / Brann footage payload',array['stay awake'],'main_beat','{"voice_key":"oracleMainBeat","set_flags":{"v5_kb01_stay_awake":true},"node_key":"KB01"}'::jsonb,2,true,8,'phrase','{"v5_case_c02_complete":true}'::jsonb),
  ('v5-kb03-altered-watch','C03 / Brann timestamp conclusion',array['toma rill at bell eight nessa was inserted later','toma rill worked bell eight and nessa was inserted later','toma rill bell eight nessa inserted later'],'main_beat','{"voice_key":"oracleMainBeat","set_flags":{"v5_kb03_affidavit":true},"node_key":"KB03"}'::jsonb,2,true,8,'phrase','{"v5_kb02_toll":true}'::jsonb),
  ('v5-ki03-iss-correction','C03 / Iss corrected account',array['iss signed averyn out','iss signed averyn out of the roster','iss removed averyn from the roster'],'main_beat','{"voice_key":"oracleMainBeat","set_flags":{"v5_ki03_affidavit":true},"node_key":"KI03"}'::jsonb,2,true,8,'phrase','{"v5_ki02_keepsake":true}'::jsonb),
  ('v5-cw05-counterfeit','C04 / counterfeit invoice',array['false filters','counterfeit filter material','east market supplied false filters','east market supplied counterfeit filter material'],'main_beat','{"voice_key":"oracleMainBeat","set_flags":{"v5_cw05_counterfeit":true},"node_key":"CW05"}'::jsonb,2,true,8,'phrase','{"v5_cw04_roster":true}'::jsonb),
  ('v5-cw06-reeds','C04 / reeds footage payload',array['where the reeds fold back','the reeds fold back','reeds fold back'],'main_beat','{"voice_key":"oracleMainBeat","set_flags":{"v5_cw06_reeds":true},"node_key":"CW06"}'::jsonb,2,true,8,'phrase','{"v5_cw05_counterfeit":true}'::jsonb),
  ('v5-cw08-clear-nessa','C04 / Nessa conclusion',array['nessa followed procedure counterfeit filters were hidden','nessa followed procedure diverted counterfeit supplies caused the failure and the hearing hid evidence','nessa followed procedure counterfeit filters caused the failure and the hearing hid evidence'],'main_beat','{"voice_key":"oracleMainBeat","set_flags":{"v5_case_c04_complete":true},"node_key":"CW08"}'::jsonb,2,true,8,'phrase','{"v5_cw07_cache":true}'::jsonb),
  ('v5-bi08-break-inquest','C05 / Break synthesis',array['preexisting fracture resource neglect iss secret cut delayed edited response and record feedback','preexisting heat load diverted resources iss cut falsified timing delayed response and record feedback','the break combined preexisting failure diverted resources iss cut falsified timing delayed response and record feedback','preexisting fracture resource neglect hidden cut delayed edited response record feedback'],'main_beat','{"voice_key":"oracleMainBeat","set_flags":{"v5_case_c05_complete":true},"node_key":"BI08"}'::jsonb,3,true,8,'phrase','{"v5_bi07_threshold":true}'::jsonb),
  ('v5-a01-camp-ash','C07 / Camp Ash bearing contradiction',array['the map and supply entry agree wrens distance is false','the map and supply entry agree use wren only to identify which distance is false','the map and supply route agree wren lied about the distance','follow the map and supply entry not wrens distance'],'main_beat','{"voice_key":"oracleMainBeat","set_flags":{"v5_a01_location":true},"node_key":"A01"}'::jsonb,4,true,8,'phrase','{"v5_case_c06_complete":true}'::jsonb),
  ('v5-a08-ash-13','C07 / Ash footage payload',array['ash 13','ash thirteen'],'main_beat','{"voice_key":"oracleMainBeat","set_flags":{"v5_a08_ash13":true},"node_key":"A08"}'::jsonb,4,true,8,'code','{"v5_a07_clip1":true}'::jsonb),
  ('v5-a10-sabotage','C07 / prior-company conclusion',array['wren leaked the plan because he feared record closure would erase him','wren leaked the plan because he feared the record would erase him','wren betrayed the company because he feared closing the record would erase him'],'main_beat','{"voice_key":"oracleMainBeat","set_flags":{"v5_case_c07_complete":true},"node_key":"A10"}'::jsonb,4,true,8,'phrase','{"v5_a09_spool":true}'::jsonb),
  ('v5-ar01-not-kept','C09 / spectrogram payload',array['i was not kept','was not kept'],'main_beat','{"voice_key":"oracleMainBeat","set_flags":{"v5_ar01_not_kept":true},"node_key":"AR01"}'::jsonb,5,true,8,'phrase','{"v5_case_c08_complete":true}'::jsonb),
  ('v5-ar08-averyn','C09 / restored registrar name',array['averyn'],'main_beat','{"voice_key":"oracleMainBeat","set_flags":{"v5_case_c09_complete":true},"node_key":"AR08"}'::jsonb,5,true,8,'phrase','{"v5_ar07_n":true}'::jsonb),
  ('v5-rp01-release-instruction','C10 / release footage payload',array['six return one is not kept','six return and one is not kept','six return one not kept'],'main_beat','{"voice_key":"oracleMainBeat","set_flags":{"v5_rp01_instruction":true},"node_key":"RP01"}'::jsonb,5,true,8,'phrase','{"v5_case_c09_complete":true}'::jsonb)
on conflict (puzzle_key) do update set
  title=excluded.title, accepted_answers=excluded.accepted_answers,
  outcome_type=excluded.outcome_type, outcome_payload=excluded.outcome_payload,
  movement=excluded.movement, active=true, max_attempts=excluded.max_attempts,
  answer_kind=excluded.answer_kind, requires_flags=excluded.requires_flags;

insert into public.investigation_nodes
  (node_key,case_key,ordinal,title,room_id,modality,input_surface,prerequisite_flags,completion_flag,reward,recovery,oracle_puzzle_key)
values
  ('LS01','C01',1,'Three mundane traces','Copperline and village','deduction','none',array[]::text[],'v5_ls01_traces','service docket plus uploader reference plus expired listing','reopen public traces',null),
  ('LS02','C01',2,'Service 1842 teaching rung','Copperline','cipher','website answer',array['v5_ls01_traces'],'v5_ls02_service_1842','Copperline service page','tiered A1Z26 and rune example',null),
  ('LS03','C01',3,'Ordinary Copperline trail','Copperline','web navigation','none',array['v5_ls02_service_1842'],'v5_ls03_directory_trail','support ticket plus community archive','reopen indexed pages',null),
  ('LS04','C01',4,'Rebuilt Hold archive','Copperline','cross-media','website archive input',array['v5_ls03_directory_trail'],'v5_ls04_archive_solved','host fragments plus service digits','redownload immutable archive',null),
  ('LS05','C01',5,'Callback and proof-bound identity','Discord','account handoff','Discord identity link plus in-game proof',array['v5_ls04_archive_solved'],'v5_ls05_bound','durable handoff receipt','reissue receipt from linked account',null),
  ('LS06','C01',6,'Surface Mouth triangulation','orientation','physical recovery','tagged key deposit',array['v5_ls05_bound'],'v5_case_c01_complete','Orientation Key and C01 receipt','reissue key from completion flag',null),
  ('LC01','C02',1,'Construction phase overlays','orientation','deduction','item-frame overlay',array['v5_case_c01_complete'],'v5_lc01_phases','phase ordering','reset overlay frames',null),
  ('LC02','C02',2,'Rations population and heat','orientation','forensic comparison','answer sign',array['v5_lc01_phases'],'v5_lc02_rations','correct population and heat estimate','restore source ledgers',null),
  ('LC03','C02',3,'Ordinary life records','orientation','document synthesis','none',array['v5_lc02_rations'],'v5_lc03_daily_life','school market and dwelling receipt','reopen lecterns',null),
  ('LC04','C02',4,'Practical safety inscription','orientation','traditional cipher','answer sign',array['v5_lc03_daily_life'],'v5_lc04_safety_script','decoded evacuation instruction','show taught script card',null),
  ('LC05','C02',5,'Why they dug down','orientation','deduction','Discord answer',array['v5_lc04_safety_script'],'v5_lc05_motive','founding motive receipt','tiered evidence matrix hint','v5-lc05-motive'),
  ('LC06','C02',6,'Orientation model','orientation','physical configuration','item frames and tagged lever',array['v5_lc05_motive'],'v5_case_c02_complete','Survey Seal and G1 open','reset model and reissue seal',null),
  ('KV01','C03',1,'Vaun quartermaster audit','keeper_vaun','forensic comparison','container inspection',array['v5_case_c02_complete'],'v5_kv01_audit','shortage table','restore exact container slots',null),
  ('KV02','C03',2,'Vaun returned-goods sort','keeper_vaun','physical Minecraft','tagged container sort',array['v5_kv01_audit'],'v5_kv02_sort','diverted-filter proof','return wrong deposits',null),
  ('KV03','C03',3,'Vaun private reconciliation','keeper_vaun','traditional cipher','answer sign',array['v5_kv02_sort'],'v5_kv03_affidavit','Vaun sealed affidavit','reissue affidavit from flag',null),
  ('KM01','C03',4,'Mara conflicting editions','keeper_mara','forensic comparison','lectern pages',array['v5_case_c02_complete'],'v5_km01_editions','correct manual edition','restore locked lecterns',null),
  ('KM02','C03',5,'Mara page-line-word','keeper_mara','traditional cipher','answer sign',array['v5_km01_editions'],'v5_km02_extraction','verified route sequence','tiered index hint',null),
  ('KM03','C03',6,'Mara route configuration','keeper_mara','physical Minecraft','bounded route walk',array['v5_km02_extraction'],'v5_km03_affidavit','Mara sealed affidavit','rearm route and reissue affidavit',null),
  ('KS01','C03',7,'Sella reflected bearing','keeper_sella','visual cipher','reflection alignment',array['v5_case_c02_complete'],'v5_ks01_bearing','shoreline bearing','restore overlay and water anchor',null),
  ('KS02','C03',8,'Sella shoreline overlay','keeper_sella','physical Minecraft','item-frame map overlay',array['v5_ks01_bearing'],'v5_ks02_overlay','cistern intake location','reset frames',null),
  ('KS03','C03',9,'Sella sample note','keeper_sella','traditional cipher','answer sign',array['v5_ks02_overlay'],'v5_ks03_affidavit','Sella sealed affidavit','reissue affidavit from flag',null),
  ('KO01','C03',10,'Orin low masonry marks','keeper_orin','embodied observation','crouched sightline',array['v5_case_c02_complete'],'v5_ko01_marks','survey rank order','replay private fragments',null),
  ('KO02','C03',11,'Orin heraldry dials','keeper_orin','physical Minecraft','item-frame dials',array['v5_ko01_marks'],'v5_ko02_dials','verified bearing','reset exact frame rotations',null),
  ('KO03','C03',12,'Orin crack-map contradiction','keeper_orin','forensic comparison','Discord answer',array['v5_ko02_dials'],'v5_ko03_affidavit','Orin sealed affidavit','reissue affidavit from flag','v5-ko03-crack-map'),
  ('KB01','C03',13,'Brann footage recovery','keeper_brann','cross-media','media payload answer',array['v5_case_c02_complete'],'v5_kb01_stay_awake','STAY AWAKE receipt','reveal clip 3 again','v5-kb01-stay-awake'),
  ('KB02','C03',14,'Brann visual toll rail','keeper_brann','traditional cipher','visual dial and answer sign',array['v5_kb01_stay_awake'],'v5_kb02_toll','watch substitution order','visual redundancy card',null),
  ('KB03','C03',15,'Brann altered timestamp','keeper_brann','logic deduction','Discord answer',array['v5_kb02_toll'],'v5_kb03_affidavit','Brann sealed affidavit','reissue affidavit from flag','v5-kb03-altered-watch'),
  ('KI01','C03',16,'Iss persuasive account','keeper_iss','traditional cipher','answer sign',array['v5_case_c02_complete'],'v5_ki01_vigenere','decoded warm account','keyed cipher hint',null),
  ('KI02','C03',17,'Iss keepsake record','keeper_iss','forensic comparison','tagged item inspection',array['v5_ki01_vigenere'],'v5_ki02_keepsake','physical contradiction receipt','reissue inspection item',null),
  ('KI03','C03',18,'Iss corrected acrostic','keeper_iss','traditional cipher','Discord answer',array['v5_ki02_keepsake'],'v5_ki03_affidavit','Iss sealed affidavit','reissue affidavit from flag','v5-ki03-iss-correction'),
  ('CW01','C04',1,'Nessa and first complaints','archive_cistern','document deduction','lectern and sample labels',array['v5_kv03_affidavit','v5_km03_affidavit','v5_ks03_affidavit','v5_ko03_affidavit','v5_kb03_affidavit','v5_ki03_affidavit'],'v5_cw01_complaints','complaint chronology','reopen records',null),
  ('CW02','C04',2,'Water sample sort','archive_cistern','physical Minecraft','tagged barrel sort',array['v5_cw01_complaints'],'v5_cw02_samples','intake and date sequence','return wrong samples',null),
  ('CW03','C04',3,'Static pipe and valve model','archive_cistern','physical Minecraft','item-frame valves',array['v5_cw02_samples'],'v5_cw03_valves','flow-path receipt','reset frame states',null),
  ('CW04','C04',4,'Replacement shift roster','archive_watch','logic deduction','answer sign',array['v5_cw03_valves'],'v5_cw04_roster','missing-shift identity','tiered grid hint',null),
  ('CW05','C04',5,'Counterfeit supplier invoice','archive_market','traditional cipher','Discord answer',array['v5_cw04_roster'],'v5_cw05_counterfeit','counterfeit filter proof','reopen invoice and taught grille','v5-cw05-counterfeit'),
  ('CW06','C04',6,'Reeds footage','archive_water','cross-media','media payload answer',array['v5_cw05_counterfeit'],'v5_cw06_reeds','WHERE THE REEDS FOLD BACK receipt','reveal clip 2 again','v5-cw06-reeds'),
  ('CW07','C04',7,'Original filter cache','archive_water','physical recovery','tagged cache interaction',array['v5_cw06_reeds'],'v5_cw07_cache','original filter plus receipt plus forged report','rebuild cache without duplicating progress',null),
  ('CW08','C04',8,'Clear Nessa','archive_cistern','deduction','Discord conclusion',array['v5_cw07_cache'],'v5_case_c04_complete','Cistern Seal and G2 condition','reissue seal from completion flag','v5-cw08-clear-nessa'),
  ('BI01','C05',1,'Lamp house chronology','unlit_lamp','forensic comparison','house mechanism',array['v5_case_c04_complete'],'v5_bi01_lamp','true outage chronology','reset house evidence',null),
  ('BI02','C05',2,'Cairn pressure fracture','unlit_cairn','physical observation','tagged fragments',array['v5_bi01_lamp'],'v5_bi02_cairn','pre-breach fracture proof','reissue fragments',null),
  ('BI03','C05',3,'Coop warning failure','unlit_coop','deduction','item arrangement',array['v5_bi02_cairn'],'v5_bi03_coop','bird-failure timing','restore tagged evidence',null),
  ('BI04','C05',4,'Well reflection map','unlit_well','visual overlay','reflection and frames',array['v5_bi03_coop'],'v5_bi04_well','pressure-map alignment','reset overlay',null),
  ('BI05','C05',5,'Watch house timestamp','unlit_watch','logic deduction','answer sign',array['v5_bi04_well'],'v5_bi05_watch','altered-watch proof','reopen rota',null),
  ('BI06','C05',6,'Warm house surface proof','unlit_warm','forensic comparison','tagged sample deposit',array['v5_bi05_watch'],'v5_bi06_warm','healed-surface proof','reissue sample',null),
  ('BI07','C05',7,'Threshold house inside seal','unlit_threshold','spatial deduction','bounded group walk',array['v5_bi06_warm'],'v5_bi07_threshold','multi-cause sequence','rearm route',null),
  ('BI08','C05',8,'Base mirror synthesis','unlit_base','cross-source deduction','Discord conclusion',array['v5_bi07_threshold'],'v5_case_c05_complete','Breach Plate and Deep Line open','reissue plate from completion flag','v5-bi08-break-inquest'),
  ('HS01','C06',1,'Recover filter cartridge','puzzle_works','physical recovery','tagged item claim',array['v5_case_c05_complete','v5_case_c04_complete'],'v5_hs01_filter','Filter Cartridge','reissue cartridge',null),
  ('HS02','C06',2,'Install cistern cartridge','puzzle_works','physical Minecraft','exact tagged deposit',array['v5_hs01_filter'],'v5_hs02_installed','restored water state','return wrong deposits',null),
  ('HS03','C06',3,'Restore lamp circuit','puzzle_works','physical Minecraft','item-frame lamp states',array['v5_hs02_installed'],'v5_hs03_lamps','lit service lane','reset frames without clearing solve',null),
  ('HS04','C06',4,'Calibrate pressure register','puzzle_works','physical Minecraft','chiseled-bookshelf pattern',array['v5_hs03_lamps'],'v5_hs04_pressure','pressure calibration','restore shelf slots',null),
  ('HS05','C06',5,'Set survey dials','lower_works','physical Minecraft','item-frame dials',array['v5_hs04_pressure'],'v5_hs05_dials','alignment receipt','reset frame rotations',null),
  ('HS06','C06',6,'Walk painted pressure line','lower_works','embodied sequence','bounded route walk',array['v5_hs05_dials'],'v5_hs06_passage','System Key and service passage','rearm route and reissue key',null),
  ('HS07','C06',7,'System synchronization','lower_works','composite meta','tagged key console',array['v5_hs06_passage'],'v5_case_c06_complete','Deep Access Plate and G4 open','reissue plate from completion flag',null),
  ('A01','C07',1,'Locate Camp Ash','prior_case','cross-source deduction','Discord answer',array['v5_case_c06_complete'],'v5_a01_location','camp bearing receipt','tiered source checklist','v5-a01-camp-ash'),
  ('A02','C07',2,'Four camp stations','prior_camp','forensic exploration','tagged station inspection',array['v5_a01_location'],'v5_a02_stations','mkept Ash Rook Wren roster','repair camp fixtures',null),
  ('A03','C07',3,'Supply barrel order','prior_camp','physical Minecraft','container order',array['v5_a02_stations'],'v5_a03_supplies','notebook key','restore exact slots',null),
  ('A04','C07',4,'Field notebook transposition','prior_camp','traditional cipher','answer sign',array['v5_a03_supplies'],'v5_a04_notebook','archive route fragment','tiered transposition hint',null),
  ('A05','C07',5,'Torn map Cardan overlay','prior_camp','visual cipher','item-frame overlay',array['v5_a04_notebook'],'v5_a05_overlay','archive route fragment','reset overlay',null),
  ('A06','C07',6,'Copperline archive route','prior_case','composite meta','website route input',array['v5_a05_overlay'],'v5_a06_route','gated archive access','reopen source fragments',null),
  ('A07','C07',7,'Ash footage access','Copperline','cross-media','automatic page reveal',array['v5_a06_route'],'v5_a07_clip1','clip 1 playback route','reveal page again',null),
  ('A08','C07',8,'Derive ASH-13','Copperline','cross-media','Discord answer',array['v5_a07_clip1'],'v5_a08_ash13','ASH-13 receipt','reveal clip and camp manifest','v5-a08-ash-13'),
  ('A09','C07',9,'Open Locker 13','prior_camp','physical recovery','tagged locker interaction',array['v5_a08_ash13'],'v5_a09_spool','Witness Spool','reissue spool from flag',null),
  ('A10','C07',10,'Reconstruct the sabotage','prior_case','cross-source deduction','Discord conclusion',array['v5_a09_spool'],'v5_case_c07_complete','Wren case file and G5 subcondition','tiered evidence matrix','v5-a10-sabotage'),
  ('WR01','C08',1,'Match leaked quotations','dread','deduction','answer sign',array['v5_case_c07_complete'],'v5_wr01_quotes','leak source proof','reopen quotation cards',null),
  ('WR02','C08',2,'Transmission index','dread','forensic comparison','tagged spool reader',array['v5_wr01_quotes'],'v5_wr02_index','names plans and fears ledger','reissue spool',null),
  ('WR03','C08',3,'Confront Wren','dread','dialogue investigation','NPC choices',array['v5_wr02_index'],'v5_wr03_confession','Wren confession receipt','replay stable dialogue state',null),
  ('WR04','C08',4,'Protocol Bridge proof','lower_threshold','physical recovery','blocked-route mechanism',array['v5_wr03_confession'],'v5_wr04_bridge','Protocol Bridge','reissue bridge from flag',null),
  ('WR05','C08',5,'Wren reckoning','lower_vault','collective choice','tagged group rite',array['v5_wr04_bridge'],'v5_case_c08_complete','condemn understand or free branch plus G5','operator recovery preserves chosen branch',null),
  ('AR01','C09',1,'Recovered archive spectrogram','accepting','cross-media','media payload answer',array['v5_case_c08_complete'],'v5_ar01_not_kept','I WAS NOT KEPT receipt','reveal spectrogram again','v5-ar01-not-kept'),
  ('AR02','C09',2,'Vaun letter A','accepting','traditional cipher','affidavit reader',array['v5_ar01_not_kept','v5_kv03_affidavit'],'v5_ar02_a','A fragment','reissue private fragment',null),
  ('AR03','C09',3,'Mara letter V','accepting','traditional cipher','affidavit reader',array['v5_ar02_a','v5_km03_affidavit'],'v5_ar03_v','V fragment','reissue private fragment',null),
  ('AR04','C09',4,'Sella letter E','accepting','visual cipher','affidavit reader',array['v5_ar03_v','v5_ks03_affidavit'],'v5_ar04_e','E fragment','reissue private fragment',null),
  ('AR05','C09',5,'Orin letter R','accepting','embodied cipher','affidavit reader',array['v5_ar04_e','v5_ko03_affidavit'],'v5_ar05_r','R fragment','reissue private fragment',null),
  ('AR06','C09',6,'Brann letter Y','accepting','traditional cipher','affidavit reader',array['v5_ar05_r','v5_kb03_affidavit'],'v5_ar06_y','Y fragment','reissue private fragment',null),
  ('AR07','C09',7,'Iss false M corrected to N','accepting','traditional cipher','affidavit reader',array['v5_ar06_y','v5_ki03_affidavit'],'v5_ar07_n','N fragment','reissue private fragment',null),
  ('AR08','C09',8,'Assemble AVERYN','accepting','cross-source deduction','Discord answer',array['v5_ar07_n'],'v5_case_c09_complete','Averyn identity and G6 release instructions','reopen name console with solved state','v5-ar08-averyn'),
  ('RP01','C10',1,'Release footage','unwriting','cross-media','media payload answer',array['v5_case_c09_complete'],'v5_rp01_instruction','SIX RETURN ONE IS NOT KEPT receipt','reveal clip 4 again','v5-rp01-release-instruction'),
  ('RP02','C10',2,'Configure release chamber','unwriting','physical Minecraft','tagged deposits and empty slot',array['v5_rp01_instruction'],'v5_rp02_configured','six affidavits plus seals keys bridge installed','reissue all earned artifacts',null),
  ('RP03','C10',3,'Choose name treatment','unwriting','collective choice','protected choice markers',array['v5_rp02_configured'],'v5_rp03_name_choice','publish or release-unnamed branch','operator recovery preserves chosen branch',null),
  ('RP04','C10',4,'Active roster operation','unwriting','collective rite','group presence and tagged bridge',array['v5_rp03_name_choice'],'v5_rp04_collective','finale-ready receipt','rearm presence without changing choices',null),
  ('RP05','C10',5,'Sever the Record','release','final choice','armed confirmation',array['v5_rp04_collective'],'v5_rp05_severed','durable ending state plus website and Discord coda','cancel before safe cutoff',null),
  ('RP06','C10',6,'Cinematic close and Coda','release','cinematic state machine','automatic armed finale',array['v5_rp05_severed'],'v5_case_c10_complete','save goodbye kick shutdown and persistent Coda Mode','resume idempotently from durable finale phase',null)
on conflict (node_key) do update set
  case_key=excluded.case_key, ordinal=excluded.ordinal, title=excluded.title,
  room_id=excluded.room_id, modality=excluded.modality, input_surface=excluded.input_surface,
  prerequisite_flags=excluded.prerequisite_flags, completion_flag=excluded.completion_flag,
  reward=excluded.reward, recovery=excluded.recovery,
  oracle_puzzle_key=excluded.oracle_puzzle_key, required=true, active=true, updated_at=now();

-- Runtime ownership is seeded from design/ARG-V5-RUNTIME-BINDINGS.csv. Keeping it on every node
-- gives operators and recovery tooling one authoritative, queryable input contract instead of
-- inferring behavior from room names or old V4 code.
update public.investigation_nodes as node
set metadata = jsonb_build_object(
  'runtime_owner', binding.owner,
  'handler', binding.handler,
  'site_id', binding.site_id,
  'replay_policy', binding.replay_policy
), updated_at = now()
from (values
  ('LS01','website','route_receipt','copperline_traces','v5_ls01_traces','idempotent'),
  ('LS02','website','answer_resolver','copperline_service_1842','v5_ls02_service_1842','idempotent'),
  ('LS03','website','route_receipt','copperline_directory','v5_ls03_directory_trail','idempotent'),
  ('LS04','website','archive_resolver','copperline_world_backup','v5_ls04_archive_solved','idempotent'),
  ('LS05','discord','identity_link','discord_link','v5_ls05_bound','idempotent'),
  ('LS06','plugin','tagged_deposit','forgotten_mouth','v5_case_c01_complete','return_wrong_and_idempotent'),
  ('LC01','plugin','item_frame_overlay','orientation_register','v5_lc01_phases','idempotent_state_match'),
  ('LC02','plugin','answer_sign','offering_cairn_01','v5_lc02_rations','oracle_idempotent'),
  ('LC03','plugin','source_inspection','orientation_register','v5_lc03_daily_life','idempotent_receipt'),
  ('LC04','plugin','answer_sign','rune_rosetta','v5_lc04_safety_script','oracle_idempotent'),
  ('LC05','discord','answer_resolver','discord_answer','v5_lc05_motive','idempotent'),
  ('LC06','plugin','frame_and_tagged_lever','undercroft_seal','v5_case_c02_complete','idempotent_state_match'),
  ('KV01','plugin','container_inspection','vaun_hoard_chest','v5_kv01_audit','idempotent_receipt'),
  ('KV02','plugin','tagged_container_sort','vaun_hoard_chest','v5_kv02_sort','return_wrong_and_idempotent'),
  ('KV03','plugin','answer_sign','stone_vaun','v5_kv03_affidavit','oracle_and_artifact_idempotent'),
  ('KM01','plugin','lectern_page_comparison','mara_lectern_1','v5_km01_editions','idempotent_state_match'),
  ('KM02','plugin','answer_sign','stone_mara','v5_km02_extraction','oracle_idempotent'),
  ('KM03','plugin','bounded_route','mara_route_marker_1','v5_km03_affidavit','artifact_idempotent'),
  ('KS01','plugin','reflection_alignment','sella_pool','v5_ks01_bearing','idempotent_state_match'),
  ('KS02','plugin','item_frame_overlay','sella_anchor','v5_ks02_overlay','idempotent_state_match'),
  ('KS03','plugin','answer_sign','stone_sella','v5_ks03_affidavit','oracle_and_artifact_idempotent'),
  ('KO01','plugin','crouched_sightline','orin_marker_1','v5_ko01_marks','idempotent_receipt'),
  ('KO02','plugin','item_frame_dials','orin_frame_dial_1','v5_ko02_dials','idempotent_state_match'),
  ('KO03','discord','answer_resolver','discord_answer','v5_ko03_affidavit','artifact_idempotent'),
  ('KB01','discord','media_answer','discord_answer','v5_kb01_stay_awake','idempotent'),
  ('KB02','plugin','visual_dial_answer','brann_toll_tower','v5_kb02_toll','oracle_idempotent'),
  ('KB03','discord','answer_resolver','discord_answer','v5_kb03_affidavit','artifact_idempotent'),
  ('KI01','plugin','answer_sign','stone_iss','v5_ki01_vigenere','oracle_idempotent'),
  ('KI02','plugin','tagged_item_inspection','the_cold_hearth','v5_ki02_keepsake','idempotent_receipt'),
  ('KI03','discord','answer_resolver','discord_answer','v5_ki03_affidavit','artifact_idempotent'),
  ('CW01','plugin','source_inspection','cistern_7','v5_cw01_complaints','idempotent_receipt'),
  ('CW02','plugin','tagged_barrel_sort','cistern_7','v5_cw02_samples','return_wrong_and_idempotent'),
  ('CW03','plugin','item_frame_valves','cistern_7','v5_cw03_valves','idempotent_state_match'),
  ('CW04','plugin','answer_sign','watch_floor','v5_cw04_roster','oracle_idempotent'),
  ('CW05','discord','answer_resolver','discord_answer','v5_cw05_counterfeit','idempotent'),
  ('CW06','discord','media_answer','discord_answer','v5_cw06_reeds','idempotent'),
  ('CW07','plugin','tagged_cache','the_far_water','v5_cw07_cache','idempotent_receipt'),
  ('CW08','discord','conclusion_resolver','discord_answer','v5_case_c04_complete','artifact_idempotent'),
  ('BI01','plugin_unlit','house_mechanism','unlit_house_lamp','v5_bi01_lamp','idempotent_state_match'),
  ('BI02','plugin_unlit','tagged_fragments','unlit_house_cairn','v5_bi02_cairn','return_wrong_and_idempotent'),
  ('BI03','plugin_unlit','item_arrangement','unlit_house_coop','v5_bi03_coop','idempotent_state_match'),
  ('BI04','plugin_unlit','reflection_frames','unlit_house_well','v5_bi04_well','idempotent_state_match'),
  ('BI05','plugin_unlit','answer_sign','unlit_house_watch','v5_bi05_watch','oracle_idempotent'),
  ('BI06','plugin_unlit','tagged_sample_deposit','unlit_house_warm','v5_bi06_warm','return_wrong_and_idempotent'),
  ('BI07','plugin_unlit','bounded_group_walk','unlit_house_threshold','v5_bi07_threshold','idempotent_receipt'),
  ('BI08','discord','conclusion_resolver','discord_answer','v5_case_c05_complete','artifact_idempotent'),
  ('HS01','plugin','tagged_item_claim','dead_stall','v5_hs01_filter','artifact_idempotent'),
  ('HS02','plugin','exact_tagged_deposit','offering_cairn_01','v5_hs02_installed','return_wrong_and_idempotent'),
  ('HS03','plugin','item_frame_lamps','lampworks_stair','v5_hs03_lamps','idempotent_state_match'),
  ('HS04','plugin','chiseled_bookshelf_pattern','third_lamp_stand','v5_hs04_pressure','idempotent_state_match'),
  ('HS05','plugin','item_frame_dials','orin_frame_dial_1','v5_hs05_dials','idempotent_state_match'),
  ('HS06','plugin','bounded_route','painted_line','v5_hs06_passage','artifact_idempotent'),
  ('HS07','plugin','tagged_key_console','stone_of_reckoning','v5_case_c06_complete','artifact_idempotent'),
  ('A01','discord','answer_resolver','discord_answer','v5_a01_location','idempotent'),
  ('A02','plugin','tagged_station_inspection','prior_camp','v5_a02_stations','idempotent_receipt'),
  ('A03','plugin','container_order','prior_camp','v5_a03_supplies','return_wrong_and_idempotent'),
  ('A04','plugin','answer_sign','prior_camp','v5_a04_notebook','oracle_idempotent'),
  ('A05','plugin','item_frame_overlay','prior_camp','v5_a05_overlay','idempotent_state_match'),
  ('A06','website','route_resolver','copperline_archive_route','v5_a06_route','idempotent'),
  ('A07','website','automatic_media_reveal','clip_01_ash_locker','v5_a07_clip1','idempotent'),
  ('A08','discord','media_answer','discord_answer','v5_a08_ash13','idempotent'),
  ('A09','plugin','tagged_locker','prior_camp','v5_a09_spool','artifact_idempotent'),
  ('A10','discord','conclusion_resolver','discord_answer','v5_case_c07_complete','idempotent'),
  ('WR01','plugin','answer_sign','dread_route_start','v5_wr01_quotes','oracle_idempotent'),
  ('WR02','plugin','tagged_spool_reader','dread_route_elsewhere','v5_wr02_index','idempotent_receipt'),
  ('WR03','plugin','npc_choice','npc_wren_anchor','v5_wr03_confession','idempotent_dialogue'),
  ('WR04','plugin','blocked_route','the_threshold','v5_wr04_bridge','artifact_idempotent'),
  ('WR05','plugin','tagged_group_rite','threshold_vault','v5_case_c08_complete','branch_locked_idempotent'),
  ('AR01','discord','media_answer','discord_answer','v5_ar01_not_kept','idempotent'),
  ('AR02','plugin','affidavit_reader','keeper_altar','v5_ar02_a','fragment_idempotent'),
  ('AR03','plugin','affidavit_reader','keeper_altar','v5_ar03_v','fragment_idempotent'),
  ('AR04','plugin','affidavit_reader','keeper_altar','v5_ar04_e','fragment_idempotent'),
  ('AR05','plugin','affidavit_reader','keeper_altar','v5_ar05_r','fragment_idempotent'),
  ('AR06','plugin','affidavit_reader','keeper_altar','v5_ar06_y','fragment_idempotent'),
  ('AR07','plugin','affidavit_reader','keeper_altar','v5_ar07_n','fragment_idempotent'),
  ('AR08','discord','answer_resolver','discord_answer','v5_case_c09_complete','idempotent'),
  ('RP01','discord','media_answer','discord_answer','v5_rp01_instruction','idempotent'),
  ('RP02','plugin','tagged_release_configuration','the_unwriting','v5_rp02_configured','return_wrong_and_idempotent'),
  ('RP03','plugin','protected_choice_markers','the_unwriting','v5_rp03_name_choice','branch_locked_idempotent'),
  ('RP04','plugin','group_presence_bridge','coop_plate','v5_rp04_collective','idempotent_receipt'),
  ('RP05','plugin_finale','armed_confirmation','release_record','v5_rp05_severed','durable_before_theater'),
  ('RP06','plugin_finale','automatic_finale','release_record','v5_case_c10_complete','terminal_coda_idempotent')
) as binding(node_key, owner, handler, site_id, completion_flag, replay_policy)
where node.node_key = binding.node_key
  and node.completion_flag = binding.completion_flag;

insert into public.required_media
  (media_key,case_key,node_key,media_kind,title,delivery_url,filename,sha1,expected_payload,prerequisite_flags,delivery_state,active)
values
  ('clip_01_ash_locker','C07','A07','video','Ash field footage 01','https://youtu.be/du-qp_clP7c','clip_01_prior_base.mp4','844c2aaf8fb51836add4b59e81abe4131c8d6d0a','ASH-13',array['v5_a06_route'],'configured',true),
  ('clip_02_reeds_cache','C04','CW06','video','Reeds field footage','https://youtu.be/iKqvPMHjR74','clip_02_far_water_count.mp4','9b979e349c7a0d7497fd0fe76d0450e744dc39d0','WHERE THE REEDS FOLD BACK',array['v5_cw05_counterfeit'],'configured',true),
  ('clip_03_watch_correction','C03','KB01','video','Watch floor footage','https://youtu.be/pSPhBYMGIRc','clip_03_black_moon_toll.mp4','9b6552e21ec01e6f046027247a689c8dd78b8ce1','STAY AWAKE',array['v5_case_c02_complete'],'configured',true),
  ('clip_04_release_instruction','C10','RP01','video','Room below release footage','https://youtu.be/DtZizx5QIEs','clip_04_release_room_late.mp4','1cb3e600d3e16e9bb1434fa65ddbdff04f512fbd','SIX RETURN, ONE IS NOT KEPT',array['v5_case_c09_complete'],'configured',true),
  ('spectrogram_averyn_voice','C09','AR01','audio','Recovered field audio 03','https://www.dropbox.com/scl/fo/72dz7n8lpa1gtiymtkyjl/AMbzcJsSm0x2_TkUq1Bzkv4?rlkey=tsom0g4z87qqxv7jo6cr989v5&st=014v4y3g&dl=0','field_audio_03.wav','2003f0151c1ba643c649b5ed0e19d1b31bb68319','I WAS NOT KEPT',array['v5_case_c08_complete'],'configured',true)
on conflict (media_key) do update set
  case_key=excluded.case_key,node_key=excluded.node_key,media_kind=excluded.media_kind,
  title=excluded.title,delivery_url=excluded.delivery_url,filename=excluded.filename,
  sha1=excluded.sha1,expected_payload=excluded.expected_payload,
  prerequisite_flags=excluded.prerequisite_flags,delivery_state=excluded.delivery_state,
  active=true,updated_at=now();

-- Exact three-tier rescue rails from arc/v5/SOLUTION-CASEBOOK.md. Hints never invent evidence;
-- autocomplete sees only active, prerequisite-open V5 answer nodes.
insert into public.hints (puzzle_key,tier,body,active)
values
  ('v5-lc05-motive',1,'Reject reasons crossed out in the founding minutes.',true),
  ('v5-lc05-motive',2,'The east road fails on exposure and water; shallow shelter fails on fuel.',true),
  ('v5-lc05-motive',3,'Name heat, water, and cover.',true),
  ('v5-ko03-crack-map',1,'Signed safe is not the whole sentence.',true),
  ('v5-ko03-crack-map',2,'Test each certificate condition against the physical evidence.',true),
  ('v5-ko03-crack-map',3,'Both safety conditions failed.',true),
  ('v5-kb01-stay-awake',1,'Watch the visible lamps and titles; audio is reinforcement.',true),
  ('v5-kb01-stay-awake',2,'Note the repeated imperative at the black-watch sequence.',true),
  ('v5-kb01-stay-awake',3,'STAY AWAKE.',true),
  ('v5-kb03-altered-watch',1,'Test names against availability, not only the clean rota.',true),
  ('v5-kb03-altered-watch',2,'Nessa cannot work after removal; follow the meal chit.',true),
  ('v5-kb03-altered-watch',3,'Toma Rill / bell eight.',true),
  ('v5-ki03-iss-correction',1,'Follow correction marks, not the warm first letters.',true),
  ('v5-ki03-iss-correction',2,'Read only lines with the same cut as the keepsake tag.',true),
  ('v5-ki03-iss-correction',3,'The actor is ISS; action SIGNED OUT; subject AVERYN.',true),
  ('v5-cw05-counterfeit',1,'Use row then column; I/J share a stall.',true),
  ('v5-cw05-counterfeit',2,'Decode the five crate pairs.',true),
  ('v5-cw05-counterfeit',3,'F / A / L / S / E.',true),
  ('v5-cw06-reeds',1,'Frame location and shoreline shape matter.',true),
  ('v5-cw06-reeds',2,'Combine visible words in temporal order.',true),
  ('v5-cw06-reeds',3,'WHERE THE REEDS FOLD BACK.',true),
  ('v5-cw08-clear-nessa',1,'Separate worker conduct from material cause.',true),
  ('v5-cw08-clear-nessa',2,'Use samples, invoice, and both discipline drafts.',true),
  ('v5-cw08-clear-nessa',3,'Clear Nessa and name counterfeit filters or supply diversion.',true),
  ('v5-bi08-break-inquest',1,'Make a cause table: existed before / accelerated / hidden / supernatural feedback.',true),
  ('v5-bi08-break-inquest',2,'Use all seven house receipts and the copied base.',true),
  ('v5-bi08-break-inquest',3,'Preexisting fracture + resource neglect + hidden cut + delayed or edited response + Record feedback.',true),
  ('v5-a01-camp-ash',1,'Three sources describe one journey in different units.',true),
  ('v5-a01-camp-ash',2,'Convert supply turns, map compass, and Wren''s walking time to one relative route.',true),
  ('v5-a01-camp-ash',3,'The map and supply entry agree; use Wren only to identify which distance is false.',true),
  ('v5-a08-ash-13',1,'Do not treat 13 as another Keeper count.',true),
  ('v5-a08-ash-13',2,'Combine the name in the frame with Locker 13 in the camp.',true),
  ('v5-a08-ash-13',3,'ASH-13.',true),
  ('v5-a10-sabotage',1,'Test who knew each leaked detail and when.',true),
  ('v5-a10-sabotage',2,'Eliminate Ash and mkept with camera and offline receipts; Rook authored the private revision.',true),
  ('v5-a10-sabotage',3,'Wren is the only remaining source.',true),
  ('v5-ar01-not-kept',1,'Inspect frequency over time, not only listen.',true),
  ('v5-ar01-not-kept',2,'Use a spectrogram view; speech playback is not required.',true),
  ('v5-ar01-not-kept',3,'I WAS NOT KEPT.',true),
  ('v5-ar08-averyn',1,'Keep letter order by Keeper dossier order in the archive.',true),
  ('v5-ar08-averyn',2,'Vaun / Mara / Sella / Orin / Brann / Iss.',true),
  ('v5-ar08-averyn',3,'AVERYN.',true),
  ('v5-rp01-release-instruction',1,'This is a chamber instruction, not a new roster reveal.',true),
  ('v5-rp01-release-instruction',2,'Distinguish return from bind or keep.',true),
  ('v5-rp01-release-instruction',3,'SIX RETURN, ONE IS NOT KEPT.',true)
on conflict (puzzle_key,tier) do update set body=excluded.body, active=true;

do $$
declare
  v_cases int;
  v_nodes int;
  v_expected int;
  v_media int;
begin
  select count(*) into v_cases from public.investigations where active and required;
  select count(*) into v_nodes from public.investigation_nodes where active and required;
  select sum(expected_nodes) into v_expected from public.investigations where active and required;
  select count(*) into v_media from public.required_media where active;
  if v_cases <> 10 then raise exception 'V5 seed expected 10 mandatory cases, found %', v_cases; end if;
  if v_nodes <> 82 then raise exception 'V5 seed expected 82 mandatory nodes, found %', v_nodes; end if;
  if v_expected <> 82 then raise exception 'V5 case budgets sum to %, expected 82', v_expected; end if;
  if v_media <> 5 then raise exception 'V5 media manifest expected 5 required assets, found %', v_media; end if;
  if exists (
    select 1 from public.investigations i
    left join public.investigation_nodes n on n.case_key=i.case_key and n.active and n.required
    where i.active and i.required
    group by i.case_key,i.expected_nodes
    having count(n.node_key) <> i.expected_nodes
  ) then raise exception 'V5 per-case node budget mismatch'; end if;
end;
$$;

commit;
