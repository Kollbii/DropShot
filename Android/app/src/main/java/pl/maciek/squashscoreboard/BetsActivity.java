package pl.maciek.squashscoreboard;

import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Shows initial credits and empty bet history for one player profile. */
public final class BetsActivity extends Activity {
    public static final String EXTRA_PROFILE_ID = "profile";
    // TEMPORARY: Replace this value when bets can change credits.
    private static final int INITIAL_CREDITS = 1000;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        draw();
    }

    @Override public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        draw();
    }

    private void draw() {
        String profileId = InputValidator.sanitizeProfileIdOrNull(
            getIntent() == null ? null : getIntent().getStringExtra(EXTRA_PROFILE_ID));
        PlayerProfile profile = ProfileStore.find(this, profileId);
        if (profile == null) {
            finish();
            return;
        }

        UiTheme theme = new UiTheme(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(12), dp(16), dp(12));
        root.setBackgroundColor(theme.background);

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        Button back = theme.outlinedButton(this, getString(R.string.bets_back));
        back.setOnClickListener(view -> finish());
        header.addView(back, new LinearLayout.LayoutParams(-2, dp(48)));
        TextView title = text(getString(R.string.bets_player_title, profile.name), 24, theme.onSurface);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setSingleLine(true);
        title.setEllipsize(TextUtils.TruncateAt.END);
        title.setPadding(dp(16), 0, 0, 0);
        header.addView(title, new LinearLayout.LayoutParams(0, dp(48), 1));
        root.addView(header, new LinearLayout.LayoutParams(-1, dp(52)));

        LinearLayout creditsCard = new LinearLayout(this);
        creditsCard.setGravity(Gravity.CENTER_VERTICAL);
        creditsCard.setPadding(dp(24), dp(12), dp(24), dp(12));
        creditsCard.setBackground(theme.roundedBox(theme.surfaceLow, 16, 1, theme.outlineVariant, this));
        creditsCard.setFocusable(true);
        creditsCard.setContentDescription(getString(R.string.bets_credits_description, INITIAL_CREDITS));
        TextView creditsLabel = text(getString(R.string.bets_credits), 22, theme.onSurfaceVariant);
        creditsLabel.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        creditsCard.addView(creditsLabel, new LinearLayout.LayoutParams(-2, -2));
        TextView credits = text(String.valueOf(INITIAL_CREDITS), 40, theme.onSurface);
        credits.setTypeface(Typeface.DEFAULT_BOLD);
        credits.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        UiTheme.applyTabularNumbers(credits);
        credits.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        creditsCard.addView(credits, new LinearLayout.LayoutParams(0, -1, 1));
        root.addView(creditsCard, spaced(-1, dp(96), 12));

        LinearLayout historyCard = new LinearLayout(this);
        historyCard.setOrientation(LinearLayout.VERTICAL);
        historyCard.setPadding(dp(24), dp(16), dp(24), dp(16));
        historyCard.setBackground(theme.roundedBox(theme.surfaceLow, 16, 1, theme.outlineVariant, this));
        TextView historyTitle = text(getString(R.string.bets_history), 22, theme.onSurface);
        historyTitle.setTypeface(Typeface.DEFAULT_BOLD);
        historyCard.addView(historyTitle, new LinearLayout.LayoutParams(-1, dp(40)));
        LinearLayout empty = new LinearLayout(this);
        empty.setOrientation(LinearLayout.VERTICAL);
        empty.setGravity(Gravity.CENTER);
        TextView emptyTitle = text(getString(R.string.bets_empty_title), 20, theme.onSurface);
        emptyTitle.setGravity(Gravity.CENTER);
        TextView emptyDescription = text(getString(R.string.bets_empty_description), 16,
            theme.onSurfaceVariant);
        emptyDescription.setGravity(Gravity.CENTER);
        empty.addView(emptyTitle);
        empty.addView(emptyDescription);
        historyCard.addView(empty, new LinearLayout.LayoutParams(-1, 0, 1));
        root.addView(historyCard, spaced(-1, 0, 12, 1));

        setContentView(root);
    }

    private TextView text(String value, int size, int color) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setGravity(Gravity.CENTER_VERTICAL);
        return view;
    }

    private LinearLayout.LayoutParams spaced(int width, int height, int top) {
        return spaced(width, height, top, 0);
    }

    private LinearLayout.LayoutParams spaced(int width, int height, int top, float weight) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(width, height, weight);
        params.topMargin = dp(top);
        return params;
    }

    private int dp(int value) { return UiTheme.dp(this, value); }
}
