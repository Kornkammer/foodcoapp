package org.baobab.foodcoapp;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

import org.baobab.foodcoapp.fragments.TransactionListFragment;


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
                account.moveToFirst();
                if (account.getString(4).equals("aktiva")) {
                    if (getIntent().getData().getQueryParameter("credit") != null) {
                        title = " Abgänge:  - ";
                    } else if (getIntent().getData().getQueryParameter("debit") != null) {
                        title = " Zugänge:  + ";
                    }
                }
                getSupportActionBar().setTitle(account.getString(1) + title +
                        String.format("%.2f", Math.abs(account.getFloat(3))));
            } else {
                getSupportActionBar().setTitle("Umsätze " );
            }

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
