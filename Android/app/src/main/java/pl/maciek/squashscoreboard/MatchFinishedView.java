package pl.maciek.squashscoreboard;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public final class MatchFinishedView extends LinearLayout {
    private Runnable backAction, resetAction, continueAction;
    public MatchFinishedView(Context c, String winner, Runnable reset, Runnable continueMatch) {
        super(c); setOrientation(VERTICAL); setGravity(Gravity.CENTER); setPadding(dp(50),dp(20),dp(50),dp(20)); setBackgroundColor(0xf40d1016);
        TextView end=label("KONIEC MECZU",42,Color.rgb(255,200,74)); TextView won=label("WYGRYWA "+winner,27,Color.WHITE);
        backAction=this::close; resetAction=()->{close();reset.run();}; continueAction=()->{close();continueMatch.run();};
        addView(end,new LayoutParams(-1,dp(66))); addView(won,new LayoutParams(-1,dp(48))); addView(button("POWROT  [POPRZEDNI]",Color.rgb(49,57,70),v->triggerBack()),new LayoutParams(-1,dp(58)));
        LayoutParams p=new LayoutParams(-1,dp(58));p.topMargin=dp(10); addView(button("RESET  [PLAY/PAUSE]",Color.rgb(157,64,64),v->triggerReset()),p);
        LayoutParams q=new LayoutParams(-1,dp(58));q.topMargin=dp(10); addView(button("KONTYNUUJ +3 SETY  [NASTEPNY]",Color.rgb(44,111,88),v->triggerContinue()),q);
    }
    private Button button(String text,int color,OnClickListener l){Button b=new Button(getContext());b.setText(text);b.setTextSize(18);b.setTextColor(Color.WHITE);b.setOnClickListener(l);b.setBackgroundColor(color);return b;}
    private TextView label(String text,int size,int color){TextView v=new TextView(getContext());v.setText(text);v.setTextSize(size);v.setTextColor(color);v.setGravity(Gravity.CENTER);return v;}
    private void close(){if(getParent() instanceof ViewGroup)((ViewGroup)getParent()).removeView(this);} private int dp(int v){return(int)(v*getResources().getDisplayMetrics().density+.5f);}
    public void triggerBack(){backAction.run();} public void triggerReset(){resetAction.run();} public void triggerContinue(){continueAction.run();}
}
