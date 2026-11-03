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
        int[] servesTotal = new int[2], servesWon = new int[2];
        int[] receivesTotal = new int[2], receivesWon = new int[2];
        int[] rightPicks = new int[2], leftPicks = new int[2];
        int[] rightServes = new int[2], rightWon = new int[2];
        int[] leftServes = new int[2], leftWon = new int[2];
        int previous = 0, streak = 0;
        int n = record.pointWinners.size();
        for (int i = 0; i < n; i++) {
            int winner = record.pointWinners.get(i);
            if (winner < 1 || winner > 2) continue;
            int wIdx = winner - 1;
            points[wIdx]++;
            streak = winner == previous ? streak + 1 : 1;
            streaks[wIdx] = Math.max(streaks[wIdx], streak);
            previous = winner;

            int srv = record.serverAtPoint(i);
            int sIdx = srv == 2 ? 1 : 0;
            int rIdx = 1 - sIdx;
            char side = record.sideAtPoint(i);
            boolean picked = record.isSidePickedAtPoint(i);

            servesTotal[sIdx]++;
            receivesTotal[rIdx]++;
            if (winner == srv) {
                servesWon[sIdx]++;
            } else {
                receivesWon[rIdx]++;
            }
            if (picked) {
                if (side == 'L') leftPicks[sIdx]++; else rightPicks[sIdx]++;
            }
            if (side == 'L') {
                leftServes[sIdx]++;
                if (winner == srv) leftWon[sIdx]++;
            } else {
                rightServes[sIdx]++;
                if (winner == srv) rightWon[sIdx]++;
            }
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
        row(
            rows,
            rateWithCount(servesWon[0], servesTotal[0]),
            "SERVE HOLD RATE",
            rateWithCount(servesWon[1], servesTotal[1]),
            c1,
            c2,
            false
        );
        row(
            rows,
            rateWithCount(receivesWon[0], receivesTotal[0]),
            "SIDE-OUT RATE",
            rateWithCount(receivesWon[1], receivesTotal[1]),
            c1,
            c2,
            false
        );
        row(
            rows,
            markovServeShare(servesWon[0], servesTotal[0], receivesWon[0], receivesTotal[0]),
            "MARKOV SERVE SHARE",
            markovServeShare(servesWon[1], servesTotal[1], receivesWon[1], receivesTotal[1]),
            c1,
            c2,
            false
        );
        row(
            rows,
            pickAndWin(rightPicks[0], rightWon[0], rightServes[0]),
            "RIGHT [R] PICKS (WIN %)",
            pickAndWin(rightPicks[1], rightWon[1], rightServes[1]),
            c1,
            c2,
            false
        );
        row(
            rows,
            pickAndWin(leftPicks[0], leftWon[0], leftServes[0]),
            "LEFT [L] PICKS (WIN %)",
            pickAndWin(leftPicks[1], leftWon[1], leftServes[1]),
            c1,
            c2,
            false
        );

        if (n > 0) {
            TextView logTitle = cell("POINT-BY-POINT SERVE & COURT SIDE LOG", 15, theme.onSurfaceVariant, true);
            LinearLayout.LayoutParams logTitleLp = new LinearLayout.LayoutParams(-1, dp(38));
            logTitleLp.topMargin = dp(12);
            rows.addView(logTitle, logTitleLp);

            int currentGame = 0;
            for (int i = 0; i < n; i++) {
                MatchRecord.PointScore ps = record.scoreAtPoint(i);
                if (ps.setNumber != currentGame) {
                    currentGame = ps.setNumber;
                    TextView gameHeader = cell("GAME " + currentGame, 14, theme.primary, true);
                    LinearLayout.LayoutParams ghLp = new LinearLayout.LayoutParams(-1, dp(32));
                    ghLp.topMargin = dp(6);
                    rows.addView(gameHeader, ghLp);
                }
                int winner = record.pointWinners.get(i);
                String srvName = ps.server == 2 ? record.playerTwoName : record.playerOneName;
                String winName = winner == 2 ? record.playerTwoName : record.playerOneName;
                String sideTag = "[" + ps.serviceSide + "] " + (ps.sidePicked ? "PICK" : "ALT");
                String scoreStr = ps.pointsOne + ":" + ps.pointsTwo;
                pointLogRow(rows, i + 1, srvName, sideTag, scoreStr, winName, ps.server == 1 ? c1 : c2, winner == 1 ? c1 : c2);
            }
        }
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

    private String rateWithCount(int won, int total) {
        if (total == 0) return "0% (0/0)";
        return String.format(Locale.US, "%.0f%% (%d/%d)", 100f * won / total, won, total);
    }

    private String markovServeShare(int srvWon, int srvTotal, int rcvWon, int rcvTotal) {
        if (srvTotal == 0 && rcvTotal == 0) return "50.0%";
        double pS = srvTotal == 0 ? 0.5 : (double) srvWon / srvTotal;
        double pR = rcvTotal == 0 ? 0.5 : (double) rcvWon / rcvTotal;
        double denom = (1.0 - pS) + pR;
        double pi = denom <= 1e-9 ? 0.5 : pR / denom;
        return String.format(Locale.US, "%.1f%%", 100.0 * pi);
    }

    private String pickAndWin(int picks, int boxWon, int boxTotal) {
        if (boxTotal == 0) return picks + " (0%)";
        return String.format(Locale.US, "%d (%.0f%%)", picks, 100f * boxWon / boxTotal);
    }

    private void pointLogRow(
        LinearLayout parent,
        int rallyNumber,
        String serverName,
        String sideTag,
        String scoreStr,
        String winnerName,
        int srvColor,
        int winColor
    ) {
        LinearLayout row = new LinearLayout(getContext());
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(dp(38));
        row.setBackground(theme.roundedBox(theme.surfaceLow, 8, 1, theme.outlineVariant, getContext()));
        row.setContentDescription(
            "Point " + rallyNumber + ": Server " + serverName + " " + sideTag + ", score " + scoreStr + ", won by " + winnerName
        );

        TextView numCell = cell("#" + rallyNumber, 13, theme.onSurfaceVariant, false);
        TextView srvCell = cell("SRV: " + serverName + " " + sideTag, 13, srvColor, true);
        TextView scoreCell = cell(scoreStr, 15, theme.onSurface, true);
        TextView winCell = cell("WIN: " + winnerName, 13, winColor, true);
        UiTheme.applyTabularNumbers(numCell);
        UiTheme.applyTabularNumbers(scoreCell);

        row.addView(numCell, new LinearLayout.LayoutParams(0, dp(38), 0.55f));
        row.addView(srvCell, new LinearLayout.LayoutParams(0, dp(38), 1.45f));
        row.addView(scoreCell, new LinearLayout.LayoutParams(0, dp(38), 0.65f));
        row.addView(winCell, new LinearLayout.LayoutParams(0, dp(38), 1.15f));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(38));
        lp.topMargin = dp(3);
        parent.addView(row, lp);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
