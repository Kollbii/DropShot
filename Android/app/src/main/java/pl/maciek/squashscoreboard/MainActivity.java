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
import pl.maciek.squashscoreboard.lootbox.RewardFonts;
import pl.maciek.squashscoreboard.lootbox.RewardInventory;

public class MainActivity extends Activity {
    private UiTheme theme;
    private int background, surface, foreground, muted, playerOneColor, playerTwoColor, remoteOne, remoteTwo, remoteUndo, remoteReset;
    private boolean darkTheme, highContrast, showServeSideIndicator = true, paused, awaitingNextGame;
    private final MatchState match = new MatchState();
    private String playerOneName = "PLAYER 1", playerTwoName = "PLAYER 2", playerOneProfileId, playerTwoProfileId;
    private long currentMatchId, matchElapsedMs, currentSetElapsedMs, lastClockMs;
    private final ArrayList<Long> setDurationsMs = new ArrayList<>();
    private final ArrayList<Long> pointTimesMs = new ArrayList<>();
    private TextView playerOnePoints, playerTwoPoints, playerOneSets, playerTwoSets, status, playerOneTitle, playerTwoTitle;
    private TextView playerOneServeBadge, playerTwoServeBadge;
    private FrameLayout playerOneServeContainer, playerTwoServeContainer;
    private GradientDrawable playerOneServeBadgeBg, playerTwoServeBadgeBg;
    private LinearLayout playerOnePanelView, playerTwoPanelView;
    private Button pauseButton, undoButton;
    private TextView pauseOverlay;
    private MediaSession mediaSession;
    private WatchCommandServer watchCommandServer;
    private BluetoothWatchServer bluetoothWatchServer;
    private boolean requestedBluetoothPermission;
    private GradientDrawable playerOneBackground, playerTwoBackground;
    private MatchFinishedView finishedOverlay;
    private FireworksView fireworksView;
    private ValueAnimator playerOnePulse, playerTwoPulse;
    private Button courtButton;
    private GradientDrawable courtBackground;
    private CourtAlertView courtAlertView;
    private View bottomNav;
    private boolean bottomNavVisible = true;
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
        RewardInventory.setOnUnlockListener((ctx, profileId, reward) ->
            SupabaseSyncClient.syncProfileByIdInBackground(ctx, profileId)
        );
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
        currentMatchId = InputValidator.generateSecureMatchId();
        startClock();
        restoreMatchFromIntent(getIntent());
        createMediaSession();
        Handler mainLoopHandler = new Handler(Looper.getMainLooper());
        watchCommandServer = new WatchCommandServer(mainLoopHandler, this::handleWatchCommand);
        bluetoothWatchServer = new BluetoothWatchServer(
            this,
            mainLoopHandler,
            this::handleWatchCommand,
            new BluetoothWatchServer.StateProvider() {
                @Override
                public String buildStateJsonLine() {
                    return buildWatchStateLine();
                }

                @Override
                public boolean isGameOrMatchBall() {
                    return match.isGameOrMatchBall();
                }
            }
        );
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
        if (Build.VERSION.SDK_INT >= 31
            && !BluetoothWatchServer.hasBluetoothConnectPermission(this)
            && !requestedBluetoothPermission) {
            requestedBluetoothPermission = true;
            requestPermissions(new String[]{android.Manifest.permission.BLUETOOTH_CONNECT}, 1001);
        } else if (bluetoothWatchServer != null) {
            bluetoothWatchServer.start();
        }
        clockHandler.removeCallbacks(clockTicker);
        clockHandler.post(clockTicker);
        loadStyle();
        syncSelectedProfiles();
        int targetPts = AppPreferences.pointsPerSet(this);
        int targetSets = AppPreferences.setsPerMatch(this);
        if (targetPts != match.getPointsToWinSet() || targetSets != match.getSetsToWinMatch()) {
            archiveCurrentMatch();
            match.configure(targetPts, targetSets);
            resetClock();
            currentMatchId = InputValidator.generateSecureMatchId();
            setContentView(createContent());
        }
        refreshScreen();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1001 && bluetoothWatchServer != null) {
            bluetoothWatchServer.start();
        }
    }

    private void syncSelectedProfiles() {
        PlayerProfile one = ProfileStore.find(this, playerOneProfileId);
        PlayerProfile two = ProfileStore.find(this, playerTwoProfileId);
        if (one == null && playerOneName != null) {
            one = ProfileStore.findByName(this, playerOneName);
            if (one != null) playerOneProfileId = one.id;
        }
        if (two == null && playerTwoName != null) {
            two = ProfileStore.findByName(this, playerTwoName);
            if (two != null) playerTwoProfileId = two.id;
        }
        boolean changed = false;
        if (one != null && (!one.name.equals(playerOneName) || one.color != playerOneColor)) {
            playerOneName = one.name;
            playerOneColor = one.color;
            changed = true;
        }
        if (two != null && (!two.name.equals(playerTwoName) || two.color != playerTwoColor)) {
            playerTwoName = two.name;
            playerTwoColor = two.color;
            changed = true;
        }
        if (changed) {
            AppPreferences.save(this, darkTheme, playerOneColor, playerTwoColor, remoteOne, remoteTwo);
            setContentView(createContent());
        }
        applyPlayerFont(playerOneTitle, one);
        applyPlayerFont(playerTwoTitle, two);
    }

    private void applyPlayerFont(TextView title, PlayerProfile profile) {
        if (title != null) RewardFonts.apply(title,
            profile == null ? null : profile.fontId, 24f);
    }

    @Override
    protected void onPause() {
        if (watchCommandServer != null) watchCommandServer.stop();
        if (bluetoothWatchServer != null) bluetoothWatchServer.stop();
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
        if (bluetoothWatchServer != null) bluetoothWatchServer.stop();
        if (mediaSession != null) mediaSession.release();
        if (bottomNav != null) bottomNav.animate().cancel();
        super.onDestroy();
    }

    @Override public void onUserInteraction() {
        super.onUserInteraction();
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
        bottomNav = theme.createBottomNavBar(this, UiTheme.TAB_MATCH, this::showSettings);
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
        applyPlayerFont(title, ProfileStore.find(this, player == 1 ? playerOneProfileId : playerTwoProfileId));
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

        FrameLayout serveContainer = new FrameLayout(this);
        serveContainer.setMinimumHeight(dp(48));
        serveContainer.setFilterTouchesWhenObscured(true);

        GradientDrawable serveBadgeBg = new GradientDrawable();
        serveBadgeBg.setCornerRadius(dp(8));
        serveBadgeBg.setColor(0x2EFFFFFF);
        serveBadgeBg.setStroke(dp(1), 0x88FFFFFF);

        TextView serveBadge = text("[R]", 18, Color.WHITE);
        serveBadge.setTypeface(Typeface.DEFAULT_BOLD);
        serveBadge.setGravity(Gravity.CENTER);
        serveBadge.setSingleLine(true);
        serveBadge.setPadding(dp(14), dp(4), dp(14), dp(4));
        serveBadge.setBackground(serveBadgeBg);

        FrameLayout.LayoutParams badgeParams = new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            Gravity.CENTER
        );
        serveContainer.addView(serveBadge, badgeParams);
        serveContainer.setOnClickListener(v -> {
            if (match.isMatchFinished()) return;
            if (match.getActiveServer() == player) {
                if (match.canChooseServiceSide() && match.toggleServiceSide()) {
                    refreshScreen();
                }
            } else if (match.canSelectInitialServer()) {
                if (match.selectInitialServer(player)) {
                    refreshScreen();
                }
            } else {
                addPoint(player);
            }
        });

        TextView hint = text("TAP PANEL TO ADD 1 POINT", 16, 0xFFF4F0E9);
        hint.setGravity(Gravity.CENTER);
        hint.setSingleLine(true);

        panel.addView(title, new LinearLayout.LayoutParams(-1, dp(36)));
        panel.addView(sets, new LinearLayout.LayoutParams(-1, dp(38)));
        panel.addView(points, new LinearLayout.LayoutParams(-1, 0, 1));
        panel.addView(serveContainer, new LinearLayout.LayoutParams(-1, dp(48)));
        panel.addView(hint, new LinearLayout.LayoutParams(-1, dp(24)));

        if (player == 1) {
            playerOnePoints = points;
            playerOneSets = sets;
            playerOneTitle = title;
            playerOneBackground = bg;
            playerOnePanelView = panel;
            playerOneServeContainer = serveContainer;
            playerOneServeBadge = serveBadge;
            playerOneServeBadgeBg = serveBadgeBg;
        } else {
            playerTwoPoints = points;
            playerTwoSets = sets;
            playerTwoTitle = title;
            playerTwoBackground = bg;
            playerTwoPanelView = panel;
            playerTwoServeContainer = serveContainer;
            playerTwoServeBadge = serveBadge;
            playerTwoServeBadgeBg = serveBadgeBg;
        }
        return panel;
    }

    private void showSettings() {
        showSettings(0);
    }

    private void showSettings(int initialTab) {
        SettingsDialog.show(this, initialTab, () -> {
            int pts = AppPreferences.pointsPerSet(this);
            int sts = AppPreferences.setsPerMatch(this);
            String nextProfileOneId = AppPreferences.profileOneId(this);
            String nextProfileTwoId = AppPreferences.profileTwoId(this);
            String nextNameOne = AppPreferences.playerOneName(this);
            String nextNameTwo = AppPreferences.playerTwoName(this);
            int c1 = AppPreferences.color(this, "color1", AppPreferences.BLUE);
            int c2 = AppPreferences.color(this, "color2", AppPreferences.RED);

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
                currentMatchId = InputValidator.generateSecureMatchId();
            }

            loadStyle();
            setContentView(createContent());
            refreshScreen();
        });
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
        if ("TOGGLE_PAUSE".equals(command)) {
            if (match.isMatchFinished()) {
                resetMatchSafely();
                return true;
            }
            if (awaitingNextGame) {
                awaitingNextGame = false;
                resumeMatch();
                refreshScreen();
                return true;
            }
            togglePause();
            return true;
        }
        if ("TOGGLE_SIDE".equals(command)) {
            if (match.isMatchFinished() || !match.canChooseServiceSide()) return false;
            boolean toggled = match.toggleServiceSide();
            if (toggled) refreshScreen();
            return toggled;
        }
        if ("SWITCH_SERVER".equals(command)) {
            if (match.isMatchFinished() || !match.canSelectInitialServer()) return false;
            boolean switched = match.switchInitialServer();
            if (switched) refreshScreen();
            return switched;
        }
        if ("RESET".equals(command)) {
            resetMatchSafely();
            return true;
        }
        return false;
    }

    private String computeWatchActionLabel() {
        if (awaitingNextGame && !match.isMatchFinished()) {
            return "NEXT";
        }
        if (match.isMatchFinished() || paused) {
            return "START";
        }
        return "STOP";
    }

    private String buildWatchStateLine() {
        RewardCatalog.Reward r1 = RewardCatalog.byKey(playerOneColor);
        RewardCatalog.Reward r2 = RewardCatalog.byKey(playerTwoColor);
        boolean p1Rainbow = playerOneColor == BluetoothWatchServer.RAINBOW_COLOR_KEY
            || (r1 != null && "rainbow".equalsIgnoreCase(r1.id));
        boolean p2Rainbow = playerTwoColor == BluetoothWatchServer.RAINBOW_COLOR_KEY
            || (r2 != null && "rainbow".equalsIgnoreCase(r2.id));
        int c1 = (r1 != null && r1.colors.length > 1) ? playerOneColor
            : (theme != null ? theme.playerPanelFill(playerOneColor) : playerOneColor);
        int c2 = (r2 != null && r2.colors.length > 1) ? playerTwoColor
            : (theme != null ? theme.playerPanelFill(playerTwoColor) : playerTwoColor);
        return BluetoothWatchServer.buildStateJsonLine(
            playerOneName,
            playerTwoName,
            match.getPlayerOnePoints(),
            match.getPlayerTwoPoints(),
            match.getPlayerOneSets(),
            match.getPlayerTwoSets(),
            match.getSetsToWinMatch(),
            match.getActiveServer(),
            match.getServiceSide(),
            match.canChooseServiceSide(),
            match.canSelectInitialServer(),
            c1,
            p1Rainbow,
            c2,
            p2Rainbow,
            computeWatchActionLabel(),
            match.isGameOrMatchBall(),
            match.isMatchFinished(),
            paused
        );
    }

    private void addPoint(int player) {
        if (paused || match.isMatchFinished()) return;
        awaitingNextGame = false;
        tickClock();
        int oldSets = match.getPlayerOneSets() + match.getPlayerTwoSets();
        match.addPoint(player);
        pointTimesMs.add(matchElapsedMs);
        if (match.getPlayerOneSets() + match.getPlayerTwoSets() > oldSets) {
            setDurationsMs.add(currentSetElapsedMs);
            currentSetElapsedMs = 0;
            if (!match.isMatchFinished()) {
                awaitingNextGame = true;
                pauseMatch();
                archiveCurrentMatch();
            }
        }
        if (match.isMatchFinished()) {
            awaitingNextGame = false;
            pauseMatch();
            archiveCurrentMatch();
            showMatchFinished();
        }
        refreshScreen();
    }

    private void restoreSetAfterUndo(int before) {
        if (match.getPlayerOneSets() + match.getPlayerTwoSets() < before && !setDurationsMs.isEmpty()) {
            currentSetElapsedMs = setDurationsMs.remove(setDurationsMs.size() - 1);
            awaitingNextGame = false;
        }
        while (pointTimesMs.size() > match.getPointWinners().size()) {
            pointTimesMs.remove(pointTimesMs.size() - 1);
        }
    }

    private void togglePause() {
        if (match.isMatchFinished()) return;
        awaitingNextGame = false;
        if (paused) resumeMatch(); else pauseMatch();
        refreshScreen();
    }

    private void startClock() {
        paused = false;
        lastClockMs = SystemClock.elapsedRealtime();
    }

    private void resetClock() {
        awaitingNextGame = false;
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
        updatePhoneServeBadge(1);
        updatePhoneServeBadge(2);

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
            boolean shouldShowNav = paused || match.isMatchFinished() || !match.hasPoints();
            setBottomNavVisible(shouldShowNav);
            if (shouldShowNav && finishedOverlay == null) {
                bottomNav.bringToFront();
            }
        }
        if (bluetoothWatchServer != null) {
            bluetoothWatchServer.broadcastStateAsync(buildWatchStateLine());
        }
    }

    private void setBottomNavVisible(boolean show) {
        if (bottomNav == null) return;
        if (bottomNavVisible == show && bottomNav.getVisibility() == (show ? View.VISIBLE : View.GONE)) {
            return;
        }
        bottomNavVisible = show;
        bottomNav.animate().cancel();
        if (show) {
            bottomNav.setVisibility(View.VISIBLE);
            if (UiTheme.isReduceMotionEnabled(this)) {
                bottomNav.setTranslationY(0);
                bottomNav.setAlpha(1f);
            } else {
                bottomNav.setTranslationY(dp(56));
                bottomNav.setAlpha(0f);
                bottomNav.animate().translationY(0).alpha(1f).setDuration(220).start();
            }
        } else {
            if (UiTheme.isReduceMotionEnabled(this)) {
                bottomNav.setVisibility(View.GONE);
            } else {
                bottomNav.animate()
                    .translationY(dp(56))
                    .alpha(0f)
                    .setDuration(220)
                    .withEndAction(() -> {
                        if (!bottomNavVisible && bottomNav != null) {
                            bottomNav.setVisibility(View.GONE);
                        }
                    })
                    .start();
            }
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
            playerTwoColor,
            match.getInitialServer(),
            match.getEncodedPointSides()
        );
    }

    private void archiveCurrentMatch() {
        if (!match.hasPoints()) return;
        MatchRecord record = buildCurrentRecord();
        MatchHistoryStore.add(this, record);
        SupabaseSyncClient.syncMatchInBackground(this, record);
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
                r.pointWinners,
                r.initialServer,
                r.pointSides
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
        playerOneName = AppPreferences.playerOneName(this);
        playerTwoName = AppPreferences.playerTwoName(this);
        playerOneProfileId = AppPreferences.profileOneId(this);
        playerTwoProfileId = AppPreferences.profileTwoId(this);
        playerOneColor = AppPreferences.color(this, "color1", AppPreferences.BLUE);
        playerTwoColor = AppPreferences.color(this, "color2", AppPreferences.RED);
        showServeSideIndicator = AppPreferences.showServeSideIndicator(this);
        remoteOne = AppPreferences.remote(this, "remote1", KeyEvent.KEYCODE_VOLUME_UP);
        remoteTwo = AppPreferences.remote(this, "remote2", KeyEvent.KEYCODE_VOLUME_DOWN);
        remoteUndo = AppPreferences.remote(this, "undo", KeyEvent.KEYCODE_MEDIA_PAUSE);
        remoteReset = AppPreferences.remote(this, "reset", KeyEvent.KEYCODE_MEDIA_PLAY);
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
            boolean activeServer = !match.isMatchFinished() && match.getActiveServer() == player;
            border.setStroke(dp(activeServer ? 5 : 2), strokeColor);
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

    private void updatePhoneServeBadge(int player) {
        FrameLayout container = player == 1 ? playerOneServeContainer : playerTwoServeContainer;
        TextView badge = player == 1 ? playerOneServeBadge : playerTwoServeBadge;
        GradientDrawable bg = player == 1 ? playerOneServeBadgeBg : playerTwoServeBadgeBg;
        if (container == null || badge == null || bg == null) return;

        if (!showServeSideIndicator || match.isMatchFinished()) {
            container.setVisibility(View.GONE);
            return;
        }

        int activeServer = match.getActiveServer();
        String side = match.getServiceSide();
        boolean canPickSide = match.canChooseServiceSide();
        boolean canPickServer = match.canSelectInitialServer();

        if (activeServer == player) {
            container.setVisibility(View.VISIBLE);
            badge.setText("[" + side + "]");
            if (canPickSide) {
                container.setClickable(true);
                bg.setColor(0x55000000);
                bg.setStroke(dp(2), 0xDDFFFFFF);
                badge.setTextColor(Color.WHITE);
                container.setContentDescription(
                    "Serve side " + ("L".equals(side) ? "left" : "right") + ". Tap to switch serve side."
                );
            } else {
                container.setClickable(false);
                bg.setColor(0x66B71C1C);
                bg.setStroke(dp(1), 0x88EF5350);
                badge.setTextColor(0xCCFFCDD2);
                container.setContentDescription(
                    "Serve side " + ("L".equals(side) ? "left" : "right") + " locked during serve streak."
                );
            }
        } else if (canPickServer) {
            container.setVisibility(View.VISIBLE);
            container.setClickable(true);
            badge.setText("[SERVE]");
            bg.setColor(0x44000000);
            bg.setStroke(dp(1), 0xAAFFFFFF);
            badge.setTextColor(0xEEFFFFFF);
            String name = player == 1 ? playerOneName : playerTwoName;
            container.setContentDescription("Tap to set " + name + " as starting server.");
        } else {
            container.setVisibility(View.GONE);
            container.setClickable(false);
        }
    }

    private void stopBallPulse(int player) {
        ValueAnimator pulse = player == 1 ? playerOnePulse : playerTwoPulse;
        if (pulse != null) pulse.cancel();
        if (player == 1) playerOnePulse = null; else playerTwoPulse = null;
    }

    private void showMatchFinished() {
        String winnerName = match.getWinner() == 1 ? playerOneName : playerTwoName;
        String finalScore = match.getPlayerOneSets() + " - " + match.getPlayerTwoSets();
        if (bluetoothWatchServer != null) {
            bluetoothWatchServer.broadcastMatchEndedAsync(winnerName, finalScore);
        }
        finishedOverlay = new MatchFinishedView(
            this,
            winnerName,
            () -> {
                finishedOverlay = null;
                match.reset();
                currentMatchId = InputValidator.generateSecureMatchId();
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
        currentMatchId = InputValidator.generateSecureMatchId();
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
        b.setFilterTouchesWhenObscured(true);
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
