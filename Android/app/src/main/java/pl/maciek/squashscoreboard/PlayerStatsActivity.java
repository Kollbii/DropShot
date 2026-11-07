package pl.maciek.squashscoreboard;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

/**
 * Forwarder activity to maintain backward compatibility with external/manifest intents.
 * Redirects immediately to {@link HistoryActivity} with the Players tab selected.
 */
public class PlayerStatsActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = new Intent(this, HistoryActivity.class);
        intent.putExtra(HistoryActivity.EXTRA_ACTIVE_TAB, UiTheme.TAB_PLAYERS);
        intent.addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        overridePendingTransition(0, 0);
        finish();
    }
}
