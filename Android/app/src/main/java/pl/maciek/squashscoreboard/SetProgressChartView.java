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

/** Each game gets its own score timeline, spaced by recorded point times. */
public final class SetProgressChartView extends View {
    public interface OnSetFocusChanged { void onSetFocusChanged(int setNumber); }
    private final MatchRecord record;
    private final List<int[]> cachedSets;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path firstPath = new Path();
    private final Path secondPath = new Path();
    private final DashPathEffect dashEffect;
    private final UiTheme theme;
    private final ScaleGestureDetector scaleDetector;
    private final long cachedAxisDuration;
    private boolean interactive, dragged;
    private float zoom = 1, pan, lastX, downX;
    private int selected = -1;
    private String selectedLabel = "";
    private int focusedSet = -1;
    private OnSetFocusChanged focusListener;

    public SetProgressChartView(Context context, MatchRecord record) {
        super(context);
        this.record = record;
        this.cachedSets = record.analytics().gameSegments;
        this.cachedAxisDuration = ChartUtils.axisDurationMs(record);
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
        selectedLabel = "";
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
        List<int[]> sets = cachedSets;
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
            long end = segment[1] > segment[0] ? pointTime(segment[1] - 1) : start;
            long span = Math.max(1, end - start);
            int a = segment[2], b = segment[3];
            int max = Math.max(1, Math.max(a, b));

            paint.setColor(theme.outlineVariant);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(1));
            paint.setPathEffect(null);
            canvas.drawRect(x0, top, x1, bottom, paint);

            firstPath.reset();
            secondPath.reset();
            firstPath.moveTo(x0, bottom);
            secondPath.moveTo(x0, bottom);
            int one = 0, two = 0;
            for (int i = segment[0]; i < segment[1]; i++) {
                int winner = record.pointWinners.get(i);
                if (winner == 1) one++; else two++;
                float x = x0 + (x1 - x0) * (pointTime(i) - start) / (float) span;
                float y1 = bottom - (bottom - top) * one / max, y2 = bottom - (bottom - top) * two / max;
                firstPath.lineTo(x, y1);
                secondPath.lineTo(x, y2);
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
            canvas.drawPath(firstPath, paint);

            paint.setPathEffect(dashEffect);
            paint.setColor(c2);
            canvas.drawPath(secondPath, paint);
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
        String note = selected >= 0 && !selectedLabel.isEmpty()
            ? selectedLabel
            : (recorded ? "MATCH TIME" : "ESTIMATED DURATION FOR OLD RECORD");
        canvas.drawText(note, (left + right) / 2, getHeight() - dp(6), paint);
    }

    private boolean hasRecordedTimes() {
        return ChartUtils.hasRecordedTimes(record);
    }

    private long pointTime(int index) {
        return ChartUtils.pointTimeMs(record, index, cachedAxisDuration);
    }

    private String format(long ms) {
        return ChartUtils.formatClock(ms);
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
                if (zoom > 1.01f && getParent() != null) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                downX = lastX = event.getX();
                dragged = false;
                return true;
            case MotionEvent.ACTION_POINTER_DOWN:
                if (getParent() != null) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                return true;
            case MotionEvent.ACTION_MOVE:
                if (scaleDetector.isInProgress() && getParent() != null) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                if (event.getPointerCount() == 1 && !scaleDetector.isInProgress()) {
                    float dx = event.getX() - lastX;
                    if (Math.abs(event.getX() - downX) > dp(8)) dragged = true;
                    if (dragged && zoom > 1.01f) {
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
        List<int[]> sets = cachedSets;
        if (sets.isEmpty()) return;
        float left = dp(42), width = Math.max(1, getWidth() - dp(58));
        if (x < left || x > getWidth() - dp(16)) return;
        float setWidth = width * zoom / (focusedSet < 0 ? sets.size() : 1);
        int set = focusedSet < 0 ? Math.max(0, Math.min(sets.size() - 1, (int) ((x - left + pan) / setWidth))) : focusedSet;
        if (focusedSet < 0) {
            focusedSet = set;
            selected = -1;
            selectedLabel = "";
            zoom = 1;
            pan = 0;
            if (focusListener != null) focusListener.onSetFocusChanged(set + 1);
            invalidate();
            return;
        }
        int[] segment = sets.get(set);
        if (segment[0] >= segment[1]) return;
        float x0 = left + dp(5) - pan, x1 = left + setWidth - dp(5) - pan;
        long start = segment[0] == 0 ? 0 : pointTime(segment[0] - 1), end = pointTime(segment[1] - 1);
        double wanted = start + (x - x0) / Math.max(1, x1 - x0) * (end - start);
        selected = ChartUtils.binarySearchNearestPoint(record, segment[0], segment[1], cachedAxisDuration, wanted);
        selectedLabel = ChartUtils.formatPointInspection(record, selected, cachedAxisDuration);
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
