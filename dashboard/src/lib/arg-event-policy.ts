export const ARG_SURFACES = [
  'minecraft',
  'copperline',
  'discord',
  'dashboard',
  'media',
  'npc',
] as const;

export type ArgSurface = (typeof ARG_SURFACES)[number];

export type ArgEventDefinition = {
  phase: `P${number}`;
  prerequisites: readonly string[];
  sourceSurfaces: readonly ArgSurface[];
  projectionSurfaces: readonly ArgSurface[];
  automation: 'A0' | 'A1';
};

/**
 * Open-ended story event catalog. These rows identify authored campaign events; they are not a
 * mechanism taxonomy and never decide how an event is earned. Exact platform predicates remain in
 * their owning Paper, web, Discord, or media adapters.
 */
export const ARG_EVENT_DEFINITIONS = {
  'p1.attachment_history_restored': event('P1', [], ['copperline'], ['copperline', 'discord']),
  'p1.mkept_intent_authenticated': event('P1', ['p1.attachment_history_restored'], ['copperline', 'discord'], ['copperline', 'discord']),
  'p2.artifact_authenticated': event('P2', ['p1.mkept_intent_authenticated'], ['copperline', 'discord'], ['copperline', 'discord', 'dashboard']),
  'p2.live_runtime_handoff': event('P2', ['p2.artifact_authenticated'], ['minecraft', 'copperline'], ['minecraft', 'copperline', 'discord']),
  'p3.resident_accounts_opened': event('P3', ['p2.live_runtime_handoff'], ['minecraft', 'npc'], ['minecraft', 'npc', 'discord']),
  'p3.dispatch_authorized': event('P3', ['p3.resident_accounts_opened'], ['minecraft', 'discord'], ['minecraft', 'copperline', 'discord', 'npc']),
  'p4.mouth_revision_restored': event('P4', ['p3.dispatch_authorized'], ['copperline'], ['copperline', 'minecraft', 'discord']),
  'p4.copy_hypothesis_tested': event('P4', ['p4.mouth_revision_restored'], ['minecraft', 'discord'], ['minecraft', 'copperline', 'discord', 'npc']),
  // A correct conclusion is accepted after phase entry even when its expected sources/tests were
  // never touched. The restored revision and chosen test make the deduction fair; they never gate it.
  'p4.control_reversal_earned': event('P4', ['p3.dispatch_authorized'], ['minecraft'], ['minecraft', 'copperline', 'discord', 'npc']),
  'p5.service_chronology_shared': event('P5', ['p4.control_reversal_earned'], ['minecraft'], ['minecraft', 'copperline', 'discord', 'npc']),
  'p5.civic_gallery_recurated': event('P5', ['p5.service_chronology_shared'], ['minecraft'], ['minecraft', 'copperline', 'discord', 'npc']),
  'p6.professional_models_recovered': event('P6', ['p5.civic_gallery_recurated'], ['minecraft'], ['minecraft', 'copperline', 'discord', 'npc']),
  'p6.six_responsibilities_acknowledged': event('P6', ['p6.professional_models_recovered'], ['minecraft'], ['minecraft', 'copperline', 'discord', 'npc']),
  'p7.counterfeit_material_proven': event('P7', ['p6.six_responsibilities_acknowledged'], ['minecraft'], ['minecraft', 'copperline', 'discord', 'npc']),
  'p7.supplier_history_restored': event('P7', ['p7.counterfeit_material_proven'], ['copperline'], ['copperline', 'minecraft', 'discord']),
  // A correct exoneration is knowledge, not proof that expected exhibits or restore buttons were touched.
  'p7.nessa_publicly_cleared': event('P7', ['p6.six_responsibilities_acknowledged'], ['minecraft', 'discord', 'npc'], ['minecraft', 'copperline', 'discord', 'npc']),
  'p8.intervention_plan_accepted': event('P8', ['p7.nessa_publicly_cleared'], ['minecraft', 'copperline'], ['minecraft', 'copperline', 'discord', 'npc']),
  'p8.unlit_house_synthesis_completed': event('P8', ['p7.nessa_publicly_cleared'], ['minecraft'], ['minecraft', 'copperline', 'discord', 'npc']),
  'p8.hold_systems_repaired': event('P8', ['p8.intervention_plan_accepted', 'p8.unlit_house_synthesis_completed'], ['minecraft'], ['minecraft', 'copperline', 'discord', 'npc']),
  'p9.company_biographies_restored': event('P9', ['p8.hold_systems_repaired'], ['minecraft', 'copperline', 'discord'], ['minecraft', 'copperline', 'discord', 'media']),
  'p9.leak_window_proven': event('P9', ['p9.company_biographies_restored'], ['minecraft', 'copperline', 'media'], ['minecraft', 'copperline', 'discord', 'media', 'npc']),
  'p10.player_copy_proof': event('P10', ['p9.leak_window_proven'], ['minecraft'], ['minecraft', 'copperline', 'discord', 'dashboard']),
  'p10.wren_confronted': event('P10', ['p9.leak_window_proven'], ['minecraft', 'discord', 'npc'], ['minecraft', 'copperline', 'discord', 'npc']),
  'p10.wren_remembrance_committed': event('P10', ['p10.wren_confronted'], ['minecraft', 'discord'], ['minecraft', 'copperline', 'discord', 'npc']),
  'p11.averyn_identified': event('P11', ['p10.wren_remembrance_committed'], ['minecraft', 'discord', 'media'], ['minecraft', 'copperline', 'discord', 'npc']),
  'p11.averyn_restored_unbound': event('P11', ['p11.averyn_identified'], ['minecraft'], ['minecraft', 'copperline', 'discord', 'npc']),
  'p12.release_configuration_ready': event('P12', ['p11.averyn_restored_unbound'], ['minecraft'], ['minecraft', 'discord', 'dashboard']),
  'p12.name_treatment_committed': event('P12', ['p12.release_configuration_ready'], ['minecraft'], ['minecraft', 'copperline', 'discord', 'dashboard', 'npc']),
  'p12.record_closed_averyn_released': event('P12', ['p12.name_treatment_committed'], ['minecraft'], ['minecraft', 'copperline', 'discord', 'dashboard', 'npc']),
} as const satisfies Record<string, ArgEventDefinition>;

export type ArgEventKey = keyof typeof ARG_EVENT_DEFINITIONS;

function event(
  phase: `P${number}`,
  prerequisites: readonly string[],
  sourceSurfaces: readonly ArgSurface[],
  projectionSurfaces: readonly ArgSurface[],
): ArgEventDefinition {
  return { phase, prerequisites, sourceSurfaces, projectionSurfaces, automation: 'A1' };
}

export function isArgEventKey(value: string): value is ArgEventKey {
  return Object.hasOwn(ARG_EVENT_DEFINITIONS, value);
}

export function canRecordArgEvent(eventKey: ArgEventKey, surface: ArgSurface): boolean {
  return ARG_EVENT_DEFINITIONS[eventKey].sourceSurfaces.includes(surface);
}
