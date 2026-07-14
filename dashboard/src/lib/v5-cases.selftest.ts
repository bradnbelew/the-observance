import { UNIVERSAL_GOODBYE, V5_CASE_BUDGET, V5_TOTAL_NODES } from './v5-cases';

if (V5_CASE_BUDGET.length !== 10) throw new Error('V5 case budget must contain 10 cases');
if (V5_TOTAL_NODES !== 82) throw new Error(`V5 case budget must total 82 nodes, found ${V5_TOTAL_NODES}`);
if (new Set(V5_CASE_BUDGET.map((item) => item.key)).size !== 10) throw new Error('V5 case keys must be unique');
if (!UNIVERSAL_GOODBYE.join('\n').includes('the record is closed.')) throw new Error('V5 goodbye is incomplete');

console.log('dashboard V5 canon selftest OK');
