package pl.maciek.squashscoreboard.lootbox;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.List;

/** Persists unlocked cosmetics by stable reward ID, independent of name and tier changes. */
public final class RewardInventory {
    private static final String FILE = "reward_inventory";
    private static final String DEVELOPER_KEY = "developer_profile_id";
    private RewardInventory() {}

    public static boolean owns(Context context, String profileId, RewardCatalog.Reward reward) {
        return isStarter(reward.id) || isDeveloper(context, profileId)
            || (profileId != null && preferences(context).getBoolean(profileId + ":" + reward.id, false));
    }

    public static void unlock(Context context, String profileId, RewardCatalog.Reward reward) {
        if (profileId == null) throw new IllegalArgumentException("A reward requires a player profile");
        preferences(context).edit().putBoolean(profileId + ":" + reward.id, true).apply();
    }

    public static RewardCatalog.Reward[] available(Context context, String profileId) {
        List<RewardCatalog.Reward> result = new ArrayList<>();
        for (RewardCatalog.Reward reward : RewardCatalog.all()) {
            if (owns(context, profileId, reward)) result.add(reward);
        }
        return result.toArray(new RewardCatalog.Reward[0]);
    }

    public static String developerId(Context context) {
        return preferences(context).getString(DEVELOPER_KEY, null);
    }

    public static void setDeveloper(Context context, String profileId) {
        preferences(context).edit().putString(DEVELOPER_KEY, profileId).apply();
    }

    public static boolean isDeveloper(Context context, String profileId) {
        return profileId != null && profileId.equals(developerId(context));
    }

    public static void forgetProfile(Context context, String profileId) {
        SharedPreferences.Editor editor = preferences(context).edit();
        for (RewardCatalog.Reward reward : RewardCatalog.all())
            editor.remove(profileId + ":" + reward.id);
        editor.apply();
    }

    /** Old builds stored unlocks globally; preserve them for every existing player. */
    public static void migrateLegacy(Context context, List<String> profileIds) {
        SharedPreferences store = preferences(context);
        if (store.getBoolean("per_player_migrated", false)) return;
        SharedPreferences.Editor editor = store.edit();
        for (RewardCatalog.Reward reward : RewardCatalog.all()) {
            if (!store.getBoolean(reward.id, false)) continue;
            for (String profileId : profileIds)
                editor.putBoolean(profileId + ":" + reward.id, true);
            editor.remove(reward.id);
        }
        editor.putBoolean("per_player_migrated", true).apply();
    }

    private static boolean isStarter(String id) {
        return "blue".equals(id) || "red".equals(id)
            || "green".equals(id) || "gold".equals(id);
    }

    private static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }
}
