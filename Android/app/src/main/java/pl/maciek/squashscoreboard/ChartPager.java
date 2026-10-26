package pl.maciek.squashscoreboard;

import android.content.Context;
import android.view.MotionEvent;
import android.widget.HorizontalScrollView;

/** Snaps one chart page in the direction of a swipe, including a fast swipe. */
public final class ChartPager extends HorizontalScrollView {
    private int pageCount=1,startingPage;
    private float downX;
    private long downTime;

    public ChartPager(Context context) { super(context); }
    public void setPageCount(int count) { pageCount=Math.max(1,count); }

    @Override public boolean onInterceptTouchEvent(MotionEvent event) {
        if(event.getActionMasked()==MotionEvent.ACTION_DOWN)rememberDown(event);
        return super.onInterceptTouchEvent(event);
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        int action=event.getActionMasked();
        if(action==MotionEvent.ACTION_DOWN)rememberDown(event);
        boolean handled=super.onTouchEvent(event);
        if(action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_CANCEL) {
            int width=Math.max(1,getWidth());
            float distance=downX-event.getX();
            long elapsed=Math.max(1,event.getEventTime()-downTime);
            boolean decisive=Math.abs(distance)>width*.18f
                || (elapsed<350&&Math.abs(distance)>dp(32));
            int target=decisive?startingPage+(distance>0?1:-1):Math.round(getScrollX()/(float)width);
            target=Math.max(0,Math.min(pageCount-1,target));
            final int x=target*width;
            post(()->smoothScrollTo(x,0));
        }
        return handled;
    }

    @Override public void fling(int velocityX) { /* Page snapping controls the destination. */ }

    private void rememberDown(MotionEvent event) {
        downX=event.getX();downTime=event.getEventTime();
        startingPage=Math.round(getScrollX()/(float)Math.max(1,getWidth()));
    }
    private int dp(int value) { return (int)(value*getResources().getDisplayMetrics().density+.5f); }
}
