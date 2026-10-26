package pl.maciek.squashscoreboard;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;
import android.os.SystemClock;

/** Repeating, staggered bursts while the score qualifies. The activity removes the overlay. */
public final class FireworksView extends View {
    private static final long BURST_MS = 1300;
    private static final long SPACING_MS = 320;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final long startedAt = SystemClock.uptimeMillis();
    private final int[] colors = {Color.rgb(49,132,255), Color.rgb(244,92,92), Color.rgb(255,213,79), Color.WHITE};
    private final float[][] centers = {{.23f,.30f},{.72f,.26f},{.48f,.57f},{.82f,.68f},{.16f,.72f}};
    public FireworksView(Context context) {
        super(context);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        long elapsed = SystemClock.uptimeMillis() - startedAt;
        long cycle = BURST_MS + SPACING_MS * centers.length;
        for (int burst = 0; burst < centers.length; burst++) {
            long age = (elapsed - burst * SPACING_MS + cycle) % cycle;
            if (age >= BURST_MS) continue;
            float progress = age / (float) BURST_MS;
            float fade = 1f - progress;
            float cx = getWidth() * centers[burst][0], cy = getHeight() * centers[burst][1];
            for (int spark = 0; spark < 18; spark++) {
                double angle = Math.PI * 2 * spark / 18.0 + burst * .45;
                float distance = getWidth() * (.055f + progress * .11f);
                float x = cx + (float) Math.cos(angle) * distance;
                float y = cy + (float) Math.sin(angle) * distance + progress * progress * dp(26);
                paint.setColor(colors[(spark + burst) % colors.length]); paint.setAlpha((int) (255 * fade)); paint.setStrokeWidth(dp(2));
                canvas.drawLine(cx, cy, x, y, paint); canvas.drawCircle(x, y, dp(3) * fade, paint);
            }
        }
        postInvalidateOnAnimation();
    }
    private int dp(int value) { return (int) (value * getResources().getDisplayMetrics().density + .5f); }
}
