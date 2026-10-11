#!/usr/bin/env node
// XML comments must not contain "--" (only legal as the closing "-->").
// Affects hand-written vector/shape XML; aapt reports it as a vague
// "not well-formed (invalid token)", so check it explicitly.
const fs = require('fs');
const path = require('path');

const roots = process.argv.slice(2);
if (!roots.length) roots.push('res', 'AndroidManifest.xml');

let bad = 0;
const seen = new Set();

function walk(p) {
  const st = fs.statSync(p);
  if (st.isDirectory()) {
    for (const f of fs.readdirSync(p)) walk(path.join(p, f));
    return;
  }
  if (!p.endsWith('.xml')) return;
  const src = fs.readFileSync(p, 'utf8');
  let i = 0;
  while ((i = src.indexOf('<!--', i)) >= 0) {
    const end = src.indexOf('-->', i + 4);
    if (end < 0) {
      if (!seen.has(p + ':unclosed')) {
        seen.add(p + ':unclosed');
        console.log(`${p}: unclosed comment`);
        bad++;
      }
      break;
    }
    const body = src.slice(i + 4, end);
    if (body.includes('--')) {
      const line = src.slice(0, i).split('\n').length;
      console.log(`${p}:${line}: comment body contains "--" -> ${body.trim().slice(0, 60)}`);
      bad++;
    }
    i = end + 3;
  }
}

for (const r of roots) {
  if (fs.existsSync(r)) walk(r);
}
console.log(bad ? `FAIL: ${bad} invalid XML comment(s)` : 'OK: all XML comments well-formed');
process.exit(bad ? 1 : 0);
