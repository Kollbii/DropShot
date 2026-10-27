package pl.maciek.squashscoreboard;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import java.util.List;
import java.util.Locale;

/** Cumulative points against active match time. Fullscreen supports pinch, drag, zoom buttons, and point inspection. */
public final class ScoreChartView extends View {
    private final MatchRecord record;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path firstPath = new Path();
    private final Path secondPath = new Path();
    private final DashPathEffect dashEffect;
    private final UiTheme theme;
    private final ScaleGestureDetector scaleDetector;
    private boolean interactive, dragged;
    private double zoom = 1, windowStart;
    private float lastX, downX;
    private int selected = -1;

    public ScoreChartView(Context context, MatchRecord record) {
        super(context);
        this.record = record;
        this.theme = new UiTheme(context);
        this.dashEffect = new DashPathEffect(new float[]{dp(10), dp(6)}, 0);
        setContentDescription("Point timeline chart for " + record.playerOneName + " versus " + record.playerTwoName + ".");
        scaleDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override public boolean onScale(ScaleGestureDetector detector) {
                double duration = axisDuration(), oldWindow = duration / zoom;
                float left = dp(50), width = Math.max(1, getWidth() - left - dp(18));
                double fraction = Math.max(0, Math.min(1, (detector.getFocusX() - left) / width));
                double focusTime = windowStart + fraction * oldWindow;
                zoom = Math.max(1, Math.min(Math.min(128, duration / 1000d), zoom * detector.getScaleFactor()));
                windowStart = focusTime - fraction * duration / zoom;
                clampWindow();
                invalidate();
                return true;
            }
        });
    }

    public void setInteractive(boolean enabled) { interactive = enabled; }

    public void zoomBy(double factor) {
        double duration = axisDuration(), oldWindow = duration / zoom;
        double centerTime = windowStart + 0.5 * oldWindow;
        zoom = Math.max(1, Math.min(Math.min(128, duration / 1000d), zoom * factor));
        windowStart = centerTime - 0.5 * duration / zoom;
        clampWindow();
        invalidate();
    }

    public void resetZoom() {
        zoom = 1;
        windowStart = 0;
        selected = -1;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas c) {
        super.onDraw(c);
        int width = getWidth(), height = getHeight();
        float left = dp(50), top = dp(32), right = width - dp(18), bottom = height - dp(50);
        if (right <= left || bottom <= top) return;
        long duration = axisDuration();
        double window = duration / zoom, end = windowStart + window;
        List<Integer> winners = record.pointWinners;
        int one, two, min = Integer.MAX_VALUE, max = 0;
        int startOne = 0, startTwo = 0;
        for (int i = 0; i < winners.size(); i++) {
            long time = pointTime(i, duration);
            if (time < windowStart) {
                if (winners.get(i) == 1) startOne++; else startTwo++;
            }
        }
        one = startOne;
        two = startTwo;
        min = Math.min(one, two);
        max = Math.max(one, two);
        for (int i = 0; i < winners.size(); i++) {
            long time = pointTime(i, duration);
            if (time < windowStart || time > end) continue;
            if (winners.get(i) == 1) one++; else two++;
            min = Math.min(min, Math.min(one, two));
            max = Math.max(max, Math.max(one, two));
        }
        int yMin = Math.max(0, min - 1), yMax = Math.max(yMin + 1, max + 1);
        drawGrid(c, left, top, right, bottom, yMin, yMax, windowStart, end);

        int c1 = theme.playerAccentOnSurface(record.playerOneColor);
        int c2 = theme.playerAccentOnSurface(record.playerTwoColor);

        firstPath.reset();
        secondPath.reset();
        firstPath.moveTo(left, y(startOne, yMin, yMax, top, bottom));
        secondPath.moveTo(left, y(startTwo, yMin, yMax, top, bottom));
        one = startOne;
        two = startTwo;
        for (int i = 0; i < winners.size(); i++) {
            long time = pointTime(i, duration);
            if (time < windowStart || time > end) continue;
            if (winners.get(i) == 1) one++; else two++;
            float x = x(time, left, right, windowStart, window);
            float y1 = y(one, yMin, yMax, top, bottom);
            float y2 = y(two, yMin, yMax, top, bottom);
            firstPath.lineTo(x, y1);
            secondPath.lineTo(x, y2);
            if (zoom > 1.5 || winners.size() < 40) {
                paint.setStyle(Paint.Style.FILL);
                paint.setPathEffect(null);
                if (winners.get(i) == 1) {
                    paint.setColor(c1);
                    c.drawCircle(x, y1, dp(3), paint);
                } else {
                    paint.setColor(c2);
                    c.drawRect(x - dp(3), y2 - dp(3), x + dp(3), y2 + dp(3), paint);
                }
            }
        }

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(3));
        paint.setPathEffect(null);
        paint.setColor(c1);
        c.drawPath(firstPath, paint);

        paint.setPathEffect(dashEffect);
        paint.setColor(c2);
        c.drawPath(secondPath, paint);
        paint.setPathEffect(null);

        paint.setStyle(Paint.Style.FILL);
        drawSelection(c, left, right, top, bottom, windowStart, window, duration);

        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(dp(12));
        paint.setColor(theme.onSurfaceVariant);
        String caption = hasRecordedTimes()
            ? "MATCH TIME  ·  PINCH, DRAG, OR TAP A POINT"
            : "ESTIMATED DURATION FOR OLD RECORD";
        c.drawText(interactive ? caption : "MATCH TIME", (left + right) / 2, height - dp(7), paint);
    }

    private void drawGrid(Canvas c, float left, float top, float right, float bottom, int min, int max, double start, double end) {
        for (int i = 0; i <= 4; i++) {
            float yy = bottom - (bottom - top) * i / 4f;
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(1));
            paint.setColor(theme.outlineVariant);
            c.drawLine(left, yy, right, yy, paint);
            paint.setStyle(Paint.Style.FILL);
            paint.setTextAlign(Paint.Align.RIGHT);
            paint.setTextSize(dp(12));
            paint.setColor(theme.onSurfaceVariant);
            c.drawText(String.valueOf(Math.round(min + (max - min) * i / 4f)), left - dp(7), yy + dp(4), paint);
        }
        long step = tickStep((end - start) / 5);
        long first = (long) Math.ceil(start / step) * step;
        for (long time = first; time <= end; time += step) {
            float xx = x(time, left, right, start, end - start);
            paint.setStyle(Paint.Style.STROKE);
            paint.setColor(theme.outlineVariant);
            paint.setStrokeWidth(dp(1));
            c.drawLine(xx, top, xx, bottom, paint);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(theme.onSurfaceVariant);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTextSize(dp(11));
            c.drawText(format(time), xx, bottom + dp(19), paint);
            if (Long.MAX_VALUE - time < step) break;
        }
    }

    private void drawSelection(Canvas c, float left, float right, float top, float bottom, double start, double window, long duration) {
        if (selected < 0 || selected >= record.pointWinners.size()) return;
        long time = pointTime(selected, duration);
        if (time < start || time > start + window) return;
        float xx = x(time, left, right, start, window);
        paint.setColor(theme.onSurface);
        paint.setStrokeWidth(dp(2));
        paint.setStyle(Paint.Style.STROKE);
        c.drawLine(xx, top, xx, bottom, paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setTextSize(dp(13));
        paint.setTextAlign(Paint.Align.CENTER);
        MatchRecord.PointScore score = record.scoreAtPoint(selected);
        String label = "GAME " + score.setNumber + "  " + score.pointsOne + ":" + score.pointsTwo
            + "  ·  GAMES " + score.setsOne + ":" + score.setsTwo + "  ·  " + formatPrecise(time);
        c.drawText(label, (left + right) / 2, top - dp(9), paint);
    }

    private static long tickStep(double desired) {
        long[] steps = {1000, 2000, 5000, 10000, 15000, 30000, 60000, 120000, 300000, 600000, 900000, 1800000, 3600000};
        for (long step : steps) if (step >= desired) return step;
        return 3600000L * Math.max(1, (long) Math.ceil(desired / 3600000));
    }

    private float x(double time, float left, float right, double start, double window) {
        return left + (right - left) * (float) ((time - start) / window);
    }

    private float y(int score, int min, int max, float top, float bottom) {
        return bottom - (bottom - top) * (score - min) / (max - min);
    }

    private long axisDuration() {
        long last = 0;
        for (long t : record.pointTimesMs) last = Math.max(last, t);
        return Math.max(1, Math.max(record.durationMs, last));
    }

    private boolean hasRecordedTimes() {
        return !record.pointTimesMs.isEmpty() && record.pointTimesMs.size() == record.pointWinners.size();
    }

    private long pointTime(int index, long duration) {
        return hasRecordedTimes()
            ? Math.min(duration, Math.max(0, record.pointTimesMs.get(index)))
            : duration * (index + 1) / Math.max(1, record.pointWinners.size());
    }

    private String format(long ms) {
        long seconds = ms / 1000;
        return seconds >= 3600
            ? String.format(Locale.US, "%d:%02d:%02d", seconds / 3600, (seconds / 60) % 60, seconds % 60)
            : String.format(Locale.US, "%d:%02d", seconds / 60, seconds % 60);
    }

    private String formatPrecise(long ms) {
        return String.format(Locale.US, "%d:%02d.%03d", ms / 60000, (ms / 1000) % 60, ms % 1000);
    }

    private void clampWindow() {
        windowStart = Math.max(0, Math.min(axisDuration() - axisDuration() / zoom, windowStart));
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!interactive) return super.onTouchEvent(event);
        scaleDetector.onTouchEvent(event);
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                getParent().requestDisallowInterceptTouchEvent(true);
                downX = lastX = event.getX();
                dragged = false;
                return true;
            case MotionEvent.ACTION_MOVE:
                if (event.getPointerCount() == 1 && !scaleDetector.isInProgress()) {
                    float dx = event.getX() - lastX;
                    if (Math.abs(event.getX() - downX) > dp(8)) dragged = true;
                    if (dragged) {
                        windowStart -= dx / Math.max(1f, getWidth() - dp(68)) * axisDuration() / zoom;
                        clampWindow();
                        invalidate();
                    }
                    lastX = event.getX();
                }
                return true;
            case MotionEvent.ACTION_UP:
                if (!dragged && !scaleDetector.isInProgress()) selectNearest(event.getX());
                performClick();
                return true;
            case MotionEvent.ACTION_CANCEL:
                return true;
            default:
                return true;
        }
    }

    private void selectNearest(float touchX) {
        if (record.pointWinners.isEmpty()) return;
        double wanted = windowStart + (touchX - dp(50)) / Math.max(1f, getWidth() - dp(68)) * axisDuration() / zoom;
        long duration = axisDuration();
        double best = Double.MAX_VALUE;
        for (int i = 0; i < record.pointWinners.size(); i++) {
            double difference = Math.abs(pointTime(i, duration) - wanted);
            if (difference < best) {
                best = difference;
                selected = i;
            }
        }
        invalidate();
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
