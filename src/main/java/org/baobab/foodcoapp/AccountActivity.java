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

import org.baobab.foodcoapp.io.BackupExport;
import org.baobab.foodcoapp.io.KnkExport;
import org.baobab.foodcoapp.util.Barcode;
import org.baobab.foodcoapp.view.StretchableGrid;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class AccountActivity extends CheckoutActivity {

    public static final String TAG = "FoodCoApp";

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
        transactionView.addable(true);
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
        if (loader.getId() == 42) {
            data.moveToFirst();
            if (data.getString(10).equals("final")) {
                getSupportActionBar().setTitle(getString(R.string.view) +
                        " " + getString(R.string.transaction) + " " +
                        getIntent().getData().getLastPathSegment() + "   (final)");
                transactionFragment.setUneditable();
                editable = false;
            } else {
                if (getIntent().hasExtra("import")) {
                    getSupportActionBar().setTitle(getString(R.string.importe) +
                            " " + getString(R.string.transaction) + " " +
                            getIntent().getData().getLastPathSegment() + "   (draft)");
                } else {
                    getSupportActionBar().setTitle(getString(R.string.edit) +
                            " " + getString(R.string.transaction) + " " +
                            getIntent().getData().getLastPathSegment() + "   (draft)");
                }
                transactionFragment.setEditable();
                editable = true;
            }
            return;
        }
        editable = true;
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
                    if (button == 16) {
                        page.addView(new ProductButton(
                                AccountActivity.this, -2, "EAN", 1, "",
                                "android.resource://org.baobab.foodcoapp/drawable/ic_menu_add", button), 13);
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
                container.addView(page);
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
            }
        }
    }

    static final DecimalFormat df = new DecimalFormat("0.000");

    @Override
    public boolean onLongClick(View v) {
        startActivity(new Intent(Intent.ACTION_EDIT,
                Uri.parse("content://org.baobab.foodcoapp/products" +
                        (((ProductButton) v).empty ? "" : "/" + ((ProductButton) v).id)))
                .putExtra("button", ((ProductButton) v).button));
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.pos, menu);
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
                intent.putExtra(Intent.EXTRA_EMAIL, new String[] { mail });
                intent.putExtra(Intent.EXTRA_SUBJECT, "Transaktion " +
                        getIntent().getData().getLastPathSegment());
                intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(
                        KnkExport.create(this, getIntent().getData())));
                intent.setType("application/knk");
                Intent chooser = Intent.createChooser(intent, "Share transaction " +
                        getIntent().getData().getLastPathSegment());
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