/* Rain simulation: vendored Raindrop FX, SardineFish, MIT. See assets/licenses.
 * This adapter adds lyrics, a shared curved-glass pass, and a bounded 30 fps lifecycle. */
(function () {
    'use strict';
    const screen = document.getElementById('screen');
    const background = document.createElement('canvas');
    const rain = document.createElement('canvas');
    const staticCanvas = document.createElement('canvas');
    const staticPaint = staticCanvas.getContext('2d');
    const paint = background.getContext('2d');
    const fallback = new Image();
    fallback.src = 'rain-static.webp';
    let lines = [], synced = true, manualIndex = null, manualUntil = 0;
    let position = 0, playing = false, reducedMotion = false, visible = true, message = '';
    let effect = null, ready = false, frame = 0, lastFrame = 0, lastScene = '', lastLight = '';
    let lastPositionAt = performance.now(), lastRainTime = 0, rainTime = 5, failed = false;
    let gl, program, texture, surface;

    function shader(type, source) {
        const result = gl.createShader(type);
        gl.shaderSource(result, source); gl.compileShader(result);
        if (!gl.getShaderParameter(result, gl.COMPILE_STATUS)) throw new Error(gl.getShaderInfoLog(result));
        return result;
    }
    function initializeCurve() {
        gl = screen.getContext('webgl', { alpha: false, antialias: false, preserveDrawingBuffer: true });
        if (!gl) { surface = screen.getContext('2d'); return; }
        program = gl.createProgram();
        gl.attachShader(program, shader(gl.VERTEX_SHADER,
            'attribute vec2 point; varying vec2 uv; void main(){ uv=(point+1.0)*0.5; gl_Position=vec4(point,0.,1.); }'));
        gl.attachShader(program, shader(gl.FRAGMENT_SHADER, `
            precision mediump float;
            varying vec2 uv;
            uniform sampler2D image;
            void main() {
                vec2 p = uv * 2.0 - 1.0;
                vec2 curved = p * (1.0 + 0.085 * dot(p, p));
                vec2 sampleUv = curved * 0.5 + 0.5;
                float edge = 1.0 - smoothstep(0.92, 1.04, max(abs(curved.x), abs(curved.y)));
                vec3 color = texture2D(image, sampleUv).rgb;
                float vignette = 1.0 - 0.36 * dot(p * vec2(0.8, 1.0), p * vec2(0.8, 1.0));
                float scanline = 0.985 + 0.015 * sin(uv.y * 900.0);
                gl_FragColor = vec4(color * vignette * edge * scanline, 1.0);
            }`));
        gl.linkProgram(program);
        if (!gl.getProgramParameter(program, gl.LINK_STATUS)) throw new Error('Curved glass unavailable');
        gl.useProgram(program);
        const buffer = gl.createBuffer(); gl.bindBuffer(gl.ARRAY_BUFFER, buffer);
        gl.bufferData(gl.ARRAY_BUFFER, new Float32Array([-1,-1, 1,-1, -1,1, -1,1, 1,-1, 1,1]), gl.STATIC_DRAW);
        const point = gl.getAttribLocation(program, 'point');
        gl.enableVertexAttribArray(point); gl.vertexAttribPointer(point, 2, gl.FLOAT, false, 0, 0);
        texture = gl.createTexture(); gl.bindTexture(gl.TEXTURE_2D, texture);
        gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MIN_FILTER, gl.LINEAR);
        gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MAG_FILTER, gl.LINEAR);
        gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_S, gl.CLAMP_TO_EDGE);
        gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_T, gl.CLAMP_TO_EDGE);
        gl.pixelStorei(gl.UNPACK_FLIP_Y_WEBGL, true);
        gl.uniform1i(gl.getUniformLocation(program, 'image'), 0);
    }
    function curve(source) {
        if (gl && !gl.isContextLost()) {
            gl.viewport(0, 0, screen.width, screen.height);
            gl.texImage2D(gl.TEXTURE_2D, 0, gl.RGBA, gl.RGBA, gl.UNSIGNED_BYTE, source);
            gl.drawArrays(gl.TRIANGLES, 0, 6);
        } else if (surface) {
            surface.fillStyle = '#050506'; surface.fillRect(0, 0, screen.width, screen.height);
            // Same bow for text and the sourced static rain on devices without WebGL.
            for (let y = 0; y < source.height; y += 3) {
                const distance = (y / source.height - 0.5) * 2;
                const inset = Math.pow(distance, 2) * source.width * 0.035;
                surface.drawImage(source, 0, y, source.width, 3, inset, y, source.width - inset * 2, 3);
            }
        }
    }
    function font(size) { paint.font = size + 'px Anton, sans-serif'; }
    function fittedSize(text, base, width) {
        font(base);
        return Math.min(base, base * width / Math.max(1, paint.measureText(text).width));
    }
    function wrap(text, size, width) {
        font(size);
        const rows = []; let row = '';
        for (const word of text.toUpperCase().split(/\s+/)) {
            const next = row ? row + ' ' + word : word;
            if (row && paint.measureText(next).width > width) { rows.push(row); row = word; } else row = next;
        }
        if (row) rows.push(row);
        return rows;
    }
    function layout(scene) {
        const width = background.width, height = background.height;
        if (!scene.words.length) {
            const text = scene.text || scene.message || '';
            let size = Math.min(width * (scene.message ? 0.046 : 0.105), height * 0.2);
            let rows = wrap(text, size, width * 0.76);
            while (rows.length > 3 && size > width * 0.024) { size *= 0.88; rows = wrap(text, size, width * 0.76); }
            return rows.map((text, index) => ({ text, size: fittedSize(text, size, width * 0.76),
                x: width / 2, y: height / 2 + (index - (rows.length - 1) / 2) * size * 1.23, centered: true }));
        }
        const result = [], groups = scene.emphasis && scene.words.length > 1
            ? [scene.words.slice(0, -1), scene.words.slice(-1)] : [scene.words];
        groups.forEach((words, row) => {
            const text = words.map(word => word.text).join(' ').toUpperCase();
            const large = scene.emphasis && row === groups.length - 1;
            const size = fittedSize(text, Math.min(width * (large ? 0.155 : 0.115), height * (large ? 0.28 : 0.2)), width * 0.77);
            font(size); const gap = paint.measureText(' ').width;
            const total = words.reduce((sum, word) => sum + paint.measureText(word.text.toUpperCase()).width, 0) + gap * (words.length - 1);
            let x = (width - total) / 2;
            for (const word of words) {
                result.push({ ...word, text: word.text.toUpperCase(), size, x,
                    y: height * 0.5 + (groups.length === 1 ? 0 : row === 0 ? -width * 0.08 : width * 0.065) });
                x += paint.measureText(word.text.toUpperCase()).width + gap;
            }
        });
        return result;
    }
    let fixedLayout = [];
    function drawLyrics(scene, time, flicker) {
        const width = background.width, height = background.height;
        const glow = paint.createRadialGradient(width * 0.5, height * 0.48, 0, width * 0.5, height * 0.5, Math.hypot(width, height) * 0.5);
        glow.addColorStop(0, '#353639'); glow.addColorStop(0.5, '#17181b'); glow.addColorStop(1, '#050507');
        paint.fillStyle = glow; paint.fillRect(0, 0, width, height);
        paint.textBaseline = 'middle';
        for (const word of fixedLayout) {
            const active = !scene.words.length || scene.preview || (time >= word.start && time < word.end);
            const sung = scene.words.length && time >= word.end;
            const light = active ? (flicker ? 0.45 : 1) : sung ? 0.5 : 0.13;
            font(word.size); paint.textAlign = word.centered ? 'center' : 'left';
            paint.fillStyle = 'rgba(255,255,255,' + light + ')';
            paint.shadowColor = 'rgba(246,249,255,' + (active ? 0.8 : 0.12) + ')';
            paint.shadowBlur = active ? width * 0.031 : width * 0.006;
            paint.fillText(word.text, word.x, word.y);
            if (active) { paint.shadowBlur = width * 0.009; paint.fillText(word.text, word.x, word.y); }
        }
        paint.shadowBlur = 0;
        if (scene.translation) {
            paint.font = width * 0.027 + 'px sans-serif'; paint.textAlign = 'center';
            paint.fillStyle = 'rgba(255,255,255,.65)';
            paint.fillText(scene.translation, width / 2, height * 0.85, width * 0.76);
        }
    }
    function staticFrame() {
        staticPaint.globalCompositeOperation = 'source-over'; staticPaint.globalAlpha = 1;
        staticPaint.drawImage(background, 0, 0);
        if (fallback.complete && fallback.naturalWidth) {
            staticPaint.globalCompositeOperation = 'screen'; staticPaint.globalAlpha = 0.6;
            staticPaint.drawImage(fallback, 0, 0, staticCanvas.width, staticCanvas.height);
        }
        curve(staticCanvas);
    }
    function render(now) {
        frame = 0;
        if (!visible) return;
        if (now - lastFrame < 32) { schedule(); return; }
        lastFrame = now;
        const time = position + (playing ? Math.min(now - lastPositionAt, 250) : 0);
        if (manualIndex != null && synced && now > manualUntil) manualIndex = null;
        const scene = TvLyricsModel.scene(lines, time, synced, manualIndex, message);
        if (scene.key !== lastScene) { lastScene = scene.key; fixedLayout = layout(scene); lastLight = ''; }
        // One brief dip on selected phrases, never per letter and never while paused.
        const elapsed = scene.words.length ? time - scene.words[0].start : 0;
        const flicker = playing && !reducedMotion && scene.index % 4 === 2 && elapsed >= 740 && elapsed < 790;
        const light = scene.words.map(word => time < word.start ? 0 : time < word.end ? 1 : 2).join('') + ':' + flicker;
        const changed = light !== lastLight;
        if (changed) {
            drawLyrics(scene, time, flicker); lastLight = light;
            if (ready) effect.setBackground(background);
        }
        if (ready) {
            const dt = lastRainTime ? Math.min((now - lastRainTime) / 1000, 0.05) : 1 / 30;
            lastRainTime = now;
            const rainDt = playing && !reducedMotion ? dt : 0;
            rainTime += rainDt;
            const tick = { dt: rainDt, total: rainTime };
            effect.simulator.update(tick); effect.renderer.render(effect.simulator.raindrops, tick);
            curve(rain);
        } else if (changed) staticFrame();
        if (playing && !reducedMotion) schedule();
    }
    function schedule() { if (!frame && visible) frame = requestAnimationFrame(render); }
    async function resize() {
        if (!screen.clientWidth || !screen.clientHeight) return;
        const scale = Math.min(devicePixelRatio, 1.5, 768 / screen.clientWidth, 1024 / screen.clientHeight);
        const width = Math.max(1, Math.round(screen.clientWidth * scale));
        const height = Math.max(1, Math.round(screen.clientHeight * scale));
        screen.width = background.width = rain.width = staticCanvas.width = width;
        screen.height = background.height = rain.height = staticCanvas.height = height;
        lastScene = ''; lastLight = '';
        if (ready) effect.resize(width, height);
        schedule();
    }
    window.IrideTV = {
        update(data) {
            if (data.lines) { lines = TvLyricsModel.prepare(data.lines); lastScene = ''; manualIndex = null; }
            if (data.synced != null) synced = data.synced;
            if (data.position != null) { position = data.position; lastPositionAt = performance.now(); }
            if (data.playing != null) playing = data.playing;
            if (data.reducedMotion != null) reducedMotion = data.reducedMotion;
            if (data.message != null && message !== data.message) { message = data.message; lastScene = ''; }
            if (data.visible != null) { visible = data.visible; lastRainTime = 0; }
            schedule();
        },
        scroll(direction) {
            const active = TvLyricsModel.scene(lines, position, synced, manualIndex, message).index;
            manualIndex = Math.max(0, Math.min(lines.length - 1, active + direction));
            manualUntil = performance.now() + 4000; schedule();
        },
        dispose() { visible = false; cancelAnimationFrame(frame); frame = 0; if (effect) effect.destroy(); }
    };
    document.addEventListener('visibilitychange', () => { visible = !document.hidden; schedule(); });
    screen.addEventListener('webglcontextlost', event => { event.preventDefault(); ready = false; failed = true; });
    screen.addEventListener('webglcontextrestored', () => { initializeCurve(); schedule(); });
    fallback.onload = () => { lastLight = ''; schedule(); };
    if (typeof ResizeObserver !== 'undefined') new ResizeObserver(resize).observe(screen);
    else window.addEventListener('resize', resize);
    async function initialize() {
        try {
            initializeCurve(); await document.fonts.load('32px Anton'); await resize();
            effect = new RaindropFX({ canvas: rain, background, backgroundBlurSteps: 0, mist: false,
                spawnInterval: [0.16, 0.4], spawnSize: [30, 54], spawnLimit: 72,
                dropletsPerSeconds: 48, dropletSize: [13, 25], gravity: 65, evaporate: 1,
                trailDistance: [8, 16], trailDropSize: [0.25, 0.4], raindropSpecularLight: [0.1, 0.1, 0.1],
                raindropDiffuseLight: [0.35, 0.35, 0.35], raindropShadowOffset: 0.55,
                raindropSpecularShininess: 64, refractBase: 0.22, refractScale: 0.36 });
            await effect.renderer.loadAssets();
            // Populate glass immediately with the existing simulator, rather than wait for rain to accumulate.
            for (let i = 0; i < 150; i++) {
                const tick = { dt: 1 / 30, total: i / 30 };
                effect.simulator.update(tick); effect.renderer.render(effect.simulator.raindrops, tick);
            }
            ready = !failed;
            screen.dataset.rain = ready ? 'animated' : 'static';
            lastScene = ''; schedule();
        } catch (error) {
            ready = false; failed = true; screen.dataset.rain = 'static';
            screen.dataset.error = String(error); schedule();
        }
    }
    initialize();
})();
