package pl.maciek.squashscoreboard.lootbox;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Self-contained placeholder lootbox screen; the host app only needs an entry point. */
public final class LootboxActivity extends Activity {
    private static final int BACKGROUND = 0xFF171A22;
    private static final int FOREGROUND = 0xFFF5F2EA;
    private static final int MUTED = 0xFFB5BAC5;
    private static final int ACCENT = 0xFFE6AD54;
    private static final int IDLE = 0, SHAKING = 1, CRACKING = 2, POPPING = 3, SPINNING = 4, WON = 5;

    private final TierRoller roller = new TierRoller();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable revealTask = this::showReward;
    private ValueAnimator phaseAnimator;
    private FrameLayout rewardOverlay;
    private LinearLayout rewardCard;
    private BallView ball;
    private ReelView reel;
    private Button openButton;
    private TextView resultLabel;
    private LootTier lastResult;
    private int phase = IDLE;
    private float shakeProgress, crackProgress, popProgress, reelProgress;
    private boolean landscape;
    private boolean rewardDismissed;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        buildScreen();
    }

    @Override public void onConfigurationChanged(Configuration config) {
        super.onConfigurationChanged(config);
        buildScreen(); // Keep the current phase and animator, but fit the new screen dimensions.
    }

    private void buildScreen() {
        landscape = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(BACKGROUND);
        LinearLayout content = column();
        content.setPadding(dp(landscape ? 12 : 20), dp(landscape ? 4 : 16),
            dp(landscape ? 12 : 20), dp(landscape ? 4 : 16));
        root.addView(content, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        Button back = new Button(this);
        back.setText("← Back");
        back.setAllCaps(false);
        back.setTextColor(FOREGROUND);
        back.setBackgroundColor(0xFF303642);
        back.setOnClickListener(v -> finish());
        header.addView(back, new LinearLayout.LayoutParams(dp(landscape ? 92 : 100), dp(44)));
        TextView title = label("SQUASH LOOTBOX", landscape ? 22 : 24, FOREGROUND);
        title.setTypeface(null, 1);
        title.setGravity(Gravity.CENTER);
        if (landscape) {
            header.addView(title, new LinearLayout.LayoutParams(0, dp(48), 1));
            header.addView(new View(this), new LinearLayout.LayoutParams(dp(92), 1));
        }
        content.addView(header, new LinearLayout.LayoutParams(-1, dp(48)));

        if (!landscape) {
            content.addView(title, new LinearLayout.LayoutParams(-1, dp(40)));
            TextView subtitle = label("Crack the ball. See where the reel stops.", 16, MUTED);
            subtitle.setGravity(Gravity.CENTER);
            content.addView(subtitle, new LinearLayout.LayoutParams(-1, dp(32)));
        }

        LinearLayout body = new LinearLayout(this);
        body.setOrientation(landscape ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);
        body.setGravity(Gravity.CENTER);
        content.addView(body, new LinearLayout.LayoutParams(-1, 0, 1));

        LinearLayout ballSide = column();
        ballSide.setGravity(Gravity.CENTER_HORIZONTAL);
        LinearLayout reelSide = column();
        reelSide.setGravity(Gravity.CENTER_HORIZONTAL);
        if (landscape) {
            LinearLayout.LayoutParams left = new LinearLayout.LayoutParams(0, -1, 1);
            left.rightMargin = dp(12);
            body.addView(ballSide, left);
            body.addView(reelSide, new LinearLayout.LayoutParams(0, -1, 1.3f));
        } else {
            body.addView(ballSide, new LinearLayout.LayoutParams(-1, 0, 1));
            body.addView(reelSide, new LinearLayout.LayoutParams(-1, dp(188)));
        }

        ball = new BallView();
        ballSide.addView(ball, new LinearLayout.LayoutParams(-1, 0, 1));
        openButton = new Button(this);
        openButton.setText("Open lootbox");
        openButton.setAllCaps(false);
        openButton.setTextSize(landscape ? 16 : 18);
        openButton.setTypeface(null, 1);
        openButton.setTextColor(BACKGROUND);
        openButton.setBackgroundColor(ACCENT);
        openButton.setOnClickListener(v -> open());
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(-1, dp(48));
        buttonParams.setMargins(dp(8), dp(4), dp(8), dp(4));
        ballSide.addView(openButton, buttonParams);

        reel = new ReelView();
        reelSide.addView(reel, new LinearLayout.LayoutParams(-1, 0, 1));
        resultLabel = label("", landscape ? 17 : 20, FOREGROUND);
        resultLabel.setGravity(Gravity.CENTER);
        resultLabel.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        reelSide.addView(resultLabel, new LinearLayout.LayoutParams(-1, dp(44)));

        TextView odds = label("Gray 70%   ·   Green 25%   ·   Purple 4%   ·   Gold 1%",
            landscape ? 12 : 14, MUTED);
        odds.setGravity(Gravity.CENTER);
        odds.setSingleLine(true);
        content.addView(odds, new LinearLayout.LayoutParams(-1, dp(landscape ? 26 : 40)));

        rewardOverlay = new FrameLayout(this);
        rewardOverlay.setBackgroundColor(0xE912151B);
        rewardOverlay.setVisibility(View.GONE);
        root.addView(rewardOverlay, new FrameLayout.LayoutParams(-1, -1));
        rewardCard = column();
        rewardCard.setGravity(Gravity.CENTER_HORIZONTAL);
        rewardCard.setPadding(dp(16), dp(10), dp(16), dp(12));
        rewardCard.setBackgroundColor(0xFF2B303A);
        FrameLayout.LayoutParams cardParams = new FrameLayout.LayoutParams(
            dp(landscape ? 270 : 300), -2, Gravity.CENTER);
        rewardOverlay.addView(rewardCard, cardParams);

        renderState();
        setContentView(root);
    }

    private void renderState() {
        ball.setShake(shakeProgress);
        ball.setCrack(crackProgress);
        ball.setVisibility(phase >= POPPING ? View.INVISIBLE : View.VISIBLE);
        reel.setVisibility(phase >= POPPING ? View.VISIBLE : View.INVISIBLE);
        if (lastResult != null) {
            reel.prepare(lastResult);
            reel.setProgress(reelProgress);
        }
        applyPop();
        openButton.setEnabled(phase == IDLE || phase == WON);
        openButton.setAlpha(openButton.isEnabled() ? 1f : .55f);
        openButton.setText(phase == WON ? "Open another lootbox" : "Open lootbox");
        if (phase == WON && lastResult != null) {
            resultLabel.setText("You got: " + lastResult.label + " square");
            resultLabel.setTextColor(lastResult.color);
            buildRewardCard();
            rewardOverlay.setVisibility(rewardDismissed ? View.GONE : View.VISIBLE);
        } else {
            resultLabel.setText("");
            rewardOverlay.setVisibility(View.GONE);
        }
    }

    private void open() {
        if (phase != IDLE && phase != WON) return;
        handler.removeCallbacks(revealTask);
        if (phaseAnimator != null) phaseAnimator.cancel();
        phase = SHAKING;
        rewardDismissed = false;
        lastResult = roller.roll();
        shakeProgress = crackProgress = popProgress = reelProgress = 0f;
        renderState();
        if (reduceMotion()) {
            crackProgress = popProgress = reelProgress = 1f;
            phase = WON;
            renderState();
            return;
        }
        animatePhase(1000, null, value -> {
            shakeProgress = value;
            ball.setShake(value);
        }, this::crackBall);
    }

    private void crackBall() {
        phase = CRACKING;
        shakeProgress = 0f;
        ball.setShake(0f);
        animatePhase(1200, new DecelerateInterpolator(1.1f), value -> {
            crackProgress = value;
            ball.setCrack(value);
        }, this::popReel);
    }

    private void popReel() {
        phase = POPPING;
        ball.setVisibility(View.INVISIBLE);
        reel.prepare(lastResult);
        reel.setVisibility(View.VISIBLE);
        animatePhase(650, new OvershootInterpolator(1.3f), value -> {
            popProgress = value;
            applyPop();
        }, this::spinReel);
    }

    private void applyPop() {
        if (phase < POPPING) return;
        float scale = .2f + .8f * popProgress;
        reel.setScaleX(scale);
        reel.setScaleY(scale);
        reel.setAlpha(Math.min(1f, Math.max(0f, popProgress * 1.8f)));
        if (landscape) {
            reel.setTranslationX(-dp(120) * (1f - popProgress));
            reel.setTranslationY(0);
        } else {
            reel.setTranslationY(-dp(100) * (1f - popProgress));
            reel.setTranslationX(0);
        }
    }

    private void spinReel() {
        phase = SPINNING;
        popProgress = 1f;
        applyPop();
        animatePhase(7000, new DecelerateInterpolator(1.7f), value -> {
            reelProgress = value;
            reel.setProgress(value);
        }, () -> handler.postDelayed(revealTask, 500));
    }

    private void showReward() {
        if (phase != SPINNING) return;
        phase = WON;
        rewardDismissed = false;
        reelProgress = 1f;
        resultLabel.setText("You got: " + lastResult.label + " square");
        resultLabel.setTextColor(lastResult.color);
        openButton.setText("Open another lootbox");
        openButton.setEnabled(true);
        openButton.setAlpha(1f);
        buildRewardCard();
        rewardOverlay.setVisibility(View.VISIBLE);
        if (!reduceMotion()) {
            rewardCard.setScaleX(.35f);
            rewardCard.setScaleY(.35f);
            rewardCard.setAlpha(0f);
            rewardCard.animate().scaleX(1f).scaleY(1f).alpha(1f)
                .setInterpolator(new OvershootInterpolator(1.5f)).setDuration(900).start();
        }
    }

    private void buildRewardCard() {
        rewardCard.removeAllViews();
        TextView heading = label("YOU WON!", landscape ? 22 : 26, ACCENT);
        heading.setTypeface(null, 1);
        heading.setGravity(Gravity.CENTER);
        rewardCard.addView(heading, new LinearLayout.LayoutParams(-1, dp(36)));
        View square = new View(this);
        square.setBackgroundColor(lastResult.color);
        LinearLayout.LayoutParams squareParams = new LinearLayout.LayoutParams(dp(landscape ? 82 : 108), dp(landscape ? 82 : 108));
        squareParams.topMargin = dp(10);
        squareParams.bottomMargin = dp(10);
        rewardCard.addView(square, squareParams);
        TextView name = label(lastResult.label.toUpperCase() + " SQUARE", landscape ? 20 : 24, lastResult.color);
        name.setTypeface(null, 1);
        name.setGravity(Gravity.CENTER);
        rewardCard.addView(name, new LinearLayout.LayoutParams(-1, dp(38)));
        Button continueButton = new Button(this);
        continueButton.setText("Continue");
        continueButton.setAllCaps(false);
        continueButton.setTextColor(BACKGROUND);
        continueButton.setBackgroundColor(ACCENT);
        continueButton.setOnClickListener(v -> {
            rewardDismissed = true;
            rewardOverlay.setVisibility(View.GONE);
        });
        LinearLayout.LayoutParams continueParams = new LinearLayout.LayoutParams(-1, dp(44));
        continueParams.topMargin = dp(8);
        rewardCard.addView(continueButton, continueParams);
        rewardOverlay.setContentDescription("You won a " + lastResult.label + " square");
    }

    private interface ProgressUpdate { void update(float value); }

    private void animatePhase(long duration, android.animation.TimeInterpolator interpolator,
                              ProgressUpdate update, Runnable next) {
        ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
        phaseAnimator = animator;
        animator.setDuration(duration);
        if (interpolator != null) animator.setInterpolator(interpolator);
        animator.addUpdateListener(value -> update.update((float) value.getAnimatedValue()));
        animator.addListener(new AnimatorListenerAdapter() {
            private boolean cancelled;
            @Override public void onAnimationCancel(Animator animation) { cancelled = true; }
            @Override public void onAnimationEnd(Animator animation) {
                if (!cancelled && !isFinishing()) next.run();
            }
        });
        animator.start();
    }

    @Override protected void onDestroy() {
        handler.removeCallbacks(revealTask);
        if (phaseAnimator != null) phaseAnimator.cancel();
        if (rewardCard != null) rewardCard.animate().cancel();
        super.onDestroy();
    }

    private boolean reduceMotion() {
        try {
            return Settings.Global.getFloat(getContentResolver(), Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f;
        } catch (Exception ignored) { return false; }
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + .5f);
    }

    private LinearLayout column() {
        LinearLayout view = new LinearLayout(this);
        view.setOrientation(LinearLayout.VERTICAL);
        return view;
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

        void setShake(float value) {
            float pulse = (float) Math.sin(value * Math.PI * 18);
            setTranslationX(dp(11) * value * pulse);
            setRotation(5f * value * pulse);
        }

        void setCrack(float value) { crack = value; invalidate(); }

        @Override protected void onDraw(Canvas canvas) {
            float cx = getWidth() / 2f, cy = getHeight() / 2f;
            float radius = Math.min(getWidth(), getHeight()) * .39f;
            float split = Math.max(0, (crack - .65f) / .35f) * radius * .32f;
            canvas.save();
            canvas.clipRect(0, 0, cx, getHeight());
            canvas.translate(-split, -split * .12f);
            drawBall(canvas, cx, cy, radius);
            canvas.restore();
            canvas.save();
            canvas.clipRect(cx, 0, getWidth(), getHeight());
            canvas.translate(split, split * .12f);
            drawBall(canvas, cx, cy, radius);
            canvas.restore();

            if (crack > 0f && crack < .98f) {
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(dp(3));
                paint.setColor(0xFFFFE4B2);
                Path path = new Path();
                path.moveTo(cx - radius * .12f, cy - radius * .9f);
                path.lineTo(cx + radius * .15f, cy - radius * .45f);
                path.lineTo(cx - radius * .18f, cy - radius * .12f);
                path.lineTo(cx + radius * .2f, cy + radius * .18f);
                path.lineTo(cx - radius * .1f, cy + radius * .53f);
                path.lineTo(cx + radius * .12f, cy + radius * .9f);
                canvas.save();
                canvas.clipRect(cx - radius, cy - radius, cx + radius,
                    cy - radius + 2 * radius * crack);
                canvas.drawPath(path, paint);
                canvas.restore();
            }
        }

        private void drawBall(Canvas canvas, float cx, float cy, float radius) {
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
        }
    }

    private final class ReelView extends View {
        private static final int TARGET = 42;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final LootTier[] strip = new LootTier[50];
        private float progress;

        ReelView() { super(LootboxActivity.this); }

        void prepare(LootTier result) {
            // Filler uses the same odds; the result was fixed before the animation.
            for (int i = 0; i < strip.length; i++) strip[i] = roller.roll();
            strip[TARGET] = result;
            invalidate();
        }

        void setProgress(float value) { progress = value; invalidate(); }

        @Override protected void onDraw(Canvas canvas) {
            int save = canvas.save();
            canvas.clipRect(0, 0, getWidth(), getHeight());
            float size = Math.min(dp(72), getHeight() - dp(28));
            float step = size + dp(12), center = getWidth() / 2f;
            float top = (getHeight() - size) / 2f;
            float offset = TARGET * step * progress;
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(0xFF262B35);
            canvas.drawRoundRect(0, 0, getWidth(), getHeight(), dp(14), dp(14), paint);
            for (int i = 0; i < strip.length; i++) {
                float x = center + i * step - offset;
                if (x + size / 2 < 0 || x - size / 2 > getWidth() || strip[i] == null) continue;
                paint.setColor(strip[i].color);
                canvas.drawRoundRect(x - size / 2, top, x + size / 2,
                    top + size, dp(8), dp(8), paint);
            }
            paint.setColor(ACCENT);
            canvas.drawRect(center - dp(2), 0, center + dp(2), Math.max(dp(10), top - dp(3)), paint);
            canvas.drawRect(center - dp(2), top + size + dp(3), center + dp(2), getHeight(), paint);
            canvas.restoreToCount(save);
        }
    }
}
