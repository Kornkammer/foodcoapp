package org.baobab.foodcoapp;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import org.baobab.foodcoapp.fragments.TransactionListFragment;
import org.baobab.foodcoapp.io.BackupExport;
import org.baobab.foodcoapp.io.Report;

import java.io.File;
import java.net.URLEncoder;

import static org.baobab.foodcoapp.io.BackupExport.YEAR;
import static org.baobab.foodcoapp.io.GlsImport.AUTHORITY;


public class JahresabschlussActivity extends AppCompatActivity {

    private Report report;
    private int year;
    private TextView title;
    private AsyncTask<String, String, Report> task;
    private Handler handler;
    private Runnable work;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_import);
        getSupportActionBar().setTitle(R.string.jahresabschluss);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(false);
            actionBar.setDisplayShowCustomEnabled(true);
            View customView = getLayoutInflater().inflate(R.layout.actionbar_title, null);
            title = (TextView) customView.findViewById(R.id.actionbarTitle);
            title.setFocusable(true);
            title.setClickable(true);
            title.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showYearPicker();
                }
            });
            title.setText(R.string.jahresabschluss);
            actionBar.setCustomView(customView);
        }
        showYearPicker();
    }

    private void showYearPicker() {
        final Dialog d = new Dialog(this);
        d.setTitle(R.string.jahresabschluss);
        d.setContentView(R.layout.dialog_year_pick);
        Button set = (Button) d.findViewById(R.id.button1);
        Button cancel = (Button) d.findViewById(R.id.button2);
        final NumberPicker nopicker = (NumberPicker) d.findViewById(R.id.numberPicker1);

        Cursor all = getContentResolver().query(
                Uri.parse("content://org.baobab.foodcoapp/transactions"),
                null, null, null, "transactions.start");
        all.moveToFirst();
        int first = Integer.parseInt(YEAR.format(all.getLong(2)));
        all.moveToLast();
        int last = Integer.parseInt(YEAR.format(all.getLong(2)));

        nopicker.setMinValue(first);
        nopicker.setMaxValue(last);
        if (year == 0) {
            nopicker.setValue(first);
        } else {
            nopicker.setValue(year);
        }
        nopicker.setWrapSelectorWheel(false);
        nopicker.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);

        set.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                d.dismiss();
                pickYear(nopicker.getValue());
            }
        });
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                d.dismiss();
            }
        });
        d.show();
    }

    private void pickYear(int y) {
        if (year != y) {
            year = y;
            Cursor s = getContentResolver().query(Uri.parse("content://" + AUTHORITY +
                            "/sessions?comment=" + URLEncoder.encode("Jahr " + year)),
                    null, null, null, null);
            if (s.getCount() > 0) {
                s.moveToLast();
                Uri session = Uri.parse("content://" + AUTHORITY + "/sessions/" + s.getLong(0));
                showTransactions(session);
                if (s.getString(4).contains("(final)")) {
                    title.setText("Jahr " + year + " (final)");
                    findViewById(R.id.ok).setVisibility(View.GONE);
                } else {
                    title.setText("Jahr " + year);
                    displayImportButton(session);
                }

            } else {
                generate(year);
                title.setText("Jahr " + year);
            }
        }
    }

    private void generate(final int year) {
        final ProgressDialog dialog = new ProgressDialog(JahresabschlussActivity.this);
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        dialog.setMessage("wird berechnet...\n\n\n");
        dialog.setCanceledOnTouchOutside(false);
        dialog.setIndeterminate(true);
        dialog.show();

        handler = new Handler();
        work = new Runnable() {

            @Override
            public void run() {
                ((Vibrator) getSystemService(Context.VIBRATOR_SERVICE))
                        .vibrate((int) (Math.random() * 150));
                handler.postDelayed(this, (long) (Math.random() * 250));
            }
        };
        handler.post(work);
        task = new AsyncTask<String, String, Report>() {

            @Override
            protected Report doInBackground(String... params) {
                return new Report(JahresabschlussActivity.this, year);
            }

            @Override
            protected void onPostExecute(Report r) {
                handler.removeCallbacks(work);
                if (dialog != null && dialog.isShowing()) {
                    dialog.dismiss();
                }
                report = r;
                showTransactions(report.getSession());
            }
        };
        task.execute();
        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                if (task != null) task.cancel(false);
                if (handler != null) handler.removeCallbacks(work);
            }
        });
    }


    private void showTransactions(Uri session) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.container, TransactionListFragment.newInstance(
                        session.buildUpon().appendEncodedPath(
                                "accounts/Jahresabschluss/transactions").build())).commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.jahresabschluss, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.accounts:
                startActivity(new Intent(this, BalanceActivity.class)
                        .putExtra("year", year));
                break;
            case R.id.refresh:
                generate(year);
                break;
            case R.id.export:
                if (year != 0) {
                    exportZip(BackupExport.file(year + ".zip"), year);
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void exportZip(File export, int year) {
        String mail = PreferenceManager.getDefaultSharedPreferences(JahresabschlussActivity.this)
                .getString("export_email", "");
        final Intent intent = new Intent(Intent.ACTION_SEND, Uri.parse("mailto:" + mail));
        intent.putExtra(Intent.EXTRA_EMAIL, new String[] {mail});
        intent.putExtra(Intent.EXTRA_TEXT, "Jahresabschluss " + year);
        intent.putExtra(Intent.EXTRA_SUBJECT, "Jahresabschluss" + year);
        intent.setType("application/zip");
        intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(export));
        Intent chooser = Intent.createChooser(intent, "Jahresabschluss Ex(el)port");
        startActivity(chooser);
    }

    private void displayImportButton(final Uri session) {
        TextView button = (TextView) findViewById(R.id.ok);
        button.setText(" Jahr " + year + " abschließen");
        button.setVisibility(View.VISIBLE);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(JahresabschlussActivity.this)
                        .setTitle("Jahr " + year + " wirklich abschließen?")
                        .setMessage("\n\nAlle Transaktionen des Jahres " + year +
                                        " werden unwiederbringlich gelöscht " +
                                        "und mit dieser Zusammenfassung hier ersetzt\n")
                        .setNegativeButton("(noch) nicht", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                        .setPositiveButton("JA, ich will", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                abschluss(session);
                            }
                        })
                        .show();
            }
        });
    }

    private void abschluss(final Uri session) {
        final ProgressDialog dialog = new ProgressDialog(JahresabschlussActivity.this);
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        dialog.setMessage("Jahresabschluss..");
        dialog.setCanceledOnTouchOutside(false);
        dialog.setIndeterminate(true);
        dialog.show();
        new AsyncTask<String, String, Boolean>() {

            @Override
            protected Boolean doInBackground(String... params) {
                ContentValues cv = new ContentValues();
                cv.put("status", "final");
                cv.put("session_log", "Jahr " + year + " (final)");
                int r = getContentResolver().update(session, cv, null, null);
                if (r > 0) {
                    getContentResolver().delete(Uri.parse("content://" + AUTHORITY +
                            "/transactions?" + BackupExport.getTimeWindowQuery(year)), null, null);
                    return true;
                } else {
                    return false;
                }
            }

            @Override
            protected void onPostExecute(Boolean success) {
                ((Vibrator) getSystemService(Context.VIBRATOR_SERVICE)).vibrate(100);
                if (success) {
                    MediaPlayer.create(JahresabschlussActivity.this, R.raw.chaching).start();
                    Toast.makeText(JahresabschlussActivity.this, "Abgeschlossen :-)", Toast.LENGTH_SHORT).show();
                    findViewById(R.id.ok).setVisibility(View.GONE);
                    title.setText("Jahr " + year + "(final)");
                } else {
                    MediaPlayer.create(JahresabschlussActivity.this, R.raw.error_2).start();
                    Toast.makeText(JahresabschlussActivity.this, "Error :/", Toast.LENGTH_SHORT).show();
                }
                if (dialog != null && dialog.isShowing()) {
                    dialog.dismiss();
                }

            }
        }.execute();


    }
}

