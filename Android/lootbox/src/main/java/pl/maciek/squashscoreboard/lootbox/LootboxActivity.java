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
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.Random;

/** Self-contained placeholder lootbox screen; the host app only needs an entry point. */
public final class LootboxActivity extends Activity {
    public static final String EXTRA_PROFILE_ID = "reward_profile_id";
    public static final String EXTRA_PLAYER_NAME = "reward_player_name";
    private static final int BACKGROUND = 0xFF171A22;
    private static final int FOREGROUND = 0xFFF5F2EA;
    private static final int MUTED = 0xFFB5BAC5;
    private static final int ACCENT = 0xFFE6AD54;
    private static final int IDLE = 0, SHAKING = 1, CRACKING = 2, POPPING = 3, SPINNING = 4, WON = 5;

    private final TierRoller roller = new TierRoller();
    private final Random visualRandom = new Random();
    private final RewardCatalog.Reward[] reelStrip = new RewardCatalog.Reward[50];
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable revealTask = this::showReward;
    private final Runnable splitTask = this::popReel;
    private ValueAnimator phaseAnimator;
    private FrameLayout rewardOverlay;
    private LinearLayout rewardCard;
    private BallView ball;
    private ReelView reel;
    private Button openButton;
    private TextView resultLabel;
    private LootTier lastResult;
    private RewardCatalog.Reward lastReward;
    private int phase = IDLE;
    private float shakeProgress, crackProgress, popProgress, reelProgress;
    private float landingFraction;
    private int reelStartIndex;
    private boolean landscape;
    private boolean rewardDismissed;
    private boolean rewardAlreadyOwned;
    private String profileId;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        profileId = getIntent().getStringExtra(EXTRA_PROFILE_ID);
        if (profileId == null) { finish(); return; }
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
            String playerName = getIntent().getStringExtra(EXTRA_PLAYER_NAME);
            TextView subtitle = label("Rewards for " + (playerName == null ? "player" : playerName), 16, MUTED);
            subtitle.setGravity(Gravity.CENTER);
            content.addView(subtitle, new LinearLayout.LayoutParams(-1, dp(32)));
        }

        FrameLayout stage = new FrameLayout(this);
        content.addView(stage, new LinearLayout.LayoutParams(-1, 0, 1));
        ball = new BallView();
        stage.addView(ball, new FrameLayout.LayoutParams(-1, -1, Gravity.CENTER));
        reel = new ReelView();
        FrameLayout.LayoutParams reelParams = new FrameLayout.LayoutParams(-1, -1, Gravity.CENTER);
        reelParams.leftMargin = dp(landscape ? 28 : 8);
        reelParams.rightMargin = dp(landscape ? 28 : 8);
        stage.addView(reel, reelParams);

        resultLabel = label("", landscape ? 17 : 20, FOREGROUND);
        resultLabel.setGravity(Gravity.CENTER);
        resultLabel.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        content.addView(resultLabel, new LinearLayout.LayoutParams(-1, dp(landscape ? 34 : 44)));

        openButton = new Button(this);
        openButton.setText("Open lootbox");
        openButton.setAllCaps(false);
        openButton.setTextSize(landscape ? 16 : 18);
        openButton.setTypeface(null, 1);
        openButton.setTextColor(BACKGROUND);
        openButton.setBackgroundColor(ACCENT);
        openButton.setOnClickListener(v -> open());
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
            dp(landscape ? 250 : 220), dp(48));
        buttonParams.gravity = Gravity.CENTER_HORIZONTAL;
        buttonParams.bottomMargin = dp(landscape ? 4 : 10);
        content.addView(openButton, buttonParams);

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
            reel.prepare(reelStrip, landingFraction, reelStartIndex);
            reel.setProgress(reelProgress);
        }
        applyPop();
        openButton.setEnabled(phase == IDLE || phase == WON);
        openButton.setAlpha(openButton.isEnabled() ? 1f : .55f);
        openButton.setText(phase == WON ? "Open another lootbox" : "Open lootbox");
        if (phase == WON && lastResult != null) {
            resultLabel.setText("You got: " + lastReward.name + " · " + lastResult.label);
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
        handler.removeCallbacks(splitTask);
        if (phaseAnimator != null) phaseAnimator.cancel();
        phase = SHAKING;
        rewardDismissed = false;
        lastResult = roller.roll();
        lastReward = RewardCatalog.pick(lastResult, visualRandom);
        rewardAlreadyOwned = RewardInventory.owns(this, profileId, lastReward);
        for (int i = 0; i < reelStrip.length; i++)
            reelStrip[i] = RewardCatalog.pick(roller.roll(), visualRandom);
        reelStrip[ReelView.TARGET] = lastReward;
        landingFraction = ReelLanding.pickOffsetFraction(visualRandom);
        reelStartIndex = 10 + visualRandom.nextInt(6);
        shakeProgress = crackProgress = popProgress = reelProgress = 0f;
        renderState();
        if (reduceMotion()) {
            crackProgress = popProgress = reelProgress = 1f;
            phase = WON;
            RewardInventory.unlock(this, profileId, lastReward);
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
        }, () -> handler.postDelayed(splitTask, 350));
    }

    private void popReel() {
        phase = POPPING;
        ball.setVisibility(View.INVISIBLE);
        reel.prepare(reelStrip, landingFraction, reelStartIndex);
        applyPop();
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
        reel.setTranslationY(dp(24) * (1f - popProgress));
    }

    private void spinReel() {
        phase = SPINNING;
        popProgress = 1f;
        applyPop();
        animatePhase(6500, new LinearInterpolator(), value -> {
            reelProgress = ReelTiming.progressAt(Math.round(value * 6500));
            reel.setProgress(reelProgress);
        }, () -> handler.postDelayed(revealTask, 500));
    }

    private void showReward() {
        if (phase != SPINNING) return;
        phase = WON;
        rewardDismissed = false;
        RewardInventory.unlock(this, profileId, lastReward);
        reelProgress = 1f;
        resultLabel.setText("You got: " + lastReward.name + " · " + lastResult.label);
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
        View square;
        if (lastReward.kind == RewardCatalog.Kind.FONT) {
            RewardCatalog.Reward fontReward = lastReward;
            square = new View(this) {
                private final Paint tilePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                @Override protected void onDraw(Canvas canvas) {
                    float size = Math.min(getWidth(), getHeight());
                    RewardTilePainter.draw(canvas, (getWidth() - size) / 2f,
                        (getHeight() - size) / 2f, size, fontReward, tilePaint, this);
                }
            };
        } else {
            square = new View(this);
            RewardCatalog.paint(square, lastReward, dp(10));
        }
        LinearLayout.LayoutParams squareParams = new LinearLayout.LayoutParams(dp(landscape ? 82 : 108), dp(landscape ? 82 : 108));
        squareParams.topMargin = dp(10);
        squareParams.bottomMargin = dp(10);
        rewardCard.addView(square, squareParams);
        TextView name = label(lastReward.name.toUpperCase() + " · " + lastResult.label.toUpperCase(),
            landscape ? 18 : 21, lastResult.color);
        name.setTypeface(lastReward.kind == RewardCatalog.Kind.FONT
            ? RewardFonts.forReward(this, lastReward) : android.graphics.Typeface.DEFAULT_BOLD);
        name.setGravity(Gravity.CENTER);
        rewardCard.addView(name, new LinearLayout.LayoutParams(-1, dp(38)));
        TextView status = label(rewardAlreadyOwned ? "Already in your collection"
            : "New player " + (lastReward.kind == RewardCatalog.Kind.FONT ? "font" : "color") + " unlocked",
            14, MUTED);
        status.setGravity(Gravity.CENTER);
        rewardCard.addView(status, new LinearLayout.LayoutParams(-1, dp(26)));
        Button continueButton = new Button(this);
        continueButton.setText("Continue");
        continueButton.setAllCaps(false);
        continueButton.setTextColor(BACKGROUND);
        continueButton.setBackgroundColor(ACCENT);
        continueButton.setOnClickListener(v -> resetToReady());
        LinearLayout.LayoutParams continueParams = new LinearLayout.LayoutParams(-1, dp(44));
        continueParams.topMargin = dp(8);
        rewardCard.addView(continueButton, continueParams);
        rewardOverlay.setContentDescription("You won " + lastReward.name + ", " + lastResult.label + " tier");
    }

    private void resetToReady() {
        rewardCard.animate().cancel();
        rewardOverlay.setVisibility(View.GONE);
        lastResult = null;
        lastReward = null;
        phase = IDLE;
        shakeProgress = crackProgress = popProgress = reelProgress = 0f;
        rewardDismissed = true;
        renderState();
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
        handler.removeCallbacks(splitTask);
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
            float split = Math.max(0, (crack - .6f) / .4f) * radius * .38f;
            Path leftHalf = new Path();
            leftHalf.moveTo(0, 0);
            leftHalf.lineTo(cx, 0);
            addSeam(leftHalf, cx, cy, radius);
            leftHalf.lineTo(cx, getHeight());
            leftHalf.lineTo(0, getHeight());
            leftHalf.close();
            Path rightHalf = new Path();
            rightHalf.moveTo(getWidth(), 0);
            rightHalf.lineTo(cx, 0);
            addSeam(rightHalf, cx, cy, radius);
            rightHalf.lineTo(cx, getHeight());
            rightHalf.lineTo(getWidth(), getHeight());
            rightHalf.close();

            canvas.save();
            canvas.translate(-split, -split * .12f);
            canvas.clipPath(leftHalf);
            drawBall(canvas, cx, cy, radius);
            canvas.restore();
            canvas.save();
            canvas.translate(split, split * .12f);
            canvas.clipPath(rightHalf);
            drawBall(canvas, cx, cy, radius);
            canvas.restore();

            if (crack > 0f && crack < .6f) {
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(dp(3));
                paint.setColor(0xFFFFE4B2);
                Path path = new Path();
                path.moveTo(cx, 0);
                addSeam(path, cx, cy, radius);
                canvas.save();
                canvas.clipRect(cx - radius, cy - radius, cx + radius,
                    cy - radius + 2 * radius * (crack / .6f));
                canvas.drawPath(path, paint);
                canvas.restore();
            }
        }

        private void addSeam(Path path, float cx, float cy, float radius) {
            path.lineTo(cx, cy - radius);
            path.lineTo(cx - radius * .12f, cy - radius * .6f);
            path.lineTo(cx + radius * .09f, cy - radius * .25f);
            path.lineTo(cx - radius * .1f, cy + radius * .12f);
            path.lineTo(cx + radius * .12f, cy + radius * .48f);
            path.lineTo(cx, cy + radius);
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
        private final RewardCatalog.Reward[] strip = new RewardCatalog.Reward[50];
        private float progress;
        private float landingFraction;
        private int startIndex;

        ReelView() { super(LootboxActivity.this); }

        void prepare(RewardCatalog.Reward[] items, float offsetFraction, int firstIndex) {
            System.arraycopy(items, 0, strip, 0, strip.length);
            landingFraction = offsetFraction;
            startIndex = firstIndex;
            invalidate();
        }

        void setProgress(float value) { progress = value; invalidate(); }

        @Override protected void onDraw(Canvas canvas) {
            int save = canvas.save();
            canvas.clipRect(0, 0, getWidth(), getHeight());
            float size = Math.min(dp(72), getHeight() - dp(28));
            float step = size + dp(12), center = getWidth() / 2f;
            float top = (getHeight() - size) / 2f;
            float offset = startIndex * step
                + ((TARGET - startIndex) * step + size * landingFraction) * progress;
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(0xFF262B35);
            canvas.drawRoundRect(0, 0, getWidth(), getHeight(), dp(14), dp(14), paint);
            int visibleEachSide = (int) Math.ceil(getWidth() / (2f * step)) + 2;
            int underMarker = (int) Math.floor(offset / step);
            for (int i = underMarker - visibleEachSide; i <= underMarker + visibleEachSide; i++) {
                float x = center + i * step - offset;
                if (x + size / 2 < 0 || x - size / 2 > getWidth()) continue;
                RewardCatalog.Reward square = strip[Math.floorMod(i, strip.length)];
                if (square == null) continue;
                RewardTilePainter.draw(canvas, x - size / 2, top, size, square, paint, this);
            }
            paint.setColor(ACCENT);
            canvas.drawRect(center - dp(2), 0, center + dp(2), Math.max(dp(10), top - dp(3)), paint);
            canvas.drawRect(center - dp(2), top + size + dp(3), center + dp(2), getHeight(), paint);
            canvas.restoreToCount(save);
        }


    }
}
