package org.baobab.foodcoapp;

import android.app.ProgressDialog;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import au.com.bytecode.opencsv.CSVReader;


public class TransactionsActivity extends AppCompatActivity {

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
        } else if (getIntent().getDataString().contains("/accounts")) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, TransactionListFragment
                            .newInstance(getIntent().getData()))
                    .commit();
            Cursor account = getContentResolver().query(
                    Uri.parse("content://org.baobab.foodcoapp/accounts"),
                    null, "guid IS '" +
                            getIntent().getData().getPathSegments().get(1) +
                            "'", null, null);
            account.moveToFirst();
            getSupportActionBar().setTitle(account.getString(1) + " Guthaben: "
                    + String.format("%.2f", - account.getFloat(4)));
        } else if (getIntent().getScheme().equals("content")) {
            final ProgressDialog dialog = new ProgressDialog(this);
            dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            dialog.setMessage("Importing. Please wait...");
            dialog.setIndeterminate(true);
            dialog.setCanceledOnTouchOutside(false);
            dialog.show();
            new AsyncTask<Uri, String, String>() {

                @Override
                protected String doInBackground(Uri... uri) {
                    return Import.file(TransactionsActivity.this, uri[0]);
                }

                @Override
                protected void onPostExecute(String message) {
                    Toast.makeText(TransactionsActivity.this, message, Toast.LENGTH_LONG).show();
                    dialog.dismiss();
                }
            }.execute(getIntent().getData());
        }
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
