package org.baobab.foodcoapp;

import android.annotation.TargetApi;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.Loader;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;

import org.baobab.foodcoapp.io.Export;
import org.baobab.foodcoapp.util.Barcode;
import org.baobab.foodcoapp.util.Scale;
import org.baobab.foodcoapp.view.StretchableGrid;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class AccountActivity extends CheckoutActivity {

    public static final String TAG = "FoodCoApp";
    private TextView scaleView;

    int layout() {
        return R.layout.activity_pos;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().show();
        findViewById(R.id.bar).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Cursor sum = getContentResolver().query(getIntent().getData().buildUpon()
                        .appendEncodedPath("sum").build(), null, null, null, null);
                sum.moveToFirst();
                ContentValues b = new ContentValues();
                b.put("quantity", - sum.getFloat(2));
                b.put("account_guid", "kasse");
                b.put("product_id", 1);
                b.put("title", "Cash");
                b.put("price", 1);
                getContentResolver().insert(getIntent().getData().buildUpon()
                        .appendEncodedPath("products").build(), b);
            }
        });
        findViewById(R.id.pin).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(AccountActivity.this,
                        LegitimateActivity.class)
                .setData(getIntent().getData())
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET));
            }
        });
        findViewById(R.id.scan).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(AccountActivity.this,
                        LegitimateActivity.class)
                        .setData(getIntent().getData())
                        .putExtra("SCAN", true)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET));
            }
        });
        getSupportFragmentManager().addOnBackStackChangedListener(new FragmentManager.OnBackStackChangedListener() {
            @Override
            public void onBackStackChanged() {
                fullscreen();
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        fullscreen();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getSupportFragmentManager().addOnBackStackChangedListener(new FragmentManager.OnBackStackChangedListener() {
            @Override
            public void onBackStackChanged() {
                fullscreen();
            }
        });
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
    public void onLoadFinished(Loader<Cursor> loader, final Cursor data) {
        final int pages;
        if (data.getCount() > 0) {
            pages = (data.getCount() + 3) / 16 + 1;
            data.moveToFirst();
        } else {
            pages = 1;
        }
        pager.setOffscreenPageLimit(42);
        pager.setAdapter(new PagerAdapter() {
            @Override
            public int getCount() {
                return pages;
            }

            @Override
            public Object instantiateItem(ViewGroup container, int position) {
                StretchableGrid page = new StretchableGrid(AccountActivity.this, 4, 4);
                for (int i = 1; i <= 16; i++) {
                    int button = (int) position * 16 + i;
                    if (button == 13) {
                        page.addView(new ProductButton(
                                AccountActivity.this, -1, "EAN", 1, "",
                                "android.resource://org.baobab.foodcoapp/drawable/ic_menu_add", button), 13);
                    } else if (button == 16) {
                        page.addView(new ProductButton(
                                AccountActivity.this, -2, "EAN", 1, "",
                                "android.resource://org.baobab.foodcoapp/drawable/scan", button), 16);
                    } else if (data.getCount() > 0 && !data.isAfterLast()) {
                        page.addView(new ProductButton(
                                AccountActivity.this,
                                data.getLong(0),
                                data.getString(1),
                                data.getFloat(2),
                                data.getString(3),
                                data.getString(4), button), i);
                        if (!data.isAfterLast()) {
                            data.moveToNext();
                        }
                    } else {
                        page.addView(new ProductButton(
                                AccountActivity.this, 0, "", 0, null, null, button), i);
                    }
                }
                ((ViewPager) container).addView(page);
                return page;

            }

            @Override
            public void destroyItem(View collection, int position, Object o) {
                View view = (View)o;
                ((ViewPager) collection).removeView(view);
                view = null;
            }

            @Override
            public boolean isViewFromObject(View view, Object object) {
                return view == object;
            }
        });
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }

    @Override
    public void onClick(View v) {
        if (((ProductButton) v).id == -2) {
            Barcode.scan(this, "EAN_8");
            return;
        } else if (((ProductButton) v).id == -1) {
            startActivityForResult(new Intent(this, ProductEditActivity.class), 42);
            return;
        } else {
            super.onClick(v);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent d) {
        super.onActivityResult(requestCode, resultCode, d);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case 0:
                    String ean = d.getStringExtra("SCAN_RESULT");
                    handleBarcode(ean);
                    break;
                case 42:
                    addProductToTransaction(d.getLongExtra("id", 0), d.getStringExtra("title"), -1,
                            d.getFloatExtra("price", 1.0f), d.getStringExtra("unit"), d.getStringExtra("img"));
            }
        }
    }

    static final DecimalFormat df = new DecimalFormat("0.000");

    @Override
    public void onWeight(int gramms) {
        super.onWeight(gramms);
        if (scaleView == null) return;
        if (gramms < 1000) {
            scaleView.setText("Waage: " + gramms + "g");
        } else {
            scaleView.setText("Waage: " + df.format(((float) gramms) / 1000) + "kg");
        }
    }

    @Override
    public boolean onLongClick(View v) {
        startActivity(new Intent(Intent.ACTION_EDIT,
                Uri.parse("content://org.baobab.foodcoapp/products/" +
                        ((ProductButton) v).id))
                .putExtra("button", ((ProductButton) v).button));
        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.pos, menu);
        scaleView = (TextView) menu.findItem(R.id.scale).getActionView().findViewById(R.id.weight);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.transactions:
                startActivity(new Intent(this, BrowseActivity.class));
                break;
            case R.id.settings:
                startActivity(new Intent(this, SettingsActivity.class));
                break;
            case R.id.accounts:
                startActivity(new Intent(this, BalanceActivity.class));
                break;
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
                Intent chooser = Intent.createChooser(intent, "Ex(el)port");
                startActivity(chooser);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    boolean goToDashboard() {
        return false;
    }
}