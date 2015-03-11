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

public class TransactionFragment extends Fragment
        implements LoaderManager.LoaderCallbacks<Cursor> {

    private GridLayout products;
    private float sum;

    @Override
    public View onCreateView(LayoutInflater flate, ViewGroup p, Bundle state) {
        View frame = flate.inflate(R.layout.fragment_transaction, null, false);
        products = (GridLayout) frame.findViewById(R.id.transaction_products);
        return frame;
    }

    public void load() {
        getActivity().getSupportLoaderManager().initLoader(1, null, this);
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
        data.moveToPosition(-1);
        while (data.moveToNext()) {
            addProduct(data);
        }
        ((TextView) getActivity().findViewById(R.id.total))
                .setText(String.format("%.2f", sum));
    }

    private void addProduct(Cursor data) {
        int quantity = data.getInt(4);
        float price = data.getFloat(5) * -1;
        float total = quantity * price;
        sum += (quantity * price);
        if (!data.isNull(7)) {
            Uri img = Uri.parse(data.getString(7));
            LinearLayout images = new LinearLayout(getActivity());
            images.setOrientation(LinearLayout.HORIZONTAL);
            for (int i = 0; i < quantity; i++) {
                ImageView image = new ImageView(getActivity());
                image.setImageURI(img);
                image.setPadding(2, 2, 2, 2);
                image.setScaleType(ImageView.ScaleType.FIT_XY);
                double factor = (2.0 - (1.0/quantity)) / quantity;
//                double factor = (((double) quantity) - (1 - (1.0/Math.pow(2, quantity-1)))) / quantity;
                images.addView(image,
                        new LinearLayout.LayoutParams((int) (67 * factor ), 42));
            }
            images.setClickable(true);
            final long product_id = data.getLong(3);
            images.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    getActivity().getContentResolver().delete(
                            getActivity().getIntent().getData().buildUpon()
                                    .appendEncodedPath("products/" + product_id)
                                    .build(), null, null);
                }
            });
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.rowSpec = GridLayout.spec(0, 2);
            lp.topMargin = 8;
            products.addView(images, lp);
        }
        TextView amount = new TextView(getActivity());
        amount.setTextSize(getActivity().getResources().getDimension(R.dimen.font_size_xlarge));
        int padding = getActivity().getResources().getDimensionPixelSize(R.dimen.padding_large);
        amount.setPadding(0, -padding, 0, -padding);
        amount.setText(String.valueOf(quantity));
        GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
        lp.rowSpec = GridLayout.spec(0, 2);
        products.addView(amount, lp);

        TextView x = new TextView(getActivity());
        x.setTextSize(getActivity().getResources().getDimension(R.dimen.font_size_small));
        x.setTextSize(15);
        x.setText("x ");
        lp = new GridLayout.LayoutParams();
        lp.rowSpec = GridLayout.spec(0, 2);
        lp.leftMargin = -2;
        products.addView(x, lp);

        TextView title = new TextView(getActivity());
        title.setTextSize(getActivity().getResources().getDimension(R.dimen.font_size_medium));
        title.setText(data.getString(6));
        title.setTypeface(null, Typeface.BOLD);
        title.setPadding(0, 0, 0, - getActivity().getResources().getDimensionPixelSize(R.dimen.padding_small));
        lp = new GridLayout.LayoutParams();
        lp.columnSpec = GridLayout.spec(3, 2);
//        lp.topMargin = 27;
        products.addView(title, lp);

        TextView result = new TextView(getActivity());
        result.setText(String.format("%.2f", total));
        result.setTypeface(null, Typeface.BOLD);
        result.setTextColor(getResources().getColor(android.R.color.black));
        result.setPadding(0, -padding, 0, 0);
        result.setTextSize(getActivity().getResources().getDimension(R.dimen.font_size_large));
        lp = new GridLayout.LayoutParams();
        lp.rowSpec = GridLayout.spec(0, 2);
        lp.setGravity(Gravity.RIGHT);
        products.addView(result, lp);

        TextView details = new TextView(getActivity());
        details.setTextColor(getResources().getColor(android.R.color.black));
        details.setText(String.format("%.2f", price) + "/St");
        details.setTextSize(getActivity().getResources().getDimension(R.dimen.font_size_xsmall));
        lp = new GridLayout.LayoutParams();
        lp.columnSpec = GridLayout.spec(3);
        products.addView(details, lp);

        TextView eq = new TextView(getActivity());
        eq.setText(" =");
        eq.setTextSize(getActivity().getResources().getDimension(R.dimen.font_size_small));
        lp = new GridLayout.LayoutParams();
        lp.columnSpec = GridLayout.spec(4);
        lp.setGravity(Gravity.BOTTOM);
        lp.bottomMargin = 12;
        products.addView(eq, lp);


    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }

}
