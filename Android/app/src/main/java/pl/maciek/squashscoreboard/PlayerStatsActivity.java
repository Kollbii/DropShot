package pl.maciek.squashscoreboard;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import java.util.List;
import java.util.Locale;
import pl.maciek.squashscoreboard.lootbox.LootboxActivity;

public class PlayerStatsActivity extends Activity {
    private UiTheme theme;
    private UiTheme.CollapsibleBottomNav bottomNav;

    @Override public void onUserInteraction() {
        super.onUserInteraction();
        if (bottomNav != null) bottomNav.onUserInteraction();
    }

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
        theme = new UiTheme(this);
        List<PlayerProfile> profiles = ProfileStore.load(this);

        LinearLayout root = box(theme.background);
        root.setPadding(dp(16), dp(12), dp(16), dp(8));

        // Top App Bar
        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);

        Button back = theme.outlinedButton(this, "← Go back");
        back.setContentDescription("Go back.");
        back.setOnClickListener(v -> finish());

        TextView title = text("Players and statistics", 22, theme.onSurface);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setPadding(dp(16), 0, dp(16), 0);

        Button compare = theme.tonalButton(this, "Compare players");
        compare.setOnClickListener(v -> chooseComparison(profiles));

        Button add = theme.primaryButton(this, "Add player");
        add.setOnClickListener(v -> addProfile());

        header.addView(back, new LinearLayout.LayoutParams(-2, dp(48)));
        header.addView(title, new LinearLayout.LayoutParams(0, dp(48), 1f));
        header.addView(compare, btnMargin());
        header.addView(add, btnMargin());
        root.addView(header, new LinearLayout.LayoutParams(-1, dp(52)));

        ScrollView scroll = new ScrollView(this);
        scroll.setPadding(0, dp(8), 0, dp(8));
        LinearLayout list = box(theme.background);

        if (profiles.isEmpty()) {
            LinearLayout emptyBox = box(theme.background);
            emptyBox.setGravity(Gravity.CENTER);
            emptyBox.setPadding(dp(24), dp(36), dp(24), dp(36));

            TextView emptyTitle = text("No saved player profiles", 22, theme.onSurface);
            emptyTitle.setTypeface(Typeface.DEFAULT_BOLD);
            emptyTitle.setGravity(Gravity.CENTER);

            TextView emptyDesc = text(
                "Add a player profile to track career statistics and compare players.",
                16,
                theme.onSurfaceVariant
            );
            emptyDesc.setGravity(Gravity.CENTER);
            emptyDesc.setPadding(0, dp(8), 0, dp(16));

            Button emptyAdd = theme.primaryButton(this, "Add player");
            emptyAdd.setOnClickListener(v -> addProfile());

            emptyBox.addView(emptyTitle);
            emptyBox.addView(emptyDesc);
            emptyBox.addView(emptyAdd, new LinearLayout.LayoutParams(-2, dp(48)));
            list.addView(emptyBox, new LinearLayout.LayoutParams(-1, -2));
        } else {
            for (PlayerProfile p : profiles) {
                list.addView(profileRow(p), rowParams());
            }
        }

        Button lootbox = theme.outlinedButton(this, "Open lootbox prototype");
        lootbox.setContentDescription("Open the lootbox prototype from player profiles");
        lootbox.setOnClickListener(v -> startActivity(new Intent(this, LootboxActivity.class)));
        LinearLayout.LayoutParams lootboxParams = new LinearLayout.LayoutParams(-1, dp(48));
        lootboxParams.topMargin = dp(16);
        list.addView(lootbox, lootboxParams);

        scroll.addView(list);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));

        bottomNav = theme.createCollapsibleBottomNav(this, UiTheme.TAB_PLAYERS, null);
        root.addView(bottomNav, new LinearLayout.LayoutParams(-1, dp(56)));
        setContentView(root);
    }

    private LinearLayout.LayoutParams btnMargin() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, dp(48));
        lp.leftMargin = dp(8);
        return lp;
    }

    private View profileRow(PlayerProfile p) {
        PlayerStatistics.Summary s = PlayerStatistics.calculate(this, p);
        int accent = theme.playerAccentOnSurface(p.color);
        int rate = s.matches == 0 ? 0 : Math.round(s.wins * 100f / s.matches);
        String avgPts = s.matches == 0 ? "0.0" : String.format(Locale.US, "%.1f", s.points / (float) s.matches);

        LinearLayout row = box(theme.surfaceLow);
        row.setPadding(dp(18), dp(12), dp(18), dp(12));
        row.setMinimumHeight(dp(92));
        row.setBackground(theme.interactiveBackground(this, theme.surfaceLow, 14, 2, accent));

        String headerText = p.name + "  (" + UiTheme.playerColorName(p.color) + ")";
        TextView name = text(headerText, 20, accent);
        name.setTypeface(Typeface.DEFAULT_BOLD);

        TextView basic = text(
            "Matches: " + s.matches + "   ·   Wins: " + s.wins + " (" + rate + "%)"
                + "   ·   Points: " + s.points + "   ·   Avg pts/match: " + avgPts,
            16,
            theme.onSurface
        );
        UiTheme.applyTabularNumbers(basic);

        TextView special = text(
            "Points to 6-7 or 7-6: " + s.score67 + "   ·   Points to 6-9 or 9-6: " + s.score69,
            16,
            theme.onSurfaceVariant
        );
        UiTheme.applyTabularNumbers(special);

        row.addView(name);
        row.addView(basic);
        row.addView(special);
        row.setContentDescription(
            p.name + ", Color " + UiTheme.playerColorName(p.color)
                + ". " + s.matches + " matches, " + s.wins + " wins, win rate " + rate + " percent. Double-tap to open player profile."
        );
        row.setOnClickListener(v ->
            startActivity(new Intent(this, PlayerDetailActivity.class).putExtra("profile", p.id))
        );
        return row;
    }

    private void chooseComparison(List<PlayerProfile> profiles) {
        if (profiles.size() < 2) {
            new AlertDialog.Builder(this)
                .setTitle("Cannot compare players")
                .setMessage("The app cannot compare players yet. A comparison requires at least 2 player profiles. Tap Add player to create another profile.")
                .setNegativeButton("Cancel action", null)
                .setPositiveButton("Add player", (d, w) -> addProfile())
                .show();
            return;
        }
        // UX simplification: if exactly 2 profiles exist, open the comparison immediately in 1 tap!
        if (profiles.size() == 2) {
            startActivity(
                new Intent(this, PlayerComparisonActivity.class)
                    .putExtra("one", profiles.get(0).id)
                    .putExtra("two", profiles.get(1).id)
            );
            return;
        }
        String[] names = new String[profiles.size()];
        for (int i = 0; i < profiles.size(); i++) names[i] = profiles.get(i).name;

        LinearLayout form = box(theme.surface);
        form.setPadding(dp(20), dp(12), dp(20), dp(12));
        Spinner one = theme.styledSpinner(this, names, 0);
        Spinner two = theme.styledSpinner(this, names, 1);

        form.addView(label("Profile for Player 1"));
        form.addView(one, new LinearLayout.LayoutParams(-1, dp(48)));
        form.addView(label("Profile for Player 2"));
        form.addView(two, new LinearLayout.LayoutParams(-1, dp(48)));

        AlertDialog dialog = new AlertDialog.Builder(this)
            .setTitle("Compare 2 players")
            .setView(form)
            .setNegativeButton("Cancel action", null)
            .setPositiveButton("Show comparison", null)
            .create();

        dialog.setOnShowListener(ignored -> {
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(theme.surface));
            }
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(theme.primary);
            Button showBtn = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            showBtn.setTextColor(theme.primary);
            showBtn.setOnClickListener(v -> {
                if (one.getSelectedItemPosition() == two.getSelectedItemPosition()) {
                    new AlertDialog.Builder(this)
                        .setTitle("Same player selected")
                        .setMessage("The selected profiles are the same. You must select 2 different player profiles. Select a different profile for Player 2.")
                        .setPositiveButton("Close dialog", null)
                        .show();
                    return;
                }
                dialog.dismiss();
                startActivity(
                    new Intent(this, PlayerComparisonActivity.class)
                        .putExtra("one", profiles.get(one.getSelectedItemPosition()).id)
                        .putExtra("two", profiles.get(two.getSelectedItemPosition()).id)
                );
            });
        });
        dialog.show();
    }

    private void addProfile() {
        LinearLayout form = box(theme.surface);
        form.setPadding(dp(20), dp(12), dp(20), dp(12));
        EditText name = theme.editField(this, "", "Player name", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        Spinner color = theme.styledSpinner(this, UiTheme.playerColorNames(), 0);

        form.addView(label("Player name"));
        form.addView(name, new LinearLayout.LayoutParams(-1, dp(48)));
        form.addView(label("Player color"));
        form.addView(color, new LinearLayout.LayoutParams(-1, dp(48)));

        AlertDialog dialog = new AlertDialog.Builder(this)
            .setTitle("Add player")
            .setView(form)
            .setNegativeButton("Cancel action", null)
            .setPositiveButton("Save player", null)
            .create();

        dialog.setOnShowListener(ignored -> {
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(theme.surface));
            }
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(theme.primary);
            Button saveBtn = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            saveBtn.setTextColor(theme.primary);
            saveBtn.setOnClickListener(v -> {
                String n = name.getText().toString().trim();
                if (n.isEmpty()) {
                    new AlertDialog.Builder(this)
                        .setTitle("Empty player name")
                        .setMessage("The player name is empty. Each player profile must have a name. Enter a name in the text field.")
                        .setPositiveButton("Close dialog", null)
                        .show();
                    return;
                }
                if (ProfileStore.findByName(this, n) != null) {
                    new AlertDialog.Builder(this)
                        .setTitle("Duplicate player name")
                        .setMessage("This player name already exists. Each player profile must have a unique name. Enter a different name.")
                        .setPositiveButton("Close dialog", null)
                        .show();
                    return;
                }
                ProfileStore.add(this, new PlayerProfile(n, UiTheme.playerColors()[color.getSelectedItemPosition()]));
                dialog.dismiss();
                draw();
            });
        });
        dialog.show();
    }

    private TextView label(String s) {
        TextView t = text(s, 16, theme.onSurfaceVariant);
        t.setPadding(0, dp(8), 0, dp(2));
        return t;
    }

    private LinearLayout box(int color) {
        LinearLayout x = new LinearLayout(this);
        x.setOrientation(LinearLayout.VERTICAL);
        x.setBackgroundColor(color);
        return x;
    }

    private TextView text(String s, int z, int c) {
        TextView v = new TextView(this);
        v.setText(s);
        v.setTextSize(z);
        v.setTextColor(c);
        v.setGravity(Gravity.CENTER_VERTICAL);
        return v;
    }

    private LinearLayout.LayoutParams rowParams() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.setMargins(0, dp(5), 0, dp(5));
        return p;
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }
}
