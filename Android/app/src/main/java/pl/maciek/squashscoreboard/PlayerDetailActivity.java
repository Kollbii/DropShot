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
import android.widget.Button;
import android.widget.EditText;
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
        boolean cloudLinked = CloudConfigStore.isCloudLinked(this, profile.id);

        LinearLayout root = box(theme.background);
        root.setPadding(dp(16), dp(12), dp(16), dp(12));

        // Top Bar
        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);

        Button back = theme.outlinedButton(this, "← Go back");
        back.setContentDescription("Go back.");
        back.setOnClickListener(v -> finish());

        TextView title = t(profile.name, 24, accent);
        RewardFonts.apply(title, profile.fontId, 24f);
        title.setPadding(dp(16), 0, dp(16), 0);

        Button cloudBtn = theme.outlinedButton(this, cloudLinked ? "Sync cloud" : "Link cloud");
        cloudBtn.setOnClickListener(v -> showCloudLinkDialog());

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
        header.addView(cloudBtn, btnMargin());
        header.addView(edit, btnMargin());
        if (!RewardInventory.isDeveloper(this, profile.id)) header.addView(del, btnMargin());
        root.addView(header, new LinearLayout.LayoutParams(-1, dp(52)));

        ScrollView scroll = new ScrollView(this);
        LinearLayout content = box(theme.background);
        content.setPadding(0, dp(12), 0, dp(12));

        // Career Squash Court [L] / [R] Server Scoring & Hold Diagram
        addSectionHeader(content, "COURT [L] AND [R] SERVE HIGHLIGHTS");
        CourtSideDiagramView courtDiagram = new CourtSideDiagramView(this, profile, s);
        LinearLayout.LayoutParams courtLp = new LinearLayout.LayoutParams(-1, -2);
        courtLp.bottomMargin = dp(8);
        content.addView(courtDiagram, courtLp);

        Button matchesBtn = theme.tonalButton(this, "Show matches for " + profile.name);
        matchesBtn.setOnClickListener(v -> {
            startActivity(
                new Intent(this, HistoryActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    .putExtra(HistoryActivity.EXTRA_ACTIVE_TAB, UiTheme.TAB_HISTORY)
                    .putExtra(HistoryActivity.EXTRA_SEARCH_QUERY, profile.name)
            );
            finish();
        });
        LinearLayout.LayoutParams mLp = new LinearLayout.LayoutParams(-1, dp(48));
        mLp.bottomMargin = dp(10);
        content.addView(matchesBtn, mLp);

        addSectionHeader(content, "CAREER WIN & SERVE EFFICIENCY");
        addMetricBar(
            content,
            "WIN RATE",
            pct(s.wins, s.matches) + "%",
            s.wins + "/" + s.matches + " wins",
            ratio(s.wins, s.matches)
        );
        addMetricBar(
            content,
            "BAYESIAN WIN PROB (LAPLACE)",
            String.format(Locale.US, "%.1f%%", 100.0 * s.laplaceWinProbability()),
            String.format(Locale.US, "Wilson 95%%: %.1f%%", 100.0 * s.wilsonLowerBound95()),
            s.laplaceWinProbability()
        );
        addMetricBar(
            content,
            "SERVE HOLD RATE",
            pct(s.servesWon, s.servesTotal) + "%",
            s.servesWon + "/" + s.servesTotal + " serves",
            s.serveHoldRate()
        );
        addMetricBar(
            content,
            "SIDE-OUT RATE",
            pct(s.receivesWon, s.receivesTotal) + "%",
            s.receivesWon + "/" + s.receivesTotal + " returns",
            s.sideOutRate()
        );
        addMetricBar(
            content,
            "MARKOV SERVE SHARE",
            String.format(Locale.US, "%.1f%%", 100.0 * s.markovServeShare()),
            String.format(Locale.US, "Rally win: %.1f%%", 100.0 * s.expectedRallyWinRate()),
            s.markovServeShare()
        );

        addSectionHeader(content, "COURT [L] AND [R] BOX BREAKDOWN");
        addMetricBar(
            content,
            "LEFT [L] SERVE HOLD",
            pct(s.leftServesWon, s.leftServesTotal) + "%",
            s.leftServesWon + "/" + s.leftServesTotal + " pts · Pick " + s.leftPicks,
            ratio(s.leftServesWon, s.leftServesTotal)
        );
        addMetricBar(
            content,
            "RIGHT [R] SERVE HOLD",
            pct(s.rightServesWon, s.rightServesTotal) + "%",
            s.rightServesWon + "/" + s.rightServesTotal + " pts · Pick " + s.rightPicks,
            ratio(s.rightServesWon, s.rightServesTotal)
        );
        addMetricBar(
            content,
            "CHOSEN BOX (PICK) HOLD",
            pct(s.pickedServesWon, s.pickedServesTotal) + "%",
            s.pickedServesWon + "/" + s.pickedServesTotal + " pts",
            ratio(s.pickedServesWon, s.pickedServesTotal)
        );
        addMetricBar(
            content,
            "STREAK BOX (ALT) HOLD",
            pct(s.altServesWon, s.altServesTotal) + "%",
            s.altServesWon + "/" + s.altServesTotal + " pts",
            ratio(s.altServesWon, s.altServesTotal)
        );

        addSectionHeader(content, "VOLUME, CLUTCH & CLOUD STATUS");
        addMetricBar(
            content,
            "GAMES WON",
            pct(s.wonSets, s.totalSets) + "%",
            s.wonSets + "/" + s.totalSets + " games (" + avg(s.wonSets, s.matches) + "/match)",
            ratio(s.wonSets, s.totalSets)
        );
        addMetricBar(
            content,
            "POINTS WON",
            s.points + " pts",
            avg(s.points, s.matches) + "/match · " + avg(s.points, s.totalSets) + "/game",
            -1.0
        );
        addMetricBar(
            content,
            "CLUTCH 6-7 & 6-9 POINTS",
            "6-7: " + s.score67,
            "6-9: " + s.score69,
            -1.0
        );
        addMetricBar(
            content,
            "CLOUD SYNC",
            cloudLinked ? "Linked" : "Local only",
            cloudLinked ? "Auto-sync active" : "Tap Link cloud",
            cloudLinked ? 1.0 : 0.0
        );

        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));
        setContentView(root);
    }

    private void showCloudLinkDialog() {
        boolean cloudLinked = CloudConfigStore.isCloudLinked(this, profile.id);
        boolean hasServerConfig = CloudConfigStore.isServerConfigured(this);

        LinearLayout form = box(theme.surface);
        form.setPadding(dp(20), dp(12), dp(20), dp(12));

        EditText urlField = theme.editField(
            this,
            CloudConfigStore.getProjectUrl(this),
            "https://your-project.supabase.co",
            InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI
        );
        EditText keyField = theme.editField(
            this,
            CloudConfigStore.getAnonKey(this),
            "Supabase public key",
            InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD
        );
        EditText tokenField = theme.editField(
            this,
            "",
            cloudLinked ? "Leave empty to keep current token, or enter new token" : "Player secret token",
            InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD
        );

        if (!hasServerConfig) {
            form.addView(t("Supabase project URL", 16, theme.onSurfaceVariant));
            form.addView(urlField, new LinearLayout.LayoutParams(-1, dp(48)));
            form.addView(t("Supabase public key", 16, theme.onSurfaceVariant));
            form.addView(keyField, new LinearLayout.LayoutParams(-1, dp(48)));
        }
        form.addView(t("Player secret token", 16, theme.onSurfaceVariant));
        form.addView(tokenField, new LinearLayout.LayoutParams(-1, dp(48)));

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
            .setTitle(cloudLinked ? "Sync or update cloud token" : "Link player to cloud")
            .setView(form)
            .setNegativeButton("Cancel action", null)
            .setPositiveButton("Upload to cloud", null);

        if (cloudLinked) {
            builder.setNeutralButton("Unlink cloud", (d, w) -> {
                CloudConfigStore.unlinkProfile(this, profile.id);
                draw();
            });
        }

        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(ignored -> {
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(theme.surface));
            }
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(theme.primary);
            if (dialog.getButton(AlertDialog.BUTTON_NEUTRAL) != null) {
                dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(theme.error);
            }
            Button pushBtn = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            pushBtn.setTextColor(theme.primary);
            pushBtn.setOnClickListener(v -> {
                if (!hasServerConfig) {
                    String rawUrl = urlField.getText().toString().trim();
                    String rawKey = keyField.getText().toString().trim();
                    if (!CloudConfigStore.isValidHttpsEndpoint(rawUrl) || rawKey.isEmpty()) {
                        new AlertDialog.Builder(this)
                            .setTitle("Invalid cloud settings")
                            .setMessage("The Supabase URL or public key is not valid. The URL must start with https:// and the key cannot be empty. Enter both values.")
                            .setPositiveButton("Close dialog", null)
                            .show();
                        return;
                    }
                    CloudConfigStore.saveServerConfig(this, rawUrl, rawKey);
                }
                String rawToken = tokenField.getText().toString().trim();
                String existingHash = CloudConfigStore.getTokenHash(this, profile.id);
                if (rawToken.isEmpty() && !CloudConfigStore.isValidTokenHash(existingHash)) {
                    new AlertDialog.Builder(this)
                        .setTitle("Empty player token")
                        .setMessage("The player secret token is empty. Enter a secret token to link this player to Supabase.")
                        .setPositiveButton("Close dialog", null)
                        .show();
                    return;
                }
                String tokenToUse = rawToken.isEmpty() ? existingHash : rawToken;
                pushBtn.setEnabled(false);
                pushBtn.setText("Uploading...");
                SupabaseSyncClient.registerOrPushPlayerAsync(this, profile, tokenToUse, new SupabaseSyncClient.Callback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        if (isFinishing() || isDestroyed()) return;
                        dialog.dismiss();
                        draw();
                    }

                    @Override
                    public void onError(String userMessage) {
                        if (isFinishing() || isDestroyed()) return;
                        pushBtn.setEnabled(true);
                        pushBtn.setText("Upload to cloud");
                        new AlertDialog.Builder(PlayerDetailActivity.this)
                            .setTitle("Cannot upload player")
                            .setMessage(userMessage)
                            .setPositiveButton("Close dialog", null)
                            .show();
                    }
                });
            });
        });
        dialog.show();
    }

    private LinearLayout.LayoutParams btnMargin() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, dp(48));
        lp.leftMargin = dp(8);
        return lp;
    }

    private void addSectionHeader(LinearLayout parent, String titleText) {
        TextView hdr = new TextView(this);
        hdr.setText(titleText);
        hdr.setTextSize(14);
        hdr.setTextColor(theme.primary);
        hdr.setTypeface(Typeface.DEFAULT_BOLD);
        hdr.setGravity(Gravity.CENTER_VERTICAL);
        hdr.setPadding(dp(4), dp(8), dp(4), dp(4));
        parent.addView(hdr, new LinearLayout.LayoutParams(-1, -2));
    }

    private void addMetricBar(LinearLayout parent, String label, String primaryValue, String secondaryDetail, double fillRatio) {
        StatComparisonRowView row = StatComparisonRowView.createSinglePlayerMetric(
            this,
            theme,
            label,
            primaryValue,
            secondaryDetail,
            profile.color,
            fillRatio
        );
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = dp(4);
        parent.addView(row, lp);
    }

    private static double ratio(int a, int b) {
        return b <= 0 ? 0.0 : (double) a / b;
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
