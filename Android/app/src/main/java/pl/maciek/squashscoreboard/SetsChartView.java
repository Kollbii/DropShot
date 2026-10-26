package pl.maciek.squashscoreboard;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;
import java.util.ArrayList;

public final class SetsChartView extends View {
    private final MatchRecord r;
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final UiTheme theme;

    public SetsChartView(Context c, MatchRecord x) {
        super(c);
        r = x;
        theme = new UiTheme(c);
        setContentDescription("Game scores bar chart for " + r.playerOneName + " versus " + r.playerTwoName + ".");
    }

    @Override
    protected void onDraw(Canvas c) {
        float l = dp(42), t = dp(38), rr = getWidth() - dp(16), b = getHeight() - dp(48);
        if (rr <= l || b <= t) return;
        ArrayList<int[]> sets = all();
        int max = 1;
        for (int[] s : sets) max = Math.max(max, Math.max(s[0], s[1]));
        float step = (rr - l) / Math.max(1, sets.size());
        p.setTextAlign(Paint.Align.CENTER);
        int c1 = theme.playerAccentOnSurface(r.playerOneColor);
        int c2 = theme.playerAccentOnSurface(r.playerTwoColor);

        for (int i = 0; i < sets.size(); i++) {
            int[] s = sets.get(i);
            float cx = l + step * (i + 0.5f), bar = step * 0.25f;
            float y1 = b - (b - t) * s[0] / max;
            float y2 = b - (b - t) * s[1] / max;

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
            c.drawText(s[0] + ":" + s[1], cx, t - dp(10), p);
        }
    }

    private ArrayList<int[]> all() {
        ArrayList<int[]> o = new ArrayList<>();
        int a = 0, z = 0;
        for (int q : r.pointWinners) {
            if (q == 1) a++; else z++;
            if (Math.max(a, z) >= r.pointsToWinSet && Math.abs(a - z) >= 2) {
                o.add(new int[]{a, z});
                a = 0;
                z = 0;
            }
        }
        if (a + z > 0) o.add(new int[]{a, z});
        return o;
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }
}
