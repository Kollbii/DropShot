package pl.maciek.squashscoreboard.lootbox;

/** Absolute drop weights. Add future tiers here, ordered from rarest to most common. */
public enum LootTier {
    GOLD("Gold", 1, 0xFFFFC857),
    PURPLE("Purple", 4, 0xFFAD79E8),
    GREEN("Green", 25, 0xFF69CB87),
    GRAY("Gray", 70, 0xFFADB2BA);

    public final String label;
    public final int weight;
    public final int color;

    LootTier(String label, int weight, int color) {
        this.label = label;
        this.weight = weight;
        this.color = color;
    }
}
