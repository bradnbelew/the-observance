import {
  LS02_DOCKET_CIPHER,
  OBSERVANCE_DIRECTORY_KEY,
} from './v5-ls02-docket';
import { isCorrectLs02DocketAnswer, normalizeLs02DocketAnswer } from './v5-ls02-answer';
import { publicServers } from './legacy-content';

function check(condition: boolean, message: string): void {
  if (!condition) throw new Error(`V5 LS02 docket self-test failed: ${message}`);
}

for (const accepted of [
  '1842',
  'service 1842',
  ' SERVICE ONE EIGHT FOUR TWO ',
  'service-one/eight.four_two',
  'ＳＥＲＶＩＣＥ １８４２',
]) {
  check(isCorrectLs02DocketAnswer(accepted), `normal accepted form was rejected: ${accepted}`);
}

for (const rejected of [
  '',
  '1843',
  'ticket 1842',
  'service 1842 extra',
  'xservice one eight four two',
  LS02_DOCKET_CIPHER,
]) {
  check(!isCorrectLs02DocketAnswer(rejected), `non-answer was accepted: ${rejected}`);
}

check(normalizeLs02DocketAnswer('one eight four two') === '1842', 'number words must normalize exactly');
check(!OBSERVANCE_DIRECTORY_KEY.includes('1842'), 'the directory route must not disclose the answer');

const observance = publicServers.find((server) => server.recoveredDocket);
check(observance?.id === OBSERVANCE_DIRECTORY_KEY, 'the damaged public row must use the opaque directory key');
check(observance?.listingLabel === 'Docket reference damaged', 'the public row must describe damage, not print the number');
check(
  publicServers.every((server) => server.id !== '1842' && server.listingLabel !== '1842'),
  'the public directory must not contain the LS02 answer',
);

console.log('V5 LS02 docket self-test: OK - encoded route opaque, normalization exact, bypass and substring forms denied');
