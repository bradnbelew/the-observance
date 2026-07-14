export const V5_CASE_BUDGET = [
  { key: 'C01', title: 'The Lost Server', nodes: 6 },
  { key: 'C02', title: 'The Long Cold', nodes: 6 },
  { key: 'C03', title: 'Keeper Dossiers', nodes: 18 },
  { key: 'C04', title: 'Cistern Winter', nodes: 8 },
  { key: 'C05', title: 'Break Inquest', nodes: 8 },
  { key: 'C06', title: 'Restoring the Hold', nodes: 7 },
  { key: 'C07', title: 'ASH-13 Company', nodes: 10 },
  { key: 'C08', title: "Wren's Betrayal", nodes: 5 },
  { key: 'C09', title: 'Averyn and the Unwriting', nodes: 8 },
  { key: 'C10', title: 'Release Protocol', nodes: 6 },
] as const;

export const V5_TOTAL_NODES = V5_CASE_BUDGET.reduce((sum, item) => sum + item.nodes, 0);

export const UNIVERSAL_GOODBYE = [
  'i have your names.',
  'i am giving them back.',
  'the record is closed.',
  'the observance is over.',
  'thank you for coming back for us.',
  '— averyn',
] as const;
