package pl.maciek.squashscoreboard;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import java.util.ArrayList;
import java.util.Locale;

/** Each game gets its own score timeline, spaced by recorded point times. */
public final class SetProgressChartView extends View {
    public interface OnSetFocusChanged { void onSetFocusChanged(int setNumber); }
    private final MatchRecord record;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final DashPathEffect dashEffect;
    private final UiTheme theme;
    private final ScaleGestureDetector scaleDetector;
    private boolean interactive, dragged;
    private float zoom = 1, pan, lastX, downX;
    private int selected = -1;
    private int focusedSet = -1;
    private OnSetFocusChanged focusListener;

    public SetProgressChartView(Context context, MatchRecord record) {
        super(context);
        this.record = record;
        this.theme = new UiTheme(context);
        this.dashEffect = new DashPathEffect(new float[]{dp(10), dp(6)}, 0);
        setContentDescription("Game progress chart for " + record.playerOneName + " versus " + record.playerTwoName + ".");
        scaleDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override public boolean onScale(ScaleGestureDetector detector) {
                float plot = Math.max(1, getWidth() - dp(58));
                float focus = (detector.getFocusX() - dp(42) + pan) / (plot * zoom);
                zoom = Math.max(1, Math.min(12, zoom * detector.getScaleFactor()));
                pan = focus * plot * zoom - (detector.getFocusX() - dp(42));
                clampPan();
                invalidate();
                return true;
            }
        });
    }

    public void setInteractive(boolean enabled) { interactive = enabled; }
    public void setOnSetFocusChanged(OnSetFocusChanged listener) { focusListener = listener; }

    public void showAllSets() {
        focusedSet = -1;
        selected = -1;
        zoom = 1;
        pan = 0;
        if (focusListener != null) focusListener.onSetFocusChanged(0);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float left = dp(42), top = dp(36), right = getWidth() - dp(16), bottom = getHeight() - dp(50);
        if (right <= left || bottom <= top) return;
        ArrayList<int[]> sets = split();
        int visibleCount = focusedSet < 0 ? sets.size() : 1;
        float setWidth = (right - left) * zoom / Math.max(1, visibleCount);
        boolean recorded = hasRecordedTimes();
        int c1 = theme.playerAccentOnSurface(record.playerOneColor);
        int c2 = theme.playerAccentOnSurface(record.playerTwoColor);

        canvas.save();
        canvas.clipRect(left, 0, right, getHeight());
        for (int si = 0; si < sets.size(); si++) {
            if (focusedSet >= 0 && si != focusedSet) continue;
            int[] segment = sets.get(si);
            int visibleIndex = focusedSet < 0 ? si : 0;
            float x0 = left + visibleIndex * setWidth - pan + dp(5), x1 = left + (visibleIndex + 1) * setWidth - pan - dp(5);
            if (x1 < left || x0 > right) continue;
            long start = segment[0] == 0 ? 0 : pointTime(segment[0] - 1);
            long end = pointTime(segment[1] - 1);
            long span = Math.max(1, end - start);
            int a = 0, b = 0;
            for (int i = segment[0]; i < segment[1]; i++) if (record.pointWinners.get(i) == 1) a++; else b++;
            int max = Math.max(1, Math.max(a, b));

            paint.setColor(theme.outlineVariant);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(1));
            paint.setPathEffect(null);
            canvas.drawRect(x0, top, x1, bottom, paint);

            Path first = new Path(), second = new Path();
            first.moveTo(x0, bottom);
            second.moveTo(x0, bottom);
            int one = 0, two = 0;
            for (int i = segment[0]; i < segment[1]; i++) {
                int winner = record.pointWinners.get(i);
                if (winner == 1) one++; else two++;
                float x = x0 + (x1 - x0) * (pointTime(i) - start) / (float) span;
                float y1 = bottom - (bottom - top) * one / max, y2 = bottom - (bottom - top) * two / max;
                first.lineTo(x, y1);
                second.lineTo(x, y2);
                if (zoom > 1.5f || segment[1] - segment[0] < 30) {
                    paint.setStyle(Paint.Style.FILL);
                    paint.setColor(winner == 1 ? c1 : c2);
                    canvas.drawCircle(x, winner == 1 ? y1 : y2, dp(2), paint);
                }
                if (i == selected) {
                    paint.setStyle(Paint.Style.STROKE);
                    paint.setColor(theme.onSurface);
                    paint.setStrokeWidth(dp(2));
                    canvas.drawLine(x, top, x, bottom, paint);
                }
            }
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(3));
            paint.setPathEffect(null);
            paint.setColor(c1);
            canvas.drawPath(first, paint);

            paint.setPathEffect(dashEffect);
            paint.setColor(c2);
            canvas.drawPath(second, paint);
            paint.setPathEffect(null);

            paint.setStyle(Paint.Style.FILL);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setColor(a > b ? c1 : c2);
            paint.setTextSize(dp(13));
            canvas.drawText("GAME " + (si + 1) + "  " + a + ":" + b, (x0 + x1) / 2, top - dp(11), paint);
            paint.setColor(theme.onSurfaceVariant);
            paint.setTextSize(dp(11));
            canvas.drawText(format(start) + " – " + format(end), (x0 + x1) / 2, bottom + dp(17), paint);
        }
        canvas.restore();
        paint.setStyle(Paint.Style.FILL);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setColor(theme.onSurfaceVariant);
        paint.setTextSize(dp(12));
        String note = recorded ? "MATCH TIME" : "ESTIMATED DURATION FOR OLD RECORD";
        if (selected >= 0) {
            MatchRecord.PointScore score = record.scoreAtPoint(selected);
            note = "GAME " + score.setNumber + "  " + score.pointsOne + ":" + score.pointsTwo
                + "  ·  GAMES " + score.setsOne + ":" + score.setsTwo + "  ·  " + formatPrecise(pointTime(selected));
        }
        canvas.drawText(note, (left + right) / 2, getHeight() - dp(6), paint);
    }

    private ArrayList<int[]> split() {
        ArrayList<int[]> out = new ArrayList<>();
        int a = 0, b = 0, start = 0;
        for (int i = 0; i < record.pointWinners.size(); i++) {
            if (record.pointWinners.get(i) == 1) a++; else b++;
            if (Math.max(a, b) >= record.pointsToWinSet && Math.abs(a - b) >= 2) {
                out.add(new int[]{start, i + 1});
                start = i + 1;
                a = 0;
                b = 0;
            }
        }
        if (start < record.pointWinners.size()) out.add(new int[]{start, record.pointWinners.size()});
        return out;
    }

    private boolean hasRecordedTimes() {
        return !record.pointTimesMs.isEmpty() && record.pointTimesMs.size() == record.pointWinners.size();
    }

    private long pointTime(int index) {
        if (hasRecordedTimes()) return record.pointTimesMs.get(index);
        return Math.max(1, record.durationMs) * (index + 1) / Math.max(1, record.pointWinners.size());
    }

    private String format(long ms) {
        long seconds = ms / 1000;
        return String.format(Locale.US, "%d:%02d", seconds / 60, seconds % 60);
    }

    private String formatPrecise(long ms) {
        return String.format(Locale.US, "%d:%02d.%03d", ms / 60000, (ms / 1000) % 60, ms % 1000);
    }

    private void clampPan() {
        pan = Math.max(0, Math.min(Math.max(0, (getWidth() - dp(58)) * (zoom - 1)), pan));
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
                        pan -= dx;
                        clampPan();
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

    private void selectNearest(float x) {
        ArrayList<int[]> sets = split();
        if (sets.isEmpty()) return;
        float left = dp(42), width = Math.max(1, getWidth() - dp(58));
        if (x < left || x > getWidth() - dp(16)) return;
        float setWidth = width * zoom / (focusedSet < 0 ? sets.size() : 1);
        int set = focusedSet < 0 ? Math.max(0, Math.min(sets.size() - 1, (int) ((x - left + pan) / setWidth))) : focusedSet;
        if (focusedSet < 0) {
            focusedSet = set;
            selected = -1;
            zoom = 1;
            pan = 0;
            if (focusListener != null) focusListener.onSetFocusChanged(set + 1);
            invalidate();
            return;
        }
        int[] segment = sets.get(set);
        float x0 = left + dp(5) - pan, x1 = left + setWidth - dp(5) - pan;
        long start = segment[0] == 0 ? 0 : pointTime(segment[0] - 1), end = pointTime(segment[1] - 1);
        double wanted = start + (x - x0) / Math.max(1, x1 - x0) * (end - start), best = Double.MAX_VALUE;
        for (int i = segment[0]; i < segment[1]; i++) {
            double difference = Math.abs(pointTime(i) - wanted);
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
