package pl.maciek.squashscoreboard;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.List;
import java.util.Locale;

/**
 * Per-match statistics view showcasing visual Squash Court [L]/[R] service box dominance,
 * dual-bar head-to-head metrics, and a 120 Hz clipped Canvas rally-by-rally court-side log.
 */
public final class MatchStatsView extends ScrollView {
    private final UiTheme theme;

    public MatchStatsView(Context context, MatchRecord record) {
        super(context);
        theme = new UiTheme(context);
        LinearLayout rows = new LinearLayout(context);
        rows.setOrientation(LinearLayout.VERTICAL);
        rows.setPadding(dp(16), dp(8), dp(16), dp(16));
        addView(rows);

        MatchRecord.Analytics a = record.analytics();
        int[] sets = a.sets;
        int totalPoints = a.totalPoints();
        int format = Math.max(1, record.setsToWinMatch) * 2 - 1;

        String gameScores = record.formattedGameScores();
        String bannerText = record.playerOneName + " vs " + record.playerTwoName
            + " (" + Math.max(record.playerOneSets, sets[0]) + ":" + Math.max(record.playerTwoSets, sets[1]) + ")"
            + "  ·  BEST OF " + format
            + (gameScores.isEmpty() ? "" : "  ·  " + gameScores);
        TextView title = sectionHeader(bannerText, 15, theme.onSurface);
        UiTheme.applyTabularNumbers(title);
        rows.addView(title, new LinearLayout.LayoutParams(-1, dp(28)));

        TextView timing = sectionHeader(formatTimingSummary(record), 13, theme.onSurfaceVariant);
        UiTheme.applyTabularNumbers(timing);
        rows.addView(timing, new LinearLayout.LayoutParams(-1, dp(24)));

        // 1. Visual Squash Court [L] / [R] Server Scoring & Hold Diagram
        TextView courtSection = sectionHeader("COURT [L] AND [R] SERVE CHART", 14, theme.primary);
        rows.addView(courtSection, new LinearLayout.LayoutParams(-1, dp(28)));

        CourtSideDiagramView courtDiagram = new CourtSideDiagramView(context, record);
        LinearLayout.LayoutParams courtLp = new LinearLayout.LayoutParams(-1, -2);
        courtLp.topMargin = dp(2);
        courtLp.bottomMargin = dp(10);
        rows.addView(courtDiagram, courtLp);

        // 2. Side-by-Side Comparison Rows with Broadcast Proportion Bars
        PlayerProfile p1Profile = ProfileStore.find(context, record.playerOneProfileId);
        PlayerProfile p2Profile = ProfileStore.find(context, record.playerTwoProfileId);
        String font1 = p1Profile != null ? p1Profile.fontId : null;
        String font2 = p2Profile != null ? p2Profile.fontId : null;

        int rawC1 = record.playerOneColor;
        int rawC2 = record.playerTwoColor;

        addRow(rows, StatComparisonRowView.createHeader(
            context, theme, record.playerOneName, font1, rawC1, "STATISTICS", record.playerTwoName, font2, rawC2
        ));
        addMetric(rows, String.valueOf(sets[0]), "GAMES WON", String.valueOf(sets[1]), rawC1, rawC2, sets[0], sets[1]);
        addMetric(rows, String.valueOf(a.points[0]), "POINTS WON", String.valueOf(a.points[1]), rawC1, rawC2, a.points[0], a.points[1]);
        addMetric(
            rows,
            share(a.points[0], totalPoints),
            "POINT SHARE",
            share(a.points[1], totalPoints),
            rawC1,
            rawC2,
            a.points[0],
            a.points[1]
        );
        addMetric(
            rows,
            String.valueOf(a.longestStreak[0]),
            "LONGEST STREAK",
            String.valueOf(a.longestStreak[1]),
            rawC1,
            rawC2,
            a.longestStreak[0],
            a.longestStreak[1]
        );
        addMetric(
            rows,
            rateWithCount(a.servesWon[0], a.servesTotal[0]),
            "SERVE HOLD RATE",
            rateWithCount(a.servesWon[1], a.servesTotal[1]),
            rawC1,
            rawC2,
            100.0 * a.serveHoldRate(0),
            100.0 * a.serveHoldRate(1)
        );
        addMetric(
            rows,
            rateWithCount(a.receivesWon[0], a.receivesTotal[0]),
            "SIDE-OUT RATE",
            rateWithCount(a.receivesWon[1], a.receivesTotal[1]),
            rawC1,
            rawC2,
            100.0 * a.sideOutRate(0),
            100.0 * a.sideOutRate(1)
        );
        addMetric(
            rows,
            String.format(Locale.US, "%.1f%%", 100.0 * a.markovServeShare(0)),
            "MARKOV SERVE SHARE",
            String.format(Locale.US, "%.1f%%", 100.0 * a.markovServeShare(1)),
            rawC1,
            rawC2,
            100.0 * a.markovServeShare(0),
            100.0 * a.markovServeShare(1)
        );
        addMetric(
            rows,
            boxPtsAndHold(a.leftServesWon[0], a.leftServesTotal[0]),
            "LEFT [L] SERVE POINTS",
            boxPtsAndHold(a.leftServesWon[1], a.leftServesTotal[1]),
            rawC1,
            rawC2,
            a.leftServesWon[0],
            a.leftServesWon[1]
        );
        addMetric(
            rows,
            boxPtsAndHold(a.rightServesWon[0], a.rightServesTotal[0]),
            "RIGHT [R] SERVE POINTS",
            boxPtsAndHold(a.rightServesWon[1], a.rightServesTotal[1]),
            rawC1,
            rawC2,
            a.rightServesWon[0],
            a.rightServesWon[1]
        );
        addMetric(
            rows,
            "L:" + a.leftPicks[0] + " · R:" + a.rightPicks[0],
            "HANDOUT [L]/[R] PICKS",
            "L:" + a.leftPicks[1] + " · R:" + a.rightPicks[1],
            rawC1,
            rawC2,
            a.leftPicks[0] + a.rightPicks[0],
            a.leftPicks[1] + a.rightPicks[1]
        );
        addMetric(
            rows,
            boxPtsAndHold(a.pickedServesWon[0], a.pickedServesTotal[0]),
            "CHOSEN BOX (PICK) HOLD",
            boxPtsAndHold(a.pickedServesWon[1], a.pickedServesTotal[1]),
            rawC1,
            rawC2,
            pctValue(a.pickedServesWon[0], a.pickedServesTotal[0]),
            pctValue(a.pickedServesWon[1], a.pickedServesTotal[1])
        );
        addMetric(
            rows,
            boxPtsAndHold(a.altServesWon[0], a.altServesTotal[0]),
            "STREAK BOX (ALT) HOLD",
            boxPtsAndHold(a.altServesWon[1], a.altServesTotal[1]),
            rawC1,
            rawC2,
            pctValue(a.altServesWon[0], a.altServesTotal[0]),
            pctValue(a.altServesWon[1], a.altServesTotal[1])
        );

        // 3. Fast Clipped-Canvas Point-by-Point Serve & Court Side [L]/[R] Log with Game Filter Chips
        int n = record.pointWinners.size();
        if (n > 0) {
            TextView logTitle = sectionHeader("POINT-BY-POINT SERVE & COURT SIDE [L] / [R] LOG", 14, theme.primary);
            LinearLayout.LayoutParams logTitleLp = new LinearLayout.LayoutParams(-1, dp(34));
            logTitleLp.topMargin = dp(14);
            rows.addView(logTitle, logTitleLp);

            List<int[]> segments = a.gameSegments;
            RallyLogCanvasView logCanvas = new RallyLogCanvasView(context, theme, record);

            if (segments.size() > 1) {
                HorizontalScrollView chipScroll = new HorizontalScrollView(context);
                chipScroll.setHorizontalScrollBarEnabled(false);
                LinearLayout chipBar = new LinearLayout(context);
                chipBar.setOrientation(LinearLayout.HORIZONTAL);
                chipBar.setGravity(Gravity.CENTER_VERTICAL);
                chipScroll.addView(chipBar);

                Button[] chips = new Button[segments.size() + 1];
                for (int gi = 0; gi <= segments.size(); gi++) {
                    final int filterGame = gi; // 0 = all games, 1..K = specific game
                    String label = gi == 0
                        ? "Select all (" + n + ")"
                        : "Select G" + gi + " (" + segments.get(gi - 1)[2] + ":" + segments.get(gi - 1)[3] + ")";
                    Button chip = new Button(context);
                    chip.setAllCaps(false);
                    chip.setTextSize(14);
                    chip.setMinHeight(dp(48));
                    chip.setMinimumHeight(dp(48));
                    chip.setPadding(dp(12), 0, dp(12), 0);
                    chip.setFilterTouchesWhenObscured(true);
                    chip.setText(label);
                    chip.setOnClickListener(v -> {
                        logCanvas.setGameFilter(filterGame);
                        styleGameChips(chips, filterGame);
                    });
                    chips[gi] = chip;
                    LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(-2, dp(48));
                    if (gi > 0) clp.leftMargin = dp(8);
                    chipBar.addView(chip, clp);
                }
                styleGameChips(chips, 0);
                LinearLayout.LayoutParams barLp = new LinearLayout.LayoutParams(-1, dp(52));
                barLp.bottomMargin = dp(6);
                rows.addView(chipScroll, barLp);
            }

            rows.addView(logCanvas, new LinearLayout.LayoutParams(-1, -2));
        }
    }

    private void styleGameChips(Button[] chips, int activeIndex) {
        for (int i = 0; i < chips.length; i++) {
            Button chip = chips[i];
            if (chip == null) continue;
            boolean selected = (i == activeIndex);
            chip.setTypeface(selected ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
            chip.setTextColor(selected ? theme.onSecondaryContainer : theme.onSurfaceVariant);
            chip.setBackground(theme.interactiveBackground(
                getContext(),
                selected ? theme.secondaryContainer : theme.surfaceLow,
                12,
                1,
                selected ? theme.primary : theme.outline
            ));
        }
    }

    private void addRow(LinearLayout parent, View row) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = dp(4);
        parent.addView(row, lp);
    }

    private void addMetric(
        LinearLayout parent,
        String left,
        String label,
        String right,
        int rawC1,
        int rawC2,
        double v1,
        double v2
    ) {
        addRow(parent, StatComparisonRowView.createMetric(getContext(), theme, left, label, right, rawC1, rawC2, v1, v2));
    }

    private TextView sectionHeader(String value, int sizeSp, int color) {
        TextView view = new TextView(getContext());
        view.setText(value);
        view.setTextSize(sizeSp);
        view.setTextColor(color);
        view.setTypeface(Typeface.DEFAULT_BOLD);
        view.setGravity(Gravity.CENTER);
        view.setSingleLine(true);
        view.setEllipsize(TextUtils.TruncateAt.END);
        return view;
    }

    private static String share(int points, int total) {
        return total == 0 ? "0%" : String.format(Locale.US, "%.1f%%", 100f * points / total);
    }

    private static String rateWithCount(int won, int total) {
        if (total == 0) return "0% (0/0)";
        return String.format(Locale.US, "%.0f%% (%d/%d)", 100f * won / total, won, total);
    }

    private static String boxPtsAndHold(int won, int total) {
        if (total == 0) return "0/0 (0%)";
        return String.format(Locale.US, "%d/%d (%.0f%%)", won, total, 100f * won / total);
    }

    private static double pctValue(int won, int total) {
        return total <= 0 ? 0.0 : (100.0 * won) / total;
    }

    static String formatTimingSummary(MatchRecord r) {
        StringBuilder sb = new StringBuilder();
        sb.append("DURATION: ").append(r.durationMs > 0 ? ChartUtils.formatClock(r.durationMs) : "no data");
        if (!r.setDurationsMs.isEmpty()) {
            sb.append("   ·   GAMES: ");
            for (int i = 0; i < r.setDurationsMs.size(); i++) {
                if (i > 0) sb.append(" · ");
                sb.append("G").append(i + 1).append(" ").append(ChartUtils.formatClock(r.setDurationsMs.get(i)));
            }
        }
        return sb.toString();
    }

    private int dp(int value) {
        return UiTheme.dp(getContext(), value);
    }

    /**
     * [PERF-120HZ] Single-view clipped Canvas renderer for the rally-by-rally court-side log.
     * Replaces hundreds of nested LinearLayout/TextView instances with 1 View and draws only
     * the rows intersecting `canvas.getClipBounds(clipRect)` with 0 allocations in `onDraw`.
     */
    private static final class RallyLogCanvasView extends View {
        private final UiTheme theme;
        private final MatchRecord record;
        private final List<int[]> segments;
        private final String[] colNum;
        private final String[] colServe;
        private final String[] colSide;
        private final String[] colScore;
        private final String[] colOutcome;
        private final int[] srvColors;
        private final int[] winColors;
        private final boolean[] leftSides;
        private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Rect clipBounds = new Rect();
        private final RectF rowRect = new RectF();
        private final RectF pillRect = new RectF();
        private int startIdx;
        private int endIdx;

        RallyLogCanvasView(Context context, UiTheme theme, MatchRecord record) {
            super(context);
            this.theme = theme;
            this.record = record;
            this.segments = record.analytics().gameSegments;
            int n = record.pointWinners.size();
            this.colNum = new String[n];
            this.colServe = new String[n];
            this.colSide = new String[n];
            this.colScore = new String[n];
            this.colOutcome = new String[n];
            this.srvColors = new int[n];
            this.winColors = new int[n];
            this.leftSides = new boolean[n];

            int c1 = theme.playerAccentOnSurface(record.playerOneColor);
            int c2 = theme.playerAccentOnSurface(record.playerTwoColor);

            for (int i = 0; i < n; i++) {
                MatchRecord.PointScore ps = record.scoreAtPoint(i);
                int winner = record.pointWinners.get(i);
                String srvName = ps.server == 2 ? record.playerTwoName : record.playerOneName;
                String winName = winner == 2 ? record.playerTwoName : record.playerOneName;
                boolean serverHeld = (winner == ps.server);

                colNum[i] = "#" + (i + 1) + " G" + ps.setNumber;
                colServe[i] = "SRV " + srvName;
                colSide[i] = "[" + ps.serviceSide + "] " + (ps.sidePicked ? "PICK" : "ALT");
                colScore[i] = ps.pointsOne + ":" + ps.pointsTwo;
                colOutcome[i] = (serverHeld ? "HOLD · " : "OUT · ") + winName;
                srvColors[i] = ps.server == 1 ? c1 : c2;
                winColors[i] = winner == 1 ? c1 : c2;
                leftSides[i] = (ps.serviceSide == 'L');
            }

            bgPaint.setStyle(Paint.Style.FILL);
            strokePaint.setStyle(Paint.Style.STROKE);
            strokePaint.setStrokeWidth(UiTheme.dp(context, 1));
            textPaint.setStyle(Paint.Style.FILL);
            textPaint.setFontFeatureSettings("tnum");

            this.startIdx = 0;
            this.endIdx = n;
            updateDescription(0);
        }

        void setGameFilter(int gameNumber) {
            if (gameNumber <= 0 || gameNumber > segments.size()) {
                startIdx = 0;
                endIdx = record.pointWinners.size();
            } else {
                int[] seg = segments.get(gameNumber - 1);
                startIdx = seg[0];
                endIdx = seg[1];
            }
            updateDescription(gameNumber);
            requestLayout();
            invalidate();
        }

        private void updateDescription(int gameNumber) {
            int count = Math.max(0, endIdx - startIdx);
            setContentDescription(
                (gameNumber == 0 ? "All games rally log, " : "Game " + gameNumber + " rally log, ")
                    + count + " points with server and left or right court side."
            );
        }

        private int rowHeightPx() {
            return UiTheme.dp(getContext(), 38);
        }

        private int rowGapPx() {
            return UiTheme.dp(getContext(), 3);
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            int width = MeasureSpec.getSize(widthMeasureSpec);
            int count = Math.max(0, endIdx - startIdx);
            int step = rowHeightPx() + rowGapPx();
            int height = count == 0 ? 0 : (count * step);
            setMeasuredDimension(width, resolveSize(height, heightMeasureSpec));
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            int count = endIdx - startIdx;
            if (count <= 0) return;

            int w = getWidth();
            int rowH = rowHeightPx();
            int step = rowH + rowGapPx();
            float radius = UiTheme.dp(getContext(), 8);

            boolean hasClip = canvas.getClipBounds(clipBounds);
            int firstVisible = hasClip ? Math.max(0, clipBounds.top / step) : 0;
            int lastVisible = hasClip ? Math.min(count - 1, (clipBounds.bottom / step) + 1) : (count - 1);

            float textYOffset = rowH * 0.62f;
            float sidePillW = w * 0.20f;
            float xNum = w * 0.09f;
            float xSrv = w * 0.30f;
            float xSideCenter = w * 0.51f;
            float xScore = w * 0.67f;
            float xWin = w * 0.85f;

            float smallText = cappedSp(12f, UiTheme.dp(getContext(), 13));
            float scoreText = cappedSp(14f, UiTheme.dp(getContext(), 15));

            for (int v = firstVisible; v <= lastVisible; v++) {
                int i = startIdx + v;
                float top = v * step;
                float bottom = top + rowH;

                rowRect.set(0f, top, w, bottom);
                bgPaint.setColor(theme.surfaceLow);
                canvas.drawRoundRect(rowRect, radius, radius, bgPaint);
                strokePaint.setColor(theme.outlineVariant);
                canvas.drawRoundRect(rowRect, radius, radius, strokePaint);

                float baseline = top + textYOffset;
                textPaint.setTextAlign(Paint.Align.CENTER);

                // #Rally + Game
                textPaint.setTypeface(Typeface.DEFAULT);
                textPaint.setTextSize(smallText);
                textPaint.setColor(theme.onSurfaceVariant);
                canvas.drawText(colNum[i], xNum, baseline, textPaint);

                // Server name
                textPaint.setTypeface(Typeface.DEFAULT_BOLD);
                textPaint.setColor(srvColors[i]);
                canvas.drawText(colServe[i], xSrv, baseline, textPaint);

                // [L] / [R] PICK/ALT badge pill
                pillRect.set(
                    xSideCenter - sidePillW * 0.5f,
                    top + UiTheme.dp(getContext(), 6),
                    xSideCenter + sidePillW * 0.5f,
                    bottom - UiTheme.dp(getContext(), 6)
                );
                bgPaint.setColor(leftSides[i] ? theme.secondaryContainer : theme.surfaceHigh);
                canvas.drawRoundRect(pillRect, radius * 0.7f, radius * 0.7f, bgPaint);
                textPaint.setColor(leftSides[i] ? theme.onSecondaryContainer : theme.onSurface);
                canvas.drawText(colSide[i], xSideCenter, baseline, textPaint);

                // Running score
                textPaint.setTextSize(scoreText);
                textPaint.setColor(theme.onSurface);
                canvas.drawText(colScore[i], xScore, baseline, textPaint);

                // Outcome (HOLD / OUT + winner)
                textPaint.setTextSize(smallText);
                textPaint.setColor(winColors[i]);
                canvas.drawText(colOutcome[i], xWin, baseline, textPaint);
            }
        }

        private float cappedSp(float spValue, int maxPx) {
            float px = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP,
                spValue,
                getResources().getDisplayMetrics()
            );
            return Math.min(px, maxPx);
        }
    }
}
