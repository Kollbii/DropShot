package pl.maciek.squashscoreboard;

import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.Locale;

public class PlayerComparisonActivity extends Activity {
    private PlayerProfile one, two;
    private UiTheme theme;

    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);
        draw();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        draw();
    }

    private void draw() {
        one = ProfileStore.find(this, getIntent().getStringExtra("one"));
        two = ProfileStore.find(this, getIntent().getStringExtra("two"));
        if (one == null || two == null) {
            finish();
            return;
        }
        theme = new UiTheme(this);
        PlayerStatistics.Summary a = PlayerStatistics.calculate(this, one);
        PlayerStatistics.Summary b = PlayerStatistics.calculate(this, two);
        int[] h2h = headToHead();

        LinearLayout root = box(theme.background);
        root.setPadding(dp(16), dp(12), dp(16), dp(12));

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);

        Button back = theme.outlinedButton(this, "← Go back");
        back.setContentDescription("Go back.");
        back.setOnClickListener(v -> finish());

        TextView title = text("Player comparison", 22, theme.onSurface);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER);

        header.addView(back, new LinearLayout.LayoutParams(-2, dp(48)));
        header.addView(title, new LinearLayout.LayoutParams(0, dp(48), 1f));
        root.addView(header, new LinearLayout.LayoutParams(-1, dp(52)));

        ScrollView scroll = new ScrollView(this);
        LinearLayout table = box(theme.background);
        table.setPadding(0, dp(6), 0, dp(6));
        table.addView(headerRow(), rowParams());

        String[] labels = {
            "Matches",
            "Wins",
            "Win rate",
            "Points",
            "Avg pts/match",
            "6-7 or 7-6",
            "6-9 or 9-6"
        };
        String[] left = {
            String.valueOf(a.matches),
            String.valueOf(a.wins),
            pct(a.wins, a.matches) + "%",
            String.valueOf(a.points),
            avg(a.points, a.matches),
            String.valueOf(a.score67),
            String.valueOf(a.score69)
        };
        String[] right = {
            String.valueOf(b.matches),
            String.valueOf(b.wins),
            pct(b.wins, b.matches) + "%",
            String.valueOf(b.points),
            avg(b.points, b.matches),
            String.valueOf(b.score67),
            String.valueOf(b.score69)
        };

        for (int i = 0; i < labels.length; i++) {
            table.addView(statRow(left[i], labels[i], right[i]), rowParams());
        }
        scroll.addView(table);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));

        TextView direct = text(
            "HEAD-TO-HEAD SCORE: " + one.name + " " + h2h[1] + " : " + h2h[2] + " " + two.name + "   (" + h2h[0] + " matches)",
            17,
            theme.onSecondaryContainer
        );
        direct.setTypeface(Typeface.DEFAULT_BOLD);
        direct.setGravity(Gravity.CENTER);
        UiTheme.applyTabularNumbers(direct);
        direct.setBackground(theme.roundedBox(theme.secondaryContainer, 12, 1, theme.outlineVariant, this));
        root.addView(direct, new LinearLayout.LayoutParams(-1, dp(48)));
        setContentView(root);
    }

    private LinearLayout.LayoutParams rowParams() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(48));
        lp.topMargin = dp(4);
        return lp;
    }

    private LinearLayout headerRow() {
        int c1 = theme.playerAccentOnSurface(one.color);
        int c2 = theme.playerAccentOnSurface(two.color);
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackground(theme.roundedBox(theme.surfaceHigh, 10, 1, theme.outlineVariant, this));

        TextView first = cell(UiTheme.playerSymbol(one.color) + " " + one.name, 18, c1, true);
        first.setSingleLine(true);
        first.setEllipsize(TextUtils.TruncateAt.END);

        TextView middle = cell("Metric", 16, theme.onSurfaceVariant, true);

        TextView second = cell(UiTheme.playerSymbol(two.color) + " " + two.name, 18, c2, true);
        second.setSingleLine(true);
        second.setEllipsize(TextUtils.TruncateAt.END);

        row.addView(first, new LinearLayout.LayoutParams(0, -1, 1f));
        row.addView(middle, new LinearLayout.LayoutParams(dp(160), -1));
        row.addView(second, new LinearLayout.LayoutParams(0, -1, 1f));
        return row;
    }

    private LinearLayout statRow(String left, String label, String right) {
        int c1 = theme.playerAccentOnSurface(one.color);
        int c2 = theme.playerAccentOnSurface(two.color);
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackground(theme.roundedBox(theme.surfaceLow, 10, 1, theme.outlineVariant, this));
        row.setContentDescription(label + ": " + one.name + " " + left + ", " + two.name + " " + right);

        TextView leftCell = cell(left, 18, c1, true);
        TextView midCell = cell(label, 16, theme.onSurfaceVariant, false);
        TextView rightCell = cell(right, 18, c2, true);
        UiTheme.applyTabularNumbers(leftCell);
        UiTheme.applyTabularNumbers(rightCell);

        row.addView(leftCell, new LinearLayout.LayoutParams(0, -1, 1f));
        row.addView(midCell, new LinearLayout.LayoutParams(dp(160), -1));
        row.addView(rightCell, new LinearLayout.LayoutParams(0, -1, 1f));
        return row;
    }

    private TextView cell(String value, int size, int color, boolean bold) {
        TextView v = text(value, size, color);
        if (bold) v.setTypeface(Typeface.DEFAULT_BOLD);
        v.setGravity(Gravity.CENTER);
        return v;
    }

    private int[] headToHead() {
        int[] out = new int[3];
        for (MatchRecord r : MatchHistoryStore.load(this)) {
            boolean oneFirst = PlayerStatistics.belongs(r, true, one) && PlayerStatistics.belongs(r, false, two);
            boolean twoFirst = PlayerStatistics.belongs(r, true, two) && PlayerStatistics.belongs(r, false, one);
            if (!oneFirst && !twoFirst) continue;
            out[0]++;
            int winner = r.statisticalWinner();
            if (winner == 0) continue;
            if ((oneFirst && winner == 1) || (twoFirst && winner == 2)) out[1]++;
            else out[2]++;
        }
        return out;
    }

    private LinearLayout box(int c) {
        LinearLayout x = new LinearLayout(this);
        x.setOrientation(LinearLayout.VERTICAL);
        x.setBackgroundColor(c);
        return x;
    }

    private TextView text(String s, int size, int color) {
        TextView v = new TextView(this);
        v.setText(s);
        v.setTextSize(size);
        v.setTextColor(color);
        return v;
    }

    private String avg(int a, int b) {
        return b == 0 ? "0.0" : String.format(Locale.US, "%.1f", a / (float) b);
    }

    private String pct(int a, int b) {
        return b == 0 ? "0" : String.valueOf(Math.round(a * 100f / b));
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }
}
