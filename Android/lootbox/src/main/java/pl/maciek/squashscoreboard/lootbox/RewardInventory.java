package pl.maciek.squashscoreboard.lootbox;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.List;

/** Persists unlocked cosmetics by stable reward ID, independent of name and tier changes. */
public final class RewardInventory {
    private static final String FILE = "reward_inventory";
    private RewardInventory() {}

    public static boolean owns(Context context, RewardCatalog.Reward reward) {
        return isStarter(reward.id) || preferences(context).getBoolean(reward.id, false);
    }

    public static void unlock(Context context, RewardCatalog.Reward reward) {
        preferences(context).edit().putBoolean(reward.id, true).apply();
    }

    public static RewardCatalog.Reward[] available(Context context) {
        List<RewardCatalog.Reward> result = new ArrayList<>();
        for (RewardCatalog.Reward reward : RewardCatalog.all()) {
            if (owns(context, reward)) result.add(reward);
        }
        return result.toArray(new RewardCatalog.Reward[0]);
    }

    private static boolean isStarter(String id) {
        return "blue".equals(id) || "red".equals(id)
            || "green".equals(id) || "gold".equals(id);
    }

    private static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }
}
