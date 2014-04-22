package org.baobab.pos;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

public class PosActivity extends FragmentActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String TAG = "POS";
    private StretchableGrid products;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_pos);

        products = (StretchableGrid) findViewById(R.id.products);

        getSupportLoaderManager().initLoader(0, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(this,
                Uri.parse("content://org.baobab.pos/products"),
                null, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        while (data.moveToNext()) {
            ProductButton b = new ProductButton(
                    PosActivity.this,
                    data.getString(1));
            products.addView(b);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }

    static class ProductButton extends FrameLayout {

        private ImageView image;

        public ProductButton(Context context, String title) {
            super(context);
            View.inflate(getContext(), R.layout.product_button, this);
            image = (ImageView) findViewById(R.id.image);
            ((TextView) findViewById(R.id.title)).setText(title);
            setBackgroundResource(R.drawable.background_product_button);
            setClickable(true);
        }

        @Override
        protected void onFinishInflate() {

        }
    }
}
