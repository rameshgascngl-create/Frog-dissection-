const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

const root = path.resolve(__dirname, '..');
const html = fs.readFileSync(path.join(root, 'index.html'), 'utf8');
const script = [...html.matchAll(/<script(?:\s[^>]*)?>([\s\S]*?)<\/script>/g)].map(m => m[1]).join('\n');

test('inline application JavaScript parses', () => {
  assert.ok(script.length > 1000, 'application script missing');
  assert.doesNotThrow(() => new vm.Script(script));
});

test('all five anatomical photographs are committed', () => {
  for (const name of ['frog external.jpg', 'frog muscle.jpg', 'frog viscera.jpg', 'frog heart.jpg',
    'Internal deep male.avif', 'Internal deep female.avif']) {
    assert.ok(fs.existsSync(path.join(root, name)), 'missing plate: ' + name);
  }
});

test('Layer 5 has independent male and female image selection', () => {
  assert.match(script, /const UROGENITAL_IMAGES\s*=\s*\{[^}]*male:\s*"Internal deep male\.avif"[^}]*female:\s*"Internal deep female\.avif"/);
  assert.match(script, /state\.layerImages\["5_" \+ state\.sex\]/);
  assert.match(script, /state\.sexCoords\[state\.sex\]/);
  assert.match(script, /frog_sex_coords_v1/);
});

test('male and female photo controls and descriptions exist', () => {
  assert.match(html, /id="btn-toggle-sex"/);
  assert.match(script, /state\.layer === 5/);
  assert.match(script, /name: "Paired Coiled Oviducts/);
  assert.match(script, /unplaced: true/);
  assert.match(script, /key === "oviducts" && state.sex !== "female"/);
  assert.match(script, /Testes/);
  assert.match(html, /id="calibrate-organ-select"/);
});

test('main photograph starts with a file that actually exists', () => {
  const tag = html.match(/<img\s+id="photo-specimen"[\s\S]*?>/);
  assert.ok(tag, 'main specimen image missing');
  const name = tag[0].match(/src="([^"]+)"/)?.[1];
  assert.ok(name, 'main image has no src');
  assert.ok(fs.existsSync(path.join(root, name)), 'missing main image: ' + name);
});
