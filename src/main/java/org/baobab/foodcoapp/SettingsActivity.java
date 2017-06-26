
package org.baobab.foodcoapp;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.Button;
import android.widget.NumberPicker;

import org.baobab.foodcoapp.io.BackupExport;
import org.baobab.foodcoapp.io.Report;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


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
        findPreference("reports").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                exportReports(SettingsActivity.this);
                return false;
            }
        });
        findPreference("jahresabschluss").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                jahresabschluss(SettingsActivity.this);
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

    private void exportReports(final SettingsActivity ctx) {
        String mail = PreferenceManager.getDefaultSharedPreferences(this)
                .getString("export_email", "");
        final Intent intent = new Intent(Intent.ACTION_SEND, Uri.parse("mailto:" + mail));
        String date = new SimpleDateFormat("yyyy_MM_dd--HH_mm", Locale.GERMAN).format(new Date());
        intent.putExtra(Intent.EXTRA_EMAIL, new String[] {mail});
        intent.putExtra(Intent.EXTRA_TEXT, "FoodCoApp Berichte " + date);
        intent.putExtra(Intent.EXTRA_SUBJECT, "FoodCoApp Berichte" + date);
        intent.setType("application/zip");
        final ProgressDialog dialog = new ProgressDialog(this);
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        dialog.setMessage("generating reports Stand " + date);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setIndeterminate(true);
        dialog.show();
        new AsyncTask<String, String, File>() {

            @Override
            protected File doInBackground(String... params) {
                return BackupExport.createReports(ctx, params[0]);
            }

            @Override
            protected void onPostExecute(File export) {
                intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(export));
                Intent chooser = Intent.createChooser(intent, "Berichte Ex(el)port");
                startActivity(chooser);
                if (dialog != null && dialog.isShowing()) {
                    dialog.dismiss();
                }
            }
        }.execute(date);
    }

    private void jahresabschluss(SettingsActivity settingsActivity) {
        final Dialog d = new Dialog(this);
        d.setTitle(R.string.jahresabschluss);
        d.setContentView(R.layout.dialog_year_pick);
        Button set = (Button) d.findViewById(R.id.button1);
        Button cancel = (Button) d.findViewById(R.id.button2);
        final NumberPicker nopicker = (NumberPicker) d.findViewById(R.id.numberPicker1);

        nopicker.setMinValue(2015);
        nopicker.setMaxValue(2017);
        nopicker.setWrapSelectorWheel(false);
        nopicker.setValue(2016);
        nopicker.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);

        set.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v) {
                d.dismiss();
                final int year = nopicker.getValue();
                final ProgressDialog dialog = new ProgressDialog(SettingsActivity.this);
                dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                dialog.setMessage("rechne im Kreis alles, was alles " + year + " so alles überall passiert ist...");
                dialog.setCanceledOnTouchOutside(false);
                dialog.setIndeterminate(true);
                dialog.show();
                new AsyncTask<String, String, File>() {

                    @Override
                    protected File doInBackground(String... params) {
                        return new Report(SettingsActivity.this, year).getZip();

                    }

                    @Override
                    protected void onPostExecute(File export) {
                        String mail = PreferenceManager.getDefaultSharedPreferences(SettingsActivity.this)
                                .getString("export_email", "");
                        final Intent intent = new Intent(Intent.ACTION_SEND, Uri.parse("mailto:" + mail));
                        intent.putExtra(Intent.EXTRA_EMAIL, new String[] {mail});
                        intent.putExtra(Intent.EXTRA_TEXT, "Jahresabschluss " + year);
                        intent.putExtra(Intent.EXTRA_SUBJECT, "Jahresabschluss" + year);
                        intent.setType("application/zip");
                        intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(export));
                        Intent chooser = Intent.createChooser(intent, "Jahresabschluss Ex(el)port");
                        startActivity(chooser);
                        if (dialog != null && dialog.isShowing()) {
                            dialog.dismiss();
                        }
                    }
                }.execute();
            }
        });
        cancel.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v) {
                d.dismiss();
            }
        });
        d.show();
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
