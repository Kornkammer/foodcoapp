package org.baobab.pos;

import android.database.Cursor;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.GridLayout;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;

public class TransactionFragment extends Fragment
        implements LoaderManager.LoaderCallbacks<Cursor> {

    private GridLayout products;
    private float sum;

    @Override
    public View onCreateView(LayoutInflater flate, ViewGroup p, Bundle state) {
        View frame = flate.inflate(R.layout.fragment_transaction, null, false);
        products = (GridLayout) frame.findViewById(R.id.transaction_products);
        getActivity().getSupportLoaderManager().initLoader(1, null, this);
        return frame;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(getActivity(),
                getActivity().getIntent().getData(),
                null, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        products.removeAllViews();
        sum = 0;
        while (data.moveToNext()) {
            addProduct(data);
        }
        ((TextView) getActivity().findViewById(R.id.sum))
                .setText(String.format("%.2f", sum));
    }

    private void addProduct(Cursor data) {
        TextView title = new TextView(getActivity());
        title.setText(data.getString(5));
        GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
        lp.columnSpec = GridLayout.spec(0, 2);
        products.addView(title, lp);
        int quantity = data.getInt(3);
        float price = data.getFloat(7);
        float total = quantity * price;
        sum += (quantity * price);
        if (!data.isNull(6)) {
            Uri imageUri = Uri.fromFile(new File(data.getString(6)));
            LinearLayout images = new LinearLayout(getActivity());
            images.setOrientation(LinearLayout.HORIZONTAL);
            for (int i = 0; i < quantity; i++) {
                ImageView image = new ImageView(getActivity());
                image.setImageURI(imageUri);
                image.setPadding(2, 2, 2, 2);
                image.setScaleType(ImageView.ScaleType.FIT_XY);
                images.addView(image,
                        new LinearLayout.LayoutParams(36, 36));
            }
            images.setClickable(true);
            final long product_id = data.getLong(2);
            images.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    getActivity().getContentResolver().delete(
                            getActivity().getIntent().getData().buildUpon()
                            .appendEncodedPath("products/" + product_id)
                            .build(), null, null);
                }
            });
            products.addView(images);
        }
        TextView result = new TextView(getActivity());
        result.setText(String.format("%.2f", total));
        result.setTypeface(null, Typeface.BOLD);
        result.setTextColor(getResources().getColor(android.R.color.black));
        result.setTextSize(34);
        lp = new GridLayout.LayoutParams();
        lp.rowSpec = GridLayout.spec(1, 2);
        lp.setGravity(Gravity.RIGHT);
        lp.topMargin = -21;
        products.addView(result, lp);
        TextView details = new TextView(getActivity());
        details.setTextColor(getResources().getColor(android.R.color.black));
        details.setText(quantity + " x " + String.format("%.2f", price));
        details.setTextSize(7);
        lp = new GridLayout.LayoutParams();
        lp.setGravity(Gravity.RIGHT);
        lp.topMargin = -3;
        lp.bottomMargin = 8;
        products.addView(details, lp);

    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }

}
