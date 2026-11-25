// Run from project root: node scripts/checks/check_spa_security_levels.js
'use strict';
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const path = require('node:path');

// Minimal DOM implementation to exercise the actual rendering functions.
class Element {
  constructor(tag) { this.tag = tag; this.children = []; this.className = ''; this.textContent = ''; this.style = {}; }
  append(...children) { this.children.push(...children); }
  replaceChildren(...children) { this.children = children; }
  setAttribute() {}
  addEventListener() {}
}
const elements = new Map();
const document = {
  createElement: tag => new Element(tag),
  getElementById: id => {
    if (!elements.has(id)) elements.set(id, new Element('div'));
    return elements.get(id);
  },
  addEventListener() {}
};
const context = vm.createContext({document});
const source = path.resolve(__dirname, '../../src/test/java/utilities/reporting/spa/assets/app.js');
vm.runInContext(fs.readFileSync(source, 'utf8'), context);
const execute = code => vm.runInContext(code, context);
const byClass = (element, name) => [
  ...(element.className.split(' ').includes(name) ? [element] : []),
  ...element.children.flatMap(child => byClass(child, name))
];
const byTag = (element, tag) => [
  ...(element.tag === tag ? [element] : []),
  ...element.children.flatMap(child => byTag(child, tag))
];
const runs = [
  {securityLevel:'LOW', status:'PASSED', screenshots:['/api/screenshots/example.png']},
  {securityLevel:'low', status:'FAILED', error:'Example failure'},
  {securityLevel:'Medium', status:'SKIPPED'},
  {securityLevel:' HIGH ', status:'PASSED'},
  {securityLevel:'IMPOSSIBLE', status:'PASSED'},
  {status:'FAILED'},
  {securityLevel:'UNKNOWN', status:'PASSED'}
].map(run => ({className:'ExampleTest', method:'example', suite:'Example', durationMs:12, ...run}));
context.runs = runs;
const grid = execute('makeSecurityLevelGrid(runs)');
const cards = byClass(grid, 'security-level-card');
assert.equal(cards.length, 5);
assert.deepEqual(cards.map(card => byTag(card, 'h3')[0].textContent), ['Low','Medium','High','Impossible','Unclassified']);
assert.deepEqual(cards.map(card => byClass(card, 'result-card').length), [2,1,1,1,2]);
assert.equal(byClass(cards[0], 'screenshot').length, 1);
assert.equal(byClass(cards[0], 'code-evidence')[0].textContent, 'Example failure');
assert.deepEqual(byClass(cards[0], 'test-tile-status').map(el => el.children[0].textContent), ['1','1','0']);
const empty = execute('makeSecurityLevelGrid([])');
assert.equal(byClass(empty, 'security-level-card').length, 4);
assert.equal(byClass(empty, 'security-level-empty').length, 4);
document.getElementById('test-search').value = '';
document.getElementById('test-status').value = 'ALL';
execute('state.data = {tests:runs}; state.selectedTestClass="ExampleTest"; state.targetId="dvwa"; renderTests()');
const list = document.getElementById('tests-list');
assert.equal(byClass(list, 'security-level-card').length, 5);
document.getElementById('test-status').value = 'FAILED';
execute('renderTests()');
assert.equal(byClass(list, 'result-card').length, 2);
document.getElementById('test-status').value = 'ALL';
document.getElementById('test-search').value = 'medium';
execute('renderTests()');
assert.equal(byClass(list, 'result-card').length, 1);
document.getElementById('test-search').value = '';
execute('state.targetId="juiceshop"; renderTests()');
assert.equal(byClass(list, 'security-level-card').length, 0);
assert.equal(byClass(list, 'result-card').length, runs.length);
console.log('SPA security level checks passed.');
