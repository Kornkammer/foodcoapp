package org.baobab.foodcoapp;

import android.annotation.TargetApi;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;

public class DashboardActivity extends AppCompatActivity {

    public static final int LEGITIMATE = 55;
    final HashSet<Integer> multitouch = new HashSet<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);
        findViewById(R.id.deposit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(DashboardActivity.this, DepositActivity.class));
            }
        });
        findViewById(R.id.shop).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                startActivity(
                        new Intent(DashboardActivity.this,
                                PoSimpleActivity.class));
            }
        });
        findViewById(R.id.bilanz).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(DashboardActivity.this, AccountActivity.class));
            }
        });
        findViewById(R.id.kontoauszug).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(
                        new Intent(DashboardActivity.this,
                                LegitimateActivity.class),
                        LEGITIMATE);
            }
        });
        View.OnTouchListener touch = new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == 0) {
                    multitouch.add(v.getId());
                } else if (event.getAction() == 1) {
                    multitouch.remove(v.getId());
                }
                if (multitouch.size() == 2 && multitouch.contains(R.id.bilanz) && multitouch.contains(R.id.shop)) {
                    startActivity(new Intent(DashboardActivity.this, PosActivity.class));
                } else if (multitouch.size() == 3) {
                    Intent i = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);
                    for (ResolveInfo a : getPackageManager().queryIntentActivities(i, 0)) {
                        if (!a.activityInfo.packageName.contains("baobab")) {
                            startActivity(new Intent()
                                    .setClassName(a.activityInfo.packageName, a.activityInfo.name));
                        }
                    }
                }
                return false;
            }
        };
        findViewById(R.id.deposit).setOnTouchListener(touch);
        findViewById(R.id.shop).setOnTouchListener(touch);
        findViewById(R.id.bilanz).setOnTouchListener(touch);
        findViewById(R.id.kontoauszug).setOnTouchListener(touch);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK && requestCode == LEGITIMATE) {
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.dashboard, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.export:
                String mail = PreferenceManager.getDefaultSharedPreferences(this)
                        .getString("export_email", "");
                Intent intent = new Intent(Intent.ACTION_SEND, Uri.parse("mailto:" + mail));
                String date = new SimpleDateFormat("yyyy_MM_dd").format(new Date());
                intent.putExtra(Intent.EXTRA_EMAIL, new String[] {mail});
                intent.putExtra(Intent.EXTRA_TEXT, "FoodCoApp Backup und Excel Export vom " + date);
                intent.putExtra(Intent.EXTRA_SUBJECT, "FoodCoApp " + date + " Export");
                intent.setType("application/zip");
                intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(
                        Export.create(this, "foodcoapp_" + date + ".zip")));
                Intent chooser = Intent.createChooser(intent, "Daten Backup Ex(el)port");
                startActivity(chooser);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause() {
        super.onPause();
        multitouch.clear();
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
}
