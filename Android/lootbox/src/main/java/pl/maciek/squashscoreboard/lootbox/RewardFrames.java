package pl.maciek.squashscoreboard.lootbox;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

/** Draws frame rewards with fixed-size corners and repeated edge marks. */
public final class RewardFrames extends Drawable {
    private static final String[] IDS = {
        "frame-photon-blade", "frame-tribal-fang", "frame-barbed-wire",
        "frame-forged-steel", "frame-neon-circuit", "frame-arcane-runes",
        "frame-thorn-vine", "frame-pixel-glitch", "frame-racing-stripes",
        "frame-royal-filigree", "frame-liquid-chrome", "frame-inferno",
        "frame-frost-crystal", "frame-carbon-hex", "frame-shattered-glass"
    };
    private static final float[] TRIBAL = {18,85,18,45,31,28,53,18,88,18,72,32,53,32,66,47,45,40,37,63,26,50};
    private static final float[] RACE_ORANGE = {15,73,70,15,86,15,29,86};
    private static final float[] RACE_WHITE = {26,88,90,15,104,15,37,101};
    private static final float[] FIRE_OUTER = {19,93,23,56,35,64,40,35,54,48,63,12,73,41,61,67,42,72,36,96};
    private static final float[] FIRE_INNER = {27,83,35,57,44,63,55,36,56,70,42,81};
    private static final float[] ICE = {17,91,20,45,30,55,40,14,51,39,83,17,68,49,94,39,66,67,43,80};
    private static final float[] GLASS_TOP = {13,13,80,18,45,44,16,62};
    private static final float[] GLASS_SIDE = {16,62,45,44,30,103};
    private static final RewardFrames[] PREVIEWS = new RewardFrames[IDS.length];
    static { for (int i = 0; i < PREVIEWS.length; i++) PREVIEWS[i] = new RewardFrames(i); }
    private final int style;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private int alpha = 255;

    private RewardFrames(int style) { this.style = style; }

    public static Drawable forId(String id) {
        if (id == null) return null;
        for (int i = 0; i < IDS.length; i++) if (IDS[i].equals(id)) return new RewardFrames(i);
        return null;
    }

    public static void drawPreview(Canvas canvas, float left, float top, float width,
                                   float height, RewardCatalog.Reward reward) {
        if (reward == null || reward.kind != RewardCatalog.Kind.FRAME) return;
        RewardFrames frame = null;
        for (int i = 0; i < IDS.length; i++) if (IDS[i].equals(reward.id)) { frame = PREVIEWS[i]; break; }
        if (frame == null) return;
        int save = canvas.save();
        canvas.translate(left, top);
        frame.setBounds(0, 0, Math.round(width), Math.round(height));
        frame.draw(canvas);
        canvas.restoreToCount(save);
    }

    @Override public void draw(Canvas canvas) {
        Rect bounds = getBounds();
        if (bounds.isEmpty()) return;
        float scale = Math.min(bounds.width() / 640f, bounds.height() / 360f);
        int save = canvas.save();
        canvas.translate(bounds.left, bounds.top);
        canvas.scale(scale, scale);
        float w = bounds.width() / scale, h = bounds.height() / scale;
        paint.setAlpha(alpha);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        drawBase(canvas, w, h);
        for (int corner = 0; corner < 4; corner++) {
            int mark = canvas.save();
            if ((corner & 1) != 0) { canvas.translate(w, 0); canvas.scale(-1, 1); }
            if ((corner & 2) != 0) { canvas.translate(0, h); canvas.scale(1, -1); }
            drawCorner(canvas);
            canvas.restoreToCount(mark);
        }
        drawEdges(canvas, w, h);
        canvas.restoreToCount(save);
    }

    private void drawBase(Canvas canvas, float w, float h) {
        int dark, light, inner;
        switch (style) {
            case 0: dark = 0xFF07506B; light = 0xFF46E5FF; inner = 0xFFF3FFFF; break;
            case 1: dark = 0xFF211627; light = 0xFFE1CBF1; inner = 0xFFED65B6; break;
            case 2: dark = 0xFF121820; light = 0xFFDDE5E8; inner = 0xFF788994; break;
            case 3: dark = 0xFF26323B; light = 0xFFD8DFE0; inner = 0xFF778A94; break;
            case 4: dark = 0xFF102D35; light = 0xFF3CF5D0; inner = 0xFFCCFFF1; break;
            case 5: dark = 0xFF261B3F; light = 0xFFD5C4FF; inner = 0xFF7C65CF; break;
            case 6: dark = 0xFF17402A; light = 0xFFA7DB87; inner = 0xFFDBF3B7; break;
            case 7: dark = 0xFF121D2D; light = 0xFF86E8FF; inner = 0xFFF184CF; break;
            case 8: dark = 0xFF17232A; light = 0xFFF2F7ED; inner = 0xFFFFB44E; break;
            case 9: dark = 0xFF4D3017; light = 0xFFFFD98D; inner = 0xFFB98647; break;
            case 10: dark = 0xFF1B2A36; light = 0xFFEAFBFF; inner = 0xFF7DB7D2; break;
            case 11: dark = 0xFF682A17; light = 0xFFFF8F35; inner = 0xFFFFD37C; break;
            case 12: dark = 0xFF1C4C65; light = 0xFFD7FAFF; inner = 0xFF79D5EB; break;
            case 13: dark = 0xFF131C23; light = 0xFF8A9AA2; inner = 0xFFF5B451; break;
            default: dark = 0xFF33304C; light = 0xFFE8E1FF; inner = 0xFFAAA5D0;
        }
        round(canvas, 26, 26, w - 26, h - 26, 29, dark, 20);
        round(canvas, 26, 26, w - 26, h - 26, 29, light, style == 0 ? 9 : 7);
        round(canvas, 36, 36, w - 36, h - 36, 21, inner, style == 0 ? 4 : 3);
        if (style == 0) round(canvas, 26, 26, w - 26, h - 26, 29, 0xFFFFFFFF, 3);
        if (style == 5 || style == 9 || style == 12)
            round(canvas, 43, 43, w - 43, h - 43, 15, inner, 3);
    }

    private void drawCorner(Canvas canvas) {
        switch (style) {
            case 0:
                line(canvas, 20, 68, 20, 45, 0xFF182D34, 14);
                line(canvas, 20, 45, 46, 20, 0xFF182D34, 14);
                line(canvas, 46, 20, 72, 20, 0xFF182D34, 14);
                line(canvas, 23, 67, 23, 47, 0xFFE4FAFF, 5);
                line(canvas, 55, 15, 55, 40, 0xFF263B43, 8);
                line(canvas, 65, 15, 65, 40, 0xFFE4FAFF, 4);
                break;
            case 1:
                polygon(canvas, TRIBAL, 0xFF25182B);
                line(canvas, 30, 74, 52, 48, 0xFFED65B6, 6);
                line(canvas, 52, 48, 77, 27, 0xFFED65B6, 6);
                break;
            case 2:
                line(canvas, 15, 83, 42, 101, 0xFFE4ECEF, 6);
                line(canvas, 15, 101, 42, 83, 0xFFE4ECEF, 6);
                line(canvas, 83, 15, 101, 42, 0xFFE4ECEF, 6);
                line(canvas, 101, 15, 83, 42, 0xFFE4ECEF, 6);
                break;
            case 3:
                circle(canvas, 45, 45, 10, 0xFF313C45);
                circle(canvas, 45, 45, 6, 0xFFE0E6E2);
                line(canvas, 41, 49, 49, 41, 0xFF35444E, 3);
                break;
            case 4:
                line(canvas, 27, 96, 27, 58, 0xFF24BDA8, 6);
                line(canvas, 27, 58, 58, 27, 0xFF24BDA8, 6);
                line(canvas, 58, 27, 105, 27, 0xFF24BDA8, 6);
                circle(canvas, 105, 27, 8, 0xFFD0FFF1);
                circle(canvas, 27, 96, 7, 0xFFD0FFF1);
                break;
            case 5:
                diamond(canvas, 51, 28, 16, 0xFFE3D8FF, 4);
                circle(canvas, 51, 28, 5, 0xFFE3D8FF);
                break;
            case 6:
                leaf(canvas, 46, 37, 72, 13);
                leaf(canvas, 30, 64, 11, 47);
                line(canvas, 25, 86, 45, 42, 0xFFDAF3B8, 5);
                line(canvas, 45, 42, 88, 25, 0xFFDAF3B8, 5);
                break;
            case 7:
                line(canvas, 16, 62, 30, 62, 0xFFF184CF, 8);
                line(canvas, 30, 62, 30, 43, 0xFFF184CF, 8);
                line(canvas, 30, 43, 45, 43, 0xFFF184CF, 8);
                square(canvas, 16, 82, 9, 0xFFBAEEFF);
                square(canvas, 29, 94, 9, 0xFFBAEEFF);
                break;
            case 8:
                polygon(canvas, RACE_ORANGE, 0xFFFFB44E);
                polygon(canvas, RACE_WHITE, 0xFFF7F8EF);
                break;
            case 9:
                line(canvas, 25, 83, 25, 58, 0xFFFFD98D, 7);
                line(canvas, 25, 58, 58, 25, 0xFFFFD98D, 7);
                line(canvas, 58, 25, 83, 25, 0xFFFFD98D, 7);
                circle(canvas, 52, 52, 7, 0xFFFFEEC3);
                break;
            case 10:
                line(canvas, 23, 72, 23, 51, 0xFFFFFFFF, 6);
                line(canvas, 23, 51, 51, 23, 0xFFFFFFFF, 6);
                line(canvas, 51, 23, 79, 23, 0xFFFFFFFF, 6);
                circle(canvas, 46, 48, 5, 0xFFDAF6FF);
                break;
            case 11:
                polygon(canvas, FIRE_OUTER, 0xFFFF9C43);
                polygon(canvas, FIRE_INNER, 0xFFFFDB79);
                break;
            case 12:
                polygon(canvas, ICE, 0xFFC8F9FF);
                line(canvas, 40, 14, 43, 80, 0xFF70C9E2, 3);
                break;
            case 13:
                circle(canvas, 27, 27, 10, 0xFFF5B451);
                break;
            default:
                polygon(canvas, GLASS_TOP, 0xFFB7B2DD);
                polygon(canvas, GLASS_SIDE, 0xFFEAE6FF);
                line(canvas, 45, 44, 94, 65, 0xFFEAE6FF, 4);
        }
    }

    private void drawEdges(Canvas canvas, float w, float h) {
        if (style == 2 || style == 13) {
            for (float x = 120; x < w - 90; x += style == 2 ? 95 : 65) {
                if (style == 2) {
                    cross(canvas, x, 26, 0xFFDDE5E8);
                    cross(canvas, x, h - 26, 0xFFDDE5E8);
                } else {
                    diamond(canvas, x, 26, 11, 0xFF9BA9AC, 3);
                    diamond(canvas, x, h - 26, 11, 0xFF9BA9AC, 3);
                }
            }
            for (float y = 115; y < h - 90; y += 95) {
                if (style == 2) { cross(canvas, 26, y, 0xFFDDE5E8); cross(canvas, w - 26, y, 0xFFDDE5E8); }
                else { diamond(canvas, 26, y, 10, 0xFF9BA9AC, 3); diamond(canvas, w - 26, y, 10, 0xFF9BA9AC, 3); }
            }
        } else if (style == 5 || style == 9) {
            for (float x = 180; x < w - 120; x += 150) {
                diamond(canvas, x, 26, style == 9 ? 12 : 14,
                    style == 9 ? 0xFFFFEFC0 : 0xFFE2D6FF, 4);
                diamond(canvas, x, h - 26, style == 9 ? 12 : 14,
                    style == 9 ? 0xFFFFEFC0 : 0xFFE2D6FF, 4);
            }
        } else if (style == 4) {
            for (float x = 180; x < w - 120; x += 130) {
                circle(canvas, x, 26, 5, 0xFFD0FFF1);
                circle(canvas, x, h - 26, 5, 0xFFD0FFF1);
            }
        } else if (style == 8) {
            for (float x = 175; x < w - 115; x += 33)
                line(canvas, x, 15, x - 13, 38, 0xFFFFB44E, 7);
        } else if (style == 3) {
            circle(canvas, w / 2, 26, 8, 0xFFE0E6E2);
            circle(canvas, w / 2, h - 26, 8, 0xFFE0E6E2);
        }
    }

    private void round(Canvas c, float l, float t, float r, float b, float radius, int color, float width) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(width);
        setColor(color);
        c.drawRoundRect(l, t, r, b, radius, radius, paint);
    }

    private void line(Canvas c, float x1, float y1, float x2, float y2, int color, float width) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(width);
        setColor(color);
        c.drawLine(x1, y1, x2, y2, paint);
    }

    private void circle(Canvas c, float x, float y, float radius, int color) {
        paint.setStyle(Paint.Style.FILL);
        setColor(color);
        c.drawCircle(x, y, radius, paint);
    }

    private void square(Canvas c, float x, float y, float size, int color) {
        paint.setStyle(Paint.Style.FILL);
        setColor(color);
        c.drawRect(x, y, x + size, y + size, paint);
    }

    private void polygon(Canvas c, float[] points, int color) {
        path.reset();
        path.moveTo(points[0], points[1]);
        for (int i = 2; i < points.length; i += 2) path.lineTo(points[i], points[i + 1]);
        path.close();
        paint.setStyle(Paint.Style.FILL);
        setColor(color);
        c.drawPath(path, paint);
    }

    private void diamond(Canvas c, float x, float y, float radius, int color, float width) {
        path.reset();
        path.moveTo(x, y - radius);
        path.lineTo(x + radius, y);
        path.lineTo(x, y + radius);
        path.lineTo(x - radius, y);
        path.close();
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(width);
        setColor(color);
        c.drawPath(path, paint);
    }

    private void cross(Canvas c, float x, float y, int color) {
        line(c, x - 10, y - 10, x + 10, y + 10, color, 6);
        line(c, x + 10, y - 10, x - 10, y + 10, color, 6);
    }

    private void leaf(Canvas c, float x1, float y1, float x2, float y2) {
        path.reset();
        path.moveTo(x1, y1);
        path.quadTo(x1 - 4, y2, x2, y2);
        path.quadTo(x2 + 4, y1, x1, y1);
        paint.setStyle(Paint.Style.FILL);
        setColor(0xFFA5D678);
        c.drawPath(path, paint);
    }

    private void setColor(int color) {
        paint.setColor(Color.argb(Color.alpha(color) * alpha / 255,
            Color.red(color), Color.green(color), Color.blue(color)));
    }

    @Override public void setAlpha(int alpha) { this.alpha = alpha; invalidateSelf(); }
    @Override public void setColorFilter(ColorFilter filter) { paint.setColorFilter(filter); invalidateSelf(); }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
