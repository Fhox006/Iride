package com.metrolist.music.ui.component;

import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/** A printed jacket, not a rotating UI card. All lengths are in a 1024-unit print space. */
public final class AlbumSleeveRenderer {
    static final int SIZE = 1024;
    static final float DEPTH = 13f;
    private final Path contour;
    private final float[][] boundary;
    private final Bitmap fibres;
    private final int seed;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Matrix matrix = new Matrix();
    private final Path path = new Path();
    private final float[] source = {0, 0, SIZE, 0, SIZE, SIZE, 0, SIZE};
    private final float[] destination = new float[8];

    public AlbumSleeveRenderer(String albumId, Path contour) {
        this.contour = new Path(contour);
        // Adaptive tessellation: preserve curved corners to subpixel accuracy without
        // splitting each straight edge into hundreds of redundant draw calls.
        float[] flattened = contour.approximate(.15f);
        int count = flattened.length / 3;
        if (count > 1 && flattened[1] == flattened[(count - 1) * 3 + 1]
                && flattened[2] == flattened[(count - 1) * 3 + 2]) count--;
        boundary = new float[count][2];
        for (int i = 0; i < count; i++) {
            boundary[i][0] = flattened[i * 3 + 1];
            boundary[i][1] = flattened[i * 3 + 2];
        }
        seed = albumId.hashCode();
        fibres = paperFibres(seed);
    }

    public static final class PrintedSleeve {
        public final Bitmap bitmap;
        final PrintLayout layout;
        PrintedSleeve(Bitmap bitmap, PrintLayout layout) {
            this.bitmap = bitmap;
            this.layout = layout;
        }
    }

    static final class Track {
        final int number;
        final float x, y, width;
        final StaticLayout text;
        Track(int number, float x, float y, float width, StaticLayout text) {
            this.number = number;
            this.x = x; this.y = y; this.width = width; this.text = text;
        }
    }

    static final class PrintLayout {
        final StaticLayout title;
        final float titleY, fontSize, numberWidth;
        final int columns;
        final List<Track> tracks;
        PrintLayout(StaticLayout title, float titleY, float fontSize, float numberWidth,
                    int columns, List<Track> tracks) {
            this.title = title; this.titleY = titleY; this.fontSize = fontSize;
            this.numberWidth = numberWidth; this.columns = columns; this.tracks = tracks;
        }
    }

    /** Measures every complete title; no ellipsis, character-count estimates or scrolling. */
    static PrintLayout layout(String title, List<String> allTracks, Typeface headingTypeface,
                              Typeface bodyTypeface) {
        List<String> titles = allTracks.subList(0, Math.min(40, allTracks.size()));
        TextPaint heading = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        heading.setTypeface(Typeface.create(headingTypeface, Typeface.BOLD));
        heading.setColor(Color.rgb(230, 229, 222));
        StaticLayout titleLayout;
        float titleSize = 72;
        do {
            heading.setTextSize(titleSize);
            titleLayout = text(title, heading, 800, Layout.Alignment.ALIGN_CENTER);
            if (titleLayout.getHeight() <= 158) break;
            titleSize *= .9f;
        } while (titleSize > 1);

        int columns = titles.size() > 16 ? 2 : 1;
        PrintLayout candidate = fit(titleLayout, titles, columns, bodyTypeface);
        // Very long titles can use two columns even on a shorter album, if that gives larger type.
        if (titles.size() >= 10 && columns == 1 && candidate.fontSize < 15) {
            PrintLayout two = fit(titleLayout, titles, 2, bodyTypeface);
            if (two.fontSize > candidate.fontSize) candidate = two;
        }
        return candidate;
    }

    private static PrintLayout fit(StaticLayout title, List<String> titles, int columns,
                                   Typeface bodyTypeface) {
        TextPaint body = bodyPaint(bodyTypeface);
        boolean few = titles.size() <= 8;
        float titleLeft = 1024;
        for (int i = 0; i < title.getLineCount(); i++) titleLeft = Math.min(titleLeft, 112 + title.getLineLeft(i));
        float font = Math.max(18, 46 - titles.size() * 1.6f);
        float top = few ? 500 : 478;
        float available = 850 - top;
        int split = (titles.size() + columns - 1) / columns;
        float width = columns == 1 ? (few ? 920 - titleLeft : 720) : 382;
        List<StaticLayout> measured = new ArrayList<>();
        float numberWidth;
        float gap;
        float[] heights;
        // Shrink both type and leading until every wrapped track fits its column.
        // No fixed minimum: pathological long strings must still fit without truncation.
        while (true) {
            body.setTextSize(font);
            numberWidth = body.measureText(Math.max(1, titles.size()) + ". ") + font * .12f;
            gap = font * .19f;
            heights = new float[columns];
            measured.clear();
            for (int i = 0; i < titles.size(); i++) {
                StaticLayout line = text(titles.get(i), body, Math.max(1, (int)(width - numberWidth)),
                        Layout.Alignment.ALIGN_NORMAL);
                measured.add(line);
                heights[Math.min(columns - 1, i / Math.max(1, split))] += line.getHeight() + gap;
            }
            float maximum = 0;
            for (float h : heights) maximum = Math.max(maximum, h);
            if (maximum <= available || font <= .1f) break;
            font *= .94f;
        }
        float blockWidth = width;
        if (columns == 1 && !few) {
            // Longer single-column prints keep their existing centered composition.
            float longest = 0;
            for (StaticLayout line : measured) {
                for (int n = 0; n < line.getLineCount(); n++) longest = Math.max(longest, line.getLineWidth(n));
            }
            blockWidth = Math.min(width, Math.max(300, (float)Math.ceil(longest + numberWidth) + 2));
            for (int i = 0; i < titles.size(); i++) {
                measured.set(i, text(titles.get(i), body, Math.max(1, (int)(blockWidth - numberWidth)),
                        Layout.Alignment.ALIGN_NORMAL));
            }
        }
        List<Track> tracks = new ArrayList<>();
        float[] y = new float[columns];
        for (int c = 0; c < columns; c++) y[c] = top;
        for (int i = 0; i < titles.size(); i++) {
            int c = Math.min(columns - 1, i / Math.max(1, split));
            float x = columns == 1 ? (few ? titleLeft : (SIZE - blockWidth) / 2) : 104 + c * 434;
            tracks.add(new Track(i + 1, x, y[c], width, measured.get(i)));
            y[c] += measured.get(i).getHeight() + gap;
        }
        return new PrintLayout(title, 174, font, numberWidth, columns, tracks);
    }

    private static TextPaint bodyPaint(Typeface typeface) {
        TextPaint body = new TextPaint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
        body.setTypeface(Typeface.create(typeface, Typeface.NORMAL));
        body.setColor(Color.rgb(211, 211, 204));
        return body;
    }

    private static StaticLayout text(String value, TextPaint paint, int width, Layout.Alignment alignment) {
        return StaticLayout.Builder.obtain(value, 0, value.length(), new TextPaint(paint), width)
                .setAlignment(alignment).setIncludePad(false).setLineSpacing(0, 1.0f)
                .setBreakStrategy(Layout.BREAK_STRATEGY_HIGH_QUALITY)
                .setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_NORMAL).build();
    }

    public static PrintedSleeve printBack(String albumId, String title, List<String> titles, int accent,
                                          Typeface headingTypeface, Typeface bodyTypeface) {
        Bitmap bitmap = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        canvas.drawColor(Color.rgb(24, 23, 21));
        // Pigment stays close to black, but comes from this album's artwork/background.
        int halo = Color.rgb(22 + Color.red(accent) / 7, 21 + Color.green(accent) / 7,
                19 + Color.blue(accent) / 7);
        p.setShader(new RadialGradient(640, 360, 790, new int[]{halo, Color.rgb(24, 23, 21)},
                null, Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, SIZE, SIZE, p);
        p.setShader(null);
        PrintLayout layout = layout(title, titles, headingTypeface, bodyTypeface);
        canvas.save();
        canvas.translate(112, layout.titleY);
        layout.title.draw(canvas);
        canvas.restore();
        TextPaint body = bodyPaint(bodyTypeface);
        body.setTextSize(layout.fontSize);
        for (Track track : layout.tracks) {
            canvas.drawText(track.number + ".", track.x, track.y + track.text.getLineBaseline(0), body);
            canvas.save();
            canvas.translate(track.x + layout.numberWidth, track.y);
            track.text.draw(canvas);
            canvas.restore();
        }
        // A record's pressure leaves broken, barely visible arcs, not a uniform retro filter.
        Random random = new Random(albumId.hashCode());
        p.setStyle(Paint.Style.STROKE);
        for (int i = 0; i < 110; i++) {
            float radius = 437 + random.nextFloat() * 9;
            p.setStrokeWidth(.4f + random.nextFloat() * 1.2f);
            p.setColor(Color.argb(9 + random.nextInt(14), 185, 179, 156));
            float angle = random.nextBoolean() ? 234 + random.nextFloat() * 66 : 55 + random.nextFloat() * 70;
            canvas.drawArc(512 - radius, 528 - radius, 512 + radius, 528 + radius,
                    angle, 1 + random.nextFloat() * 7, false, p);
        }
        p.setStyle(Paint.Style.FILL);
        return new PrintedSleeve(bitmap, layout);
    }

    /** Perspective projection of a thin box. Both faces and all edges share these vertices. */
    public void draw(Canvas canvas, float width, float height, Bitmap front, Bitmap back, float degrees) {
        float theta = (float)Math.toRadians(degrees);
        float lift = (float)Math.sin(theta);
        float pitch = (float)Math.toRadians(-5 * lift);
        float roll = (float)Math.toRadians(-2.2f * lift);
        float scale = Math.min(width, height) / SIZE * (1 - .045f * lift) * (5600 - DEPTH / 2) / 5600;
        int count = boundary.length;
        float[][] points = new float[count * 2][];
        float centerX = width / 2, centerY = height / 2 - lift * height * .025f;
        for (int side = 0; side < 2; side++) {
            for (int i = 0; i < count; i++) {
                points[side * count + i] = project(boundary[i][0] - 512, boundary[i][1] - 512,
                        side == 0 ? DEPTH / 2 : -DEPTH / 2,
                        theta, pitch, roll, scale, centerX, centerY);
            }
        }
        // Soft contact shadow becomes wider as the jacket lifts from the page.
        silhouette(points);
        paint.setShader(null);
        paint.setColor(Color.argb(18, 0, 0, 0));
        paint.setShadowLayer((15 + 20 * lift) * scale, 7 * lift * scale, (9 + 12 * lift) * scale, 0x80000000);
        canvas.drawPath(path, paint);
        paint.clearShadowLayer();
        paint.setAlpha(255);

        // Extrude the actual continuous contour, including every rounded corner.
        Integer[] edges = new Integer[count];
        for (int i = 0; i < count; i++) edges[i] = i;
        Arrays.sort(edges, Comparator.comparingDouble(i ->
                (points[i][2] + points[(i + 1) % count][2]) / 2));
        for (int i : edges) {
            int next = (i + 1) % count;
            polygon(points, new int[]{i, next, next + count, i + count});
            float[] a = points[i], b = points[i + count];
            paint.setShader(new LinearGradient(a[0], a[1], b[0] + .01f, b[1] + .01f,
                    new int[]{0xFF8D816A, 0xFF3B352B, 0xFF9B8A6C}, null, Shader.TileMode.CLAMP));
            canvas.drawPath(path, paint);
        }
        paint.setShader(null);
        // Lamination follows the same curved outline as the two printed skins.
        if (lift > .04f) {
            for (float fraction : new float[]{.17f, .48f, .82f}) {
                path.reset();
                for (int i = 0; i < count; i++) {
                    float[] p = between(points[i], points[i + count], fraction);
                    if (i == 0) path.moveTo(p[0], p[1]); else path.lineTo(p[0], p[1]);
                }
                path.close();
                paint.setStyle(Paint.Style.STROKE);
                paint.setColor(fraction == .48f ? 0xFF2B2822 : 0xFFAA9878);
                paint.setStrokeWidth(.65f * scale);
                canvas.drawPath(path, paint);
            }
            paint.setStyle(Paint.Style.FILL);
        }
        boolean facingFront = degrees <= 90;
        float[][] corners = {{-512,-512}, {512,-512}, {512,512}, {-512,512}};
        for (int i = 0; i < 4; i++) {
            float[] p = project(facingFront ? corners[i][0] : -corners[i][0], corners[i][1],
                    facingFront ? DEPTH / 2 : -DEPTH / 2,
                    theta, pitch, roll, scale, centerX, centerY);
            destination[i * 2] = p[0];
            destination[i * 2 + 1] = p[1];
        }
        // At exactly edge-on, the cardboard remains visible; do not apply a singular homography.
        if (Math.abs(Math.cos(theta)) < .001) return;
        matrix.reset();
        if (!matrix.setPolyToPoly(source, 0, destination, 0, 4)) return;
        canvas.save();
        canvas.concat(matrix);
        canvas.clipPath(contour);
        Bitmap image = facingFront ? front : back;
        paint.setAlpha(255);
        if (image != null) {
            int side = Math.min(image.getWidth(), image.getHeight());
            int left = (image.getWidth() - side) / 2, top = (image.getHeight() - side) / 2;
            canvas.drawBitmap(image, new Rect(left, top, left + side, top + side),
                    new RectF(0, 0, SIZE, SIZE), paint);
        } else canvas.drawColor(0xFF151616);
        if (facingFront && lift > .02f) {
            paint.setShader(null);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(3.2f);
            paint.setColor(Color.argb((int)(lift * 150), 248, 246, 235));
            canvas.drawPath(contour, paint);
            paint.setStyle(Paint.Style.FILL);
        }
        material(canvas, lift, facingFront);
        canvas.restore();
    }

    private void material(Canvas canvas, float lift, boolean facingFront) {
        if (facingFront) {
            if (lift > .025f) {
                paint.setShader(new LinearGradient(-280 + lift * 580, 0, 680 + lift * 580, 1024,
                        new int[]{0x00FFFFFF, 0x00FFFFFF,
                                Color.argb((int)(5 + lift * 15), 255, 255, 255), 0x00FFFFFF, 0x00FFFFFF},
                        new float[]{0, .38f, .53f, .68f, 1}, Shader.TileMode.CLAMP));
                canvas.drawRect(0, 0, SIZE, SIZE, paint);
                paint.setShader(null);
            }
            return;
        }
        paint.setShader(new BitmapShader(fibres, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT));
        canvas.drawRect(0, 0, SIZE, SIZE, paint);
        paint.setShader(null);
        // Broken rubbed edges expose warm fibres, concentrated around corners and spine.
        Random random = new Random(seed ^ 418);
        for (int i = 0; i < 210; i++) {
            float along = random.nextFloat() * SIZE;
            boolean nearCorner = along < 85 || along > 950;
            float length = nearCorner ? 2 + random.nextFloat() * 10 : random.nextFloat() * 3;
            paint.setColor(Color.argb(nearCorner ? 88 : 34, 182, 168, 133));
            paint.setStrokeWidth(nearCorner ? 2.4f : 1.1f);
            canvas.drawLine(1 + random.nextFloat() * 2, along, 2, along + length, paint);
            if (nearCorner) canvas.drawLine(along, 1022, along + length, 1021, paint);
        }
        // Lamination has a broad moving reflection, with a much weaker second crease.
        float shift = -250 + lift * 460;
        paint.setAlpha(255);
        paint.setShader(new LinearGradient(shift, 0, 910 + shift, 1024,
                new int[]{0x00FFFFFF, 0x00FFFFFF, Color.argb((int)(12 + lift * 23), 236, 241, 235),
                        0x00FFFFFF, 0x00FFFFFF}, new float[]{0, .36f, .54f, .72f, 1}, Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, SIZE, SIZE, paint);
        paint.setShader(null);
        paint.setColor(Color.argb((int)(3 + lift * 5), 255, 255, 245));
        paint.setStrokeWidth(.6f);
        canvas.drawLine(975, 110, 1015, 710, paint);
        // Fine creases and short hairline scuffs in the film, not bright decorative scratches.
        paint.setStrokeWidth(.5f);
        for (int i = 0; i < 14; i++) {
            float x = 850 + random.nextFloat() * 145;
            float y = 110 + random.nextFloat() * 760;
            paint.setColor(Color.argb((int)(4 + lift * 5), 247, 248, 239));
            canvas.drawLine(x, y, x - 5 + random.nextFloat() * 10, y + 10 + random.nextFloat() * 30, paint);
        }
        // Fold stays inside the original rounded corner; the external silhouette is unchanged.
        int corner = 0;
        for (int i = 1; i < boundary.length; i++) {
            if (boundary[i][0] + boundary[i][1] > boundary[corner][0] + boundary[corner][1]) corner = i;
        }
        float[] tip = boundary[corner];
        float[] a = nearCorner(tip, boundary[(corner + boundary.length - 2) % boundary.length]);
        float[] b = nearCorner(tip, boundary[(corner + 2) % boundary.length]);
        path.reset(); path.moveTo(a[0], a[1]); path.lineTo(tip[0] - 6, tip[1] - 6);
        path.lineTo(b[0], b[1]); path.close();
        paint.setAlpha(255);
        paint.setShader(new LinearGradient(tip[0] - 12, tip[1] - 12, tip[0], tip[1],
                0xFF373832, 0xFF93856B, Shader.TileMode.CLAMP));
        canvas.drawPath(path, paint);
        paint.setShader(null); paint.setColor(0x5033332A); paint.setStrokeWidth(.8f);
        canvas.drawLine(a[0], a[1], b[0], b[1], paint);
        paint.setColor(Color.argb((int)(lift * 38), 0, 0, 0));
        canvas.drawRect(0, 0, SIZE, SIZE, paint);
    }

    private static Bitmap paperFibres(int seed) {
        int side = 256;
        int[] pixels = new int[side * side];
        Random random = new Random(seed);
        for (int i = 0; i < pixels.length; i++) {
            boolean light = random.nextBoolean();
            pixels[i] = Color.argb(random.nextInt(light ? 10 : 18), light ? 196 : 0,
                    light ? 189 : 0, light ? 172 : 0);
        }
        return Bitmap.createBitmap(pixels, side, side, Bitmap.Config.ARGB_8888);
    }

    private static float[] project(float x, float y, float z, float theta, float pitch, float roll,
                                   float scale, float centerX, float centerY) {
        float rx = (float)(x * Math.cos(theta) + z * Math.sin(theta));
        float rz = (float)(-x * Math.sin(theta) + z * Math.cos(theta));
        float ry = (float)(y * Math.cos(pitch) - rz * Math.sin(pitch));
        rz = (float)(y * Math.sin(pitch) + rz * Math.cos(pitch));
        float px = (float)(rx * Math.cos(roll) - ry * Math.sin(roll));
        float py = (float)(rx * Math.sin(roll) + ry * Math.cos(roll));
        float perspective = 5600 / (5600 - rz);
        return new float[]{centerX + px * perspective * scale, centerY + py * perspective * scale, rz};
    }

    private void polygon(float[][] points, int[] indices) {
        path.reset();
        path.moveTo(points[indices[0]][0], points[indices[0]][1]);
        for (int i = 1; i < indices.length; i++) path.lineTo(points[indices[i]][0], points[indices[i]][1]);
        path.close();
    }

    private void silhouette(float[][] points) {
        float[][] sorted = points.clone();
        Arrays.sort(sorted, Comparator.comparingDouble(p -> p[0]));
        float[][] hull = new float[points.length * 2][];
        int count = 0;
        for (float[] p : sorted) {
            while (count >= 2 && cross(hull[count - 2], hull[count - 1], p) <= 0) count--;
            hull[count++] = p;
        }
        int lower = count + 1;
        for (int i = sorted.length - 2; i >= 0; i--) {
            float[] p = sorted[i];
            while (count >= lower && cross(hull[count - 2], hull[count - 1], p) <= 0) count--;
            hull[count++] = p;
        }
        path.reset();
        path.moveTo(hull[0][0], hull[0][1]);
        for (int i = 1; i < count - 1; i++) path.lineTo(hull[i][0], hull[i][1]);
        path.close();
    }

    private static float cross(float[] a, float[] b, float[] c) {
        return (b[0] - a[0]) * (c[1] - a[1]) - (b[1] - a[1]) * (c[0] - a[0]);
    }

    private static float[] between(float[] a, float[] b, float t) {
        return new float[]{a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t};
    }

    private static float[] nearCorner(float[] tip, float[] adjacent) {
        float distance = (float)Math.hypot(adjacent[0] - tip[0], adjacent[1] - tip[1]);
        return between(tip, adjacent, Math.min(1, 10 / Math.max(1, distance)));
    }
}
