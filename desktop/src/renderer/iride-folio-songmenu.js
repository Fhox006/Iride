/* SongMenu.kt 1:1: header, NewActionGrid, Material3MenuGroup (icone drawable Android, testi strings.xml) */
(function () {
  const P = {
    edit: ['0 0 960 960', 'M200,760L257,760L648,369L591,312L200,703L200,760ZM120,840L120,670L648,143Q660,132 674.5,126Q689,120 705,120Q721,120 736,126Q751,132 762,144L817,200Q829,211 834.5,226Q840,241 840,256Q840,272 834.5,286.5Q829,301 817,313L290,840L120,840ZM760,256L760,256L704,200L704,200L760,256ZM619,341L591,312L591,312L648,369L648,369L619,341Z'],
    playlist_add: ['0 0 960 960', 'M120,640L120,560L400,560L400,640L120,640ZM120,480L120,400L560,400L560,480L120,480ZM120,320L120,240L560,240L560,320L120,320ZM640,800L640,640L480,640L480,560L640,560L640,400L720,400L720,560L880,560L880,640L720,640L720,800L640,800Z'],
    share: ['0 0 24 24', 'M15,5.63L20.66,12L15,18.37V15v-1h-1c-3.96,0 -7.14,1 -9.75,3.09c1.84,-4.07 5.11,-6.4 9.89,-7.1L15,9.86V9V5.63M14,3v6C6.22,10.13 3.11,15.33 2,21c2.78,-3.97 6.44,-6 12,-6v6l8,-9L14,3L14,3z'],
    playlist_play: ['0 0 960 960', 'M100,640L100,560L420,560L420,640L100,640ZM100,480L100,400L580,400L580,480L100,480ZM100,320L100,240L580,240L580,320L100,320ZM620,840L620,520L860,680L620,840Z'],
    queue_music: ['0 0 960 960', 'M640,800Q590,800 555,765Q520,730 520,680Q520,630 555,595Q590,560 640,560Q651,560 661,561.5Q671,563 680,568L680,240L880,240L880,320L760,320L760,680Q760,730 725,765Q690,800 640,800ZM120,640L120,560L440,560L440,640L120,640ZM120,480L120,400L600,400L600,480L120,480ZM120,320L120,240L600,240L600,320L120,320Z'],
    add: ['0 0 960 960', 'M440,760L440,520L200,520L200,440L440,440L440,200L520,200L520,440L760,440L760,520L520,520L520,760L440,760Z'],
    remove: ['0 0 960 960', 'M200,520L200,440L760,440L760,520L200,520Z'],
    download: ['0 0 960 960', 'M240,800Q207,800 183.5,776.5Q160,753 160,720L160,600L240,600L240,720Q240,720 240,720Q240,720 240,720L720,720Q720,720 720,720Q720,720 720,720L720,600L800,600L800,720Q800,753 776.5,776.5Q753,800 720,800L240,800ZM480,640L280,440L336,382L440,486L440,160L520,160L520,486L624,382L680,440L480,640Z'],
    artist: ['0 0 960 960', 'M740,400h140v80h-80v220q0,42 -29,71t-71,29q-42,0 -71,-29t-29,-71q0,-42 29,-71t71,-29q8,0 18,1.5t22,6.5v-208ZM120,800v-112q0,-35 17.5,-63t46.5,-43q62,-31 126,-46.5T440,520q42,0 83.5,6.5T607,546q-20,12 -36,29t-28,37q-26,-6 -51.5,-9t-51.5,-3q-57,0 -112,14t-108,40q-9,5 -14.5,14t-5.5,20v32h321q2,20 9.5,40t20.5,40L120,800ZM440,480q-66,0 -113,-47t-47,-113q0,-66 47,-113t113,-47q66,0 113,47t47,113q0,66 -47,113t-113,47ZM440,400q33,0 56.5,-23.5T520,320q0,-33 -23.5,-56.5T440,240q-33,0 -56.5,23.5T360,320q0,33 23.5,56.5T440,400ZM440,320ZM440,720Z'],
    delete: ['0 0 960 960', 'M280,840q-33,0 -56.5,-23.5T200,760v-520h-40v-80h200v-40h240v40h200v80h-40v520q0,33 -23.5,56.5T680,840L280,840ZM680,240L280,240v520h400v-520ZM360,680h80v-360h-80v360ZM520,680h80v-360h-80v360ZM280,240v520,-520Z'],
    star: ['0 0 24 24', 'M12 17.27L18.18 21l-1.64-7.03L22 9.24l-7.19-.61L12 2 9.19 8.63 2 9.24l5.46 4.73L5.82 21z'],
    star_border: ['0 0 24 24', 'M22 9.24l-7.19-.62L12 2 9.19 8.63 2 9.24l5.46 4.73L5.82 21 12 17.27 18.18 21l-1.63-7.03L22 9.24zM12 15.4l-3.76 2.27 1-4.28-3.32-2.88 4.38-.38L12 6.1l1.71 4.04 4.38.38-3.32 2.88 1 4.28L12 15.4z'],
    close: ['0 0 960 960', 'm256,760 -56,-56 224,-224 -224,-224 56,-56 224,224 224,-224 56,56 -224,224 224,224 -56,56 -224,-224 -224,224Z'],
    block: ['0 0 960 960', 'M480,880q-83,0 -156,-31.5T197,763q-54,-54 -85.5,-127T80,480q0,-83 31.5,-156T197,197q54,-54 127,-85.5T480,80q83,0 156,31.5T763,197q54,54 85.5,127T880,480q0,83 -31.5,156T763,763q-54,54 -127,85.5T480,880ZM480,800q54,0 104,-17.5t92,-50.5L228,284q-33,42 -50.5,92T160,480q0,134 93,227t227,93ZM732,676q33,-42 50.5,-92T800,480q0,-134 -93,-227t-227,-93q-54,0 -104,17.5T284,228l448,448Z'],
    image: ['0 -960 960 960', 'm460-380 280-180-280-180v360ZM320-240q-33 0-56.5-23.5T240-320v-480q0-33 23.5-56.5T320-880h480q33 0 56.5 23.5T880-800v480q0 33-23.5 56.5T800-240H320Zm0-80h480v-480H320v480ZM160-80q-33 0-56.5-23.5T80-160v-560h80v560h560v80H160Zm160-720v480-480Z'],
  };
  const ic = (n, size) => '<svg class="sm-ic" viewBox="' + P[n][0] + '" width="' + (size || 24) + '" height="' + (size || 24) + '" fill="currentColor" aria-hidden="true"><path d="' + P[n][1] + '"/></svg>';
  const esc = (s) => String(s == null ? '' : s).replace(/[&<>"]/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' }[c]));

  window.irideRenderSongMenu = function (menu, track, o) {
    const tr = (s) => (typeof window.t === 'function' ? window.t(s) : s);
    const run = (fn) => (ev) => { ev.stopPropagation(); o.close(); fn(); };
    menu.classList.add('iride-songmenu');
    menu.innerHTML = '';
    const el = (cls, html) => { const d = document.createElement('div'); d.className = cls; if (html != null) d.innerHTML = html; return d; };

    // SongListItem: miniatura 48 + titolo + artista + stella
    const head = el('sm-head');
    const thumb = o.thumb ? '<img class="sm-thumb" src="' + esc(o.thumb) + '" alt="">' : '<div class="sm-thumb"></div>';
    head.innerHTML = thumb + '<div class="sm-head-txt"><div class="sm-head-title">' + esc(track.title) + '</div><div class="sm-head-sub">' + esc(track.artist || '') + '</div></div>';
    const star = document.createElement('button');
    star.type = 'button'; star.className = 'sm-star' + (o.isFav ? ' on' : '');
    star.innerHTML = ic(o.isFav ? 'star' : 'star_border');
    star.addEventListener('click', run(o.byId.fav.action));
    head.appendChild(star);
    menu.appendChild(head);
    menu.appendChild(el('sm-hr'));

    // NewActionGrid: Edit / Add to playlist / Share
    const grid = el('sm-grid');
    [['edit', 'Edit', 'rename'], ['playlist_add', 'Add to playlist', 'playlist'], ['share', 'Share', 'share']].forEach(([i, label, id]) => {
      const it = o.byId[id]; if (!it) return;
      const b = document.createElement('button');
      b.type = 'button'; b.className = 'sm-action';
      b.innerHTML = '<span class="sm-action-ic">' + ic(i, 28) + '</span><span class="sm-action-tx">' + esc(tr(label)) + '</span>';
      b.addEventListener('click', run(it.action));
      grid.appendChild(b);
    });
    menu.appendChild(grid);

    // Material3MenuGroup
    const group = (rows) => {
      rows = rows.filter(Boolean);
      if (!rows.length) return;
      const g = el('sm-group');
      rows.forEach((r) => {
        const b = document.createElement('button');
        b.type = 'button'; b.className = 'sm-item' + (r.danger ? ' danger' : '');
        b.innerHTML = ic(r.icon) + '<span class="sm-item-col"><span class="sm-item-title">' + esc(tr(r.title)) + '</span>' +
          (r.desc ? '<span class="sm-item-desc">' + esc(tr(r.desc)) + '</span>' : '') + '</span>';
        b.addEventListener('click', run(r.action));
        g.appendChild(b);
      });
      menu.appendChild(g);
    };
    const B = o.byId;
    group([
      o.playNext && { icon: 'playlist_play', title: 'Play next', desc: 'Play next in the queue', action: o.playNext },
      B.queue && { icon: 'queue_music', title: 'Add to queue', desc: 'Add to the end of the queue', action: B.queue.action },
    ]);
    group([
      B['pin-sidebar'] && { icon: B['pin-sidebar'].active ? 'remove' : 'add', title: B['pin-sidebar'].active ? 'Unpin from sidebar' : 'Pin to sidebar', action: B['pin-sidebar'].action },
      B.pin && { icon: B.pin.active ? 'remove' : 'add', title: B.pin.active ? 'Unpin from Speed dial' : 'Pin to Speed dial', action: B.pin.action },
      B.banner && { icon: 'image', title: 'Change banner', action: B.banner.action },
      B['remove-pl'] && { icon: 'delete', title: 'Remove from playlist', action: B['remove-pl'].action },
      B['remove-q'] && { icon: 'delete', title: 'Remove from queue', action: B['remove-q'].action },
      B['remove-local'] && { icon: 'delete', title: 'Remove from library', action: B['remove-local'].action },
    ]);
    group([B.download && { icon: 'download', title: 'Download', desc: 'Download for offline playback', action: B.download.action }]);
    group([track.artist && o.viewArtist && { icon: 'artist', title: 'View artist', action: o.viewArtist }]);
    group([
      B['block-track'] && { icon: 'block', title: B['block-track'].label, action: B['block-track'].action, danger: true },
      B['block-artist'] && { icon: 'block', title: B['block-artist'].label, action: B['block-artist'].action, danger: true },
    ]);
  };
})();
