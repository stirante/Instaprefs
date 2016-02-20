package com.stirante.instaprefs;

import android.content.Intent;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.v7.app.AppCompatActivity;

import java.io.File;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction().replace(android.R.id.content, new SettingsFragment()).commit();
    }

    @Override
    protected void onPause() {
        super.onPause();
        File prefsDir = new File(getApplicationInfo().dataDir, "shared_prefs");
        File prefsFile = new File(prefsDir, getPackageName() + "_preferences.xml");
        if (prefsFile.exists()) {
            prefsFile.setReadable(true, false);
        }
    }

    public static class SettingsFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.fragment_preferences);
            findPreference("disable_tags").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    findPreference("manage_tags").setEnabled(((boolean) newValue));
                    return true;
                }
            });
            findPreference("manage_tags").setEnabled(((CheckBoxPreference) findPreference("disable_tags")).isChecked());
            findPreference("manage_tags").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    if (!preference.isEnabled()) return false;
                    Intent intent = new Intent(getActivity(), TagsActivity.class);
                    startActivity(intent);
                    return true;
                }
            });
        }
    }

}
