package org.baobab.pos;

import android.annotation.TargetApi;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.text.SimpleDateFormat;

public class PosActivity extends ActionBarActivity
        implements LoaderManager.LoaderCallbacks<Cursor>,
        View.OnClickListener, View.OnLongClickListener {

    private static final String TAG = "POS";
    private StretchableGrid products;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pos);
        if (savedInstanceState == null) {
            resetTransaction();
        }
        getSupportLoaderManager().initLoader(0, null, this);
        products = (StretchableGrid) findViewById(R.id.products);
        findViewById(R.id.bar).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Cursor sum = getContentResolver().query(getIntent().getData().buildUpon()
                        .appendEncodedPath("sum").build(), null, null, null, null);
                sum.moveToFirst();
                Log.d(TAG, "sum " + sum.getFloat(2));
                ContentValues b = new ContentValues();
                b.put("quantity", sum.getFloat(2));
                b.put("account_guid", "kasse");
                getContentResolver().insert(getIntent().getData().buildUpon()
                        .appendEncodedPath("products/17").build(), b);
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
    }

    public void resetTransaction() {
        Uri uri = getContentResolver().insert(Uri.parse(
                "content://org.baobab.pos/transactions"), null);
        setIntent(getIntent().setData(uri));
    }

    @Override
    protected void onStart() {
        super.onStart();
        fullscreen();
        ((TransactionFragment) getSupportFragmentManager()
                .findFragmentById(R.id.transaction)).load();
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
                Uri.parse("content://org.baobab.pos/products"),
                null, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        for (int i = 0; i < data.getCount(); i++) {
            data.moveToPosition(i);
            products.addView(new ProductButton(
                    PosActivity.this,
                    data.getLong(0),
                    data.getString(1),
                    data.getString(4)), i);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }

    @Override
    public void onClick(View v) {
        if (((ProductButton) v).empty) {
            return;
        }
        ContentValues cv = new ContentValues();
        cv.put("account_guid", "lager");

        getContentResolver().insert(
                getIntent().getData().buildUpon()
                .appendEncodedPath("products/" + ((ProductButton) v).id)
                .build(), cv);

    }

    @Override
    public boolean onLongClick(View v) {
        startActivity(new Intent(Intent.ACTION_EDIT,
                Uri.parse("content://org.baobab.pos/products/" +
                        ((ProductButton) v).id)));
        return false;
    }

    class ProductButton extends FrameLayout {

        boolean empty;
        long id;

        public ProductButton(Context context, long id, String title, String img) {
            super(context);
            this.id = id;
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
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.settings:
                startActivity(new Intent(this, SettingsActivity.class));
                break;
            case R.id.accounts:
                startActivity(new Intent(this, AccountActivity.class));
                break;
            case R.id.export:
                Export.csv(this);
                Intent mail = new Intent(Intent.ACTION_SEND,
                        Uri.parse("mailto:flo@sonnenstreifen.de"));
                mail.putExtra(Intent.EXTRA_EMAIL, new String[] {"flo@sonnenstreifen.de"});
//                mail.putExtra(Intent.EXTRA_TEXT, "neuster Stand Biergarten");
                mail.putExtra(Intent.EXTRA_SUBJECT, "Kiosk " +
                        new SimpleDateFormat().format(System.currentTimeMillis()));
                mail.setType("application/csv");
                mail.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(Export.file()));
                Intent chooser = Intent.createChooser(mail, "Ex(el)port");
                startActivity(chooser);
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}
