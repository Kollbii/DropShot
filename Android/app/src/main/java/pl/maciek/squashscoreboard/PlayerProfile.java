package pl.maciek.squashscoreboard;

import org.json.JSONException;
import org.json.JSONObject;

/** A saved player keeps a stable ID and cosmetic choices across renames. */
public final class PlayerProfile {
    public final String id, name, fontId, frameId;
    public final int color;
    public PlayerProfile(String name, int color) {
        this(InputValidator.generateProfileUuid(), name, color);
    }

    public PlayerProfile(String id, String name, int color) {
        this(id, name, color, null, null);
    }

    public PlayerProfile(String id, String name, int color, String fontId) {
        this(id, name, color, fontId, null);
    }

    public PlayerProfile(String id, String name, int color, String fontId, String frameId) {
        this.id = InputValidator.isValidProfileId(id) ? id : InputValidator.generateProfileUuid();
        this.name = InputValidator.sanitizePlayerName(name, "Player");
        this.color = color;
        this.fontId = fontId;
        this.frameId = frameId;
    }

    JSONObject json() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("id", id);
        o.put("name", name);
        o.put("color", color);
        if (fontId != null) o.put("font", fontId);
        if (frameId != null) o.put("frame", frameId);
        return o;
    }

    static PlayerProfile from(JSONObject o) throws JSONException {
        String name = InputValidator.sanitizePlayerName(o.optString("name", "Player"), "Player");
        String rawId = o.optString("id", null);
        String validId = InputValidator.isValidProfileId(rawId)
            ? rawId : InputValidator.generateProfileUuid();
        return new PlayerProfile(validId, name, o.optInt("color", AppPreferences.BLUE),
            o.optString("font", null), o.optString("frame", null));
    }
}
