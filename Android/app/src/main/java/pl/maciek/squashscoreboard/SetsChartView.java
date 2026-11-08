package pl.maciek.squashscoreboard;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;
import java.util.List;

public final class SetsChartView extends View {
    private final MatchRecord r;
    private final List<int[]> cachedSets;
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final UiTheme theme;

    public SetsChartView(Context c, MatchRecord x) {
        super(c);
        r = x;
        cachedSets = x.analytics().gameSegments;
        theme = new UiTheme(c);
        setContentDescription("Game scores bar chart for " + r.playerOneName + " versus " + r.playerTwoName + ".");
    }

    @Override
    protected void onDraw(Canvas c) {
        float l = dp(42), t = dp(38), rr = getWidth() - dp(16), b = getHeight() - dp(48);
        if (rr <= l || b <= t) return;
        List<int[]> sets = cachedSets;
        int max = 1;
        for (int[] s : sets) max = Math.max(max, Math.max(s[2], s[3]));
        float step = (rr - l) / Math.max(1, sets.size());
        p.setTextAlign(Paint.Align.CENTER);
        int c1 = theme.playerAccentOnSurface(r.playerOneColor);
        int c2 = theme.playerAccentOnSurface(r.playerTwoColor);

        for (int i = 0; i < sets.size(); i++) {
            int[] s = sets.get(i);
            float cx = l + step * (i + 0.5f), bar = step * 0.25f;
            float y1 = b - (b - t) * s[2] / max;
            float y2 = b - (b - t) * s[3] / max;

            p.setStyle(Paint.Style.FILL);
            p.setColor(theme.playerPanelFill(r.playerOneColor));
            c.drawRect(cx - bar, y1, cx - dp(3), b, p);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(dp(2));
            p.setColor(c1);
            c.drawRect(cx - bar, y1, cx - dp(3), b, p);

            p.setStyle(Paint.Style.FILL);
            p.setColor(theme.playerPanelFill(r.playerTwoColor));
            c.drawRect(cx + dp(3), y2, cx + bar, b, p);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(dp(2));
            p.setColor(c2);
            c.drawRect(cx + dp(3), y2, cx + bar, b, p);

            p.setStyle(Paint.Style.FILL);
            p.setColor(theme.onSurfaceVariant);
            p.setTextSize(dp(14));
            c.drawText("G" + (i + 1), cx, b + dp(22), p);
            p.setColor(theme.onSurface);
            c.drawText(s[2] + ":" + s[3], cx, t - dp(10), p);
        }
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }
}
