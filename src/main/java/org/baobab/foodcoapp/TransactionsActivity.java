package org.baobab.foodcoapp;

import android.app.ProgressDialog;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
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
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.container, TransactionListFragment.newInstance(
                        Uri.parse("content://org.baobab.foodcoapp/transactions")))
                .commit();
        if (getIntent().getScheme().equals("content")) {
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

}
