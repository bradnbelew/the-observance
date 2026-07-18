export const EXPECTED_CAMP_TRACES = Object.freeze({
  mkept: 'admin-custody',
  ash: 'camera-humor',
  rook: 'builder-countermark',
  wren: 'route-companion',
});

export type CampTraceMapping = Record<keyof typeof EXPECTED_CAMP_TRACES, string>;

/** Returns owner-card names only; it never exposes the expected relationship trace. */
export function unsupportedOwnerCards(mapping: CampTraceMapping): string[] {
  return (Object.keys(EXPECTED_CAMP_TRACES) as Array<keyof typeof EXPECTED_CAMP_TRACES>)
    .filter((person) => mapping[person] !== EXPECTED_CAMP_TRACES[person]);
}
