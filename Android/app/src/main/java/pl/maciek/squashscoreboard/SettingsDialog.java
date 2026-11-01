package pl.maciek.squashscoreboard;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.text.InputType;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import java.util.List;

/**
 * Shared settings dialog for DropShot.
 * Can be displayed from any Activity without navigating back to Match.
 */
public final class SettingsDialog {
    private SettingsDialog() {}

    public static void show(Activity activity, int initialTab, Runnable onSave) {
        if (activity == null || activity.isFinishing()) return;

        UiTheme theme = new UiTheme(activity);
        int surface = theme.surface;
        int muted = theme.onSurfaceVariant;
        int foreground = theme.onSurface;

        LinearLayout playersForm = settingsPage(activity, surface);
        LinearLayout appearanceForm = settingsPage(activity, surface);
        LinearLayout remoteForm = settingsPage(activity, surface);
        LinearLayout courtForm = settingsPage(activity, surface);

        TextView note = text(activity, "A change to the players or the rules starts a new match. Remote and theme settings apply immediately.", 16, muted);
        note.setPadding(0, 0, 0, dp(activity, 8));
        playersForm.addView(note);

        List<PlayerProfile> profiles = ProfileStore.load(activity);
        String[] choices = new String[profiles.size() + 1];
        choices[0] = "Guest";
        for (int i = 0; i < profiles.size(); i++) {
            choices[i + 1] = profiles.get(i).name + " (" + UiTheme.playerColorName(profiles.get(i).color) + ")";
        }

        String curP1Id = AppPreferences.profileOneId(activity);
        String curP2Id = AppPreferences.profileTwoId(activity);
        String curP1Name = AppPreferences.playerOneName(activity);
        String curP2Name = AppPreferences.playerTwoName(activity);
        int curP1Color = AppPreferences.color(activity, "color1", AppPreferences.BLUE);
        int curP2Color = AppPreferences.color(activity, "color2", AppPreferences.RED);

        Spinner profileOne = spinner(activity, theme, choices, profileIndex(profiles, curP1Id));
        Spinner profileTwo = spinner(activity, theme, choices, profileIndex(profiles, curP2Id));
        EditText nameOne = field(activity, theme, curP1Name, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        EditText nameTwo = field(activity, theme, curP2Name, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        EditText points = field(activity, theme, String.valueOf(AppPreferences.pointsPerSet(activity)), InputType.TYPE_CLASS_NUMBER);
        EditText sets = field(activity, theme, String.valueOf(AppPreferences.setsPerMatch(activity)), InputType.TYPE_CLASS_NUMBER);
        Spinner colorOne = spinner(activity, theme, colorNames(activity), colorIndex(activity, curP1Color));
        Spinner colorTwo = spinner(activity, theme, colorNames(activity), colorIndex(activity, curP2Color));

        LinearLayout nameOneRow = settingRow(activity, surface, muted, "Name of Player 1", nameOne);
        LinearLayout colorOneRow = settingRow(activity, surface, muted, "Color for Player 1", colorOne);
        LinearLayout nameTwoRow = settingRow(activity, surface, muted, "Name of Player 2", nameTwo);
        LinearLayout colorTwoRow = settingRow(activity, surface, muted, "Color for Player 2", colorTwo);

        addSetting(activity, playersForm, muted, "Profile for Player 1", profileOne);
        playersForm.addView(nameOneRow);
        playersForm.addView(colorOneRow);
        addSetting(activity, playersForm, muted, "Profile for Player 2", profileTwo);
        playersForm.addView(nameTwoRow);
        playersForm.addView(colorTwoRow);
        profileOne.setOnItemSelectedListener(profileVisibility(profileOne, nameOneRow, colorOneRow));
        profileTwo.setOnItemSelectedListener(profileVisibility(profileTwo, nameTwoRow, colorTwoRow));

        addSetting(activity, playersForm, muted, "Points to win a game", points);
        addSetting(activity, playersForm, muted, "Games to win the match", sets);

        CheckBox darkBox = checkBox(activity, "Use dark theme", AppPreferences.dark(activity), foreground);
        CheckBox hcBox = checkBox(activity, "Use high-contrast mode", AppPreferences.highContrast(activity), foreground);
        appearanceForm.addView(darkBox);
        appearanceForm.addView(hcBox);

        int curPilotOne = AppPreferences.remote(activity, "remote1", KeyEvent.KEYCODE_VOLUME_UP);
        int curPilotTwo = AppPreferences.remote(activity, "remote2", KeyEvent.KEYCODE_VOLUME_DOWN);
        int curPilotUndo = AppPreferences.remote(activity, "undo", KeyEvent.KEYCODE_MEDIA_PAUSE);
        int curPilotReset = AppPreferences.remote(activity, "reset", KeyEvent.KEYCODE_MEDIA_PLAY);

        Spinner pilotOne = spinner(activity, theme, remoteNames(), remoteIndex(curPilotOne));
        Spinner pilotTwo = spinner(activity, theme, remoteNames(), remoteIndex(curPilotTwo));
        Spinner pilotUndo = spinner(activity, theme, remoteNames(), remoteIndex(curPilotUndo));
        Spinner pilotReset = spinner(activity, theme, remoteNames(), remoteIndex(curPilotReset));
        addSetting(activity, remoteForm, muted, "Remote key for Player 1", pilotOne);
        addSetting(activity, remoteForm, muted, "Remote key for Player 2", pilotTwo);
        addSetting(activity, remoteForm, muted, "Remote key for Undo", pilotUndo);
        addSetting(activity, remoteForm, muted, "Remote key for Reset", pilotReset);

        EditText duration = field(activity, theme, String.valueOf(AppPreferences.courtDuration(activity)), InputType.TYPE_CLASS_NUMBER);
        EditText green = field(activity, theme, String.valueOf(AppPreferences.courtGreen(activity)), InputType.TYPE_CLASS_NUMBER);
        EditText yellow = field(activity, theme, String.valueOf(AppPreferences.courtYellow(activity)), InputType.TYPE_CLASS_NUMBER);
        EditText red = field(activity, theme, String.valueOf(AppPreferences.courtRed(activity)), InputType.TYPE_CLASS_NUMBER);
        addSetting(activity, courtForm, muted, "Rental duration (min)", duration);
        addSetting(activity, courtForm, muted, "Green warning (min before end)", green);
        addSetting(activity, courtForm, muted, "Yellow warning (min before end)", yellow);
        addSetting(activity, courtForm, muted, "Red warning (min before end)", red);
        CheckBox vibration = checkBox(activity, "Vibrate at warnings (1, 2, and 3 pulses)", AppPreferences.courtVibration(activity), foreground);
        courtForm.addView(vibration);

        Button helpBtn = theme.outlinedButton(activity, "Open help and remote guide");
        LinearLayout.LayoutParams helpParams = new LinearLayout.LayoutParams(-1, dp(activity, 48));
        helpParams.topMargin = dp(activity, 8);
        helpBtn.setOnClickListener(v -> showHelpDialog(activity));
        courtForm.addView(helpBtn, helpParams);

        LinearLayout settingsContent = box(activity, surface);
        HorizontalScrollView tabStrip = new HorizontalScrollView(activity);
        tabStrip.setHorizontalScrollBarEnabled(false);
        tabStrip.setFillViewport(true);
        LinearLayout tabs = new LinearLayout(activity);
        tabs.setGravity(Gravity.CENTER_VERTICAL);
        tabStrip.addView(tabs);
        settingsContent.addView(tabStrip, new LinearLayout.LayoutParams(-1, dp(activity, 56)));

        FrameLayout pageFrame = new FrameLayout(activity);
        String[] titles = {"Players & rules", "Appearance", "Remote keys", "Court & help"};
        LinearLayout[] forms = {playersForm, appearanceForm, remoteForm, courtForm};
        ScrollView[] pages = new ScrollView[forms.length];
        Button[] tabButtons = new Button[forms.length];
        for (int i = 0; i < forms.length; i++) {
            final int selected = i;
            pages[i] = new ScrollView(activity);
            pages[i].setFillViewport(true);
            pages[i].addView(forms[i]);
            pages[i].setVisibility(i == 0 ? View.VISIBLE : View.GONE);
            pageFrame.addView(pages[i], new FrameLayout.LayoutParams(-1, -1));

            Button tab = theme.outlinedButton(activity, titles[i]);
            tab.setTextSize(15);
            tab.setSingleLine(true);
            tab.setContentDescription(titles[i] + ", settings tab " + (i + 1) + " of 4");
            tab.setOnClickListener(v -> selectSettingsTab(activity, theme, selected, pages, tabButtons));
            tabButtons[i] = tab;
            LinearLayout.LayoutParams tabParams = new LinearLayout.LayoutParams(-2, dp(activity, 48));
            tabParams.setMargins(dp(activity, 4), 0, dp(activity, 4), 0);
            tabs.addView(tab, tabParams);
        }
        selectSettingsTab(activity, theme, initialTab, pages, tabButtons);
        int pageHeight = Math.min(dp(activity, 480), Math.max(dp(activity, 180), activity.getResources().getDisplayMetrics().heightPixels - dp(activity, 190)));
        settingsContent.addView(pageFrame, new LinearLayout.LayoutParams(-1, pageHeight));
        View[] settingsInputs = {
            profileOne, profileTwo, nameOne, nameTwo, points, sets,
            darkBox, hcBox, colorOne, colorTwo, pilotOne, pilotTwo, pilotUndo, pilotReset,
            duration, green, yellow, red, vibration
        };
        String baseline = settingsSignature(settingsInputs);

        AlertDialog dialog = new AlertDialog.Builder(activity)
            .setTitle("Set up match and settings")
            .setView(settingsContent)
            .setNegativeButton("Cancel action", null)
            .setPositiveButton("Save settings", null)
            .create();

        dialog.setOnShowListener(x -> {
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(surface));
            }
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(theme.primary);
            Button save = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            save.setTextColor(theme.primary);
            save.setOnClickListener(v -> {
                if (saveSettings(
                    activity, profiles, profileOne, profileTwo, nameOne, nameTwo,
                    points, sets, darkBox, hcBox, colorOne, colorTwo,
                    pilotOne, pilotTwo, pilotUndo, pilotReset,
                    duration, green, yellow, red, vibration
                )) {
                    dialog.dismiss();
                    if (onSave != null) onSave.run();
                }
            });
            protectSettingsDialog(activity, dialog, baseline, settingsInputs);
        });
        dialog.show();
    }

    private static boolean saveSettings(
        Activity activity,
        List<PlayerProfile> profiles,
        Spinner profileOne,
        Spinner profileTwo,
        EditText nameOne,
        EditText nameTwo,
        EditText points,
        EditText sets,
        CheckBox darkBox,
        CheckBox hcBox,
        Spinner colorOne,
        Spinner colorTwo,
        Spinner pilotOne,
        Spinner pilotTwo,
        Spinner pilotUndo,
        Spinner pilotReset,
        EditText duration,
        EditText green,
        EditText yellow,
        EditText red,
        CheckBox vibration
    ) {
        try {
            int pts = Integer.parseInt(points.getText().toString().trim());
            int sts = Integer.parseInt(sets.getText().toString().trim());
            if (pts < 1 || sts < 1) throw new NumberFormatException();

            int courtDuration, courtGreen, courtYellow, courtRed;
            try {
                courtDuration = Integer.parseInt(duration.getText().toString().trim());
                courtGreen = Integer.parseInt(green.getText().toString().trim());
                courtYellow = Integer.parseInt(yellow.getText().toString().trim());
                courtRed = Integer.parseInt(red.getText().toString().trim());
                if (courtDuration < 1 || courtDuration > 1440 || courtRed < 0
                    || courtRed >= courtYellow || courtYellow >= courtGreen || courtGreen >= courtDuration) {
                    throw new NumberFormatException();
                }
            } catch (NumberFormatException error) {
                new AlertDialog.Builder(activity)
                    .setTitle("Invalid court timer values")
                    .setMessage("The court timer values must follow 0 <= red < yellow < green < duration (maximum 1440 min). Enter whole numbers in min.")
                    .setPositiveButton("Close dialog", null)
                    .show();
                return false;
            }

            String nextProfileOneId = selectedProfileId(profiles, profileOne);
            String nextProfileTwoId = selectedProfileId(profiles, profileTwo);
            PlayerProfile one = ProfileStore.find(activity, nextProfileOneId);
            PlayerProfile two = ProfileStore.find(activity, nextProfileTwoId);
            String nextNameOne = one == null ? InputValidator.sanitizePlayerName(nameOne.getText().toString(), "PLAYER 1") : one.name;
            String nextNameTwo = two == null ? InputValidator.sanitizePlayerName(nameTwo.getText().toString(), "PLAYER 2") : two.name;
            int c1 = one == null ? colorAt(activity, colorOne.getSelectedItemPosition()) : one.color;
            int c2 = two == null ? colorAt(activity, colorTwo.getSelectedItemPosition()) : two.color;

            AppPreferences.save(
                activity,
                darkBox.isChecked(),
                c1,
                c2,
                remoteAt(pilotOne.getSelectedItemPosition()),
                remoteAt(pilotTwo.getSelectedItemPosition())
            );
            AppPreferences.saveAppearance(activity, darkBox.isChecked(), hcBox.isChecked());
            AppPreferences.saveRules(activity, pts, sts);
            AppPreferences.saveExtra(
                activity,
                remoteAt(pilotUndo.getSelectedItemPosition()),
                remoteAt(pilotReset.getSelectedItemPosition())
            );
            AppPreferences.saveCourtSettings(activity, courtDuration, courtGreen, courtYellow, courtRed);
            AppPreferences.saveCourtVibration(activity, vibration.isChecked());
            AppPreferences.savePlayers(activity, nextProfileOneId, nextProfileTwoId, nextNameOne, nextNameTwo);
            return true;
        } catch (Exception e) {
            new AlertDialog.Builder(activity)
                .setTitle("Invalid input")
                .setMessage("Please check that points and games are valid positive numbers.")
                .setPositiveButton("Close dialog", null)
                .show();
            return false;
        }
    }

    private static void showHelpDialog(Activity activity) {
        String guide =
            "1. SCORE ON THE SCREEN\n" +
            "1. Tap the Player 1 panel to add 1 point to Player 1.\n" +
            "2. Tap the Player 2 panel to add 1 point to Player 2.\n" +
            "3. Tap Undo point to remove the last point.\n" +
            "The app awards a game at 11 points with a 2-point lead.\n\n" +
            "2. USE A BLUETOOTH REMOTE CONTROL\n" +
            "WARNING: The volume keys change the score only while the Match screen is open.\n" +
            "1. Pair the Bluetooth remote control in the Android settings.\n" +
            "2. Press Volume + for Player 1 or Volume - for Player 2.\n" +
            "3. Press Pause to undo 1 point, or press Play to start a new match.\n\n" +
            "3. CONNECT THE WATCH APPS\n" +
            "1. Android Watch: Install DropShot Watch and connect via Bluetooth.\n" +
            "2. Amazfit Watch: Pair with Zepp app and open Punkty Squash.\n" +
            "3. Tap player tiles to add points, or tap serve badge to switch side.";

        new AlertDialog.Builder(activity)
            .setTitle("Help and remote control guide")
            .setMessage(guide)
            .setPositiveButton("Close guide", null)
            .show();
    }

    private static CheckBox checkBox(Context context, String label, boolean checked, int color) {
        CheckBox box = new CheckBox(context);
        box.setText(label);
        box.setTextSize(16);
        box.setTextColor(color);
        box.setMinHeight(dp(context, 48));
        box.setMinimumHeight(dp(context, 48));
        box.setChecked(checked);
        return box;
    }

    private static LinearLayout settingsPage(Context context, int surface) {
        LinearLayout page = box(context, surface);
        page.setPadding(dp(context, 20), dp(context, 12), dp(context, 20), dp(context, 12));
        return page;
    }

    private static void selectSettingsTab(Activity activity, UiTheme theme, int selected, ScrollView[] pages, Button[] tabs) {
        for (int i = 0; i < tabs.length; i++) {
            boolean active = i == selected;
            pages[i].setVisibility(active ? View.VISIBLE : View.GONE);
            tabs[i].setSelected(active);
            tabs[i].setTypeface(active ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
            tabs[i].setTextColor(active ? theme.onSecondaryContainer : theme.onSurface);
            tabs[i].setBackground(theme.interactiveBackground(
                activity,
                active ? theme.secondaryContainer : theme.surfaceLow,
                24,
                1,
                active ? theme.primary : theme.outline
            ));
        }
    }

    private static AdapterView.OnItemSelectedListener profileVisibility(final Spinner spinner, final View... rows) {
        return new AdapterView.OnItemSelectedListener() {
            @Override public void onNothingSelected(AdapterView<?> p) {}
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                for (View row : rows) row.setVisibility(pos == 0 ? View.VISIBLE : View.GONE);
            }
        };
    }

    private static int profileIndex(List<PlayerProfile> p, String id) {
        if (id != null) {
            for (int i = 0; i < p.size(); i++) {
                if (id.equals(p.get(i).id)) return i + 1;
            }
        }
        return 0;
    }

    private static LinearLayout settingRow(Context context, int surface, int muted, String label, View input) {
        LinearLayout row = box(context, surface);
        TextView c = text(context, label, 16, muted);
        c.setPadding(0, dp(context, 8), 0, dp(context, 2));
        row.addView(c);
        row.addView(input, new LinearLayout.LayoutParams(-1, dp(context, 48)));
        return row;
    }

    private static void addSetting(Context context, LinearLayout f, int muted, String label, View input) {
        TextView c = text(context, label, 16, muted);
        c.setPadding(0, dp(context, 8), 0, dp(context, 2));
        f.addView(c);
        f.addView(input, new LinearLayout.LayoutParams(-1, dp(context, 48)));
    }

    private static Spinner spinner(Context context, UiTheme theme, String[] values, int selected) {
        return theme.styledSpinner(context, values, selected);
    }

    private static EditText field(Context context, UiTheme theme, String value, int type) {
        return theme.editField(context, value, null, type);
    }

    private static String settingsSignature(View... inputs) {
        StringBuilder signature = new StringBuilder();
        for (View input : inputs) {
            if (input instanceof EditText) {
                String value = ((EditText) input).getText().toString();
                signature.append('E').append(value.length()).append(':').append(value);
            } else if (input instanceof Spinner) {
                signature.append('S').append(((Spinner) input).getSelectedItemPosition());
            } else if (input instanceof CompoundButton) {
                signature.append('B').append(((CompoundButton) input).isChecked());
            }
            signature.append(';');
        }
        return signature.toString();
    }

    private static void leaveSettings(Activity activity, AlertDialog dialog, String initial, Runnable after, View... inputs) {
        if (initial.equals(settingsSignature(inputs))) {
            dialog.dismiss();
            if (after != null) after.run();
            return;
        }
        new AlertDialog.Builder(activity)
            .setTitle("Discard unsaved changes?")
            .setMessage("You changed the settings. If you leave now, the app will not save the changes.")
            .setNegativeButton("Keep editing", null)
            .setPositiveButton("Discard changes", (ignored, which) -> {
                dialog.dismiss();
                if (after != null) after.run();
            })
            .show();
    }

    private static void protectSettingsDialog(Activity activity, AlertDialog dialog, String initial, View... inputs) {
        dialog.setCanceledOnTouchOutside(false);
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(v -> leaveSettings(activity, dialog, initial, null, inputs));
        dialog.setOnKeyListener((ignored, key, event) -> {
            if (key != KeyEvent.KEYCODE_BACK) return false;
            if (event.getAction() == KeyEvent.ACTION_UP) leaveSettings(activity, dialog, initial, null, inputs);
            return true;
        });
    }

    private static String selectedProfileId(List<PlayerProfile> p, Spinner s) {
        int i = s.getSelectedItemPosition();
        return i == 0 ? null : p.get(i - 1).id;
    }

    private static String[] colorNames(Context context) { return UiTheme.playerColorNames(context); }
    private static int colorIndex(Context context, int c) {
        int[] x = UiTheme.playerColors(context);
        for (int i = 0; i < x.length; i++) if (x[i] == c) return i;
        return 0;
    }
    private static int colorAt(Context context, int i) { return UiTheme.playerColors(context)[i]; }

    private static String[] remoteNames() {
        return new String[]{"Volume +", "Volume -", "Next track", "Previous track"};
    }

    private static int[] remotes() {
        return new int[]{
            KeyEvent.KEYCODE_VOLUME_UP,
            KeyEvent.KEYCODE_VOLUME_DOWN,
            KeyEvent.KEYCODE_MEDIA_NEXT,
            KeyEvent.KEYCODE_MEDIA_PREVIOUS
        };
    }

    private static int remoteAt(int i) { return remotes()[i]; }
    private static int remoteIndex(int k) {
        int[] r = remotes();
        for (int i = 0; i < r.length; i++) if (r[i] == k) return i;
        return 0;
    }

    private static int dp(Context context, int dp) {
        return (int) (dp * context.getResources().getDisplayMetrics().density + 0.5f);
    }

    private static LinearLayout box(Context context, int color) {
        LinearLayout box = new LinearLayout(context);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setBackgroundColor(color);
        return box;
    }

    private static TextView text(Context context, String text, int sizeSp, int color) {
        TextView tv = new TextView(context);
        tv.setText(text);
        tv.setTextSize(sizeSp);
        tv.setTextColor(color);
        return tv;
    }
}
