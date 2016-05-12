package org.baobab.foodcoapp;

import android.annotation.TargetApi;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Toast;


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
            final float amount;
            try {
                amount = Float.parseFloat(((EditText) findViewById(R.id.amount))
                        .getText().toString().replace(",", "."));
            } catch (Exception e) {
                Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                return;
            }
            final String title;
            final String guid = data.getStringExtra("guid");
            final String name = data.getStringExtra("name");
            AlertDialog.Builder alert = new AlertDialog.Builder(this);
            if (((RadioButton) findViewById(R.id.bar)).isChecked()) {
                title = "Bar " + data.getStringExtra("name");
                alert.setMessage(String.format(getString(R.string.msg_deposit_cash),
                        String.format("%.2f", amount), guid, name));
            } else {
                title = "Bank " + data.getStringExtra("name");
                alert.setMessage(String.format(getString(R.string.msg_deposit_bank),
                        String.format("%.2f", amount), guid, name));
            }
            alert.setNegativeButton(R.string.btn_no, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    ((Vibrator) getSystemService(VIBRATOR_SERVICE)).vibrate(2000);
                }
            })
            .setPositiveButton(R.string.btn_yes, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    store(guid, amount, title);
                }
            }).show();

        }
    }

    private void store(final String guid, float amount, String title) {
        if (amount > 1000) {
            Toast.makeText(this, "So viel gibts ja gar nicht!", Toast.LENGTH_LONG).show();
            return;
        }
        ((Vibrator) getSystemService(VIBRATOR_SERVICE)).vibrate(200);
        MediaPlayer.create(this, R.raw.chaching).start();
        ContentValues t = new ContentValues();
        t.put("comment", "Einzahlung:\n" + title + "\n" +
                ((EditText) findViewById(R.id.comment)).getText().toString());
        Uri transaction = getContentResolver().insert(Uri.parse(
                "content://org.baobab.foodcoapp/transactions"), t);
        ContentValues b = new ContentValues();
        b.put("product_id", 3);
        b.put("account_guid", "forderungen");
        b.put("title", title);
        b.put("quantity", 1);
        b.put("price", amount);
        getContentResolver().insert(transaction.buildUpon()
                .appendEncodedPath("products").build(), b);
        ContentValues g = new ContentValues();
        g.put("product_id", 2);
        g.put("title", "Korns");
        g.put("account_guid", guid);
        g.put("quantity", - amount);
        g.put("price", 1);
        getContentResolver().insert(transaction.buildUpon()
                .appendEncodedPath("products").build(), g);
        ContentValues f = new ContentValues();
        f.put("status", "final");
        int r = getContentResolver().update(transaction, f, null, null);
        if (r == 0) {
            Toast.makeText(this, "deposit error - invalid txn", Toast.LENGTH_LONG).show();
            Log.e(AccountActivity.TAG, "deposit error - invalid txn");
        } else {
            Toast.makeText(this, "Guthaben aufgeladen \n" + amount, Toast.LENGTH_LONG).show();
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    startActivity(new Intent(DepositActivity.this, BrowseActivity.class)
                            .setData(Uri.parse("content://org.baobab.foodcoapp/accounts/" +
                                    guid + "/transactions")));
                    finish();
                }

            }, 550);
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
