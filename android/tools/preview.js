#!/usr/bin/env node
/*
 * Emit preview/tokens.css and preview/index.html from res/values + res/drawable.
 *
 * The APK is built from these XML files, so this tool is the only sanctioned way
 * to get those numbers into a browser: a hex code typed by hand under preview/ is
 * a bug, because it can drift from colors.xml without anything noticing.
 *
 * Unit mapping (deliberate, and the reason there is no per-density option):
 *   dp -> CSS px 1:1  (both are already density-independent)
 *   sp -> CSS px 1:1  (sp == dp at the default font scale)
 * Device font scaling is therefore NOT represented here. Neither is Android's
 * measurement pass, so this previews design intent, not layout results.
 */
'use strict';

const fs = require('fs');
const path = require('path');

const ROOT = path.resolve(__dirname, '..');
const RES = path.join(ROOT, 'res');
const OUT = path.join(ROOT, 'preview');

const unresolved = [];
const ignored = new Set();

/* ------------------------------------------------------------------ scrapers */

function read(...p) {
  return fs.readFileSync(path.join(...p), 'utf8');
}

function attrs(tag) {
  const out = {};
  const re = /([\w:]+)\s*=\s*"([^"]*)"/g;
  let m;
  while ((m = re.exec(tag))) out[m[1]] = m[2];
  return out;
}

function argbToCss(value) {
  const m = /^#([0-9A-Fa-f]{8})$/.exec(value);
  if (!m) return value;
  const hex = '#' + m[1].slice(2).toUpperCase();
  const a = parseInt(m[1].slice(0, 2), 16) / 255;
  if (a >= 0.999) return hex;
  const r = parseInt(hex.slice(1, 3), 16);
  const g = parseInt(hex.slice(3, 5), 16);
  const b = parseInt(hex.slice(5, 7), 16);
  return `rgba(${r}, ${g}, ${b}, ${(+a.toFixed(3))})`;
}

function parseColors() {
  const map = new Map();
  const src = read(RES, 'values', 'colors.xml');
  const re = /<color\s+name="([^"]+)"\s*>([^<]+)<\/color>/g;
  let m;
  while ((m = re.exec(src))) map.set(m[1], argbToCss(m[2].trim()));
  return map;
}

/** res/color/*.xml are state selectors; CSS cannot key off Android states, so the
 *  unselected item is the token and the rest are reported as notes. */
function parseColorSelectors() {
  const map = new Map();
  const dir = path.join(RES, 'color');
  if (!fs.existsSync(dir)) return map;
  for (const f of fs.readdirSync(dir)) {
    if (!f.endsWith('.xml')) continue;
    const src = read(dir, f);
    const items = [...src.matchAll(/<item\s+([^>]*?)\/>/g)];
    let fallback = null;
    let states = [];
    for (const it of items) {
      const a = attrs(it[1]);
      const body = /color="([^"]+)"/.exec(it[1]);
      if (!body) continue;
      const st = Object.keys(a).filter(k => k.startsWith('android:state_'));
      if (st.length) states.push(st.map(s => s.replace('android:state_', '')).join('+'));
      else fallback = body[1];
    }
    if (!fallback) continue;
    // The unselected item is usually itself a reference: @color/ink_2.
    const nested = /^@color\/([\w.]+)$/.exec(fallback);
    const lit = nested && COLORS.has(nested[1]) ? COLORS.get(nested[1]) : argbToCss(fallback);
    map.set(f.replace(/\.xml$/, ''), { value: lit, states, from: fallback });
  }
  return map;
}

function dimToCss(raw) {
  const m = /^(-?[\d.]+)(dp|sp)$/.exec(raw);
  if (!m) return raw;
  return `${parseFloat(m[1])}px`;
}

function parseDimens() {
  const map = new Map();
  const src = read(RES, 'values', 'dimens.xml');
  const re = /<dimen\s+name="([^"]+)"\s*>([^<]+)<\/dimen>/g;
  let m;
  while ((m = re.exec(src))) {
    const raw = m[2].trim();
    map.set(m[1], { raw, css: dimToCss(raw) });
  }
  return map;
}

function parseStyles() {
  const src = read(RES, 'values', 'styles.xml');
  const map = new Map();
  const re = /<style\s+([^>]*?)\/>|<style\s+([^>]*?)>([\s\S]*?)<\/style>/g;
  let m;
  while ((m = re.exec(src))) {
    const a = attrs(m[1] || m[2]);
    const items = {};
    const body = m[3] || '';
    const ire = /<item\s+([^>]+)>([^<]*)<\/item>/g;
    let x;
    while ((x = ire.exec(body))) {
      const k = attrs(x[1]).name;
      if (k) items[k] = x[2].trim();
    }
    map.set(a.name, { parent: a.parent || null, items });
  }
  return map;
}

/* ---------------------------------------------------------------- drawables */

/** Android's angle runs anticlockwise from "rightward"; CSS runs clockwise from "upward". */
function gradientAngle(deg) {
  const a = ((90 - parseFloat(deg)) % 360 + 360) % 360;
  return `${a}deg`;
}

function shapeCss(inner, a) {
  const css = {};
  const shape = /<shape[^>]*shape="([^"]+)"/.exec(inner);
  const solid = /<solid\s+([^>]*?)\/>/.exec(inner);
  const corners = /<corners\s+([^>]*?)\/>/.exec(inner);
  const stroke = /<stroke\s+([^>]*?)\/>/.exec(inner);
  const grad = /<gradient\s+([^>]*?)\/>/.exec(inner);

  if (a['android:shape'] === 'oval' || (shape && shape[1] === 'oval')) {
    css['border-radius'] = '50%';
  }
  if (solid) {
    const c = attrs(solid[1])['android:color'];
    if (c) css.background = ref(c, 'color');
  }
  if (grad) {
    const g = attrs(grad[1]);
    const stops = [g['android:startColor'], g['android:centerColor'], g['android:endColor']]
      .filter(Boolean).map(c => ref(c, 'color'));
    css.background = `linear-gradient(${gradientAngle(g['android:angle'] || '0')}, ${stops.join(', ')})`;
  }
  if (corners) {
    const r = attrs(corners[1])['android:radius'];
    if (r) css['border-radius'] = ref(r, 'dimen');
  }
  if (stroke) {
    const s = attrs(stroke[1]);
    const w = s['android:width'], c = s['android:color'];
    if (w && c) css['box-shadow'] = `inset 0 0 0 ${ref(w, 'dimen')} ${ref(c, 'color')}`;
  }
  return css;
}

function parseDrawable(name) {
  const file = path.join(RES, 'drawable', name + '.xml');
  if (!fs.existsSync(file)) {
    unresolved.push(`@drawable/${name}`);
    return { css: {}, states: {} };
  }
  const src = read(file);
  const out = { css: {}, states: {}, notes: [] };

  const ripple = /<ripple\s+([^>]*)>/.exec(src);
  if (ripple) {
    const c = attrs(ripple[1])['android:color'];
    if (c) out.states[':active'] = { 'box-shadow': `inset 0 0 0 999px ${ref(c, 'color')}` };
  }

  // A bare <shape>, or a <selector> of <item>s each holding a shape. The body
  // pattern refuses to cross a nested <item>, so the <ripple>'s wrapper item is
  // skipped by the matcher itself instead of swallowing the states inside it.
  const shapeOnly = /<shape[\s\S]*?<\/shape>/.exec(src);
  const selItems = [...src.matchAll(/<item([^>]*)>((?:(?!<item)[\s\S])*?)<\/item>/g)];

  if (selItems.length) {
    for (const it of selItems) {
      const a = attrs(it[1]);
      const inner = it[2];
      const sh = /<shape[\s\S]*?<\/shape>/.exec(inner);
      if (!sh) continue;
      const css = shapeCss(sh[0], a);
      const st = Object.keys(a).filter(k => k.startsWith('android:state_'))
        .map(k => k.replace('android:state_', ''));
      if (!st.length) Object.assign(out.css, css);
      else {
        const sel = st.includes('selected') || st.includes('checked') ? '.is-on'
          : st.includes('pressed') ? ':active'
          : st.includes('enabled') ? '.is-off' : null;
        if (sel) out.states[sel] = Object.assign(out.states[sel] || {}, css);
        else out.notes.push('state_' + st.join('+') + ' has no CSS equivalent');
      }
    }
  } else if (shapeOnly) {
    out.css = shapeCss(shapeOnly[0], {});
  }

  // A ripple's default layer is often a reference rather than a shape:
  // <item android:drawable="@drawable/bg_btn_primary_solid" />.
  for (const m of src.matchAll(/<item([^>]*?)\/>/g)) {
    const a = attrs(m[1]);
    const dref = a['android:drawable'];
    if (!dref || !/^@drawable\//.test(dref)) continue;
    const d = ref(dref, 'drawable');
    const st = Object.keys(a).filter(k => k.startsWith('android:state_'))
      .map(k => k.replace('android:state_', ''));
    if (!st.length) {
      Object.assign(out.css, d.css);
      for (const [sel, decl] of Object.entries(d.states)) {
        out.states[sel] = Object.assign(out.states[sel] || {}, decl);
      }
    }
  }
  return out;
}

/* -------------------------------------------------------------- resolution */

const COLORS = parseColors();
const COLOR_SELECTORS = parseColorSelectors();
const DIMENS = parseDimens();
const STYLES = parseStyles();
const DRAWABLES = new Map();

function ref(value, kind) {
  const v = String(value).trim();
  let m;
  if ((m = /^@color\/([\w.]+)$/.exec(v))) {
    const n = m[1];
    if (COLORS.has(n)) return COLORS.get(n);
    if (COLOR_SELECTORS.has(n)) return COLOR_SELECTORS.get(n).value;
    unresolved.push(`@color/${n}`);
    return 'currentColor';
  }
  if ((m = /^@dimen\/([\w.]+)$/.exec(v))) {
    const n = m[1];
    if (DIMENS.has(n)) return DIMENS.get(n).css;
    unresolved.push(`@dimen/${n}`);
    return '0px';
  }
  if ((m = /^@drawable\/([\w.]+)$/.exec(v))) {
    const n = m[1];
    if (!DRAWABLES.has(n)) DRAWABLES.set(n, parseDrawable(n));
    return DRAWABLES.get(n);
  }
  if (/^@(android|style|null)\b|^@null$/.test(v)) return null;
  if (kind === 'dimen') {
    const d = /^(-?[\d.]+)(dp|sp)$/.test(v);
    if (d) return dimToCss(v);
    if (/^\d+$/.test(v)) return `${v}px`;
    return null;
  }
  if (kind === 'color') {
    if (/^#/.test(v)) return argbToCss(v);
    return null;
  }
  return null;
}

/* ------------------------------------------------------------ style -> CSS */

const ELEVATION = {
  1: '0 1px 2px rgba(15, 23, 42, .10), 0 1px 3px rgba(15, 23, 42, .06)',
  3: '0 1px 3px rgba(15, 23, 42, .12), 0 4px 8px rgba(15, 23, 42, .08)',
  6: '0 3px 6px rgba(15, 23, 42, .14), 0 8px 16px rgba(15, 23, 42, .10)',
  12: '0 6px 12px rgba(15, 23, 42, .16), 0 16px 28px rgba(15, 23, 42, .12)',
};

const GRAVITY = {
  center: ['align-items', 'center', 'justify-content', 'center'],
  center_horizontal: [null, null, 'text-align', 'center'],
  center_vertical: ['align-items', 'center', null, null],
  top: ['align-items', 'flex-start', null, null],
};

function styleChain(name) {
  // Android's implicit dot-parent rule: Btn.Tonal inherits Btn unless told otherwise.
  const out = [];
  let cur = name;
  while (cur) {
    const s = STYLES.get(cur);
    if (!s) break;
    out.unshift(s);
    cur = s.parent && STYLES.has(s.parent) ? s.parent
      : (cur.includes('.') ? cur.slice(0, cur.lastIndexOf('.')) : null);
    if (s.parent && s.parent.startsWith('@android')) cur = null;
  }
  return out;
}

function styleToCss(name) {
  const css = {};
  const states = {};
  for (const s of styleChain(name)) {
    for (const [k, raw] of Object.entries(s.items)) {
      const key = k.replace('android:', '');
      switch (key) {
        case 'textSize': css['font-size'] = ref(raw, 'dimen'); break;
        case 'textColor': css.color = ref(raw, 'color'); break;
        case 'textStyle':
          if (raw === 'bold') css['font-weight'] = '700';
          else if (raw === 'italic') css['font-style'] = 'italic';
          break;
        case 'textAllCaps': css['text-transform'] = raw === 'true' ? 'uppercase' : 'none'; break;
        case 'letterSpacing': css['letter-spacing'] = `${parseFloat(raw) || 0}em`; break;
        case 'lineSpacingExtra': css['line-height'] = `calc(1em + ${ref(raw, 'dimen')})`; break;
        case 'maxLines':
          if (raw === '1') { css['white-space'] = 'nowrap'; css.overflow = 'hidden'; }
          else css['-webkit-line-clamp'] = raw;
          break;
        case 'ellipsize': if (raw === 'end') css['text-overflow'] = 'ellipsis'; break;
        case 'gravity': {
          const g = GRAVITY[raw];
          if (g) {
            css[g[0]] = g[1];
            css[g[2]] = g[3];
            css.display = 'flex';
          }
          break;
        }
        case 'singleLine': if (raw === 'true') css['white-space'] = 'nowrap'; break;
        case 'padding': css.padding = ref(raw, 'dimen'); break;
        case 'paddingLeft': css['padding-left'] = ref(raw, 'dimen'); break;
        case 'paddingRight': css['padding-right'] = ref(raw, 'dimen'); break;
        case 'paddingTop': css['padding-top'] = ref(raw, 'dimen'); break;
        case 'paddingBottom': css['padding-bottom'] = ref(raw, 'dimen'); break;
        case 'minHeight': css['min-height'] = ref(raw, 'dimen'); break;
        case 'minWidth': break;
        case 'layout_height': {
          const h = ref(raw, 'dimen');
          if (h) css.height = h;
          break;
        }
        case 'layout_width':
          if (raw === 'match_parent') css.width = '100%';
          else {
            const w = ref(raw, 'dimen');
            if (w) css.width = w;
          }
          break;
        case 'background': {
          const d = ref(raw, 'drawable');
          if (d && d.css) {
            Object.assign(css, d.css);
            for (const [sel, decl] of Object.entries(d.states)) {
              states[sel] = Object.assign(states[sel] || {}, decl);
            }
          } else if (d === null) {
            const c = ref(raw, 'color');
            if (c) css.background = c;
          }
          break;
        }
        case 'elevation': {
          const px = parseFloat(ref(raw, 'dimen')) || 0;
          const step = px <= 1 ? 1 : px <= 3 ? 3 : px <= 6 ? 6 : 12;
          css['box-shadow'] = ELEVATION[step];
          break;
        }
        case 'scaleType': case 'focusable': case 'stateListAnimator': case 'inputType':
        case 'digits': case 'maxLength': case 'includeFontPadding': case 'windowBackground':
        case 'windowNoTitle': case 'windowIsTranslucent': case 'windowContentOverlay':
        case 'backgroundDimEnabled': case 'backgroundDimAmount': case 'colorPrimary':
        case 'colorPrimaryDark': case 'colorAccent': case 'colorBackground':
        case 'statusBarColor': case 'navigationBarColor': case 'textColorPrimary':
        case 'textColorSecondary': case 'textColorHint': case 'windowAnimationStyle':
        case 'activityOpenEnterAnimation': case 'activityCloseExitAnimation':
        case 'windowEnterAnimation': case 'windowExitAnimation':
          ignored.add(key);
          break;
        default:
          ignored.add(key);
      }
    }
  }
  return { css, states };
}

/* ------------------------------------------------------------------ emitters */

/* Dots become dashes (R.style.Text.Body -> .R-Text-Body) but underscores are
 * kept, so every generated name still reads as the R.* identifier it came from:
 * --color-ok_border is R.color.ok_border, not --color-ok-border. */
const kebab = s => s.replace(/\./g, '-');

function tokensCss() {
  const L = [];
  L.push('/* GENERATED by tools/preview.js from res/values + res/drawable.');
  L.push('   Do not edit and do not hand-type design values in preview/*.html. */');
  L.push(':root {');
  for (const [n, v] of COLORS) L.push(`  --color-${kebab(n)}: ${v};`);
  for (const [n, v] of DIMENS) L.push(`  --dimen-${kebab(n)}: ${v.css};`);
  for (const [n, v] of COLOR_SELECTORS) {
    L.push(`  --color-${kebab(n)}: ${v.value}; /* res/color selector; states ${v.states.join(', ') || 'n/a'} */`);
  }
  L.push('}');
  L.push('');
  L.push('body { font-family: Roboto, "Helvetica Neue", system-ui, sans-serif; }');
  L.push('');

  const names = [...STYLES.keys()].filter(n => !n.startsWith('Theme.') && !n.startsWith('Anim.') && !n.startsWith('Animation.'));
  for (const n of names) {
    const { css, states } = styleToCss(n);
    const decl = Object.entries(css).map(([k, v]) => `  ${k}: ${v};`).join('\n');
    if (decl) L.push(`.R-${kebab(n)} { /* R.style.${n} */\n${decl}\n}`);
    for (const [sel, decl2] of Object.entries(states)) {
      const d = Object.entries(decl2).map(([k, v]) => `  ${k}: ${v};`).join('\n');
      L.push(`.R-${kebab(n)}${sel} {\n${d}\n}`);
    }
  }

  // Every non-icon drawable too, so a mockup can use .D-bg_hero_card instead of
  // re-typing a gradient that already exists in res/drawable.
  const dnames = fs.readdirSync(path.join(RES, 'drawable'))
    .filter(f => f.endsWith('.xml') && !f.startsWith('ic_'))
    .map(f => f.replace(/\.xml$/, ''))
    .sort();
  for (const n of dnames) {
    const d = ref(`@drawable/${n}`, 'drawable');
    const decl = Object.entries(d.css).map(([k, v]) => `  ${k}: ${v};`).join('\n');
    if (decl) L.push(`.D-${kebab(n)} { /* R.drawable.${n} */\n${decl}\n}`);
    for (const [sel, decl2] of Object.entries(d.states)) {
      const dd = Object.entries(decl2).map(([k, v]) => `  ${k}: ${v};`).join('\n');
      L.push(`.D-${kebab(n)}${sel} {\n${dd}\n}`);
    }
    for (const note of d.notes || []) L.push(`/* ${n}: ${note} */`);
  }

  L.push('');
  L.push(`/* Ignored Android-only attributes: ${[...ignored].sort().join(', ')} */`);
  return L.join('\n') + '\n';
}

function esc(s) {
  return String(s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
}

function sheetHtml() {
  const groups = new Map();
  for (const [n, v] of COLORS) {
    const g = n.split('_')[0];
    if (!groups.has(g)) groups.set(g, []);
    groups.get(g).push([n, v]);
  }
  const textStyles = [...STYLES.keys()].filter(n => n.startsWith('Text.'));
  const swatches = [...groups.entries()].map(([g, items]) => `
    <section>
      <h3>${esc(g)} <span>${items.length}</span></h3>
      <div class="sw">
        ${items.map(([n, v]) => `
          <div class="chip">
            <i style="background:${v}"></i>
            <b>${esc(n)}</b>
            <code>${esc(v)}</code>
          </div>`).join('')}
      </div>
    </section>`).join('\n');

  const ramp = textStyles.map(n => {
    const { css } = styleToCss(n);
    const size = css['font-size'] || '?';
    return `<div class="row"><span class="tag">${esc(n)} · ${size}</span>
      <span class="R-${kebab(n)}">CSP Daily 12,345 figures</span></div>`;
  }).join('\n');

  const comps = ['Card', 'Card.Flush', 'Btn', 'Btn.Tonal', 'Btn.Outline', 'Btn.Danger', 'Btn.Sm',
    'FilterChip', 'NumField', 'TextField', 'IconBtn', 'Hr', 'Text.Section']
    .filter(n => STYLES.has(n))
    .map(n => `<div class="row"><span class="tag">R.style.${esc(n)}</span>
      <div class="R-${kebab(n)} sample">${esc(n.split('.').pop())}</div></div>`)
    .join('\n');

  const dims = [...DIMENS.entries()].map(([n, v]) =>
    `<tr><td>${esc(n)}</td><td>${esc(v.raw)}</td><td>${esc(v.css)}</td></tr>`).join('');

  return `<!doctype html>
<html lang="en"><head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>CSP Daily — native design tokens</title>
<link rel="stylesheet" href="tokens.css">
<style>
  body { background: var(--color-bg_app); color: var(--color-ink); margin: 0; padding: 20px 14px 60px; }
  h1 { font-size: 22px; margin: 0 0 4px; }
  h2 { font-size: 13px; text-transform: uppercase; letter-spacing: .08em; color: var(--color-ink_3); margin: 28px 0 10px; }
  h3 { font-size: 12px; text-transform: uppercase; letter-spacing: .06em; color: var(--color-ink_3); margin: 18px 0 8px; }
  h3 span { color: var(--color-ink_4); }
  p.note { font-size: 12px; color: var(--color-ink_3); max-width: 62ch; line-height: 1.5; }
  .sw { display: flex; flex-wrap: wrap; gap: 8px; }
  .chip { width: 104px; }
  .chip i { display: block; height: 42px; border-radius: 10px; border: 1px solid var(--color-line_2); }
  .chip b { display: block; font-size: 10px; margin-top: 4px; word-break: break-all; }
  .chip code { font-size: 9px; color: var(--color-ink_4); }
  .row { display: flex; align-items: center; gap: 12px; padding: 7px 0; border-bottom: 1px solid var(--color-line); }
  .row .tag { flex: 0 0 190px; font-size: 10px; color: var(--color-ink_4); font-family: ui-monospace, monospace; }
  .sample { padding: 10px 14px; }
  table { border-collapse: collapse; font-size: 11px; }
  td, th { padding: 4px 10px; border-bottom: 1px solid var(--color-line); text-align: left; font-family: ui-monospace, monospace; }
  th { color: var(--color-ink_3); text-transform: uppercase; font-size: 10px; letter-spacing: .05em; }
  .hero { background: var(--color-hero_start); padding: 16px; border-radius: 18px; color: #fff; }
</style>
</head><body>
  <h1>CSP Daily — native design tokens</h1>
  <p class="note">Generated from <code>res/values/colors.xml</code>, <code>dimens.xml</code>,
  <code>styles.xml</code> and <code>res/drawable</code>. dp and sp both map to CSS px 1:1;
  device font scaling and Android's measurement pass are not represented, so use this sheet to
  judge colour, type and component look — not spacing outcomes. Verify those in the APK.</p>

  <h2>Palette (${COLORS.size} colours)</h2>
  ${swatches}

  <h2>Type ramp</h2>
  ${ramp}

  <h2>Components</h2>
  ${comps}

  <h2>Hero gradient</h2>
  <div class="row"><span class="tag">R.drawable.bg_hero_card</span>
    <div class="D-bg_hero_card sample" style="color:var(--color-paper)">
      hero_start → hero_mid → hero_end</div></div>

  <h2>Dimensions (${DIMENS.size})</h2>
  <table><tr><th>name</th><th>xml</th><th>css</th></tr>${dims}</table>
</body></html>
`;
}

/**
 * res/drawable/ic_*.xml -> preview/icons.js, which injects an inline <symbol>
 * sprite. Chrome cannot <use> a symbol from an external .svg, so the sprite has
 * to land in the document; a script keeps the icons generated rather than
 * retyped, and works over file:// as well as http://.
 */
function iconsJs() {
  const dir = path.join(RES, 'drawable');
  const syms = [];
  let skipped = 0;
  for (const f of fs.readdirSync(dir).sort()) {
    if (!f.startsWith('ic_') || !f.endsWith('.xml')) continue;
    const src = read(dir, f);
    if (/<group\b/.test(src)) { skipped++; continue; }
    const v = attrs((/<vector\s+([^>]*?)>/ .exec(src) || [''])[0]);
    const parts = [];
    for (const p of src.matchAll(/<path\s+([^>]*?)\/>/g)) {
      const a = attrs(p[1]);
      if (!a['android:pathData']) continue;
      const fill = a['android:fillColor'];
      const tinted = !fill || /^#ff000000$/i.test(fill);
      parts.push(`<path fill="${tinted ? 'currentColor' : argbToCss(fill)}" d="${a['android:pathData']}"/>`);
    }
    if (!parts.length) { skipped++; continue; }
    syms.push(`<symbol id="${f.replace(/\.xml$/, '')}" viewBox="0 0 ` +
      `${v['android:viewportWidth'] || 24} ${v['android:viewportHeight'] || 24}">${parts.join('')}</symbol>`);
  }
  return '/* GENERATED by tools/preview.js from res/drawable/ic_*.xml. Do not edit. */\n' +
    "document.currentScript.insertAdjacentHTML('afterend', `" +
    `<svg xmlns="http://www.w3.org/2000/svg" style="display:none">${syms.join('')}</svg>` +
    "`);\n" +
    `/* ${syms.length} icons inlined, ${skipped} skipped (unsupported structure) */\n`;
}

/* ------------------------------------------------------------------- write */

fs.mkdirSync(OUT, { recursive: true });
fs.writeFileSync(path.join(OUT, 'tokens.css'), tokensCss());
fs.writeFileSync(path.join(OUT, 'index.html'), sheetHtml());
fs.writeFileSync(path.join(OUT, 'icons.js'), iconsJs());

const bad = [...new Set(unresolved)];
console.log(`tokens: ${COLORS.size} colours, ${DIMENS.size} dims, ${STYLES.size} styles, ` +
  `${COLOR_SELECTORS.size} colour selectors, ${DRAWABLES.size} drawables`);
if (bad.length) {
  console.log('UNRESOLVED references:');
  for (const b of bad) console.log('  ' + b);
  process.exit(1);
}
console.log('OK: every @color/@dimen/@drawable reference resolved');
