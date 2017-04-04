package org.baobab.foodcoapp;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Toast;

import org.baobab.foodcoapp.io.BackupExport;
import org.baobab.foodcoapp.util.Trace;
import org.baobab.foodcoapp.view.TransactionView;
import org.baobab.foodcoapp.util.Trace.Txn;
import org.baobab.foodcoapp.util.Trace.Prod;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;


public class TraceActivity extends AppCompatActivity {

    private Txn txn;
    private TransactionView txnView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transactions);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        if (getIntent().getDataString().contains("/transactions")) {

            getSupportActionBar().setTitle("Trace " + getIntent().getData().getQueryParameter("title"));

            txnView = new org.baobab.foodcoapp.view.TransactionView(this);
            txnView.showImages(true);
            txnView.headersClickable(false);
            txnView.setColumnWidth(R.dimen.column_small);
            txnView.headersClickable(false);
            txnView.showHeaders(true);

            txnView.setOnTitleClick(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    System.out.println("HUi " + v.getId() + " pos " + v.getTag());
                    Prod p = txn.lookup.get(v.getId());
                    Toast.makeText(TraceActivity.this, p.title, Toast.LENGTH_SHORT).show();
                }
            });
            txnView.setOnTitleLongClick(new View.OnLongClickListener() {

                @Override
                public boolean onLongClick(View v) {
                    final Prod p = txn.lookup.get(v.getId());
                    String[] menu = new String[]{"Kontoumsätze", "Trace..."};
                    new AlertDialog.Builder(TraceActivity.this)
                            .setItems(menu, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    switch (which) {
                                        case 0:
                                            startActivity(new Intent(TraceActivity.this, BrowseActivity.class)
                                                    .setData(Uri.parse("content://org.baobab.foodcoapp/transactions")
                                                            .buildUpon().appendQueryParameter("title", p.title)
                                                            .appendQueryParameter("price",
                                                                    String.format(Locale.ENGLISH, "%.2f", p.price)).build()));
                                            break;
                                        case 1:
                                            search(p.title, p.price + "");
                                            break;
                                    }
                                }
                            }).show();
                    return true;
                }
            });
            search(getIntent().getData().getQueryParameter("title"),
                    getIntent().getData().getQueryParameter("price"));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT);
            lp.bottomMargin = 96;
            ScrollView sv = new ScrollView(this);
            FrameLayout fl = new FrameLayout(this);
            sv.addView(txnView, lp);
            ((ViewGroup) findViewById(R.id.container)).addView(sv, lp);
        }
    }

    void search(String title, String price) {
        final ProgressDialog progress = new ProgressDialog(this);
        progress.setTitle("HARDCORE TRACING ACTION!");
        progress.setMessage("suche nach allem(!), was mit " +
                getIntent().getData().getQueryParameter("title") +
                " zu tun hat...");
        progress.show();

        new AsyncTask<String, String, Txn>() {

            @Override
            protected Txn doInBackground(String... params) {
                Trace t = new Trace(TraceActivity.this, params[0], Float.valueOf(params[1]), "", -1);
                return t.split;
            }

            @Override
            protected void onPostExecute(final Txn t) {
                txnView.populate(t.toCursor());
                progress.dismiss();
                txn = t;
            }
        }.execute(title, price);
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
                    final Intent intent = new Intent(Intent.ACTION_SEND, Uri.parse("mailto:" + mail));
                    String date = new SimpleDateFormat("yyyy_MM_dd--HH_mm", Locale.GERMAN).format(new Date());
                    intent.putExtra(Intent.EXTRA_EMAIL, new String[] {mail});
                    intent.putExtra(Intent.EXTRA_TEXT, "FoodCoApp Kontoauszug " + date);
                    intent.putExtra(Intent.EXTRA_SUBJECT, "Kontoauszug " + date);
                    intent.setType("application/zip");
                    final ProgressDialog dialog = new ProgressDialog(this);
                    dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                    dialog.setMessage("Export Trace " + getIntent().getData()
                            .getQueryParameter("title") + " - Stand " + date);
                    dialog.setCanceledOnTouchOutside(false);
                    dialog.setIndeterminate(true);
                    dialog.show();
                    new AsyncTask<String, String, File>() {

                        @Override
                        protected File doInBackground(String... params) {
                            return BackupExport.create(TraceActivity.this, params[0], params[1]);
                        }

                        @Override
                        protected void onPostExecute(File export) {
                            intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(export));
                            Intent chooser = Intent.createChooser(intent, "Kontoauszug ex(el)portieren");
                            startActivity(chooser);
                            dialog.dismiss();
                        }
                    }.execute("_" + date);
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}
