package pl.maciek.squashscoreboard;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import pl.maciek.squashscoreboard.lootbox.RewardCatalog;
import pl.maciek.squashscoreboard.lootbox.RewardInventory;
import pl.maciek.squashscoreboard.lootbox.RewardTilePainter;

/** Full-screen player cosmetics editor. Future cosmetic categories have reserved tabs. */
public final class PlayerCustomizationActivity extends Activity {
    private PlayerProfile profile;
    private RewardCatalog.Reward selected;
    private UiTheme theme;
    private LinearLayout root, previewHost;
    private FrameLayout page;
    private TextView nickname, availability;
    private Button setColor;
    private final java.util.List<ColorTile> tiles = new java.util.ArrayList<>();
    private int activeTab;
    private boolean landscape;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        profile = ProfileStore.find(this, getIntent().getStringExtra("profile"));
        if (profile == null) { finish(); return; }
        selected = RewardCatalog.byKey(profile.color);
        if (selected == null) selected = RewardCatalog.all()[0];
        buildScreen();
    }

    @Override public void onConfigurationChanged(Configuration config) {
        super.onConfigurationChanged(config);
        buildScreen();
    }

    private void buildScreen() {
        theme = new UiTheme(this);
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(theme.background);
        root.setPadding(dp(16), dp(12), dp(16), dp(12));

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        Button back = theme.outlinedButton(this, "← Back");
        back.setOnClickListener(v -> finish());
        header.addView(back, new LinearLayout.LayoutParams(-2, dp(48)));
        nickname = label(profile.name + "  ✎", 24, theme.onSurface);
        nickname.setTypeface(Typeface.DEFAULT_BOLD);
        nickname.setGravity(Gravity.CENTER_VERTICAL);
        nickname.setPadding(dp(16), 0, dp(8), 0);
        nickname.setContentDescription("Edit player name: " + profile.name);
        nickname.setOnClickListener(v -> editNickname());
        header.addView(nickname, new LinearLayout.LayoutParams(0, dp(52), 1));
        root.addView(header);

        HorizontalScrollView tabScroll = new HorizontalScrollView(this);
        tabScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout tabs = new LinearLayout(this);
        String[] names = {"Color", "Font", "Frame", "Animation"};
        for (int i = 0; i < names.length; i++) {
            final int index = i;
            Button tab = i == activeTab ? theme.tonalButton(this, names[i])
                : theme.outlinedButton(this, names[i]);
            tab.setAllCaps(false);
            tab.setTextSize(13);
            tab.setMinWidth(0);
            tab.setMinimumWidth(0);
            tab.setPadding(dp(7), 0, dp(7), 0);
            tab.setOnClickListener(v -> { activeTab = index; buildScreen(); });
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-2, dp(44));
            params.setMargins(0, dp(8), dp(8), dp(8));
            tabs.addView(tab, params);
        }
        tabScroll.addView(tabs);
        root.addView(tabScroll, new LinearLayout.LayoutParams(-1, dp(60)));

        landscape = getResources().getConfiguration().orientation
            == Configuration.ORIENTATION_LANDSCAPE;
        LinearLayout workspace = new LinearLayout(this);
        workspace.setOrientation(landscape ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);
        root.addView(workspace, new LinearLayout.LayoutParams(-1, 0, 1));
        page = new FrameLayout(this);
        workspace.addView(page, landscape
            ? new LinearLayout.LayoutParams(0, -1, 1)
            : new LinearLayout.LayoutParams(-1, 0, 1));
        LinearLayout side = new LinearLayout(this);
        side.setOrientation(LinearLayout.VERTICAL);
        if (landscape) side.setPadding(dp(12), 0, 0, 0);
        workspace.addView(side, landscape
            ? new LinearLayout.LayoutParams(dp(300), -1)
            : new LinearLayout.LayoutParams(-1, -2));
        previewHost = new LinearLayout(this);
        previewHost.setOrientation(LinearLayout.VERTICAL);
        side.addView(previewHost, new LinearLayout.LayoutParams(-1, -2));
        availability = label("", 14, theme.onSurfaceVariant);
        availability.setGravity(Gravity.CENTER);
        side.addView(availability, new LinearLayout.LayoutParams(-1, dp(28)));
        setColor = theme.primaryButton(this, "Set color");
        setColor.setOnClickListener(v -> equipSelected());
        side.addView(setColor, new LinearLayout.LayoutParams(-1, dp(48)));

        if (activeTab == 0) showColors();
        else showPlaceholder(names[activeTab]);
        setContentView(root);
    }

    private void showColors() {
        tiles.clear();
        ScrollView scroll = new ScrollView(this);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        TextView intro = label("Choose a color · win locked colors in lootboxes", 15,
            theme.onSurfaceVariant);
        intro.setPadding(0, dp(4), 0, dp(10));
        content.addView(intro);
        RewardCatalog.Reward[] rewards = RewardCatalog.all();
        for (int start = 0; start < rewards.length; start += 3) {
            LinearLayout row = new LinearLayout(this);
            for (int col = 0; col < 3; col++) {
                LinearLayout cell = new LinearLayout(this);
                cell.setOrientation(LinearLayout.VERTICAL);
                cell.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);
                if (start + col < rewards.length) {
                    RewardCatalog.Reward reward = rewards[start + col];
                    ColorTile tile = new ColorTile(reward);
                    tiles.add(tile);
                    int tileSize = landscape ? 76 : 92;
                    cell.addView(tile, new LinearLayout.LayoutParams(dp(tileSize), dp(tileSize)));
                    TextView name = label(reward.name, 12, theme.onSurface);
                    name.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);
                    name.setMaxLines(2);
                    cell.addView(name, new LinearLayout.LayoutParams(-1, dp(landscape ? 30 : 38)));
                    cell.setContentDescription(reward.name + ", " + reward.tier.label + ", "
                        + (RewardInventory.owns(this, profile.id, reward) ? "unlocked" : "locked"));
                    cell.setOnClickListener(v -> select(reward));
                }
                LinearLayout.LayoutParams cellParams = new LinearLayout.LayoutParams(0, -2, 1);
                cellParams.setMargins(dp(2), dp(2), dp(2), dp(6));
                row.addView(cell, cellParams);
            }
            content.addView(row);
        }
        scroll.addView(content);
        page.addView(scroll, new FrameLayout.LayoutParams(-1, -1));
        refreshSelection();
    }

    private void showPlaceholder(String category) {
        TextView message = label(category + " customization is coming soon", 20,
            theme.onSurfaceVariant);
        message.setGravity(Gravity.CENTER);
        page.addView(message, new FrameLayout.LayoutParams(-1, -1));
        previewHost.setVisibility(View.GONE);
        availability.setVisibility(View.GONE);
        setColor.setVisibility(View.GONE);
    }

    private void select(RewardCatalog.Reward reward) {
        selected = reward;
        refreshSelection();
    }

    private void refreshSelection() {
        for (ColorTile tile : tiles) tile.invalidate();
        renderPreview();
        boolean owned = RewardInventory.owns(this, profile.id, selected);
        availability.setText(owned ? selected.name + " · " + selected.tier.label
            : selected.name + " · Locked — find it in a lootbox");
        setColor.setVisibility(owned ? View.VISIBLE : View.GONE);
        setColor.setEnabled(owned && profile.color != selected.key());
        setColor.setText(profile.color == selected.key() ? "Color equipped" : "Set color");
    }

    private void renderPreview() {
        previewHost.removeAllViews();
        TextView caption = label("MATCH PREVIEW", 13, theme.onSurfaceVariant);
        caption.setTypeface(Typeface.DEFAULT_BOLD);
        caption.setPadding(0, dp(5), 0, dp(5));
        previewHost.addView(caption);
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setGravity(Gravity.CENTER);
        panel.setPadding(dp(12), dp(7), dp(12), dp(7));
        GradientDrawable background = new GradientDrawable();
        background.setColor(theme.playerPanelFill(selected.key()));
        background.setCornerRadius(dp(16));
        background.setStroke(dp(3), theme.playerAccentOnSurface(selected.key()));
        panel.setBackground(background);
        if (selected.colors.length > 1)
            RewardCatalog.configure(panel, background, selected, true);
        TextView name = previewText(profile.name, landscape ? 14 : 18, Color.WHITE);
        TextView games = previewText("GAMES  0", landscape ? 12 : 16, 0xFFF4F0E9);
        TextView points = previewText("0", landscape ? 32 : 55, Color.WHITE);
        TextView hint = previewText("TAP PANEL TO ADD 1 POINT", landscape ? 8 : 10, 0xFFF4F0E9);
        panel.addView(name);
        panel.addView(games);
        panel.addView(points);
        panel.addView(hint);
        previewHost.addView(panel, new LinearLayout.LayoutParams(-1, dp(landscape ? 110 : 170)));
    }

    private TextView previewText(String text, int size, int color) {
        TextView view = label(text, size, color);
        view.setTypeface(Typeface.DEFAULT_BOLD);
        view.setGravity(Gravity.CENTER);
        view.setSingleLine(true);
        return view;
    }

    private void equipSelected() {
        if (!RewardInventory.owns(this, profile.id, selected)) return;
        profile = new PlayerProfile(profile.id, profile.name, selected.key());
        ProfileStore.update(this, profile);
        refreshSelection();
    }

    private void editNickname() {
        android.widget.EditText input = theme.editField(this, profile.name, "Player name",
            InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        LinearLayout form = new LinearLayout(this);
        form.setPadding(dp(20), dp(10), dp(20), 0);
        form.addView(input, new LinearLayout.LayoutParams(-1, dp(52)));
        AlertDialog dialog = new AlertDialog.Builder(this)
            .setTitle("Edit player name")
            .setView(form)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save", null)
            .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            .setOnClickListener(v -> {
                String next = input.getText().toString().trim();
                PlayerProfile duplicate = ProfileStore.findByName(this, next);
                if (next.isEmpty() || (duplicate != null && !duplicate.id.equals(profile.id))) {
                    input.setError(next.isEmpty() ? "Enter a name" : "Name already in use");
                    return;
                }
                String previous = profile.name;
                profile = new PlayerProfile(profile.id, next, profile.color);
                ProfileStore.update(this, profile);
                MatchHistoryStore.renameProfile(this, profile.id, previous, next);
                nickname.setText(next + "  ✎");
                nickname.setContentDescription("Edit player name: " + next);
                if (activeTab == 0) renderPreview();
                dialog.dismiss();
            }));
        dialog.show();
    }

    private TextView label(String text, int size, int color) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(size);
        view.setTextColor(color);
        return view;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + .5f);
    }

    private final class ColorTile extends View {
        private final RewardCatalog.Reward reward;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        ColorTile(RewardCatalog.Reward reward) { super(PlayerCustomizationActivity.this); this.reward = reward; }

        @Override protected void onDraw(Canvas canvas) {
            float size = Math.min(getWidth(), getHeight()) - dp(8);
            float left = (getWidth() - size) / 2f, top = (getHeight() - size) / 2f;
            RewardTilePainter.draw(canvas, left, top, size, reward, paint, this);
            if (!RewardInventory.owns(PlayerCustomizationActivity.this, profile.id, reward)) {
                paint.setShader(null);
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(0x990B1018);
                canvas.drawRoundRect(left, top, left + size, top + size,
                    dp(9), dp(9), paint);
                float cx = getWidth() / 2f, cy = getHeight() / 2f;
                paint.setColor(Color.WHITE);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(dp(3));
                canvas.drawRoundRect(cx - dp(10), cy - dp(17), cx + dp(10), cy + dp(5),
                    dp(10), dp(10), paint);
                paint.setStyle(Paint.Style.FILL);
                canvas.drawRoundRect(cx - dp(15), cy - dp(3), cx + dp(15), cy + dp(20),
                    dp(3), dp(3), paint);
                paint.setColor(0xFF30343B);
                canvas.drawCircle(cx, cy + dp(7), dp(2), paint);
            }
            if (selected == reward) {
                paint.setShader(null);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(dp(4));
                paint.setColor(0xFFFFD166);
                canvas.drawRoundRect(left + dp(1), top + dp(1),
                    left + size - dp(1), top + size - dp(1), dp(9), dp(9), paint);
                paint.setStyle(Paint.Style.FILL);
            }
        }
    }
}
