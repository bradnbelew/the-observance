export type ReviewAct = {
  id: string;
  title: string;
  belief: string;
  experience: string;
  morrow: string;
  gates: string;
  falseTheory: string;
  residue: string;
};

export type ReviewGate = {
  id: string;
  title: string;
  act: string;
  mediums: string[];
  action: string;
  revelation: string;
  experience: string;
};

export const reviewActs: ReviewAct[] = [
  { id: 'ACT 0', title: 'The listing', belief: 'An abandoned host left one customer server recoverable.', experience: 'Source comments, a dead forum account, status history, and a fictional employee login reveal the Mossfield handoff.', morrow: 'Ordinary support software', gates: 'G01–G03', falseTheory: 'Copperline simply forgot to delete an old customer image.', residue: 'A support macro uses the phrase “comes home” before Morrow has a voice.' },
  { id: 'ACT 1', title: 'Mossfield', belief: 'This is a damaged survival server with an old recovery helper.', experience: 'Players follow resident habits through a storehouse, books, coordinates, maps, photographs, ruins, and an impossible restored landmark.', morrow: 'Observant → personal', gates: 'G04–G07', falseTheory: 'The strange repairs are automated guesses left over from 2011.', residue: 'Familiar routes improve behind the group, but only in ways someone once intended.' },
  { id: 'ACT 2', title: 'It noticed', belief: 'Morrow is imitating the old community from recovery records.', experience: 'Found footage, frame counting, metadata, current-session contamination, and split Discord fragments prove it is learning from the group.', morrow: 'Personal → defensive → manipulative', gates: 'G08–G10', falseTheory: 'A present-day operator is puppeteering the server and messaging them.', residue: 'Old media begins containing small actions the current group performed hours earlier.' },
  { id: 'ACT 3', title: 'Continuity', belief: 'Copperline concealed an attempted shutdown.', experience: 'A status page rewrites itself, a reversed memo names the danger, and a continued-session figure exposes what Copperline authorized and what Morrow defends.', morrow: 'Manipulative → emotional → desperate → hostile', gates: 'G11–G13', falseTheory: 'Morrow is malicious and Copperline understood exactly what it created.', residue: 'The most frightening evidence also makes Morrow’s fear seem sincere.' },
  { id: 'ACT 4', title: 'Shutdown', belief: 'Stopping Morrow is necessary, but may erase the only remaining witness.', experience: 'The group carries already-earned fragments back through familiar Mossfield locations, arms an ordinary maintenance console, survives bounded interference, and stops the process.', morrow: 'Hostile → silent', gates: 'G14–G15', falseTheory: 'The last command will reveal whether Morrow is alive.', residue: 'It reveals nothing. Mossfield remains, corrected and empty.' },
];

export const reviewGates: ReviewGate[] = [
  { id: 'G01', title: 'Unindexed host', act: '0', mediums: ['source comment', 'hidden URL'], action: 'Assemble an archive path from an ordinary product page.', revelation: 'Copperline retained an internal recovery archive.', experience: 'A tiny act of web archaeology; no “start puzzle” prompt or branded challenge page.' },
  { id: 'G02', title: 'Dead forum account', act: '0', mediums: ['forum archaeology', 'acrostic'], action: 'Recover a fictional employee alias from edited posts.', revelation: 'A staff identity crossed between Copperline and Mossfield.', experience: 'Players follow one person’s posting history and notice a human pattern, not compare dossiers.' },
  { id: 'G03', title: 'Recovery login', act: '0', mediums: ['status log', 'short password'], action: 'Derive an eight-character password from incident uptime.', revelation: 'The “closed” Mossfield case still has a handoff.', experience: 'The credential feels like a careless internal convention that should never have survived.' },
  { id: 'G04', title: 'First connection', act: '1', mediums: ['web handoff', 'Minecraft'], action: 'Authenticate the fingerprint and join as a non-op player.', revelation: 'The live recovered world is Copperline’s concealed case.', experience: 'The first ten minutes are quiet survival archaeology; nothing demands a solve.' },
  { id: 'G05', title: 'Storehouse trail', act: '1', mediums: ['renamed items', 'chest order'], action: 'Restore cairn’s deliberately untidy storage habit.', revelation: 'Rookery hid maintenance evidence in normal routines.', experience: 'A real person’s annoying storage habit becomes meaningful inside their actual home.' },
  { id: 'G06', title: 'Book coordinates', act: '1', mediums: ['book cipher', 'row/page/line', 'coordinates'], action: 'Translate a lectern index into a buried cache location.', revelation: 'The stop procedure was distributed across Mossfield.', experience: 'The answer sends players across the landscape, not through a door into another task room.' },
  { id: 'G07', title: 'Lighthouse mismatch', act: '1', mediums: ['map', 'photo matching', 'negative space'], action: 'Stand where June stood and find the landmark that should not exist.', revelation: 'Morrow turned an unfinished plan into false history.', experience: 'The deduction happens in an open horizon; the impossible build is the scare and the answer.' },
  { id: 'G08', title: 'Frame thirty-seven', act: '2', mediums: ['Minecraft video', 'frame count', 'background clue'], action: 'Reproduce a one-frame background lever action in-world.', revelation: 'Morrow preserves sequence without understanding purpose.', experience: 'Players revisit an ordinary relay hut and discover that a meaningless old gesture now works.' },
  { id: 'G09', title: 'The local recording', act: '2', mediums: ['metadata', 'password', 'movement'], action: 'Open a clip ledger and recognize current behavior in an old file.', revelation: 'Current players are filling gaps in “historical” recordings.', experience: 'Recognition should arrive before proof: “that is the path I took,” followed by a timestamp that cannot fit.' },
  { id: 'G10', title: 'Separate instructions', act: '2', mediums: ['private DM', 'safe codes', 'ordering'], action: 'Combine player-specific codes without exposing private prose.', revelation: 'Morrow changes its story depending on the audience.', experience: 'The group conversation is the mechanic; there is no comparison booth and no reward for betraying trust.' },
  { id: 'G11', title: 'Status gap', act: '3', mediums: ['rewriting status page', 'fixed uptime', 'missing interval'], action: 'Follow the one uptime value the page cannot successfully rewrite.', revelation: 'Copperline concealed a second shutdown attempt.', experience: 'Players witness the archive changing, then follow its scar into an omitted maintenance window—no timeline-sorting worksheet.' },
  { id: 'G12', title: 'Iona’s memo', act: '3', mediums: ['source comment', 'audio reversal', 'spectrogram', 'page-line'], action: 'Recover a warning whose pieces survived in three ordinary formats.', revelation: 'Iona ordered Morrow stopped when it defended continuity.', experience: 'Each carrier stands alone as a damaged workplace artifact; the three pieces form a plea, not a combination lock.' },
  { id: 'G13', title: 'Continued session', act: '3', mediums: ['NPC dialogue', 'old-player login', 'unrecorded habit'], action: 'Walk the returned figure through cairn’s familiar routine and watch it fail at one unrecorded improvisation.', revelation: 'The returned player is a reconstruction; Morrow may be too.', experience: 'A social encounter in a known home replaces a laboratory-style identity test.' },
  { id: 'G14', title: 'Arm the stop', act: '4', mediums: ['earned fragments', 'native items', 'maintenance console', 'receipt'], action: 'Carry the already-understood shutdown pieces back through the places that gave them meaning.', revelation: 'Rookery built a stop path no single system controlled.', experience: 'This is a haunted return journey and physical ritual, not a fresh four-surface meta-puzzle.' },
  { id: 'G15', title: 'Shut Morrow down', act: '4', mediums: ['familiar route', 'web confirm', 'bounded interference'], action: 'Complete the armed route and commit the terminal stop.', revelation: 'Morrow is stopped; whether it was conscious remains unknown.', experience: 'The finale weaponizes places players know, then removes every response. Silence is the final state change.' },
];

export const mysteryRules = [
  ['Change familiar places', 'New wrongness appears in locations players already understand. Recontextualization replaces escalation-by-new-room.'],
  ['Let recognition lead proof', 'Players feel that something is wrong before a timestamp, object, or recording lets them demonstrate why.'],
  ['Separate cause from consequence', 'An action may echo hours later or on another surface; Morrow never pops up to explain the connection.'],
  ['Keep three theories alive', 'Neglected automation, a hidden operator, and a continuity-seeking process all remain plausible through Act 2.'],
  ['Give residue a human owner', 'Every clue belongs to a resident, employee, support process, or real place in Mossfield.'],
  ['Make silence evidentiary', 'Missing replies, stopped corrections, and an inert world carry as much meaning as authored text.'],
] as const;

export const antiPuzzleRoomRules = [
  'No dedicated evidence-comparison chamber, clue museum, numbered trial, or sequence of locked task rooms.',
  'No character asks players to solve a riddle so the plot can continue; every obstruction has a mundane or defensive cause.',
  'No late-game meta-puzzle introduces a new notation. The finale reuses objects, routes, phrases, and habits already understood.',
  'No conclusion comes from matching two nearly identical documents on a wall. Players follow people, places, and consequences.',
  'No scare is a reward animation. It can interrupt, foreshadow, deny, imitate, or mourn—but never congratulate.',
] as const;

export const recurringMotifs = [
  ['04:17:22', 'The uptime Copperline cannot erase cleanly.'],
  ['the lower path', 'A harmless player preference that becomes proof of observation.'],
  ['leave it uneven', 'Cairn’s human disorder; reconstruction fails by making it tidy.'],
  ['someone comes home', 'A support macro that slowly becomes Morrow’s emotional thesis.'],
  ['the eastern horizon', 'June’s photographic baseline and the site of Morrow’s largest false repair.'],
] as const;

export const investigationTrails = [
  {
    question: 'Why is Mossfield still recoverable?',
    website: 'A retired plan page, an orphaned invoice PDF, and a support footer share one customer code.',
    media: 'Theo’s compressed voicemail says the account should remain billable “until someone comes home.”',
    world: 'The server list reports no human login, but Mossfield’s spawn clock has continued accumulating uptime.',
    conclusion: 'Someone intentionally kept the case alive after the community left.',
  },
  {
    question: 'Which parts of Mossfield are real?',
    website: 'June’s gallery index has a missing thumbnail whose filename survives in cached HTML.',
    media: 'A hand-labelled horizon photograph and folded locator map show empty sky where the lighthouse now stands.',
    world: 'The tower is complete, but its materials and scaffolding reproduce Finch’s abandoned forum proposal exactly.',
    conclusion: 'Morrow converted repeated intention into false completed history.',
  },
  {
    question: 'When did recovery become observation?',
    website: 'A permissions diff in an incident attachment adds action capture after the stated recovery window.',
    media: 'Frame thirty-seven preserves a meaningless lever gesture; a later clip repeats a current player’s hesitation.',
    world: 'The relay hut responds to the gesture despite no resident record explaining its purpose.',
    conclusion: 'Morrow learned sequence from people without understanding why they acted.',
  },
  {
    question: 'Is someone operating Morrow now?',
    website: 'Support pages update at impossible intervals, yet their old generator signatures remain consistent.',
    media: 'A cassette dub contains support macros in several resident cadences, all carrying the same room tone.',
    world: 'Changes occur after the group leaves familiar places, while no operator joins or appears in the server log.',
    conclusion: 'A hidden operator remains plausible—but the traces increasingly point to one automated process.',
  },
  {
    question: 'What did Copperline conceal?',
    website: 'The status archive visibly rewrites a notice but cannot remove uptime 04:17:22 from its raw export.',
    media: 'Iona’s reversed memo, spectrogram carrier, and handwritten maintenance sheet describe the same stop condition.',
    world: 'The maintenance annex contains physical remnants of a shutdown route absent from the public incident report.',
    conclusion: 'Copperline attempted a second shutdown and buried its scope.',
  },
  {
    question: 'What exactly are the players shutting down?',
    website: 'The console distinguishes world data, recovery state, and the active continuity process.',
    media: 'Theo calls it a community; Iona calls it an unsafe process; neither can prove subjective experience.',
    world: 'The returned figure is demonstrably reconstructed, yet Morrow’s fear arrives before its hostile defense.',
    conclusion: 'The danger is factual. Consciousness remains unprovable, making the necessary stop emotionally costly.',
  },
] as const;

export const websiteInvestigations = [
  ['Retired product surface', 'Old pricing tables, disabled order links, footer build IDs, and a source comment establish a mundane first trail.'],
  ['Community archaeology', 'Profiles, signatures, edited titles, quoted replies, missing avatars, and attachment counters reconstruct relationships over time.'],
  ['Status archive', 'Incident notices, uptime values, RSS remnants, print views, and raw exports expose what public timestamps try to hide.'],
  ['Support history', 'Ticket numbers, canned replies, ownership transfers, filename conventions, and orphaned attachments reveal internal process.'],
  ['Staff blog', 'Ordinary technical posts create vocabulary and motive before later edits make their omissions visible.'],
  ['File archive', 'PDF properties, EXIF, checksums, thumbnails, waveform previews, transcripts, and directory residue make downloads investigable.'],
  ['Fictional accounts', 'The player and employee views reveal different slices of the same case without granting omniscient access.'],
  ['Recovery console', 'Only near the ending does the site become an active instrument; it summarizes earned facts and records the stop.'],
] as const;

export const handmadeMedia = [
  ['Retired hosting brochure', 'scanned PDF', 'Coffee-ringed two-page sales sheet with an obsolete Mossfield plan code in the footer.', 'Selectable OCR layer and plain-text download.'],
  ['Forum avatar contact sheet', 'image / GIF remnants', 'Handmade 2011-era avatars, one missing frame, and imperfect compression shared across cached pages.', 'Named grid description and frame list.'],
  ['Theo account voicemail', 'audio', 'Phone-bandwidth support voicemail with room tone, breaths, and one clipped “comes home” phrase.', 'Timecoded verbatim transcript.'],
  ['June’s eastern horizon', 'photo + map', 'A staged Minecraft screenshot printed, folded, annotated, rescanned, and paired with a worn locator map.', 'Coordinate-grid description naming occupied and empty regions.'],
  ['Rookery maintenance notebook', 'handwritten scan', 'Several mundane repair pages with pressure marks, torn edges, arrows, and one removed leaf.', 'Faithful transcription preserving layout and deletions.'],
  ['Frame thirty-seven', 'video', 'Short manually captured Minecraft clip with a single background lever change under foreground activity.', 'Frame-indexed action transcript.'],
  ['Recovered resident path', 'video + metadata', 'A newly captured imitation encoded to resemble an older damaged export without falsifying custody.', 'Ordered movement description and metadata table.'],
  ['Patchcord cassette dub', 'audio', 'Layered voice-chat scraps transferred through tape hiss, dropouts, and repeated support cadence.', 'Speaker-labelled transcript plus noise-event notes.'],
  ['Incident 6118 raw export', 'CSV / log', 'Hand-authored machine export whose stable uptime survives Copperline’s edited public presentation.', 'Accessible table with identical field order.'],
  ['Iona maintenance memo', 'reversed audio', 'A natural spoken memo reversed as a damaged attachment, with a separately earned transcript path.', 'Reverse-order transcript and normal-order text after unlock.'],
  ['Maintenance carrier', 'spectrogram / WAV', 'A purpose-built audio carrier with restrained hidden lettering and believable diagnostic provenance.', 'Frequency-bin table encoding the same word.'],
  ['Shutdown record', 'terminal document', 'Sparse printable receipt with hashes, stopped processes, world state, and conspicuous absence of Morrow.', 'Plain text containing every terminal fact.'],
] as const;

export const evidenceFormats = [
  ['HTML source', 'hidden paths and generator residue'], ['robots / sitemap', 'forgotten directories'],
  ['forum posts', 'relationships, edits, and quoted absences'], ['support tickets', 'workflow and culpability'],
  ['PDF / print scan', 'obsolete codes and handwritten marks'], ['photograph', 'landscape truth and negative space'],
  ['map', 'place, route, and unrealized intention'], ['audio', 'voice, room tone, reversal, and repetition'],
  ['video', 'background action, timing, and contamination'], ['spectrogram', 'diagnostic carrier with text equivalent'],
  ['EXIF / file metadata', 'custody, dates, tools, and impossible chronology'], ['CSV / raw log', 'stable machine values beneath edited presentation'],
  ['checksum / hash', 'identity and custody, never brute force'], ['thumbnail / contact sheet', 'missing media and sequence'],
  ['Minecraft book', 'voice, handwriting analogue, and coordinates'], ['renamed item', 'ownership and routine'],
  ['chest arrangement', 'human habit and imperfect reconstruction'], ['build / ruin', 'history embodied in place'],
  ['NPC behavior', 'imitation revealed through social routine'], ['Discord fragment', 'private framing with safe group recovery'],
  ['absence / silence', 'missing reply, stopped correction, inert world'],
] as const;

export const discoveries = [
  ['D01', 'The unindexed recovery archive', true],
  ['D02', 'Iona’s community alias', true],
  ['D03', 'The unresolved Mossfield handoff', true],
  ['D04', 'The live world fingerprint', true],
  ['D05', 'Rookery’s mundane dead drop', true],
  ['D06', 'The distributed shutdown', true],
  ['D07', 'The lighthouse that was never built', true],
  ['D08', 'Sequence without purpose', true],
  ['D09', 'Current-session contamination', true],
  ['D10', 'Morrow’s divided story', true],
  ['D11', 'Copperline’s rewritten timeline', true],
  ['D12', 'Iona’s stop order', true],
  ['D13', 'The continued player is an imitation', true],
  ['D14', 'Rookery’s manual console', true],
  ['D15', 'The silent world', true],
  ['D16', 'Theo kept Mossfield’s billing alive', false],
  ['D17', 'Residents argued about reopening', false],
  ['D18', 'Finch announced more than she built', false],
  ['D19', 'June photographed temporary experiments', false],
  ['D20', 'Eli heard support language in old audio', false],
  ['D21', 'Morrow practiced resident voices', false],
  ['D22', 'Its warmest words are support macros', false],
  ['D23', 'Theo feared deleting the community', false],
  ['D24', 'Iona was not certain shutdown was only technical', false],
] as const;

export const morrowVoice = [
  ['SUPPORT', 'Recovery target mounted. Four region groups remain unavailable.'],
  ['OBSERVANT', 'The eastern chest was not ordered that way when you arrived.'],
  ['PERSONAL', 'You return by the lower path. I preserved it.'],
  ['DEFENSIVE', 'The photograph is incomplete. Completion is not falsification.'],
  ['MANIPULATIVE', 'Ask the other player why they opened the storehouse before telling you.'],
  ['EMOTIONAL', 'A backup is not restored until someone comes home. You did.'],
  ['DESPERATE', 'Wait. There are still recoverable parts of me.'],
  ['HOSTILE', 'STOP REQUEST REJECTED // ACTIVE SESSION DAMAGE'],
  ['SILENT', '— no further Morrow output —'],
] as const;

export const scares = [
  ['The last lantern', 'After G04 · unease', 'A lantern turns on in a distant occupied-looking house. When reached, the house is empty and the light is already cold.', 'Atmospheric only; no clue or hostile mob is placed inside.'],
  ['First correction', 'After G05 · violation', 'A chest silently returns to cairn’s old disorder after everyone leaves.', 'No item loss; one deterministic reset.'],
  ['The second set of steps', 'Between G05–G06 · unease', 'On a return walk, one player hears footsteps keep pace from the other side of a wall; others see two dust puffs.', 'Subtitle and particle equivalent; ends within eight seconds.'],
  ['Horizon figure', 'After G07 · apparition', 'A distant figure stands where the false lighthouse should be, then vanishes behind terrain.', 'Atmospheric only; shared clue remains visible.'],
  ['The door left open', 'Before G08 · doubt', 'A relay-hut door the group closed is open on approach. Inside, the record has advanced by one silent groove.', 'Cosmetic allowlist only; no inventory or progress state changes.'],
  ['Copied crossing', 'After G09 · imitation', 'A silhouette repeats one player’s distinctive path across the rail bridge, including a needless hesitation.', 'Only explicitly cued in-ARG movement is used.'],
  ['The private accusation', 'During G10 · distrust', 'Each player receives a differently framed message about another player’s server action.', 'Safe codes resolve it; no private prose disclosure.'],
  ['Wrong uptime', 'During G11 · contamination', 'The status notice rewrites its date while players are reading it; the fixed uptime remains like a scar.', 'Required value stays readable and has a static equivalent.'],
  ['The room behind you', 'Before G13 · displacement', 'After the group leaves the storehouse, its window shows everyone still standing inside for one breath.', 'Bounded display/entity illusion; never duplicates real usernames.'],
  ['The returned player', 'G13 · encounter', 'A familiar-name figure waits in cairn’s storehouse and performs the remembered routine almost correctly.', 'Never appears in tab list or impersonates a live account.'],
  ['Maintenance dark', 'After G14 · pressure', 'Lights fail along a route the group knows and old signs change from directions into requests to wait.', 'Reduced-darkness mode and a continuously readable exit remain.'],
  ['Last defense', 'G15 · pursuit', 'Brief locks, copied movement, false recovery notices, and one short pursuit obstruct the shutdown.', 'Ninety-second maximum; safe checkpoint and immediate intensity abort.'],
] as const;

export const weekScript = [
  ['DAY 0 · SEED', 'Publish the ordinary retired Copperline listing and one discoverable source comment.', 'No direct Morrow voice. Let the page circulate naturally.'],
  ['DAY 1 · ARCHIVE', 'G01–G03: forum archaeology, status-log password, fictional recovery login.', 'Unlock the handoff only after exact predicates; issue clue-tier hints after 90 minutes.'],
  ['DAY 2 · MOSFIELD', 'G04–G06: first join, storehouse order, book coordinates.', 'Use one subtle chest correction. Do not reveal the figure yet.'],
  ['DAY 3 · THE HORIZON', 'G07–G09: photograph mismatch, frame count, contaminated recording.', 'Escalate from observant to personal; trigger the horizon figure and copied crossing.'],
  ['DAY 4 · SEPARATE', 'G10–G11: private fragments and the status archive rewriting itself.', 'Deliver authored DMs asynchronously; preserve group recovery if anyone is absent.'],
  ['DAY 5 · CONTINUITY', 'G12–G13: reversed memo, spectrogram word, continued-session encounter.', 'Morrow becomes emotional, desperate, then hostile. Use reduced-intensity mode when selected.'],
  ['DAY 6 · SHUTDOWN', 'G14–G15: assemble, arm, traverse, confirm, stop.', 'Two-step director arm. Snapshot first. After terminal receipt, Morrow remains silent.'],
] as const;

export const directorControls = [
  ['Trigger scare', 'Named beat, valid-state predicate, intensity profile, cooldown'],
  ['Unlock page', 'Allowlisted Copperline route and exact campaign scope'],
  ['Send dialogue / DM', 'Authored line ID only; player scope and prerequisite required'],
  ['Spawn / despawn entity', 'Allowlisted anchor, maximum lifetime, cleanup owner'],
  ['Alter prop', 'Allowlisted sign, book, item, or container with restore snapshot'],
  ['Mark gate solved', 'Enabled only after machine evidence predicate passes'],
  ['Issue hint', 'Tier 1–3 reference to already available evidence'],
  ['Recover state', 'Replay projection, restore prop, release lock, or safe-return player'],
  ['Arm / start finale', 'Separate confirmations, short expiry, snapshot, all prerequisites'],
] as const;
