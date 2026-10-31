package pl.maciek.squashscoreboard.watch;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Vibrator;
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
 * Replicates the 4-button layout and command behavior of ZeppOS:
 * - Top-Left (+1): Add 1 point to Player 1 (POINT_1)
 * - Top-Right (+2): Add 1 point to Player 2 (POINT_2)
 * - Bottom-Left (<<): Undo the last point (UNDO)
 * - Bottom-Right (R): Finish current match and start a new match (RESET)
 */
public final class MainActivity extends Activity {
    private static final int COLOR_BG = 0xFF080B10;

    private static final int P1_NORMAL = 0xFF1479D1;
    private static final int P1_PRESSED = 0xFF0C559C;
    private static final int P1_RIM = 0xFF2897EE;

    private static final int P2_NORMAL = 0xFFD8424F;
    private static final int P2_PRESSED = 0xFF9F2531;
    private static final int P2_RIM = 0xFFF1636E;

    private static final int UNDO_NORMAL = 0xFF3C4B59;
    private static final int UNDO_PRESSED = 0xFF26323D;
    private static final int UNDO_RIM = 0xFF627487;

    private static final int RESET_NORMAL = 0xFF157C68;
    private static final int RESET_PRESSED = 0xFF0D594B;
    private static final int RESET_RIM = 0xFF28A58C;

    private static final int FLASH_ACCEPTED = 0xFF19B96B;
    private static final int FLASH_REJECTED = 0xFFD8455D;

    private static final long VIBRATION_MS = 35L;
    private static final long FLASH_DURATION_MS = 320L;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private BluetoothWatchClient watchClient;
    private Vibrator vibrator;
    private TextView statusBadge;
    private GradientDrawable statusBadgeBg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_FULLSCREEN
        );

        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        watchClient = new BluetoothWatchClient(mainHandler, this::updateStatusIndicator);

        setContentView(createWatchLayout());
        updateStatusIndicator(false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (watchClient != null) {
            watchClient.start();
            updateStatusIndicator(watchClient.isConnected());
        }
    }

    @Override
    protected void onPause() {
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

    private View createWatchLayout() {
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(COLOR_BG);

        int gap = dp(4);
        LinearLayout grid = new LinearLayout(this);
        grid.setOrientation(LinearLayout.VERTICAL);
        grid.setBackgroundColor(COLOR_BG);

        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams topRowParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
        );
        topRowParams.bottomMargin = gap / 2;

        LinearLayout bottomRow = new LinearLayout(this);
        bottomRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams bottomRowParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
        );
        bottomRowParams.topMargin = gap / 2;

        View btnPoint1 = createQuadrantButton(
            "+1",
            "Add 1 point to Player 1.",
            WatchProtocol.CMD_POINT_1,
            P1_NORMAL,
            P1_PRESSED,
            P1_RIM,
            34
        );
        View btnPoint2 = createQuadrantButton(
            "+2",
            "Add 1 point to Player 2.",
            WatchProtocol.CMD_POINT_2,
            P2_NORMAL,
            P2_PRESSED,
            P2_RIM,
            34
        );
        View btnUndo = createQuadrantButton(
            "<<",
            "Undo the last point.",
            WatchProtocol.CMD_UNDO,
            UNDO_NORMAL,
            UNDO_PRESSED,
            UNDO_RIM,
            34
        );
        View btnReset = createQuadrantButton(
            "R",
            "Start or stop the match.",
            WatchProtocol.CMD_RESET,
            RESET_NORMAL,
            RESET_PRESSED,
            RESET_RIM,
            38
        );

        LinearLayout.LayoutParams leftCell = new LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.MATCH_PARENT, 1f
        );
        leftCell.rightMargin = gap / 2;

        LinearLayout.LayoutParams rightCell = new LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.MATCH_PARENT, 1f
        );
        rightCell.leftMargin = gap / 2;

        topRow.addView(btnPoint1, leftCell);
        topRow.addView(btnPoint2, rightCell);

        LinearLayout.LayoutParams bottomLeftCell = new LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.MATCH_PARENT, 1f
        );
        bottomLeftCell.rightMargin = gap / 2;

        LinearLayout.LayoutParams bottomRightCell = new LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.MATCH_PARENT, 1f
        );
        bottomRightCell.leftMargin = gap / 2;

        bottomRow.addView(btnUndo, bottomLeftCell);
        bottomRow.addView(btnReset, bottomRightCell);

        grid.addView(topRow, topRowParams);
        grid.addView(bottomRow, bottomRowParams);
        root.addView(grid, new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ));

        // Compact Bluetooth connection status pill centered between the 4 quadrants
        statusBadge = new TextView(this);
        statusBadge.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 10);
        statusBadge.setTypeface(Typeface.DEFAULT_BOLD);
        statusBadge.setTextColor(Color.WHITE);
        statusBadge.setGravity(Gravity.CENTER);
        statusBadge.setPadding(dp(8), dp(2), dp(8), dp(2));

        statusBadgeBg = new GradientDrawable();
        statusBadgeBg.setCornerRadius(dp(10));
        statusBadgeBg.setColor(0xDD141923);
        statusBadgeBg.setStroke(dp(1), 0xFF627487);
        statusBadge.setBackground(statusBadgeBg);
        statusBadge.setOnClickListener(v -> {
            vibrateShort();
            if (watchClient != null) {
                watchClient.requestReconnectNow();
            }
        });

        FrameLayout.LayoutParams badgeParams = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.CENTER
        );
        root.addView(statusBadge, badgeParams);

        return root;
    }

    private View createQuadrantButton(
        final String symbol,
        final String contentDesc,
        final String command,
        final int normalColor,
        final int pressedColor,
        final int rimColor,
        final int textDp
    ) {
        final GradientDrawable surfaceDrawable = new GradientDrawable();
        surfaceDrawable.setColor(normalColor);
        surfaceDrawable.setStroke(dp(2), rimColor);

        final TextView buttonView = new TextView(this);
        buttonView.setText(symbol);
        buttonView.setContentDescription(contentDesc);
        buttonView.setTextColor(Color.WHITE);
        buttonView.setTypeface(Typeface.DEFAULT_BOLD);
        buttonView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, textDp);
        buttonView.setGravity(Gravity.CENTER);
        buttonView.setBackground(surfaceDrawable);
        buttonView.setClickable(true);
        buttonView.setFocusable(true);

        buttonView.setOnTouchListener((v, event) -> {
            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN) {
                surfaceDrawable.setColor(pressedColor);
                vibrateShort();
                return true;
            } else if (action == MotionEvent.ACTION_UP) {
                surfaceDrawable.setColor(normalColor);
                triggerCommand(command, surfaceDrawable, normalColor, rimColor);
                return true;
            } else if (action == MotionEvent.ACTION_CANCEL) {
                surfaceDrawable.setColor(normalColor);
                return true;
            }
            return false;
        });

        return buttonView;
    }

    private void triggerCommand(
        final String command,
        final GradientDrawable surfaceDrawable,
        final int normalColor,
        final int rimColor
    ) {
        if (watchClient == null) return;
        watchClient.sendCommand(command, accepted -> {
            final int flashColor = accepted ? FLASH_ACCEPTED : FLASH_REJECTED;
            surfaceDrawable.setColor(flashColor);
            surfaceDrawable.setStroke(dp(2), Color.WHITE);
            mainHandler.postDelayed(() -> {
                surfaceDrawable.setColor(normalColor);
                surfaceDrawable.setStroke(dp(2), rimColor);
            }, FLASH_DURATION_MS);
        });
    }

    private void updateStatusIndicator(boolean isConnected) {
        if (statusBadge == null || statusBadgeBg == null) return;
        if (isConnected) {
            statusBadge.setText("Connected");
            statusBadge.setContentDescription("Bluetooth status: Connected.");
            statusBadgeBg.setColor(0xDD0D3B2E);
            statusBadgeBg.setStroke(dp(1), 0xFF39B87F);
        } else {
            statusBadge.setText("Disconnected");
            statusBadge.setContentDescription("Bluetooth status: Disconnected. Tap to reconnect.");
            statusBadgeBg.setColor(0xDD2A1215);
            statusBadgeBg.setStroke(dp(1), 0xFFF1636E);
        }
    }

    @SuppressWarnings("deprecation")
    private void vibrateShort() {
        if (vibrator != null && vibrator.hasVibrator()) {
            try {
                vibrator.vibrate(VIBRATION_MS);
            } catch (Exception ignored) { }
        }
    }

    private int dp(int value) {
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        return (int) (value * metrics.density + 0.5f);
    }
}

