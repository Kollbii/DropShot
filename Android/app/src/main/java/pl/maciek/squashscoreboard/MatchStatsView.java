package pl.maciek.squashscoreboard;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.Locale;

/** Side-by-side figures for one recorded match. */
public final class MatchStatsView extends ScrollView {
    private final int muted;

    public MatchStatsView(Context context, MatchRecord record) {
        super(context);
        boolean dark=AppPreferences.dark(context);
        muted=dark?Color.rgb(174,185,201):Color.rgb(80,92,108);
        LinearLayout rows=new LinearLayout(context);
        rows.setOrientation(LinearLayout.VERTICAL);
        rows.setPadding(dp(12),dp(8),dp(12),dp(8));
        addView(rows);

        int[] sets=record.playedSets();
        int[] points=new int[2], streaks=new int[2];
        int previous=0, streak=0;
        for(int winner:record.pointWinners) {
            if(winner<1||winner>2)continue;
            points[winner-1]++;
            streak=winner==previous?streak+1:1;
            streaks[winner-1]=Math.max(streaks[winner-1],streak);
            previous=winner;
        }
        int total=points[0]+points[1];
        int format=Math.max(1,(sets[0]+sets[1]+2)/2)*2-1;
        TextView title=cell("PORÓWNANIE GRACZY  ·  BEST OF "+format,15,muted);
        rows.addView(title,new LinearLayout.LayoutParams(-1,dp(38)));
        row(rows,record.playerOneName,"",record.playerTwoName,record.playerOneColor,record.playerTwoColor);
        row(rows,String.valueOf(sets[0]),"WYGRANE SETY",String.valueOf(sets[1]),record.playerOneColor,record.playerTwoColor);
        row(rows,String.valueOf(points[0]),"ZDOBYTE PUNKTY",String.valueOf(points[1]),record.playerOneColor,record.playerTwoColor);
        row(rows,share(points[0],total),"UDZIAŁ W PUNKTACH",share(points[1],total),record.playerOneColor,record.playerTwoColor);
        row(rows,String.valueOf(streaks[0]),"NAJDŁUŻSZA SERIA",String.valueOf(streaks[1]),record.playerOneColor,record.playerTwoColor);
    }

    private void row(LinearLayout parent,String left,String label,String right,int color1,int color2) {
        LinearLayout row=new LinearLayout(getContext());
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.addView(cell(left,18,color1),new LinearLayout.LayoutParams(0,dp(46),1));
        row.addView(cell(label,12,muted),new LinearLayout.LayoutParams(0,dp(46),1.4f));
        row.addView(cell(right,18,color2),new LinearLayout.LayoutParams(0,dp(46),1));
        parent.addView(row);
    }
    private TextView cell(String value,int size,int color) {
        TextView view=new TextView(getContext());
        view.setText(value);view.setTextSize(size);view.setTextColor(color);
        view.setGravity(Gravity.CENTER);view.setSingleLine(true);
        view.setEllipsize(android.text.TextUtils.TruncateAt.END);
        return view;
    }
    private String share(int points,int total) {
        return total==0?"0%":String.format(Locale.US,"%.1f%%",100f*points/total);
    }
    private int dp(int value) { return (int)(value*getResources().getDisplayMetrics().density+.5f); }
}
