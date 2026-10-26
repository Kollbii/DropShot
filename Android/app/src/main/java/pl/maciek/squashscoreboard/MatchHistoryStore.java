package pl.maciek.squashscoreboard;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import java.util.ArrayList;
import java.util.List;

public final class MatchHistoryStore {
    private static final String PREFS = "match_history";
    private static final String KEY = "records";
    private MatchHistoryStore() { }

    public static List<MatchRecord> load(Context context) {
        ArrayList<MatchRecord> records = new ArrayList<>();
        try {
            String source = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "[]");
            JSONArray values = new JSONArray(source);
            for (int i = 0; i < values.length(); i++) records.add(MatchRecord.fromJson(values.getJSONObject(i)));
        } catch (Exception ignored) { }
        return records;
    }

    public static void add(Context context, MatchRecord record) {
        List<MatchRecord> records = load(context);
        boolean replaced = false;
        for (int i = 0; i < records.size(); i++) {
            if (records.get(i).id == record.id) { records.set(i, record); replaced = true; break; }
        }
        if (!replaced) records.add(0, record);
        while (records.size() > 100) records.remove(records.size() - 1);
        JSONArray values = new JSONArray();
        try { for (MatchRecord value : records) values.put(value.toJson()); }
        catch (Exception ignored) { return; }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, values.toString()).apply();
    }
    public static void remove(Context context, long id) {
        List<MatchRecord> records=load(context); for(int i=records.size()-1;i>=0;i--) if(records.get(i).id==id) records.remove(i);
        JSONArray values=new JSONArray(); try { for(MatchRecord value:records) values.put(value.toJson()); } catch(Exception ignored){return;}
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, values.toString()).apply();
    }
    public static void removePlayer(Context context, String player) { List<MatchRecord> records=load(context); for(int i=records.size()-1;i>=0;i--)if(records.get(i).playerOneName.equals(player)||records.get(i).playerTwoName.equals(player))records.remove(i); JSONArray a=new JSONArray();try{for(MatchRecord r:records)a.put(r.toJson());}catch(Exception e){return;}context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit().putString(KEY,a.toString()).apply(); }
    public static void renameProfile(Context context, String profileId, String oldName, String newName) {
        List<MatchRecord> records=load(context); boolean changed=false;
        for(int i=0;i<records.size();i++) { MatchRecord r=records.get(i);
            boolean one=profileId.equals(r.playerOneProfileId) || (r.playerOneProfileId==null && oldName.equalsIgnoreCase(r.playerOneName));
            boolean two=profileId.equals(r.playerTwoProfileId) || (r.playerTwoProfileId==null && oldName.equalsIgnoreCase(r.playerTwoName));
            if(one||two) { records.set(i,new MatchRecord(r.id,one?newName:r.playerOneName,two?newName:r.playerTwoName,one?profileId:r.playerOneProfileId,two?profileId:r.playerTwoProfileId,r.playerOneSets,r.playerTwoSets,r.playerOnePoints,r.playerTwoPoints,r.pointsToWinSet,r.setsToWinMatch,r.finished,r.finishedAt,r.pointWinners,r.durationMs,r.setDurationsMs,r.pointTimesMs,r.paused,r.playerOneColor,r.playerTwoColor));changed=true; }
        }
        if(!changed)return; JSONArray a=new JSONArray(); try{for(MatchRecord r:records)a.put(r.toJson());}catch(Exception e){return;} context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit().putString(KEY,a.toString()).apply();
    }
}
