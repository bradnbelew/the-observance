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

export const MORROW_MEDIA_CATALOG = Object.freeze(
  catalog.assets.map((asset) => Object.freeze(asset as MorrowMediaAsset)),
);

const BY_KEY = new Map(MORROW_MEDIA_CATALOG.map((asset) => [asset.key, asset]));

export function morrowMediaByKey(key: string): MorrowMediaAsset | undefined {
  return BY_KEY.get(key);
}

export function morrowMediaIndex(key: string): number {
  return MORROW_MEDIA_CATALOG.findIndex((asset) => asset.key === key);
}
