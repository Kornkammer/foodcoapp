
package org.baobab.foodcoapp;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;


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
                restoreLastTransaction(SettingsActivity.this);
                return false;
            }
        });
    }

    public static void restoreLastTransaction(Context ctx) {
        Cursor c = ctx.getContentResolver().query(
                Uri.parse("content://org.baobab.foodcoapp/transactions"),
                null, "transactions.status IS NOT NULL", null, null);
        c.moveToLast();
        ctx.startActivity(new Intent(ctx, AccountActivity.class)
                .setData(Uri.parse("content://org.baobab.foodcoapp/transactions/" + c.getLong(0))));
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

    public static void crashCheck(final Context ctx) {
        if (ctx.getSharedPreferences("crash", MODE_MULTI_PROCESS).contains("crashed")) {
            new AlertDialog.Builder(ctx)
                    .setTitle(R.string.session_restore)
                    .setPositiveButton(R.string.btn_yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ctx.getSharedPreferences("crash", MODE_MULTI_PROCESS).edit().remove("crashed").commit();
                            SettingsActivity.restoreLastTransaction(ctx);
                        }
                    })
                    .setNegativeButton(R.string.btn_no, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ctx.getSharedPreferences("crash", MODE_MULTI_PROCESS).edit().remove("crashed").commit();
                        }
                    }).show();
        }
    }

}
