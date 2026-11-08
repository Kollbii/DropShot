package pl.maciek.squashscoreboard;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.View;
import java.util.Locale;

/**
 * [PERF-120HZ] Zero-allocation custom Canvas view rendering a top-down squash court
 * service-box heatmap for Left [L] and Right [R] server points, hold gauges, center "T"
 * Markov serve dominance badge, handout PICK vs ALT hold split, and return break rates.
 * Shared across {@link MatchStatsView} (2 match courts), {@link PlayerDetailActivity}
 * (1 career court), and {@link PlayerComparisonActivity} (2 career courts).
 */
public final class CourtSideDiagramView extends View {
    private static final class CourtModel {
        final String title;
        final String insight;
        final String tBadgeText;
        final String leftHeader;
        final String rightHeader;
        final String leftWonText;
        final String rightWonText;
        final String leftHoldText;
        final String rightHoldText;
        final String leftPickText;
        final String rightPickText;
        final String pickAltFooter;
        final String returnFooter;
        final int accentColor;
        final int leftHeatFill;
        final int rightHeatFill;
        final float leftRatio;
        final float rightRatio;
        final boolean leftStronger;
        final boolean rightStronger;

        CourtModel(
            UiTheme theme,
            String playerName,
            int rawColor,
            int leftWon,
            int leftTotal,
            int leftPicks,
            int rightWon,
            int rightTotal,
            int rightPicks,
            int pickedWon,
            int pickedTotal,
            int altWon,
            int altTotal,
            int rcvLeftWon,
            int rcvLeftTotal,
            int rcvRightWon,
            int rcvRightTotal,
            double markovServeShare
        ) {
            this.accentColor = theme.playerAccentOnSurface(rawColor);
            this.title = UiTheme.playerSymbol(rawColor) + " " + playerName;
            this.insight = buildTacticalInsight(leftWon, leftTotal, leftPicks, rightWon, rightTotal, rightPicks);
            this.tBadgeText = String.format(Locale.US, "T · SRV %.0f%%", 100.0 * markovServeShare);

            int lPct = pct(leftWon, leftTotal);
            int rPct = pct(rightWon, rightTotal);
            boolean comparable = leftTotal >= 2 && rightTotal >= 2 && Math.abs(lPct - rPct) >= 8;
            this.leftStronger = comparable && lPct > rPct;
            this.rightStronger = comparable && rPct > lPct;

            this.leftHeader = leftStronger ? "[L] LEFT ★" : "[L] LEFT";
            this.rightHeader = rightStronger ? "[R] RIGHT ★" : "[R] RIGHT";
            this.leftWonText = "PTS " + leftWon + "/" + leftTotal;
            this.rightWonText = "PTS " + rightWon + "/" + rightTotal;
            this.leftHoldText = "Hold " + lPct + "%";
            this.rightHoldText = "Hold " + rPct + "%";
            this.leftPickText = "Pick " + leftPicks;
            this.rightPickText = "Pick " + rightPicks;
            this.pickAltFooter = "PICK hold " + pct(pickedWon, pickedTotal) + "% (" + pickedWon + "/" + pickedTotal + ")"
                + "  ·  ALT hold " + pct(altWon, altTotal) + "% (" + altWon + "/" + altTotal + ")";
            this.returnFooter = "Return win vs [L] " + pct(rcvLeftWon, rcvLeftTotal)
                + "%  ·  vs [R] " + pct(rcvRightWon, rcvRightTotal) + "%";

            this.leftRatio = leftTotal <= 0 ? 0f : Math.max(0f, Math.min(1f, (float) leftWon / leftTotal));
            this.rightRatio = rightTotal <= 0 ? 0f : Math.max(0f, Math.min(1f, (float) rightWon / rightTotal));
            // Subtle 6%..26% tint keeps >= 5.5:1 WCAG AA foreground text contrast in light & dark themes.
            this.leftHeatFill = blend(theme.surfaceLow, accentColor, leftTotal <= 0 ? 0.03f : (0.06f + 0.20f * leftRatio));
            this.rightHeatFill = blend(theme.surfaceLow, accentColor, rightTotal <= 0 ? 0.03f : (0.06f + 0.20f * rightRatio));
        }
    }

    private final UiTheme theme;
    private final CourtModel[] courts;
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF courtRect = new RectF();
    private final RectF quadRect = new RectF();
    private final RectF boxRect = new RectF();
    private final RectF gaugeRect = new RectF();
    private final RectF tPillRect = new RectF();

    /** Dual-court constructor for per-match statistics in {@link MatchStatsView}. */
    public CourtSideDiagramView(Context context, MatchRecord record) {
        super(context);
        this.theme = new UiTheme(context);
        MatchRecord.Analytics a = record.analytics();
        this.courts = new CourtModel[]{
            new CourtModel(
                theme,
                record.playerOneName,
                record.playerOneColor,
                a.leftServesWon[0],
                a.leftServesTotal[0],
                a.leftPicks[0],
                a.rightServesWon[0],
                a.rightServesTotal[0],
                a.rightPicks[0],
                a.pickedServesWon[0],
                a.pickedServesTotal[0],
                a.altServesWon[0],
                a.altServesTotal[0],
                a.receivesVsLeftWon[0],
                a.receivesVsLeftTotal[0],
                a.receivesVsRightWon[0],
                a.receivesVsRightTotal[0],
                a.markovServeShare(0)
            ),
            new CourtModel(
                theme,
                record.playerTwoName,
                record.playerTwoColor,
                a.leftServesWon[1],
                a.leftServesTotal[1],
                a.leftPicks[1],
                a.rightServesWon[1],
                a.rightServesTotal[1],
                a.rightPicks[1],
                a.pickedServesWon[1],
                a.pickedServesTotal[1],
                a.altServesWon[1],
                a.altServesTotal[1],
                a.receivesVsLeftWon[1],
                a.receivesVsLeftTotal[1],
                a.receivesVsRightWon[1],
                a.receivesVsRightTotal[1],
                a.markovServeShare(1)
            )
        };
        configurePaints();
        setContentDescription(
            "Squash court serve side chart. "
                + record.playerOneName + ": left " + courts[0].leftWonText + " (" + courts[0].leftHoldText + "), right "
                + courts[0].rightWonText + " (" + courts[0].rightHoldText + "). "
                + record.playerTwoName + ": left " + courts[1].leftWonText + " (" + courts[1].leftHoldText + "), right "
                + courts[1].rightWonText + " (" + courts[1].rightHoldText + ")."
        );
    }

    /** Single-court constructor for career profile statistics in {@link PlayerDetailActivity}. */
    public CourtSideDiagramView(Context context, PlayerProfile profile, PlayerStatistics.Summary s) {
        super(context);
        this.theme = new UiTheme(context);
        this.courts = new CourtModel[]{
            buildSummaryCourt(theme, profile, s)
        };
        configurePaints();
        setContentDescription(
            "Career squash court serve side chart for " + profile.name
                + ": left " + courts[0].leftWonText + " (" + courts[0].leftHoldText + "), right "
                + courts[0].rightWonText + " (" + courts[0].rightHoldText + "). " + courts[0].insight
        );
    }

    /** Dual-court constructor for head-to-head career comparison in {@link PlayerComparisonActivity}. */
    public CourtSideDiagramView(
        Context context,
        PlayerProfile one,
        PlayerStatistics.Summary a,
        PlayerProfile two,
        PlayerStatistics.Summary b
    ) {
        super(context);
        this.theme = new UiTheme(context);
        this.courts = new CourtModel[]{
            buildSummaryCourt(theme, one, a),
            buildSummaryCourt(theme, two, b)
        };
        configurePaints();
        setContentDescription(
            "Career squash court comparison. "
                + one.name + ": left " + courts[0].leftWonText + " (" + courts[0].leftHoldText + "), right "
                + courts[0].rightWonText + " (" + courts[0].rightHoldText + "). "
                + two.name + ": left " + courts[1].leftWonText + " (" + courts[1].leftHoldText + "), right "
                + courts[1].rightWonText + " (" + courts[1].rightHoldText + ")."
        );
    }

    private static CourtModel buildSummaryCourt(UiTheme theme, PlayerProfile profile, PlayerStatistics.Summary s) {
        return new CourtModel(
            theme,
            profile.name,
            profile.color,
            s.leftServesWon,
            s.leftServesTotal,
            s.leftPicks,
            s.rightServesWon,
            s.rightServesTotal,
            s.rightPicks,
            s.pickedServesWon,
            s.pickedServesTotal,
            s.altServesWon,
            s.altServesTotal,
            s.receivesVsLeftWon,
            s.receivesVsLeftTotal,
            s.receivesVsRightWon,
            s.receivesVsRightTotal,
            s.markovServeShare()
        );
    }

    private void configurePaints() {
        fillPaint.setStyle(Paint.Style.FILL);
        strokePaint.setStyle(Paint.Style.STROKE);
        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFontFeatureSettings("tnum");
    }

    /**
     * Pure-Java tactical recommendation derived from Left [L] vs Right [R] serve hold rates and handout picks.
     */
    static String buildTacticalInsight(
        int leftWon,
        int leftTotal,
        int leftPicks,
        int rightWon,
        int rightTotal,
        int rightPicks
    ) {
        if (leftTotal + rightTotal == 0) {
            return "No serves recorded";
        }
        int lPct = pct(leftWon, leftTotal);
        int rPct = pct(rightWon, rightTotal);
        if (leftTotal >= 2 && rightTotal >= 2) {
            int diff = lPct - rPct;
            int totalPicks = leftPicks + rightPicks;
            if (diff >= 8) {
                boolean underPicked = totalPicks >= 2 && leftPicks * 2 < totalPicks;
                return underPicked
                    ? String.format(Locale.US, "[L] +%d%% hold · pick [L] more", diff)
                    : String.format(Locale.US, "Best box: [L] (+%d%% hold)", diff);
            }
            if (diff <= -8) {
                int rGain = -diff;
                boolean underPicked = totalPicks >= 2 && rightPicks * 2 < totalPicks;
                return underPicked
                    ? String.format(Locale.US, "[R] +%d%% hold · pick [R] more", rGain)
                    : String.format(Locale.US, "Best box: [R] (+%d%% hold)", rGain);
            }
            return "Balanced [L] / [R] serve hold";
        }
        return leftTotal >= rightTotal
            ? "Primary box: [L] (" + lPct + "% hold)"
            : "Primary box: [R] (" + rPct + "% hold)";
    }

    private static int pct(int won, int total) {
        return total <= 0 ? 0 : Math.round(100f * won / total);
    }

    private static int blend(int base, int overlay, float ratio) {
        float clamped = Math.max(0f, Math.min(1f, ratio));
        int r = Math.round(Color.red(base) * (1f - clamped) + Color.red(overlay) * clamped);
        int g = Math.round(Color.green(base) * (1f - clamped) + Color.green(overlay) * clamped);
        int b = Math.round(Color.blue(base) * (1f - clamped) + Color.blue(overlay) * clamped);
        return Color.rgb(r, g, b);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int specW = MeasureSpec.getSize(widthMeasureSpec);
        int width = specW > 0 ? specW : dp(360);
        boolean stackVertically = courts.length > 1 && width < dp(320);
        int singleCourtHeight = dp(218);
        int desiredHeight = stackVertically
            ? singleCourtHeight * 2 + dp(10)
            : singleCourtHeight;
        setMeasuredDimension(width, resolveSize(desiredHeight, heightMeasureSpec));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        if (w <= dp(20) || h <= dp(20)) return;

        if (courts.length == 1) {
            float maxCourtW = Math.min(w, dp(440));
            float left = (w - maxCourtW) * 0.5f;
            drawCourt(canvas, courts[0], left, 0f, left + maxCourtW, h);
            return;
        }

        boolean stack = w < dp(320);
        if (stack) {
            float gap = dp(10);
            float ch = (h - gap) * 0.5f;
            drawCourt(canvas, courts[0], 0f, 0f, w, ch);
            drawCourt(canvas, courts[1], 0f, ch + gap, w, h);
        } else {
            float gap = dp(10);
            float cw = (w - gap) * 0.5f;
            drawCourt(canvas, courts[0], 0f, 0f, cw, h);
            drawCourt(canvas, courts[1], cw + gap, 0f, w, h);
        }
    }

    private void drawCourt(Canvas c, CourtModel m, float x0, float y0, float x1, float y1) {
        float footerH = dp(36);
        float courtBottom = y1 - footerH;
        float radius = dp(10);

        // Outer squash court surface
        courtRect.set(x0 + dp(1), y0 + dp(2), x1 - dp(1), courtBottom);
        fillPaint.setColor(theme.surfaceLow);
        c.drawRoundRect(courtRect, radius, radius, fillPaint);

        float shortLineY = courtRect.top + courtRect.height() * 0.34f;
        float midX = courtRect.centerX();

        // Left [L] and Right [R] back-court heat quadrants
        quadRect.set(courtRect.left + dp(2), shortLineY + dp(1), midX - dp(1), courtRect.bottom - dp(2));
        fillPaint.setColor(m.leftHeatFill);
        c.drawRoundRect(quadRect, dp(6), dp(6), fillPaint);

        quadRect.set(midX + dp(1), shortLineY + dp(1), courtRect.right - dp(2), courtRect.bottom - dp(2));
        fillPaint.setColor(m.rightHeatFill);
        c.drawRoundRect(quadRect, dp(6), dp(6), fillPaint);

        // Authentic squash service boxes tucked into the outer corners below the short line
        float boxSize = Math.min((courtRect.width() * 0.5f) * 0.40f, (courtRect.bottom - shortLineY) * 0.38f);
        strokePaint.setStrokeWidth(dp(1));
        strokePaint.setColor(theme.outlineVariant);
        boxRect.set(courtRect.left, shortLineY, courtRect.left + boxSize, shortLineY + boxSize);
        c.drawRect(boxRect, strokePaint);
        boxRect.set(courtRect.right - boxSize, shortLineY, courtRect.right, shortLineY + boxSize);
        c.drawRect(boxRect, strokePaint);

        // Short line & Half-court T-line
        strokePaint.setStrokeWidth(dp(1.5f));
        strokePaint.setColor(theme.outline);
        c.drawLine(courtRect.left, shortLineY, courtRect.right, shortLineY, strokePaint);
        c.drawLine(midX, shortLineY, midX, courtRect.bottom, strokePaint);

        // Outer wall outline + emphasized Front Wall at top
        strokePaint.setStrokeWidth(dp(1.5f));
        strokePaint.setColor(theme.outlineVariant);
        c.drawRoundRect(courtRect, radius, radius, strokePaint);
        strokePaint.setStrokeWidth(dp(3));
        strokePaint.setColor(m.accentColor);
        c.drawLine(courtRect.left + radius * 0.6f, courtRect.top, courtRect.right - radius * 0.6f, courtRect.top, strokePaint);

        // Center "T" Markov Serve Dominance Pill right on the T intersection
        float pillW = Math.min(courtRect.width() * 0.48f, dp(92));
        float pillH = dp(16);
        tPillRect.set(midX - pillW * 0.5f, shortLineY - pillH * 0.5f, midX + pillW * 0.5f, shortLineY + pillH * 0.5f);
        fillPaint.setColor(theme.surfaceHigh);
        c.drawRoundRect(tPillRect, pillH * 0.5f, pillH * 0.5f, fillPaint);
        strokePaint.setStrokeWidth(dp(1));
        strokePaint.setColor(m.accentColor);
        c.drawRoundRect(tPillRect, pillH * 0.5f, pillH * 0.5f, strokePaint);

        textPaint.setTypeface(Typeface.DEFAULT_BOLD);
        textPaint.setTextSize(cappedSp(10f, dp(11)));
        textPaint.setColor(m.accentColor);
        c.drawText(m.tBadgeText, midX, shortLineY + dp(3.5f), textPaint);

        // Front-court header: Player name + Tactical insight
        float frontCenterY = (courtRect.top + shortLineY - pillH * 0.5f) * 0.5f;
        textPaint.setTypeface(Typeface.DEFAULT_BOLD);
        textPaint.setTextSize(cappedSp(13.5f, dp(15)));
        textPaint.setColor(m.accentColor);
        c.drawText(m.title, midX, frontCenterY - dp(3), textPaint);

        textPaint.setTypeface(Typeface.DEFAULT);
        textPaint.setTextSize(cappedSp(11f, dp(12)));
        textPaint.setColor(theme.onSurfaceVariant);
        c.drawText(m.insight, midX, frontCenterY + dp(12), textPaint);

        // Left [L] and Right [R] quadrant metrics + visual hold bars
        float leftCx = (courtRect.left + midX) * 0.5f;
        float rightCx = (midX + courtRect.right) * 0.5f;
        float quadW = (courtRect.width() * 0.5f);
        float backTop = shortLineY + pillH * 0.35f;
        float backH = courtRect.bottom - backTop;

        drawBoxMetrics(
            c,
            leftCx,
            quadW,
            backTop,
            backH,
            m.leftHeader,
            m.leftWonText,
            m.leftHoldText,
            m.leftPickText,
            m.leftRatio,
            m.accentColor,
            m.leftStronger
        );
        drawBoxMetrics(
            c,
            rightCx,
            quadW,
            backTop,
            backH,
            m.rightHeader,
            m.rightWonText,
            m.rightHoldText,
            m.rightPickText,
            m.rightRatio,
            m.accentColor,
            m.rightStronger
        );

        // 2-line tactical footer below back wall (PICK vs ALT hold + Return break vs [L]/[R])
        textPaint.setTypeface(Typeface.DEFAULT_BOLD);
        textPaint.setTextSize(cappedSp(10.5f, dp(11)));
        textPaint.setColor(theme.onSurface);
        c.drawText(m.pickAltFooter, midX, courtBottom + dp(14), textPaint);

        textPaint.setTypeface(Typeface.DEFAULT);
        textPaint.setTextSize(cappedSp(10.5f, dp(11)));
        textPaint.setColor(theme.onSurfaceVariant);
        c.drawText(m.returnFooter, midX, y1 - dp(4), textPaint);
    }

    private void drawBoxMetrics(
        Canvas c,
        float cx,
        float quadW,
        float backTop,
        float backH,
        String header,
        String wonText,
        String holdText,
        String pickText,
        float holdRatio,
        int accent,
        boolean stronger
    ) {
        textPaint.setTypeface(Typeface.DEFAULT_BOLD);
        textPaint.setTextSize(cappedSp(11.5f, dp(13)));
        textPaint.setColor(stronger ? accent : theme.onSurfaceVariant);
        c.drawText(header, cx, backTop + backH * 0.21f, textPaint);

        textPaint.setTypeface(Typeface.DEFAULT_BOLD);
        textPaint.setTextSize(cappedSp(14.5f, dp(16)));
        textPaint.setColor(theme.onSurface);
        c.drawText(wonText, cx, backTop + backH * 0.44f, textPaint);

        textPaint.setTypeface(Typeface.DEFAULT_BOLD);
        textPaint.setTextSize(cappedSp(12f, dp(13)));
        textPaint.setColor(accent);
        c.drawText(holdText, cx, backTop + backH * 0.64f, textPaint);

        // Visual hold rate progress gauge inside quadrant
        float barW = Math.min(quadW * 0.68f, dp(88));
        float barH = dp(4);
        float barTop = backTop + backH * 0.70f;
        float barLeft = cx - barW * 0.5f;
        gaugeRect.set(barLeft, barTop, barLeft + barW, barTop + barH);
        fillPaint.setColor(theme.outlineVariant);
        c.drawRoundRect(gaugeRect, barH * 0.5f, barH * 0.5f, fillPaint);
        if (holdRatio > 0.01f) {
            gaugeRect.set(barLeft, barTop, barLeft + barW * holdRatio, barTop + barH);
            fillPaint.setColor(accent);
            c.drawRoundRect(gaugeRect, barH * 0.5f, barH * 0.5f, fillPaint);
        }

        textPaint.setTypeface(Typeface.DEFAULT);
        textPaint.setTextSize(cappedSp(10.5f, dp(12)));
        textPaint.setColor(theme.onSurfaceVariant);
        c.drawText(pickText, cx, backTop + backH * 0.91f, textPaint);
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private float cappedSp(float spValue, int maxPx) {
        float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, spValue, getResources().getDisplayMetrics());
        return Math.min(px, maxPx);
    }
}
