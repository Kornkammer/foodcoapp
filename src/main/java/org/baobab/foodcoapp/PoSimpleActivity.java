package org.baobab.foodcoapp;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class PoSimpleActivity extends AppCompatActivity
        implements LoaderManager.LoaderCallbacks<Cursor>,
        View.OnClickListener, Scale.ScaleListener, View.OnKeyListener {

    private ViewPager pager;
    private Scale scale;
    private int weight;
    float currency = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().hide();
        setContentView(R.layout.activity_possimple);
        if (savedInstanceState == null) {
            resetTransaction();
        }
        getSupportLoaderManager().initLoader(0, null, this);
        findViewById(R.id.scanner).setOnKeyListener(this);
        pager = (ViewPager) findViewById(R.id.pager);
        scale = new Scale(this);
    }

    public void resetTransaction() {
        Uri uri = getContentResolver().insert(Uri.parse(
                "content://org.baobab.foodcoapp/transactions"), null);
        setIntent(getIntent().setData(uri));
    }

    @Override
    public void onStart() {
        super.onStart();
        ((TransactionSimpleFragment) getSupportFragmentManager()
                .findFragmentById(R.id.transaction)).load();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        scale.registerForUsb();
    }

    @Override
    protected void onStop() {
        super.onStop();
        scale.unregisterUsb();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(this,
                Uri.parse("content://org.baobab.foodcoapp/products"),
                null, "_id > 5", null, "UPPER(title)");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, final Cursor data) {
        final int pages;
        if (data.getCount() > 0) {
            pages = (data.getCount() + 1) / 16 + 1;
            data.moveToFirst();
        } else {
            pages = 1;
        }
        System.out.println("pages " + pages);
        pager.setOffscreenPageLimit(42);
        pager.setAdapter(new PagerAdapter() {
            @Override
            public int getCount() {
                return pages;
            }

            @Override
            public Object instantiateItem(ViewGroup container, int position) {
                StretchableGrid page = new StretchableGrid(PoSimpleActivity.this, 4, 4);
                for (int i = 1; i <= 16; i++) {
                    int button = (int) position * 16 + i;
                    if (data.getCount() > 0 && !data.isAfterLast()) {
                        page.addView(new ProductButton(
                                PoSimpleActivity.this,
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
                                PoSimpleActivity.this, 0, "", 0, null, null, button), i);
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
        if (((ProductButton) v).empty) {
            return;
        }
        ProductButton b = (ProductButton) v;
        addProductToTransaction(b.id, b.title, (-(float) weight) / 1000, b.price, b.unit, b.img);
    }

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_ENTER) {
            EditText scan = (EditText) findViewById(R.id.scanner);
            String ean = scan.getText().toString();
            if (ean.length() > 0) {
                handleBarcode(ean);
                scan.setText("");
                return true;
            }
        }
        return false;
    }

    private void handleBarcode(String ean) {
        Cursor p = getContentResolver().query(Uri.parse(
                        "content://org.baobab.foodcoapp/products"),
                null, "ean IS ?", new String[] { ean }, null);
        if (p.getCount() > 0) {
            p.moveToFirst();
            String title = p.getString(1).replace("AND ", "").replace("Adechser ", "")
                    .replace("Bioland ", "").replace("Demeter ", "");
            addProductToTransaction(p.getLong(0), title, -1, p.getFloat(2), p.getString(3), p.getString(4));
//            Toast.makeText(this, "Found " + p.getString(1) + " " + p.getFloat(2), Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "Product NOT found! \n" + ean, Toast.LENGTH_LONG).show();
        }
    }

    private void addProductToTransaction(long id, String title, float quantity, float price, String unit, String img) {
        ContentValues cv = new ContentValues();
        cv.put("account_guid", "lager");
        cv.put("product_id", id);
        cv.put("title", title);
        if (quantity != -1 && quantity != 0 && unit.equals(getString(R.string.weight))) {
            cv.put("quantity", quantity);
        }
        cv.put("price", price);
        cv.put("unit", unit);
        cv.put("img", img);
        getContentResolver().insert(getIntent().getData().buildUpon()
                .appendEncodedPath("products").build(), cv);
    }

    @Override
    public void onWeight(int gramms) {
        if (gramms == -1) {
            weight = 0;
            return;
        }
        weight = gramms;
    }

    @Override
    public void onBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            super.onBackPressed();
        } else {
            startActivity(new Intent(this, DashboardActivity.class));
        }
    }


    class ProductButton extends FrameLayout {

        boolean empty;
        int button;
        long id;
        String title;
        float price;
        String unit;
        String img;

        public ProductButton(Context context, long id, String title, float price, String unit, String img, int button) {
            super(context);
            this.id = id;
            this.button = button;
            this.title = title;
            this.price = price;
            this.unit = unit;
            this.img = img;
            View.inflate(getContext(), R.layout.view_product_button, this);
            ((TextView) findViewById(R.id.title)).setText(title);
            if (img != null) {
                ((ImageView) findViewById(R.id.image))
                        .setImageURI(Uri.parse(img));
            } else {
                empty = true;
            }
            setBackgroundResource(R.drawable.background_product_button);
            setClickable(true);
            setOnClickListener(PoSimpleActivity.this);
        }
    }

}
