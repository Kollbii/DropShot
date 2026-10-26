package pl.maciek.squashscoreboard;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.json.JSONObject;
import java.util.Locale;

public class MatchDetailActivity extends Activity {
    public static final String EXTRA_RECORD = "record";
    private UiTheme theme;
    private MatchRecord currentRecord;
    private Button[] tabButtons;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        try {
            currentRecord = MatchRecord.fromJson(new JSONObject(getIntent().getStringExtra(EXTRA_RECORD)));
            showRecord(currentRecord);
        } catch (Exception e) {
            finish();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (currentRecord != null) showRecord(currentRecord);
    }

    private void showRecord(MatchRecord r) {
        theme = new UiTheme(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(10), dp(16), dp(10));
        root.setBackgroundColor(theme.background);

        // Top Bar
        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);

        Button back = theme.outlinedButton(this, "← Go back");
        back.setContentDescription("Go back.");
        back.setOnClickListener(v -> finish());

        TextView names = text(
            r.playerOneName + "  vs  " + r.playerTwoName + "  (" + r.playerOneSets + " : " + r.playerTwoSets + ")",
            20,
            theme.onSurface
        );
        names.setTypeface(Typeface.DEFAULT_BOLD);
        names.setGravity(Gravity.CENTER);
        UiTheme.applyTabularNumbers(names);

        Button deleteBtn = theme.dangerButton(this, "Delete match");
        deleteBtn.setOnClickListener(v ->
            new AlertDialog.Builder(this)
                .setTitle("Delete this match?")
                .setMessage("The app will delete the match " + r.playerOneName + " vs " + r.playerTwoName + " permanently.")
                .setNegativeButton("Cancel action", null)
                .setPositiveButton("Delete match", (d, w) -> {
                    MatchHistoryStore.remove(this, r.id);
                    finish();
                })
                .show()
        );

        header.addView(back, new LinearLayout.LayoutParams(-2, dp(48)));
        header.addView(names, new LinearLayout.LayoutParams(0, dp(48), 1f));
        header.addView(deleteBtn, new LinearLayout.LayoutParams(-2, dp(48)));
        root.addView(header, new LinearLayout.LayoutParams(-1, dp(52)));

        // Summary Info Row
        String durText = r.durationMs > 0
            ? "MATCH DURATION: " + formatDuration(r.durationMs)
            : "MATCH DURATION: no data";
        TextView meta = text(durText + "   |   " + setTimes(r), 16, theme.onSurfaceVariant);
        meta.setGravity(Gravity.CENTER);
        UiTheme.applyTabularNumbers(meta);
        meta.setPadding(0, dp(4), 0, dp(4));
        root.addView(meta, new LinearLayout.LayoutParams(-1, -2));

        // 4 Direct Tab Buttons (Point timeline, Game progress, Game scores, Match statistics)
        LinearLayout tabRow = new LinearLayout(this);
        tabRow.setOrientation(LinearLayout.HORIZONTAL);
        tabRow.setGravity(Gravity.CENTER_VERTICAL);
        tabRow.setPadding(0, dp(4), 0, dp(4));

        String[] tabTitles = {
            "Show point chart",
            "Show game chart",
            "Show game scores",
            "Show statistics"
        };
        tabButtons = new Button[4];

        TextView caption = text("POINT TIMELINE  ·  TAP CHART TO OPEN FULLSCREEN", 16, theme.onSurfaceVariant);
        caption.setGravity(Gravity.CENTER);

        ChartPager charts = new ChartPager(this);
        charts.setPageCount(4);
        charts.setHorizontalScrollBarEnabled(false);
        LinearLayout pages = new LinearLayout(this);
        pages.setOrientation(LinearLayout.HORIZONTAL);
        charts.addView(pages);

        for (int i = 0; i < 4; i++) {
            final int pageIndex = i;
            Button tab = new Button(this);
            tab.setAllCaps(false);
            tab.setTextSize(15);
            tab.setMinHeight(dp(48));
            tab.setMinimumHeight(dp(48));
            tab.setPadding(dp(8), 0, dp(8), 0);
            tab.setText(tabTitles[i]);
            tab.setOnClickListener(v -> {
                int w = Math.max(1, charts.getWidth());
                charts.smoothScrollTo(pageIndex * w, 0);
                updateTabButtons(pageIndex, caption);
            });
            tabButtons[i] = tab;
            LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(0, dp(48), 1f);
            tlp.setMargins(dp(3), 0, dp(3), 0);
            tabRow.addView(tab, tlp);
        }
        root.addView(tabRow, new LinearLayout.LayoutParams(-1, dp(52)));
        root.addView(caption, new LinearLayout.LayoutParams(-1, dp(26)));
        root.addView(charts, new LinearLayout.LayoutParams(-1, 0, 1f));

        updateTabButtons(0, caption);

        charts.post(() -> {
            int w = Math.max(1, charts.getWidth());
            View score = new ScoreChartView(this, r);
            View progress = new SetProgressChartView(this, r);
            View sets = new SetsChartView(this, r);
            View stats = new MatchStatsView(this, r);
            score.setOnClickListener(v -> openFullscreen(0));
            progress.setOnClickListener(v -> openFullscreen(1));
            sets.setOnClickListener(v -> openFullscreen(2));
            pages.removeAllViews();
            pages.addView(score, new LinearLayout.LayoutParams(w, -1));
            pages.addView(progress, new LinearLayout.LayoutParams(w, -1));
            pages.addView(sets, new LinearLayout.LayoutParams(w, -1));
            pages.addView(stats, new LinearLayout.LayoutParams(w, -1));
            charts.setOnScrollChangeListener((v, x, y, ox, oy) -> {
                int p = Math.max(0, Math.min(3, Math.round(x / (float) Math.max(1, w))));
                updateTabButtons(p, caption);
            });
        });

        // Accessible Legend with non-color symbols + line style descriptions
        LinearLayout legend = new LinearLayout(this);
        legend.setGravity(Gravity.CENTER);
        TextView p1 = text(
            UiTheme.playerSymbol(r.playerOneColor) + " " + r.playerOneName + " (solid line)",
            16,
            theme.playerAccentOnSurface(r.playerOneColor)
        );
        TextView p2 = text(
            UiTheme.playerSymbol(r.playerTwoColor) + " " + r.playerTwoName + " (dashed line)",
            16,
            theme.playerAccentOnSurface(r.playerTwoColor)
        );
        p1.setTypeface(Typeface.DEFAULT_BOLD);
        p2.setTypeface(Typeface.DEFAULT_BOLD);
        p1.setGravity(Gravity.CENTER);
        p2.setGravity(Gravity.CENTER);
        legend.addView(p1, new LinearLayout.LayoutParams(0, -1, 1f));
        legend.addView(p2, new LinearLayout.LayoutParams(0, -1, 1f));
        root.addView(legend, new LinearLayout.LayoutParams(-1, dp(30)));

        Button resume = theme.primaryButton(this, r.finished ? "Add 3 games on board" : "Resume match");
        resume.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            intent.putExtra(EXTRA_RECORD, getIntent().getStringExtra(EXTRA_RECORD));
            intent.putExtra("continue", r.finished);
            startActivity(intent);
            finish();
        });
        LinearLayout.LayoutParams resumeParams = new LinearLayout.LayoutParams(-1, dp(48));
        resumeParams.topMargin = dp(4);
        root.addView(resume, resumeParams);

        setContentView(root);
    }

    private void updateTabButtons(int selectedPage, TextView caption) {
        if (tabButtons == null) return;
        for (int i = 0; i < tabButtons.length; i++) {
            Button b = tabButtons[i];
            if (b == null) continue;
            boolean active = (i == selectedPage);
            b.setTypeface(active ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
            b.setTextColor(active ? theme.onSecondaryContainer : theme.onSurfaceVariant);
            b.setBackground(theme.interactiveBackground(
                this,
                active ? theme.secondaryContainer : theme.surfaceLow,
                12,
                1,
                active ? theme.primary : theme.outline
            ));
        }
        if (caption != null) {
            if (selectedPage == 0) caption.setText("POINT TIMELINE  ·  TAP CHART TO OPEN FULLSCREEN");
            else if (selectedPage == 1) caption.setText("GAME PROGRESS  ·  TAP CHART TO OPEN FULLSCREEN");
            else if (selectedPage == 2) caption.setText("GAME SCORES  ·  TAP CHART TO OPEN FULLSCREEN");
            else caption.setText("MATCH STATISTICS");
        }
    }

    private void openFullscreen(int chart) {
        startActivity(
            new Intent(this, ChartFullscreenActivity.class)
                .putExtra(ChartFullscreenActivity.EXTRA_RECORD, getIntent().getStringExtra(EXTRA_RECORD))
                .putExtra(ChartFullscreenActivity.EXTRA_CHART, chart)
        );
    }

    private String setTimes(MatchRecord r) {
        if (r.setDurationsMs.isEmpty()) return "GAME DURATIONS: no data";
        StringBuilder s = new StringBuilder("GAME DURATIONS: ");
        for (int i = 0; i < r.setDurationsMs.size(); i++) {
            if (i > 0) s.append("  ·  ");
            s.append("G").append(i + 1).append(" ").append(formatDuration(r.setDurationsMs.get(i)));
        }
        return s.toString();
    }

    private String formatDuration(long ms) {
        long s = Math.max(0, ms) / 1000;
        return String.format(Locale.US, "%02d:%02d", s / 60, s % 60);
    }

    private TextView text(String v, int size, int c) {
        TextView t = new TextView(this);
        t.setText(v);
        t.setTextSize(size);
        t.setTextColor(c);
        return t;
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }
}
