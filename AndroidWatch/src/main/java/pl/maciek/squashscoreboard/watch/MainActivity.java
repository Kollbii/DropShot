package pl.maciek.squashscoreboard.watch;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.os.Vibrator;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Standalone Android Watch remote control activity for the LG G Watch (LG-W100, 280x280, API 20-22).
 * Implements:
 * - Low backlight brightness (0.08f) during active play, with a 3-second brightness boost on match end.
 * - Distinct eyes-free vibration patterns (P1 30 ms, P2 2x25 ms, Game/Match ball +150 ms, Undo 80 ms, Action 100 ms).
 * - 70/30 vertical screen split with live score tiles, 3-letter initials, sets dots, active server 4dp border,
 *   service box side ([L] / [R]), and synchronized player colors (including rainbow linear gradients).
 * - Dynamic START / STOP / NEXT button with 1-second hold protection for STOP during active play.
 * - Full-screen Match-End modal pop-up overlay with winner name, final score, and OK button.
 */
public final class MainActivity extends Activity implements BluetoothWatchClient.ClientListener {
    private static final int COLOR_BG = 0xFF080B10;

    private static final int UNDO_NORMAL = 0xFF3C4B59;
    private static final int UNDO_PRESSED = 0xFF26323D;
    private static final int UNDO_RIM = 0xFF627487;

    private static final int ACTION_START_NORMAL = 0xFF157C68;
    private static final int ACTION_NEXT_NORMAL = 0xFF1E78C9;
    private static final int ACTION_STOP_NORMAL = 0xFF8C2D38;
    private static final int ACTION_PRESSED = 0xFF1F2830;
    private static final int ACTION_RIM = 0xFF8AA2B8;

    private static final int ACTIVE_SERVER_BORDER_COLOR = 0xFFFFD54F;
    private static final int INACTIVE_SERVER_BORDER_COLOR = 0x66FFFFFF;

    private static final int STATUS_CONNECTED_COLOR = 0xFF39B87F;
    private static final int STATUS_DISCONNECTED_COLOR = 0xFFF1636E;
    private static final int STATUS_RECONNECTING_COLOR = 0xFFFFD54F;

    private static final float ACTIVE_PLAY_BRIGHTNESS = 0.2f;
    private static final float POPUP_BOOST_BRIGHTNESS = 0.85f;
    private static final long POPUP_BRIGHTNESS_DURATION_MS = 3000L;
    private static final long RESET_HOLD_DURATION_MS = 2000L;
    private static final long SHOW_HOLD_TEXT_DELAY_MS = 400L;
    private static final long SIDE_SWITCH_HOLD_DURATION_MS = 1000L;
    private static final long SIDE_SWITCH_SHOW_TEXT_DELAY_MS = 200L;
    private static final long SIDE_SWITCH_GRACE_DURATION_MS = 1500L;

    private static final long VIBRATE_POINT_MS = 35L;
    private static final long[] VIBRATE_DOUBLE_HAPTIC = new long[]{0L, 30L, 45L, 30L};
    private static final long[] VIBRATE_BALL_EXTRA_PATTERN = new long[]{200L, 150L};
    private static final long VIBRATE_UNDO_MS = 80L;
    private static final long VIBRATE_ACTION_MS = 100L;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Runnable dimBrightnessRunnable = () -> setWindowBrightness(ACTIVE_PLAY_BRIGHTNESS);

    private BluetoothWatchClient watchClient;
    private Vibrator vibrator;
    private WatchProtocol.WatchState currentState = WatchProtocol.WatchState.defaultState();
    private boolean previousGameOrMatchBall;

    // Top player tiles
    private LinearLayout p1Tile;
    private GradientDrawable p1TileBg;
    private TextView p1HeaderView;
    private TextView p1PointsView;
    private TextView p1ServiceSideView;

    private LinearLayout p2Tile;
    private GradientDrawable p2TileBg;
    private TextView p2HeaderView;
    private TextView p2PointsView;
    private TextView p2ServiceSideView;

    private boolean sideSwitchTriggered;
    private long sideSwitchGraceUntilUptimeMs;
    private Runnable sideSwitchVisualRunnable;
    private Runnable sideSwitchHoldRunnable;

    // Bottom buttons
    private TextView undoButtonView;
    private GradientDrawable undoButtonBg;
    private TextView actionButtonView;
    private GradientDrawable actionButtonBg;
    private boolean resetHoldTriggered;
    private final Runnable showHoldTextRunnable = () -> {
        if (actionButtonView != null) {
            actionButtonView.setText("HOLD 2S");
        }
    };
    private final Runnable resetHoldRunnable = () -> {
        resetHoldTriggered = true;
        vibrateOnce(200L);
        if (actionButtonView != null) {
            actionButtonView.setText("RESET");
        }
        if (watchClient != null) {
            watchClient.sendCommand(WatchProtocol.CMD_RESET);
        }
    };

    // Status indicator (Micro dot with 48x40 dp touch target)
    private FrameLayout statusHitBox;
    private View statusDotView;
    private GradientDrawable statusDotBg;

    // Match-end modal overlay
    private FrameLayout matchEndOverlay;
    private TextView matchEndWinnerView;
    private TextView matchEndScoreView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_FULLSCREEN
        );
        setWindowBrightness(ACTIVE_PLAY_BRIGHTNESS);

        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        watchClient = new BluetoothWatchClient(mainHandler, this);

        setContentView(createWatchLayout());
        applyWatchState(currentState);
        updateStatusIndicator(false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        setWindowBrightness(ACTIVE_PLAY_BRIGHTNESS);
        if (watchClient != null) {
            watchClient.start();
            updateStatusIndicator(watchClient.isConnected());
        }
    }

    @Override
    protected void onPause() {
        mainHandler.removeCallbacks(showHoldTextRunnable);
        mainHandler.removeCallbacks(resetHoldRunnable);
        mainHandler.removeCallbacks(dimBrightnessRunnable);
        if (sideSwitchVisualRunnable != null) {
            mainHandler.removeCallbacks(sideSwitchVisualRunnable);
            sideSwitchVisualRunnable = null;
        }
        if (sideSwitchHoldRunnable != null) {
            mainHandler.removeCallbacks(sideSwitchHoldRunnable);
            sideSwitchHoldRunnable = null;
        }
        if (watchClient != null) {
            watchClient.stop();
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        mainHandler.removeCallbacksAndMessages(null);
        if (watchClient != null) {
            watchClient.stop();
        }
        super.onDestroy();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev != null && ev.getActionMasked() == MotionEvent.ACTION_DOWN) {
            if (watchClient != null && !watchClient.isConnected()) {
                watchClient.requestReconnectNow();
            }
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public void onConnectionChanged(boolean connected) {
        updateStatusIndicator(connected);
    }

    @Override
    public void onStateReceived(WatchProtocol.WatchState state) {
        if (state == null) return;
        boolean newlyReachedBall = state.gameOrMatchBall && !previousGameOrMatchBall && !state.matchFinished;
        previousGameOrMatchBall = state.gameOrMatchBall;
        currentState = state;
        applyWatchState(state);
        if (newlyReachedBall) {
            vibratePattern(VIBRATE_BALL_EXTRA_PATTERN);
        }
    }

    @Override
    public void onMatchEnded(WatchProtocol.MatchEndedEvent event) {
        if (event == null) return;
        showMatchEndedPopup(event.winnerName, event.finalScore);
    }

    @Override
    public void onCommandAck(boolean accepted, boolean gameOrMatchBall) {
        if (accepted && gameOrMatchBall && !previousGameOrMatchBall) {
            previousGameOrMatchBall = true;
            vibratePattern(VIBRATE_BALL_EXTRA_PATTERN);
        }
    }

    private View createWatchLayout() {
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(COLOR_BG);

        int gap = dp(4);
        LinearLayout grid = new LinearLayout(this);
        grid.setOrientation(LinearLayout.VERTICAL);
        grid.setBackgroundColor(COLOR_BG);

        // Top row: 70% height (layout_weight="7")
        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams topRowParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 7f
        );
        topRowParams.bottomMargin = gap / 2;

        // Bottom row: 30% height (layout_weight="3")
        LinearLayout bottomRow = new LinearLayout(this);
        bottomRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams bottomRowParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 3f
        );
        bottomRowParams.topMargin = gap / 2;

        p1TileBg = new GradientDrawable();
        p1TileBg.setCornerRadius(dp(8));
        p1HeaderView = createShadowLabel("P1 ●○", 13);
        p1PointsView = createShadowLabel("0", 52);
        p1ServiceSideView = createShadowLabel("[R]", 14);
        p1Tile = createPlayerTile(
            1,
            p1TileBg,
            p1HeaderView,
            p1PointsView,
            p1ServiceSideView,
            "Add 1 point to Player 1."
        );

        p2TileBg = new GradientDrawable();
        p2TileBg.setCornerRadius(dp(8));
        p2HeaderView = createShadowLabel("P2 ○○", 13);
        p2PointsView = createShadowLabel("0", 52);
        p2ServiceSideView = createShadowLabel("[R]", 14);
        p2Tile = createPlayerTile(
            2,
            p2TileBg,
            p2HeaderView,
            p2PointsView,
            p2ServiceSideView,
            "Add 1 point to Player 2."
        );

        View btnUndo = createUndoButton();
        View btnAction = createActionButton();

        LinearLayout.LayoutParams leftCell = new LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.MATCH_PARENT, 1f
        );
        leftCell.rightMargin = gap / 2;

        LinearLayout.LayoutParams rightCell = new LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.MATCH_PARENT, 1f
        );
        rightCell.leftMargin = gap / 2;

        topRow.addView(p1Tile, leftCell);
        topRow.addView(p2Tile, rightCell);

        LinearLayout.LayoutParams bottomLeftCell = new LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.MATCH_PARENT, 1f
        );
        bottomLeftCell.rightMargin = gap / 2;

        LinearLayout.LayoutParams bottomRightCell = new LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.MATCH_PARENT, 1f
        );
        bottomRightCell.leftMargin = gap / 2;

        bottomRow.addView(btnUndo, bottomLeftCell);
        bottomRow.addView(btnAction, bottomRightCell);

        grid.addView(topRow, topRowParams);
        grid.addView(bottomRow, bottomRowParams);
        root.addView(grid, new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ));

        // Micro connection dot with an accessible 48x40 dp touch target centered in the top notch
        statusHitBox = new FrameLayout(this);
        statusHitBox.setClickable(true);
        statusHitBox.setFocusable(true);
        statusHitBox.setContentDescription("Bluetooth status. Tap to reconnect.");

        statusDotBg = new GradientDrawable();
        statusDotBg.setShape(GradientDrawable.OVAL);
        statusDotBg.setColor(STATUS_DISCONNECTED_COLOR);
        statusDotBg.setStroke(dp(1), 0xAA000000);

        statusDotView = new View(this);
        statusDotView.setBackground(statusDotBg);

        int dotSize = dp(8);
        FrameLayout.LayoutParams dotParams = new FrameLayout.LayoutParams(
            dotSize, dotSize, Gravity.TOP | Gravity.CENTER_HORIZONTAL
        );
        dotParams.topMargin = dp(6);
        statusHitBox.addView(statusDotView, dotParams);

        statusHitBox.setOnClickListener(v -> {
            vibrateOnce(VIBRATE_POINT_MS);
            setReconnectingStatusIndicator();
            if (watchClient != null) {
                watchClient.requestReconnectNow();
            }
        });

        FrameLayout.LayoutParams hitBoxParams = new FrameLayout.LayoutParams(
            dp(48), dp(40), Gravity.TOP | Gravity.CENTER_HORIZONTAL
        );
        hitBoxParams.topMargin = 0;
        root.addView(statusHitBox, hitBoxParams);

        matchEndOverlay = createMatchEndOverlay();
        root.addView(matchEndOverlay, new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ));

        return root;
    }

    private LinearLayout createPlayerTile(
        final int playerNumber,
        final GradientDrawable tileBg,
        final TextView headerView,
        final TextView pointsView,
        final TextView serviceSideView,
        final String contentDesc
    ) {
        LinearLayout tile = new LinearLayout(this);
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setGravity(Gravity.CENTER_HORIZONTAL);
        tile.setPadding(dp(6), dp(16), dp(6), dp(6));
        tile.setBackground(tileBg);
        tile.setContentDescription(contentDesc);
        tile.setClickable(true);
        tile.setFocusable(true);

        headerView.setSingleLine(true);
        headerView.setEllipsize(TextUtils.TruncateAt.END);
        pointsView.setSingleLine(true);
        serviceSideView.setSingleLine(true);

        tile.addView(headerView, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        tile.addView(pointsView, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            0,
            1f
        ));
        tile.addView(serviceSideView, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        final String command = playerNumber == 1 ? WatchProtocol.CMD_POINT_1 : WatchProtocol.CMD_POINT_2;
        tile.setOnTouchListener((v, event) -> {
            int action = event.getActionMasked();
            long now = SystemClock.uptimeMillis();
            if (now < sideSwitchGraceUntilUptimeMs) {
                if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                    v.setAlpha(1.0f);
                }
                return true;
            }
            if (action == MotionEvent.ACTION_DOWN) {
                v.setAlpha(0.82f);
                if (playerNumber == currentState.activeServer) {
                    sideSwitchTriggered = false;
                    if (sideSwitchVisualRunnable != null) {
                        mainHandler.removeCallbacks(sideSwitchVisualRunnable);
                    }
                    if (sideSwitchHoldRunnable != null) {
                        mainHandler.removeCallbacks(sideSwitchHoldRunnable);
                    }
                    sideSwitchVisualRunnable = () -> {
                        serviceSideView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11);
                        serviceSideView.setTextColor(ACTIVE_SERVER_BORDER_COLOR);
                        serviceSideView.setText("[CHANGING...]");
                    };
                    mainHandler.postDelayed(sideSwitchVisualRunnable, SIDE_SWITCH_SHOW_TEXT_DELAY_MS);

                    sideSwitchHoldRunnable = () -> {
                        sideSwitchTriggered = true;
                        sideSwitchGraceUntilUptimeMs = SystemClock.uptimeMillis() + SIDE_SWITCH_GRACE_DURATION_MS;
                        vibratePattern(VIBRATE_DOUBLE_HAPTIC);
                        if (watchClient != null) {
                            watchClient.sendCommand(WatchProtocol.CMD_TOGGLE_SIDE);
                        }
                        String newSide = "R".equals(currentState.serviceSide) ? "L" : "R";
                        currentState = new WatchProtocol.WatchState(
                            currentState.playerOneInitials,
                            currentState.playerTwoInitials,
                            currentState.playerOnePoints,
                            currentState.playerTwoPoints,
                            currentState.playerOneSets,
                            currentState.playerTwoSets,
                            currentState.targetSets,
                            currentState.activeServer,
                            newSide,
                            currentState.playerOneColor,
                            currentState.playerOneRainbow,
                            currentState.playerTwoColor,
                            currentState.playerTwoRainbow,
                            currentState.actionLabel,
                            currentState.gameOrMatchBall,
                            currentState.matchFinished,
                            currentState.paused
                        );
                        serviceSideView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
                        serviceSideView.setTextColor(Color.WHITE);
                        serviceSideView.setText("[" + newSide + "]");
                    };
                    mainHandler.postDelayed(sideSwitchHoldRunnable, SIDE_SWITCH_HOLD_DURATION_MS);
                }
                return true;
            } else if (action == MotionEvent.ACTION_UP) {
                v.setAlpha(1.0f);
                if (playerNumber == currentState.activeServer) {
                    if (sideSwitchVisualRunnable != null) {
                        mainHandler.removeCallbacks(sideSwitchVisualRunnable);
                        sideSwitchVisualRunnable = null;
                    }
                    if (sideSwitchHoldRunnable != null) {
                        mainHandler.removeCallbacks(sideSwitchHoldRunnable);
                        sideSwitchHoldRunnable = null;
                    }
                    if (sideSwitchTriggered) {
                        sideSwitchTriggered = false;
                        sideSwitchGraceUntilUptimeMs = SystemClock.uptimeMillis() + SIDE_SWITCH_GRACE_DURATION_MS;
                        serviceSideView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
                        serviceSideView.setTextColor(Color.WHITE);
                        serviceSideView.setText("[" + currentState.serviceSide + "]");
                        return true;
                    }
                    serviceSideView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
                    serviceSideView.setTextColor(Color.WHITE);
                    serviceSideView.setText("[" + currentState.serviceSide + "]");
                }
                if (watchClient != null) {
                    watchClient.sendCommand(command);
                }
                vibrateOnce(VIBRATE_POINT_MS);
                return true;
            } else if (action == MotionEvent.ACTION_CANCEL) {
                v.setAlpha(1.0f);
                if (playerNumber == currentState.activeServer) {
                    if (sideSwitchVisualRunnable != null) {
                        mainHandler.removeCallbacks(sideSwitchVisualRunnable);
                        sideSwitchVisualRunnable = null;
                    }
                    if (sideSwitchHoldRunnable != null) {
                        mainHandler.removeCallbacks(sideSwitchHoldRunnable);
                        sideSwitchHoldRunnable = null;
                    }
                    if (sideSwitchTriggered) {
                        sideSwitchTriggered = false;
                        sideSwitchGraceUntilUptimeMs = SystemClock.uptimeMillis() + SIDE_SWITCH_GRACE_DURATION_MS;
                    }
                    serviceSideView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
                    serviceSideView.setTextColor(Color.WHITE);
                    serviceSideView.setText("[" + currentState.serviceSide + "]");
                }
                return true;
            }
            return false;
        });

        return tile;
    }

    private View createUndoButton() {
        undoButtonBg = new GradientDrawable();
        undoButtonBg.setCornerRadius(dp(8));
        undoButtonBg.setColor(UNDO_NORMAL);
        undoButtonBg.setStroke(dp(2), UNDO_RIM);

        undoButtonView = createShadowLabel("UNDO", 19);
        undoButtonView.setContentDescription("Undo the last point.");
        undoButtonView.setBackground(undoButtonBg);
        undoButtonView.setClickable(true);
        undoButtonView.setFocusable(true);

        undoButtonView.setOnTouchListener((v, event) -> {
            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN) {
                undoButtonBg.setColor(UNDO_PRESSED);
                vibrateOnce(VIBRATE_UNDO_MS);
                return true;
            } else if (action == MotionEvent.ACTION_UP) {
                undoButtonBg.setColor(UNDO_NORMAL);
                if (watchClient != null) {
                    watchClient.sendCommand(WatchProtocol.CMD_UNDO);
                }
                return true;
            } else if (action == MotionEvent.ACTION_CANCEL) {
                undoButtonBg.setColor(UNDO_NORMAL);
                return true;
            }
            return false;
        });

        return undoButtonView;
    }

    private View createActionButton() {
        actionButtonBg = new GradientDrawable();
        actionButtonBg.setCornerRadius(dp(8));
        actionButtonBg.setColor(ACTION_STOP_NORMAL);
        actionButtonBg.setStroke(dp(2), ACTION_RIM);

        actionButtonView = createShadowLabel("STOP", 19);
        actionButtonView.setContentDescription("Start, stop, or hold 2 seconds to reset the match.");
        actionButtonView.setBackground(actionButtonBg);
        actionButtonView.setClickable(true);
        actionButtonView.setFocusable(true);

        actionButtonView.setOnTouchListener((v, event) -> {
            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN) {
                resetHoldTriggered = false;
                actionButtonBg.setColor(ACTION_PRESSED);
                vibrateOnce(VIBRATE_ACTION_MS);
                mainHandler.postDelayed(showHoldTextRunnable, SHOW_HOLD_TEXT_DELAY_MS);
                mainHandler.postDelayed(resetHoldRunnable, RESET_HOLD_DURATION_MS);
                return true;
            } else if (action == MotionEvent.ACTION_UP) {
                mainHandler.removeCallbacks(showHoldTextRunnable);
                mainHandler.removeCallbacks(resetHoldRunnable);
                updateActionButtonStyle(currentState.actionLabel);
                if (!resetHoldTriggered && watchClient != null) {
                    watchClient.sendCommand(WatchProtocol.CMD_TOGGLE_PAUSE);
                }
                return true;
            } else if (action == MotionEvent.ACTION_CANCEL) {
                mainHandler.removeCallbacks(showHoldTextRunnable);
                mainHandler.removeCallbacks(resetHoldRunnable);
                updateActionButtonStyle(currentState.actionLabel);
                return true;
            }
            return false;
        });

        return actionButtonView;
    }

    private FrameLayout createMatchEndOverlay() {
        FrameLayout overlay = new FrameLayout(this);
        overlay.setBackgroundColor(0xEE080B10);
        overlay.setVisibility(View.GONE);
        overlay.setClickable(true);
        overlay.setFocusable(true);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setPadding(dp(12), dp(12), dp(12), dp(12));

        TextView titleView = createShadowLabel("MATCH ENDED", 15);
        titleView.setTextColor(ACTIVE_SERVER_BORDER_COLOR);

        matchEndWinnerView = createShadowLabel("WINNER", 22);
        matchEndWinnerView.setPadding(0, dp(6), 0, dp(2));

        matchEndScoreView = createShadowLabel("2 - 0", 30);
        matchEndScoreView.setPadding(0, 0, 0, dp(10));

        TextView okButton = createShadowLabel("OK", 22);
        okButton.setContentDescription("Confirm match end.");
        GradientDrawable okBg = new GradientDrawable();
        okBg.setCornerRadius(dp(10));
        okBg.setColor(ACTION_START_NORMAL);
        okBg.setStroke(dp(2), Color.WHITE);
        okButton.setBackground(okBg);
        okButton.setMinHeight(dp(48));
        okButton.setMinimumHeight(dp(48));
        okButton.setOnClickListener(v -> {
            vibrateOnce(VIBRATE_ACTION_MS);
            dismissMatchEndedPopup();
        });

        card.addView(titleView, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        card.addView(matchEndWinnerView, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        card.addView(matchEndScoreView, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        card.addView(okButton, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            dp(54)
        ));

        overlay.addView(card, new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ));
        return overlay;
    }

    private void showMatchEndedPopup(String winnerName, String finalScore) {
        if (matchEndOverlay == null) return;
        matchEndWinnerView.setText(winnerName);
        matchEndScoreView.setText(finalScore);
        matchEndOverlay.setVisibility(View.VISIBLE);
        matchEndOverlay.bringToFront();

        // Temporarily boost screen brightness for 3 seconds, then dim back to active-play level
        mainHandler.removeCallbacks(dimBrightnessRunnable);
        setWindowBrightness(POPUP_BOOST_BRIGHTNESS);
        mainHandler.postDelayed(dimBrightnessRunnable, POPUP_BRIGHTNESS_DURATION_MS);
    }

    private void dismissMatchEndedPopup() {
        if (matchEndOverlay != null) {
            matchEndOverlay.setVisibility(View.GONE);
        }
        mainHandler.removeCallbacks(dimBrightnessRunnable);
        setWindowBrightness(ACTIVE_PLAY_BRIGHTNESS);
        currentState = new WatchProtocol.WatchState(
            currentState.playerOneInitials,
            currentState.playerTwoInitials,
            currentState.playerOnePoints,
            currentState.playerTwoPoints,
            currentState.playerOneSets,
            currentState.playerTwoSets,
            currentState.targetSets,
            currentState.activeServer,
            currentState.serviceSide,
            currentState.playerOneColor,
            currentState.playerOneRainbow,
            currentState.playerTwoColor,
            currentState.playerTwoRainbow,
            "START",
            false,
            true,
            true
        );
        applyWatchState(currentState);
    }

    private void applyWatchState(WatchProtocol.WatchState state) {
        if (p1HeaderView == null || p2HeaderView == null) return;

        String p1SetsText = WatchProtocol.formatSetsIndicator(state.playerOneSets, state.targetSets);
        String p2SetsText = WatchProtocol.formatSetsIndicator(state.playerTwoSets, state.targetSets);

        p1HeaderView.setText(state.playerOneInitials + " " + p1SetsText);
        p2HeaderView.setText(state.playerTwoInitials + " " + p2SetsText);

        p1PointsView.setText(String.valueOf(state.playerOnePoints));
        p2PointsView.setText(String.valueOf(state.playerTwoPoints));

        boolean p1Serves = state.activeServer == 1;
        String sideTag = "[" + state.serviceSide + "]";
        p1ServiceSideView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        p1ServiceSideView.setTextColor(Color.WHITE);
        p1ServiceSideView.setText(p1Serves ? sideTag : "");
        p1ServiceSideView.setVisibility(p1Serves ? View.VISIBLE : View.INVISIBLE);

        p2ServiceSideView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        p2ServiceSideView.setTextColor(Color.WHITE);
        p2ServiceSideView.setText(!p1Serves ? sideTag : "");
        p2ServiceSideView.setVisibility(!p1Serves ? View.VISIBLE : View.INVISIBLE);

        applyTileBackground(p1TileBg, state.playerOneColor, state.playerOneRainbow, p1Serves);
        applyTileBackground(p2TileBg, state.playerTwoColor, state.playerTwoRainbow, !p1Serves);

        updateActionButtonStyle(state.actionLabel);
    }

    private void applyTileBackground(
        GradientDrawable drawable,
        int solidColor,
        boolean isRainbow,
        boolean isActiveServer
    ) {
        if (drawable == null) return;
        drawable.setCornerRadius(dp(8));
        int[] gradientStops = WatchProtocol.getGradientStops(solidColor, isRainbow);
        if (gradientStops != null) {
            drawable.setOrientation(GradientDrawable.Orientation.TL_BR);
            drawable.setColors(gradientStops);
        } else {
            int opaqueColor = 0xFF000000 | solidColor;
            drawable.setColors(new int[]{opaqueColor, opaqueColor});
        }
        int strokeWidth = isActiveServer ? dp(4) : dp(1);
        int strokeColor = isActiveServer ? ACTIVE_SERVER_BORDER_COLOR : INACTIVE_SERVER_BORDER_COLOR;
        drawable.setStroke(strokeWidth, strokeColor);
    }

    private void updateActionButtonStyle(String actionLabel) {
        if (actionButtonView == null || actionButtonBg == null) return;
        String normalized = ("START".equals(actionLabel) || "NEXT".equals(actionLabel)) ? actionLabel : "STOP";
        actionButtonView.setText(normalized);
        int fill = ACTION_STOP_NORMAL;
        if ("START".equals(normalized)) {
            fill = ACTION_START_NORMAL;
        } else if ("NEXT".equals(normalized)) {
            fill = ACTION_NEXT_NORMAL;
        }
        actionButtonBg.setColor(fill);
        actionButtonBg.setStroke(dp(2), ACTION_RIM);
    }

    private void updateStatusIndicator(boolean isConnected) {
        if (statusDotBg == null || statusHitBox == null) return;
        if (isConnected) {
            statusDotBg.setColor(STATUS_CONNECTED_COLOR);
            statusDotBg.setStroke(dp(1), 0xAA000000);
            statusHitBox.setContentDescription("Bluetooth connected.");
        } else {
            statusDotBg.setColor(STATUS_DISCONNECTED_COLOR);
            statusDotBg.setStroke(dp(1), 0xFFFFFFFF);
            statusHitBox.setContentDescription("Bluetooth disconnected. Tap to reconnect.");
        }
        if (statusDotView != null) {
            statusDotView.invalidate();
        }
    }

    private void setReconnectingStatusIndicator() {
        if (statusDotBg == null || statusHitBox == null) return;
        statusDotBg.setColor(STATUS_RECONNECTING_COLOR);
        statusDotBg.setStroke(dp(1), 0xFFFFFFFF);
        statusHitBox.setContentDescription("Connecting to phone.");
        if (statusDotView != null) {
            statusDotView.invalidate();
        }
    }

    private TextView createShadowLabel(String textValue, int textSizeDp) {
        TextView tv = new TextView(this);
        tv.setText(textValue);
        tv.setTextColor(Color.WHITE);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, textSizeDp);
        tv.setGravity(Gravity.CENTER);
        tv.setShadowLayer(4f, 0f, 2f, 0xDD000000);
        return tv;
    }

    private void setWindowBrightness(float brightness) {
        Window window = getWindow();
        if (window == null) return;
        WindowManager.LayoutParams params = window.getAttributes();
        params.screenBrightness = brightness;
        window.setAttributes(params);
    }

    @SuppressWarnings("deprecation")
    private void vibrateOnce(long durationMs) {
        if (vibrator != null && vibrator.hasVibrator()) {
            try {
                vibrator.vibrate(durationMs);
            } catch (Exception ignored) { }
        }
    }

    @SuppressWarnings("deprecation")
    private void vibratePattern(long[] pattern) {
        if (vibrator != null && vibrator.hasVibrator()) {
            try {
                vibrator.vibrate(pattern, -1);
            } catch (Exception ignored) { }
        }
    }

    private int dp(int value) {
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        return (int) (value * metrics.density + 0.5f);
    }
}
