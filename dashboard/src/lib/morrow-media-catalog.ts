import catalog from '../../../morrow/contracts/media-catalog.json';

export type MorrowMediaAsset = {
  key: string;
  media_type: 'audio' | 'video' | 'image' | 'document' | 'voxel_diagram';
  prerequisite_events: string[];
  title: string;
  source: string;
  custody: string;
  summary: string;
  audio_file?: string;
  audio_sha256?: string;
  duration_ms?: number;
  transcript?: Array<{ at_ms: number; speaker: string; text: string }>;
  diagram_nodes?: string[];
  timing_labels: string[];
  accessible_equivalent: string;
  ending?: 'certify' | 'preserve_audit' | 'close_ticket' | 'create_new_branch';
};

export type MorrowMediaReviewAsset = {
  key: string;
  media_type: string;
  prerequisite_events: string[];
  title: string;
  source: string;
  purpose: string;
  required_observation: string;
  accessible_equivalent: string;
  status: 'to_be_authored';
  required_hash_before_release: true;
};

function isRuntimeAsset(asset: unknown): asset is MorrowMediaAsset {
  if (!asset || typeof asset !== 'object') return false;
  const candidate = asset as Record<string, unknown>;
  return typeof candidate.key === 'string'
    && typeof candidate.custody === 'string'
    && typeof candidate.summary === 'string'
    && Array.isArray(candidate.timing_labels);
}

function isReviewAsset(asset: unknown): asset is MorrowMediaReviewAsset {
  if (!asset || typeof asset !== 'object') return false;
  const candidate = asset as Record<string, unknown>;
  return candidate.status === 'to_be_authored'
    && candidate.required_hash_before_release === true
    && typeof candidate.required_observation === 'string';
}

const CATALOG_ASSETS: readonly unknown[] = catalog.assets;

/** Runtime delivery stays fail-closed while the v2 catalog contains design-only assets. */
export const MORROW_MEDIA_CATALOG = Object.freeze(
  CATALOG_ASSETS.filter(isRuntimeAsset).map((asset) => Object.freeze(asset)),
);

/** Review-only entries are visible to local design surfaces but cannot be served as earned media. */
export const MORROW_MEDIA_REVIEW_CATALOG = Object.freeze(
  CATALOG_ASSETS.filter(isReviewAsset).map((asset) => Object.freeze(asset)),
);

const BY_KEY = new Map(MORROW_MEDIA_CATALOG.map((asset) => [asset.key, asset]));

export function morrowMediaByKey(key: string): MorrowMediaAsset | undefined {
  return BY_KEY.get(key);
}

export function morrowMediaIndex(key: string): number {
  return MORROW_MEDIA_CATALOG.findIndex((asset) => asset.key === key);
}
