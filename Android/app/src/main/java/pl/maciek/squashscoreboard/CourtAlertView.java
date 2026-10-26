package pl.maciek.squashscoreboard;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.View;

/** A translucent warning glow around the full score screen. */
public final class CourtAlertView extends View {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private int alertColor;

    public CourtAlertView(Context context) {
        super(context);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        setVisibility(GONE);
    }

    public void setAlertColor(int color) {
        alertColor=color;
        setVisibility(color==0?GONE:VISIBLE);
        invalidate();
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if(alertColor==0)return;
        paint.setStyle(Paint.Style.FILL);
        drawBand(canvas,0,24,55);
        drawBand(canvas,5,12,135);
        drawBand(canvas,9,5,255);
    }

    private void drawBand(Canvas canvas,int insetDp,int thicknessDp,int alpha) {
        float inset=dp(insetDp),inner=inset+dp(thicknessDp);
        float right=getWidth()-inset,bottom=getHeight()-inset;
        paint.setColor(Color.argb(alpha,Color.red(alertColor),Color.green(alertColor),Color.blue(alertColor)));
        canvas.drawRect(inset,inset,right,inner,paint);
        canvas.drawRect(inset,bottom-dp(thicknessDp),right,bottom,paint);
        canvas.drawRect(inset,inner,inner,bottom-dp(thicknessDp),paint);
        canvas.drawRect(right-dp(thicknessDp),inner,right,bottom-dp(thicknessDp),paint);
    }

    @Override public boolean onTouchEvent(MotionEvent event) { return false; }
    private int dp(int value) { return (int)(value*getResources().getDisplayMetrics().density+.5f); }
}
