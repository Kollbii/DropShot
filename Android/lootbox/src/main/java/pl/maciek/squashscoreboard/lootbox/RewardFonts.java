package pl.maciek.squashscoreboard.lootbox;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.widget.TextView;
import java.util.HashMap;
import java.util.Map;

/** Loads the bundled reward typefaces once per process. */
public final class RewardFonts {
    private static final Map<String, Typeface> CACHE = new HashMap<>();
    private static final Map<String, Float> SCALE_CACHE = new HashMap<>();
    private static final String SAMPLE = "xemn";
    private RewardFonts() {}

    public static Typeface forId(Context context, String id) {
        RewardCatalog.Reward reward = RewardCatalog.byId(id);
        return forReward(context, reward);
    }

    public static synchronized Typeface forReward(Context context, RewardCatalog.Reward reward) {
        if (reward == null || reward.kind != RewardCatalog.Kind.FONT || reward.fontFile == null)
            return Typeface.DEFAULT_BOLD;
        Typeface cached = CACHE.get(reward.id);
        if (cached != null) return cached;
        try {
            Typeface loaded = Typeface.createFromAsset(context.getAssets(), "fonts/" + reward.fontFile);
            CACHE.put(reward.id, loaded);
            return loaded;
        } catch (RuntimeException missingAsset) {
            return Typeface.DEFAULT_BOLD;
        }
    }

    /** Balances visible lowercase height and width against the built-in bold face. */
    public static synchronized float sizeScale(Context context, RewardCatalog.Reward reward) {
        if (reward == null || reward.fontFile == null) return 1f;
        Float cached = SCALE_CACHE.get(reward.id);
        if (cached != null) return cached;
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setTextSize(100f);
        Rect bounds = new Rect();
        paint.setTypeface(Typeface.DEFAULT_BOLD);
        paint.getTextBounds(SAMPLE, 0, SAMPLE.length(), bounds);
        float referenceHeight = bounds.height();
        float referenceWidth = paint.measureText(SAMPLE);
        paint.setTypeface(forReward(context, reward));
        paint.getTextBounds(SAMPLE, 0, SAMPLE.length(), bounds);
        float heightRatio = referenceHeight / Math.max(1, bounds.height());
        float widthRatio = referenceWidth / Math.max(1f, paint.measureText(SAMPLE));
        float scale = (float) (Math.pow(widthRatio, .65) * Math.pow(heightRatio, .35));
        scale = Math.max(.8f, Math.min(1.7f, scale));
        SCALE_CACHE.put(reward.id, scale);
        return scale;
    }

    public static void apply(TextView view, String id, float baseSizeSp) {
        RewardCatalog.Reward reward = RewardCatalog.byId(id);
        apply(view, reward, baseSizeSp);
    }

    public static void apply(TextView view, RewardCatalog.Reward reward, float baseSizeSp) {
        view.setTypeface(forReward(view.getContext(), reward));
        view.setTextSize(baseSizeSp * sizeScale(view.getContext(), reward));
        view.setIncludeFontPadding(false);
    }
}
