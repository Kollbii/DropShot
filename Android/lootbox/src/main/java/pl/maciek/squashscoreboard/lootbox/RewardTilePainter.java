package pl.maciek.squashscoreboard.lootbox;

import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.view.View;

/** Shared tier tile art for the roulette and player collection. */
public final class RewardTilePainter {
    private RewardTilePainter() {}

    public static void draw(Canvas canvas, float left, float top, float size,
                            RewardCatalog.Reward reward, Paint paint, View host) {
        int save = canvas.save();
        canvas.translate(left, top);
        canvas.scale(size / 72f, size / 72f);

        paint.setShader(null);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(reward.tier.color);
        canvas.drawRoundRect(0, 0, 72, 72, 9, 9, paint);
        paint.setColor(0xAA151923);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
        paint.setTextSize(10);
        canvas.drawText(reward.tier.label.toUpperCase(), 36, 12, paint);

        if (reward.kind == RewardCatalog.Kind.FONT) {
            paint.setColor(0xFF18202B);
            paint.setTypeface(RewardFonts.forReward(host.getContext(), reward));
            paint.setTextSize(38 * RewardFonts.sizeScale(host.getContext(), reward));
            if (paint.measureText("Aa") > 56f)
                paint.setTextSize(paint.getTextSize() * 56f / paint.measureText("Aa"));
            canvas.drawText("Aa", 36, 55, paint);
            paint.setTypeface(null);
            paint.setTextAlign(Paint.Align.LEFT);
            canvas.restoreToCount(save);
            return;
        }

        Path flow = new Path();
        flow.moveTo(40, 35);
        flow.cubicTo(47, 37, 43, 45, 51, 48);
        flow.cubicTo(54, 50, 58, 49, 60, 54);
        flow.cubicTo(63, 62, 53, 64, 43, 61);
        flow.cubicTo(34, 59, 32, 55, 39, 50);
        flow.cubicTo(44, 46, 38, 42, 40, 35);
        flow.close();
        if (reward.colors.length > 1) {
            int[] colors = reward.colors;
            if (reward.animated) {
                colors = colors.clone();
                int shift = (int) ((System.currentTimeMillis() / 450) % 6);
                for (int j = 0; j < colors.length; j++)
                    colors[j] = reward.colors[(j + shift) % 6];
                host.postInvalidateDelayed(80);
            }
            paint.setShader(new LinearGradient(35, 35, 61, 61,
                colors, null, Shader.TileMode.CLAMP));
        } else {
            paint.setColor(reward.key());
        }
        canvas.drawPath(flow, paint);
        paint.setShader(null);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1.7f);
        paint.setColor(0xCC17202B);
        canvas.drawPath(flow, paint);

        int canSave = canvas.save();
        canvas.rotate(-25, 28, 31);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xFF202A34);
        canvas.drawRoundRect(14, 21, 42, 44, 4, 4, paint);
        paint.setColor(0xFFF7F3EA);
        canvas.drawRoundRect(16, 22, 40, 42, 3, 3, paint);
        paint.setColor(reward.colors.length > 1 ? reward.colors[1] : reward.key());
        canvas.drawRect(17, 34, 39, 41, paint);
        paint.setColor(0xFF202A34);
        canvas.drawOval(13, 17, 43, 25, paint);
        paint.setColor(0xFFF7F3EA);
        canvas.drawOval(16, 18, 40, 23, paint);
        canvas.restoreToCount(canSave);

        paint.setTypeface(null);
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setStyle(Paint.Style.FILL);
        canvas.restoreToCount(save);
    }
}
