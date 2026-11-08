package pl.maciek.squashscoreboard;

import android.content.Intent;
import android.os.Bundle;

/**
 * Compatibility entry point that delegates directly to the unified 4-page swipeable
 * {@link MatchDetailActivity} so chart views are never duplicated across activities.
 */
public class ChartFullscreenActivity extends MatchDetailActivity {
    public static final String EXTRA_RECORD = "record";
    public static final String EXTRA_CHART = "chart";

    @Override
    public void onCreate(Bundle state) {
        Intent intent = getIntent();
        if (intent != null && !intent.hasExtra(EXTRA_INITIAL_TAB)) {
            intent.putExtra(EXTRA_INITIAL_TAB, intent.getIntExtra(EXTRA_CHART, TAB_TIMELINE));
        }
        super.onCreate(state);
    }
}
