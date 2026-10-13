// mrrw-01 trailhead e2e, alongside Marked on the QA server (offline mode, port 25580, RCON 25581).
// Proves: dormant until /trailhead start; server-list hover; knock; [Recovery] line; key books
// reassemble; operators get no sector (the owner never holds a piece); no inventory is touched;
// stop restores the server list.
import { execFileSync } from 'node:child_process'
import mineflayer from 'mineflayer'
import mc from 'minecraft-protocol'

const host = '127.0.0.1', port = Number(process.env.MC_PORT ?? 25580)
const KEY = ['copperline hosting', 'case 6118', 'mossfield', 'records kept until', '31 dec 2026', 'copperlinehosting', '.com']
const RESIDENTS = ['cairn', 'juni_lantern', 'lowpath']
const sleep = ms => new Promise(r => setTimeout(r, ms))
let fails = 0
const check = (name, ok, detail = '') => { if (!ok) fails++; console.log(`[${ok ? 'PASS' : 'FAIL'}] ${name}${detail ? ': ' + String(detail).slice(0, 220) : ''}`) }
// PYTHONIOENCODING: on a cp1252 console python prints § as U+FFFD, which then survived the strip
const rcon = (...cmds) => execFileSync('python', ['tools/rcon.py', ...cmds], { cwd: 'D:/marked2-qa', encoding: 'utf8', env: { ...process.env, PYTHONIOENCODING: 'utf-8' } }).replace(/[§�]./g, '')
const sample = async () => { const r = await mc.ping({ host, port }); return { names: (r.players.sample ?? []).map(p => p.name), online: r.players.online } }

async function connect(name) {
  const bot = mineflayer.createBot({ host, port, username: name, auth: 'offline' })
  bot.heard = []
  bot.books = 0
  bot.on('messagestr', m => bot.heard.push(m))
  bot._client.on('open_book', () => bot.books++)
  await new Promise((res, rej) => { const t = setTimeout(() => rej(new Error('spawn timeout ' + name)), 40000); bot.once('spawn', () => { clearTimeout(t); res() }) })
  return bot
}
const items = bot => bot.inventory.items().map(i => i.name + 'x' + i.count).sort().join(',')

const bots = []
let opBot = null
try {
  rcon('trailhead stop', 'trailhead key reset', 'deop THOp')
  for (const n of ['THAlpha', 'THBravo', 'THCharlie']) { bots.push(await connect(n)); await sleep(500) }
  opBot = await connect('THOp')  // stands in for the server owner: an operator at the table
  rcon('op THOp')
  await sleep(2000)

  // dormant
  let s = await sample()
  check('dormant: server list shows only real players', !RESIDENTS.some(r => s.names.includes(r)), JSON.stringify(s))
  check('dormant: commands refuse', /dormant/.test(rcon('trailhead speak')))
  bots[0].chat('ok who was at the farm when the lights went out')
  await sleep(1500)
  check('dormant: no [Recovery] line', !bots.some(b => b.heard.some(m => m.includes('[Recovery]'))))

  // armed
  check('start arms', /armed/.test(rcon('trailhead start')))
  s = await sample()
  check('armed: hover lists the 2011 residents', RESIDENTS.every(r => s.names.includes(r)), JSON.stringify(s))
  check('armed: real players still listed', bots.every(b => s.names.includes(b.username)), JSON.stringify(s))
  check('armed: player count includes residents', s.online >= bots.length + RESIDENTS.length, s.online)

  const before = bots.map(items)
  rcon('trailhead knock lowpath')
  await sleep(5500)
  check('knock: joined then left', bots[1].heard.some(m => /lowpath joined the game/.test(m)) && bots[1].heard.some(m => /lowpath left the game/.test(m)),
    bots[1].heard.filter(m => /lowpath/.test(m)).join(' | '))

  bots[1].chat('i was at the farm the whole time i swear')
  await sleep(800)
  const preview = rcon('trailhead preview')
  check('preview goes only to the operator', /preview/.test(preview) && !bots.some(b => b.heard.some(m => m.includes('[Recovery]'))), preview.split('\n')[1])
  const previewed = (preview.match(/\[Recovery\] (.*)/) ?? [])[1]
  rcon('trailhead speak')
  await sleep(1000)
  const said = bots[2].heard.find(m => m.startsWith('[Recovery] '))
  check('speak posts the previewed line to everyone', said && said === '[Recovery] ' + previewed, said)
  check('the line has no numbers or answers', said && !/\d|bracken|biscuit|haltmrrw/i.test(said), said)

  const keyReply = rcon('trailhead key all')
  await sleep(1500)
  check('key all leaves the operator out', opBot.books === 0 && !opBot.heard.some(m => /recovered sector/.test(m)) && /operators left out: \[THOp\]/.test(keyReply),
    keyReply.trim())
  const sectors = []
  for (const b of bots) {
    const i = b.heard.findIndex(m => /recovered sector \d of 3/.test(m))
    sectors.push(i < 0 ? null : b.heard.slice(i + 1, i + 1 + KEY.length).map(l => l.replace(/^\s+/, '')))
    check(`${b.username}: book opened`, b.books === 1, b.books)
  }
  check('each player got a sector', sectors.every(Boolean))
  if (sectors.every(Boolean)) {
    const overlay = KEY.map((_, l) => [...sectors[0][l]].map((c, j) => sectors.map(sec => sec[l][j]).find(x => x !== '_') ?? '_').join(''))
    check('the three sectors overlay into the key', JSON.stringify(overlay) === JSON.stringify(KEY), overlay.join(' / '))
    check('no sector reads the domain alone', sectors.every(sec => !sec.join(' ').includes('copper')))
  }
  check('inventories untouched', JSON.stringify(bots.map(items)) === JSON.stringify(before), JSON.stringify(bots.map(items)))

  const resend = bots[0].heard.filter(m => /recovered sector/.test(m)).length
  rcon('trailhead key THAlpha')
  await sleep(800)
  const again = bots[0].heard.filter(m => /recovered sector/.test(m))
  check('resend gives the same sector', again.length === resend + 1 && again.at(-1) === again.at(-2), again.slice(-2).join(' | '))

  check('stop disarms', /dormant/.test(rcon('trailhead stop')))
  s = await sample()
  check('stopped: server list back to normal', !RESIDENTS.some(r => s.names.includes(r)), JSON.stringify(s))
} catch (e) {
  check('run', false, e.stack)
} finally {
  rcon('trailhead stop', 'trailhead key reset', 'deop THOp')
  for (const b of bots) b.quit()
  if (opBot) opBot.quit()
  await sleep(500)
  console.log(fails === 0 ? 'ALL TRAILHEAD CHECKS PASSED' : `${fails} FAILED`)
  process.exit(fails ? 1 : 0)
}
