// Copyright 2015 The Project Buendia Authors
//
// Licensed under the Apache License, Version 2.0 (the "License"); you may not
// use this file except in compliance with the License.  You may obtain a copy
// of the License at: http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software distrib-
// uted under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
// OR CONDITIONS OF ANY KIND, either express or implied.  See the License for
// specific language governing permissions and limitations under the License.

package org.projectbuendia.client.ui;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v4.app.NavUtils;
import android.view.MenuItem;

import org.projectbuendia.client.App;
import org.projectbuendia.client.R;
import org.projectbuendia.client.models.AppModel;
import org.projectbuendia.client.ui.login.LoginActivity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;
import javax.inject.Inject;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p/>
 * <p>See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class SettingsActivity extends PreferenceActivity {
    /**
     * Controls whether to always show the simplified UI, where settings are
     * arranged in a single list without a left navigation panel.
     */
    private static final boolean ALWAYS_SIMPLE_PREFS = false;
    static final String[] prefKeys = {
        "server",
        "openmrs_user",
        "openmrs_password",
        "openmrs_root_url",
        "package_server_root_url",
        "apk_update_interval_secs",
        "keep_form_instances_locally",
        "xform_update_client_cache",
        "incremental_observation_update",
        "require_wifi"
    };
    static boolean updatingPrefValues = false;

    static final Map<String, EditTextPreference> textPrefs = new HashMap<>();

    /** A listener that performs updates when any preference's value changes. */
    static final Preference.OnPreferenceChangeListener sPrefListener =
        new Preference.OnPreferenceChangeListener() {
            @Override public boolean onPreferenceChange(Preference pref, Object value) {
                updatePrefSummary(pref, value);
                if (updatingPrefValues)
                    return true; // prevent endless recursion

                SharedPreferences prefs =
                    PreferenceManager.getDefaultSharedPreferences(pref.getContext());
                String server = prefs.getString("server", "");
                String str = "" + value;
                try {
                    updatingPrefValues = true;
                    switch (pref.getKey()) {
                        case "server":
                            if (!str.equals("")) {
                                setTextAndSummary(prefs, "openmrs_root_url", "http://" + str + ":9000/openmrs");
                                setTextAndSummary(prefs, "package_server_root_url", "http://" + str + ":9001");
                            }
                            break;
                        case "openmrs_root_url":
                            if (!str.equals("http://" + server + ":9000/openmrs")) {
                                setTextAndSummary(prefs, "server", "");
                            }
                            break;
                        case "package_server_root_url":
                            if (!str.equals("http://" + server + ":9001")) {
                                setTextAndSummary(prefs, "server", "");
                            }
                            break;
                    }
                } finally {
                    updatingPrefValues = false;
                }
                return true;
            }
        };

    @Inject AppModel mAppModel;

    public static void start(Context caller) {
        caller.startActivity(new Intent(caller, SettingsActivity.class));
    }

    private static void setTextAndSummary(SharedPreferences prefs, String key, String value) {
        // Update the preference edit field, if it's currently showing.
        EditTextPreference pref = textPrefs.get(key);
        if (pref != null) {
            pref.setText(value);
            pref.setSummary(value);
        }

        // Update the preference itself, even if the edit field isn't visible.
        prefs.edit().putString(key, value).commit();
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            // This ID represents the Home or Up button. In the case of this
            // activity, the Up button is shown. Use NavUtils to allow users
            // to navigate up one level in the application structure. For
            // more details, see the Navigation pattern on Android Design:
            //
            // http://developer.android.com/design/patterns/navigation.html#up-vs-back
            //
            // TODO: If Settings has multiple levels, Up should navigate up
            // that hierarchy.
            NavUtils.navigateUpFromSameTask(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override public boolean onIsMultiPane() {
        return isXLargeTablet(this) && !isSimplePreferences(this);
    }

    @Override @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void onBuildHeaders(List<Header> target) {
        if (!isSimplePreferences(this)) {
            loadHeadersFromResource(R.xml.pref_headers, target);
        }
    }

    /** When the UI has two panes, this fragment shows just the general settings. */
    public static class GeneralPreferenceFragment extends PreferenceFragment {
        @Override public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_general);
            initPrefs(this);
        }
    }

    /** When the UI has two panes, this fragment shows just the advanced settings. */
    public static class AdvancedPreferenceFragment extends PreferenceFragment {
        @Override public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_advanced);
            initPrefs(this);
        }
    }

    /** When the UI has two panes, this fragment shows just the developer settings. */
    public static class DeveloperPreferenceFragment extends PreferenceFragment {
        @Override public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_developer);
            initPrefs(this);
        }
    }

    /** Sets up all the preferences in a fragment. */
    private static void initPrefs(PreferenceFragment fragment) {
        textPrefs.clear();
        for (String key : prefKeys) {
            initPref(fragment.findPreference(key));
        }
    }

    /** Sets up the listener and summary for a preference. */
    private static void initPref(@Nullable Preference pref) {
        if (pref != null) {
            pref.setOnPreferenceChangeListener(sPrefListener);
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(pref.getContext());
            updatePrefSummary(pref, prefs.getAll().get(pref.getKey()));
            if (pref instanceof EditTextPreference) {
                textPrefs.put(pref.getKey(), (EditTextPreference) pref);
            }
        }
    }

    static void updatePrefSummary(Preference pref, Object value) {
        String str = value.toString();
        switch (pref.getKey()) {
            case "server":
            case "openmrs_user":
            case "openmrs_root_url":
            case "package_server_root_url":
            case "apk_update_interval_secs":
                pref.setSummary(str);
        }
    }

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        App.getInstance().inject(this);
        setupActionBar();
    }

    /** Set up the {@link android.app.ActionBar}, if the API is available. */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void setupActionBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            // Show the Up button in the action bar.
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override protected boolean isValidFragment(String fragmentName) {
        return true;
    }

    @Override protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        setupSimplePreferencesScreen();
    }

    /**
     * Shows the simplified settings UI if the device configuration dictates
     * that a simplified, single-pane UI should be shown.
     */
    private void setupSimplePreferencesScreen() {
        if (!isSimplePreferences(this)) return;

        // The simplified UI uses the old PreferenceActivity API instead of PreferenceFragment.
        addPreferencesFromResource(R.xml.pref_general);
        addPreferencesFromResource(R.xml.pref_advanced);
        addPreferencesFromResource(R.xml.pref_developer);
        initPrefs(this);
    }

    /**
     * Determines whether the simplified settings UI should be shown. This is
     * true if this is forced via {@link #ALWAYS_SIMPLE_PREFS}, or the device
     * doesn't have newer APIs like {@link PreferenceFragment}, or the device
     * doesn't have an extra-large screen. In these cases, a single-pane
     * "simplified" settings UI should be shown.
     */
    private static boolean isSimplePreferences(Context context) {
        return !isXLargeTablet(context);
    }

    /** Sets up all the preferences in an activity. */
    private static void initPrefs(PreferenceActivity activity) {
        textPrefs.clear();
        for (String key : prefKeys) {
            initPref(activity.findPreference(key));
        }
    }

    /** Checks if the screen is extra-large (e.g. a 10" tablet is extra-large). */
    private static boolean isXLargeTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
            & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }

    @Override protected void onPause() {
        super.onPause();
        if (!mAppModel.isFullModelAvailable()) {
            // The database was cleared; go back to the login activity.
            startActivity(new Intent(this, LoginActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK));
        }
    }
}
