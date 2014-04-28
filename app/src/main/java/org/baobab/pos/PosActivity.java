package org.baobab.pos;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.encoder.QRCode;

import java.io.File;

public class PosActivity extends FragmentActivity
        implements LoaderManager.LoaderCallbacks<Cursor>,
        View.OnClickListener, View.OnLongClickListener {

    private static final String TAG = "POS";
    private StretchableGrid products;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setIntent(getIntent().setData(Uri.parse(
                "content://org.baobab.pos/transactions/1")));
        setContentView(R.layout.activity_pos);
        products = (StretchableGrid) findViewById(R.id.products);
        getSupportLoaderManager().initLoader(0, null, this);
        findViewById(R.id.sum).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(PosActivity.this,
                        LegitimateActivity.class)
                .setData(getIntent().getData()));
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        String contents = data.getStringExtra("SCAN_RESULT");
        String format = data.getStringExtra("SCAN_RESULT_FORMAT");
        Toast.makeText(this, contents, 3000).show();
    }

    @Override
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    protected void onStart() {
        super.onStart();
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
                    data.getString(2)), i);
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
        getContentResolver().insert(
                getIntent().getData().buildUpon()
                .appendEncodedPath("products/" + ((ProductButton) v).id)
                .build(), null);

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

        public ProductButton(Context context, long id, String title, String path) {
            super(context);
            this.id = id;
            View.inflate(getContext(), R.layout.view_product_button, this);
            ((TextView) findViewById(R.id.title)).setText(title);
            if (path != null) {
                ((ImageView) findViewById(R.id.image))
                        .setImageURI(Uri.fromFile(new File(path)));
            } else {
                empty = true;
            }
            setBackgroundResource(R.drawable.background_product_button);
            setClickable(true);
            setOnClickListener(PosActivity.this);
            setOnLongClickListener(PosActivity.this);
        }
    }
}
