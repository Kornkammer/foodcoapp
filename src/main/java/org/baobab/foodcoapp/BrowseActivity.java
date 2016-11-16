package org.baobab.foodcoapp;

import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;

import org.baobab.foodcoapp.fragments.TransactionListFragment;
import org.baobab.foodcoapp.io.BackupExport;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


public class BrowseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transactions);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        if (getIntent().getData() == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, TransactionListFragment.newInstance(
                            Uri.parse("content://org.baobab.foodcoapp/transactions")))
                    .commit();
            getSupportActionBar().setTitle("Transaction Log");
        } else if (getIntent().getDataString().contains("/transactions")) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, TransactionListFragment
                            .newInstance(getIntent().getData()))
                    .commit();
            if (getIntent().getDataString().contains("/accounts")) {
                String title = " Umsätze:   ";
                String q = "";
                if (getIntent().getData().getQueryParameter("credit") != null) {
                    q = "?credit=true";
                    title = " Zugänge:  + ";
                } else if (getIntent().getData().getQueryParameter("debit") != null) {
                    q = "?debit=true";
                    title = " Abgänge:  - ";
                }
                Cursor account = getContentResolver().query(
                        Uri.parse("content://org.baobab.foodcoapp/accounts" + q),
                        null, "guid IS '" + getIntent().getData().getPathSegments().get(1) +
                                "'", null, null);
                float balance = 0;
                account.moveToFirst();
                if (account.getString(4).equals("aktiva")) {
                    if (getIntent().getData().getQueryParameter("credit") != null) {
                        title = " Abgänge:  - ";
                        balance = account.getFloat(3);
                    } else if (getIntent().getData().getQueryParameter("debit") != null) {
                        title = " Zugänge:  + ";
                        balance = - account.getFloat(3);
                    }
                }
                getSupportActionBar().setTitle(account.getString(1) + title +
                        String.format("%.2f", balance));
            } else {
                getSupportActionBar().setTitle("Umsätze " );
            }

        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.dashboard, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
            case R.id.export:
                if (getIntent().getData() != null && getIntent().getDataString().contains("/accounts")) {
                    String mail = "mich@zuhause.de";
                    String guid = getIntent().getData().getPathSegments().get(1);
                    final Intent intent = new Intent(Intent.ACTION_SEND, Uri.parse("mailto:" + mail));
                    String date = new SimpleDateFormat("yyyy_MM_dd--HH_mm", Locale.GERMAN).format(new Date());
                    intent.putExtra(Intent.EXTRA_EMAIL, new String[] {mail});
                    intent.putExtra(Intent.EXTRA_TEXT, "FoodCoApp Kontoauszug " + date);
                    intent.putExtra(Intent.EXTRA_SUBJECT, "Kontoauszug " + date);
                    intent.setType("application/zip");
                    final ProgressDialog dialog = new ProgressDialog(this);
                    dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                    dialog.setMessage("Export Kontoauszug - Stand " + date);
                    dialog.setCanceledOnTouchOutside(false);
                    dialog.setIndeterminate(true);
                    dialog.show();
                    new AsyncTask<String, String, File>() {

                        @Override
                        protected File doInBackground(String... params) {
                            return BackupExport.create(BrowseActivity.this, params[0], params[1]);
                        }

                        @Override
                        protected void onPostExecute(File export) {
                            intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(export));
                            Intent chooser = Intent.createChooser(intent, "Kontoauszug ex(el)portieren");
                            startActivity(chooser);
                            dialog.dismiss();
                        }
                    }.execute(guid + "_" + date, guid);
                }
                break;

        }
        return super.onOptionsItemSelected(item);
    }
}
