package org.baobab.foodcoapp;

import android.annotation.TargetApi;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class PosActivity extends AppCompatActivity
        implements LoaderManager.LoaderCallbacks<Cursor>,
        View.OnClickListener, View.OnLongClickListener, Scale.ScaleListener, View.OnKeyListener {

    public static final String TAG = "FoodCoApp";
    private ViewPager pager;
    private TextView scaleView;
    private Scale scale;
    private int weight;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pos);
        if (savedInstanceState == null) {
            resetTransaction();
        }
        getSupportLoaderManager().initLoader(0, null, this);
        pager = (ViewPager) findViewById(R.id.pager);
        findViewById(R.id.scanner).setOnKeyListener(this);
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
                b.put("img", "android.resource://org.baobab.foodcoapp/drawable/cash");
                getContentResolver().insert(getIntent().getData().buildUpon()
                        .appendEncodedPath("products").build(), b);
            }
        });
        findViewById(R.id.pin).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(PosActivity.this,
                        LegitimateActivity.class)
                .setData(getIntent().getData())
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET));
            }
        });
        findViewById(R.id.scan).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(PosActivity.this,
                        LegitimateActivity.class)
                        .setData(getIntent().getData())
                        .putExtra("SCAN", true)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET));
            }
        });
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
        fullscreen();
        ((TransactionFragment) getSupportFragmentManager()
                .findFragmentById(R.id.transaction)).load();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        scale.registerForUsb();
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
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(this,
                Uri.parse("content://org.baobab.foodcoapp/products"),
                null, "button != 0", null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, final Cursor data) {
        data.moveToLast();
        final int pages = (data.getInt(5) / 16) + 1;
        data.moveToFirst();
        pager.setOffscreenPageLimit(42);
        pager.setAdapter(new PagerAdapter() {
            @Override
            public int getCount() {
                return pages;
            }

            @Override
            public Object instantiateItem(ViewGroup container, int position) {
                StretchableGrid page = new StretchableGrid(PosActivity.this, 4, 4);
                for (int i = 1; i <= 16; i++) {
                    int button = (int) position * 16 + i;
                    if (data.getInt(5) == button) {
                        page.addView(new ProductButton(
                                PosActivity.this,
                                data.getLong(0),
                                data.getString(1),
                                data.getFloat(2),
                                data.getString(3),
                                data.getString(4), button), i);
                        if (!data.isLast()) {
                            data.moveToNext();
                        }
                    } else {
                        page.addView(new ProductButton(
                                PosActivity.this, 0, "", 0, null, null, button), i);
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
        if (((ProductButton) v).id == 5) {
            Barcode.scan(this, "EAN_8");
            return;
        } else if (((ProductButton) v).id == 6) {
            startActivityForResult(new Intent(this, ProductEditActivity.class), 42);
            return;
        }
        ProductButton b = (ProductButton) v;
        addProductToTransaction(b.id, b.title, (- (float) weight) / 1000, b.price, b.unit, b.img);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent d) {
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
        if (quantity != -1 && quantity != 0) {
            cv.put("quantity", quantity);
        }
        cv.put("price", price);
        cv.put("unit", unit);
        cv.put("img", img);
        getContentResolver().insert(getIntent().getData().buildUpon()
                .appendEncodedPath("products").build(), cv);
    }

    @Override
    public boolean onLongClick(View v) {
        startActivity(new Intent(Intent.ACTION_EDIT,
                Uri.parse("content://org.baobab.foodcoapp/products/" +
                        ((ProductButton) v).id))
                .putExtra("button", ((ProductButton) v).button));
        return false;
    }

    static final DecimalFormat df = new DecimalFormat("0.000");

    @Override
    public void onWeight(int gramms) {
        if (gramms == -1) {
            scaleView.setText("");
            weight = 0;
            return;
        }
        weight = gramms;
        if (scaleView == null) return;
        if (gramms < 1000) {
            scaleView.setText("Waage: " + gramms + "g");
        } else {
            scaleView.setText("Waage: " + df.format(((float) gramms) / 1000) + "kg");
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
            setOnClickListener(PosActivity.this);
            setOnLongClickListener(PosActivity.this);
        }
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
                startActivity(new Intent(this, TransactionsActivity.class));
                break;
            case R.id.settings:
                startActivity(new Intent(this, SettingsActivity.class));
                break;
            case R.id.accounts:
                startActivity(new Intent(this, AccountActivity.class));
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
}
