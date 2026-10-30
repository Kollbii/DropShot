package pl.maciek.squashscoreboard.lootbox;

import android.animation.ValueAnimator;
import android.graphics.drawable.GradientDrawable;
import android.provider.Settings;
import android.view.View;
import android.view.animation.LinearInterpolator;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * [MASVS-CODE] [PERF-120HZ]
 * Single, versioned reward database with lifecycle-safe animation binding and defensive copies.
 * Uses primitive ARGB channel interpolation instead of `ArgbEvaluator` to eliminate per-frame
 * Integer autoboxing (21 boxed Integers/frame removed).
 */
public final class RewardCatalog {
    public enum Kind { COLOR, FONT, FRAME }
    private static final int ANIMATOR_TAG_KEY = 0x7f0b00f1;
    public static final class Reward {
        public final String id, name;
        public final LootTier tier;
        public final Kind kind;
        public final int[] colors;
        public final boolean animated;
        public final String fontFile;

        private Reward(String id, String name, LootTier tier, boolean animated, int... colors) {
            this.id = id;
            this.name = name;
            this.tier = tier;
            this.kind = Kind.COLOR;
            this.colors = colors.clone();
            this.animated = animated;
            this.fontFile = null;
        }

        private Reward(String id, String name, LootTier tier, String fontFile) {
            this.id = id;
            this.name = name;
            this.tier = tier;
            this.kind = Kind.FONT;
            this.colors = new int[0];
            this.animated = false;
            this.fontFile = fontFile;
        }

        private Reward(String id, String name, LootTier tier) {
            this.id = id;
            this.name = name;
            this.tier = tier;
            this.kind = Kind.FRAME;
            this.colors = new int[0];
            this.animated = false;
            this.fontFile = null;
        }

        /** Persisted player color key; the first gradient stop is deliberately unique. */
        public int key() {
            if (kind != Kind.COLOR) throw new IllegalStateException("Reward has no color key: " + id);
            return colors[0];
        }

        /** Defensive copy of color stops to prevent external array mutation. */
        public int[] colorsCopy() { return colors.clone(); }
    }

    private static Reward solid(String id, String name, LootTier tier, int color) {
        return new Reward(id, name, tier, false, color);
    }

    private static Reward gradient(String id, String name, LootTier tier, int... colors) {
        return new Reward(id, name, tier, false, colors);
    }
    private static Reward font(String id, String name, LootTier tier, String file) {
        return new Reward(id, name, tier, file);
    }
    private static Reward frame(String id, String name, LootTier tier) {
        return new Reward(id, name, tier);
    }

    private static final Reward DEFAULT_FONT = font("font-default", "Default", LootTier.GRAY, null);

    // Keep existing four color keys for profiles, saved matches and preferences.
    private static final Reward[] ENTRIES = {
        solid("blue", "Blue", LootTier.GRAY, 0xFF3184FF),
        solid("red", "Red", LootTier.GRAY, 0xFFF45C5C),
        solid("green", "Green", LootTier.GRAY, 0xFF39B87F),
        solid("gold", "Gold", LootTier.GRAY, 0xFFFFC857),
        solid("coral", "Coral", LootTier.GREEN, 0xFFFF6B6B),
        solid("turquoise", "Turquoise", LootTier.GREEN, 0xFF19B8A6),
        solid("lavender", "Lavender", LootTier.GREEN, 0xFFAA8DE8),
        solid("amber", "Amber", LootTier.GREEN, 0xFFFFB547),
        solid("rose", "Rose", LootTier.GREEN, 0xFFE77FA1),
        solid("navy", "Navy", LootTier.GRAY, 0xFF315A91),
        solid("sky", "Sky", LootTier.GRAY, 0xFF50AEE8),
        solid("mint", "Mint", LootTier.GRAY, 0xFF66CDAA),
        solid("lime", "Lime", LootTier.GRAY, 0xFF9BC94D),
        solid("forest", "Forest", LootTier.GRAY, 0xFF39805A),
        solid("plum", "Plum", LootTier.GRAY, 0xFF8A5A9B),
        solid("violet", "Violet", LootTier.GRAY, 0xFF7359C6),
        solid("peach", "Peach", LootTier.GRAY, 0xFFF2A77E),
        solid("brick", "Brick", LootTier.GRAY, 0xFFB85C55),
        solid("steel", "Steel", LootTier.GRAY, 0xFF6B879A),
        gradient("sunset", "Sunset", LootTier.PURPLE, 0xFFFF765E, 0xFFF9B45B, 0xFFDE5B8D),
        gradient("ocean", "Ocean", LootTier.PURPLE, 0xFF087FA8, 0xFF27C4C1, 0xFF91E2CD),
        gradient("aurora", "Aurora", LootTier.PURPLE, 0xFF704FC1, 0xFF20AFAC, 0xFF76D88B),
        gradient("orchid", "Orchid", LootTier.PURPLE, 0xFF9C55C2, 0xFFDA72B5, 0xFFF4A887),
        gradient("ember", "Ember", LootTier.PURPLE, 0xFFC64648, 0xFFF0924F, 0xFFFFD476),
        new Reward("rainbow", "Living Rainbow", LootTier.GOLD, true,
            0xFFFF5059, 0xFFFFA838, 0xFFF4DE4A, 0xFF57CE87, 0xFF48A9EF, 0xFF9B6AE8, 0xFFFF5059),
        font("font-rajdhani", "Rajdhani", LootTier.GRAY, "rajdhani.ttf"),
        font("font-oxanium", "Oxanium", LootTier.GRAY, "oxanium.ttf"),
        font("font-chakra-petch", "Chakra Petch", LootTier.GRAY, "chakrapetch.ttf"),
        font("font-teko", "Teko", LootTier.GRAY, "teko.ttf"),
        font("font-barlow-condensed", "Barlow Condensed", LootTier.GRAY, "barlowcondensed.ttf"),
        font("font-exo-2", "Exo 2", LootTier.GRAY, "exo2.ttf"),
        font("font-russo-one", "Russo One", LootTier.GRAY, "russoone.ttf"),
        font("font-audiowide", "Audiowide", LootTier.GRAY, "audiowide.ttf"),
        font("font-righteous", "Righteous", LootTier.GRAY, "righteous.ttf"),
        font("font-space-grotesk", "Space Grotesk", LootTier.GRAY, "spacegrotesk.ttf"),
        font("font-cinzel-decorative", "Cinzel Decorative", LootTier.GREEN, "cinzeldecorative.ttf"),
        font("font-pirata-one", "Pirata One", LootTier.GREEN, "pirataone.ttf"),
        font("font-new-rocker", "New Rocker", LootTier.GREEN, "newrocker.ttf"),
        font("font-black-ops-one", "Black Ops One", LootTier.GREEN, "blackopsone.ttf"),
        font("font-eagle-lake", "Eagle Lake", LootTier.GREEN, "eaglelake.ttf"),
        frame("frame-photon-blade", "Photon Blade", LootTier.PURPLE),
        frame("frame-tribal-fang", "Tribal Fang", LootTier.PURPLE),
        frame("frame-barbed-wire", "Barbed Wire", LootTier.GREEN),
        frame("frame-forged-steel", "Forged Steel", LootTier.GREEN),
        frame("frame-neon-circuit", "Neon Circuit", LootTier.PURPLE),
        frame("frame-arcane-runes", "Arcane Runes", LootTier.PURPLE),
        frame("frame-thorn-vine", "Thorn Vine", LootTier.GREEN),
        frame("frame-pixel-glitch", "Pixel Glitch", LootTier.GREEN),
        frame("frame-racing-stripes", "Racing Stripes", LootTier.GREEN),
        frame("frame-royal-filigree", "Royal Filigree", LootTier.PURPLE),
        frame("frame-liquid-chrome", "Liquid Chrome", LootTier.GREEN),
        frame("frame-inferno", "Inferno", LootTier.GREEN),
        frame("frame-frost-crystal", "Frost Crystal", LootTier.GREEN),
        frame("frame-carbon-hex", "Carbon Hex", LootTier.GREEN),
        frame("frame-shattered-glass", "Shattered Glass", LootTier.GREEN)
    };

    private RewardCatalog() {}

    public static Reward[] all() { return ENTRIES.clone(); }

    public static Reward[] colors() { return byKind(Kind.COLOR, false); }

    /** Includes the free built-in font, which cannot be drawn from a lootbox. */
    public static Reward[] fonts() { return byKind(Kind.FONT, true); }

    public static Reward[] frames() { return byKind(Kind.FRAME, false); }

    private static Reward[] byKind(Kind kind, boolean includeDefault) {
        List<Reward> result = new ArrayList<>();
        if (includeDefault) result.add(DEFAULT_FONT);
        for (Reward reward : ENTRIES) if (reward.kind == kind) result.add(reward);
        return result.toArray(new Reward[0]);
    }

    public static Reward byId(String id) {
        if (id == null || DEFAULT_FONT.id.equals(id)) return DEFAULT_FONT;
        for (Reward reward : ENTRIES) if (reward.id.equals(id)) return reward;
        return null;
    }

    public static Reward byKey(int key) {
        for (Reward reward : ENTRIES) if (reward.kind == Kind.COLOR && reward.key() == key) return reward;
        return null;
    }

    public static Reward pick(LootTier tier, Random random) {
        if (tier == null || random == null) throw new IllegalArgumentException("tier and random are required");
        List<Reward> pool = new ArrayList<>();
        for (Reward reward : ENTRIES) if (reward.tier == tier) pool.add(reward);
        if (pool.isEmpty()) throw new IllegalStateException("No rewards in " + tier);
        return pool.get(random.nextInt(pool.size()));
    }

    public static void paint(View view, Reward reward, float radius) {
        if (view == null || reward == null || reward.kind != Kind.COLOR) return;
        GradientDrawable drawable = new GradientDrawable();
        drawable.setOrientation(GradientDrawable.Orientation.TL_BR);
        drawable.setCornerRadius(radius);
        view.setBackground(drawable);
        configure(view, drawable, reward, false);
    }

    public static void configure(View view, GradientDrawable drawable, Reward reward, boolean darken) {
        if (view == null || drawable == null || reward == null || reward.kind != Kind.COLOR) return;
        // Cancel any previously attached animator on this view to prevent infinite animator leaks
        Object existing = view.getTag(ANIMATOR_TAG_KEY);
        if (existing instanceof ValueAnimator) {
            ((ValueAnimator) existing).cancel();
            view.setTag(ANIMATOR_TAG_KEY, null);
        }
        drawable.setOrientation(GradientDrawable.Orientation.TL_BR);
        final int[] base = new int[reward.colors.length];
        for (int i = 0; i < base.length; i++) {
            int color = reward.colors[i];
            base[i] = darken ? blend(color, 0xFF141923, .42f) : color;
        }
        if (base.length == 1) drawable.setColor(base[0]);
        else drawable.setColors(base);

        if (reward.animated && !isReduceMotion(view)) {
            final int[] shifted = new int[base.length];
            ValueAnimator animator = ValueAnimator.ofFloat(0f, 6f);
            animator.setDuration(3200);
            animator.setRepeatCount(ValueAnimator.INFINITE);
            animator.setInterpolator(new LinearInterpolator());
            animator.addUpdateListener(a -> {
                float position = (float) a.getAnimatedValue();
                int shift = (int) position;
                float fraction = position - shift;
                for (int i = 0; i < shifted.length; i++) {
                    shifted[i] = blend(
                        base[(i + shift) % 6],
                        base[(i + shift + 1) % 6],
                        fraction
                    );
                }
                drawable.setColors(shifted);
            });
            view.setTag(ANIMATOR_TAG_KEY, animator);
            view.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
                @Override public void onViewAttachedToWindow(View v) {
                    if (v.getTag(ANIMATOR_TAG_KEY) == animator && !animator.isStarted()) {
                        animator.start();
                    }
                }
                @Override public void onViewDetachedFromWindow(View v) {
                    animator.cancel();
                }
            });
            if (view.isAttachedToWindow()) animator.start();
        }
    }

    private static boolean isReduceMotion(View view) {
        try {
            return Settings.Global.getFloat(
                view.getContext().getContentResolver(),
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            ) == 0f;
        } catch (Exception ignored) {
            return false;
        }
    }

    static int blend(int from, int to, float amount) {
        int a1 = (from >>> 24) & 0xff, r1 = (from >>> 16) & 0xff, g1 = (from >>> 8) & 0xff, b1 = from & 0xff;
        int a2 = (to >>> 24) & 0xff, r2 = (to >>> 16) & 0xff, g2 = (to >>> 8) & 0xff, b2 = to & 0xff;
        int a = Math.round(a1 + (a2 - a1) * amount);
        int r = Math.round(r1 + (r2 - r1) * amount);
        int g = Math.round(g1 + (g2 - g1) * amount);
        int b = Math.round(b1 + (b2 - b1) * amount);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
