package pl.maciek.squashscoreboard;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.Locale;
import pl.maciek.squashscoreboard.lootbox.RewardInventory;
import pl.maciek.squashscoreboard.lootbox.RewardFonts;

public class PlayerDetailActivity extends Activity {
    private PlayerProfile profile;
    private UiTheme theme;

    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);
        draw();
    }

    @Override
    protected void onResume() {
        super.onResume();
        draw();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        draw();
    }

    private void draw() {
        profile = ProfileStore.find(this, getIntent().getStringExtra("profile"));
        if (profile == null) {
            finish();
            return;
        }
        theme = new UiTheme(this);
        PlayerStatistics.Summary s = PlayerStatistics.calculate(this, profile);
        int accent = theme.playerAccentOnSurface(profile.color);

        LinearLayout root = box(theme.background);
        root.setPadding(dp(16), dp(12), dp(16), dp(12));

        // Top Bar
        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);

        Button back = theme.outlinedButton(this, "← Go back");
        back.setContentDescription("Go back.");
        back.setOnClickListener(v -> finish());

        TextView title = t(profile.name, 24, accent);
        title.setTypeface(RewardFonts.forId(this, profile.fontId));
        title.setPadding(dp(16), 0, dp(16), 0);

        Button edit = theme.tonalButton(this, "Edit player");
        edit.setOnClickListener(v -> startActivity(new Intent(this, PlayerCustomizationActivity.class)
            .putExtra("profile", profile.id)));

        Button del = theme.dangerButton(this, "Delete player");
        del.setOnClickListener(v ->
            new AlertDialog.Builder(this)
                .setTitle("Delete this player profile?")
                .setMessage("The match history will stay on the device, but " + profile.name + " will not appear in the player list.")
                .setNegativeButton("Cancel action", null)
                .setPositiveButton("Delete player", (x, y) -> {
                    ProfileStore.remove(this, profile.id);
                    finish();
                })
                .show()
        );

        header.addView(back, new LinearLayout.LayoutParams(-2, dp(48)));
        header.addView(title, new LinearLayout.LayoutParams(0, dp(48), 1f));
        header.addView(edit, btnMargin());
        if (!RewardInventory.isDeveloper(this, profile.id)) header.addView(del, btnMargin());
        root.addView(header, new LinearLayout.LayoutParams(-1, dp(52)));

        ScrollView scroll = new ScrollView(this);
        LinearLayout content = box(theme.background);
        content.setPadding(0, dp(12), 0, dp(12));

        addStatCard(content, "Matches: " + s.matches + "   ·   Wins: " + s.wins + " (Win rate: " + pct(s.wins, s.matches) + "%)");
        addStatCard(content, "Points won: " + s.points + "   ·   Average points per match: " + avg(s.points, s.matches));
        addStatCard(content, "Average points per game: " + avg(s.points, s.totalSets) + "   ·   Average games won per match: " + avg(s.wonSets, s.matches));
        addStatCard(content, "Points to 6-7 or 7-6: " + s.score67 + "   ·   Points to 6-9 or 9-6: " + s.score69);

        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));
        setContentView(root);
    }

    private LinearLayout.LayoutParams btnMargin() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, dp(48));
        lp.leftMargin = dp(8);
        return lp;
    }

    private void addStatCard(LinearLayout parent, String textValue) {
        TextView card = t(textValue, 18, theme.onSurface);
        UiTheme.applyTabularNumbers(card);
        card.setPadding(dp(18), dp(14), dp(18), dp(14));
        card.setBackground(theme.roundedBox(theme.surfaceLow, 12, 1, theme.outlineVariant, this));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, dp(4), 0, dp(4));
        parent.addView(card, lp);
    }

    private LinearLayout box(int c) {
        LinearLayout x = new LinearLayout(this);
        x.setOrientation(LinearLayout.VERTICAL);
        x.setBackgroundColor(c);
        return x;
    }

    private TextView t(String a, int b, int c) {
        TextView v = new TextView(this);
        v.setText(a);
        v.setTextSize(b);
        v.setTextColor(c);
        v.setGravity(Gravity.CENTER_VERTICAL);
        v.setPadding(0, dp(6), 0, dp(6));
        return v;
    }

    private String avg(int a, int b) {
        return b == 0 ? "0.0" : String.format(Locale.US, "%.1f", a / (float) b);
    }

    private String pct(int a, int b) {
        return b == 0 ? "0" : String.valueOf(Math.round(a * 100f / b));
    }

    private int dp(int x) {
        return (int) (x * getResources().getDisplayMetrics().density + 0.5f);
    }
}
