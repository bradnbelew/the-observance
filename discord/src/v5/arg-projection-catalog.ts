/**
 * Exact Discord readbacks for earned campaign events.
 *
 * These lines report a world change or invite investigation after an event has been earned. They
 * never contain a submitted payload, a hidden answer, or a generic "correct" response. Keeping
 * the catalog keyed by open-ended event ids does not define a puzzle taxonomy; future cases may
 * add any authored event and its own consequence.
 */
export const DISCORD_EVENT_MESSAGES = Object.freeze({
  'p1.attachment_history_restored': 'Copperline archive notice: ticket 2184 now shows both the posted attachment and the retained copy. The two entries have separate dates and custody notes.',
  'p1.mkept_intent_authenticated': 'Custody update: mkept\'s retained server copy has a verified preservation chain. This proves a deliberate save, not who damaged the source.',
  'p2.artifact_authenticated': 'Archive update: the recovered artifact matches the retained copy. Its original location and later handler are still separate questions.',
  'p2.live_runtime_handoff': 'Private host notice: the authenticated runtime is answering again. Copperline has preserved the earlier outage record beside the new handoff.',
  'p3.resident_accounts_opened': 'Field docket: resident accounts are open for comparison. They disagree on dates and names; none is being treated as the official version.',
  'p3.dispatch_authorized': 'Dispatch accepted. Aro and Dob keep both accounts in the field copy, Pell marks his date as memory rather than proof, and Copperline restores mkept\'s old-copy post to the archive index.',
  'p4.mouth_revision_restored': 'Copperline archive diff: an older Mouth revision has been restored beside the public copy. It changes who issued the procedure and when.',
  'p4.copy_hypothesis_tested': 'Test result filed: the Hold reacted to the selected copy test. The archive and the threshold now preserve different parts of the result.',
  'p4.control_reversal_earned': 'Community note: the Hold was built to shelter people. The same practical routines were later used to control them. The record now keeps both facts together.',
  'p5.service_chronology_shared': 'Public chronology updated: household repairs, school closures, water shifts, and watch changes can now be read on one shared line.',
  'p5.civic_gallery_recurated': 'Settlement notice: the civic gallery has been changed by the recovered household record. The old official caption remains available in its revision history.',
  'p6.professional_models_recovered': 'Records desk: six distinct working methods have been recovered. They belong to six people with different duties, not six versions of one office.',
  'p6.six_responsibilities_acknowledged': 'Public record amended: the six Keepers are named with the people and work each one affected. Their corrections do not erase their failures.',
  'p7.counterfeit_material_proven': 'Cistern inquiry: the installed material does not match the approved sample. The physical test is logged separately from the supplier history.',
  'p7.supplier_history_restored': 'Copperline mirror restored: the supplier file now includes the removed revision and its attachment chain. That chain can be compared with the Hold sample.',
  'p7.nessa_publicly_cleared': 'Settlement correction: Nessa did not substitute the cistern material. Her removal and Toma\'s replacement shift remain part of the public record.',
  'p8.intervention_plan_accepted': 'Repair board: the proposed intervention is accepted as a testable model. The Hold will preserve what changes and what refuses to change.',
  'p8.unlit_house_synthesis_completed': 'Unlit field note: all seven copied houses and the base comparison are complete. Their findings remain separately replayable and are not source-touch prerequisites for the causal report.',
  'p8.hold_systems_repaired': 'Works notice: water and drainage are moving through the repaired route. One measured anomaly remains and has not been explained away.',
  'p9.company_biographies_restored': 'Copperline memorial updated: mkept, Ash, Rook, and Wren now have separate account histories, work, jokes, and unfinished plans.',
  'p9.leak_window_proven': 'Camp inquiry: the leak window is now fixed by custody and opportunity. A witness spool has been released for review; it is evidence, not a confession.',
  'p10.player_copy_proof': 'Unlit test: the bounded arrangement made by the group returned as an altered copy. The original and returned states are both preserved for comparison.',
  'p10.wren_confronted': 'Contact record: Wren has received the evidence that fixes the leak window. His response adds context but does not replace the proof.',
  'p10.wren_remembrance_committed': 'Memorial decision recorded: the group\'s treatment of Wren is now reflected in the private archive. Campaign completion does not depend on one moral judgment.',
  'p11.averyn_identified': 'Identity correction: Averyn is recorded as a registrar and witness, not a seventh Keeper. The six routes back to her remain evidence of responsibility.',
  'p11.averyn_restored_unbound': 'Registry update: Averyn\'s name and work have been restored without filling the binding slot that erased her.',
  'p12.release_configuration_ready': 'Release audit: the physical configuration is ready. Final custody, name treatment, and operator authority remain visible as separate checks.',
  'p12.name_treatment_committed': 'Release audit: the group\'s chosen treatment of the name is committed and replayable. It does not change whether Averyn is released.',
  'p12.record_closed_averyn_released': 'The private record is closed. Averyn is released. The settlement, Copperline archive, and Hold now retain the consequences of the group\'s choices.',
} as const satisfies Readonly<Record<string, string>>);

export type DiscordProjectionEventKey = keyof typeof DISCORD_EVENT_MESSAGES;

export function discordProjectionMessage(eventKey: string): string | null {
  return Object.prototype.hasOwnProperty.call(DISCORD_EVENT_MESSAGES, eventKey)
    ? DISCORD_EVENT_MESSAGES[eventKey as DiscordProjectionEventKey]
    : null;
}
