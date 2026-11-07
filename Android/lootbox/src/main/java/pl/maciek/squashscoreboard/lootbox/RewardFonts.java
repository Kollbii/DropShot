package pl.maciek.squashscoreboard.lootbox;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.widget.TextView;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * [PERF-120HZ] Loads bundled reward typefaces and optical size scales once per process.
 * Uses lock-free `ConcurrentHashMap` caches so 120 Hz `onDraw` passes never block on a monitor
 * or invoke JNI `measureText` repeatedly.
 */
public final class RewardFonts {
    private static final Map<String, Typeface> CACHE = new ConcurrentHashMap<>();
    private static final Map<String, Float> SCALE_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, Float> TILE_SIZE_CACHE = new ConcurrentHashMap<>();
    private static final String SAMPLE = "xemn";
    private static final String TILE_SAMPLE = "Aa";

    private static volatile boolean refMeasured;
    private static float referenceHeight = 1f;
    private static float referenceWidth = 1f;

    private RewardFonts() {}

    public static Typeface forId(Context context, String id) {
        return forReward(context, RewardCatalog.fontOrDefault(id));
    }

    public static Typeface forReward(Context context, RewardCatalog.Reward reward) {
        if (context == null || reward == null || reward.kind != RewardCatalog.Kind.FONT || reward.fontFile == null) {
            return Typeface.DEFAULT_BOLD;
        }
        Typeface cached = CACHE.get(reward.id);
        if (cached != null) return cached;
        Context appContext = context.getApplicationContext() != null ? context.getApplicationContext() : context;
        try {
            Typeface loaded = Typeface.createFromAsset(appContext.getAssets(), "fonts/" + reward.fontFile);
            Typeface prior = CACHE.putIfAbsent(reward.id, loaded);
            return prior != null ? prior : loaded;
        } catch (RuntimeException missingAsset) {
            return Typeface.DEFAULT_BOLD;
        }
    }

    /** Balances visible lowercase height and width against the built-in bold typeface. */
    public static float sizeScale(Context context, RewardCatalog.Reward reward) {
        if (context == null || reward == null || reward.fontFile == null) return 1f;
        Float cached = SCALE_CACHE.get(reward.id);
        if (cached != null) return cached;

        ensureReferenceMetrics();
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setTextSize(100f);
        paint.setTypeface(forReward(context, reward));
        Rect bounds = new Rect();
        paint.getTextBounds(SAMPLE, 0, SAMPLE.length(), bounds);
        float heightRatio = referenceHeight / Math.max(1, bounds.height());
        float widthRatio = referenceWidth / Math.max(1f, paint.measureText(SAMPLE));
        float scale = (float) (Math.pow(widthRatio, .65) * Math.pow(heightRatio, .35));
        scale = Math.max(.8f, Math.min(1.7f, scale));
        SCALE_CACHE.putIfAbsent(reward.id, scale);
        return scale;
    }

    /** Precomputed text size for the "Aa" sample inside a 72x72 tile (zero JNI measureText per frame). */
    static float tileGlyphSize(Context context, RewardCatalog.Reward reward) {
        if (context == null || reward == null) return 38f;
        Float cached = TILE_SIZE_CACHE.get(reward.id);
        if (cached != null) return cached;

        float targetSize = 38f * sizeScale(context, reward);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setTypeface(forReward(context, reward));
        paint.setTextSize(targetSize);
        float measuredWidth = paint.measureText(TILE_SAMPLE);
        if (measuredWidth > 56f) {
            targetSize = targetSize * 56f / measuredWidth;
        }
        TILE_SIZE_CACHE.putIfAbsent(reward.id, targetSize);
        return targetSize;
    }

    private static void ensureReferenceMetrics() {
        if (refMeasured) return;
        synchronized (RewardFonts.class) {
            if (refMeasured) return;
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setTextSize(100f);
            paint.setTypeface(Typeface.DEFAULT_BOLD);
            Rect bounds = new Rect();
            paint.getTextBounds(SAMPLE, 0, SAMPLE.length(), bounds);
            referenceHeight = Math.max(1f, bounds.height());
            referenceWidth = Math.max(1f, paint.measureText(SAMPLE));
            refMeasured = true;
        }
    }

    public static void apply(TextView view, String id, float baseSizeSp) {
        if (view == null) return;
        apply(view, RewardCatalog.fontOrDefault(id), baseSizeSp);
    }

    public static void apply(TextView view, RewardCatalog.Reward reward, float baseSizeSp) {
        if (view == null) return;
        view.setTypeface(forReward(view.getContext(), reward));
        view.setTextSize(baseSizeSp * sizeScale(view.getContext(), reward));
        view.setIncludeFontPadding(false);
    }
}
