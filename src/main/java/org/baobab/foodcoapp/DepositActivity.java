package org.baobab.foodcoapp;

import android.annotation.TargetApi;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;

import java.text.NumberFormat;
import java.text.ParseException;


public class DepositActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_deposit);
        setTitle("Guthaben aufladen");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        findViewById(R.id.ok).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(new Intent(DepositActivity.this, LegitimateActivity.class), 0);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            float amount;
            try {
                amount = NumberFormat.getInstance().parse(
                        ((EditText) findViewById(R.id.amount)).getText().toString()).floatValue();
            } catch (ParseException e) {
                Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                return;
            }
            ContentValues t = new ContentValues();
            t.put("status", "final");
            t.put("comment", ((EditText) findViewById(R.id.comment)).getText().toString());
            t.put("stop", System.currentTimeMillis());
            Uri transaction = getContentResolver().insert(Uri.parse(
                    "content://org.baobab.foodcoapp/transactions"), t);
            ContentValues b = new ContentValues();
            b.put("account_guid", "kasse");
            b.put("quantity", - amount);
            getContentResolver().insert(transaction.buildUpon()
                    .appendEncodedPath("products/1").build(), b);
            b = new ContentValues();
            b.put("account_guid", data.getStringExtra("guid"));
            b.put("quantity", amount);
            getContentResolver().insert(transaction.buildUpon()
                    .appendEncodedPath("products/2").build(), b);
            Toast.makeText(this, "Guthaben aufgeladen \n" + amount + " in die Kasse!", Toast.LENGTH_LONG).show();
            finish();
            startActivity(new Intent(this, TransactionsActivity.class)
                    .setData(Uri.parse("content://org.baobab.foodcoapp/accounts/" +
                            data.getStringExtra("guid") + "/transactions")));
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        fullscreen();
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    protected void fullscreen() {
        if (Build.VERSION.SDK_INT < 16) {
            getWindow().setFlags(
                    WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_FULLSCREEN);
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
