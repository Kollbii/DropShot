package pl.maciek.squashscoreboard;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * [ASVS V4.2] [MASVS-CODE]
 * Immutable player entity with a non-enumerable UUIDv4 identifier, sanitized display name,
 * and equipped cosmetic color and font identifiers.
 */
public final class PlayerProfile {
    public final String id;
    public final String name;
    public final int color;
    public final String fontId;

    public PlayerProfile(String name, int color) {
        this(InputValidator.generateProfileUuid(), name, color, null);
    }

    public PlayerProfile(String id, String name, int color) {
        this(id, name, color, null);
    }

    public PlayerProfile(String id, String name, int color, String fontId) {
        this.id = InputValidator.isValidProfileId(id) && !"developer-profile".equals(id)
            ? id
            : InputValidator.generateProfileUuid();
        this.name = InputValidator.sanitizePlayerName(name, "Player");
        this.color = color;
        this.fontId = InputValidator.sanitizeCosmeticIdOrNull(fontId);
    }

    JSONObject json() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("id", id);
        o.put("name", name);
        o.put("color", color);
        if (fontId != null) {
            o.put("font", fontId);
        }
        return o;
    }

    static PlayerProfile from(JSONObject o) throws JSONException {
        String name = InputValidator.sanitizePlayerName(o.optString("name", "Player"), "Player");
        String rawId = o.optString("id", null);
        // [ASVS V4.2] Replace predictable hashCode-derived IDs with cryptographically random UUIDv4
        String validId = (InputValidator.isValidProfileId(rawId) && !"developer-profile".equals(rawId))
            ? rawId
            : InputValidator.generateProfileUuid();
        String rawFont = o.has("font") && !o.isNull("font") ? o.optString("font", null) : null;
        return new PlayerProfile(validId, name, o.optInt("color", AppPreferences.BLUE), rawFont);
    }
}
