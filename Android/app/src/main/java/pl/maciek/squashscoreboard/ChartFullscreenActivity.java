package pl.maciek.squashscoreboard;

import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.json.JSONObject;

/** Displays one chosen history chart at full screen size with single-tap zoom controls. */
public class ChartFullscreenActivity extends Activity {
    public static final String EXTRA_RECORD = "record", EXTRA_CHART = "chart";
    private MatchRecord currentRecord;
    private int currentChart;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        try {
            currentRecord = MatchRecord.fromJson(new JSONObject(getIntent().getStringExtra(EXTRA_RECORD)));
            currentChart = getIntent().getIntExtra(EXTRA_CHART, 0);
            showChart(currentRecord, currentChart);
        } catch (Exception e) {
            finish();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (currentRecord != null) showChart(currentRecord, currentChart);
    }

    private void showChart(MatchRecord r, int chart) {
        UiTheme theme = new UiTheme(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(10), dp(16), dp(10));
        root.setBackgroundColor(theme.background);

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);

        Button close = theme.outlinedButton(this, "← Close chart");
        close.setContentDescription("Close chart.");
        close.setOnClickListener(v -> finish());

        String title = chart == 0 ? "POINT TIMELINE" : chart == 1 ? "GAME PROGRESS" : "GAME SCORES";
        TextView caption = text(title + "  ·  " + r.playerOneName + " vs " + r.playerTwoName, 20, theme.onSurface);
        caption.setTypeface(Typeface.DEFAULT_BOLD);
        caption.setGravity(Gravity.CENTER);

        header.addView(close, new LinearLayout.LayoutParams(-2, dp(48)));
        header.addView(caption, new LinearLayout.LayoutParams(0, dp(48), 1f));

        View view = chart == 0
            ? new ScoreChartView(this, r)
            : chart == 1
                ? new SetProgressChartView(this, r)
                : new SetsChartView(this, r);

        if (view instanceof ScoreChartView) {
            ScoreChartView scoreView = (ScoreChartView) view;
            scoreView.setInteractive(true);
            Button zoomIn = theme.tonalButton(this, "Zoom in");
            Button zoomOut = theme.outlinedButton(this, "Zoom out");
            Button resetZoom = theme.outlinedButton(this, "Reset zoom");
            zoomIn.setOnClickListener(v -> scoreView.zoomBy(1.5));
            zoomOut.setOnClickListener(v -> scoreView.zoomBy(1.0 / 1.5));
            resetZoom.setOnClickListener(v -> scoreView.resetZoom());
            header.addView(zoomIn, btnMargin());
            header.addView(zoomOut, btnMargin());
            header.addView(resetZoom, btnMargin());
        } else if (view instanceof SetProgressChartView) {
            SetProgressChartView setView = (SetProgressChartView) view;
            setView.setInteractive(true);
            Button allGames = theme.primaryButton(this, "Show all games");
            allGames.setVisibility(View.GONE);
            allGames.setOnClickListener(v -> setView.showAllSets());
            setView.setOnSetFocusChanged(number -> {
                caption.setText(
                    number == 0
                        ? "GAME PROGRESS · TAP A GAME TO FILTER"
                        : "GAME " + number + " · " + r.playerOneName + " vs " + r.playerTwoName
                );
                allGames.setVisibility(number == 0 ? View.GONE : View.VISIBLE);
            });
            caption.setText("GAME PROGRESS · TAP A GAME TO FILTER");
            header.addView(allGames, btnMargin());
        }

        root.addView(header, new LinearLayout.LayoutParams(-1, dp(52)));
        root.addView(view, new LinearLayout.LayoutParams(-1, 0, 1f));

        LinearLayout legend = new LinearLayout(this);
        legend.setGravity(Gravity.CENTER);
        TextView p1 = text(
            r.playerOneName + " (solid line)",
            16,
            theme.playerAccentOnSurface(r.playerOneColor)
        );
        TextView p2 = text(
            r.playerTwoName + " (dashed line)",
            16,
            theme.playerAccentOnSurface(r.playerTwoColor)
        );
        p1.setTypeface(Typeface.DEFAULT_BOLD);
        p2.setTypeface(Typeface.DEFAULT_BOLD);
        p1.setGravity(Gravity.CENTER);
        p2.setGravity(Gravity.CENTER);
        legend.addView(p1, new LinearLayout.LayoutParams(0, -1, 1f));
        legend.addView(p2, new LinearLayout.LayoutParams(0, -1, 1f));
        root.addView(legend, new LinearLayout.LayoutParams(-1, dp(32)));

        setContentView(root);
    }

    private LinearLayout.LayoutParams btnMargin() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, dp(48));
        lp.leftMargin = dp(6);
        return lp;
    }

    private TextView text(String s, int z, int c) {
        TextView v = new TextView(this);
        v.setText(s);
        v.setTextSize(z);
        v.setTextColor(c);
        return v;
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }
}
