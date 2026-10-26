# Deliverable 7: Jetpack Compose Theme Code

This folder contains the complete Material Design 3 Jetpack Compose theme implementation for **DropShot**:

- [`Color.kt`](Color.kt): All tonal palettes (`T0` to `T100`), semantic color tokens (`success`, `warning`, `error`, `info`), and player identity color tokens (`playerBlue`, `playerRed`, `playerGreen`, `playerGold`) for Light, Dark, Light High-Contrast, and Dark High-Contrast themes.
- [`Type.kt`](Type.kt): The Material 3 `DropShotTypography` type scale (minimum `16 sp` body text, tabular numerals `"tnum"` for clocks and scores) plus `ScoreboardTypography` (`scoreDisplayMin`, `scoreDisplayMax`, `gameScore`, `clock`).
- [`Theme.kt`](Theme.kt): `LightColorScheme`, `DarkColorScheme`, `LightHighContrastColorScheme`, `DarkHighContrastColorScheme`, `DropShotShapes`, `DropShotSpacing` (`8 dp` grid + `48 dp` minimum touch target), `DropShotElevation`, `DropShotMotion` (with automatic `Settings.Global.ANIMATOR_DURATION_SCALE == 0f` detection for the "Remove animations" system setting), and `DropShotTheme` supporting system font scaling up to `200%` (`maxFontScale = 2.0f`).

## Gradle Setup (When Migrating `Android/app/build.gradle` to Jetpack Compose)

```groovy
android {
    namespace 'pl.maciek.squashscoreboard'
    compileSdk 35

    defaultConfig {
        applicationId 'pl.maciek.squashscoreboard'
        minSdk 26
        targetSdk 35
    }

    buildFeatures {
        compose true
    }
}

dependencies {
    def composeBom = platform('androidx.compose:compose-bom:2024.10.01')
    implementation composeBom
    implementation 'androidx.compose.ui:ui'
    implementation 'androidx.compose.material3:material3'
    implementation 'androidx.compose.material3:material3-window-size-class'
    implementation 'androidx.activity:activity-compose:1.9.3'
}
```

