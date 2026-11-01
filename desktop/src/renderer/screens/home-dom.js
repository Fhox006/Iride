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
    const arr = Array.isArray(list) ? list : (list == null ? [] : [list]);
    arr.forEach((x) => {
      if (x == null || x === false) return;
      if (Array.isArray(x)) flatten(x, out);
      else if (x.__item) flatten([x.node], out);
      else out.push(x);
    });
    return out;
  }
  function px(v) { return typeof v === 'number' ? v + 'px' : v; }
  function appendAll(el, children) {
    flatten(children).forEach((c) => {
      if (isNode(c)) el.appendChild(c);
      else if (typeof c === 'string' || typeof c === 'number') el.appendChild(document.createTextNode(String(c)));
    });
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
  Modifier.applyTo = (el) => el; // C.Modifier usato direttamente come valore vuoto
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


  // ---------- BATCH 2 (10 dipendenze) ----------
  const artistsLine = (it) => (it && it.artists ? it.artists.map((a) => a.name).filter(Boolean).join(', ') : (it && it.artist && it.artist.name) || '');
  // KT: resize(w, h) -- PARZIALE: identita' (il ridimensionamento URL Kotlin non e' ancora portato)
  function resize(url, w, h) { return url; }
  // KT: ItemThumbnail(thumbnailUrl, isActive, isPlaying, shape, artistArtwork, requestResolution, hairlineBorder, modifier)
  function ItemThumbnail(props) {
    const el = document.createElement('img');
    el.src = props.thumbnailUrl || '';
    el.loading = 'lazy';
    el.style.objectFit = 'cover';
    el.style.borderRadius = props.shape === 'circle' ? '50%' : '6px';
    if (props.modifier) props.modifier.applyTo(el);
    return el;
  }
  // KT: YouTubeGridItem(item, isActive, isPlaying, size, modifier, ...) -- card: copertina + titolo + artisti
  function YouTubeGridItem(props) {
    const it = props.item || {};
    const card = document.createElement('div');
    card.style.width = px(props.size || 160);
    card.style.flex = '0 0 auto';
    if (props.isActive) card.dataset.active = 'true';
    const img = document.createElement('img');
    img.src = props.thumbnailUrlOverride || it.thumbnail || '';
    img.loading = 'lazy';
    img.style.width = '100%'; img.style.aspectRatio = props.forceThumbnailRatio ? '16 / 9' : '1 / 1'; img.style.objectFit = 'cover'; img.style.borderRadius = '6px';
    card.appendChild(img);
    const t = document.createElement('div');
    t.textContent = it.title || it.name || '';
    t.style.marginTop = '8px'; t.style.fontWeight = '600'; t.style.whiteSpace = 'nowrap'; t.style.overflow = 'hidden'; t.style.textOverflow = 'ellipsis';
    card.appendChild(t);
    const sub = document.createElement('div');
    sub.textContent = artistsLine(it);
    sub.style.opacity = '0.7'; sub.style.fontSize = '12px'; sub.style.whiteSpace = 'nowrap'; sub.style.overflow = 'hidden'; sub.style.textOverflow = 'ellipsis';
    card.appendChild(sub);
    if (props.modifier) props.modifier.applyTo(card);
    return card;
  }
  // KT: YouTubeListItem / SongListItem(item|song, isActive, isPlaying, trailingContent, modifier) -- riga: copertina + titolo + artisti + trailing
  function listRow(it, props) {
    const row = document.createElement('div');
    row.style.display = 'flex'; row.style.alignItems = 'center'; row.style.gap = '12px'; row.style.padding = '6px 0';
    const img = document.createElement('img');
    img.src = (it && it.thumbnail) || ''; img.style.width = '48px'; img.style.height = '48px'; img.style.objectFit = 'cover'; img.style.borderRadius = '4px';
    row.appendChild(img);
    const col = document.createElement('div');
    col.style.flex = '1'; col.style.minWidth = '0';
    const t = document.createElement('div'); t.textContent = (it && it.title) || ''; t.style.whiteSpace = 'nowrap'; t.style.overflow = 'hidden'; t.style.textOverflow = 'ellipsis';
    const sub = document.createElement('div'); sub.textContent = artistsLine(it); sub.style.opacity = '0.7'; sub.style.fontSize = '12px';
    col.appendChild(t); col.appendChild(sub); row.appendChild(col);
    if (props.trailingContent) { const tr = props.trailingContent(); if (tr) row.appendChild(tr); }
    if (props.modifier) props.modifier.applyTo(row);
    return row;
  }
  function YouTubeListItem(props) { return listRow(props.item, props); }
  function SongListItem(props) { return listRow(props.song, props); }
  // KT: SharedQuickPicksCarousel(items, key, loading, contentPadding, usePager, minimumRows, modifier) { content(item, width) }
  function SharedQuickPicksCarousel(props) {
    const wrap = document.createElement('div');
    wrap.style.display = 'flex'; wrap.style.flexDirection = 'column'; wrap.style.gap = '8px';
    if (props.loading) {                                                       // skeleton mentre carica
      for (let i = 0; i < (props.minimumRows || 1); i++) {
        const sk = document.createElement('div'); sk.style.height = '56px'; sk.style.borderRadius = '6px'; sk.style.background = 'rgba(255,255,255,0.06)';
        wrap.appendChild(sk);
      }
      return wrap;
    }
    const rows = Math.max(1, props.minimumRows || 1);
    const items = props.items || [];
    // raggruppa in colonne da `rows` elementi (come il carosello Kotlin: colonne verticali)
    const cols = [];
    for (let i = 0; i < items.length; i += rows) cols.push(items.slice(i, i + rows));
    const strip = LazyRow({ contentPadding: props.contentPadding || 0, horizontalArrangement: { gap: 16 }, modifier: props.modifier || Modifier() });
    cols.forEach((group) => {
      const col = document.createElement('div');
      col.style.display = 'flex'; col.style.flexDirection = 'column'; col.style.width = '320px'; col.style.flex = '0 0 auto';
      group.forEach((it) => { const n = props.content ? props.content(it, 320) : null; if (n) col.appendChild(n); });
      strip.appendChild(col);
    });
    wrap.appendChild(strip);
    return wrap;
  }
  // KT: SharedHomeSectionTitle(title, onClick)
  function SharedHomeSectionTitle(props) {
    const h = document.createElement('h3');
    h.textContent = props.title || '';
    h.style.margin = '0 0 8px 20px'; h.style.fontSize = '16px'; h.style.fontWeight = '600';
    if (props.onClick) { h.style.cursor = 'pointer'; h.addEventListener('click', () => props.onClick()); }
    return h;
  }
  // KT: SharedYouTubePlaylistRow(items, key, contentPadding, modifier, itemContent)
  function SharedYouTubePlaylistRow(props) {
    const row = LazyRow({ contentPadding: props.contentPadding || 0, horizontalArrangement: { gap: 16 }, modifier: props.modifier || Modifier() });
    (props.items || []).forEach((it) => { const n = props.itemContent ? props.itemContent(it) : null; if (n) row.appendChild(n); });
    return row;
  }
  // KT: PlaylistGridItem(playlist, autoPlaylist, modifier) -- copertina (thumbnail o prima traccia) + nome
  function PlaylistGridItem(props) {
    const pl = props.playlist || {};
    const entity = pl.playlist || {};
    const thumb = entity.thumbnailUrl || (pl.songThumbnails && pl.songThumbnails[0]) || '';
    const card = document.createElement('div');
    card.style.width = '160px'; card.style.flex = '0 0 auto';
    const img = document.createElement('img');
    img.src = thumb; img.style.width = '100%'; img.style.aspectRatio = '1 / 1'; img.style.objectFit = 'cover'; img.style.borderRadius = '6px';
    card.appendChild(img);
    const t = document.createElement('div'); t.textContent = entity.name || ''; t.style.marginTop = '8px'; t.style.fontWeight = '600';
    card.appendChild(t);
    if (props.modifier) props.modifier.applyTo(card);
    return card;
  }
  // KT: ReverieHomeCard(month, onClick, modifier) -- card con il mese
  function ReverieHomeCard(props) {
    const card = document.createElement('div');
    card.style.width = '300px'; card.style.padding = '16px'; card.style.border = '1px solid rgba(255,255,255,0.12)'; card.style.borderRadius = '6px'; card.style.cursor = 'pointer';
    card.textContent = String(props.month);
    if (props.onClick) card.addEventListener('click', () => props.onClick());
    if (props.modifier) props.modifier.applyTo(card);
    return card;
  }


  // ---------- BATCH 3 (10 dipendenze) ----------
  // KT: HeroCarouselSection(items, onCoverError, onArtistClick, onArtistLongClick) -- card artista con copertina
  function HeroCarouselSection(props) {
    const row = LazyRow({ contentPadding: 20, horizontalArrangement: { gap: 16 } });
    (props.items || []).forEach((it) => {
      const card = document.createElement('div');
      card.style.width = '280px'; card.style.flex = '0 0 auto'; card.style.cursor = 'pointer';
      const img = document.createElement('img');
      img.src = it.coverUrl || ''; img.style.width = '100%'; img.style.aspectRatio = '16 / 9'; img.style.objectFit = 'cover'; img.style.borderRadius = '8px';
      img.addEventListener('error', () => { if (props.onCoverError) props.onCoverError(it); });
      card.appendChild(img);
      const t = document.createElement('div'); t.textContent = it.artistName || ''; t.style.marginTop = '8px'; t.style.fontWeight = '700';
      card.appendChild(t);
      card.addEventListener('click', () => { if (props.onArtistClick) props.onArtistClick(it.artistId); });
      card.addEventListener('contextmenu', (e) => { e.preventDefault(); if (props.onArtistLongClick) props.onArtistLongClick(it); });
      row.appendChild(card);
    });
    return row;
  }
  // KT: ReverieAvailabilityCard(currentMonth, firstFullMonth, modifier) -- messaggio di disponibilita' archivio
  function ReverieAvailabilityCard(current, first, modifier) {
    const card = document.createElement('div');
    card.style.padding = '16px'; card.style.border = '1px solid rgba(255,255,255,0.12)'; card.style.borderRadius = '6px'; card.style.opacity = '0.8';
    card.textContent = first ? `Disponibile da ${String(first)}` : `Disponibile dopo ${String(current)}`;
    if (modifier) modifier.applyTo(card);
    return card;
  }
  // KT: NewReleaseChartsShelf(navController, chartViewModel, preload) -- classifiche: nessuna sorgente dati desktop ancora
  function NewReleaseChartsShelf() { return null; }
  // KT: HomeFollowedReleases(navController, snackbarHostState, isVisible) -- nessuna sorgente dati desktop ancora
  function HomeFollowedReleases() { return null; }
  // KT: rememberArtistThumbnail(artistId, name, url) -- restituisce l'URL disponibile (nessuna cache artista)
  function rememberArtistThumbnail(artistId, name, url) { return url || null; }
  // KT: rememberIrideArtistArtwork(...).value -- nessun artwork artista esterno: null
  function rememberIrideArtistArtwork() { return { value: null }; }
  // KT: rememberCollectionExpansionTile(kind, id, ...) -- nessuna espansione: modificatori vuoti
  function rememberCollectionExpansionTile() { return { first: Modifier(), second: Modifier() }; }
  // KT: rememberArtistPortraitTile(...) -- nessun ritratto artista: modificatori vuoti
  function rememberArtistPortraitTile() { return { first: Modifier(), second: Modifier() }; }
  // KT: SongMenu / YouTubeSongMenu(song|originalSong, navController, onDismiss) -- menu canzone: titolo + chiusura
  function songMenuNode(song, onDismiss) {
    const box = document.createElement('div');
    box.setAttribute('role', 'menu');
    const t = document.createElement('div'); t.textContent = (song && song.title) || ''; t.style.fontWeight = '700';
    box.appendChild(t);
    const close = document.createElement('button'); close.textContent = '×';
    close.addEventListener('click', () => { if (onDismiss) onDismiss(); });
    box.appendChild(close);
    return box;
  }
  function SongMenu(props) { return songMenuNode(props.originalSong, props.onDismiss); }
  function YouTubeSongMenu(props) { return songMenuNode(props.song, props.onDismiss); }


  // KT: IrideFolioDisplayHeadings(menuState) { content } -- PARZIALE: solo passa il contenuto (intestazioni display non ancora portate)
  function IrideFolioDisplayHeadings(props, body) { return run(body); }


  // KT: LocalPlayerAwareWindowInsets (CompositionLocal) -- PARZIALE: nessun mini-player nel desktop, padding 0
  const LocalPlayerAwareWindowInsets = () => ({ asPaddingValues() { return { calculateBottomPadding() { return 0; } }; } });


  // ---------- BATCH 4: valori, forme, colori e helper usati dalla home ----------
  const CircleShape = 'circle';                                                 // KT: CircleShape
  function FolioShape(radius) { return { radius }; }                            // KT: FolioShape(dp)
  const Color = { Black: { copy: (o) => `rgba(0,0,0,${(o && o.alpha) != null ? o.alpha : 1})` }, White: '#ffffff', Transparent: 'transparent' };
  const ColorFilter = { tint: (c) => ({ tint: c }) };
  const Rect = { Zero: { x: 0, y: 0, width: 0, height: 0 } };
  const IrideFolio = { ShelfMosaicGap: 6, ShelfItemSpacing: 12, HorizontalPadding: 20, SatoshiTextHeightScale: 1 };
  const IrideSoftBorderWidth = 1;
  const SatoshiFontFamily = 'Satoshi, sans-serif';
  function folioTypewriterFontFamily() { return 'monospace'; }
  function irideFolioShelfTypography() { return typo('bodyLarge'); }
  const CardDefaults = { cardColors: (o) => o };
  const WindowInsets = Object.assign(() => ({}), { statusBars: {} });
  function spacedBy(gap) { return { gap }; }
  function strokeCard() { return palette().strokeCard; }
  function strokeItemSoft() { return palette().strokeItemSoft; }
  function painterResource(name) { return name; }
  function stringResource(key) {                                                 // KT: stringResource(R.string.x)
    const t = (global.IrideHomeStrings || {})[key];
    return t != null ? t : key;
  }
  function compactHomeTitle(name) { return name; }
  function homePageTitle() { return stringResource('home'); }
  const asPlaylistItem = (x) => x;
  const asSongItem = (x) => x;
  const toMediaMetadata = (x) => x;
  const toMediaItem = (x) => x;
  const PlaylistEntity = Object.assign((o) => o, { LIKED_PLAYLIST_ID: 'LM' });
  const Playlist = (o) => o;
  const ListQueue = (o) => o;
  const YouTubeQueue = (endpoint, metadata) => ({ endpoint, metadata });
  const WatchEndpoint = (o) => o;
  const ImageRequest = (o) => o;
  const MutableInteractionSource = () => ({});
  const tween = (o) => o;
  function animateFloatAsState(target) { return { value: target }; }
  function animateFloat() { return 1; }
  function mutableStateMapOf() { return {}; }
  function rememberScrollState() { return {}; }
  function derivedStateOf(fn) { return { value: fn() }; }
  function listItemVisible() { return false; }
  function lastVisibleIndex() { return null; }
  function snapshotFlow() { return { collect() {} }; }
  function uuid() { return 'uuid-' + Math.random().toString(36).slice(2); }
  function collectAsStateWithLifecycle(_flow, _key, fallback) { return fallback; }
  function irideDepthEnter() { return Modifier(); }
  function rubberBandOverscroll() { return Modifier(); }
  function irideFolioCombinedClickable(o) { return Modifier().combinedClickable(o); }
  function LocalAnimatedVisibilityScope() { return null; }
  function LocalArtistPortraitNavigation() { return null; }
  function LocalArtistPortraitTransitionActive() { return false; }
  // KT: YearMonth.now() -- formato AAAA-MM
  const YearMonth = { now() { const d = new Date(); return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`; } };
  // KT: MixMood -- PARZIALE: nessun mood da titolo; default CHILL
  const MixMood = { fromMixTitle: () => null, fromChip: () => null, CHILL: 'CHILL' };
  // KT: hashCode(String) (Java) -- identico al calcolo Java
  function hashCode(s) { let h = 0; for (let i = 0; i < s.length; i++) h = (Math.imul(31, h) + s.charCodeAt(i)) | 0; return h; }
  // KT: kotlin.random.Random(seed) -- generatore deterministico (mulberry32)
  function Random(seed) { return { seed: seed | 0 }; }
  function nextRandom(rnd) {
    if (!rnd) return Math.random();
    rnd.seed = (rnd.seed + 0x6D2B79F5) | 0;
    let t = Math.imul(rnd.seed ^ (rnd.seed >>> 15), 1 | rnd.seed);
    t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t;
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  }
  // KT: Iterable.shuffled(random) -- Fisher-Yates con il generatore sopra
  function shuffled(list, rnd) {
    const a = (list || []).slice();
    for (let i = a.length - 1; i > 0; i--) { const j = Math.floor(nextRandom(rnd) * (i + 1)); [a[i], a[j]] = [a[j], a[i]]; }
    return a;
  }
  // KT: Iterable.chunked(n)
  function chunked(list, n) { const out = []; for (let i = 0; i < (list || []).length; i += n) out.push(list.slice(i, i + n)); return out; }
  // KT: distinctBy { id }
  function distinctById(list) { const seen = new Set(); return (list || []).filter((x) => (seen.has(x.id) ? false : (seen.add(x.id), true))); }
  // KT: Menu (album, artista, playlist, canzone YouTube): titolo + chiudi, azioni da portare
  function menuNode(item, onDismiss) {
    const box = document.createElement('div');
    box.setAttribute('role', 'menu');
    const t = document.createElement('div'); t.textContent = (item && (item.title || (item.playlist && item.playlist.name))) || ''; t.style.fontWeight = '700';
    box.appendChild(t);
    const close = document.createElement('button'); close.textContent = '×';
    close.addEventListener('click', () => { if (onDismiss) onDismiss(); });
    box.appendChild(close);
    return box;
  }
  function AlbumMenu(props) { return menuNode(props.originalAlbum, props.onDismiss); }
  function ArtistMenu(props) { return menuNode(props.originalArtist, props.onDismiss); }
  function PlaylistMenu(props) { return menuNode(props.playlist, props.onDismiss); }
  function YouTubeAlbumMenu(props) { return menuNode(props.albumItem, props.onDismiss); }
  function YouTubeArtistMenu(props) { return menuNode(props.artist, props.onDismiss); }
  function YouTubePlaylistMenu(props) { return menuNode(props.playlist, props.onDismiss); }
  // KT: SongMenuIconButton(onClick) { icon } -- pulsante con icona
  function SongMenuIconButton(props, body) {
    const b = document.createElement('button');
    b.style.background = 'transparent'; b.style.border = 'none'; b.style.cursor = 'pointer';
    b.addEventListener('click', (e) => { e.stopPropagation(); if (props.onClick) props.onClick(); });
    return appendAll(b, run(body));
  }
  // KT: TextButton(onClick) { content }
  function TextButton(props, body) {
    const b = document.createElement('button');
    b.style.background = 'transparent'; b.style.border = 'none'; b.style.cursor = 'pointer'; b.style.color = palette().textPrimary;
    b.addEventListener('click', () => { if (props.onClick) props.onClick(); });
    return appendAll(b, run(body));
  }
  // KT: CircularProgressIndicator / LinearProgressIndicator (progress lineare)
  function CircularProgressIndicator(props) { const el = document.createElement('span'); el.style.display = 'inline-block'; el.style.width = px(props.modifier ? 16 : 16); el.style.height = '16px'; el.style.borderRadius = '50%'; el.style.border = '2px solid currentColor'; el.style.opacity = '0.6'; return el; }
  function LinearProgressIndicator(props) {
    const el = document.createElement('div'); el.style.height = '2px'; el.style.background = props.trackColor || 'rgba(255,255,255,0.2)';
    const bar = document.createElement('div'); bar.style.height = '100%'; bar.style.width = `${Math.round((props.progress ? props.progress() : 0) * 100)}%`; bar.style.background = props.color || '#fff';
    el.appendChild(bar); return el;
  }
  // KT: MixPosterPlaceholder(title, mood) -- poster senza immagine
  function MixPosterPlaceholder(props) { const el = document.createElement('div'); el.textContent = props.title || ''; el.style.width = '100%'; el.style.height = '100%'; el.style.background = 'rgba(255,255,255,0.06)'; return el; }
  // KT: IrideFolioSectionSkeleton / IrideFolioShelfSkeleton -- segnaposto di caricamento
  function skeletonBox(size) { const el = document.createElement('div'); el.style.width = px(size || 160); el.style.height = px(size || 160); el.style.flex = '0 0 auto'; el.style.borderRadius = '6px'; el.style.background = 'rgba(255,255,255,0.06)'; return el; }
  function IrideFolioSectionSkeleton(size) { return skeletonBox(size); }
  function IrideFolioShelfSkeleton(size) { return skeletonBox(size); }
  // KT: Surface/ Card ok; KT: collectDeferredHomeState gestito in home-components

  global.IrideHomeDom = {
    typography: typo,
    Modifier, MaterialTheme, Box, Column, Row, Spacer, Text, Image, AsyncImage, Icon, Surface, Card,
    Scaffold, BoxWithConstraints, CompositionLocalProvider, item, LazyRow, IridePageLayout,
    IrideFolioSection, IrideFolioShelfHeader, IrideFolioMediaShelf, palette, typo,
    resize, ItemThumbnail, YouTubeGridItem, YouTubeListItem, SongListItem, SharedQuickPicksCarousel,
    SharedHomeSectionTitle, SharedYouTubePlaylistRow, PlaylistGridItem, ReverieHomeCard,
    HeroCarouselSection, ReverieAvailabilityCard, NewReleaseChartsShelf, HomeFollowedReleases,
    rememberArtistThumbnail, rememberIrideArtistArtwork, rememberCollectionExpansionTile,
    rememberArtistPortraitTile, SongMenu, YouTubeSongMenu, IrideFolioDisplayHeadings, LocalPlayerAwareWindowInsets,
    CircleShape, FolioShape, Color, ColorFilter, Rect, IrideFolio, IrideSoftBorderWidth, SatoshiFontFamily, folioTypewriterFontFamily, irideFolioShelfTypography, CardDefaults, WindowInsets, spacedBy, strokeCard, strokeItemSoft, painterResource, stringResource, compactHomeTitle, homePageTitle, asPlaylistItem, asSongItem, toMediaMetadata, toMediaItem, PlaylistEntity, Playlist, ListQueue, YouTubeQueue, WatchEndpoint, ImageRequest, MutableInteractionSource, tween, animateFloatAsState, animateFloat, mutableStateMapOf, rememberScrollState, derivedStateOf, listItemVisible, lastVisibleIndex, snapshotFlow, uuid, collectAsStateWithLifecycle, irideDepthEnter, rubberBandOverscroll, irideFolioCombinedClickable, LocalAnimatedVisibilityScope, LocalArtistPortraitNavigation, LocalArtistPortraitTransitionActive, YearMonth, MixMood, hashCode, Random, shuffled, chunked, distinctById, AlbumMenu, ArtistMenu, PlaylistMenu, YouTubeAlbumMenu, YouTubeArtistMenu, YouTubePlaylistMenu, SongMenuIconButton, TextButton, CircularProgressIndicator, LinearProgressIndicator, MixPosterPlaceholder, IrideFolioSectionSkeleton, IrideFolioShelfSkeleton,
  };
})(typeof window !== 'undefined' ? window : globalThis);
