package pl.maciek.squashscoreboard;

import android.content.Context;
import android.view.MotionEvent;
import android.widget.HorizontalScrollView;

/**
 * Snaps one chart or statistics page in the direction of a horizontal swipe,
 * intercepting horizontal drags over child charts and vertical scroll views.
 */
public final class ChartPager extends HorizontalScrollView {
    public interface OnPageChangeListener {
        void onPageChanged(int page);
    }

    private int pageCount = 1;
    private int startingPage;
    private float downX;
    private float downY;
    private long downTime;
    private OnPageChangeListener pageChangeListener;

    public ChartPager(Context context) {
        super(context);
        setOverScrollMode(OVER_SCROLL_NEVER);
    }

    public void setPageCount(int count) {
        pageCount = Math.max(1, count);
    }

    public void setOnPageChangeListener(OnPageChangeListener listener) {
        this.pageChangeListener = listener;
    }

    public void scrollToPage(int page, boolean smooth) {
        int clamped = Math.max(0, Math.min(pageCount - 1, page));
        startingPage = clamped;
        int width = Math.max(1, getWidth());
        int targetX = clamped * width;
        if (smooth) {
            smoothScrollTo(targetX, 0);
        } else {
            scrollTo(targetX, 0);
        }
        if (pageChangeListener != null) {
            pageChangeListener.onPageChanged(clamped);
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN) {
            rememberDown(event);
        } else if (action == MotionEvent.ACTION_MOVE && event.getPointerCount() == 1) {
            float dx = Math.abs(event.getX() - downX);
            float dy = Math.abs(event.getY() - downY);
            if (dx > dp(12) && dx > dy * 1.15f) {
                return true;
            }
        }
        return super.onInterceptTouchEvent(event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN) {
            rememberDown(event);
        }
        boolean handled = super.onTouchEvent(event);
        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            int width = Math.max(1, getWidth());
            float distance = downX - event.getX();
            long elapsed = Math.max(1, event.getEventTime() - downTime);
            boolean decisive = Math.abs(distance) > width * 0.16f
                || (elapsed < 380 && Math.abs(distance) > dp(28));
            int target = decisive
                ? startingPage + (distance > 0 ? 1 : -1)
                : Math.round(getScrollX() / (float) width);
            target = Math.max(0, Math.min(pageCount - 1, target));
            final int finalTarget = target;
            final int x = target * width;
            post(() -> {
                smoothScrollTo(x, 0);
                if (pageChangeListener != null) {
                    pageChangeListener.onPageChanged(finalTarget);
                }
            });
        }
        return handled;
    }

    @Override
    public void fling(int velocityX) {
        // Page snapping controls the destination.
    }

    private void rememberDown(MotionEvent event) {
        downX = event.getX();
        downY = event.getY();
        downTime = event.getEventTime();
        startingPage = Math.round(getScrollX() / (float) Math.max(1, getWidth()));
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
