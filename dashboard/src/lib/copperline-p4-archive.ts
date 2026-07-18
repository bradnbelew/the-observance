export type CopperlineArchiveEntry = {
  id: string;
  date: string;
  author: string;
  kind: 'listing' | 'post' | 'reply' | 'notice' | 'ticket' | 'attachment';
  title: string;
  body: string;
  clueRole: 'direct' | 'texture' | 'mixed';
  relationship?: string;
};

/** Offline-authored P4 fixture. This is committed content, not a deployed/archive receipt. */
export const copperlineP4Entries: readonly CopperlineArchiveEntry[] = [
  { id: 'texture-01', date: '2010-11-06T19:14:00-05:00', author: 'craftdad', kind: 'post', title: 'Saturday backup window', body: 'Moving our family server backup to Saturday morning. If anybody sees the panel restart twice, that is me learning the difference between stop and kill.', clueRole: 'texture', relationship: 'regular helping other small server owners' },
  { id: 'texture-02', date: '2010-11-06T19:29:00-05:00', author: 'MapleAdmin', kind: 'reply', title: 'Re: Saturday backup window', body: 'Use stop and wait for the console to say all dimensions saved. I learned that lesson with a very small and very angry castle.', clueRole: 'texture', relationship: 'experienced moderator teasing a friend' },
  { id: 'texture-03', date: '2010-12-19T09:03:00-06:00', author: 'jon_c', kind: 'post', title: 'server-icon.png again', body: 'The public list still shows a grass block. I know this is not important. My nephew has nevertheless asked me about it every day this week.', clueRole: 'texture' },
  { id: 'texture-04', date: '2011-01-04T16:22:00-06:00', author: 'cl_amy', kind: 'notice', title: 'Chicago backup queue', body: 'Off-site backup jobs are running about forty minutes late. Game nodes are healthy. Please do not restart a job that already says queued.', clueRole: 'texture', relationship: 'Copperline support staff' },
  { id: 'texture-05', date: '2011-01-04T16:48:00-06:00', author: 'mkept', kind: 'reply', title: 'Re: Chicago backup queue', body: 'Mine finished. The progress bar stayed at 92 percent until the checksum step, then closed normally.', clueRole: 'mixed', relationship: 'practical server administrator' },
  { id: 'texture-06', date: '2011-01-17T21:10:00-06:00', author: 'lilacmine', kind: 'post', title: 'winter spawn screenshots', body: 'We finished the station roof. The album has twelve pictures and somehow nine of them are of Dan falling off the same scaffold.', clueRole: 'texture' },
  { id: 'texture-07', date: '2011-01-17T21:31:00-06:00', author: 'dan-builds', kind: 'reply', title: 'Re: winter spawn screenshots', body: 'The scaffold moved. I will not be taking questions.', clueRole: 'texture' },
  { id: 'texture-08', date: '2011-02-02T07:45:00-06:00', author: 'ryan88', kind: 'post', title: 'morning lag on chi-gs2', body: 'Mobs are rubber-banding but chat is fine. Posting here because the ticket form says my attachment is too large.', clueRole: 'texture' },
  { id: 'texture-09', date: '2011-02-02T08:07:00-06:00', author: 'cl_amy', kind: 'reply', title: 'Re: morning lag on chi-gs2', body: 'Found a runaway map render on the node. It is stopped and the owner has a note. Your server should settle without a restart.', clueRole: 'texture', relationship: 'support staff answers plainly' },
  { id: 'texture-10', date: '2011-02-11T23:18:00-06:00', author: 'mkept', kind: 'post', title: 'old copy opens without mods', body: 'The recovered world opens in the listed Java version. I removed player data and chat. If you were on the original server and want something private removed, send me the path rather than posting it here.', clueRole: 'texture', relationship: 'privacy-conscious custodian' },
  { id: 'texture-11', date: '2011-02-12T00:02:00-06:00', author: 'quietorbit', kind: 'reply', title: 'Re: old copy opens without mods', body: 'Thank you for taking the names out. I do not recognize the build, but the empty waiting room is upsetting in a way I cannot explain.', clueRole: 'mixed' },
  { id: 'texture-12', date: '2011-03-03T18:55:00-06:00', author: 'cobblebee', kind: 'post', title: 'market night moved', body: 'Friday market is moving to the school basement because the square is still full of snow blocks. Same time, fewer creepers.', clueRole: 'texture' },
  { id: 'texture-13', date: '2011-04-21T11:42:00-05:00', author: 'cl_marc', kind: 'notice', title: 'FTP maintenance complete', body: 'Passive FTP ports are back in rotation. Two interrupted uploads were left as .part files and were not promoted over the previous copies.', clueRole: 'texture', relationship: 'Copperline network staff' },
  { id: 'texture-14', date: '2012-01-20T22:16:00-06:00', author: 'ryan88', kind: 'post', title: 'Chicago latency tonight', body: 'Seeing about 110ms from Ohio instead of the usual 35. Support says an upstream route is being worked on.', clueRole: 'texture' },
  { id: 'texture-15', date: '2012-05-14T14:08:00-05:00', author: 'craftdad', kind: 'post', title: 'permissions after the update', body: 'Stop the server before replacing groups.yml or the panel writes the old one back. This post is for future me, who will otherwise forget again.', clueRole: 'texture' },
  { id: 'texture-16', date: '2014-11-03T10:00:00-06:00', author: 'cl_amy', kind: 'notice', title: 'archive desk closing', body: 'Replies and replacement uploads are disabled. Existing public posts, ticket links, and retained checksums remain read-only.', clueRole: 'texture', relationship: 'final Copperline archive notice' },
  { id: 'p4-ticket', date: '2011-02-09T00:31:00-06:00', author: 'mkept', kind: 'ticket', title: 'Ticket 2184: recovered cartridge order', body: 'Please keep the cartridge sequence in the attachment table. The filenames and modified times came from the damaged guest and cannot order the copies. Cartridge 03 was imaged before 04; that is the only sequence I trust.', clueRole: 'direct', relationship: 'C01 backup practice becomes P4 provenance key' },
  { id: 'p4-ticket-reply', date: '2011-02-09T08:12:00-06:00', author: 'cl_amy', kind: 'reply', title: 'Ticket 2184 staff reply', body: 'Sequence 03 then 04 is retained from the barcode scan. Neither image was mounted by support. I added the node-clock extract you requested; chi-ret-2 stayed within two seconds of the billing host during both reads.', clueRole: 'direct', relationship: 'support corroborates custody without interpreting ancient content' },
  { id: 'p4-diff', date: '2011-02-09T08:14:00-06:00', author: 'Copperline Archive', kind: 'attachment', title: 'mouth_notice.compare.txt', body: '03: IF SMOKE HOLDS, EAST HALL BEFORE SECOND BELL. TAKE THE SMALL ONES; CHECK THE NEXT DOOR.\n04: ALL HOUSEHOLDS SHALL ATTEND EAST HALL BEFORE SECOND BELL. ABSENCE SHALL BE ENTERED TO THE HOUSEHOLD.', clueRole: 'direct', relationship: 'read-only byte comparison, not a staff-authored clue' },
  { id: 'p4-clock', date: '2011-02-09T08:15:00-06:00', author: 'Copperline Archive', kind: 'attachment', title: 'chi-ret-2.clock-and-read.log', body: '00:12:08 cartridge 03 read complete; node offset +01.4s. 00:19:41 unrelated service 1174 backup complete; node offset +01.6s. 00:27:03 cartridge 04 read complete; node offset +01.5s. Preserved guest entry on 03 carries the correction later entered on 04.', clueRole: 'direct', relationship: 'ordinary host telemetry eliminates a simple clock-failure theory' },
  { id: 'p4-index', date: '2011-02-09T08:16:00-06:00', author: 'mkept', kind: 'attachment', title: 'read-extract.index.txt', body: 'Use the six-page read extract as page-line-word: 3-1-5 / 2-1-6 / 6-3-1. I wrote this after the filenames stopped agreeing. The result names a relationship to test; it is not the case conclusion.', clueRole: 'direct', relationship: 'restored indexing note crosses into the physical copy-office book' },
] as const;

export const copperlineP4DirectEntries = copperlineP4Entries.filter((entry) => entry.clueRole === 'direct');
export const copperlineP4TextureEntries = copperlineP4Entries.filter((entry) => entry.clueRole !== 'direct');

export const P4_COPPERLINE_OFFLINE_NOTICE =
  'Offline authored fixture for M3 review. This page is not a production deployment, public archive receipt, or proof of historical hosting.';
