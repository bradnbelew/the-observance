import {
  V5_WEBSITE_NODE_KEYS,
  isAllowedV5WebsiteSequence,
  isV5WebsiteNodeKey,
} from './v5-web-node-policy';

function check(condition: boolean, message: string): void {
  if (!condition) throw new Error(`V5 web-node policy self-test failed: ${message}`);
}

check(
  JSON.stringify([...V5_WEBSITE_NODE_KEYS].sort())
    === JSON.stringify(['A06', 'A07', 'LS01', 'LS02', 'LS03', 'LS04'].sort()),
  'the exact six dedicated website nodes must remain owned by the web surface',
);
for (const nodeKey of V5_WEBSITE_NODE_KEYS) {
  check(isV5WebsiteNodeKey(nodeKey), `${nodeKey} must remain website-owned`);
  check(isAllowedV5WebsiteSequence([nodeKey]), `${nodeKey} must remain recordable by its dedicated route`);
}

check(!isV5WebsiteNodeKey('LC05'), 'a Discord conclusion must not be writable by the website');
check(!isV5WebsiteNodeKey('LC01'), 'a physical Minecraft node must not be writable by the website');
check(!isV5WebsiteNodeKey('LS07'), 'a future/unowned node must fail closed');
check(!isAllowedV5WebsiteSequence([]), 'an empty sequence must fail closed');
check(!isAllowedV5WebsiteSequence(['LS01', 'LS01']), 'duplicate receipt sequences must fail closed');
check(!isAllowedV5WebsiteSequence(['LS01', 'LC05']), 'mixed website/Discord sequences must fail closed');

console.log('V5 web-node policy self-test: OK - six exact web nodes; Discord, physical, future, and duplicate bypasses denied');
