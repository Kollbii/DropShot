package pl.maciek.squashscoreboard;

import org.json.*;
import java.util.*;

public final class MatchRecord {
    public final long id, finishedAt, durationMs;
    public final String playerOneName, playerTwoName, playerOneProfileId, playerTwoProfileId;
    public final int playerOneSets, playerTwoSets, playerOnePoints, playerTwoPoints, pointsToWinSet, setsToWinMatch, playerOneColor, playerTwoColor;
    public final boolean finished, paused;
    public final ArrayList<Integer> pointWinners;
    /** Elapsed active match time at every scored point; old records can have an empty list. */
    public final ArrayList<Long> pointTimesMs;
    public final ArrayList<Long> setDurationsMs;

    public MatchRecord(long id,String p1,String p2,String p1Id,String p2Id,int s1,int s2,int p1Points,int p2Points,int points,int sets,boolean finished,long timestamp,List<Integer>winners,long duration,List<Long>setDurations,List<Long>pointTimes,boolean paused,int color1,int color2) {
        this.id=id;playerOneName=p1;playerTwoName=p2;playerOneProfileId=p1Id;playerTwoProfileId=p2Id;playerOneSets=s1;playerTwoSets=s2;this.playerOnePoints=p1Points;this.playerTwoPoints=p2Points;pointsToWinSet=points;setsToWinMatch=sets;this.finished=finished;finishedAt=timestamp;pointWinners=new ArrayList<>(winners);durationMs=Math.max(0,duration);setDurationsMs=new ArrayList<>(setDurations);pointTimesMs=new ArrayList<>(pointTimes);this.paused=paused;
        playerOneColor=color1;playerTwoColor=color2;
    }
    public MatchRecord(long id,String p1,String p2,String p1Id,String p2Id,int s1,int s2,int p1Points,int p2Points,int points,int sets,boolean finished,long timestamp,List<Integer>winners,long duration,List<Long>setDurations,List<Long>pointTimes,boolean paused) { this(id,p1,p2,p1Id,p2Id,s1,s2,p1Points,p2Points,points,sets,finished,timestamp,winners,duration,setDurations,pointTimes,paused,AppPreferences.BLUE,AppPreferences.RED); }
    public MatchRecord(long id,String p1,String p2,String p1Id,String p2Id,int s1,int s2,int p1Points,int p2Points,int points,int sets,boolean finished,long timestamp,List<Integer>winners,long duration,List<Long>setDurations,boolean paused) { this(id,p1,p2,p1Id,p2Id,s1,s2,p1Points,p2Points,points,sets,finished,timestamp,winners,duration,setDurations,Collections.<Long>emptyList(),paused); }
    /** Kept for source compatibility with old callers. */
    public MatchRecord(long id,String p1,String p2,int s1,int s2,int p1Points,int p2Points,int points,int sets,boolean finished,long timestamp,List<Integer>winners) { this(id,p1,p2,null,null,s1,s2,p1Points,p2Points,points,sets,finished,timestamp,winners,0,Collections.<Long>emptyList(),false); }
    /** Reconstruct completed sets so older early-ended records do not count invented sets. */
    public int[] playedSets() {
        if(pointWinners.isEmpty()) return new int[]{playerOneSets,playerTwoSets};
        int[] sets={0,0};int a=0,b=0;
        for(int winner:pointWinners) {
            if(winner==1)a++;else if(winner==2)b++;
            if(Math.max(a,b)>=pointsToWinSet&&Math.abs(a-b)>=2) { sets[a>b?0:1]++;a=0;b=0; }
        }
        return sets;
    }
    public int statisticalWinner() {
        int[] sets=playedSets();
        if(sets[0]!=sets[1]) return sets[0]>sets[1]?1:2;
        if(playerOnePoints!=playerTwoPoints) return playerOnePoints>playerTwoPoints?1:2;
        return 0;
    }
    public static final class PointScore {
        public final int setNumber,setsOne,setsTwo,pointsOne,pointsTwo;
        private PointScore(int number,int s1,int s2,int p1,int p2) {
            setNumber=number;setsOne=s1;setsTwo=s2;pointsOne=p1;pointsTwo=p2;
        }
    }
    /** Score immediately after the indexed point, including a just-completed set. */
    public PointScore scoreAtPoint(int index) {
        int s1=0,s2=0,p1=0,p2=0,setNumber=1;
        boolean completed=false;
        for(int i=0;i<=index&&i<pointWinners.size();i++) {
            if(completed){p1=0;p2=0;setNumber++;completed=false;}
            if(pointWinners.get(i)==1)p1++;else p2++;
            if(Math.max(p1,p2)>=pointsToWinSet&&Math.abs(p1-p2)>=2) {
                if(p1>p2)s1++;else s2++;
                completed=true;
            }
        }
        return new PointScore(setNumber,s1,s2,p1,p2);
    }
    public JSONObject toJson() throws JSONException { JSONObject j=new JSONObject(); j.put("id",id);j.put("p1",playerOneName);j.put("p2",playerTwoName);if(playerOneProfileId!=null)j.put("p1profile",playerOneProfileId);if(playerTwoProfileId!=null)j.put("p2profile",playerTwoProfileId);j.put("color1",playerOneColor);j.put("color2",playerTwoColor);j.put("s1",playerOneSets);j.put("s2",playerTwoSets);j.put("time",finishedAt);j.put("p1points",playerOnePoints);j.put("p2points",playerTwoPoints);j.put("pointsPerSet",pointsToWinSet);j.put("setsPerMatch",setsToWinMatch);j.put("finished",finished);j.put("duration",durationMs);j.put("paused",paused);JSONArray a=new JSONArray();for(int p:pointWinners)a.put(p);j.put("points",a);JSONArray d=new JSONArray();for(long v:setDurationsMs)d.put(v);j.put("setDurations",d);JSONArray pt=new JSONArray();for(long v:pointTimesMs)pt.put(v);j.put("pointTimes",pt);return j; }
    public static MatchRecord fromJson(JSONObject j) throws JSONException { ArrayList<Integer>w=new ArrayList<>();JSONArray a=j.optJSONArray("points");if(a!=null)for(int i=0;i<a.length();i++)w.add(a.optInt(i));ArrayList<Long>d=new ArrayList<>();JSONArray sd=j.optJSONArray("setDurations");if(sd!=null)for(int i=0;i<sd.length();i++)d.add(Math.max(0,sd.optLong(i)));ArrayList<Long>pt=new ArrayList<>();JSONArray times=j.optJSONArray("pointTimes");if(times!=null)for(int i=0;i<times.length();i++)pt.add(Math.max(0,times.optLong(i)));long time=j.optLong("time",System.currentTimeMillis());return new MatchRecord(j.optLong("id",time),j.optString("p1","GRACZ 1"),j.optString("p2","GRACZ 2"),j.optString("p1profile",null),j.optString("p2profile",null),j.optInt("s1"),j.optInt("s2"),j.optInt("p1points"),j.optInt("p2points"),j.optInt("pointsPerSet",11),j.optInt("setsPerMatch",3),j.optBoolean("finished"),time,w,j.optLong("duration",0),d,pt,j.optBoolean("paused",false),j.optInt("color1",AppPreferences.BLUE),j.optInt("color2",AppPreferences.RED)); }
}
