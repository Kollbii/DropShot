package pl.maciek.squashscoreboard;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.InputType;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import pl.maciek.squashscoreboard.lootbox.RewardCatalog;

public class MainActivity extends Activity {
    private UiTheme theme;
    private int background, surface, foreground, muted, playerOneColor, playerTwoColor, remoteOne, remoteTwo, remoteUndo, remoteReset;
    private boolean darkTheme, highContrast, paused;
    private final MatchState match = new MatchState();
    private String playerOneName = "PLAYER 1", playerTwoName = "PLAYER 2", playerOneProfileId, playerTwoProfileId;
    private long currentMatchId, matchElapsedMs, currentSetElapsedMs, lastClockMs;
    private final ArrayList<Long> setDurationsMs = new ArrayList<>();
    private final ArrayList<Long> pointTimesMs = new ArrayList<>();
    private TextView playerOnePoints, playerTwoPoints, playerOneSets, playerTwoSets, status, playerOneTitle, playerTwoTitle;
    private LinearLayout playerOnePanelView, playerTwoPanelView;
    private Button pauseButton, undoButton;
    private TextView pauseOverlay;
    private MediaSession mediaSession;
    private WatchCommandServer watchCommandServer;
    private GradientDrawable playerOneBackground, playerTwoBackground;
    private MatchFinishedView finishedOverlay;
    private FireworksView fireworksView;
    private ValueAnimator playerOnePulse, playerTwoPulse;
    private Button courtButton;
    private GradientDrawable courtBackground;
    private CourtAlertView courtAlertView;
    private UiTheme.CollapsibleBottomNav bottomNav;
    private long courtStartMs;
    private final Handler clockHandler = new Handler(Looper.getMainLooper());
    private final Runnable clockTicker = new Runnable() {
        @Override public void run() {
            if (!paused && !match.isMatchFinished()) tickClock();
            refreshScreen();
            clockHandler.postDelayed(this, 1000);
        }
    };

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        loadStyle();
        match.configure(AppPreferences.pointsPerSet(this), AppPreferences.setsPerMatch(this));
        courtStartMs = AppPreferences.courtStart(this);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        );
        setContentView(createContent());
        currentMatchId = System.currentTimeMillis();
        startClock();
        restoreMatchFromIntent(getIntent());
        createMediaSession();
        watchCommandServer = new WatchCommandServer(new Handler(Looper.getMainLooper()), this::handleWatchCommand);
        refreshScreen();
        if (getIntent() != null && getIntent().getBooleanExtra(UiTheme.EXTRA_OPEN_SETTINGS, false)) {
            getIntent().removeExtra(UiTheme.EXTRA_OPEN_SETTINGS);
            clockHandler.post(this::showSettings);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (intent != null && intent.getStringExtra(MatchDetailActivity.EXTRA_RECORD) != null) {
            restoreMatchFromIntent(intent);
            loadStyle();
            setContentView(createContent());
            refreshScreen();
        } else if (intent != null && intent.getBooleanExtra(UiTheme.EXTRA_OPEN_SETTINGS, false)) {
            intent.removeExtra(UiTheme.EXTRA_OPEN_SETTINGS);
            showSettings();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        loadStyle();
        setContentView(createContent());
        refreshScreen();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mediaSession != null) mediaSession.setActive(true);
        if (watchCommandServer != null) watchCommandServer.start();
        clockHandler.removeCallbacks(clockTicker);
        clockHandler.post(clockTicker);
        refreshScreen();
    }

    @Override
    protected void onPause() {
        if (watchCommandServer != null) watchCommandServer.stop();
        pauseMatch();
        hideFireworks();
        stopBallPulse(1);
        stopBallPulse(2);
        if (mediaSession != null) mediaSession.setActive(false);
        super.onPause();
    }

    @Override
    protected void onStop() {
        archiveCurrentMatch();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        clockHandler.removeCallbacks(clockTicker);
        if (watchCommandServer != null) watchCommandServer.stop();
        if (mediaSession != null) mediaSession.release();
        super.onDestroy();
    }

    @Override public void onUserInteraction() {
        super.onUserInteraction();
        if (bottomNav != null) bottomNav.onUserInteraction();
    }

    private View createContent() {
        boolean portrait = getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
        LinearLayout root = box(background);
        root.setPadding(dp(16), dp(8), dp(16), dp(16));

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);

        status = text("", 16, muted);
        status.setGravity(Gravity.CENTER);
        status.setSingleLine(true);
        status.setEllipsize(TextUtils.TruncateAt.END);
        UiTheme.applyTabularNumbers(status);
        status.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);

        courtButton = button("Start court timer", surface);
        courtButton.setTextSize(15);
        courtButton.setAllCaps(false);
        courtButton.setMinWidth(dp(48));
        courtButton.setMinimumWidth(dp(48));
        courtButton.setMinHeight(dp(48));
        courtButton.setMinimumHeight(dp(48));
        courtButton.setPadding(dp(12), 0, dp(12), 0);
        courtButton.setContentDescription("Start court timer. Court timer is off.");
        courtBackground = new GradientDrawable();
        courtBackground.setColor(surface);
        courtBackground.setCornerRadius(dp(12));
        courtBackground.setStroke(dp(2), theme.outline);
        courtButton.setBackground(courtBackground);
        courtButton.setOnClickListener(v -> onCourtTimerClick());
        courtButton.setOnLongClickListener(v -> {
            showSettings(3);
            return true;
        });

        Button pause = iconButton("Ⅱ", "Pause match.", surface);
        pause.setOnClickListener(v -> togglePause());
        pauseButton = pause;

        Button undo = iconButton("↶", "Undo point.", surface);
        undo.setOnClickListener(v -> {
            if (!paused && match.hasPoints()) {
                int before = match.getPlayerOneSets() + match.getPlayerTwoSets();
                match.undo();
                restoreSetAfterUndo(before);
                refreshScreen();
            }
        });
        undoButton = undo;

        Button reset = iconButton("↺", "Start new match.", theme.playerPanelFill(AppPreferences.GREEN));
        reset.setOnClickListener(v -> confirmReset());

        header.addView(courtButton, new LinearLayout.LayoutParams(-2, dp(48)));
        header.addView(status, new LinearLayout.LayoutParams(0, dp(48), 1));
        header.addView(pause, actionBtnParams());
        header.addView(undo, actionBtnParams());
        header.addView(reset, actionBtnParams());
        root.addView(header, new LinearLayout.LayoutParams(-1, dp(52)));

        LinearLayout scores = new LinearLayout(this);
        scores.setOrientation(portrait ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);
        scores.setGravity(Gravity.CENTER);
        if (!portrait) scores.setPadding(0, dp(8), 0, 0);

        LinearLayout.LayoutParams first = portrait
            ? new LinearLayout.LayoutParams(-1, 0, 1)
            : new LinearLayout.LayoutParams(0, -1, 1);
        LinearLayout.LayoutParams second = portrait
            ? new LinearLayout.LayoutParams(-1, 0, 1)
            : new LinearLayout.LayoutParams(0, -1, 1);
        if (portrait) {
            first.setMargins(0, dp(6), 0, dp(4));
            second.setMargins(0, dp(4), 0, 0);
        } else {
            first.setMargins(0, 0, dp(6), 0);
            second.setMargins(dp(6), 0, 0, 0);
        }
        scores.addView(playerPanel(1, playerOneColor), first);
        scores.addView(playerPanel(2, playerTwoColor), second);
        root.addView(scores, new LinearLayout.LayoutParams(-1, 0, 1));

        FrameLayout screen = new FrameLayout(this);
        screen.setBackgroundColor(background);
        screen.addView(root, new FrameLayout.LayoutParams(-1, -1));
        courtAlertView = new CourtAlertView(this);
        screen.addView(courtAlertView, new FrameLayout.LayoutParams(-1, -1));
        bottomNav = theme.createCollapsibleBottomNav(this, UiTheme.TAB_MATCH, this::showSettings);
        screen.addView(bottomNav, new FrameLayout.LayoutParams(-1, dp(56), Gravity.BOTTOM));
        return screen;
    }

    private LinearLayout.LayoutParams actionBtnParams() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(dp(48), dp(48));
        p.leftMargin = dp(4);
        return p;
    }

    private View playerPanel(int player, int rawColor) {
        int panelFill = theme.playerPanelFill(rawColor);
        int panelAccent = theme.playerAccentOnSurface(rawColor);
        LinearLayout panel = box(panelFill);
        panel.setPadding(dp(16), dp(10), dp(16), dp(10));
        panel.setGravity(Gravity.CENTER);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(panelFill);
        bg.setCornerRadius(dp(16));
        bg.setStroke(dp(3), panelAccent);
        panel.setBackground(bg);
        RewardCatalog.Reward cosmetic = RewardCatalog.byKey(rawColor);
        if (cosmetic != null && cosmetic.colors.length > 1)
            RewardCatalog.configure(panel, bg, cosmetic, true);
        panel.setOnClickListener(v -> addPoint(player));
        panel.setOnLongClickListener(v -> {
            showSettings();
            return true;
        });

        String nameText = player == 1 ? playerOneName : playerTwoName;
        TextView title = text(nameText, 24, Color.WHITE);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER);
        title.setSingleLine(true);
        title.setEllipsize(TextUtils.TruncateAt.END);

        TextView sets = text("GAMES 0", 26, 0xFFF4F0E9);
        sets.setTypeface(Typeface.DEFAULT_BOLD);
        sets.setGravity(Gravity.CENTER);
        UiTheme.applyTabularNumbers(sets);

        TextView points = text("0", 108, Color.WHITE);
        points.setTypeface(Typeface.DEFAULT_BOLD);
        points.setGravity(Gravity.CENTER);
        UiTheme.applyTabularNumbers(points);

        TextView hint = text("TAP PANEL TO ADD 1 POINT", 16, 0xFFF4F0E9);
        hint.setGravity(Gravity.CENTER);
        hint.setSingleLine(true);

        panel.addView(title, new LinearLayout.LayoutParams(-1, dp(36)));
        panel.addView(sets, new LinearLayout.LayoutParams(-1, dp(38)));
        panel.addView(points, new LinearLayout.LayoutParams(-1, 0, 1));
        panel.addView(hint, new LinearLayout.LayoutParams(-1, dp(26)));

        if (player == 1) {
            playerOnePoints = points;
            playerOneSets = sets;
            playerOneTitle = title;
            playerOneBackground = bg;
            playerOnePanelView = panel;
        } else {
            playerTwoPoints = points;
            playerTwoSets = sets;
            playerTwoTitle = title;
            playerTwoBackground = bg;
            playerTwoPanelView = panel;
        }
        return panel;
    }

    private void showSettings() {
        showSettings(0);
    }

    private void showSettings(int initialTab) {
        LinearLayout playersForm = settingsPage();
        LinearLayout appearanceForm = settingsPage();
        LinearLayout remoteForm = settingsPage();
        LinearLayout courtForm = settingsPage();

        TextView note = text("A change to the players or the rules starts a new match. Remote and theme settings apply immediately.", 16, muted);
        note.setPadding(0, 0, 0, dp(8));
        playersForm.addView(note);

        List<PlayerProfile> profiles = ProfileStore.load(this);
        String[] choices = new String[profiles.size() + 1];
        choices[0] = "Guest";
        for (int i = 0; i < profiles.size(); i++) {
            choices[i + 1] = profiles.get(i).name + " (" + UiTheme.playerColorName(profiles.get(i).color) + ")";
        }

        Spinner profileOne = spinner(choices, profileIndex(profiles, playerOneProfileId));
        Spinner profileTwo = spinner(choices, profileIndex(profiles, playerTwoProfileId));
        EditText nameOne = field(playerOneName, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        EditText nameTwo = field(playerTwoName, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        EditText points = field(String.valueOf(match.getPointsToWinSet()), InputType.TYPE_CLASS_NUMBER);
        EditText sets = field(String.valueOf(match.getSetsToWinMatch()), InputType.TYPE_CLASS_NUMBER);
        Spinner colorOne = spinner(colorNames(), colorIndex(playerOneColor));
        Spinner colorTwo = spinner(colorNames(), colorIndex(playerTwoColor));

        LinearLayout nameOneRow = settingRow("Name of Player 1", nameOne);
        LinearLayout colorOneRow = settingRow("Color for Player 1", colorOne);
        LinearLayout nameTwoRow = settingRow("Name of Player 2", nameTwo);
        LinearLayout colorTwoRow = settingRow("Color for Player 2", colorTwo);

        addSetting(playersForm, "Profile for Player 1", profileOne);
        playersForm.addView(nameOneRow);
        playersForm.addView(colorOneRow);
        addSetting(playersForm, "Profile for Player 2", profileTwo);
        playersForm.addView(nameTwoRow);
        playersForm.addView(colorTwoRow);
        profileOne.setOnItemSelectedListener(profileVisibility(profileOne, nameOneRow, colorOneRow));
        profileTwo.setOnItemSelectedListener(profileVisibility(profileTwo, nameTwoRow, colorTwoRow));

        addSetting(playersForm, "Points to win a game", points);
        addSetting(playersForm, "Games to win the match", sets);

        CheckBox darkBox = checkBox("Use dark theme", darkTheme);
        CheckBox hcBox = checkBox("Use high-contrast mode", highContrast);
        appearanceForm.addView(darkBox);
        appearanceForm.addView(hcBox);

        Spinner pilotOne = spinner(remoteNames(), remoteIndex(remoteOne));
        Spinner pilotTwo = spinner(remoteNames(), remoteIndex(remoteTwo));
        Spinner pilotUndo = spinner(remoteNames(), remoteIndex(remoteUndo));
        Spinner pilotReset = spinner(remoteNames(), remoteIndex(remoteReset));
        addSetting(remoteForm, "Remote key for Player 1", pilotOne);
        addSetting(remoteForm, "Remote key for Player 2", pilotTwo);
        addSetting(remoteForm, "Remote key for Undo", pilotUndo);
        addSetting(remoteForm, "Remote key for Reset", pilotReset);

        EditText duration = field(String.valueOf(AppPreferences.courtDuration(this)), InputType.TYPE_CLASS_NUMBER);
        EditText green = field(String.valueOf(AppPreferences.courtGreen(this)), InputType.TYPE_CLASS_NUMBER);
        EditText yellow = field(String.valueOf(AppPreferences.courtYellow(this)), InputType.TYPE_CLASS_NUMBER);
        EditText red = field(String.valueOf(AppPreferences.courtRed(this)), InputType.TYPE_CLASS_NUMBER);
        addSetting(courtForm, "Rental duration (min)", duration);
        addSetting(courtForm, "Green warning (min before end)", green);
        addSetting(courtForm, "Yellow warning (min before end)", yellow);
        addSetting(courtForm, "Red warning (min before end)", red);
        CheckBox vibration = checkBox("Vibrate at warnings (1, 2, and 3 pulses)", AppPreferences.courtVibration(this));
        courtForm.addView(vibration);

        Button helpBtn = theme.outlinedButton(this, "Open help and remote guide");
        LinearLayout.LayoutParams helpParams = new LinearLayout.LayoutParams(-1, dp(48));
        helpParams.topMargin = dp(8);
        helpBtn.setOnClickListener(v -> showHelpDialog());
        courtForm.addView(helpBtn, helpParams);

        LinearLayout settingsContent = box(surface);
        HorizontalScrollView tabStrip = new HorizontalScrollView(this);
        tabStrip.setHorizontalScrollBarEnabled(false);
        tabStrip.setFillViewport(true);
        LinearLayout tabs = new LinearLayout(this);
        tabs.setGravity(Gravity.CENTER_VERTICAL);
        tabStrip.addView(tabs);
        settingsContent.addView(tabStrip, new LinearLayout.LayoutParams(-1, dp(56)));

        FrameLayout pageFrame = new FrameLayout(this);
        String[] titles = {"Players & rules", "Appearance", "Remote keys", "Court & help"};
        LinearLayout[] forms = {playersForm, appearanceForm, remoteForm, courtForm};
        ScrollView[] pages = new ScrollView[forms.length];
        Button[] tabButtons = new Button[forms.length];
        for (int i = 0; i < forms.length; i++) {
            final int selected = i;
            pages[i] = new ScrollView(this);
            pages[i].setFillViewport(true);
            pages[i].addView(forms[i]);
            pages[i].setVisibility(i == 0 ? View.VISIBLE : View.GONE);
            pageFrame.addView(pages[i], new FrameLayout.LayoutParams(-1, -1));

            Button tab = theme.outlinedButton(this, titles[i]);
            tab.setTextSize(15);
            tab.setSingleLine(true);
            tab.setContentDescription(titles[i] + ", settings tab " + (i + 1) + " of 4");
            tab.setOnClickListener(v -> selectSettingsTab(selected, pages, tabButtons));
            tabButtons[i] = tab;
            LinearLayout.LayoutParams tabParams = new LinearLayout.LayoutParams(-2, dp(48));
            tabParams.setMargins(dp(4), 0, dp(4), 0);
            tabs.addView(tab, tabParams);
        }
        selectSettingsTab(initialTab, pages, tabButtons);
        int pageHeight = Math.min(dp(480), Math.max(dp(180), getResources().getDisplayMetrics().heightPixels - dp(190)));
        settingsContent.addView(pageFrame, new LinearLayout.LayoutParams(-1, pageHeight));
        View[] settingsInputs = {
            profileOne, profileTwo, nameOne, nameTwo, points, sets,
            darkBox, hcBox, colorOne, colorTwo, pilotOne, pilotTwo, pilotUndo, pilotReset,
            duration, green, yellow, red, vibration
        };
        String baseline = settingsSignature(settingsInputs);

        AlertDialog dialog = new AlertDialog.Builder(this)
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
                    profiles, profileOne, profileTwo, nameOne, nameTwo,
                    points, sets, darkBox, hcBox, colorOne, colorTwo,
                    pilotOne, pilotTwo, pilotUndo, pilotReset,
                    duration, green, yellow, red, vibration
                )) {
                    dialog.dismiss();
                }
            });
            protectSettingsDialog(dialog, baseline, settingsInputs);
        });
        dialog.show();
    }

    private void showHelpDialog() {
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
            "3. CONNECT THE ZEPP OS WATCH\n" +
            "WARNING: If the watch loses the Bluetooth connection, the watch does not send the command again. This rule prevents a late point after reconnection.\n" +
            "1. Pair the Amazfit watch with the Zepp app on the phone.\n" +
            "2. Keep the Match screen open in DropShot (port 127.0.0.1:28282).\n" +
            "3. Open Punkty Squash on the watch and tap +1, +2, <<, or R.";

        new AlertDialog.Builder(this)
            .setTitle("Help and remote control guide")
            .setMessage(guide)
            .setPositiveButton("Close guide", null)
            .show();
    }

    private CheckBox checkBox(String label, boolean checked) {
        CheckBox box = new CheckBox(this);
        box.setText(label);
        box.setTextSize(16);
        box.setTextColor(foreground);
        box.setMinHeight(dp(48));
        box.setMinimumHeight(dp(48));
        box.setChecked(checked);
        return box;
    }

    private LinearLayout settingsPage() {
        LinearLayout page = box(surface);
        page.setPadding(dp(20), dp(12), dp(20), dp(12));
        return page;
    }

    private void selectSettingsTab(int selected, ScrollView[] pages, Button[] tabs) {
        for (int i = 0; i < tabs.length; i++) {
            boolean active = i == selected;
            pages[i].setVisibility(active ? View.VISIBLE : View.GONE);
            tabs[i].setSelected(active);
            tabs[i].setTypeface(active ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
            tabs[i].setTextColor(active ? theme.onSecondaryContainer : theme.onSurface);
            tabs[i].setBackground(theme.interactiveBackground(
                this,
                active ? theme.secondaryContainer : theme.surfaceLow,
                24,
                1,
                active ? theme.primary : theme.outline
            ));
        }
    }

    private AdapterView.OnItemSelectedListener profileVisibility(final Spinner spinner, final View... rows) {
        return new AdapterView.OnItemSelectedListener() {
            @Override public void onNothingSelected(AdapterView<?> p) { }
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                for (View row : rows) row.setVisibility(pos == 0 ? View.VISIBLE : View.GONE);
            }
        };
    }

    private int profileIndex(List<PlayerProfile> p, String id) {
        if (id != null) {
            for (int i = 0; i < p.size(); i++) {
                if (id.equals(p.get(i).id)) return i + 1;
            }
        }
        return 0;
    }

    private LinearLayout settingRow(String label, View input) {
        LinearLayout row = box(surface);
        TextView c = text(label, 16, muted);
        c.setPadding(0, dp(8), 0, dp(2));
        row.addView(c);
        row.addView(input, new LinearLayout.LayoutParams(-1, dp(48)));
        return row;
    }

    private void addSetting(LinearLayout f, String label, EditText input) {
        TextView c = text(label, 16, muted);
        c.setPadding(0, dp(8), 0, dp(2));
        f.addView(c);
        f.addView(input, new LinearLayout.LayoutParams(-1, dp(48)));
    }

    private void addSetting(LinearLayout f, String label, Spinner input) {
        TextView c = text(label, 16, muted);
        c.setPadding(0, dp(8), 0, dp(2));
        f.addView(c);
        f.addView(input, new LinearLayout.LayoutParams(-1, dp(48)));
    }

    private Spinner spinner(String[] values, int selected) {
        return theme.styledSpinner(this, values, selected);
    }

    private EditText field(String value, int type) {
        return theme.editField(this, value, null, type);
    }

    private String settingsSignature(View... inputs) {
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

    private void leaveSettings(AlertDialog dialog, String initial, Runnable after, View... inputs) {
        if (initial.equals(settingsSignature(inputs))) {
            dialog.dismiss();
            if (after != null) after.run();
            return;
        }
        new AlertDialog.Builder(this)
            .setTitle("Discard unsaved changes?")
            .setMessage("You changed the settings. If you leave now, the app will not save the changes.")
            .setNegativeButton("Keep editing", null)
            .setPositiveButton("Discard changes", (ignored, which) -> {
                dialog.dismiss();
                if (after != null) after.run();
            })
            .show();
    }

    private void protectSettingsDialog(AlertDialog dialog, String initial, View... inputs) {
        dialog.setCanceledOnTouchOutside(false);
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(v -> leaveSettings(dialog, initial, null, inputs));
        dialog.setOnKeyListener((ignored, key, event) -> {
            if (key != KeyEvent.KEYCODE_BACK) return false;
            if (event.getAction() == KeyEvent.ACTION_UP) leaveSettings(dialog, initial, null, inputs);
            return true;
        });
    }

    private boolean saveSettings(
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
                new AlertDialog.Builder(this)
                    .setTitle("Invalid court timer values")
                    .setMessage("The court timer values must follow 0 <= red < yellow < green < duration (maximum 1440 min). Enter whole numbers in min.")
                    .setPositiveButton("Close dialog", null)
                    .show();
                return false;
            }

            String nextProfileOneId = selectedProfileId(profiles, profileOne);
            String nextProfileTwoId = selectedProfileId(profiles, profileTwo);
            PlayerProfile one = ProfileStore.find(this, nextProfileOneId);
            PlayerProfile two = ProfileStore.find(this, nextProfileTwoId);
            String nextNameOne = one == null ? cleanName(nameOne.getText().toString(), "PLAYER 1") : one.name;
            String nextNameTwo = two == null ? cleanName(nameTwo.getText().toString(), "PLAYER 2") : two.name;
            int c1 = one == null ? colorAt(colorOne.getSelectedItemPosition()) : one.color;
            int c2 = two == null ? colorAt(colorTwo.getSelectedItemPosition()) : two.color;

            boolean matchConfigChanged =
                pts != match.getPointsToWinSet()
                || sts != match.getSetsToWinMatch()
                || !TextUtils.equals(playerOneProfileId, nextProfileOneId)
                || !TextUtils.equals(playerTwoProfileId, nextProfileTwoId)
                || !TextUtils.equals(playerOneName, nextNameOne)
                || !TextUtils.equals(playerTwoName, nextNameTwo)
                || playerOneColor != c1
                || playerTwoColor != c2;

            playerOneProfileId = nextProfileOneId;
            playerTwoProfileId = nextProfileTwoId;
            playerOneName = nextNameOne;
            playerTwoName = nextNameTwo;

            if (matchConfigChanged) {
                archiveCurrentMatch();
                match.configure(pts, sts);
                resetClock();
                currentMatchId = System.currentTimeMillis();
            }

            AppPreferences.save(
                this,
                darkBox.isChecked(),
                c1,
                c2,
                remoteAt(pilotOne.getSelectedItemPosition()),
                remoteAt(pilotTwo.getSelectedItemPosition())
            );
            AppPreferences.saveAppearance(this, darkBox.isChecked(), hcBox.isChecked());
            AppPreferences.saveRules(this, pts, sts);
            AppPreferences.saveExtra(
                this,
                remoteAt(pilotUndo.getSelectedItemPosition()),
                remoteAt(pilotReset.getSelectedItemPosition())
            );
            AppPreferences.saveCourtSettings(this, courtDuration, courtGreen, courtYellow, courtRed);
            AppPreferences.saveCourtVibration(this, vibration.isChecked());
            loadStyle();
            setContentView(createContent());
            refreshScreen();
            return true;
        } catch (NumberFormatException e) {
            new AlertDialog.Builder(this)
                .setTitle("Invalid rule value")
                .setMessage("The rule value is not valid. Each value must be at least 1. Enter a whole number of 1 or more.")
                .setPositiveButton("Close dialog", null)
                .show();
            return false;
        }
    }

    private String selectedProfileId(List<PlayerProfile> p, Spinner s) {
        int i = s.getSelectedItemPosition();
        return i == 0 ? null : p.get(i - 1).id;
    }

    private String cleanName(String s, String fallback) {
        s = s.trim();
        return s.isEmpty() ? fallback : s.toUpperCase(Locale.getDefault());
    }

    private void createMediaSession() {
        mediaSession = new MediaSession(this, "SquashScoreboard");
        mediaSession.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS | MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);
        mediaSession.setPlaybackState(
            new PlaybackState.Builder()
                .setActions(
                    PlaybackState.ACTION_PLAY_PAUSE
                        | PlaybackState.ACTION_SKIP_TO_NEXT
                        | PlaybackState.ACTION_SKIP_TO_PREVIOUS
                )
                .setState(PlaybackState.STATE_PLAYING, 0, 1f)
                .build()
        );
        mediaSession.setCallback(new MediaSession.Callback() {
            @Override public void onSkipToNext() { addPoint(1); }
            @Override public void onSkipToPrevious() { addPoint(2); }
            @Override public void onPlay() { togglePause(); }
            @Override public void onPause() { togglePause(); }
            @Override public boolean onMediaButtonEvent(Intent i) {
                KeyEvent e = i.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
                return e != null && e.getAction() == KeyEvent.ACTION_DOWN
                    ? handleRemoteKey(e.getKeyCode())
                    : super.onMediaButtonEvent(i);
            }
        });
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent e) {
        return e.getAction() == KeyEvent.ACTION_DOWN && handleRemoteKey(e.getKeyCode())
            || super.dispatchKeyEvent(e);
    }

    private boolean handleRemoteKey(int key) {
        if (finishedOverlay != null) {
            if (key == KeyEvent.KEYCODE_MEDIA_PREVIOUS) { finishedOverlay.triggerBack(); return true; }
            if (key == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE || key == KeyEvent.KEYCODE_MEDIA_PLAY) { finishedOverlay.triggerReset(); return true; }
            if (key == KeyEvent.KEYCODE_MEDIA_NEXT) { finishedOverlay.triggerContinue(); return true; }
        }
        switch (key) {
            case KeyEvent.KEYCODE_VOLUME_UP:
            case KeyEvent.KEYCODE_VOLUME_DOWN:
            case KeyEvent.KEYCODE_MEDIA_NEXT:
            case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                if (key == remoteOne) { addPoint(1); return true; }
                if (key == remoteTwo) { addPoint(2); return true; }
                if (key == remoteUndo && !paused) {
                    int before = match.getPlayerOneSets() + match.getPlayerTwoSets();
                    match.undo();
                    restoreSetAfterUndo(before);
                    refreshScreen();
                    return true;
                }
                if (key == remoteReset) { confirmReset(); return true; }
                return true;
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
            case KeyEvent.KEYCODE_MEDIA_PLAY:
                togglePause();
                return true;
            case KeyEvent.KEYCODE_MEDIA_PAUSE:
                pauseMatch();
                return true;
            default:
                return false;
        }
    }

    private boolean handleWatchCommand(String command) {
        if ("POINT_1".equals(command)) {
            if (paused || match.isMatchFinished()) return false;
            addPoint(1);
            return true;
        }
        if ("POINT_2".equals(command)) {
            if (paused || match.isMatchFinished()) return false;
            addPoint(2);
            return true;
        }
        if ("UNDO".equals(command)) {
            if (paused) return false;
            int before = match.getPlayerOneSets() + match.getPlayerTwoSets();
            match.undo();
            restoreSetAfterUndo(before);
            refreshScreen();
            return true;
        }
        if ("RESET".equals(command)) {
            resetMatchSafely();
            return true;
        }
        return false;
    }

    private void addPoint(int player) {
        if (paused || match.isMatchFinished()) return;
        tickClock();
        int oldSets = match.getPlayerOneSets() + match.getPlayerTwoSets();
        match.addPoint(player);
        pointTimesMs.add(matchElapsedMs);
        if (match.getPlayerOneSets() + match.getPlayerTwoSets() > oldSets) {
            setDurationsMs.add(currentSetElapsedMs);
            currentSetElapsedMs = 0;
        }
        if (match.isMatchFinished()) {
            pauseMatch();
            archiveCurrentMatch();
            showMatchFinished();
        }
        refreshScreen();
    }

    private void restoreSetAfterUndo(int before) {
        if (match.getPlayerOneSets() + match.getPlayerTwoSets() < before && !setDurationsMs.isEmpty()) {
            currentSetElapsedMs = setDurationsMs.remove(setDurationsMs.size() - 1);
        }
        while (pointTimesMs.size() > match.getPointWinners().size()) {
            pointTimesMs.remove(pointTimesMs.size() - 1);
        }
    }

    private void togglePause() {
        if (match.isMatchFinished()) return;
        if (paused) resumeMatch(); else pauseMatch();
        refreshScreen();
    }

    private void startClock() {
        paused = false;
        lastClockMs = SystemClock.elapsedRealtime();
    }

    private void resetClock() {
        matchElapsedMs = 0;
        currentSetElapsedMs = 0;
        setDurationsMs.clear();
        pointTimesMs.clear();
        startClock();
        hidePauseOverlay();
    }

    private void tickClock() {
        if (!paused) {
            long now = SystemClock.elapsedRealtime();
            long delta = Math.max(0, now - lastClockMs);
            matchElapsedMs += delta;
            currentSetElapsedMs += delta;
            lastClockMs = now;
        }
    }

    private void pauseMatch() {
        if (!paused) {
            tickClock();
            paused = true;
        }
        showPauseOverlay();
    }

    private void resumeMatch() {
        paused = false;
        lastClockMs = SystemClock.elapsedRealtime();
        hidePauseOverlay();
    }

    private void showPauseOverlay() {
        if (match.isMatchFinished() || pauseOverlay != null || courtAlertView == null) return;
        pauseOverlay = text("Ⅱ\nMATCH PAUSED\n\nTAP TO RESUME MATCH", 34, 0xFFFFB59A);
        pauseOverlay.setGravity(Gravity.CENTER);
        pauseOverlay.setTypeface(Typeface.DEFAULT_BOLD);
        pauseOverlay.setLineSpacing(dp(6), 1f);
        pauseOverlay.setBackgroundColor(0xE816130C);
        pauseOverlay.setContentDescription("Match paused. Tap to resume match.");
        pauseOverlay.setOnClickListener(v -> togglePause());
        ((FrameLayout) courtAlertView.getParent()).addView(pauseOverlay, new FrameLayout.LayoutParams(-1, -1));
        courtAlertView.bringToFront();
    }

    private void hidePauseOverlay() {
        if (pauseOverlay != null) {
            ((ViewGroup) pauseOverlay.getParent()).removeView(pauseOverlay);
            pauseOverlay = null;
        }
    }

    private void refreshScreen() {
        if (playerOnePoints == null) return;
        int p1Pts = match.getPlayerOnePoints();
        int p2Pts = match.getPlayerTwoPoints();
        int p1Sets = match.getPlayerOneSets();
        int p2Sets = match.getPlayerTwoSets();

        playerOnePoints.setText(String.valueOf(p1Pts));
        playerTwoPoints.setText(String.valueOf(p2Pts));
        playerOneSets.setText("GAMES  " + p1Sets);
        playerTwoSets.setText("GAMES  " + p2Sets);
        playerOneTitle.setText(playerOneName);
        playerTwoTitle.setText(playerTwoName);

        if (playerOnePanelView != null) {
            playerOnePanelView.setContentDescription(
                "Player 1, " + playerOneName + ". " + p1Sets + " games won, " + p1Pts + " points. Tap panel to add 1 point."
            );
        }
        if (playerTwoPanelView != null) {
            playerTwoPanelView.setContentDescription(
                "Player 2, " + playerTwoName + ". " + p2Sets + " games won, " + p2Pts + " points. Tap panel to add 1 point."
            );
        }

        updateBallBorder(1);
        updateBallBorder(2);

        pauseButton.setText(paused ? "▶" : "Ⅱ");
        pauseButton.setContentDescription(paused ? "Resume match." : "Pause match.");

        if (undoButton != null) {
            boolean hasPts = match.hasPoints() && !paused;
            undoButton.setEnabled(hasPts);
            undoButton.setAlpha(hasPts ? 1.0f : 0.38f);
        }

        String clock = formatDuration(matchElapsedMs);
        status.setText(
            match.isMatchFinished()
                ? "MATCH WINNER: " + (match.getWinner() == 1 ? playerOneName : playerTwoName) + "  ·  " + clock
                : (paused ? "PAUSED  ·  " : "") + clock + "  |  " + ballStatus()
        );

        syncFireworks();
        if (paused && !match.isMatchFinished()) {
            showPauseOverlay();
            if (pauseOverlay != null) pauseOverlay.bringToFront();
        } else {
            hidePauseOverlay();
        }
        updateCourtTimer();
        courtAlertView.bringToFront();
        if (bottomNav != null) {
            bottomNav.setPinned(paused && !match.isMatchFinished());
            if (finishedOverlay == null) bottomNav.bringToFront();
        }
    }

    private String formatDuration(long ms) {
        long s = ms / 1000, h = s / 3600;
        s %= 3600;
        return String.format(Locale.US, "%02d:%02d:%02d", h, s / 60, s % 60);
    }

    private MatchRecord buildCurrentRecord() {
        return new MatchRecord(
            currentMatchId,
            playerOneName,
            playerTwoName,
            playerOneProfileId,
            playerTwoProfileId,
            match.getPlayerOneSets(),
            match.getPlayerTwoSets(),
            match.getPlayerOnePoints(),
            match.getPlayerTwoPoints(),
            match.getPointsToWinSet(),
            match.getSetsToWinMatch(),
            match.isMatchFinished(),
            System.currentTimeMillis(),
            match.getPointWinners(),
            matchElapsedMs,
            setDurationsMs,
            pointTimesMs,
            paused,
            playerOneColor,
            playerTwoColor
        );
    }

    private void archiveCurrentMatch() {
        if (!match.hasPoints()) return;
        MatchHistoryStore.add(this, buildCurrentRecord());
    }

    private void restoreMatchFromIntent(Intent intent) {
        if (intent == null) return;
        String source = intent.getStringExtra(MatchDetailActivity.EXTRA_RECORD);
        if (source == null) return;
        try {
            MatchRecord r = MatchRecord.fromJson(new org.json.JSONObject(source));
            playerOneName = r.playerOneName;
            playerTwoName = r.playerTwoName;
            playerOneProfileId = r.playerOneProfileId;
            playerTwoProfileId = r.playerTwoProfileId;
            playerOneColor = r.playerOneColor;
            playerTwoColor = r.playerTwoColor;

            PlayerProfile one = ProfileStore.find(this, playerOneProfileId);
            PlayerProfile two = ProfileStore.find(this, playerTwoProfileId);
            if (one == null && playerOneProfileId == null) one = ProfileStore.findByName(this, r.playerOneName);
            if (two == null && playerTwoProfileId == null) two = ProfileStore.findByName(this, r.playerTwoName);
            if (one != null) { playerOneProfileId = one.id; playerOneName = one.name; }
            if (two != null) { playerTwoProfileId = two.id; playerTwoName = two.name; }

            currentMatchId = r.id;
            match.resume(
                r.pointsToWinSet,
                r.setsToWinMatch,
                r.playerOnePoints,
                r.playerTwoPoints,
                r.playerOneSets,
                r.playerTwoSets,
                r.finished,
                r.pointWinners
            );
            matchElapsedMs = r.durationMs;
            setDurationsMs.clear();
            setDurationsMs.addAll(r.setDurationsMs);
            pointTimesMs.clear();
            pointTimesMs.addAll(r.pointTimesMs);

            long completed = 0;
            for (long d : r.setDurationsMs) completed += d;
            currentSetElapsedMs = Math.max(0, r.durationMs - completed);

            if (intent.getBooleanExtra("continue", false) && r.finished) {
                match.continueWithThreeSets();
                currentSetElapsedMs = 0;
                paused = false;
                lastClockMs = SystemClock.elapsedRealtime();
            } else {
                // Unfinished matches resume immediately without an extra pause tap.
                paused = r.finished;
                lastClockMs = SystemClock.elapsedRealtime();
            }
            intent.removeExtra(MatchDetailActivity.EXTRA_RECORD);
        } catch (Exception ignored) { }
    }

    private boolean isFireworksScore() {
        int a = match.getPlayerOnePoints(), b = match.getPlayerTwoPoints();
        return (a == 6 && b == 7) || (a == 7 && b == 6) || (a == 6 && b == 9) || (a == 9 && b == 6);
    }

    private String ballStatus() {
        int a = match.getPlayerOnePoints(), b = match.getPlayerTwoPoints(), target = match.getPointsToWinSet();
        int p = (a >= target - 1 && a > b) ? 1 : (b >= target - 1 && b > a) ? 2 : 0;
        if (p == 0) return "GAME TO " + target + "  |  MATCH TO " + match.getSetsToWinMatch();
        boolean mb = (p == 1 ? match.getPlayerOneSets() : match.getPlayerTwoSets()) == match.getSetsToWinMatch() - 1;
        return (mb ? "MATCH BALL: " : "GAME BALL: ") + (p == 1 ? playerOneName : playerTwoName);
    }

    private void loadStyle() {
        theme = new UiTheme(this);
        darkTheme = theme.dark;
        highContrast = theme.highContrast;
        background = theme.background;
        surface = theme.surface;
        foreground = theme.onSurface;
        muted = theme.onSurfaceVariant;
        playerOneColor = AppPreferences.color(this, "color1", AppPreferences.BLUE);
        playerTwoColor = AppPreferences.color(this, "color2", AppPreferences.RED);
        remoteOne = AppPreferences.remote(this, "remote1", KeyEvent.KEYCODE_VOLUME_UP);
        remoteTwo = AppPreferences.remote(this, "remote2", KeyEvent.KEYCODE_VOLUME_DOWN);
        remoteUndo = AppPreferences.remote(this, "undo", KeyEvent.KEYCODE_MEDIA_PAUSE);
        remoteReset = AppPreferences.remote(this, "reset", KeyEvent.KEYCODE_MEDIA_PLAY);
    }

    private int[] colors() { return UiTheme.playerColors(this); }
    private String[] colorNames() { return UiTheme.playerColorNames(this); }
    private int colorAt(int i) { return colors()[i]; }
    private int colorIndex(int c) {
        int[] x = colors();
        for (int i = 0; i < x.length; i++) if (x[i] == c) return i;
        return 0;
    }

    private String[] remoteNames() {
        return new String[]{"Volume +", "Volume -", "Next track", "Previous track"};
    }

    private int[] remotes() {
        return new int[]{
            KeyEvent.KEYCODE_VOLUME_UP,
            KeyEvent.KEYCODE_VOLUME_DOWN,
            KeyEvent.KEYCODE_MEDIA_NEXT,
            KeyEvent.KEYCODE_MEDIA_PREVIOUS
        };
    }

    private int remoteAt(int i) { return remotes()[i]; }
    private int remoteIndex(int k) {
        int[] r = remotes();
        for (int i = 0; i < r.length; i++) if (r[i] == k) return i;
        return 0;
    }

    private void syncFireworks() {
        if (UiTheme.isReduceMotionEnabled(this)) {
            hideFireworks();
            return;
        }
        if (isFireworksScore() && !match.isMatchFinished()) {
            if (fireworksView == null) {
                fireworksView = new FireworksView(this);
                ((FrameLayout) courtAlertView.getParent()).addView(fireworksView, new FrameLayout.LayoutParams(-1, -1));
            }
        } else {
            hideFireworks();
        }
    }

    private void hideFireworks() {
        if (fireworksView != null) {
            if (fireworksView.getParent() instanceof ViewGroup) {
                ((ViewGroup) fireworksView.getParent()).removeView(fireworksView);
            }
            fireworksView = null;
        }
    }

    private boolean isBallFor(int p) {
        return p == 1
            ? match.getPlayerOnePoints() >= match.getPointsToWinSet() - 1 && match.getPlayerOnePoints() > match.getPlayerTwoPoints()
            : match.getPlayerTwoPoints() >= match.getPointsToWinSet() - 1 && match.getPlayerTwoPoints() > match.getPlayerOnePoints();
    }

    private void onCourtTimerClick() {
        if (courtStartMs == 0) {
            startCourtRental();
            return;
        }
        new AlertDialog.Builder(this)
            .setTitle("Court timer")
            .setMessage("Do you want to reset the start time to the current full hour, or stop the court timer?")
            .setNegativeButton("Cancel action", null)
            .setNeutralButton("Stop court timer", (dialog, which) -> {
                courtStartMs = 0;
                AppPreferences.saveCourtStart(this, 0);
                updateCourtTimer();
            })
            .setPositiveButton("Reset court timer", (dialog, which) -> startCourtRental())
            .show();
    }

    private void startCourtRental() {
        Calendar lower = Calendar.getInstance();
        lower.set(Calendar.MINUTE, 0);
        lower.set(Calendar.SECOND, 0);
        lower.set(Calendar.MILLISECOND, 0);
        courtStartMs = lower.getTimeInMillis();
        AppPreferences.saveCourtStart(this, courtStartMs);
        updateCourtTimer();
    }

    private void updateCourtTimer() {
        if (courtButton == null) return;
        if (courtStartMs == 0) {
            courtButton.setText("Start court timer");
            courtButton.setTextColor(foreground);
            courtButton.setContentDescription("Start court timer. Court timer is off.");
            courtBackground.setStroke(dp(2), theme.outline);
            courtAlertView.setAlertColor(0);
            return;
        }
        long now = System.currentTimeMillis();
        long duration = AppPreferences.courtDuration(this) * 60000L;
        long remaining = courtStartMs + duration - now;
        String start = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(courtStartMs));
        if (now < courtStartMs) {
            long minutes = (courtStartMs - now + 59999) / 60000;
            courtButton.setText(String.format(Locale.getDefault(), "COURT ON · START %s IN %d:%02d", start, minutes / 60, minutes % 60));
            courtButton.setTextColor(foreground);
            courtBackground.setStroke(dp(2), theme.outline);
            courtAlertView.setAlertColor(0);
            return;
        }
        if (remaining <= 0) {
            courtButton.setText("! COURT ON · " + start + " · END");
            courtButton.setTextColor(theme.error);
            courtButton.setContentDescription("Court timer ended. Start time " + start + ". Status END.");
            courtBackground.setStroke(dp(6), AppPreferences.RED);
            courtAlertView.setAlertColor(AppPreferences.RED);
            return;
        }
        long minutes = (remaining + 59999) / 60000;
        int color = theme.outline, width = 2, level = 0;
        String statusWord = "OK";
        if (remaining <= AppPreferences.courtGreen(this) * 60000L) {
            color = AppPreferences.GREEN;
            level = 1;
            statusWord = "OK";
        }
        if (remaining <= AppPreferences.courtYellow(this) * 60000L) {
            color = AppPreferences.GOLD;
            width = 5;
            level = 2;
            statusWord = "WARN";
        }
        if (remaining <= AppPreferences.courtRed(this) * 60000L) {
            color = AppPreferences.RED;
            width = 6;
            level = 3;
            statusWord = "WARN";
        }
        courtButton.setText(String.format(Locale.getDefault(), "COURT ON · %s · %d:%02d · %s", start, minutes / 60, minutes % 60, statusWord));
        courtButton.setContentDescription("Court timer active. Start time " + start + ". " + minutes + " min remaining. Status " + statusWord + ".");
        courtButton.setTextColor(level == 0 ? foreground : theme.playerAccentOnSurface(color));
        courtBackground.setStroke(dp(width), color);
        courtAlertView.setAlertColor(level == 0 ? 0 : color);
        maybeVibrateCourtWarning(level);
    }

    private void maybeVibrateCourtWarning(int level) {
        if (level == 0 || !AppPreferences.courtVibration(this) || level <= AppPreferences.courtVibratedLevel(this)) return;
        AppPreferences.saveCourtVibratedLevel(this, level);
        Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        if (vibrator == null || !vibrator.hasVibrator()) return;
        long[] pattern = new long[level * 2];
        for (int i = 0; i < level; i++) {
            pattern[i * 2] = i == 0 ? 0 : 150;
            pattern[i * 2 + 1] = 180;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1));
        } else {
            vibrator.vibrate(pattern, -1);
        }
    }

    private void updateBallBorder(int player) {
        GradientDrawable border = player == 1 ? playerOneBackground : playerTwoBackground;
        int rawColor = player == 1 ? playerOneColor : playerTwoColor;
        int strokeColor = theme.playerAccentOnSurface(rawColor);
        ValueAnimator pulse = player == 1 ? playerOnePulse : playerTwoPulse;

        if (!isBallFor(player) || match.isMatchFinished()) {
            stopBallPulse(player);
            border.setStroke(dp(3), strokeColor);
            return;
        }
        if (UiTheme.isReduceMotionEnabled(this)) {
            stopBallPulse(player);
            border.setStroke(dp(12), rawColor);
            return;
        }
        if (pulse != null) return;
        pulse = ValueAnimator.ofInt(40, 255);
        pulse.setDuration(750);
        pulse.setRepeatMode(ValueAnimator.REVERSE);
        pulse.setRepeatCount(ValueAnimator.INFINITE);
        pulse.setInterpolator(new AccelerateDecelerateInterpolator());
        pulse.addUpdateListener(animation -> {
            int alpha = (Integer) animation.getAnimatedValue();
            border.setStroke(dp(12), Color.argb(alpha, Color.red(rawColor), Color.green(rawColor), Color.blue(rawColor)));
        });
        if (player == 1) playerOnePulse = pulse; else playerTwoPulse = pulse;
        pulse.start();
    }

    private void stopBallPulse(int player) {
        ValueAnimator pulse = player == 1 ? playerOnePulse : playerTwoPulse;
        if (pulse != null) pulse.cancel();
        if (player == 1) playerOnePulse = null; else playerTwoPulse = null;
    }

    private void showMatchFinished() {
        finishedOverlay = new MatchFinishedView(
            this,
            match.getWinner() == 1 ? playerOneName : playerTwoName,
            () -> {
                finishedOverlay = null;
                match.reset();
                currentMatchId = System.currentTimeMillis();
                resetClock();
                refreshScreen();
            },
            () -> {
                finishedOverlay = null;
                match.continueWithThreeSets();
                currentSetElapsedMs = 0;
                resumeMatch();
                refreshScreen();
            },
            () -> {
                try {
                    String json = buildCurrentRecord().toJson().toString();
                    startActivity(new Intent(this, MatchDetailActivity.class).putExtra(MatchDetailActivity.EXTRA_RECORD, json));
                } catch (Exception ignored) { }
            }
        );
        ((FrameLayout) courtAlertView.getParent()).addView(finishedOverlay, new FrameLayout.LayoutParams(-1, -1));
        courtAlertView.bringToFront();
    }

    private void confirmReset() {
        // UX simplification: if the current match has 0 points, reset immediately in 1 tap.
        if (!match.hasPoints()) {
            resetMatchSafely();
            return;
        }
        new AlertDialog.Builder(this)
            .setTitle("Start a new match?")
            .setMessage("The app will finish the current match and save the match in the history.")
            .setNegativeButton("Cancel action", null)
            .setPositiveButton("Start new match", (d, w) -> resetMatchSafely())
            .show();
    }

    private void resetMatchSafely() {
        if (finishedOverlay != null) {
            if (finishedOverlay.getParent() instanceof ViewGroup) {
                ((ViewGroup) finishedOverlay.getParent()).removeView(finishedOverlay);
            }
            finishedOverlay = null;
        }
        if (match.hasPoints()) match.conclude();
        pauseMatch();
        archiveCurrentMatch();
        match.reset();
        currentMatchId = System.currentTimeMillis();
        resetClock();
        refreshScreen();
    }

    private LinearLayout box(int color) {
        LinearLayout x = new LinearLayout(this);
        x.setOrientation(LinearLayout.VERTICAL);
        x.setBackgroundColor(color);
        return x;
    }

    private TextView text(String value, int size, int color) {
        TextView v = new TextView(this);
        v.setText(value);
        v.setTextSize(size);
        v.setTextColor(color);
        return v;
    }

    private Button button(String label, int color) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextColor(darkTheme || color != surface ? Color.WHITE : foreground);
        b.setTextSize(16);
        b.setAllCaps(false);
        b.setMinHeight(dp(48));
        b.setMinimumHeight(dp(48));
        b.setPadding(dp(10), 0, dp(10), 0);
        b.setBackground(theme.interactiveBackground(this, color, 12, 1, theme.outlineVariant));
        return b;
    }

    private Button iconButton(String symbol, String desc, int color) {
        Button b = button(symbol, color);
        b.setTextSize(22);
        b.setContentDescription(desc);
        b.setPadding(0, 0, 0, 0);
        return b;
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }
}
