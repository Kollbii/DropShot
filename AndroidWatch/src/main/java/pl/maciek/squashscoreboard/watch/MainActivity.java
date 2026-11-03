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
import android.view.ViewConfiguration;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Standalone Android Watch remote control activity for the LG G Watch (LG-W100, 280x280, API 20-22).
 * Implements:
 * - Low backlight brightness (0.08f) during active play, with a 3-second brightness boost on match end.
 * - Distinct eyes-free vibration patterns (P1 30 ms, P2 2x25 ms, Game/Match ball +150 ms, Undo 80 ms, Action 100 ms).
 * - 75/25 vertical screen split with live score tiles, 3-letter initials, sets dots, active server 4dp border,
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

    private static final int BADGE_UNLOCKED_FILL = 0x2EFFFFFF;
    private static final int BADGE_UNLOCKED_PRESSED = 0x55FFFFFF;
    private static final int BADGE_UNLOCKED_RIM = 0x88FFFFFF;
    private static final int BADGE_LOCKED_FILL = 0x66B71C1C;
    private static final int BADGE_LOCKED_RIM = 0x99EF5350;
    private static final int BADGE_LOCKED_TEXT = 0xCCFFCDD2;
    private static final int BADGE_SERVE_PICK_FILL = 0x24FFFFFF;
    private static final int BADGE_SERVE_PICK_RIM = 0x66FFFFFF;

    private static final int STATUS_CONNECTED_COLOR = 0xFF39B87F;
    private static final int STATUS_DISCONNECTED_COLOR = 0xFFF1636E;
    private static final int STATUS_RECONNECTING_COLOR = 0xFFFFD54F;

    private static final float ACTIVE_PLAY_BRIGHTNESS = 0.2f;
    private static final float POPUP_BOOST_BRIGHTNESS = 0.85f;
    private static final long POPUP_BRIGHTNESS_DURATION_MS = 3000L;
    private static final long RESET_HOLD_DURATION_MS = 2000L;
    private static final long SHOW_HOLD_TEXT_DELAY_MS = 400L;
    private static final long REACTION_DISPLAY_DURATION_MS = 2600L;

    private static final int EDGE_SWIPE_MAX_START_DP = 36;
    private static final int SWIPE_EXIT_MIN_DX_DP = 70;

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
    private GradientDrawable p1ServiceBadgeBg;

    private LinearLayout p2Tile;
    private GradientDrawable p2TileBg;
    private TextView p2HeaderView;
    private TextView p2PointsView;
    private TextView p2ServiceSideView;
    private GradientDrawable p2ServiceBadgeBg;

    // Score reaction badges (Cat 6-7, Shrek 6-9)
    private ImageView reactionBadgeView;
    private int previousP1Points = -1;
    private int previousP2Points = -1;
    private final Runnable hideReactionRunnable = () -> hideReactionBadge(false);

    // Edge-swipe exit and touch slop
    private int touchSlopPx;
    private boolean isEdgeSwipeCandidate;
    private boolean isExitSwiping;
    private float edgeSwipeDownX;
    private float edgeSwipeDownY;

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
        touchSlopPx = Math.max(ViewConfiguration.get(this).getScaledTouchSlop(), dp(8));

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
        mainHandler.removeCallbacks(hideReactionRunnable);
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
        if (ev == null) {
            return false;
        }

        int action = ev.getActionMasked();

        if (action == MotionEvent.ACTION_DOWN) {
            edgeSwipeDownX = ev.getRawX();
            edgeSwipeDownY = ev.getRawY();
            isEdgeSwipeCandidate = edgeSwipeDownX <= dp(EDGE_SWIPE_MAX_START_DP);
            isExitSwiping = false;

            if (watchClient != null && !watchClient.isConnected()) {
                watchClient.requestReconnectNow();
            }
        } else if (action == MotionEvent.ACTION_MOVE) {
            if (isEdgeSwipeCandidate) {
                boolean isExit = WatchProtocol.isEdgeSwipeExit(
                    edgeSwipeDownX,
                    ev.getRawX(),
                    edgeSwipeDownY,
                    ev.getRawY(),
                    dp(EDGE_SWIPE_MAX_START_DP),
                    dp(SWIPE_EXIT_MIN_DX_DP)
                );
                if (isExit) {
                    if (!isExitSwiping) {
                        isExitSwiping = true;
                        MotionEvent cancelEvent = MotionEvent.obtain(ev);
                        cancelEvent.setAction(MotionEvent.ACTION_CANCEL);
                        super.dispatchTouchEvent(cancelEvent);
                        cancelEvent.recycle();
                    }
                    return true;
                }
            }
        } else if (action == MotionEvent.ACTION_UP) {
            if (isExitSwiping) {
                boolean confirmed = (ev.getRawX() - edgeSwipeDownX) >= dp(SWIPE_EXIT_MIN_DX_DP);
                isExitSwiping = false;
                isEdgeSwipeCandidate = false;
                if (confirmed) {
                    finish();
                }
                return true;
            }
            isEdgeSwipeCandidate = false;
        } else if (action == MotionEvent.ACTION_CANCEL) {
            isExitSwiping = false;
            isEdgeSwipeCandidate = false;
        }

        if (isExitSwiping) {
            return true;
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

        // Top row: 75% height (layout_weight="75")
        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams topRowParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 75f
        );
        topRowParams.bottomMargin = gap / 2;

        // Bottom row: 25% height (layout_weight="25")
        LinearLayout bottomRow = new LinearLayout(this);
        bottomRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams bottomRowParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 25f
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

        reactionBadgeView = new ImageView(this);
        reactionBadgeView.setVisibility(View.GONE);
        reactionBadgeView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        reactionBadgeView.setAdjustViewBounds(true);
        reactionBadgeView.setContentDescription("Score reaction graphic.");
        reactionBadgeView.setClickable(true);
        reactionBadgeView.setOnClickListener(v -> hideReactionBadge(true));

        FrameLayout.LayoutParams reactionParams = new FrameLayout.LayoutParams(
            dp(80), dp(80), Gravity.CENTER
        );
        root.addView(reactionBadgeView, reactionParams);

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
        tile.setPadding(dp(6), dp(10), dp(6), dp(4));
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

        // Dedicated service side badge container (Option A at 0-0 + streak lock)
        FrameLayout serviceBadgeContainer = new FrameLayout(this);
        serviceBadgeContainer.setClickable(true);
        serviceBadgeContainer.setFocusable(true);
        serviceBadgeContainer.setContentDescription("Tap to toggle serve side or select starting server.");

        final GradientDrawable badgeBg = new GradientDrawable();
        badgeBg.setCornerRadius(dp(6));
        badgeBg.setColor(BADGE_UNLOCKED_FILL);
        badgeBg.setStroke(dp(1), BADGE_UNLOCKED_RIM);
        if (playerNumber == 1) {
            p1ServiceBadgeBg = badgeBg;
        } else {
            p2ServiceBadgeBg = badgeBg;
        }

        serviceSideView.setPadding(dp(8), dp(2), dp(8), dp(2));
        serviceSideView.setBackground(badgeBg);

        FrameLayout.LayoutParams badgeTextParams = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.CENTER
        );
        serviceBadgeContainer.addView(serviceSideView, badgeTextParams);

        LinearLayout.LayoutParams containerParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            dp(30)
        );
        tile.addView(serviceBadgeContainer, containerParams);

        final float[] badgeDownCoords = new float[2];
        final boolean[] badgeDragDetected = new boolean[1];

        serviceBadgeContainer.setOnTouchListener((v, event) -> {
            boolean isServer = (playerNumber == currentState.activeServer);
            if (!isServer && !currentState.canSelectServer) {
                return false;
            }
            if (isServer && !currentState.canChooseSide) {
                // Locked during a server streak: consume touch so it does not add a point or switch side
                return true;
            }
            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN) {
                badgeDownCoords[0] = event.getX();
                badgeDownCoords[1] = event.getY();
                badgeDragDetected[0] = false;
                badgeBg.setColor(BADGE_UNLOCKED_PRESSED);
                return true;
            } else if (action == MotionEvent.ACTION_MOVE) {
                if (!badgeDragDetected[0]) {
                    if (WatchProtocol.isTouchMovementExceeded(badgeDownCoords[0], event.getX(), badgeDownCoords[1], event.getY(), touchSlopPx)) {
                        badgeDragDetected[0] = true;
                        applyWatchState(currentState);
                    }
                }
                return true;
            } else if (action == MotionEvent.ACTION_UP) {
                if (!badgeDragDetected[0]) {
                    vibratePattern(VIBRATE_DOUBLE_HAPTIC);
                    if (isServer) {
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
                            currentState.canChooseSide,
                            currentState.canSelectServer,
                            currentState.playerOneColor,
                            currentState.playerOneRainbow,
                            currentState.playerTwoColor,
                            currentState.playerTwoRainbow,
                            currentState.actionLabel,
                            currentState.gameOrMatchBall,
                            currentState.matchFinished,
                            currentState.paused
                        );
                    } else {
                        if (watchClient != null) {
                            watchClient.sendCommand(WatchProtocol.CMD_SWITCH_SERVER);
                        }
                        currentState = new WatchProtocol.WatchState(
                            currentState.playerOneInitials,
                            currentState.playerTwoInitials,
                            currentState.playerOnePoints,
                            currentState.playerTwoPoints,
                            currentState.playerOneSets,
                            currentState.playerTwoSets,
                            currentState.targetSets,
                            playerNumber,
                            "R",
                            true,
                            true,
                            currentState.playerOneColor,
                            currentState.playerOneRainbow,
                            currentState.playerTwoColor,
                            currentState.playerTwoRainbow,
                            currentState.actionLabel,
                            currentState.gameOrMatchBall,
                            currentState.matchFinished,
                            currentState.paused
                        );
                    }
                }
                applyWatchState(currentState);
                return true;
            } else if (action == MotionEvent.ACTION_CANCEL) {
                badgeDragDetected[0] = false;
                applyWatchState(currentState);
                return true;
            }
            return false;
        });

        final String command = playerNumber == 1 ? WatchProtocol.CMD_POINT_1 : WatchProtocol.CMD_POINT_2;
        final float[] downCoords = new float[2];
        final boolean[] dragDetected = new boolean[1];

        tile.setOnTouchListener((v, event) -> {
            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN) {
                downCoords[0] = event.getX();
                downCoords[1] = event.getY();
                dragDetected[0] = false;
                v.setAlpha(0.82f);
                return true;
            } else if (action == MotionEvent.ACTION_MOVE) {
                if (!dragDetected[0]) {
                    if (WatchProtocol.isTouchMovementExceeded(downCoords[0], event.getX(), downCoords[1], event.getY(), touchSlopPx)) {
                        dragDetected[0] = true;
                        v.setAlpha(1.0f);
                    }
                }
                return true;
            } else if (action == MotionEvent.ACTION_UP) {
                v.setAlpha(1.0f);
                if (!dragDetected[0]) {
                    if (watchClient != null) {
                        watchClient.sendCommand(command);
                    }
                    vibrateOnce(VIBRATE_POINT_MS);
                }
                return true;
            } else if (action == MotionEvent.ACTION_CANCEL) {
                v.setAlpha(1.0f);
                dragDetected[0] = false;
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

        final float[] undoDownCoords = new float[2];
        final boolean[] undoDragDetected = new boolean[1];

        undoButtonView.setOnTouchListener((v, event) -> {
            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN) {
                undoDownCoords[0] = event.getX();
                undoDownCoords[1] = event.getY();
                undoDragDetected[0] = false;
                undoButtonBg.setColor(UNDO_PRESSED);
                return true;
            } else if (action == MotionEvent.ACTION_MOVE) {
                if (!undoDragDetected[0]) {
                    if (WatchProtocol.isTouchMovementExceeded(undoDownCoords[0], event.getX(), undoDownCoords[1], event.getY(), touchSlopPx)) {
                        undoDragDetected[0] = true;
                        undoButtonBg.setColor(UNDO_NORMAL);
                    }
                }
                return true;
            } else if (action == MotionEvent.ACTION_UP) {
                undoButtonBg.setColor(UNDO_NORMAL);
                if (!undoDragDetected[0]) {
                    vibrateOnce(VIBRATE_UNDO_MS);
                    if (watchClient != null) {
                        watchClient.sendCommand(WatchProtocol.CMD_UNDO);
                    }
                }
                return true;
            } else if (action == MotionEvent.ACTION_CANCEL) {
                undoDragDetected[0] = false;
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

        final float[] actionDownCoords = new float[2];
        final boolean[] actionDragDetected = new boolean[1];

        actionButtonView.setOnTouchListener((v, event) -> {
            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN) {
                actionDownCoords[0] = event.getX();
                actionDownCoords[1] = event.getY();
                actionDragDetected[0] = false;
                resetHoldTriggered = false;
                actionButtonBg.setColor(ACTION_PRESSED);
                mainHandler.postDelayed(showHoldTextRunnable, SHOW_HOLD_TEXT_DELAY_MS);
                mainHandler.postDelayed(resetHoldRunnable, RESET_HOLD_DURATION_MS);
                return true;
            } else if (action == MotionEvent.ACTION_MOVE) {
                if (!actionDragDetected[0]) {
                    if (WatchProtocol.isTouchMovementExceeded(actionDownCoords[0], event.getX(), actionDownCoords[1], event.getY(), touchSlopPx)) {
                        actionDragDetected[0] = true;
                        mainHandler.removeCallbacks(showHoldTextRunnable);
                        mainHandler.removeCallbacks(resetHoldRunnable);
                        updateActionButtonStyle(currentState.actionLabel);
                    }
                }
                return true;
            } else if (action == MotionEvent.ACTION_UP) {
                mainHandler.removeCallbacks(showHoldTextRunnable);
                mainHandler.removeCallbacks(resetHoldRunnable);
                updateActionButtonStyle(currentState.actionLabel);
                if (!actionDragDetected[0] && !resetHoldTriggered) {
                    vibrateOnce(VIBRATE_ACTION_MS);
                    if (watchClient != null) {
                        watchClient.sendCommand(WatchProtocol.CMD_TOGGLE_PAUSE);
                    }
                }
                return true;
            } else if (action == MotionEvent.ACTION_CANCEL) {
                actionDragDetected[0] = false;
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

        applyServiceBadgeStyle(
            p1ServiceSideView,
            p1ServiceBadgeBg,
            p1Serves,
            sideTag,
            state.canChooseSide,
            state.canSelectServer
        );
        applyServiceBadgeStyle(
            p2ServiceSideView,
            p2ServiceBadgeBg,
            !p1Serves,
            sideTag,
            state.canChooseSide,
            state.canSelectServer
        );

        applyTileBackground(p1TileBg, state.playerOneColor, state.playerOneRainbow, p1Serves);
        applyTileBackground(p2TileBg, state.playerTwoColor, state.playerTwoRainbow, !p1Serves);

        updateActionButtonStyle(state.actionLabel);

        // Check for special score reactions (Cat 6-7, Shrek 6-9)
        boolean scoreChanged = (state.playerOnePoints != previousP1Points || state.playerTwoPoints != previousP2Points);
        if (scoreChanged && !state.matchFinished) {
            if (WatchProtocol.isReaction67Score(state.playerOnePoints, state.playerTwoPoints)) {
                if (!WatchProtocol.isReaction67Score(previousP1Points, previousP2Points)) {
                    showReactionBadge(R.drawable.reaction_cat_67);
                }
            } else if (WatchProtocol.isReaction69Score(state.playerOnePoints, state.playerTwoPoints)) {
                if (!WatchProtocol.isReaction69Score(previousP1Points, previousP2Points)) {
                    showReactionBadge(R.drawable.reaction_shrek_69);
                }
            } else {
                hideReactionBadge(false);
            }
        } else if (state.matchFinished) {
            hideReactionBadge(true);
        }
        previousP1Points = state.playerOnePoints;
        previousP2Points = state.playerTwoPoints;
    }

    private void applyServiceBadgeStyle(
        TextView badgeView,
        GradientDrawable badgeBg,
        boolean isServer,
        String sideTag,
        boolean canChooseSide,
        boolean canSelectServer
    ) {
        if (badgeView == null || badgeBg == null) return;
        badgeBg.setCornerRadius(dp(6));
        if (isServer) {
            badgeView.setVisibility(View.VISIBLE);
            badgeView.setText(sideTag);
            badgeView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
            if (canChooseSide) {
                badgeView.setTextColor(Color.WHITE);
                badgeBg.setColor(BADGE_UNLOCKED_FILL);
                badgeBg.setStroke(dp(1), BADGE_UNLOCKED_RIM);
            } else {
                // Locked during a server streak: small dimmed red background, switching disabled
                badgeView.setTextColor(BADGE_LOCKED_TEXT);
                badgeBg.setColor(BADGE_LOCKED_FILL);
                badgeBg.setStroke(dp(1), BADGE_LOCKED_RIM);
            }
        } else if (canSelectServer) {
            badgeView.setVisibility(View.VISIBLE);
            badgeView.setText("[SERVE]");
            badgeView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
            badgeView.setTextColor(0xDDFFFFFF);
            badgeBg.setColor(BADGE_SERVE_PICK_FILL);
            badgeBg.setStroke(dp(1), BADGE_SERVE_PICK_RIM);
        } else {
            badgeView.setVisibility(View.INVISIBLE);
            badgeView.setText("");
            badgeBg.setColor(Color.TRANSPARENT);
            badgeBg.setStroke(0, Color.TRANSPARENT);
        }
    }

    private void showReactionBadge(int drawableResId) {
        if (reactionBadgeView == null) return;
        mainHandler.removeCallbacks(hideReactionRunnable);
        reactionBadgeView.animate().cancel();

        reactionBadgeView.setImageResource(drawableResId);
        reactionBadgeView.setVisibility(View.VISIBLE);
        reactionBadgeView.setAlpha(0.0f);
        reactionBadgeView.setScaleX(0.2f);
        reactionBadgeView.setScaleY(0.2f);

        reactionBadgeView.animate()
            .alpha(1.0f)
            .scaleX(1.0f)
            .scaleY(1.0f)
            .setDuration(220L)
            .setInterpolator(new OvershootInterpolator(2.2f))
            .withEndAction(() -> mainHandler.postDelayed(hideReactionRunnable, REACTION_DISPLAY_DURATION_MS))
            .start();
    }

    private void hideReactionBadge(boolean immediate) {
        if (reactionBadgeView == null || reactionBadgeView.getVisibility() != View.VISIBLE) {
            return;
        }
        mainHandler.removeCallbacks(hideReactionRunnable);
        if (immediate) {
            reactionBadgeView.animate().cancel();
            reactionBadgeView.setVisibility(View.GONE);
            return;
        }
        reactionBadgeView.animate()
            .alpha(0.0f)
            .scaleX(0.85f)
            .scaleY(0.85f)
            .setDuration(600L)
            .withEndAction(() -> reactionBadgeView.setVisibility(View.GONE))
            .start();
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
