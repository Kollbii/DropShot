package pl.maciek.squashscoreboard.lootbox;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Self-contained placeholder lootbox screen; the host app only needs an entry point. */
public final class LootboxActivity extends Activity {
    private static final int BACKGROUND = 0xFF171A22;
    private static final int FOREGROUND = 0xFFF5F2EA;
    private static final int MUTED = 0xFFB5BAC5;
    private static final int ACCENT = 0xFFE6AD54;

    private final TierRoller roller = new TierRoller();
    private ValueAnimator ballAnimator;
    private ValueAnimator reelAnimator;
    private BallView ball;
    private ReelView reel;
    private Button openButton;
    private TextView resultLabel;
    private LootTier lastResult;
    private boolean opening;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        buildScreen();
    }

    @Override public void onConfigurationChanged(Configuration config) {
        super.onConfigurationChanged(config);
        // Keep the ongoing animations and their views on orientation changes.
    }

    private void buildScreen() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BACKGROUND);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.CENTER_HORIZONTAL);
        content.setPadding(dp(20), dp(20), dp(20), dp(28));
        scroll.addView(content);

        Button back = new Button(this);
        back.setText("← Back");
        back.setAllCaps(false);
        back.setTextColor(FOREGROUND);
        back.setBackgroundColor(0xFF303642);
        back.setOnClickListener(v -> finish());
        LinearLayout.LayoutParams backParams = new LinearLayout.LayoutParams(dp(110), dp(48));
        backParams.gravity = Gravity.START;
        content.addView(back, backParams);

        TextView title = label("SQUASH LOOTBOX", 28, FOREGROUND);
        title.setTypeface(null, 1);
        title.setGravity(Gravity.CENTER);
        content.addView(title, new LinearLayout.LayoutParams(-1, dp(48)));
        TextView subtitle = label("Crack the ball. See where the reel stops.", 16, MUTED);
        subtitle.setGravity(Gravity.CENTER);
        content.addView(subtitle, new LinearLayout.LayoutParams(-1, dp(40)));

        ball = new BallView();
        content.addView(ball, new LinearLayout.LayoutParams(dp(176), dp(176)));

        openButton = new Button(this);
        openButton.setText("Open lootbox");
        openButton.setAllCaps(false);
        openButton.setTextSize(18);
        openButton.setTypeface(null, 1);
        openButton.setTextColor(BACKGROUND);
        openButton.setBackgroundColor(ACCENT);
        openButton.setOnClickListener(v -> open());
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(dp(220), dp(52));
        buttonParams.topMargin = dp(12);
        content.addView(openButton, buttonParams);

        reel = new ReelView();
        reel.setVisibility(View.INVISIBLE);
        LinearLayout.LayoutParams reelParams = new LinearLayout.LayoutParams(-1, dp(130));
        reelParams.topMargin = dp(20);
        content.addView(reel, reelParams);

        resultLabel = label("", 20, FOREGROUND);
        resultLabel.setGravity(Gravity.CENTER);
        resultLabel.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        content.addView(resultLabel, new LinearLayout.LayoutParams(-1, dp(42)));

        TextView odds = label("Gray 70%   ·   Green 25%   ·   Purple 4%   ·   Gold 1%", 14, MUTED);
        odds.setGravity(Gravity.CENTER);
        content.addView(odds, new LinearLayout.LayoutParams(-1, dp(48)));
        setContentView(scroll);
    }

    private void open() {
        if (opening) return;
        opening = true;
        lastResult = roller.roll();
        openButton.setEnabled(false);
        openButton.setAlpha(0.55f);
        resultLabel.setText("");
        ball.setCrack(0f);
        ball.setVisibility(View.VISIBLE);
        reel.setVisibility(View.INVISIBLE);

        if (reduceMotion()) {
            ball.setCrack(1f);
            startReel(true);
            return;
        }
        ballAnimator = ValueAnimator.ofFloat(0f, 1f);
        ballAnimator.setDuration(720);
        ballAnimator.addUpdateListener(a -> ball.setCrack((float) a.getAnimatedValue()));
        ballAnimator.addListener(new AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(Animator animation) {
                if (opening) startReel(false);
            }
        });
        ballAnimator.start();
    }

    private void startReel(boolean instant) {
        ball.setVisibility(View.INVISIBLE);
        reel.prepare(lastResult);
        reel.setVisibility(View.VISIBLE);
        if (instant) {
            reel.setProgress(1f);
            finishOpen();
            return;
        }
        reelAnimator = ValueAnimator.ofFloat(0f, 1f);
        reelAnimator.setDuration(5200);
        reelAnimator.setInterpolator(new DecelerateInterpolator(2.4f));
        reelAnimator.addUpdateListener(a -> reel.setProgress((float) a.getAnimatedValue()));
        reelAnimator.addListener(new AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(Animator animation) {
                if (opening) finishOpen();
            }
        });
        reelAnimator.start();
    }

    private void finishOpen() {
        opening = false;
        resultLabel.setText("You got: " + lastResult.label + " square");
        resultLabel.setTextColor(lastResult.color);
        openButton.setText("Open another lootbox");
        openButton.setEnabled(true);
        openButton.setAlpha(1f);
    }

    @Override protected void onDestroy() {
        opening = false;
        if (ballAnimator != null) ballAnimator.cancel();
        if (reelAnimator != null) reelAnimator.cancel();
        super.onDestroy();
    }

    private boolean reduceMotion() {
        try {
            return Settings.Global.getFloat(getContentResolver(), Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f;
        } catch (Exception ignored) {
            return false;
        }
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private TextView label(String value, int size, int color) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        return view;
    }

    private final class BallView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private float crack;

        BallView() { super(LootboxActivity.this); }

        void setCrack(float value) { crack = value; invalidate(); }

        @Override protected void onDraw(Canvas canvas) {
            float cx = getWidth() / 2f, cy = getHeight() / 2f;
            float radius = Math.min(getWidth(), getHeight()) * 0.39f;
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(0xFF111318);
            canvas.drawCircle(cx, cy, radius, paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(4));
            paint.setColor(ACCENT);
            canvas.drawCircle(cx, cy, radius, paint);
            paint.setStrokeWidth(dp(2));
            paint.setColor(0xFF737A86);
            canvas.drawArc(cx - radius * .72f, cy - radius * .72f, cx + radius * .72f,
                cy + radius * .72f, 58, 215, false, paint);
            canvas.drawArc(cx - radius * .56f, cy - radius * .56f, cx + radius * .56f,
                cy + radius * .56f, 238, 178, false, paint);
            if (crack > 0f) {
                paint.setColor(0xFFFFE4B2);
                paint.setStrokeWidth(dp(3));
                Path path = new Path();
                path.moveTo(cx - radius * .12f, cy - radius * .9f);
                path.lineTo(cx + radius * .15f, cy - radius * .45f);
                path.lineTo(cx - radius * .18f, cy - radius * .12f);
                path.lineTo(cx + radius * .2f, cy + radius * .18f);
                path.lineTo(cx - radius * .1f, cy + radius * .53f);
                path.lineTo(cx + radius * .12f, cy + radius * .9f);
                canvas.save();
                canvas.clipRect(cx - radius, cy - radius, cx + radius, cy - radius + 2 * radius * crack);
                canvas.drawPath(path, paint);
                canvas.restore();
            }
        }
    }

    private final class ReelView extends View {
        private static final int TARGET = 42;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final LootTier[] strip = new LootTier[50];
        private float progress;

        ReelView() { super(LootboxActivity.this); }

        void prepare(LootTier result) {
            // Filler colors are visual only. The selected result is fixed before the animation.
            LootTier[] tiers = LootTier.values();
            for (int i = 0; i < strip.length; i++) {
                strip[i] = tiers[(i * 17 + i / 4) % tiers.length];
            }
            strip[TARGET] = result;
            progress = 0f;
            invalidate();
        }

        void setProgress(float value) { progress = value; invalidate(); }

        @Override protected void onDraw(Canvas canvas) {
            int save = canvas.save();
            canvas.clipRect(0, 0, getWidth(), getHeight());
            float size = dp(72), step = dp(84), center = getWidth() / 2f;
            float offset = TARGET * step * progress;
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(0xFF262B35);
            canvas.drawRoundRect(0, 0, getWidth(), getHeight(), dp(14), dp(14), paint);
            for (int i = 0; i < strip.length; i++) {
                float x = center + i * step - offset;
                if (x + size / 2 < 0 || x - size / 2 > getWidth()) continue;
                paint.setColor(strip[i].color);
                canvas.drawRoundRect(x - size / 2, dp(26), x + size / 2,
                    dp(26) + size, dp(8), dp(8), paint);
            }
            paint.setColor(ACCENT);
            canvas.drawRect(center - dp(2), 0, center + dp(2), dp(20), paint);
            canvas.drawRect(center - dp(2), getHeight() - dp(20), center + dp(2), getHeight(), paint);
            canvas.restoreToCount(save);
        }
    }
}
