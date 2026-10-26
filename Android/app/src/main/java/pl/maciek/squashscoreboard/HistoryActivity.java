package pl.maciek.squashscoreboard;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoryActivity extends Activity {
    private static final int FILTER_ALL = 0;
    private static final int FILTER_FINISHED = 1;
    private static final int FILTER_UNFINISHED = 2;

    private UiTheme theme;
    private int activeFilter = FILTER_ALL;
    private String searchQuery = "";
    private LinearLayout listContainer;
    private Button chipAll, chipFinished, chipUnfinished;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        showRecords();
    }

    @Override
    protected void onResume() {
        super.onResume();
        showRecords();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        showRecords();
    }

    private void showRecords() {
        theme = new UiTheme(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(12), dp(16), dp(8));
        root.setBackgroundColor(theme.background);

        // Top App Bar
        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);

        Button back = theme.outlinedButton(this, "← Go back");
        back.setContentDescription("Go back.");
        back.setOnClickListener(v -> finish());

        TextView title = text("Match history", 24, theme.onSurface);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER_VERTICAL);
        title.setPadding(dp(16), 0, dp(16), 0);

        Button playersBtn = theme.tonalButton(this, "Open players");
        playersBtn.setOnClickListener(v -> startActivity(new Intent(this, PlayerStatsActivity.class)));

        header.addView(back, new LinearLayout.LayoutParams(-2, dp(48)));
        header.addView(title, new LinearLayout.LayoutParams(0, dp(48), 1));
        header.addView(playersBtn, new LinearLayout.LayoutParams(-2, dp(48)));
        root.addView(header, new LinearLayout.LayoutParams(-1, dp(52)));

        // Search and Filter Bar
        LinearLayout filterBar = new LinearLayout(this);
        filterBar.setOrientation(LinearLayout.HORIZONTAL);
        filterBar.setGravity(Gravity.CENTER_VERTICAL);
        filterBar.setPadding(0, dp(8), 0, dp(8));

        EditText searchInput = theme.editField(
            this,
            searchQuery,
            "Search by player name or date",
            InputType.TYPE_CLASS_TEXT
        );
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s == null ? "" : s.toString().trim();
                populateList();
            }
            @Override public void afterTextChanged(Editable s) { }
        });

        Button clearBtn = theme.outlinedButton(this, "Clear search");
        clearBtn.setOnClickListener(v -> {
            searchInput.setText("");
            searchQuery = "";
            activeFilter = FILTER_ALL;
            updateChipStyles();
            populateList();
        });

        chipAll = filterChip("Select all", FILTER_ALL);
        chipFinished = filterChip("Select finished", FILTER_FINISHED);
        chipUnfinished = filterChip("Select unfinished", FILTER_UNFINISHED);

        LinearLayout.LayoutParams searchLp = new LinearLayout.LayoutParams(0, dp(48), 1f);
        searchLp.rightMargin = dp(8);
        filterBar.addView(searchInput, searchLp);
        filterBar.addView(clearBtn, chipParams());
        filterBar.addView(chipAll, chipParams());
        filterBar.addView(chipFinished, chipParams());
        filterBar.addView(chipUnfinished, chipParams());
        root.addView(filterBar, new LinearLayout.LayoutParams(-1, dp(64)));

        // Match List
        listContainer = new LinearLayout(this);
        listContainer.setOrientation(LinearLayout.VERTICAL);
        ScrollView scroll = new ScrollView(this);
        scroll.addView(listContainer);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        // Bottom Navigation Bar
        View navBar = theme.createBottomNavBar(this, UiTheme.TAB_HISTORY, null);
        root.addView(navBar, new LinearLayout.LayoutParams(-1, dp(56)));

        updateChipStyles();
        populateList();
        setContentView(root);
    }

    private LinearLayout.LayoutParams chipParams() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, dp(48));
        lp.leftMargin = dp(4);
        return lp;
    }

    private Button filterChip(String label, int filterMode) {
        Button chip = new Button(this);
        chip.setAllCaps(false);
        chip.setTextSize(16);
        chip.setMinHeight(dp(48));
        chip.setMinimumHeight(dp(48));
        chip.setPadding(dp(12), 0, dp(12), 0);
        chip.setOnClickListener(v -> {
            activeFilter = filterMode;
            updateChipStyles();
            populateList();
        });
        return chip;
    }

    private void updateChipStyles() {
        List<MatchRecord> all = MatchHistoryStore.load(this);
        int finCount = 0, unfinCount = 0;
        for (MatchRecord r : all) {
            if (r.finished) finCount++; else unfinCount++;
        }
        styleChip(chipAll, activeFilter == FILTER_ALL, "All (" + all.size() + ")");
        styleChip(chipFinished, activeFilter == FILTER_FINISHED, "Finished (" + finCount + ")");
        styleChip(chipUnfinished, activeFilter == FILTER_UNFINISHED, "Unfinished (" + unfinCount + ")");
    }

    private void styleChip(Button chip, boolean selected, String text) {
        if (chip == null) return;
        chip.setText((selected ? "✓ " : "") + text);
        chip.setTypeface(selected ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
        chip.setTextColor(selected ? theme.onSecondaryContainer : theme.onSurfaceVariant);
        chip.setBackground(theme.interactiveBackground(
            this,
            selected ? theme.secondaryContainer : theme.surfaceLow,
            12,
            1,
            selected ? theme.primary : theme.outline
        ));
    }

    private void populateList() {
        if (listContainer == null) return;
        listContainer.removeAllViews();

        List<MatchRecord> allRecords = MatchHistoryStore.load(this);
        if (allRecords.isEmpty()) {
            LinearLayout emptyBox = new LinearLayout(this);
            emptyBox.setOrientation(LinearLayout.VERTICAL);
            emptyBox.setGravity(Gravity.CENTER);
            emptyBox.setPadding(dp(24), dp(32), dp(24), dp(32));

            TextView emptyTitle = text("No saved matches", 22, theme.onSurface);
            emptyTitle.setTypeface(Typeface.DEFAULT_BOLD);
            emptyTitle.setGravity(Gravity.CENTER);

            TextView emptyDesc = text(
                "The app saves each match here when the match finishes or when you start a new match.",
                16,
                theme.onSurfaceVariant
            );
            emptyDesc.setGravity(Gravity.CENTER);
            emptyDesc.setPadding(0, dp(8), 0, dp(16));

            Button openMatch = theme.primaryButton(this, "Open match screen");
            openMatch.setOnClickListener(v -> finish());

            emptyBox.addView(emptyTitle);
            emptyBox.addView(emptyDesc);
            emptyBox.addView(openMatch, new LinearLayout.LayoutParams(-2, dp(48)));
            listContainer.addView(emptyBox, new LinearLayout.LayoutParams(-1, -2));
            return;
        }

        String q = searchQuery.toLowerCase(Locale.getDefault());
        List<MatchRecord> filtered = new ArrayList<>();
        for (MatchRecord r : allRecords) {
            if (activeFilter == FILTER_FINISHED && !r.finished) continue;
            if (activeFilter == FILTER_UNFINISHED && r.finished) continue;
            if (!q.isEmpty()) {
                String date = DateFormat.format("dd.MM.yyyy HH:mm", new Date(r.finishedAt)).toString().toLowerCase(Locale.getDefault());
                String score = r.playerOneSets + ":" + r.playerTwoSets;
                boolean hit = r.playerOneName.toLowerCase(Locale.getDefault()).contains(q)
                    || r.playerTwoName.toLowerCase(Locale.getDefault()).contains(q)
                    || date.contains(q)
                    || score.contains(q);
                if (!hit) continue;
            }
            filtered.add(r);
        }

        if (filtered.isEmpty()) {
            TextView noHit = text(
                "No saved matches match this filter. Tap Clear search to show all matches.",
                16,
                theme.onSurfaceVariant
            );
            noHit.setGravity(Gravity.CENTER);
            noHit.setPadding(dp(16), dp(32), dp(16), dp(32));
            listContainer.addView(noHit, new LinearLayout.LayoutParams(-1, -2));
            return;
        }

        for (MatchRecord record : filtered) {
            listContainer.addView(recordRow(record), rowParams());
        }
    }

    private View recordRow(MatchRecord record) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(16), dp(12), dp(16), dp(12));
        row.setMinimumHeight(dp(84));
        row.setBackground(theme.interactiveBackground(this, theme.surfaceLow, 14, 1, theme.outlineVariant));

        LinearLayout infoCol = new LinearLayout(this);
        infoCol.setOrientation(LinearLayout.VERTICAL);

        String titleText = UiTheme.playerSymbol(record.playerOneColor) + " " + record.playerOneName
            + "   vs   " + UiTheme.playerSymbol(record.playerTwoColor) + " " + record.playerTwoName;
        TextView names = text(titleText, 20, theme.onSurface);
        names.setTypeface(Typeface.DEFAULT_BOLD);
        names.setSingleLine(true);
        names.setEllipsize(TextUtils.TruncateAt.END);

        String date = DateFormat.format("dd.MM.yyyy  HH:mm", new Date(record.finishedAt)).toString();
        String state = record.finished ? "✓ FINISHED" : "◷ UNFINISHED";
        String duration = record.durationMs > 0 ? "  |  " + formatDuration(record.durationMs) : "";
        TextView meta = text(
            record.playerOneSets + " : " + record.playerTwoSets + "  |  " + state + duration + "  |  " + date,
            16,
            theme.onSurfaceVariant
        );
        UiTheme.applyTabularNumbers(meta);
        meta.setPadding(0, dp(4), 0, 0);

        infoCol.addView(names);
        infoCol.addView(meta);
        row.addView(infoCol, new LinearLayout.LayoutParams(0, -2, 1f));

        // 2-tap direct Resume button on unfinished matches!
        if (!record.finished) {
            Button resumeBtn = theme.primaryButton(this, "Resume match");
            LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(-2, dp(48));
            rlp.leftMargin = dp(8);
            resumeBtn.setOnClickListener(v -> {
                Intent intent = new Intent(this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                intent.putExtra(MatchDetailActivity.EXTRA_RECORD, toJson(record));
                startActivity(intent);
                finish();
            });
            row.addView(resumeBtn, rlp);
        }

        Button deleteBtn = theme.dangerButton(this, "Delete match");
        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(-2, dp(48));
        dlp.leftMargin = dp(8);
        deleteBtn.setOnClickListener(v ->
            new AlertDialog.Builder(this)
                .setTitle("Delete this match?")
                .setMessage("The app will delete the match " + record.playerOneName + " vs " + record.playerTwoName + " permanently.")
                .setNegativeButton("Cancel action", null)
                .setPositiveButton("Delete match", (d, w) -> {
                    MatchHistoryStore.remove(this, record.id);
                    updateChipStyles();
                    populateList();
                })
                .show()
        );
        row.addView(deleteBtn, dlp);

        row.setOnClickListener(v ->
            startActivity(new Intent(this, MatchDetailActivity.class).putExtra(MatchDetailActivity.EXTRA_RECORD, toJson(record)))
        );
        return row;
    }

    private LinearLayout.LayoutParams rowParams() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.setMargins(0, dp(4), 0, dp(4));
        return p;
    }

    private String toJson(MatchRecord record) {
        try { return record.toJson().toString(); } catch (Exception e) { return ""; }
    }

    private TextView text(String value, int size, int color) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        return view;
    }

    private String formatDuration(long ms) {
        long s = ms / 1000;
        return String.format(Locale.US, "%02d:%02d:%02d", s / 3600, (s / 60) % 60, s % 60);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
