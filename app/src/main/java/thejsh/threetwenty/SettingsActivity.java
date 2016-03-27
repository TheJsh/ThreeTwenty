package thejsh.threetwenty;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
import android.util.Log;

import thejsh.threetwenty.MainActivity;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class SettingsActivity extends AppCompatPreferenceActivity {
    /**
     * A preference value change listener that updates the preference's summary
     * to reflect its new value.
     */
    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            String stringValue = value.toString();
            Log.d("Changed preference", stringValue);
            return true;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Load preferences
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean use_amoled = preferences.getBoolean("amoled_key", false);
        setTheme(use_amoled ? R.style.AppThemeAmoled : R.style.AppTheme);

        super.onCreate(savedInstanceState);
        setupActionBar();

        addPreferencesFromResource(R.xml.pref_general);
    }

    @Override
    protected void onStop() {
        Log.d("onStop() called", ".");
        super.onStop();
    }

    @Override
    protected void onPause() {
        Log.d("onPause() called", ".");
        Intent intent = new Intent("settings_menu_closed_reset_animations");
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        super.onPause();
    }

    /**
     * Set up the {@link android.app.ActionBar}, if the API is available.
     */
    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onIsMultiPane() {
        return isXLargeTablet(this);
    }

    /**
     * Helper method to determine if the device has an extra-large screen. For
     * example, 10" tablets are extra-large.
     */
    private static boolean isXLargeTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }

}