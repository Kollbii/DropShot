package pl.maciek.squashscoreboard;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import pl.maciek.squashscoreboard.lootbox.RewardCatalog;
import pl.maciek.squashscoreboard.lootbox.RewardInventory;

/**
 * Shared Material Design 3 tokens, WCAG 2.2 AA contrast colors, 8 dp spacing helpers,
 * and shared navigation components for DropShot.
 */
public final class UiTheme {
    public static final int TAB_MATCH = 0;
    public static final int TAB_HISTORY = 1;
    public static final int TAB_PLAYERS = 2;
    public static final int TAB_SETTINGS = 3;

    public static final String EXTRA_OPEN_SETTINGS = "open_settings";

    public final boolean dark;
    public final boolean highContrast;
    public final int background;
    public final int surface;
    public final int surfaceLow;
    public final int surfaceHigh;
    public final int surfaceHighest;
    public final int onSurface;
    public final int onSurfaceVariant;
    public final int outline;
    public final int outlineVariant;
    public final int primary;
    public final int onPrimary;
    public final int primaryContainer;
    public final int onPrimaryContainer;
    public final int secondaryContainer;
    public final int onSecondaryContainer;
    public final int tertiary;
    public final int error;
    public final int onError;
    public final int errorContainer;
    public final int onErrorContainer;
    public final int success;
    public final int warning;

    public UiTheme(Context context) {
        this.dark = AppPreferences.dark(context);
        this.highContrast = AppPreferences.highContrast(context);
        if (dark) {
            background = 0xFF16130C;
            surface = 0xFF221F1A;
            surfaceLow = 0xFF1E1B16;
            surfaceHigh = 0xFF2C2A24;
            surfaceHighest = 0xFF37342F;
            onSurface = highContrast ? 0xFFFFFFFF : 0xFFE6E2DB;
            onSurfaceVariant = highContrast ? 0xFFF9F0DF : 0xFFCEC6B6;
            outline = highContrast ? 0xFFEAE2D1 : 0xFF989081;
            outlineVariant = highContrast ? 0xFFCEC6B6 : 0xFF4C4639;
            primary = highContrast ? 0xFFFFDBCE : 0xFFFFB59A;
            onPrimary = highContrast ? 0xFF000000 : 0xFF5B1B00;
            primaryContainer = highContrast ? 0xFFFFB59A : 0xFF852400;
            onPrimaryContainer = highContrast ? 0xFF000000 : 0xFFFFDBCE;
            secondaryContainer = highContrast ? 0xFFE2C0A9 : 0xFF5B412F;
            onSecondaryContainer = highContrast ? 0xFF000000 : 0xFFFFDCC5;
            tertiary = 0xFF79D4DF;
            error = highContrast ? 0xFFFFDAD3 : 0xFFFFB4A5;
            onError = highContrast ? 0xFF000000 : 0xFF690000;
            errorContainer = highContrast ? 0xFFFFB4A5 : 0xFF93000B;
            onErrorContainer = highContrast ? 0xFF000000 : 0xFFFFDAD3;
            success = 0xFF8ED889;
            warning = 0xFFFFB86D;
        } else {
            background = 0xFFFDF9F2;
            surface = 0xFFF1EDE6;
            surfaceLow = 0xFFF7F3EC;
            surfaceHigh = 0xFFECE8E0;
            surfaceHighest = 0xFFE6E2DB;
            onSurface = highContrast ? 0xFF000000 : 0xFF1E1B16;
            onSurfaceVariant = highContrast ? 0xFF201B0E : 0xFF4C4639;
            outline = highContrast ? 0xFF363023 : 0xFF7E7668;
            outlineVariant = highContrast ? 0xFF4C4639 : 0xFFCEC6B6;
            primary = highContrast ? 0xFF5B1B00 : 0xFFAF3000;
            onPrimary = 0xFFFFFFFF;
            primaryContainer = highContrast ? 0xFF852400 : 0xFFFFDBCE;
            onPrimaryContainer = highContrast ? 0xFFFFFFFF : 0xFF321200;
            secondaryContainer = highContrast ? 0xFF5B412F : 0xFFFFDCC5;
            onSecondaryContainer = highContrast ? 0xFFFFFFFF : 0xFF2C1700;
            tertiary = 0xFF006972;
            error = highContrast ? 0xFF690000 : 0xFFBF0312;
            onError = 0xFFFFFFFF;
            errorContainer = highContrast ? 0xFF93000B : 0xFFFFDAD3;
            onErrorContainer = highContrast ? 0xFFFFFFFF : 0xFF380C00;
            success = 0xFF206C26;
            warning = 0xFF895100;
        }
    }

    /** Player color keys are supplied by the shared reward catalog. */
    public static int[] playerColors(Context context) {
        RewardCatalog.Reward[] rewards = RewardInventory.available(context, null);
        int[] colors = new int[rewards.length];
        for (int i = 0; i < rewards.length; i++) colors[i] = rewards[i].key();
        return colors;
    }

    public static String[] playerColorNames(Context context) {
        RewardCatalog.Reward[] rewards = RewardInventory.available(context, null);
        String[] names = new String[rewards.length];
        for (int i = 0; i < rewards.length; i++)
            names[i] = rewards[i].name + " · " + rewards[i].tier.label;
        return names;
    }

    public static String playerColorName(int rawColor) {
        RewardCatalog.Reward reward = RewardCatalog.byKey(rawColor);
        return reward == null ? "Custom" : reward.name;
    }

    /** Non-color symbol per player color so color is never the only signal. */
    public static String playerSymbol(int rawColor) {
        RewardCatalog.Reward reward = RewardCatalog.byKey(rawColor);
        if (reward != null && reward.colors.length > 1) return "✦";
        if (rawColor == AppPreferences.RED) return "◆";
        if (rawColor == AppPreferences.GREEN) return "▲";
        if (rawColor == AppPreferences.GOLD) return "★";
        return "■";
    }

    /** High-contrast panel fill (T40 / T30) for score panels where white text has >= 6.45:1 contrast. */
    public int playerPanelFill(int rawColor) {
        if (RewardCatalog.byKey(rawColor) != null && rawColor != AppPreferences.BLUE
            && rawColor != AppPreferences.RED && rawColor != AppPreferences.GREEN
            && rawColor != AppPreferences.GOLD) return darken(rawColor);
        if (rawColor == AppPreferences.RED) return highContrast ? 0xFF92001D : 0xFFBB152C;
        if (rawColor == AppPreferences.GREEN) return highContrast ? 0xFF005233 : 0xFF006C45;
        if (rawColor == AppPreferences.GOLD) return highContrast ? 0xFF5C4200 : 0xFF7A5900;
        return highContrast ? 0xFF004590 : 0xFF005CBC;
    }

    /** High-contrast text/line accent on surface (T80 in dark, T40 in light) with >= 6.14:1 contrast. */
    public int playerAccentOnSurface(int rawColor) {
        if (RewardCatalog.byKey(rawColor) != null && rawColor != AppPreferences.BLUE
            && rawColor != AppPreferences.RED && rawColor != AppPreferences.GREEN
            && rawColor != AppPreferences.GOLD) return rawColor;
        if (dark) {
            if (rawColor == AppPreferences.RED) return 0xFFFFB3AD;
            if (rawColor == AppPreferences.GREEN) return 0xFF58DE9E;
            if (rawColor == AppPreferences.GOLD) return 0xFFF7BD3B;
            return 0xFFB7C4FF;
        }
        return playerPanelFill(rawColor);
    }

    private static int darken(int color) {
        return Color.rgb(Math.round(Color.red(color) * .58f),
            Math.round(Color.green(color) * .58f), Math.round(Color.blue(color) * .58f));
    }

    public static boolean isReduceMotionEnabled(Context context) {
        try {
            float scale = Settings.Global.getFloat(
                context.getContentResolver(),
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            );
            return scale == 0f;
        } catch (Exception ignored) {
            return false;
        }
    }

    public static int dp(Context context, int value) {
        return (int) (value * context.getResources().getDisplayMetrics().density + 0.5f);
    }

    public static void applyTabularNumbers(TextView view) {
        view.setFontFeatureSettings("tnum");
    }

    public GradientDrawable roundedBox(int fillColor, int cornerDp, int strokeDp, int strokeColor, Context context) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fillColor);
        drawable.setCornerRadius(dp(context, cornerDp));
        if (strokeDp > 0) {
            drawable.setStroke(dp(context, strokeDp), strokeColor);
        }
        return drawable;
    }

    public StateListDrawable interactiveBackground(Context context, int fillColor, int cornerDp, int strokeDp, int strokeColor) {
        StateListDrawable states = new StateListDrawable();
        int pressedFill = blend(fillColor, onSurface, 0.14f);
        GradientDrawable focused = roundedBox(pressedFill, cornerDp, Math.max(2, strokeDp), primary, context);
        GradientDrawable pressed = roundedBox(pressedFill, cornerDp, strokeDp, strokeColor, context);
        GradientDrawable normal = roundedBox(fillColor, cornerDp, strokeDp, strokeColor, context);
        states.addState(new int[]{android.R.attr.state_focused}, focused);
        states.addState(new int[]{android.R.attr.state_pressed}, pressed);
        states.addState(new int[]{}, normal);
        return states;
    }

    private static int blend(int base, int overlay, float ratio) {
        int r = (int) (Color.red(base) * (1f - ratio) + Color.red(overlay) * ratio);
        int g = (int) (Color.green(base) * (1f - ratio) + Color.green(overlay) * ratio);
        int b = (int) (Color.blue(base) * (1f - ratio) + Color.blue(overlay) * ratio);
        return Color.rgb(r, g, b);
    }

    public Button primaryButton(Context context, String label) {
        Button button = new Button(context);
        button.setText(label);
        button.setAllCaps(false);
        button.setTextSize(16);
        button.setTypeface(Typeface.DEFAULT_BOLD);
        button.setTextColor(onPrimary);
        button.setMinHeight(dp(context, 48));
        button.setMinimumHeight(dp(context, 48));
        button.setPadding(dp(context, 16), 0, dp(context, 16), 0);
        button.setBackground(interactiveBackground(context, primary, 24, 0, 0));
        // [MASVS-PLATFORM] Reject touches when an untrusted overlay obscures the view (anti-tapjacking)
        button.setFilterTouchesWhenObscured(true);
        return button;
    }

    public Button tonalButton(Context context, String label) {
        Button button = new Button(context);
        button.setText(label);
        button.setAllCaps(false);
        button.setTextSize(16);
        button.setTypeface(Typeface.DEFAULT_BOLD);
        button.setTextColor(onSecondaryContainer);
        button.setMinHeight(dp(context, 48));
        button.setMinimumHeight(dp(context, 48));
        button.setPadding(dp(context, 16), 0, dp(context, 16), 0);
        button.setBackground(interactiveBackground(context, secondaryContainer, 24, 0, 0));
        button.setFilterTouchesWhenObscured(true);
        return button;
    }

    public Button outlinedButton(Context context, String label) {
        Button button = new Button(context);
        button.setText(label);
        button.setAllCaps(false);
        button.setTextSize(16);
        button.setTextColor(onSurface);
        button.setMinHeight(dp(context, 48));
        button.setMinimumHeight(dp(context, 48));
        button.setPadding(dp(context, 14), 0, dp(context, 14), 0);
        button.setBackground(interactiveBackground(context, surfaceLow, 24, 1, outline));
        button.setFilterTouchesWhenObscured(true);
        return button;
    }

    public Button dangerButton(Context context, String label) {
        Button button = new Button(context);
        button.setText(label);
        button.setAllCaps(false);
        button.setTextSize(16);
        button.setTypeface(Typeface.DEFAULT_BOLD);
        button.setTextColor(onErrorContainer);
        button.setMinHeight(dp(context, 48));
        button.setMinimumHeight(dp(context, 48));
        button.setPadding(dp(context, 14), 0, dp(context, 14), 0);
        button.setBackground(interactiveBackground(context, errorContainer, 24, 1, error));
        button.setFilterTouchesWhenObscured(true);
        return button;
    }

    public EditText editField(Context context, String text, String hint, int inputType) {
        EditText edit = new EditText(context);
        edit.setText(text);
        if (hint != null) {
            edit.setHint(hint);
            edit.setHintTextColor(onSurfaceVariant);
        }
        edit.setTextColor(onSurface);
        edit.setTextSize(18);
        edit.setSingleLine(true);
        edit.setInputType(inputType);
        edit.setMinHeight(dp(context, 48));
        edit.setMinimumHeight(dp(context, 48));
        edit.setPadding(dp(context, 14), 0, dp(context, 14), 0);
        edit.setBackground(roundedBox(surfaceLow, 8, 1, outline, context));
        edit.setImeOptions(EditorInfo.IME_ACTION_DONE);
        edit.setImeActionLabel("Save", EditorInfo.IME_ACTION_DONE);
        edit.setOnEditorActionListener((view, action, event) -> {
            if (action != EditorInfo.IME_ACTION_DONE && (event == null || event.getKeyCode() != KeyEvent.KEYCODE_ENTER)) {
                return false;
            }
            InputMethodManager keyboard = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (keyboard != null) {
                keyboard.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
            return true;
        });
        return edit;
    }

    public Spinner styledSpinner(Context context, String[] items, int selectedIndex) {
        Spinner spinner = new Spinner(context);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(context, android.R.layout.simple_spinner_dropdown_item, items) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView tv = (TextView) super.getView(position, convertView, parent);
                tv.setTextColor(onSurface);
                tv.setTextSize(17);
                tv.setPadding(dp(context, 14), 0, dp(context, 14), 0);
                return tv;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                TextView tv = (TextView) super.getDropDownView(position, convertView, parent);
                tv.setTextColor(onSurface);
                tv.setTextSize(17);
                tv.setBackgroundColor(surfaceHigh);
                tv.setPadding(dp(context, 16), dp(context, 12), dp(context, 16), dp(context, 12));
                return tv;
            }
        };
        spinner.setAdapter(adapter);
        spinner.setMinimumHeight(dp(context, 48));
        spinner.setBackground(roundedBox(surfaceLow, 8, 1, outline, context));
        spinner.setPopupBackgroundDrawable(new ColorDrawable(surfaceHigh));
        if (selectedIndex >= 0 && selectedIndex < items.length) {
            spinner.setSelection(selectedIndex);
        }
        return spinner;
    }

    public CollapsibleBottomNav createCollapsibleBottomNav(Activity activity, int activeTab, Runnable onOpenSettingsHere) {
        return new CollapsibleBottomNav(activity, activeTab, onOpenSettingsHere);
    }

    public final class CollapsibleBottomNav extends FrameLayout {
        private static final long HIDE_DELAY_MS = 5000;
        private final Handler handler = new Handler(Looper.getMainLooper());
        private final Runnable hideTask = () -> setExpanded(false);
        private final Button toggle;
        private final View bar;
        private boolean expanded;
        private boolean pinned;

        private CollapsibleBottomNav(Activity activity, int activeTab, Runnable onOpenSettingsHere) {
            super(activity);
            bar = createBottomNavBar(activity, activeTab, onOpenSettingsHere);
            bar.setVisibility(GONE);
            addView(bar, new FrameLayout.LayoutParams(-1, dp(activity, 56), Gravity.BOTTOM));

            toggle = new Button(activity);
            toggle.setText("⌃");
            toggle.setTextSize(18);
            toggle.setTextColor(onSurface);
            toggle.setAllCaps(false);
            toggle.setPadding(0, 0, 0, 0);
            toggle.setBackground(interactiveBackground(activity, surfaceHigh, 24, 1, outline));
            toggle.setContentDescription("Show navigation menu");
            toggle.setOnClickListener(v -> setExpanded(!expanded));
            FrameLayout.LayoutParams toggleParams = new FrameLayout.LayoutParams(dp(activity, 48), dp(activity, 48), Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
            toggleParams.bottomMargin = dp(activity, 4);
            addView(toggle, toggleParams);
        }

        public void onUserInteraction() {
            if (expanded && !pinned) scheduleHide();
        }

        public void setPinned(boolean keepOpen) {
            if (pinned == keepOpen) return;
            pinned = keepOpen;
            if (keepOpen) {
                setExpanded(true);
            } else if (expanded) {
                scheduleHide();
            }
        }

        private void scheduleHide() {
            handler.removeCallbacks(hideTask);
            if (!pinned) handler.postDelayed(hideTask, HIDE_DELAY_MS);
        }

        private void setExpanded(boolean show) {
            bar.animate().cancel();
            expanded = show;
            toggle.setText(show ? "⌄" : "⌃");
            toggle.setContentDescription(show ? "Hide navigation menu" : "Show navigation menu");
            handler.removeCallbacks(hideTask);
            if (show) {
                bar.setVisibility(VISIBLE);
                bar.setTranslationY(dp(getContext(), 56));
                bar.setAlpha(0f);
                if (isReduceMotionEnabled(getContext())) {
                    bar.setTranslationY(0);
                    bar.setAlpha(1f);
                } else {
                    bar.animate().translationY(0).alpha(1f).setDuration(220).start();
                }
                scheduleHide();
            } else if (isReduceMotionEnabled(getContext())) {
                bar.setVisibility(GONE);
            } else {
                bar.animate().translationY(dp(getContext(), 56)).alpha(0f).setDuration(220)
                    .withEndAction(() -> { if (!expanded) bar.setVisibility(GONE); }).start();
            }
        }

        @Override protected void onDetachedFromWindow() {
            handler.removeCallbacks(hideTask);
            bar.animate().cancel();
            super.onDetachedFromWindow();
        }
    }

    /** Creates the bottom navigation bar used inside the expandable menu. */
    public View createBottomNavBar(Activity activity, int activeTab, Runnable onOpenSettingsHere) {
        LinearLayout bar = new LinearLayout(activity);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setBackgroundColor(surface);
        bar.setPadding(dp(activity, 8), dp(activity, 6), dp(activity, 8), dp(activity, 6));

        String[] labels = {"Match", "History", "Players", "Settings"};
        String[] descriptions = {
            "Match, tab 1 of 4",
            "History, tab 2 of 4",
            "Players, tab 3 of 4",
            "Settings, tab 4 of 4"
        };

        for (int i = 0; i < labels.length; i++) {
            final int tab = i;
            boolean selected = (i == activeTab);
            Button item = new Button(activity);
            item.setAllCaps(false);
            item.setSingleLine(true);
            item.setEllipsize(TextUtils.TruncateAt.END);
            item.setText(labels[i]);
            item.setTextSize(14);
            item.setTypeface(selected ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
            item.setTextColor(selected ? onSecondaryContainer : onSurfaceVariant);
            item.setContentDescription(descriptions[i] + (selected ? ", selected" : ""));
            item.setMinHeight(dp(activity, 48));
            item.setMinimumHeight(dp(activity, 48));
            item.setPadding(dp(activity, 2), 0, dp(activity, 2), 0);
            item.setBackground(interactiveBackground(
                activity,
                selected ? secondaryContainer : Color.TRANSPARENT,
                20,
                selected ? 1 : 0,
                selected ? outlineVariant : 0
            ));

            item.setOnClickListener(v -> {
                if (tab == activeTab && tab != TAB_SETTINGS) return;
                if (tab == TAB_MATCH) {
                    Intent intent = new Intent(activity, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    activity.startActivity(intent);
                    if (!(activity instanceof MainActivity)) activity.finish();
                } else if (tab == TAB_HISTORY) {
                    if (!(activity instanceof HistoryActivity)) {
                        activity.startActivity(new Intent(activity, HistoryActivity.class));
                        if (!(activity instanceof MainActivity)) activity.finish();
                    }
                } else if (tab == TAB_PLAYERS) {
                    if (!(activity instanceof PlayerStatsActivity)) {
                        activity.startActivity(new Intent(activity, PlayerStatsActivity.class));
                        if (!(activity instanceof MainActivity)) activity.finish();
                    }
                } else if (tab == TAB_SETTINGS) {
                    if (onOpenSettingsHere != null) {
                        onOpenSettingsHere.run();
                    } else {
                        Intent intent = new Intent(activity, MainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        intent.putExtra(EXTRA_OPEN_SETTINGS, true);
                        activity.startActivity(intent);
                        activity.finish();
                    }
                }
            });

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(activity, 48), 1f);
            lp.setMargins(dp(activity, 2), 0, dp(activity, 2), 0);
            bar.addView(item, lp);
            if (i == 1) bar.addView(new View(activity), new LinearLayout.LayoutParams(dp(activity, 48), dp(activity, 48)));
        }
        return bar;
    }
}

