import { createHash } from 'node:crypto';
import ledger from './morrow-data/puzzle-ledger.json';
import events from './morrow-data/event-catalog.json';
import states from './morrow-data/relationship-states.json';
import surfaces from './morrow-data/surface-contracts.json';
import media from './morrow-data/media-catalog.json';
import director from './morrow-data/director-command-schema.json';
import fixtures from './morrow-data/review-fixtures.json';

export const MORROW_FULL_REHEARSAL_RELEASE = 'morrow.local.full-spine.v1';
export const MORROW_FULL_REHEARSAL_CAMPAIGN = 'local.contract.mossfield.full';

export type GateRecord = (typeof ledger.gates)[number];
export type DiscoveryRecord = (typeof ledger.discoveries)[number];
export type EventRecord = (typeof events.events)[number];
export type MediaRequirement = (typeof media.assets)[number] & {
  gates: string[];
  releaseBlocking: boolean;
};

export const MORROW_MEDIA_INTAKE_REQUIRED_FIELDS = [
  'source_file',
  'delivery_file',
  'source_sha256',
  'delivery_sha256',
  'custody_note',
  'created_at',
  'author_identity',
  'accessibility_equivalent_file',
  'safety_review',
] as const;

export type MediaIntakeAsset = MediaRequirement & {
  workFolder: string;
  deliveryRoot: string;
  accessibilityRoot: string;
  sourceFile: string;
  deliveryFile: string;
  accessibilityFile: string;
  releaseReady: false;
  requiredBeforeRelease: typeof MORROW_MEDIA_INTAKE_REQUIRED_FIELDS;
};

export type FullGate = GateRecord & {
  eventKey: string;
  owner: string;
  projectsTo: string[];
  discovery: DiscoveryRecord | null;
  location: string | null;
  dreadBeat: string | null;
  mediaKeys: string[];
};

export function allMorrowGates(): FullGate[] {
  return ledger.gates.map((gate) => {
    const event = events.events.find((candidate) => candidate.gate === gate.id);
    if (!event) throw new Error(`missing event for ${gate.id}`);
    const discovery = ledger.discoveries.find((candidate) => candidate.feeds_gate === gate.id) ?? null;
    const location = fixtures.minecraft_locations.find((candidate) => candidate.gates.includes(gate.id))?.id ?? null;
    const dreadBeat = fixtures.dread_score.find((beat) => fixtures.minecraft_locations.some((locationRow) => locationRow.gates.includes(gate.id) && locationRow.scare_anchors.includes(beat.id)))?.id ?? null;
    const mediaKeys = media.assets.filter((asset) => asset.prerequisite_events.includes(event.key) || asset.key.includes(gate.id.toLowerCase())).map((asset) => asset.key);
    return {
      ...gate,
      eventKey: event.key,
      owner: event.owner,
      projectsTo: [...event.projects_to],
      discovery,
      location,
      dreadBeat,
      mediaKeys,
    };
  });
}

export function gateById(gateId: string): FullGate | undefined {
  return allMorrowGates().find((gate) => gate.id.toLowerCase() === gateId.toLowerCase());
}

export function gateReceiptById(gateId: string) {
  return buildFullRehearsalReceipt().gateReceipts.find((receipt) => receipt.gate.toLowerCase() === gateId.toLowerCase());
}

export function mediaRequirements(): MediaRequirement[] {
  return media.assets.map((asset) => ({
    ...asset,
    gates: events.events
      .filter((event) => asset.prerequisite_events.includes(event.key) && event.gate !== null)
      .map((event) => event.gate as string),
    releaseBlocking: asset.status !== 'complete' || asset.required_hash_before_release,
  }));
}

export function mediaIntakeAssets(): MediaIntakeAsset[] {
  return mediaRequirements().map((asset) => ({
    ...asset,
    workFolder: `morrow/media/work/${asset.key}`,
    deliveryRoot: 'morrow/media/final',
    accessibilityRoot: 'morrow/media/accessibility',
    sourceFile: `morrow/media/work/${asset.key}/source`,
    deliveryFile: `morrow/media/final/${asset.key}`,
    accessibilityFile: `morrow/media/accessibility/${asset.key}.txt`,
    releaseReady: false,
    requiredBeforeRelease: MORROW_MEDIA_INTAKE_REQUIRED_FIELDS,
  }));
}

export function mediaIntakeSummary() {
  const assets = mediaIntakeAssets();
  return {
    schemaVersion: '1.1.0-morrow-media-readiness',
    status: 'media_assets_required_before_full_release',
    productionMutation: false,
    intakePolicy: {
      manifest: 'morrow/media/media-manifest.template.json',
      hashAlgorithm: 'sha256',
      workRoot: 'morrow/media/work',
      deliveryRoot: 'morrow/media/final',
      accessibilityRoot: 'morrow/media/accessibility',
      releaseReadyRequiresAllHashes: true,
      releaseReadyRequiresAccessibilityEquivalent: true,
      releaseReadyRequiresSafetyReview: true,
    },
    totalAssets: assets.length,
    releaseBlockingAssets: assets.filter((asset) => asset.releaseBlocking).length,
    releaseReadyAssets: assets.filter((asset) => asset.releaseReady).length,
    requiredFieldCount: MORROW_MEDIA_INTAKE_REQUIRED_FIELDS.length,
    assets,
  };
}

export function directorLockContract() {
  return {
    schemaVersion: director.schema_version,
    releaseId: director.release_id ?? MORROW_FULL_REHEARSAL_RELEASE,
    controlsEnabled: director.controls_enabled,
    productionMutationsAllowed: director.production_mutations_allowed,
    requiredReceiptFields: director.required_receipt_fields,
    commands: director.commands,
    forbidden: director.forbidden,
    lockReason: 'Morrow reboot rehearsal is local-only; director controls are described but disabled.',
  };
}

function stableJson(value: unknown): string {
  return JSON.stringify(value, Object.keys(value as Record<string, unknown>).sort());
}

function sha256(value: unknown): string {
  return createHash('sha256').update(JSON.stringify(value), 'utf8').digest('hex');
}

export function buildFullRehearsalReceipt() {
  let previous = '0'.repeat(64);
  const gateReceipts = allMorrowGates().map((gate, index) => {
    const payload = {
      gate: gate.id,
      title: gate.title,
      act: gate.act,
      owner: gate.owner,
      surfaces: gate.surfaces,
      projectsTo: gate.projectsTo,
      evidence: gate.evidence,
      input: gate.input,
      failureBehavior: gate.failure_behavior,
      accessibleEquivalent: gate.accessible_equivalent,
      discovery: gate.discovery?.id ?? null,
      mediaKeys: gate.mediaKeys,
      location: gate.location,
      dreadBeat: gate.dreadBeat,
      productionMutation: false,
    };
    const core = {
      sequence: index + 1,
      eventKey: gate.eventKey,
      gate: gate.id,
      idempotencyKey: `${MORROW_FULL_REHEARSAL_RELEASE}:${MORROW_FULL_REHEARSAL_CAMPAIGN}:${gate.id}`,
      payloadSha256: sha256(payload),
      previousReceiptSha256: previous,
    };
    const receiptSha256 = sha256(core);
    previous = receiptSha256;
    return { ...core, payload, receiptSha256 };
  });
  return {
    schemaVersion: '1.0.0-morrow-full-local-rehearsal',
    status: 'local_full_spine_contract_not_playable',
    releaseId: MORROW_FULL_REHEARSAL_RELEASE,
    campaignId: MORROW_FULL_REHEARSAL_CAMPAIGN,
    boundaries: {
      productionContacted: false,
      supabaseContacted: false,
      discordGatewayContacted: false,
      minecraftServerStarted: false,
      browserRequired: false,
      humanPlayabilityClaimed: false,
    },
    shape: ledger.campaign_shape,
    surfaceContracts: Object.keys(surfaces.surfaces),
    morrowStates: states.states.map((state) => state.key),
    gateReceipts,
    journalHead: previous,
    mediaRequired: media.assets.map((asset) => ({ key: asset.key, status: asset.status, requiredHashBeforeRelease: asset.required_hash_before_release })),
    directorControlsEnabled: fixtures.director_fixture.controls_enabled,
    productionMutationsAllowed: fixtures.production_mutations_allowed,
  };
}

export function fullRehearsalDigest(): string {
  return createHash('sha256').update(stableJson(buildFullRehearsalReceipt()), 'utf8').digest('hex');
}
