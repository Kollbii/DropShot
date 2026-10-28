package pl.maciek.squashscoreboard;

import org.json.*;
import java.util.UUID;

/** A saved player has a stable id so renaming it never loses its statistics. */
public final class PlayerProfile {
    public final String id, name, fontId, frameId;
    public final int color;
    public PlayerProfile(String name, int color) { this(UUID.randomUUID().toString(), name, color); }
    public PlayerProfile(String id, String name, int color) { this(id, name, color, null); }
    public PlayerProfile(String id, String name, int color, String fontId) {
        this(id, name, color, fontId, null);
    }
    public PlayerProfile(String id, String name, int color, String fontId, String frameId) {
        this.id=id; this.name=name; this.color=color; this.fontId=fontId; this.frameId=frameId;
    }
    JSONObject json() throws JSONException {
        JSONObject o=new JSONObject(); o.put("id",id); o.put("name",name); o.put("color",color);
        if (fontId != null) o.put("font",fontId);
        if (frameId != null) o.put("frame",frameId);
        return o;
    }
    static PlayerProfile from(JSONObject o) throws JSONException {
        String name=o.getString("name");
        return new PlayerProfile(o.optString("id", "legacy-"+Integer.toHexString(name.toLowerCase().hashCode())),
            name, o.getInt("color"), o.optString("font", null), o.optString("frame", null));
    }
}
