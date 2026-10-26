package pl.maciek.squashscoreboard;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.Date;
import java.util.List;

public class HistoryActivity extends Activity {
    private int background, surface, foreground, muted;
    @Override public void onCreate(Bundle state) { super.onCreate(state); showRecords(); }
    @Override protected void onResume() { super.onResume(); showRecords(); }

    private void showRecords() {
        boolean dark=AppPreferences.dark(this);background=dark?Color.rgb(13,16,22):Color.rgb(245,247,250);surface=dark?Color.rgb(28,34,44):Color.WHITE;foreground=dark?Color.WHITE:Color.rgb(24,30,40);muted=dark?Color.rgb(174,185,201):Color.rgb(80,92,108);LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(18), dp(14), dp(18), dp(14)); root.setBackgroundColor(background);
        LinearLayout header = new LinearLayout(this); header.setGravity(Gravity.CENTER_VERTICAL);
        TextView back = text("\u2039", 40, foreground); back.setGravity(Gravity.CENTER); back.setOnClickListener(v -> finish());
        TextView title = text("HISTORIA MECZOW", 24, foreground); title.setGravity(Gravity.CENTER_VERTICAL);
        TextView stats=text("STATYSTYKI",14,Color.rgb(49,132,255)); stats.setGravity(Gravity.CENTER); stats.setOnClickListener(v->startActivity(new Intent(this,PlayerStatsActivity.class)));
        header.addView(back, new LinearLayout.LayoutParams(dp(48), -1)); header.addView(title, new LinearLayout.LayoutParams(0, -1, 1)); header.addView(stats,new LinearLayout.LayoutParams(dp(100),-1));
        root.addView(header, new LinearLayout.LayoutParams(-1, dp(54)));
        LinearLayout list = new LinearLayout(this); list.setOrientation(LinearLayout.VERTICAL);
        List<MatchRecord> records = MatchHistoryStore.load(this);
        if (records.isEmpty()) { TextView empty = text("Brak zapisanych meczow. Mecz zostanie dodany tutaj po zakonczeniu lub rozpoczeciu nowego.", 18, muted); empty.setGravity(Gravity.CENTER); list.addView(empty, new LinearLayout.LayoutParams(-1, 0, 1)); }
        else for (MatchRecord record : records) list.addView(recordRow(record), rowParams());
        ScrollView scroll=new ScrollView(this); scroll.addView(list); root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1)); setContentView(root);
    }

    private View recordRow(MatchRecord record) {
        LinearLayout row = new LinearLayout(this); row.setOrientation(LinearLayout.VERTICAL); row.setPadding(dp(18), dp(10), dp(18), dp(10));
        GradientDrawable bg = new GradientDrawable(); bg.setColor(surface); bg.setCornerRadius(dp(14)); row.setBackground(bg);
        TextView names = text(record.playerOneName + "  vs  " + record.playerTwoName, 20, foreground); names.setSingleLine(true);
        String date = DateFormat.format("dd.MM.yyyy  HH:mm", new Date(record.finishedAt)).toString();
        String state = record.finished ? "ZAKONCZONY" : "DO KONTYNUOWANIA";
        String duration = record.durationMs > 0 ? "  |  " + formatDuration(record.durationMs) : "";
        TextView score = text(record.playerOneSets + " : " + record.playerTwoSets + "  |  " + state + duration + "  |  " + date, 15, muted);
        TextView remove=text("USUN",16,Color.WHITE); remove.setGravity(Gravity.CENTER); remove.setBackgroundColor(Color.rgb(180,65,65)); remove.setOnClickListener(v->new android.app.AlertDialog.Builder(this).setTitle("Usunac mecz?").setMessage(record.playerOneName+" vs "+record.playerTwoName).setNegativeButton("ANULUJ",null).setPositiveButton("USUN",(d,w)->{MatchHistoryStore.remove(this,record.id);showRecords();}).show());
        row.addView(names, new LinearLayout.LayoutParams(-1, 0, 1)); LinearLayout bottom=new LinearLayout(this); bottom.addView(score,new LinearLayout.LayoutParams(0,-1,1)); bottom.addView(remove,new LinearLayout.LayoutParams(dp(70),-1)); row.addView(bottom, new LinearLayout.LayoutParams(-1, 0, 1));
        row.setOnClickListener(v -> startActivity(new Intent(this, MatchDetailActivity.class).putExtra(MatchDetailActivity.EXTRA_RECORD, toJson(record))));
        return row;
    }
    private LinearLayout.LayoutParams rowParams() { LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, dp(74)); p.setMargins(0, dp(5), 0, dp(5)); return p; }
    private String toJson(MatchRecord record) { try { return record.toJson().toString(); } catch (Exception e) { return ""; } }
    private TextView text(String value, int size, int color) { TextView view = new TextView(this); view.setText(value); view.setTextSize(size); view.setTextColor(color); return view; }
    private String formatDuration(long ms) { long s=ms/1000; return String.format(java.util.Locale.US, "%02d:%02d:%02d", s/3600, (s/60)%60, s%60); }
    private int dp(int value) { return (int) (value * getResources().getDisplayMetrics().density + .5f); }
}
