package pl.maciek.squashscoreboard;

import android.content.Context;
import org.json.*;
import java.util.*;
import pl.maciek.squashscoreboard.lootbox.RewardInventory;

public final class ProfileStore {
    private ProfileStore() { }
    public static List<PlayerProfile> load(Context c) {
        ArrayList<PlayerProfile> profiles=new ArrayList<>(); boolean migrate=false;
        try { JSONArray a=new JSONArray(c.getSharedPreferences("profiles",0).getString("items","[]"));
            for(int i=0;i<a.length();i++) { JSONObject o=a.getJSONObject(i); if(!o.has("id")) migrate=true; profiles.add(PlayerProfile.from(o)); }
        } catch(Exception ignored) { }
        String developerId = RewardInventory.developerId(c);
        if (developerId == null) {
            PlayerProfile existing = null;
            for (PlayerProfile p : profiles) if (p.name.equalsIgnoreCase("deweloper")) { existing = p; break; }
            if (existing == null) {
                existing = new PlayerProfile("developer-profile", "deweloper", AppPreferences.BLUE);
                profiles.add(existing);
                migrate = true;
            }
            RewardInventory.setDeveloper(c, existing.id);
        }
        if(migrate) save(c, profiles);
        List<String> ids = new ArrayList<>();
        for (PlayerProfile p : profiles) ids.add(p.id);
        RewardInventory.migrateLegacy(c, ids);
        return profiles;
    }
    public static void save(Context c,List<PlayerProfile> profiles) {
        JSONArray a=new JSONArray(); try { for(PlayerProfile p:profiles)a.put(p.json()); } catch(Exception e) { return; }
        c.getSharedPreferences("profiles",0).edit().putString("items",a.toString()).apply();
    }
    public static void add(Context c,PlayerProfile p) { List<PlayerProfile>x=load(c); x.add(p); save(c,x); }
    public static PlayerProfile find(Context c,String id) { if(id==null) return null; for(PlayerProfile p:load(c))if(p.id.equals(id))return p; return null; }
    public static PlayerProfile findByName(Context c,String name) { for(PlayerProfile p:load(c))if(p.name.equalsIgnoreCase(name))return p; return null; }
    public static void update(Context c, PlayerProfile profile) { List<PlayerProfile>x=load(c); for(int i=0;i<x.size();i++)if(x.get(i).id.equals(profile.id)){x.set(i,profile);break;} save(c,x); }
    public static void remove(Context c,String id) {
        if (RewardInventory.isDeveloper(c, id)) return;
        List<PlayerProfile>x=load(c); for(int i=x.size()-1;i>=0;i--)if(x.get(i).id.equals(id))x.remove(i);
        save(c,x);
        RewardInventory.forgetProfile(c,id);
    }
}
