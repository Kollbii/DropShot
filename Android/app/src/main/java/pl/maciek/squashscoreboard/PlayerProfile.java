package pl.maciek.squashscoreboard;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * [ASVS V4.2] [MASVS-CODE]
 * Immutable player entity with a non-enumerable UUIDv4 identifier and sanitized display name.
 */
public final class PlayerProfile {
    public final String id, name;
    public final int color;

    public PlayerProfile(String name, int color) {
        this(InputValidator.generateProfileUuid(), name, color);
    }

    public PlayerProfile(String id, String name, int color) {
        this.id = InputValidator.isValidProfileId(id) ? id : InputValidator.generateProfileUuid();
        this.name = InputValidator.sanitizePlayerName(name, "Player");
        this.color = color;
    }

    JSONObject json() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("id", id);
        o.put("name", name);
        o.put("color", color);
        return o;
    }

    static PlayerProfile from(JSONObject o) throws JSONException {
        String name = InputValidator.sanitizePlayerName(o.optString("name", "Player"), "Player");
        String rawId = o.optString("id", null);
        // [ASVS V4.2] Replace predictable hashCode-derived IDs with cryptographically random UUIDv4
        String validId = (InputValidator.isValidProfileId(rawId) && !"developer-profile".equals(rawId))
            ? rawId
            : InputValidator.generateProfileUuid();
        return new PlayerProfile(validId, name, o.optInt("color", AppPreferences.BLUE));
    }
}
