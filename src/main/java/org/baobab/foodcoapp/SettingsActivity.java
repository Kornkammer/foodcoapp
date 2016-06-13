
package org.baobab.foodcoapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;


public class SettingsActivity extends PreferenceActivity
        implements OnSharedPreferenceChangeListener {

    private SharedPreferences prefs;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings);
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        setMail();
        findPreference("restore").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Cursor c = getContentResolver().query(
                        Uri.parse("content://org.baobab.foodcoapp/transactions"),
                        null, "transactions.status IS NOT NULL", null, null);
                c.moveToLast();
                startActivity(new Intent(SettingsActivity.this, AccountActivity.class)
                        .setData(Uri.parse("content://org.baobab.foodcoapp/transactions/" + c.getLong(0))));
                return false;
            }
        });
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        if (key.equals("export_email")) {
            setMail();
        }
    }

    private void setMail() {
        findPreference("export_email").setTitle(
                getString(R.string.prefs_email_title) + ": " +
                        prefs.getString("export_email", ""));
    }

}
