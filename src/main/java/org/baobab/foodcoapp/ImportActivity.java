package org.baobab.foodcoapp;

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
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import au.com.bytecode.opencsv.CSVReader;


public class ImportActivity extends AppCompatActivity {

    static final String TAG = PosActivity.TAG;
    Importer importer = null;
    String err = null;

    public interface Importer {
        int read(CSVReader csv)  throws IOException;
        Uri getSession();
        String getMsg();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_import);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        if (getIntent().getScheme().equals("file") ||
                    getIntent().getScheme().equals("content")) {
            getSupportActionBar().setTitle("Import " + getIntent().getData().getLastPathSegment());
            final ProgressDialog dialog = new ProgressDialog(this);
            dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            dialog.setMessage("Reading. Please wait...");
            dialog.setCanceledOnTouchOutside(false);
            dialog.setIndeterminate(true);
            dialog.show();
            new AsyncTask<String, String, Integer>() {

                @Override
                protected Integer doInBackground(String... args) {
                    try {
                        InputStream is = getContentResolver().openInputStream(getIntent().getData());
                        CSVReader csv = new CSVReader(new BufferedReader(new InputStreamReader(is, "utf-8")), ';');

                        String[] line = csv.readNext();
                        if (line.length == 12) {
                            importer = new BnnImport(ImportActivity.this);
                        } else if (line.length == 22) {
                            importer = new GlsImport(ImportActivity.this);
                        } else {
                            Log.d(TAG, "No idea how to import this file (line length " + line.length + ")");
                            err = "No idea how to import this file (line length " + line.length + ")";
                            return 0;
                        }
                        return importer.read(csv);
                    } catch (Exception e) {
                        Log.e(TAG, "Error " + e);
                        err = "Error " + e;
                        e.printStackTrace();
                        return 0;
                    }
                }

                @Override
                protected void onPostExecute(Integer readCount) {
                    dialog.dismiss();
                    if (importer == null) {
                        ((Vibrator) getSystemService(Context.VIBRATOR_SERVICE)).vibrate(500);
                        MediaPlayer.create(ImportActivity.this, R.raw.error_3).start();
                        showMsg("Unbekanntes Dateiformat");
                        return;
                    }
                    if (err != null) {
                        MediaPlayer.create(ImportActivity.this, R.raw.error_4).start();
                        ((Vibrator) getSystemService(Context.VIBRATOR_SERVICE)).vibrate(500);
                        showMsg(err);
                        return;
                    }
                    if (importer.getMsg() != null && !importer.getMsg().equals("")) {
                        ((Vibrator) getSystemService(Context.VIBRATOR_SERVICE)).vibrate(200);
                        showMsg(importer.getMsg());
                    }
                    if (importer.getSession() != null) {
                        getSupportFragmentManager().beginTransaction()
                                .replace(R.id.container, TransactionListFragment.newInstance(
                                                importer.getSession().buildUpon().appendPath(
                                                        "transactions").build())).commit();
                        displayImportButton(readCount);
                    }
                }
            }.execute();
        }
    }

    private void displayImportButton(final Integer readCount) {
        TextView button = (TextView) findViewById(R.id.ok);
        button.setText(readCount + " Transaktionen\nimportieren");
        button.setVisibility(View.VISIBLE);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MediaPlayer.create(ImportActivity.this, R.raw.chaching).start();
                final ProgressDialog dialog = new ProgressDialog(ImportActivity.this);
                dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                dialog.setMessage("Importing. Please wait...");
                dialog.setCanceledOnTouchOutside(false);
                dialog.setIndeterminate(true);
                dialog.show();
                new AsyncTask<Void, Void, Integer>() {
                    @Override
                    protected Integer doInBackground(Void... params) {
                        ContentValues cv = new ContentValues();
                        cv.put("status", "final");
                        cv.put("session_log", importer.getMsg());
                        return getContentResolver().update(importer.getSession(), cv, null, null);
                    }

                    @Override
                    protected void onPostExecute(Integer stored) {
                        dialog.dismiss();
                        if (stored == readCount) {
                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    startActivity(new Intent(ImportActivity.this, AccountActivity.class));
                                    finish();
                                }
                            },  700);
                        } else {
                            ((Vibrator) getSystemService(Context.VIBRATOR_SERVICE)).vibrate(100);
                            new AlertDialog.Builder(ImportActivity.this)
                                    .setMessage(stored + " Transaktionen importiert\n" +
                                            " (von " + readCount + ")" + "\n")
                                    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            startActivity(new Intent(ImportActivity.this, AccountActivity.class));
                                            finish();
                                        }
                                    })
                                    .show();
                            MediaPlayer.create(ImportActivity.this, R.raw.error_2).start();
                        }
                    }
                }.execute();
            }
        });
    }

    private void showMsg(String msg) {
        new AlertDialog.Builder(ImportActivity.this)
                .setMessage(msg + "\n").show();
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
        }
        return super.onOptionsItemSelected(item);
    }
}