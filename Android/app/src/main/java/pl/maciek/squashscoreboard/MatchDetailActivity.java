package pl.maciek.squashscoreboard;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import org.json.JSONObject;

/**
 * Unified match detail screen with a single compact top bar (Back, View Switcher spinner,
 * and Manage match dialog) and a 4-page swipeable {@link ChartPager} for Point chart,
 * Game chart, Game scores, and Match statistics.
 * Maximizes vertical screen visibility for {@link MatchStatsView} and deduplicates
 * the former split between preview charts and fullscreen charts.
 */
public class MatchDetailActivity extends Activity {
    public static final String EXTRA_RECORD = "record";
    public static final String EXTRA_INITIAL_TAB = "initialTab";
    public static final int TAB_TIMELINE = 0;
    public static final int TAB_PROGRESS = 1;
    public static final int TAB_SETS = 2;
    public static final int TAB_STATS = 3;

    private UiTheme theme;
    private MatchRecord currentRecord;
    private int currentTab = TAB_TIMELINE;
    private boolean updatingTabSelection;
    private Spinner tabSpinner;
    private LinearLayout chartFooterBar;
    private LinearLayout legendView;
    private LinearLayout zoomControlsGroup;
    private Button showAllGamesBtn;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        try {
            String rawRecord = getIntent().getStringExtra(EXTRA_RECORD);
            currentRecord = MatchRecord.fromJson(new JSONObject(rawRecord));
            int requestedTab = getIntent().getIntExtra(
                EXTRA_INITIAL_TAB,
                getIntent().getIntExtra(ChartFullscreenActivity.EXTRA_CHART, TAB_TIMELINE)
            );
            currentTab = Math.max(0, Math.min(3, requestedTab));
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
        root.setPadding(dp(12), dp(6), dp(12), dp(6));
        root.setBackgroundColor(theme.background);

        // Single Compact Top Bar (48 dp) to maximize vertical visibility for Statistics & Charts
        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);

        Button back = theme.outlinedButton(this, "← Go back");
        back.setContentDescription("Go back.");
        back.setOnClickListener(v -> finish());

        int[] sets = r.playedSets();
        int dispS1 = Math.max(r.playerOneSets, sets[0]);
        int dispS2 = Math.max(r.playerTwoSets, sets[1]);
        String scoreSummary = dispS1 + ":" + dispS2;

        String[] viewChoices = {
            "1/4 · Point chart (" + scoreSummary + ")",
            "2/4 · Game chart (" + scoreSummary + ")",
            "3/4 · Game scores (" + scoreSummary + ")",
            "4/4 · Statistics (" + scoreSummary + ")"
        };
        tabSpinner = theme.styledSpinner(this, viewChoices, currentTab);
        tabSpinner.setContentDescription("Select match view or swipe left and right.");

        Button manageBtn = theme.tonalButton(this, "Manage match");
        manageBtn.setContentDescription("Manage match: resume, add 3 games, or delete match.");
        manageBtn.setOnClickListener(v -> showManageMatchDialog(r, dispS1, dispS2));

        header.addView(back, new LinearLayout.LayoutParams(-2, dp(48)));
        LinearLayout.LayoutParams spinnerLp = new LinearLayout.LayoutParams(0, dp(48), 1f);
        spinnerLp.leftMargin = dp(6);
        spinnerLp.rightMargin = dp(6);
        header.addView(tabSpinner, spinnerLp);
        header.addView(manageBtn, new LinearLayout.LayoutParams(-2, dp(48)));
        root.addView(header, new LinearLayout.LayoutParams(-1, dp(50)));

        // 4-Page Swipeable Chart & Statistics Pager
        ChartPager charts = new ChartPager(this);
        charts.setPageCount(4);
        charts.setHorizontalScrollBarEnabled(false);
        LinearLayout pages = new LinearLayout(this);
        pages.setOrientation(LinearLayout.HORIZONTAL);
        charts.addView(pages);
        root.addView(charts, new LinearLayout.LayoutParams(-1, 0, 1f));

        // Interactive charts + Statistics view
        ScoreChartView scoreView = new ScoreChartView(this, r);
        scoreView.setInteractive(true);

        SetProgressChartView progressView = new SetProgressChartView(this, r);
        progressView.setInteractive(true);

        SetsChartView setsView = new SetsChartView(this, r);
        MatchStatsView statsView = new MatchStatsView(this, r);

        // Chart-only footer bar (Legend + inline Zoom / Game filter controls; hidden on Statistics tab)
        chartFooterBar = new LinearLayout(this);
        chartFooterBar.setOrientation(LinearLayout.HORIZONTAL);
        chartFooterBar.setGravity(Gravity.CENTER_VERTICAL);
        chartFooterBar.setPadding(0, dp(2), 0, 0);

        legendView = theme.createChartLegend(this, r, currentTab == TAB_SETS);
        chartFooterBar.addView(legendView, new LinearLayout.LayoutParams(0, dp(44), 1f));

        zoomControlsGroup = new LinearLayout(this);
        zoomControlsGroup.setOrientation(LinearLayout.HORIZONTAL);
        zoomControlsGroup.setGravity(Gravity.CENTER_VERTICAL);
        Button zoomIn = compactButton("Zoom +");
        Button zoomOut = compactButton("Zoom −");
        Button resetZoom = compactButton("Reset");
        zoomIn.setOnClickListener(v -> scoreView.zoomBy(1.5));
        zoomOut.setOnClickListener(v -> scoreView.zoomBy(1.0 / 1.5));
        resetZoom.setOnClickListener(v -> scoreView.resetZoom());
        zoomControlsGroup.addView(zoomIn, compactBtnMargin());
        zoomControlsGroup.addView(zoomOut, compactBtnMargin());
        zoomControlsGroup.addView(resetZoom, compactBtnMargin());
        chartFooterBar.addView(zoomControlsGroup, new LinearLayout.LayoutParams(-2, dp(48)));

        showAllGamesBtn = compactButton("Show all games");
        showAllGamesBtn.setVisibility(View.GONE);
        showAllGamesBtn.setOnClickListener(v -> progressView.showAllSets());
        progressView.setOnSetFocusChanged(number ->
            showAllGamesBtn.setVisibility(currentTab == TAB_PROGRESS && number > 0 ? View.VISIBLE : View.GONE)
        );
        chartFooterBar.addView(showAllGamesBtn, compactBtnMargin());

        root.addView(chartFooterBar, new LinearLayout.LayoutParams(-1, dp(48)));

        updatePageChrome(currentTab, r);

        tabSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onNothingSelected(AdapterView<?> parent) { }

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (updatingTabSelection || position == currentTab) return;
                currentTab = position;
                charts.scrollToPage(position, true);
                updatePageChrome(position, r);
            }
        });

        charts.setOnPageChangeListener(page -> {
            currentTab = page;
            syncSpinnerAndChrome(page, r);
        });

        charts.post(() -> {
            int w = Math.max(1, charts.getWidth());
            pages.removeAllViews();
            pages.addView(scoreView, new LinearLayout.LayoutParams(w, -1));
            pages.addView(progressView, new LinearLayout.LayoutParams(w, -1));
            pages.addView(setsView, new LinearLayout.LayoutParams(w, -1));
            pages.addView(statsView, new LinearLayout.LayoutParams(w, -1));
            if (currentTab > 0) {
                charts.scrollTo(currentTab * w, 0);
            }
            charts.setOnScrollChangeListener((v, x, y, ox, oy) -> {
                int p = Math.max(0, Math.min(3, Math.round(x / (float) Math.max(1, w))));
                if (p != currentTab) {
                    currentTab = p;
                    syncSpinnerAndChrome(p, r);
                }
            });
        });

        setContentView(root);
    }

    private void syncSpinnerAndChrome(int page, MatchRecord r) {
        updatingTabSelection = true;
        if (tabSpinner != null && tabSpinner.getSelectedItemPosition() != page) {
            tabSpinner.setSelection(page);
        }
        updatingTabSelection = false;
        updatePageChrome(page, r);
    }

    private void updatePageChrome(int selectedPage, MatchRecord r) {
        if (chartFooterBar == null) return;
        if (selectedPage == TAB_STATS) {
            chartFooterBar.setVisibility(View.GONE);
            return;
        }
        chartFooterBar.setVisibility(View.VISIBLE);
        if (zoomControlsGroup != null) {
            zoomControlsGroup.setVisibility(selectedPage == TAB_TIMELINE ? View.VISIBLE : View.GONE);
        }
        if (showAllGamesBtn != null && selectedPage != TAB_PROGRESS) {
            showAllGamesBtn.setVisibility(View.GONE);
        }
        if (legendView != null && legendView.getChildCount() >= 2) {
            String styleOne = selectedPage == TAB_SETS ? " (left bar)" : " (solid)";
            String styleTwo = selectedPage == TAB_SETS ? " (right bar)" : " (dashed)";
            ((TextView) legendView.getChildAt(0)).setText(UiTheme.playerSymbol(r.playerOneColor) + " " + r.playerOneName + styleOne);
            ((TextView) legendView.getChildAt(1)).setText(UiTheme.playerSymbol(r.playerTwoColor) + " " + r.playerTwoName + styleTwo);
        }
    }

    private void showManageMatchDialog(MatchRecord r, int dispS1, int dispS2) {
        String gameScores = r.formattedGameScores();
        String message = "Score: " + dispS1 + " : " + dispS2
            + (gameScores.isEmpty() ? "" : "  (" + gameScores + ")")
            + "\n" + MatchStatsView.formatTimingSummary(r)
            + "\n\nTip: Swipe left or right to switch between Point chart, Game chart, Game scores, and Statistics.";

        AlertDialog dialog = new AlertDialog.Builder(this)
            .setTitle(r.playerOneName + " vs " + r.playerTwoName)
            .setMessage(message)
            .setNegativeButton("Close dialog", null)
            .setNeutralButton("Delete match", (d, w) -> confirmDeleteMatch(r))
            .setPositiveButton(r.finished ? "Add 3 games on board" : "Resume match", (d, w) -> resumeOrContinueMatch(r))
            .create();

        dialog.setOnShowListener(ignored -> {
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(theme.surface));
            }
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(theme.onSurfaceVariant);
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(theme.error);
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(theme.primary);
        });
        dialog.show();
    }

    private void resumeOrContinueMatch(MatchRecord r) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra(EXTRA_RECORD, getIntent().getStringExtra(EXTRA_RECORD));
        intent.putExtra("continue", r.finished);
        startActivity(intent);
        finish();
    }

    private void confirmDeleteMatch(MatchRecord r) {
        new AlertDialog.Builder(this)
            .setTitle("Delete this match?")
            .setMessage("The app will delete the match " + r.playerOneName + " vs " + r.playerTwoName + " permanently.")
            .setNegativeButton("Cancel action", null)
            .setPositiveButton("Delete match", (d, w) -> {
                MatchHistoryStore.remove(this, r.id);
                finish();
            })
            .show();
    }

    private Button compactButton(String label) {
        Button b = theme.outlinedButton(this, label);
        b.setTextSize(13);
        b.setPadding(dp(10), 0, dp(10), 0);
        return b;
    }

    private LinearLayout.LayoutParams compactBtnMargin() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, dp(48));
        lp.leftMargin = dp(4);
        return lp;
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }
}
