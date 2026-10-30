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

        addStatCard(
            content,
            cloudLinked
                ? "Cloud sync: Linked (automatic sync after each game and match)"
                : "Cloud sync: Local only (tap Link cloud to enable Supabase sync)"
        );
        addStatCard(content, "Matches: " + s.matches + "   ·   Wins: " + s.wins + " (Win rate: " + pct(s.wins, s.matches) + "%)");
        addStatCard(content, "Points won: " + s.points + "   ·   Average points per match: " + avg(s.points, s.matches));
        addStatCard(content, "Average points per game: " + avg(s.points, s.totalSets) + "   ·   Average games won per match: " + avg(s.wonSets, s.matches));
        addStatCard(content, "Points to 6-7 or 7-6: " + s.score67 + "   ·   Points to 6-9 or 9-6: " + s.score69);

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
