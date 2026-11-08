package pl.maciek.squashscoreboard;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;
import pl.maciek.squashscoreboard.lootbox.RewardFonts;

/**
 * [PERF-120HZ] Shared metric row supporting both 3-column head-to-head comparison bars
 * ({@link MatchStatsView}, {@link PlayerComparisonActivity}) and 2-column single-player
 * career progress bars ({@link PlayerDetailActivity}) with zero allocations in {@code dispatchDraw}.
 */
final class StatComparisonRowView extends LinearLayout {
    private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF barRect = new RectF();
    private final boolean singlePlayerMode;
    private final boolean showBars;
    private final float leftRatio;
    private final float rightRatio;
    private final int colorOne;
    private final int colorTwo;
    private final int trackColor;

    private StatComparisonRowView(
        Context context,
        UiTheme theme,
        String leftText,
        String fontOne,
        String label,
        String rightText,
        String fontTwo,
        int rawColorOne,
        int rawColorTwo,
        boolean header,
        double valOne,
        double valTwo
    ) {
        super(context);
        this.singlePlayerMode = false;
        this.colorOne = theme.playerAccentOnSurface(rawColorOne);
        this.colorTwo = theme.playerAccentOnSurface(rawColorTwo);
        this.trackColor = theme.outlineVariant;
        double sum = Math.max(0.0, valOne) + Math.max(0.0, valTwo);
        this.showBars = !header && sum > 0.0;
        this.leftRatio = showBars ? (float) (Math.max(0.0, valOne) / sum) : 0f;
        this.rightRatio = showBars ? (float) (Math.max(0.0, valTwo) / sum) : 0f;
        this.barPaint.setStyle(Paint.Style.FILL);

        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);
        int minH = UiTheme.dp(context, header ? 48 : 50);
        setMinimumHeight(minH);
        setPadding(UiTheme.dp(context, 6), 0, UiTheme.dp(context, 6), showBars ? UiTheme.dp(context, 6) : 0);
        setBackground(theme.roundedBox(
            header ? theme.surfaceHigh : theme.surfaceLow,
            10,
            1,
            theme.outlineVariant,
            context
        ));
        setWillNotDraw(false);
        setContentDescription(label + ": " + leftText + " versus " + rightText);

        boolean oneLeads = !header && valOne > valTwo + 1e-6;
        boolean twoLeads = !header && valTwo > valOne + 1e-6;

        String dispLeft = header
            ? UiTheme.playerSymbol(rawColorOne) + " " + leftText
            : (oneLeads ? "▸ " + leftText : leftText);
        String dispRight = header
            ? UiTheme.playerSymbol(rawColorTwo) + " " + rightText
            : (twoLeads ? rightText + " ◂" : rightText);

        TextView leftCell = buildCell(context, dispLeft, header ? 17 : 16, colorOne, header || oneLeads || valOne == valTwo, Gravity.CENTER);
        TextView midCell = buildCell(context, label, header ? 15 : 14, theme.onSurfaceVariant, header, Gravity.CENTER);
        TextView rightCell = buildCell(context, dispRight, header ? 17 : 16, colorTwo, header || twoLeads || valOne == valTwo, Gravity.CENTER);

        if (header) {
            RewardFonts.apply(leftCell, fontOne, 17f);
            RewardFonts.apply(rightCell, fontTwo, 17f);
        } else {
            UiTheme.applyTabularNumbers(leftCell);
            UiTheme.applyTabularNumbers(rightCell);
        }

        addView(leftCell, new LayoutParams(0, minH, 1.05f));
        addView(midCell, new LayoutParams(0, minH, 1.35f));
        addView(rightCell, new LayoutParams(0, minH, 1.05f));
    }

    /** Single-player 2-column constructor with a full-width bottom progress bar for {@link PlayerDetailActivity}. */
    private StatComparisonRowView(
        Context context,
        UiTheme theme,
        String label,
        String primaryValue,
        String secondaryDetail,
        int rawPlayerColor,
        double fillRatio0to1
    ) {
        super(context);
        this.singlePlayerMode = true;
        this.colorOne = theme.playerAccentOnSurface(rawPlayerColor);
        this.colorTwo = this.colorOne;
        this.trackColor = theme.outlineVariant;
        this.showBars = fillRatio0to1 >= 0.0;
        this.leftRatio = showBars ? (float) Math.max(0.0, Math.min(1.0, fillRatio0to1)) : 0f;
        this.rightRatio = 0f;
        this.barPaint.setStyle(Paint.Style.FILL);

        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);
        int minH = UiTheme.dp(context, 52);
        setMinimumHeight(minH);
        setPadding(UiTheme.dp(context, 14), 0, UiTheme.dp(context, 14), showBars ? UiTheme.dp(context, 6) : 0);
        setBackground(theme.roundedBox(theme.surfaceLow, 10, 1, theme.outlineVariant, context));
        setWillNotDraw(false);

        String combinedRight = (secondaryDetail == null || secondaryDetail.isEmpty())
            ? primaryValue
            : primaryValue + "   ·   " + secondaryDetail;
        setContentDescription(label + ": " + combinedRight);

        TextView labelCell = buildCell(context, label, 14, theme.onSurfaceVariant, true, Gravity.START | Gravity.CENTER_VERTICAL);
        TextView valueCell = buildCell(context, combinedRight, 15, colorOne, true, Gravity.END | Gravity.CENTER_VERTICAL);
        UiTheme.applyTabularNumbers(valueCell);

        addView(labelCell, new LayoutParams(0, minH, 1.05f));
        addView(valueCell, new LayoutParams(0, minH, 1.25f));
    }

    static StatComparisonRowView createHeader(
        Context context,
        UiTheme theme,
        String playerOneName,
        String fontOne,
        int rawColorOne,
        String centerLabel,
        String playerTwoName,
        String fontTwo,
        int rawColorTwo
    ) {
        return new StatComparisonRowView(
            context,
            theme,
            playerOneName,
            fontOne,
            centerLabel,
            playerTwoName,
            fontTwo,
            rawColorOne,
            rawColorTwo,
            true,
            0.0,
            0.0
        );
    }

    static StatComparisonRowView createMetric(
        Context context,
        UiTheme theme,
        String leftText,
        String label,
        String rightText,
        int rawColorOne,
        int rawColorTwo,
        double valueOne,
        double valueTwo
    ) {
        return new StatComparisonRowView(
            context,
            theme,
            leftText,
            null,
            label,
            rightText,
            null,
            rawColorOne,
            rawColorTwo,
            false,
            valueOne,
            valueTwo
        );
    }

    static StatComparisonRowView createSinglePlayerMetric(
        Context context,
        UiTheme theme,
        String label,
        String primaryValue,
        String secondaryDetail,
        int rawPlayerColor,
        double fillRatio0to1
    ) {
        return new StatComparisonRowView(
            context,
            theme,
            label,
            primaryValue,
            secondaryDetail,
            rawPlayerColor,
            fillRatio0to1
        );
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        if (!showBars) return;
        int w = getWidth();
        int h = getHeight();
        float inset = UiTheme.dp(getContext(), 12);
        float barH = UiTheme.dp(getContext(), 4);
        float bottom = h - UiTheme.dp(getContext(), 3);
        float top = bottom - barH;
        float span = Math.max(0f, w - inset * 2f);
        if (span <= 0f) return;

        // Subtle background track
        barPaint.setColor(trackColor);
        barRect.set(inset, top, w - inset, bottom);
        canvas.drawRoundRect(barRect, barH * 0.5f, barH * 0.5f, barPaint);

        if (singlePlayerMode) {
            float fillLen = span * leftRatio;
            if (fillLen > 0.5f) {
                barPaint.setColor(colorOne);
                barRect.set(inset, top, inset + fillLen, bottom);
                canvas.drawRoundRect(barRect, barH * 0.5f, barH * 0.5f, barPaint);
            }
            return;
        }

        // Player 1 grows outward to the left from center; Player 2 grows outward to the right from center
        float midX = w * 0.5f;
        float halfSpan = Math.max(0f, midX - inset - UiTheme.dp(getContext(), 2));
        if (halfSpan <= 0f) return;

        float gap = UiTheme.dp(getContext(), 1);
        float p1Len = halfSpan * leftRatio;
        if (p1Len > 0.5f) {
            barPaint.setColor(colorOne);
            barRect.set(midX - gap - p1Len, top, midX - gap, bottom);
            canvas.drawRoundRect(barRect, barH * 0.5f, barH * 0.5f, barPaint);
        }
        float p2Len = halfSpan * rightRatio;
        if (p2Len > 0.5f) {
            barPaint.setColor(colorTwo);
            barRect.set(midX + gap, top, midX + gap + p2Len, bottom);
            canvas.drawRoundRect(barRect, barH * 0.5f, barH * 0.5f, barPaint);
        }
    }

    private static TextView buildCell(Context context, String value, int sizeSp, int color, boolean bold, int gravity) {
        TextView view = new TextView(context);
        view.setText(value);
        view.setTextSize(sizeSp);
        view.setTextColor(color);
        view.setTypeface(bold ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
        view.setGravity(gravity);
        view.setSingleLine(true);
        view.setEllipsize(TextUtils.TruncateAt.END);
        return view;
    }
}
