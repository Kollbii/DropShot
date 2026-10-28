package pl.maciek.squashscoreboard.lootbox;

import android.content.Context;
import android.graphics.Typeface;
import java.util.HashMap;
import java.util.Map;

/** Loads the bundled reward typefaces once per process. */
public final class RewardFonts {
    private static final Map<String, Typeface> CACHE = new HashMap<>();
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
}
