package pl.maciek.squashscoreboard;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
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
import java.util.Map;
import pl.maciek.squashscoreboard.lootbox.LootboxActivity;
import pl.maciek.squashscoreboard.lootbox.RewardFonts;

/**
 * Self-contained page view for player profiles and statistics.
 * Hosted inside {@link HistoryActivity} to eliminate inter-activity transitions.
 */
public class PlayersPageView extends LinearLayout {
    private final Activity activity;
    private final UiTheme theme;
    private final Runnable onGoBack;
    private LinearLayout listContainer;

    public PlayersPageView(Activity activity, UiTheme theme, Runnable onGoBack) {
        super(activity);
        this.activity = activity;
        this.theme = theme;
        this.onGoBack = onGoBack;
        init();
    }

    private void init() {
        setOrientation(VERTICAL);
        setBackgroundColor(theme.background);
        setPadding(dp(16), dp(12), dp(16), dp(8));

        // Top App Bar
        LinearLayout header = new LinearLayout(activity);
        header.setGravity(Gravity.CENTER_VERTICAL);

        Button back = theme.outlinedButton(activity, "← Go back");
        back.setContentDescription("Go back to scoreboard.");
        back.setOnClickListener(v -> {
            if (onGoBack != null) onGoBack.run();
        });

        TextView title = text("Players and statistics", 22, theme.onSurface);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setPadding(dp(16), 0, dp(16), 0);

        Button cloudBtn = theme.outlinedButton(activity, "Set up cloud");
        cloudBtn.setContentDescription("Set up Supabase cloud connection.");
        cloudBtn.setOnClickListener(v -> showCloudSettingsDialog(null));

        Button compare = theme.tonalButton(activity, "Compare players");
        compare.setOnClickListener(v -> chooseComparison(ProfileStore.load(activity)));

        Button add = theme.primaryButton(activity, "Add player");
        add.setOnClickListener(v -> chooseAddPlayerMode());

        header.addView(back, new LinearLayout.LayoutParams(-2, dp(48)));
        header.addView(title, new LinearLayout.LayoutParams(0, dp(48), 1f));
        header.addView(cloudBtn, btnMargin());
        header.addView(compare, btnMargin());
        header.addView(add, btnMargin());
        addView(header, new LinearLayout.LayoutParams(-1, dp(52)));

        ScrollView scroll = new ScrollView(activity);
        scroll.setPadding(0, dp(8), 0, dp(8));
        listContainer = box(theme.background);
        scroll.addView(listContainer);
        addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));

        populateList();
    }

    public void refresh() {
        populateList();
    }

    private void populateList() {
        if (listContainer == null) return;
        listContainer.removeAllViews();

        List<PlayerProfile> profiles = ProfileStore.load(activity);
        if (profiles.isEmpty()) {
            LinearLayout emptyBox = box(theme.background);
            emptyBox.setGravity(Gravity.CENTER);
            emptyBox.setPadding(dp(24), dp(36), dp(24), dp(36));

            TextView emptyTitle = text("No saved player profiles", 22, theme.onSurface);
            emptyTitle.setTypeface(Typeface.DEFAULT_BOLD);
            emptyTitle.setGravity(Gravity.CENTER);

            TextView emptyDesc = text(
                "Add a local player or pull a cloud player with a secret token to track career statistics.",
                16,
                theme.onSurfaceVariant
            );
            emptyDesc.setGravity(Gravity.CENTER);
            emptyDesc.setPadding(0, dp(8), 0, dp(16));

            Button emptyAdd = theme.primaryButton(activity, "Add player");
            emptyAdd.setOnClickListener(v -> chooseAddPlayerMode());

            emptyBox.addView(emptyTitle);
            emptyBox.addView(emptyDesc);
            emptyBox.addView(emptyAdd, new LinearLayout.LayoutParams(-2, dp(48)));
            listContainer.addView(emptyBox, new LinearLayout.LayoutParams(-1, -2));
        } else {
            // [PERF-120HZ] Single-pass batch calculation across all profiles
            Map<String, PlayerStatistics.Summary> summaries = PlayerStatistics.calculateAll(activity, profiles);
            for (PlayerProfile p : profiles) {
                PlayerStatistics.Summary summary = summaries.get(p.id);
                listContainer.addView(profileRow(p, summary != null ? summary : new PlayerStatistics.Summary()), rowParams());
            }
        }

        Button lootbox = theme.outlinedButton(activity, "Open reward lootbox");
        lootbox.setContentDescription("Open the reward lootbox.");
        lootbox.setOnClickListener(v -> chooseLootboxPlayer());
        LinearLayout.LayoutParams lootboxParams = new LinearLayout.LayoutParams(-1, dp(48));
        lootboxParams.topMargin = dp(16);
        listContainer.addView(lootbox, lootboxParams);
    }

    private View profileRow(PlayerProfile p, PlayerStatistics.Summary s) {
        int accent = theme.playerAccentOnSurface(p.color);
        int rate = s.matches == 0 ? 0 : Math.round(s.wins * 100f / s.matches);
        String avgPts = s.matches == 0 ? "0.0" : String.format(Locale.US, "%.1f", s.points / (float) s.matches);
        boolean cloudLinked = CloudConfigStore.isCloudLinked(activity, p.id);

        LinearLayout row = box(theme.surfaceLow);
        row.setPadding(dp(18), dp(12), dp(18), dp(12));
        row.setMinimumHeight(dp(92));
        row.setBackground(theme.interactiveBackground(activity, theme.surfaceLow, 14, 2, accent));

        String headerText = cloudLinked ? p.name + "  ·  ☁ CLOUD" : p.name;
        TextView name = text(headerText, 20, accent);
        RewardFonts.apply(name, p.fontId, 20f);

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
            p.name + (cloudLinked ? ", Cloud linked" : ", Local profile")
                + ", Color " + UiTheme.playerColorName(p.color)
                + ". " + s.matches + " matches, " + s.wins + " wins, win rate " + rate + " percent. Double-tap to open player profile."
        );
        row.setOnClickListener(v ->
            activity.startActivity(new Intent(activity, PlayerDetailActivity.class).putExtra("profile", p.id))
        );
        return row;
    }

    private void chooseComparison(List<PlayerProfile> profiles) {
        if (profiles.size() < 2) {
            new AlertDialog.Builder(activity)
                .setTitle("Cannot compare players")
                .setMessage("The app cannot compare players yet. A comparison requires at least 2 player profiles. Tap Add player to create another profile.")
                .setNegativeButton("Cancel action", null)
                .setPositiveButton("Add player", (d, w) -> chooseAddPlayerMode())
                .show();
            return;
        }
        if (profiles.size() == 2) {
            activity.startActivity(
                new Intent(activity, PlayerComparisonActivity.class)
                    .putExtra("one", profiles.get(0).id)
                    .putExtra("two", profiles.get(1).id)
            );
            return;
        }
        String[] names = new String[profiles.size()];
        for (int i = 0; i < profiles.size(); i++) names[i] = profiles.get(i).name;

        LinearLayout form = box(theme.surface);
        form.setPadding(dp(20), dp(12), dp(20), dp(12));
        Spinner one = theme.styledSpinner(activity, names, 0);
        Spinner two = theme.styledSpinner(activity, names, 1);

        form.addView(label("Profile for Player 1"));
        form.addView(one, new LinearLayout.LayoutParams(-1, dp(48)));
        form.addView(label("Profile for Player 2"));
        form.addView(two, new LinearLayout.LayoutParams(-1, dp(48)));

        AlertDialog dialog = new AlertDialog.Builder(activity)
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
                    new AlertDialog.Builder(activity)
                        .setTitle("Same player selected")
                        .setMessage("The selected profiles are the same. You must select 2 different player profiles. Select a different profile for Player 2.")
                        .setPositiveButton("Close dialog", null)
                        .show();
                    return;
                }
                dialog.dismiss();
                activity.startActivity(
                    new Intent(activity, PlayerComparisonActivity.class)
                        .putExtra("one", profiles.get(one.getSelectedItemPosition()).id)
                        .putExtra("two", profiles.get(two.getSelectedItemPosition()).id)
                );
            });
        });
        dialog.show();
    }

    private void chooseLootboxPlayer() {
        List<PlayerProfile> profiles = ProfileStore.load(activity);
        String[] names = new String[profiles.size()];
        for (int i = 0; i < profiles.size(); i++) names[i] = profiles.get(i).name;
        new AlertDialog.Builder(activity)
            .setTitle("Open lootbox for player")
            .setItems(names, (dialog, index) -> {
                PlayerProfile player = profiles.get(index);
                activity.startActivity(new Intent(activity, LootboxActivity.class)
                    .putExtra(LootboxActivity.EXTRA_PROFILE_ID, player.id)
                    .putExtra(LootboxActivity.EXTRA_PLAYER_NAME, player.name));
            })
            .setNegativeButton("Cancel action", null)
            .show();
    }

    private void chooseAddPlayerMode() {
        String[] modes = {
            "Create local player (offline)",
            "Pull player from cloud (token)",
            "Create new cloud player (token)"
        };
        AlertDialog dialog = new AlertDialog.Builder(activity)
            .setTitle("Add player")
            .setItems(modes, (d, which) -> {
                if (which == 0) {
                    addLocalProfileDialog();
                } else if (which == 1) {
                    pullCloudProfileDialog();
                } else if (which == 2) {
                    createCloudProfileDialog();
                }
            })
            .setNeutralButton("Set up cloud", (d, w) -> showCloudSettingsDialog(null))
            .setNegativeButton("Cancel action", null)
            .create();
        styleDialog(dialog);
        dialog.show();
    }

    private void showCloudSettingsDialog(Runnable onSaved) {
        LinearLayout form = box(theme.surface);
        form.setPadding(dp(20), dp(12), dp(20), dp(12));

        EditText urlField = theme.editField(
            activity,
            CloudConfigStore.getProjectUrl(activity),
            "https://your-project.supabase.co",
            InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI
        );
        EditText keyField = theme.editField(
            activity,
            CloudConfigStore.getAnonKey(activity),
            "Supabase public key",
            InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD
        );

        form.addView(label("Supabase project URL"));
        form.addView(urlField, new LinearLayout.LayoutParams(-1, dp(48)));
        form.addView(label("Supabase public key"));
        form.addView(keyField, new LinearLayout.LayoutParams(-1, dp(48)));

        AlertDialog dialog = new AlertDialog.Builder(activity)
            .setTitle("Set up Supabase cloud")
            .setView(form)
            .setNegativeButton("Cancel action", null)
            .setPositiveButton("Save settings", null)
            .create();

        dialog.setOnShowListener(ignored -> {
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(theme.surface));
            }
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(theme.primary);
            Button saveBtn = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            saveBtn.setTextColor(theme.primary);
            saveBtn.setOnClickListener(v -> {
                String rawUrl = urlField.getText().toString().trim();
                String rawKey = keyField.getText().toString().trim();
                if (!CloudConfigStore.isValidHttpsEndpoint(rawUrl) || rawKey.isEmpty()) {
                    showError(
                        "Invalid cloud settings",
                        "The Supabase URL or public key is not valid. The URL must start with https:// and the key cannot be empty. Enter both values."
                    );
                    return;
                }
                CloudConfigStore.saveServerConfig(activity, rawUrl, rawKey);
                dialog.dismiss();
                if (onSaved != null) onSaved.run();
            });
        });
        dialog.show();
    }

    private void pullCloudProfileDialog() {
        LinearLayout form = box(theme.surface);
        form.setPadding(dp(20), dp(12), dp(20), dp(12));

        boolean hasServerConfig = CloudConfigStore.isServerConfigured(activity);
        EditText urlField = theme.editField(
            activity,
            CloudConfigStore.getProjectUrl(activity),
            "https://your-project.supabase.co",
            InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI
        );
        EditText keyField = theme.editField(
            activity,
            CloudConfigStore.getAnonKey(activity),
            "Supabase public key",
            InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD
        );
        EditText tokenField = theme.editField(
            activity,
            "",
            "Player secret token",
            InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD
        );

        if (!hasServerConfig) {
            form.addView(label("Supabase project URL"));
            form.addView(urlField, new LinearLayout.LayoutParams(-1, dp(48)));
            form.addView(label("Supabase public key"));
            form.addView(keyField, new LinearLayout.LayoutParams(-1, dp(48)));
        }
        form.addView(label("Player secret token"));
        form.addView(tokenField, new LinearLayout.LayoutParams(-1, dp(48)));

        TextView note = text(
            "The app hashes the token with SHA-256 and sends x-player-token-hash to pull your profile and statistics.",
            14,
            theme.onSurfaceVariant
        );
        note.setPadding(0, dp(8), 0, 0);
        form.addView(note);

        AlertDialog dialog = new AlertDialog.Builder(activity)
            .setTitle("Pull player from cloud")
            .setView(form)
            .setNegativeButton("Cancel action", null)
            .setNeutralButton("Edit server", null)
            .setPositiveButton("Pull player", null)
            .create();

        dialog.setOnShowListener(ignored -> {
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(theme.surface));
            }
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(theme.primary);
            Button serverBtn = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
            serverBtn.setTextColor(theme.primary);
            serverBtn.setOnClickListener(v -> {
                dialog.dismiss();
                showCloudSettingsDialog(this::pullCloudProfileDialog);
            });

            Button pullBtn = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            pullBtn.setTextColor(theme.primary);
            pullBtn.setOnClickListener(v -> {
                if (!hasServerConfig) {
                    String rawUrl = urlField.getText().toString().trim();
                    String rawKey = keyField.getText().toString().trim();
                    if (!CloudConfigStore.isValidHttpsEndpoint(rawUrl) || rawKey.isEmpty()) {
                        showError(
                            "Invalid cloud settings",
                            "The Supabase URL or public key is not valid. The URL must start with https:// and the key cannot be empty. Enter both values."
                        );
                        return;
                    }
                    CloudConfigStore.saveServerConfig(activity, rawUrl, rawKey);
                }
                String secretToken = tokenField.getText().toString().trim();
                if (secretToken.isEmpty()) {
                    showError(
                        "Empty player token",
                        "The player secret token is empty. Supabase requires a secret token to authenticate the player. Enter your secret token."
                    );
                    return;
                }
                pullBtn.setEnabled(false);
                pullBtn.setText("Pulling...");
                SupabaseSyncClient.pullPlayerAsync(activity, secretToken, new SupabaseSyncClient.Callback<SupabaseSyncClient.PulledPlayer>() {
                    @Override
                    public void onSuccess(SupabaseSyncClient.PulledPlayer result) {
                        if (activity.isFinishing() || activity.isDestroyed()) return;
                        dialog.dismiss();
                        refresh();
                        new AlertDialog.Builder(activity)
                            .setTitle("Player synced")
                            .setMessage("Pulled " + result.profile.name + " from Supabase (" + result.syncedMatchesCount + " saved matches).")
                            .setPositiveButton("Close dialog", null)
                            .show();
                    }

                    @Override
                    public void onError(String userMessage) {
                        if (activity.isFinishing() || activity.isDestroyed()) return;
                        pullBtn.setEnabled(true);
                        pullBtn.setText("Pull player");
                        showError("Cannot pull player", userMessage);
                    }
                });
            });
        });
        dialog.show();
    }

    private void createCloudProfileDialog() {
        LinearLayout form = box(theme.surface);
        form.setPadding(dp(20), dp(12), dp(20), dp(12));

        boolean hasServerConfig = CloudConfigStore.isServerConfigured(activity);
        EditText urlField = theme.editField(
            activity,
            CloudConfigStore.getProjectUrl(activity),
            "https://your-project.supabase.co",
            InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI
        );
        EditText keyField = theme.editField(
            activity,
            CloudConfigStore.getAnonKey(activity),
            "Supabase public key",
            InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD
        );
        EditText nameField = theme.editField(
            activity,
            "",
            "Player name",
            InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        );
        Spinner colorSpinner = theme.styledSpinner(activity, UiTheme.playerColorNames(activity), 0);
        EditText tokenField = theme.editField(
            activity,
            "",
            "Choose a secret token",
            InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD
        );

        if (!hasServerConfig) {
            form.addView(label("Supabase project URL"));
            form.addView(urlField, new LinearLayout.LayoutParams(-1, dp(48)));
            form.addView(label("Supabase public key"));
            form.addView(keyField, new LinearLayout.LayoutParams(-1, dp(48)));
        }
        form.addView(label("Player name"));
        form.addView(nameField, new LinearLayout.LayoutParams(-1, dp(48)));
        form.addView(label("Player color"));
        form.addView(colorSpinner, new LinearLayout.LayoutParams(-1, dp(48)));
        form.addView(label("New player secret token"));
        form.addView(tokenField, new LinearLayout.LayoutParams(-1, dp(48)));

        AlertDialog dialog = new AlertDialog.Builder(activity)
            .setTitle("Create cloud player")
            .setView(form)
            .setNegativeButton("Cancel action", null)
            .setPositiveButton("Save and upload", null)
            .create();

        dialog.setOnShowListener(ignored -> {
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(theme.surface));
            }
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(theme.primary);
            Button saveBtn = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            saveBtn.setTextColor(theme.primary);
            saveBtn.setOnClickListener(v -> {
                if (!hasServerConfig) {
                    String rawUrl = urlField.getText().toString().trim();
                    String rawKey = keyField.getText().toString().trim();
                    if (!CloudConfigStore.isValidHttpsEndpoint(rawUrl) || rawKey.isEmpty()) {
                        showError(
                            "Invalid cloud settings",
                            "The Supabase URL or public key is not valid. The URL must start with https:// and the key cannot be empty. Enter both values."
                        );
                        return;
                    }
                    CloudConfigStore.saveServerConfig(activity, rawUrl, rawKey);
                }
                String n = nameField.getText().toString().trim();
                if (n.isEmpty()) {
                    showError(
                        "Empty player name",
                        "The player name is empty. Each player profile must have a name. Enter a name in the text field."
                    );
                    return;
                }
                if (ProfileStore.findByName(activity, n) != null) {
                    showError(
                        "Duplicate player name",
                        "This player name already exists. Each player profile must have a unique name. Enter a different name."
                    );
                    return;
                }
                String secretToken = tokenField.getText().toString().trim();
                if (secretToken.isEmpty()) {
                    showError(
                        "Empty player token",
                        "The player secret token is empty. Supabase requires a secret token to protect the profile. Enter a secret token."
                    );
                    return;
                }
                PlayerProfile created = new PlayerProfile(n, UiTheme.playerColors(activity)[colorSpinner.getSelectedItemPosition()]);
                ProfileStore.add(activity, created);
                saveBtn.setEnabled(false);
                saveBtn.setText("Uploading...");
                SupabaseSyncClient.registerOrPushPlayerAsync(activity, created, secretToken, new SupabaseSyncClient.Callback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        if (activity.isFinishing() || activity.isDestroyed()) return;
                        dialog.dismiss();
                        refresh();
                    }

                    @Override
                    public void onError(String userMessage) {
                        if (activity.isFinishing() || activity.isDestroyed()) return;
                        saveBtn.setEnabled(true);
                        saveBtn.setText("Save and upload");
                        refresh();
                        showError("Saved locally, cloud upload failed", userMessage);
                    }
                });
            });
        });
        dialog.show();
    }

    private void addLocalProfileDialog() {
        LinearLayout form = box(theme.surface);
        form.setPadding(dp(20), dp(12), dp(20), dp(12));
        EditText name = theme.editField(activity, "", "Player name", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        Spinner color = theme.styledSpinner(activity, UiTheme.playerColorNames(activity), 0);

        form.addView(label("Player name"));
        form.addView(name, new LinearLayout.LayoutParams(-1, dp(48)));
        form.addView(label("Player color"));
        form.addView(color, new LinearLayout.LayoutParams(-1, dp(48)));

        AlertDialog dialog = new AlertDialog.Builder(activity)
            .setTitle("Create local player")
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
                    showError(
                        "Empty player name",
                        "The player name is empty. Each player profile must have a name. Enter a name in the text field."
                    );
                    return;
                }
                if (ProfileStore.findByName(activity, n) != null) {
                    showError(
                        "Duplicate player name",
                        "This player name already exists. Each player profile must have a unique name. Enter a different name."
                    );
                    return;
                }
                ProfileStore.add(activity, new PlayerProfile(n, UiTheme.playerColors(activity)[color.getSelectedItemPosition()]));
                dialog.dismiss();
                refresh();
            });
        });
        dialog.show();
    }

    private void styleDialog(AlertDialog dialog) {
        dialog.setOnShowListener(ignored -> {
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(theme.surface));
            }
            if (dialog.getButton(AlertDialog.BUTTON_NEGATIVE) != null) {
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(theme.primary);
            }
            if (dialog.getButton(AlertDialog.BUTTON_NEUTRAL) != null) {
                dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(theme.primary);
            }
            if (dialog.getButton(AlertDialog.BUTTON_POSITIVE) != null) {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(theme.primary);
            }
        });
    }

    private void showError(String title, String message) {
        new AlertDialog.Builder(activity)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Close dialog", null)
            .show();
    }

    private TextView label(String s) {
        TextView t = text(s, 16, theme.onSurfaceVariant);
        t.setPadding(0, dp(8), 0, dp(2));
        return t;
    }

    private LinearLayout box(int color) {
        LinearLayout x = new LinearLayout(activity);
        x.setOrientation(LinearLayout.VERTICAL);
        x.setBackgroundColor(color);
        return x;
    }

    private TextView text(String s, int z, int c) {
        TextView v = new TextView(activity);
        v.setText(s);
        v.setTextSize(z);
        v.setTextColor(c);
        v.setGravity(Gravity.CENTER_VERTICAL);
        return v;
    }

    private LinearLayout.LayoutParams btnMargin() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, dp(48));
        lp.leftMargin = dp(8);
        return lp;
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

