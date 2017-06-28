package org.baobab.foodcoapp;

import android.annotation.TargetApi;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.Loader;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import org.baobab.foodcoapp.io.BackupExport;
import org.baobab.foodcoapp.io.KnkExport;
import org.baobab.foodcoapp.util.Barcode;
import org.baobab.foodcoapp.util.Nfc;
import org.baobab.foodcoapp.view.StretchableGrid;

import java.text.DecimalFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;

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
        pager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                multitouch.clear();
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())) {
            String msg = Nfc.readTag(intent);
            handleBarcode(msg.split(": ")[1]);
        }
        if (intent.getData() == null) {
            if (getIntent().getData() == null ||
                    getSupportActionBar().getTitle().toString().contains("(final)")) {
                resetTransaction();
            }
            if (intent.hasExtra("account")) {
                addProductToTransaction(3,
                        intent.getStringExtra("title"),
                        intent.getFloatExtra("amount", 0) * -1,
                        intent.getFloatExtra("price", 0),
                        intent.getStringExtra("unit"),
                        intent.getStringExtra("img"),
                        intent.getStringExtra("account"));
                time = intent.getLongExtra("time", System.currentTimeMillis());
                ContentValues cv = new ContentValues();
                cv.put("start", time);
                getContentResolver().update(getIntent().getData(), cv, null, null);
                String date = BackupExport.df.format(new Date(time));
                getSupportActionBar().setTitle(getSupportActionBar().getTitle() + "   " + date);
            }
        } else {
            editable = false;
            setIntent(intent);
            getSupportLoaderManager().restartLoader(42, null, this);
        }
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
            if (data.getCount() == 0) {
                Log.e(TAG, "empty txn cursor");
                return;
            }
            data.moveToFirst();
            time = data.getLong(2);
            comment = data.getString(4);
            String date = BackupExport.df.format(new Date(time));
            if (data.getString(10).equals("final")) {
                getSupportActionBar().setTitle(getString(R.string.view) +
                        " " + getString(R.string.transaction) + " " +
                        getIntent().getData().getLastPathSegment() + "   (final) " + date);
                transactionFragment.setUneditable();
                editable = false;
            } else {
                if (getIntent().hasExtra("import")) {
                    getSupportActionBar().setTitle(getString(R.string.importe) +
                            " " + getString(R.string.transaction) + " " +
                            getIntent().getData().getLastPathSegment() + "   (draft) " + date);
                } else {
                    getSupportActionBar().setTitle(getString(R.string.edit) +
                            " " + getString(R.string.transaction) + " " +
                            getIntent().getData().getLastPathSegment() + "   (draft) " + date);
                }
                transactionFragment.enableEdit(true);
                editable = true;
            }
            return;
        }
        if (editable) {
            transactionFragment.enableEdit(true);
        }
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
                                AccountActivity.this, -2, "SCAN", 1, "",
                                "android.resource://org.baobab.foodcoapp/drawable/scan", button), 13);
                    } else if (data.getCount() > 0 && !data.isAfterLast()) {
                        page.addView(new ProductButton(
                                AccountActivity.this,
                                data.getLong(3),
                                data.getString(7),
                                data.getFloat(5),
                                data.getString(6),
                                data.getString(8), button), i);
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
        if (multitouch.size() > 1) {
            if (!misching) {
                misching = true;
            } else {
                misching = false;
                Cursor t = getContentResolver().query(getIntent().getData().buildUpon()
                        .appendEncodedPath("products").build(), null, null, null, null);
                if (t.getCount() > 0) {
                    Snackbar.make(findViewById(R.id.frame), "Mischen braucht leere Txn!",
                            Snackbar.LENGTH_LONG).show();
                    return true;
                }
                String msg = "";
                for (ProductButton b : multitouch) msg += " und " + b.title;
                Snackbar.make(findViewById(R.id.frame), msg.substring(4) + " zusammen mischen", Snackbar.LENGTH_LONG).show();
                float sumPrice = 0;
                float sumQuantity = 0;
                ContentValues cv = new ContentValues();
                for (ProductButton b : multitouch) {
                    Cursor stock = getContentResolver().query(
                            Uri.parse("content://org.baobab.foodcoapp/accounts/lager/products"),
                            null, "title IS '" + b.title + "' AND rounded = ROUND(" + b.price + ", 2)", null, null);
                    if (stock.getCount() == 0) {
                        Snackbar.make(findViewById(R.id.frame), b.title + " " +
                                String.format(Locale.GERMAN, "%.2f", b.price) +
                                " nicht mehr auf Lager", Snackbar.LENGTH_LONG).show();
                        return true;
                    }
                    stock.moveToFirst();
                    sumQuantity += stock.getFloat(4);
                    sumPrice += stock.getFloat(5) * stock.getFloat(4);
                    cv = new ContentValues();
                    cv.put("account_guid", "lager");
                    cv.put("product_id", b.id);
                    cv.put("title", b.title);
                    cv.put("quantity", - stock.getFloat(4));
                    cv.put("price", b.price);
                    cv.put("unit", b.unit);
                    cv.put("img", b.img);
                    getContentResolver().insert(getIntent().getData().buildUpon()
                            .appendEncodedPath("products").build(), cv);
                }
                cv.put("quantity", sumQuantity);
                cv.put("price", sumPrice / sumQuantity);
                getContentResolver().insert(getIntent().getData().buildUpon()
                        .appendEncodedPath("products").build(), cv);
            }
            return true;
        }
        startActivity(new Intent(Intent.ACTION_EDIT,
                Uri.parse("content://org.baobab.foodcoapp/products" +
                        (((ProductButton) v).empty ? "" : "/" + ((ProductButton) v).id)))
                .putExtra("button", ((ProductButton) v).button));
        return true;
    }

    final HashSet<ProductButton> multitouch = new HashSet<>();
    boolean misching = false;

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (event.getAction() == 0) {
            multitouch.add(((ProductButton) v));
        } else if (event.getAction() == 1) {
            multitouch.remove(v);
        }
        return false;
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