# [MASVS-RESILIENCE] [MASVS-CODE] R8 / ProGuard configuration
# Strip debug/verbose/info logging in release builds
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
}

# Repackage and obfuscate internal classes while keeping Android entry points
-repackageclasses 'pl.maciek.squashscoreboard.obf'
-allowaccessmodification

-keep public class * extends android.app.Activity
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

