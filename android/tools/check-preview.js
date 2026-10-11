#!/usr/bin/env node
/*
 * Fails when a preview page references something the generated layer does not
 * define. Without this, a typo'd var() or a missing .R- class renders as
 * browser defaults and the preview quietly lies about the design.
 */
'use strict';
const fs = require('fs');
const path = require('path');

const DIR = path.resolve(__dirname, '..', 'preview');
const tokens = fs.readFileSync(path.join(DIR, 'tokens.css'), 'utf8');
const icons = fs.readFileSync(path.join(DIR, 'icons.js'), 'utf8');
const shared = fs.readFileSync(path.join(DIR, 'app.css'), 'utf8');

const definedVars = new Set([...tokens.matchAll(/(--[\w-]+)\s*:/g)].map(m => m[1]));
// app.css may declare its own parameterised vars (e.g. --pct on .ring), so those
// count as defined too.
for (const m of shared.matchAll(/(--[\w-]+)\s*:/g)) definedVars.add(m[1]);
// Every class token appearing in any selector, so .nav-item.on defines both
// "nav-item" and "on" without a hand-kept allowlist.
const definedCls = new Set();
for (const css of [tokens, shared]) {
  for (const sel of css.matchAll(/([^{}]+)\{/g)) {
    for (const c of sel[1].matchAll(/\.([\w-]+)/g)) definedCls.add(c[1]);
  }
}
const definedIds = new Set([...icons.matchAll(/symbol id="([\w-]+)"/g)].map(m => m[1]));

let bad = 0;
for (const m of shared.matchAll(/var\((--[\w-]+)[),]/g)) {
  if (!definedVars.has(m[1])) { console.log(`app.css: undefined token var(${m[1]})`); bad++; }
}

for (const f of fs.readdirSync(DIR).filter(n => n.endsWith('.html'))) {
  const src = fs.readFileSync(path.join(DIR, f), 'utf8');
  // Classes defined in the page's own <style> blocks count as defined.
  const page = new Set();
  for (const block of src.matchAll(/<style>([\s\S]*?)<\/style>/g)) {
    for (const sel of block[1].matchAll(/([^{}]+)\{/g)) {
      for (const c of sel[1].matchAll(/\.([\w-]+)/g)) page.add(c[1]);
    }
  }

  for (const m of src.matchAll(/var\((--[\w-]+)[),]/g)) {
    if (!definedVars.has(m[1])) { console.log(`${f}: undefined token var(${m[1]})`); bad++; }
  }
  for (const m of src.matchAll(/class="([^"]+)"/g)) {
    for (const c of m[1].trim().split(/\s+/)) {
      if (!c || definedCls.has(c) || page.has(c)) continue;
      console.log(`${f}: class "${c}" is in no stylesheet`); bad++;
    }
  }
  for (const m of src.matchAll(/use href="#([\w-]+)"/g)) {
    if (!definedIds.has(m[1])) { console.log(`${f}: no icon "#${m[1]}" in icons.js`); bad++; }
  }
}
if (bad) {
  console.log(`${bad} broken reference(s)`);
  process.exit(1);
}
console.log('OK: every var(), class and icon id used by preview/*.html is defined');
