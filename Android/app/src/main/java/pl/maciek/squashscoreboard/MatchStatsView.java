package pl.maciek.squashscoreboard;

import android.content.Context;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.Locale;

/** Side-by-side figures for one recorded match. */
public final class MatchStatsView extends ScrollView {
    private final UiTheme theme;

    public MatchStatsView(Context context, MatchRecord record) {
        super(context);
        theme = new UiTheme(context);
        LinearLayout rows = new LinearLayout(context);
        rows.setOrientation(LinearLayout.VERTICAL);
        rows.setPadding(dp(16), dp(8), dp(16), dp(8));
        addView(rows);

        int[] sets = record.playedSets();
        int[] points = new int[2], streaks = new int[2];
        int previous = 0, streak = 0;
        for (int winner : record.pointWinners) {
            if (winner < 1 || winner > 2) continue;
            points[winner - 1]++;
            streak = winner == previous ? streak + 1 : 1;
            streaks[winner - 1] = Math.max(streaks[winner - 1], streak);
            previous = winner;
        }
        int total = points[0] + points[1];
        int format = Math.max(1, (sets[0] + sets[1] + 2) / 2) * 2 - 1;

        TextView title = cell("MATCH STATISTICS  ·  BEST OF " + format + " GAMES", 16, theme.onSurfaceVariant, true);
        rows.addView(title, new LinearLayout.LayoutParams(-1, dp(38)));

        int c1 = theme.playerAccentOnSurface(record.playerOneColor);
        int c2 = theme.playerAccentOnSurface(record.playerTwoColor);

        row(
            rows,
            record.playerOneName,
            "METRIC",
            record.playerTwoName,
            c1,
            c2,
            true
        );
        row(rows, String.valueOf(sets[0]), "GAMES WON", String.valueOf(sets[1]), c1, c2, false);
        row(rows, String.valueOf(points[0]), "POINTS WON", String.valueOf(points[1]), c1, c2, false);
        row(rows, share(points[0], total), "POINT SHARE", share(points[1], total), c1, c2, false);
        row(rows, String.valueOf(streaks[0]), "LONGEST STREAK", String.valueOf(streaks[1]), c1, c2, false);
    }

    private void row(LinearLayout parent, String left, String label, String right, int color1, int color2, boolean header) {
        LinearLayout row = new LinearLayout(getContext());
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(dp(48));
        row.setBackground(theme.roundedBox(header ? theme.surfaceHigh : theme.surfaceLow, 10, 1, theme.outlineVariant, getContext()));
        row.setContentDescription(label + ": " + left + " versus " + right);

        TextView leftCell = cell(left, 18, color1, true);
        TextView midCell = cell(label, 16, theme.onSurfaceVariant, header);
        TextView rightCell = cell(right, 18, color2, true);
        UiTheme.applyTabularNumbers(leftCell);
        UiTheme.applyTabularNumbers(rightCell);

        row.addView(leftCell, new LinearLayout.LayoutParams(0, dp(48), 1f));
        row.addView(midCell, new LinearLayout.LayoutParams(0, dp(48), 1.4f));
        row.addView(rightCell, new LinearLayout.LayoutParams(0, dp(48), 1f));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(48));
        lp.topMargin = dp(4);
        parent.addView(row, lp);
    }

    private TextView cell(String value, int size, int color, boolean bold) {
        TextView view = new TextView(getContext());
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        if (bold) view.setTypeface(Typeface.DEFAULT_BOLD);
        view.setGravity(Gravity.CENTER);
        view.setSingleLine(true);
        view.setEllipsize(TextUtils.TruncateAt.END);
        return view;
    }

    private String share(int points, int total) {
        return total == 0 ? "0%" : String.format(Locale.US, "%.1f%%", 100f * points / total);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
