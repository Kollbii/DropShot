package pl.maciek.squashscoreboard;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public final class MatchFinishedView extends LinearLayout {
    private final Runnable backAction;
    private final Runnable resetAction;
    private final Runnable continueAction;
    private final Runnable detailsAction;

    public MatchFinishedView(
        Context c,
        String winner,
        Runnable reset,
        Runnable continueMatch,
        Runnable showDetails
    ) {
        super(c);
        UiTheme theme = new UiTheme(c);
        setOrientation(VERTICAL);
        setGravity(Gravity.CENTER);
        setPadding(dp(32), dp(16), dp(32), dp(16));
        setBackgroundColor(0xF016130C);

        TextView end = label("MATCH FINISHED", 36, 0xFFFFB59A, true);
        TextView won = label("WINNER: " + winner, 24, Color.WHITE, true);

        backAction = this::close;
        resetAction = () -> { close(); reset.run(); };
        continueAction = () -> { close(); continueMatch.run(); };
        detailsAction = () -> { if (showDetails != null) showDetails.run(); };

        addView(end, new LayoutParams(-1, dp(52)));
        addView(won, new LayoutParams(-1, dp(42)));

        LinearLayout container = new LinearLayout(c);
        container.setOrientation(VERTICAL);
        container.setGravity(Gravity.CENTER);

        container.addView(
            actionButton(theme, "Show match board  [Previous track]", theme.surfaceHigh, Color.WHITE, v -> triggerBack()),
            buttonParams(0)
        );
        container.addView(
            actionButton(theme, "Start new match  [Play / Pause]", theme.primary, theme.onPrimary, v -> triggerReset()),
            buttonParams(8)
        );
        container.addView(
            actionButton(theme, "Add 3 games  [Next track]", theme.playerPanelFill(AppPreferences.GREEN), Color.WHITE, v -> triggerContinue()),
            buttonParams(8)
        );
        if (showDetails != null) {
            container.addView(
                actionButton(theme, "Show match details", theme.secondaryContainer, theme.onSecondaryContainer, v -> triggerDetails()),
                buttonParams(8)
            );
        }

        LayoutParams boxParams = new LayoutParams(dp(440), -2);
        addView(container, boxParams);
    }

    private LayoutParams buttonParams(int topDp) {
        LayoutParams p = new LayoutParams(-1, dp(50));
        p.topMargin = dp(topDp);
        return p;
    }

    private Button actionButton(UiTheme theme, String text, int fill, int textColor, OnClickListener listener) {
        Button b = new Button(getContext());
        b.setText(text);
        b.setAllCaps(false);
        b.setTextSize(17);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setTextColor(textColor);
        b.setMinHeight(dp(48));
        b.setMinimumHeight(dp(48));
        b.setBackground(theme.interactiveBackground(getContext(), fill, 24, 1, theme.outlineVariant));
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
}
