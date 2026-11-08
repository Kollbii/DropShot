package pl.maciek.squashscoreboard;

import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;

/**
 * Side-by-side career comparison between 2 players using shared {@link StatComparisonRowView}
 * broadcast proportion bars and [L]/[R] serve box metrics.
 */
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
        Map<String, PlayerStatistics.Summary> summaries =
            PlayerStatistics.calculateAll(this, Arrays.asList(one, two));
        PlayerStatistics.Summary a = summaries.getOrDefault(one.id, new PlayerStatistics.Summary());
        PlayerStatistics.Summary b = summaries.getOrDefault(two.id, new PlayerStatistics.Summary());
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

        TextView courtHeader = text("COURT [L] AND [R] SERVE COMPARISON", 14, theme.primary);
        courtHeader.setTypeface(Typeface.DEFAULT_BOLD);
        courtHeader.setGravity(Gravity.CENTER);
        table.addView(courtHeader, new LinearLayout.LayoutParams(-1, dp(28)));

        CourtSideDiagramView courtDiagram = new CourtSideDiagramView(this, one, a, two, b);
        LinearLayout.LayoutParams courtLp = new LinearLayout.LayoutParams(-1, -2);
        courtLp.topMargin = dp(2);
        courtLp.bottomMargin = dp(8);
        table.addView(courtDiagram, courtLp);

        addRow(table, StatComparisonRowView.createHeader(
            this, theme, one.name, one.fontId, one.color, "STATISTICS", two.name, two.fontId, two.color
        ));

        addMetric(table, String.valueOf(a.matches), "Matches", String.valueOf(b.matches), a.matches, b.matches);
        addMetric(table, String.valueOf(a.wins), "Wins", String.valueOf(b.wins), a.wins, b.wins);
        addMetric(table, pct(a.wins, a.matches) + "%", "Win rate", pct(b.wins, b.matches) + "%", pctVal(a.wins, a.matches), pctVal(b.wins, b.matches));
        addMetric(
            table,
            String.format(Locale.US, "%.1f%%", 100.0 * a.laplaceWinProbability()),
            "Laplace win prob",
            String.format(Locale.US, "%.1f%%", 100.0 * b.laplaceWinProbability()),
            a.laplaceWinProbability(),
            b.laplaceWinProbability()
        );
        addMetric(
            table,
            String.format(Locale.US, "%.1f%%", 100.0 * a.wilsonLowerBound95()),
            "Wilson 95% bound",
            String.format(Locale.US, "%.1f%%", 100.0 * b.wilsonLowerBound95()),
            a.wilsonLowerBound95(),
            b.wilsonLowerBound95()
        );
        addMetric(table, String.valueOf(a.points), "Points", String.valueOf(b.points), a.points, b.points);
        addMetric(table, avg(a.points, a.matches), "Avg pts/match", avg(b.points, b.matches), avgVal(a.points, a.matches), avgVal(b.points, b.matches));
        addMetric(
            table,
            pct(a.servesWon, a.servesTotal) + "%",
            "Serve hold rate",
            pct(b.servesWon, b.servesTotal) + "%",
            a.serveHoldRate(),
            b.serveHoldRate()
        );
        addMetric(
            table,
            pct(a.receivesWon, a.receivesTotal) + "%",
            "Side-out rate",
            pct(b.receivesWon, b.receivesTotal) + "%",
            a.sideOutRate(),
            b.sideOutRate()
        );
        addMetric(
            table,
            String.format(Locale.US, "%.1f%%", 100.0 * a.markovServeShare()),
            "Markov serve %",
            String.format(Locale.US, "%.1f%%", 100.0 * b.markovServeShare()),
            a.markovServeShare(),
            b.markovServeShare()
        );
        addMetric(
            table,
            boxStat(a.leftServesWon, a.leftServesTotal),
            "Left [L] serve points",
            boxStat(b.leftServesWon, b.leftServesTotal),
            pctVal(a.leftServesWon, a.leftServesTotal),
            pctVal(b.leftServesWon, b.leftServesTotal)
        );
        addMetric(
            table,
            boxStat(a.rightServesWon, a.rightServesTotal),
            "Right [R] serve points",
            boxStat(b.rightServesWon, b.rightServesTotal),
            pctVal(a.rightServesWon, a.rightServesTotal),
            pctVal(b.rightServesWon, b.rightServesTotal)
        );
        addMetric(
            table,
            "L:" + a.leftPicks + " · R:" + a.rightPicks,
            "Handout [L]/[R] picks",
            "L:" + b.leftPicks + " · R:" + b.rightPicks,
            a.leftPicks + a.rightPicks,
            b.leftPicks + b.rightPicks
        );
        addMetric(
            table,
            pct(a.pickedServesWon, a.pickedServesTotal) + "%",
            "Chosen box (PICK) %",
            pct(b.pickedServesWon, b.pickedServesTotal) + "%",
            pctVal(a.pickedServesWon, a.pickedServesTotal),
            pctVal(b.pickedServesWon, b.pickedServesTotal)
        );
        addMetric(
            table,
            pct(a.altServesWon, a.altServesTotal) + "%",
            "Streak box (ALT) %",
            pct(b.altServesWon, b.altServesTotal) + "%",
            pctVal(a.altServesWon, a.altServesTotal),
            pctVal(b.altServesWon, b.altServesTotal)
        );
        addMetric(table, String.valueOf(a.score67), "6-7 or 7-6", String.valueOf(b.score67), a.score67, b.score67);
        addMetric(table, String.valueOf(a.score69), "6-9 or 9-6", String.valueOf(b.score69), a.score69, b.score69);

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

    private void addRow(LinearLayout table, View row) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = dp(4);
        table.addView(row, lp);
    }

    private void addMetric(LinearLayout table, String left, String label, String right, double v1, double v2) {
        addRow(table, StatComparisonRowView.createMetric(this, theme, left, label, right, one.color, two.color, v1, v2));
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

    private static String boxStat(int won, int total) {
        if (total == 0) return "0/0 (0%)";
        return won + "/" + total + " (" + Math.round(won * 100f / total) + "%)";
    }

    private static double avgVal(int a, int b) {
        return b == 0 ? 0.0 : (double) a / b;
    }

    private static double pctVal(int a, int b) {
        return b == 0 ? 0.0 : (100.0 * a) / b;
    }

    private static String avg(int a, int b) {
        return b == 0 ? "0.0" : String.format(Locale.US, "%.1f", a / (float) b);
    }

    private static String pct(int a, int b) {
        return b == 0 ? "0" : String.valueOf(Math.round(a * 100f / b));
    }

    private int dp(int v) {
        return UiTheme.dp(this, v);
    }
}
