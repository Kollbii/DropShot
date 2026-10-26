package pl.maciek.squashscoreboard;

import android.content.Context;

public final class AppPreferences {
    private static final String NAME = "settings";
    public static final int BLUE = 0xff3184ff, RED = 0xfff45c5c, GREEN = 0xff39b87f, GOLD = 0xffffc857;
    private AppPreferences() { }

    public static boolean dark(Context c) { return c.getSharedPreferences(NAME, 0).getBoolean("dark", true); }
    public static boolean highContrast(Context c) { return c.getSharedPreferences(NAME, 0).getBoolean("highContrast", false); }
    public static int color(Context c, String key, int fallback) { return c.getSharedPreferences(NAME, 0).getInt(key, fallback); }
    public static int remote(Context c, String key, int fallback) { return c.getSharedPreferences(NAME, 0).getInt(key, fallback); }
    public static int pointsPerSet(Context c) { return c.getSharedPreferences(NAME, 0).getInt("pointsPerSet", 11); }
    public static int setsPerMatch(Context c) { return c.getSharedPreferences(NAME, 0).getInt("setsPerMatch", 3); }

    public static void save(Context c, boolean dark, int one, int two, int remoteOne, int remoteTwo) {
        c.getSharedPreferences(NAME, 0).edit()
            .putBoolean("dark", dark)
            .putInt("color1", one)
            .putInt("color2", two)
            .putInt("remote1", remoteOne)
            .putInt("remote2", remoteTwo)
            .apply();
    }

    public static void saveAppearance(Context c, boolean dark, boolean highContrast) {
        c.getSharedPreferences(NAME, 0).edit()
            .putBoolean("dark", dark)
            .putBoolean("highContrast", highContrast)
            .apply();
    }

    public static void saveRules(Context c, int pointsPerSet, int setsPerMatch) {
        c.getSharedPreferences(NAME, 0).edit()
            .putInt("pointsPerSet", pointsPerSet)
            .putInt("setsPerMatch", setsPerMatch)
            .apply();
    }

    public static void saveExtra(Context c, int undo, int reset) {
        c.getSharedPreferences(NAME, 0).edit()
            .putInt("undo", undo)
            .putInt("reset", reset)
            .apply();
    }

    public static int courtDuration(Context c) { return c.getSharedPreferences(NAME, 0).getInt("courtDuration", 60); }
    public static int courtGreen(Context c) { return c.getSharedPreferences(NAME, 0).getInt("courtGreen", 30); }
    public static int courtYellow(Context c) { return c.getSharedPreferences(NAME, 0).getInt("courtYellow", 15); }
    public static int courtRed(Context c) { return c.getSharedPreferences(NAME, 0).getInt("courtRed", 5); }
    public static long courtStart(Context c) { return c.getSharedPreferences(NAME, 0).getLong("courtStart", 0); }
    public static boolean courtVibration(Context c) { return c.getSharedPreferences(NAME, 0).getBoolean("courtVibration", false); }
    public static void saveCourtVibration(Context c, boolean enabled) { c.getSharedPreferences(NAME, 0).edit().putBoolean("courtVibration", enabled).apply(); }
    public static int courtVibratedLevel(Context c) { return c.getSharedPreferences(NAME, 0).getInt("courtVibratedLevel", 0); }
    public static void saveCourtVibratedLevel(Context c, int level) { c.getSharedPreferences(NAME, 0).edit().putInt("courtVibratedLevel", level).apply(); }

    public static void saveCourtSettings(Context c, int duration, int green, int yellow, int red) {
        c.getSharedPreferences(NAME, 0).edit()
            .putInt("courtDuration", duration)
            .putInt("courtGreen", green)
            .putInt("courtYellow", yellow)
            .putInt("courtRed", red)
            .apply();
    }

    public static void saveCourtStart(Context c, long start) {
        c.getSharedPreferences(NAME, 0).edit()
            .putLong("courtStart", start)
            .putInt("courtVibratedLevel", 0)
            .apply();
    }
}
