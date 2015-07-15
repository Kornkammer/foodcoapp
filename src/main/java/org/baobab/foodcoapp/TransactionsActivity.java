package org.baobab.foodcoapp;

import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;


public class TransactionsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transactions);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.container, TransactionListFragment.newInstance(
                        Uri.parse("content://org.baobab.foodcoapp/transactions")))
                .commit();
    }

}
