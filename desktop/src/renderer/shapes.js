


(function () {
  const SAMPLE_COUNT = 360;
  const PROFILE_RADIUS = 160;
  const FULL_TURN = Math.PI * 2;
  const DEGREE = Math.PI / 180;
  const BISECTION_STEPS = 56;

  const SHAPE_DEFINITIONS = [
    {
      name: "Cookie7Sided",
      kind: "cookie",
      petals: 7,
      center: [160, 608.78580952],
      stretch: 1.00227424,
      rim: [154.0236, 15.1787],
      handle: [167.8238, 5.6686],
      firstFlank: [[153.2414, 15.8961], [152.2361, 16.7034], [150.3202, 18.3516]],
      secondFlank: [[149.7071, 18.9139], [149.4061, 19.195], [149.1302, 19.4773]],
      valleyHandle: [145.6613, 23.5231]
    },
    {
      name: "Cookie9Sided",
      kind: "cookie",
      petals: 9,
      center: [1503, 163.34198056],
      stretch: 1.01036691,
      rim: [154.3557, 11.7227],
      handle: [165.6694, 4.4348],
      firstFlank: [[153.9368, 12.0547], [153.4294, 12.4257], [152.4353, 13.1735]],
      secondFlank: [[151.9994, 13.5097], [151.7843, 13.6784], [151.5812, 13.8451]],
      valleyHandle: [147.3456, 17.7843]
    },
    {
      name: "Cookie12Sided",
      kind: "cookie",
      petals: 12,
      center: [1974, 604.001],
      stretch: 1,
      rim: [152.8016, 8.376],
      handle: [163.2344, 3.4256],
      firstFlank: [[152.6152, 8.4738], [152.3933, 8.5891], [151.9524, 8.8207]],
      secondFlank: [[151.6913, 8.9609], [151.5576, 9.0305], [151.4338, 9.0971]],
      valleyHandle: [145.4447, 12.6887]
    },
    {
      name: "Clover4Leaf",
      kind: "clover",
      center: [1066.99925, 604.003],
      outerStart: [38.73, -133.7908],
      outerHandle: [98.8705, -159.911],
      innerStart: [129.471, -28.79],
      innerHandle: [121.4998, -10.42]
    }
  ];

  function pointOnCurve(curve, t) {
    const rest = 1 - t;
    const a = rest * rest * rest;
    const b = 3 * rest * rest * t;
    const c = 3 * rest * t * t;
    const d = t * t * t;
    return [
      a * curve[0][0] + b * curve[1][0] + c * curve[2][0] + d * curve[3][0],
      a * curve[0][1] + b * curve[1][1] + c * curve[2][1] + d * curve[3][1]
    ];
  }

  function straightLine(from, to) {
    const stepX = (to[0] - from[0]) / 3;
    const stepY = (to[1] - from[1]) / 3;
    return [from, [from[0] + stepX, from[1] + stepY], [from[0] + 2 * stepX, from[1] + 2 * stepY], to];
  }

  function placeOnPolar(center, stretch, radius, angle) {
    return [center[0] + radius * Math.cos(angle), center[1] + stretch * radius * Math.sin(angle)];
  }

  function normalizeAngleDifference(difference) {
    let result = difference % FULL_TURN;
    if (result > Math.PI) result -= FULL_TURN;
    if (result <= -Math.PI) result += FULL_TURN;
    return result;
  }

  function buildCookieOutline(definition) {
    const { petals, center, stretch, rim, handle, firstFlank, secondFlank, valleyHandle } = definition;
    const lobeStep = FULL_TURN / petals;
    const outline = [];

    for (let lobe = 0; lobe < petals; lobe++) {
      const baseAngle = -Math.PI / 2 + lobe * lobeStep;
      const place = (point) => placeOnPolar(center, stretch, point[0], baseAngle + point[1] * DEGREE);
      const placeMirrored = (point) => placeOnPolar(center, stretch, point[0], baseAngle + lobeStep - point[1] * DEGREE);
      const placeLeft = (point) => place([point[0], -point[1]]);

      const firstFlankEnd = firstFlank[2];
      const secondFlankEnd = secondFlank[2];

      outline.push([placeLeft(rim), placeLeft(handle), place(handle), place(rim)]);
      outline.push([place(rim), place(firstFlank[0]), place(firstFlank[1]), place(firstFlankEnd)]);
      outline.push([place(firstFlankEnd), place(secondFlank[0]), place(secondFlank[1]), place(secondFlankEnd)]);
      outline.push([place(secondFlankEnd), place(valleyHandle), placeMirrored(valleyHandle), placeMirrored(secondFlankEnd)]);
      outline.push([placeMirrored(secondFlankEnd), placeMirrored(secondFlank[1]), placeMirrored(secondFlank[0]), placeMirrored(firstFlankEnd)]);
      outline.push([placeMirrored(firstFlankEnd), placeMirrored(firstFlank[1]), placeMirrored(firstFlank[0]), placeMirrored(rim)]);
    }

    return outline;
  }

  function buildCloverOutline(definition) {
    const { center, outerStart, outerHandle, innerStart, innerHandle } = definition;
    const outline = [];

    const turnQuarters = (point, quarters) => {
      let x = point[0];
      let y = point[1];
      for (let step = 0; step < quarters; step++) {
        const turnedX = -y;
        y = x;
        x = turnedX;
      }
      return [center[0] + x, center[1] + y];
    };

    for (let quarter = 0; quarter < 4; quarter++) {
      const place = (point) => turnQuarters(point, quarter);
      const outerArc = [
        place(outerStart),
        place(outerHandle),
        place([-outerHandle[1], -outerHandle[0]]),
        place([-outerStart[1], -outerStart[0]])
      ];
      const innerArc = [
        place(innerStart),
        place(innerHandle),
        place([innerHandle[0], -innerHandle[1]]),
        place([innerStart[0], -innerStart[1]])
      ];
      const nextOuterStart = turnQuarters(outerStart, quarter + 1);

      outline.push(outerArc);
      outline.push(straightLine(outerArc[3], innerArc[0]));
      outline.push(innerArc);
      outline.push(straightLine(innerArc[3], nextOuterStart));
    }

    return outline;
  }

  function buildOutline(definition) {
    return definition.kind === "clover" ? buildCloverOutline(definition) : buildCookieOutline(definition);
  }

  function findCurveBounds(curve) {
    const bounds = [Infinity, Infinity, -Infinity, -Infinity];
    const include = (point) => {
      bounds[0] = Math.min(bounds[0], point[0]);
      bounds[1] = Math.min(bounds[1], point[1]);
      bounds[2] = Math.max(bounds[2], point[0]);
      bounds[3] = Math.max(bounds[3], point[1]);
    };

    include(curve[0]);
    include(curve[3]);

    for (let axis = 0; axis < 2; axis++) {
      const first = curve[1][axis] - curve[0][axis];
      const second = curve[2][axis] - curve[1][axis];
      const third = curve[3][axis] - curve[2][axis];
      const a = first - 2 * second + third;
      const b = 2 * (second - first);
      const c = first;
      const roots = [];

      if (Math.abs(a) < 1e-12) {
        if (Math.abs(b) > 1e-12) roots.push(-c / b);
      } else {
        const discriminant = b * b - 4 * a * c;
        if (discriminant >= 0) {
          const root = Math.sqrt(discriminant);
          roots.push((-b + root) / (2 * a), (-b - root) / (2 * a));
        }
      }

      roots.forEach((t) => {
        if (t > 0 && t < 1) include(pointOnCurve(curve, t));
      });
    }

    return bounds;
  }

  function findBoundsCenter(outline) {
    const total = [Infinity, Infinity, -Infinity, -Infinity];
    outline.forEach((curve) => {
      const bounds = findCurveBounds(curve);
      total[0] = Math.min(total[0], bounds[0]);
      total[1] = Math.min(total[1], bounds[1]);
      total[2] = Math.max(total[2], bounds[2]);
      total[3] = Math.max(total[3], bounds[3]);
    });
    return [(total[0] + total[2]) / 2, (total[1] + total[3]) / 2];
  }

  function angleFromCenter(point, center) {
    return Math.atan2(point[1] - center[1], point[0] - center[0]);
  }

  function createSections(outline, center) {
    const sections = [];
    let angle = angleFromCenter(outline[0][0], center);

    outline.forEach((curve) => {
      const startAngle = angle;
      const endAngle = startAngle + normalizeAngleDifference(angleFromCenter(curve[3], center) - startAngle);
      sections.push({ curve, startAngle, endAngle });
      angle = endAngle;
    });

    return sections;
  }

  function reverseOutline(outline) {
    return outline.slice().reverse().map((curve) => curve.slice().reverse());
  }

  function createAscendingSections(outline, center) {
    let sections = createSections(outline, center);
    const sweep = sections[sections.length - 1].endAngle - sections[0].startAngle;
    if (sweep < 0) sections = createSections(reverseOutline(outline), center);
    return sections;
  }

  function measureRadius(sections, center, angle) {
    const firstAngle = sections[0].startAngle;
    const target = firstAngle + ((((angle - firstAngle) % FULL_TURN) + FULL_TURN) % FULL_TURN);
    let section = sections[sections.length - 1];

    for (let index = 0; index < sections.length; index++) {
      if (target >= sections[index].startAngle && target <= sections[index].endAngle) {
        section = sections[index];
        break;
      }
    }

    let low = 0;
    let high = 1;
    for (let step = 0; step < BISECTION_STEPS; step++) {
      const middle = (low + high) / 2;
      const point = pointOnCurve(section.curve, middle);
      const pointAngle = section.startAngle + normalizeAngleDifference(angleFromCenter(point, center) - section.startAngle);
      if (pointAngle < target) low = middle; else high = middle;
    }

    const point = pointOnCurve(section.curve, (low + high) / 2);
    return Math.hypot(point[0] - center[0], point[1] - center[1]);
  }

  function createProfile(definition) {
    const outline = buildOutline(definition);
    const center = findBoundsCenter(outline);
    const sections = createAscendingSections(outline, center);
    const radii = new Float32Array(SAMPLE_COUNT);
    let largestRadius = 0;

    for (let index = 0; index < SAMPLE_COUNT; index++) {
      const angle = -Math.PI + (FULL_TURN * index) / SAMPLE_COUNT;
      radii[index] = measureRadius(sections, center, angle);
      if (radii[index] > largestRadius) largestRadius = radii[index];
    }

    const scale = PROFILE_RADIUS / largestRadius;
    for (let index = 0; index < SAMPLE_COUNT; index++) radii[index] *= scale;
    return radii;
  }

  function easeInOutCubic(progress) {
    return progress < 0.5 ? 4 * progress * progress * progress : 1 - Math.pow(-2 * progress + 2, 3) / 2;
  }

  const shapeProfiles = SHAPE_DEFINITIONS.map(createProfile);
  const shapeNames = SHAPE_DEFINITIONS.map((definition) => definition.name);
  const shapeHistory = [0];

  function chooseNextShape(current) {
    const lastShown = shapeHistory[shapeHistory.length - 1];
    if (current !== undefined && (shapeHistory.length === 0 || lastShown !== current)) {
      shapeHistory.push(current);
    }

    const recentlyShown = shapeHistory.slice(-2);
    const candidates = shapeProfiles.map((_, index) => index).filter((index) => !recentlyShown.includes(index));
    const chosen = candidates.length ? candidates[Math.floor(Math.random() * candidates.length)] : 0;

    shapeHistory.push(chosen);
    if (shapeHistory.length > 10) shapeHistory.shift();
    return chosen;
  }

  window.RiffShapes = {
    K: SAMPLE_COUNT,
    HOLD_MS: 2000,
    MORPH_MS: 1250,
    ease: easeInOutCubic,
    ready: () => shapeProfiles.length > 0,
    profile: (index) => shapeProfiles[index],
    count: () => shapeProfiles.length,
    next: chooseNextShape,
    names: shapeNames,
    name: (index) => shapeNames[index]
  };
})();

(function initRiffSpinnerShapes() {
  const PI = Math.PI;
  function roundedPolygon(pts, radii) {
    const n = pts.length;
    const p = new Path2D();
    const mid = (a, b) => [(a[0] + b[0]) / 2, (a[1] + b[1]) / 2];
    const s = mid(pts[0], pts[1]);
    p.moveTo(s[0], s[1]);
    for (let i = 1; i <= n; i++) {
      const v = pts[i % n];
      const m = mid(v, pts[(i + 1) % n]);
      p.arcTo(v[0], v[1], m[0], m[1], radii[i % n]);
    }
    p.closePath();
    return p;
  }
  function ghost() {
    const p = new Path2D();
    p.moveTo(0, 144.762);
    p.ellipse(152, 144.762, 152, 144.762, 0, PI, 2 * PI);
    p.lineTo(304, 246.095);
    p.ellipse(243.2, 246.095, 60.8, 57.905, 0, 0, PI / 2);
    p.bezierCurveTo(208, 297, 180, 275.048, 156.327, 275.048);
    p.lineTo(147.673, 275.048);
    p.bezierCurveTo(124, 275.048, 96, 297, 60.8, 304);
    p.ellipse(60.8, 246.095, 60.8, 57.905, 0, PI / 2, PI);
    p.closePath();
    return p;
  }
  function dumbbell() {
    const r = 81.36, W = 310, H = 320;
    const dy = H / 2 - r;
    const dx = Math.sqrt(r * r - dy * dy);
    const a = Math.atan2(dy, dx);
    const p = new Path2D();
    p.moveTo(r, 0);
    p.lineTo(W - r, 0);
    p.arc(W - r, r, r, -PI / 2, a);
    p.arc(W - r, H - r, r, -a, PI / 2);
    p.lineTo(r, H);
    p.arc(r, H - r, r, PI / 2, PI + a);
    p.arc(r, r, r, PI - a, 1.5 * PI);
    p.closePath();
    return p;
  }
  function capsule() {
    const S = 300, r = 128.95;
    const p = new Path2D();
    p.arc(S - r, r, r, -0.75 * PI, 0.25 * PI);
    p.arc(r, S - r, r, 0.25 * PI, 1.25 * PI);
    p.closePath();
    return p;
  }
  function arch() {
    const S = 304, r = 152, c = 50.28;
    const p = new Path2D();
    p.moveTo(S, r);
    p.lineTo(S, S - c);
    p.arcTo(S, S, S - c, S, c);
    p.lineTo(c, S);
    p.arcTo(0, S, 0, S - c, c);
    p.lineTo(0, r);
    p.arc(r, r, r, PI, 2 * PI);
    p.closePath();
    return p;
  }
  function pentagon() {
    const cx = 160;
    return roundedPolygon([[cx, -14.7], [cx + 169, 116], [cx + 105, 319.8], [cx - 105, 319.8], [cx - 169, 116]], [87, 87, 87, 87, 87]);
  }
  function hexagon() {
    const cx = 160;
    return roundedPolygon([[cx + 104.66, 0], [cx + 167.7, 127], [cx + 104.66, 254], [cx - 104.66, 254], [cx - 167.7, 127], [cx - 104.66, 0]], [68.7, 96.4, 68.7, 68.7, 96.4, 68.7]);
  }
  function triangle() {
    const cx = 157.6;
    return roundedPolygon([[cx, -52.4], [cx + 215, 279], [cx - 215, 279]], [62.7, 69.5, 69.5]);
  }
  const shapesList = [
    { name: 'ghost', path: ghost(), w: 304, h: 304 },
    { name: 'dumbbell', path: dumbbell(), w: 310, h: 320 },
    { name: 'capsule', path: capsule(), w: 300, h: 300 },
    { name: 'arch', path: arch(), w: 304, h: 304 },
    { name: 'pentagon', path: pentagon(), w: 320, h: 320 },
    { name: 'hexagon', path: hexagon(), w: 320, h: 254 },
    { name: 'triangle', path: triangle(), w: 315.3, h: 279 }
  ];
  function drawShapeCentered(ctx, shape, cx, cy, width, rotation, fill) {
    const s = width / shape.w;
    ctx.save();
    ctx.translate(cx, cy);
    ctx.rotate(rotation);
    ctx.scale(s, s);
    ctx.translate(-shape.w / 2, -shape.h / 2);
    ctx.fillStyle = fill;
    ctx.fill(shape.path);
    ctx.restore();
  }
  window.RiffSpinnerShapes = { list: shapesList, drawShapeCentered };
})();

(function initAll11MorphingShapes() {
  const S = window.RiffShapes;
  if (!S) return;
  const SAMPLE_COUNT = S.K || 360;
  const PROFILE_RADIUS = 160;
  const FULL_TURN = Math.PI * 2;

  function samplePathToRadialProfile(path2d, w, h) {
    const canvas = document.createElement('canvas');
    canvas.width = 320;
    canvas.height = 320;
    const ctx = canvas.getContext('2d');
    const cx = 160, cy = 160;
    const scale = 270 / Math.max(w, h);
    ctx.translate(cx, cy);
    ctx.scale(scale, scale);
    ctx.translate(-w / 2, -h / 2);

    const radii = new Float32Array(SAMPLE_COUNT);
    let largest = 0;
    for (let i = 0; i < SAMPLE_COUNT; i++) {
      const angle = -Math.PI + (FULL_TURN * i) / SAMPLE_COUNT;
      const cos = Math.cos(angle);
      const sin = Math.sin(angle);
      let low = 0, high = 160;
      for (let step = 0; step < 9; step++) {
        const mid = (low + high) / 2;
        if (ctx.isPointInPath(path2d, cx + cos * mid, cy + sin * mid)) low = mid;
        else high = mid;
      }
      const r = (low + high) / 2;
      radii[i] = r;
      if (r > largest) largest = r;
    }
    const norm = PROFILE_RADIUS / (largest || 1);
    for (let i = 0; i < SAMPLE_COUNT; i++) radii[i] *= norm;
    return radii;
  }

  const list = (window.RiffSpinnerShapes && window.RiffSpinnerShapes.list) || [];
  const extraProfiles = list.map(item => samplePathToRadialProfile(item.path, item.w, item.h));

  const baseCount = S.count();
  const allProfiles = [];
  for (let i = 0; i < baseCount; i++) allProfiles.push(S.profile(i));
  allProfiles.push(...extraProfiles);

  let spinnerHistory = [0];
  function nextSpinnerShape(cur) {
    const recently = spinnerHistory.slice(-3);
    const candidates = allProfiles.map((_, i) => i).filter(i => !recently.includes(i));
    const chosen = candidates.length ? candidates[Math.floor(Math.random() * candidates.length)] : 0;
    spinnerHistory.push(chosen);
    if (spinnerHistory.length > 10) spinnerHistory.shift();
    return chosen;
  }

  S.spinnerProfile = (idx) => allProfiles[idx % allProfiles.length];
  S.spinnerCount = () => allProfiles.length;
  S.nextSpinner = nextSpinnerShape;
})();