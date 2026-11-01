/*
 * Implementazioni DOM dei primitivi Compose usati da home-screen.js (HomeScreen).
 * Fonte Kotlin: Compose foundation/material3 (layout, Modifier, Text, Image, LazyRow).
 * Stato: PRIMA BATCH. Layout base, Modifier, tema, testo, immagini, LazyRow, page layout.
 * Le funzioni di menu, Coil, lifecycle e dati restano STUB in home-components.js.
 * Non caricato da index.html: il collegamento e' un passo successivo.
 */
(function (global) {
  'use strict';

  // ---------- util ----------
  const isNode = (v) => v && typeof v === 'object' && v.nodeType === 1;
  function flatten(list, out) {
    out = out || [];
    (list || []).forEach((x) => {
      if (x == null || x === false) return;
      if (Array.isArray(x)) flatten(x, out);
      else if (x.__item) flatten([x.node], out);
      else out.push(x);
    });
    return out;
  }
  function px(v) { return typeof v === 'number' ? v + 'px' : v; }
  function appendAll(el, children) {
    flatten(children).forEach((c) => el.appendChild(isNode(c) ? c : document.createTextNode(String(c))));
    return el;
  }
  function run(children) { return typeof children === 'function' ? children() : children; }

  // ---------- Modifier (catena di stili) ----------
  // KT: androidx.compose.ui.Modifier -- ogni metodo ritorna un nuovo Modifier con uno stile in piu'.
  class ModifierBuilder {
    constructor(style, clicks) { this.style = style || {}; this.clicks = clicks || null; this.onPos = null; }
    _with(patch) { return new ModifierBuilder(Object.assign({}, this.style, patch), this.clicks); }
    fillMaxWidth() { return this._with({ width: '100%' }); }
    fillMaxHeight() { return this._with({ height: '100%' }); }
    fillMaxSize() { return this._with({ width: '100%', height: '100%' }); }
    width(v) { return this._with({ width: px(v), minWidth: px(v) }); }
    height(v) { return this._with({ height: px(v), minHeight: px(v) }); }
    heightIn(opts) { return this._with({ minHeight: px(opts && typeof opts === 'object' ? opts.min : opts) }); }
    size(v) { return this._with({ width: px(v), height: px(v), minWidth: px(v), minHeight: px(v) }); }
    padding(v) {
      if (typeof v === 'number') return this._with({ padding: px(v) });
      const o = v || {};
      return this._with({ paddingTop: px(o.top || o.vertical || 0), paddingBottom: px(o.bottom || o.vertical || 0),
        paddingLeft: px(o.start || o.horizontal || 0), paddingRight: px(o.end || o.horizontal || 0) });
    }
    background(color) { return this._with({ background: color }); }
    border(width, color, shape) {
      if (width && typeof width === 'object') { const o = width; return this._with({ border: `${px(o.width)} solid ${o.color}` }); }
      return this._with({ border: `${px(width)} solid ${color}`, borderRadius: shape && shape.radius ? px(shape.radius) : undefined });
    }
    clip(shape) { return this._with({ borderRadius: shape && shape.radius ? px(shape.radius) : (shape === 'circle' ? '50%' : undefined), overflow: 'hidden' }); }
    align(a) { return this._with({ alignSelf: a === 'Center' ? 'center' : (a === 'TopEnd' ? 'flex-end' : undefined), __align: a }); }
    weight(w) { return this._with({ flex: `${w} 1 0` }); }
    alpha(a) { return this._with({ opacity: a }); }
    offset(fn) { return this._with({ __offset: fn }); }
    then(other) { return other ? new ModifierBuilder(Object.assign({}, this.style, other.style), other.clicks || this.clicks) : this; }
    clickable(opts, fn) { return this._with({}).__click(fn || (opts && opts.onClick)); }
    combinedClickable(opts) { return this.__click(opts && opts.onClick, opts && opts.onLongClick); }
    __click(onClick, onLongClick) { const m = new ModifierBuilder(Object.assign({}, this.style), { onClick, onLongClick }); m.style.cursor = 'pointer'; return m; }
    onGloballyPositioned(fn) { const m = new ModifierBuilder(Object.assign({}, this.style), this.clicks); m.onPos = fn; return m; }
    windowInsetsPadding() { return this; }
    animateItem() { return this; }
    rubberBandOverscroll() { return this; }
    horizontalScroll() { return this._with({ overflowX: 'auto' }); }
    border_() { return this; }
    // nessun effetto visivo: placeholder per modificatori non ancora resi
    irideFolioCombinedClickable(o) { return this.combinedClickable(o); }
    irideArtworkOverlayBorder() { return this; }
    irideDepthEnter() { return this; }
    padding_() { return this; }
    applyTo(el) {
      Object.assign(el.style, this.style);
      if (this.clicks) {
        el.style.cursor = 'pointer';
        if (this.clicks.onClick) el.addEventListener('click', () => this.clicks.onClick());
        if (this.clicks.onLongClick) el.addEventListener('contextmenu', (e) => { e.preventDefault(); this.clicks.onLongClick(); });
      }
      if (this.onPos) this.onPos({ positionInParent: () => ({ x: 0 }), size: { width: el.offsetWidth }, boundsInWindow: () => el.getBoundingClientRect() });
      return el;
    }
  }
  const METHODS = Object.getOwnPropertyNames(ModifierBuilder.prototype).filter((n) => n !== 'constructor' && !n.startsWith('_') && !n.startsWith('__') && n !== 'applyTo');
  // Modifier e' sia funzione (C.Modifier()) sia oggetto con i metodi (C.Modifier.fillMaxWidth()).
  const Modifier = function () { return new ModifierBuilder(); };
  METHODS.forEach((name) => { Modifier[name] = (...args) => new ModifierBuilder()[name](...args); });
  Modifier.style = {};
  Modifier.then = (other) => new ModifierBuilder().then(other);

  // ---------- tema ----------
  // KT: ui/theme (colori Iride, tema scuro). Valori copiati dal progetto desktop (var CSS).
  function palette(overrides) {
    const base = {
      background: '#000000', surface: '#0d0d0d', onSurface: '#f2f2f2', onSurfaceVariant: '#a6a6a6',
      surfaceContainerHighest: '#1e1e1e', textPrimary: '#f2f2f2', textSecondary: '#b3b3b3', textTertiary: '#7a7a7a',
      strokeCard: 'rgba(255,255,255,0.12)', strokeItemSoft: 'rgba(255,255,255,0.08)', fillSelected: 'rgba(255,255,255,0.18)',
      secondaryContainer: '#2a2a2a', onSecondaryContainer: '#f2f2f2', primary: '#f2f2f2', onPrimary: '#0d0d0d',
    };
    const p = Object.assign(base, overrides || {});
    p.copy = (o) => Object.assign({}, p, o || {}, { copy: p.copy });
    return p;
  }
  function typo(name) {
    // KT: MaterialTheme.typography -- scale di dimensioni del tema Iride
    const scale = { labelSmall: { fontSize: 11 }, labelLarge: { fontSize: 14 }, bodySmall: { fontSize: 12 }, bodyMedium: { fontSize: 14 },
      bodyLarge: { fontSize: 16 }, titleSmall: { fontSize: 14, fontWeight: 600 }, titleMedium: { fontSize: 16, fontWeight: 600 } };
    const s = Object.assign({}, scale[name] || { fontSize: 14 });
    s.copy = (o) => Object.assign({}, s, o || {});
    return s;
  }
  const MaterialTheme = function (props, body) { return run(body); };
  MaterialTheme.colorScheme = () => palette();
  MaterialTheme.typography = typo;

  // ---------- primitivi di layout ----------
  function Box(props) {                                                           // KT: Box(modifier, contentAlignment)
    const el = document.createElement('div');
    el.style.display = 'flex'; el.style.position = 'relative'; el.style.flexDirection = 'column';
    if (props && props.contentAlignment === 'Center') { el.style.alignItems = 'center'; el.style.justifyContent = 'center'; }
    if (props && props.modifier) props.modifier.applyTo(el);
    return appendAll(el, run(props && props.children));
  }
  function Column(props) {                                                        // KT: Column
    const el = document.createElement('div');
    el.style.display = 'flex'; el.style.flexDirection = 'column';
    if (props && props.horizontalAlignment === 'CenterHorizontally') el.style.alignItems = 'center';
    if (props && props.verticalArrangement && props.verticalArrangement.gap) el.style.gap = px(props.verticalArrangement.gap);
    if (props && props.modifier) props.modifier.applyTo(el);
    const kids = typeof props === 'function' ? props : (props && props.children);
    return appendAll(el, run(kids || (props && props.content)));
  }
  function Row(props) {                                                           // KT: Row
    const el = document.createElement('div');
    el.style.display = 'flex'; el.style.flexDirection = 'row';
    if (props && props.verticalAlignment === 'CenterVertically') el.style.alignItems = 'center';
    if (props && props.horizontalArrangement && props.horizontalArrangement.gap) el.style.gap = px(props.horizontalArrangement.gap);
    if (props && props.modifier) props.modifier.applyTo(el);
    return appendAll(el, run(props && (props.children || props.content)));
  }
  function Spacer(props) {                                                        // KT: Spacer(modifier)
    const el = document.createElement('div');
    if (props && props.modifier) props.modifier.applyTo(el);
    return el;
  }
  // KT: Text(text, style, color, fontWeight, maxLines, overflow, textAlign, modifier)
  function Text(props) {
    const el = document.createElement('div');
    el.textContent = props.text == null ? '' : String(props.text);
    const st = props.style || {};
    if (st.fontSize) el.style.fontSize = px(st.fontSize);
    if (st.letterSpacing != null) el.style.letterSpacing = px(st.letterSpacing);
    if (st.fontFamily) el.style.fontFamily = st.fontFamily;
    if (props.color) el.style.color = props.color;
    const w = props.fontWeight;
    if (w) el.style.fontWeight = ({ Bold: 700, SemiBold: 600, Black: 900, Normal: 400 })[w] || w;
    if (props.textAlign === 'Center') el.style.textAlign = 'center';
    if (props.maxLines) { el.style.display = '-webkit-box'; el.style.webkitLineClamp = String(props.maxLines); el.style.webkitBoxOrient = 'vertical'; el.style.overflow = 'hidden'; }
    if (props.overflow === 'Ellipsis') el.style.textOverflow = 'ellipsis';
    if (props.modifier) props.modifier.applyTo(el);
    return el;
  }
  // KT: Image(painter/bitmap, contentDescription, contentScale, modifier) e AsyncImage(model, ...)
  function Image(props) {
    const el = document.createElement('img');
    el.alt = props.contentDescription || '';
    if (props.bitmap && props.bitmap.src) el.src = props.bitmap.src;
    el.style.objectFit = props.contentScale === 'Crop' ? 'cover' : 'contain';
    if (props.modifier) props.modifier.applyTo(el);
    return el;
  }
  function AsyncImage(props) {                                                    // KT: coil3 AsyncImage(model)
    const el = document.createElement('img');
    el.alt = props.contentDescription || '';
    const m = props.model;
    const url = (m && typeof m === 'object' && m.url) ? m.url : m;
    if (url) el.src = url;
    el.loading = 'lazy';
    el.style.objectFit = props.contentScale === 'Crop' ? 'cover' : 'contain';
    el.style.display = 'block';
    if (props.modifier) props.modifier.applyTo(el);
    return el;
  }
  // KT: Icon(painter, contentDescription, tint, modifier): resa come span vuoto (icone Material via CSS)
  function Icon(props) {
    const el = document.createElement('span');
    el.setAttribute('aria-label', props.contentDescription || '');
    if (props.tint) el.style.color = props.tint;
    if (props.modifier) props.modifier.applyTo(el);
    return el;
  }
  // KT: Surface(onClick, modifier, shape, color, border) { content }
  function Surface(props, body) {
    const el = document.createElement('div');
    if (props.modifier) props.modifier.applyTo(el);
    if (props.color && props.color !== 'transparent') el.style.background = props.color;
    if (props.border) el.style.border = `${px(props.border.width)} solid ${props.border.color}`;
    if (props.onClick) el.addEventListener('click', () => props.onClick());
    return appendAll(el, run(body));
  }
  function Card(props, body) { return Surface(Object.assign({}, props, { color: props.colors ? props.colors.containerColor : undefined }), body); }
  function Scaffold(props, body) {                                              // KT: Scaffold(modifier) { paddingValues -> }
    const el = document.createElement('div');
    el.style.minHeight = '100%';
    return appendAll(el, run(body({ top: 0, bottom: 0 })));
  }
  function BoxWithConstraints(props, body) {                                    // KT: BoxWithConstraints
    const el = document.createElement('div');
    el.style.position = 'relative';
    if (props && props.modifier) props.modifier.applyTo(el);
    return appendAll(el, run(body));
  }
  function CompositionLocalProvider(locals, body) { return run(body); }         // KT: CompositionLocalProvider -> solo body

  // ---------- LazyRow / item ----------
  // KT: LazyListScope.item(key) { ... } -- eager: il nodo e' gia' costruito, lo LazyRow lo appende.
  function item(opts, fn) {
    const node = typeof fn === 'function' ? fn() : null;
    return { __item: true, key: opts && opts.key, node };
  }
  // KT: LazyRow(state, contentPadding, horizontalArrangement, modifier) { items(...) }
  function LazyRow(props) {
    const el = document.createElement('div');
    el.style.display = 'flex'; el.style.flexDirection = 'row'; el.style.overflowX = 'auto';
    if (props.horizontalArrangement && props.horizontalArrangement.gap) el.style.gap = px(props.horizontalArrangement.gap);
    if (props.contentPadding) {
      const p = props.contentPadding;
      el.style.paddingLeft = px(typeof p === 'number' ? p : (p.horizontal || 0));
      el.style.paddingRight = px(typeof p === 'number' ? p : (p.horizontal || 0));
    }
    if (props.modifier) props.modifier.applyTo(el);
    let list = props.items;
    if (!list && Array.isArray(props.itemsIndexed)) list = props.itemsIndexed.map((x) => x.content ? x.content() : x);
    if (list && !Array.isArray(list)) list = [];
    if (list && list.length && list[0] && list[0].__item !== undefined) return appendAll(el, list);
    return appendAll(el, list || []);
  }
  // KT: IridePageLayout(title, state, bottomPadding, listModifier, overlay) { body } -- colonna verticale con titolo e overlay
  function IridePageLayout(props, body) {
    const el = document.createElement('div');
    el.style.display = 'flex'; el.style.flexDirection = 'column'; el.style.overflowY = 'auto'; el.style.height = '100%';
    if (props.bottomPadding) el.style.paddingBottom = px(props.bottomPadding);
    if (props.title) {
      const h = document.createElement('h1');
      h.textContent = props.title;
      h.style.margin = '0 0 8px 0';
      el.appendChild(h);
    }
    const overlay = props.overlay ? props.overlay() : null;
    if (overlay) el.appendChild(overlay);
    return appendAll(el, flatten(run(body)));
  }
  // KT: IrideFolioSection(sectionKey, ready, artwork, placeholder) { content }
  function IrideFolioSection(props, body) {
    if (props.ready === false && props.placeholder) return props.placeholder();
    const el = document.createElement('section');
    el.dataset.section = props.sectionKey || '';
    el.style.margin = '0 0 24px 0';
    if (props.modifier) props.modifier.applyTo(el);
    return appendAll(el, run(body));
  }
  // KT: IrideFolioShelfHeader(title, onClick, showNavigationArrow)
  function IrideFolioShelfHeader(props) {
    const el = document.createElement('h2');
    el.textContent = props.title || '';
    el.style.fontSize = '20px'; el.style.fontWeight = '700'; el.style.margin = '0 0 12px 20px';
    if (props.onClick) { el.style.cursor = 'pointer'; el.addEventListener('click', () => props.onClick()); }
    return el;
  }
  // KT: IrideFolioMediaShelf(items, songContent, tileContent)
  function IrideFolioMediaShelf(props) {
    const row = LazyRow({ contentPadding: 20, horizontalArrangement: { gap: 16 } });
    (props.items || []).forEach((it) => {
      const n = it.type === 'SongItem' && props.songContent ? props.songContent(it, 160) : (props.tileContent ? props.tileContent(it) : null);
      if (n) row.appendChild(n);
    });
    return row;
  }

  global.IrideHomeDom = {
    Modifier, MaterialTheme, Box, Column, Row, Spacer, Text, Image, AsyncImage, Icon, Surface, Card,
    Scaffold, BoxWithConstraints, CompositionLocalProvider, item, LazyRow, IridePageLayout,
    IrideFolioSection, IrideFolioShelfHeader, IrideFolioMediaShelf, palette, typo,
  };
})(typeof window !== 'undefined' ? window : globalThis);
