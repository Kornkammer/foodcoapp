package org.baobab.foodcoapp;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;


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
