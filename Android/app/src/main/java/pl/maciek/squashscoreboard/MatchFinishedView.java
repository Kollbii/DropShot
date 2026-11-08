package pl.maciek.squashscoreboard;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.Locale;

/**
 * Post-match overlay with immediate match highlights, game score breakdown,
 * Left [L] / Right [R] server scoring summary, and 1-tap navigation to statistics or charts.
 */
public final class MatchFinishedView extends LinearLayout {
    private final Runnable backAction;
    private final Runnable resetAction;
    private final Runnable continueAction;
    private final Runnable detailsAction;
    private final Runnable statsAction;

    public MatchFinishedView(
        Context c,
        String winner,
        Runnable reset,
        Runnable continueMatch,
        Runnable showDetails
    ) {
        this(c, winner, null, null, reset, continueMatch, showDetails, showDetails, null);
    }

    public MatchFinishedView(
        Context c,
        String winner,
        MatchRecord record,
        Runnable onShowBoard,
        Runnable reset,
        Runnable continueMatch,
        Runnable showDetails,
        Runnable showStats,
        Runnable onUndoPoint
    ) {
        super(c);
        UiTheme theme = new UiTheme(c);
        setOrientation(VERTICAL);
        setGravity(Gravity.CENTER);
        setPadding(dp(24), dp(12), dp(24), dp(12));
        setBackgroundColor(0xF216130C);

        backAction = () -> {
            close();
            if (onShowBoard != null) onShowBoard.run();
        };
        resetAction = () -> { close(); reset.run(); };
        continueAction = () -> { close(); continueMatch.run(); };
        detailsAction = () -> { if (showDetails != null) showDetails.run(); };
        statsAction = () -> { if (showStats != null) showStats.run(); };

        ScrollView scroll = new ScrollView(c);
        scroll.setFillViewport(true);
        LinearLayout content = new LinearLayout(c);
        content.setOrientation(VERTICAL);
        content.setGravity(Gravity.CENTER);
        scroll.addView(content);
        addView(scroll, new LayoutParams(-1, -1));

        TextView end = label("MATCH FINISHED", 30, 0xFFFFB59A, true);
        TextView won = label("WINNER: " + winner, 22, Color.WHITE, true);
        content.addView(end, new LayoutParams(-1, dp(40)));
        content.addView(won, new LayoutParams(-1, dp(34)));

        if (record != null) {
            MatchRecord.Analytics a = record.analytics();
            int[] sets = a.sets;
            int dispS1 = Math.max(record.playerOneSets, sets[0]);
            int dispS2 = Math.max(record.playerTwoSets, sets[1]);

            LinearLayout card = new LinearLayout(c);
            card.setOrientation(VERTICAL);
            card.setGravity(Gravity.CENTER);
            card.setPadding(dp(16), dp(10), dp(16), dp(10));
            card.setBackground(theme.roundedBox(0xFF221F1A, 14, 1, 0xFF4C4639, c));

            String gameScores = record.formattedGameScores();
            String scoreHeadline = record.playerOneName + "  " + dispS1 + " : " + dispS2 + "  " + record.playerTwoName
                + (gameScores.isEmpty() ? "" : "   (" + gameScores + ")");
            TextView scoreLine = label(scoreHeadline, 17, Color.WHITE, true);
            UiTheme.applyTabularNumbers(scoreLine);
            card.addView(scoreLine);

            String durStr = record.durationMs > 0 ? formatDuration(record.durationMs) : "00:00";
            String kpiText = "Duration: " + durStr
                + "   ·   Points: " + a.points[0] + ":" + a.points[1]
                + "   ·   Best streak: " + a.longestStreak[0] + ":" + a.longestStreak[1]
                + "   ·   Hold: " + pct(a.servesWon[0], a.servesTotal[0]) + "% vs " + pct(a.servesWon[1], a.servesTotal[1]) + "%";
            TextView kpiLine = label(kpiText, 14, 0xFFCEC6B6, false);
            UiTheme.applyTabularNumbers(kpiLine);
            kpiLine.setPadding(0, dp(4), 0, dp(4));
            card.addView(kpiLine);

            String sideText = "Server [L] / [R] pts:   "
                + record.playerOneName + " [L] " + a.leftServesWon[0] + "/" + a.leftServesTotal[0]
                + " · [R] " + a.rightServesWon[0] + "/" + a.rightServesTotal[0]
                + "     |     "
                + record.playerTwoName + " [L] " + a.leftServesWon[1] + "/" + a.leftServesTotal[1]
                + " · [R] " + a.rightServesWon[1] + "/" + a.rightServesTotal[1];
            TextView sideLine = label(sideText, 13, 0xFFFFDCC5, true);
            UiTheme.applyTabularNumbers(sideLine);
            card.addView(sideLine);

            LayoutParams cardParams = new LayoutParams(dp(540), -2);
            cardParams.topMargin = dp(6);
            cardParams.bottomMargin = dp(8);
            content.addView(card, cardParams);
        }

        LinearLayout container = new LinearLayout(c);
        container.setOrientation(VERTICAL);
        container.setGravity(Gravity.CENTER);

        if (showStats != null || showDetails != null) {
            LinearLayout topActions = new LinearLayout(c);
            topActions.setOrientation(HORIZONTAL);
            topActions.setGravity(Gravity.CENTER);
            if (showStats != null) {
                Button statsBtn = actionButton(
                    theme,
                    "Show match statistics",
                    theme.primary,
                    theme.onPrimary,
                    v -> triggerStats()
                );
                LayoutParams slp = new LayoutParams(0, dp(48), 1f);
                slp.rightMargin = showDetails != null ? dp(6) : 0;
                topActions.addView(statsBtn, slp);
            }
            if (showDetails != null) {
                Button chartsBtn = actionButton(
                    theme,
                    "Show match charts",
                    theme.secondaryContainer,
                    theme.onSecondaryContainer,
                    v -> triggerDetails()
                );
                LayoutParams clp = new LayoutParams(0, dp(48), 1f);
                clp.leftMargin = showStats != null ? dp(6) : 0;
                topActions.addView(chartsBtn, clp);
            }
            container.addView(topActions, buttonParams(0));
        }

        container.addView(
            actionButton(theme, "Start new match  [Play / Pause]", theme.playerPanelFill(AppPreferences.BLUE), Color.WHITE, v -> triggerReset()),
            buttonParams(8)
        );

        LinearLayout secondaryActions = new LinearLayout(c);
        secondaryActions.setOrientation(HORIZONTAL);
        secondaryActions.setGravity(Gravity.CENTER);

        Button boardBtn = actionButton(theme, "Show match board  [Prev]", theme.surfaceHigh, Color.WHITE, v -> triggerBack());
        LayoutParams blp = new LayoutParams(0, dp(48), 1f);
        blp.rightMargin = dp(6);
        secondaryActions.addView(boardBtn, blp);

        Button addGamesBtn = actionButton(theme, "Add 3 games  [Next]", theme.playerPanelFill(AppPreferences.GREEN), Color.WHITE, v -> triggerContinue());
        LayoutParams alp = new LayoutParams(0, dp(48), 1f);
        alp.leftMargin = dp(6);
        secondaryActions.addView(addGamesBtn, alp);

        container.addView(secondaryActions, buttonParams(8));

        if (onUndoPoint != null) {
            container.addView(
                actionButton(theme, "Undo last point", 0xFF2C2A24, 0xFFCEC6B6, v -> onUndoPoint.run()),
                buttonParams(8)
            );
        }

        LayoutParams boxParams = new LayoutParams(dp(540), -2);
        content.addView(container, boxParams);
    }

    private LayoutParams buttonParams(int topDp) {
        LayoutParams p = new LayoutParams(-1, dp(48));
        p.topMargin = dp(topDp);
        return p;
    }

    private Button actionButton(UiTheme theme, String text, int fill, int textColor, OnClickListener listener) {
        Button b = new Button(getContext());
        b.setText(text);
        b.setAllCaps(false);
        b.setTextSize(16);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setTextColor(textColor);
        b.setMinHeight(dp(48));
        b.setMinimumHeight(dp(48));
        b.setBackground(theme.interactiveBackground(getContext(), fill, 24, 1, theme.outlineVariant));
        b.setFilterTouchesWhenObscured(true);
        b.setOnClickListener(listener);
        return b;
    }

    private TextView label(String text, int size, int color, boolean bold) {
        TextView v = new TextView(getContext());
        v.setText(text);
        v.setTextSize(size);
        v.setTextColor(color);
        if (bold) v.setTypeface(Typeface.DEFAULT_BOLD);
        v.setGravity(Gravity.CENTER);
        return v;
    }

    private static int pct(int won, int total) {
        return total <= 0 ? 0 : Math.round(100f * won / total);
    }

    private static String formatDuration(long ms) {
        long s = Math.max(0, ms) / 1000;
        return s >= 3600
            ? String.format(Locale.US, "%d:%02d:%02d", s / 3600, (s / 60) % 60, s % 60)
            : String.format(Locale.US, "%02d:%02d", s / 60, s % 60);
    }

    private void close() {
        if (getParent() instanceof ViewGroup) {
            ((ViewGroup) getParent()).removeView(this);
        }
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }

    public void triggerBack() { backAction.run(); }
    public void triggerReset() { resetAction.run(); }
    public void triggerContinue() { continueAction.run(); }
    public void triggerDetails() { detailsAction.run(); }
    public void triggerStats() { statsAction.run(); }
}
