package org.baobab.pos;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.Uri;
import android.support.v7.widget.GridLayout;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class TransactionView extends ScrollView {

    private GridLayout products;
    private float sum;

    public TransactionView(Context context) {
        this(context, null);
    }

    public TransactionView(Context context, AttributeSet attrs) {
        super(context, attrs);
        View.inflate(getContext(), R.layout.view_transaction, this);
        products = (GridLayout) findViewById(R.id.transaction_products);
    }

    public void populate(Cursor data) {
        products.removeAllViews();
        data.moveToPosition(-1);
        sum = 0;
        while (data.moveToNext()) {
            addProduct(data);
        }
//        ((TextView) findViewById(R.id.total))
//                .setText(String.format("%.2f", sum));
    }

    private void addProduct(Cursor data) {
        int quantity = data.getInt(4);
        float price = data.getFloat(5) * -1;
        float total = quantity * price;
        sum += (quantity * price);
        if (!data.isNull(7)) {
            Uri img = Uri.parse(data.getString(7));
            LinearLayout images = new LinearLayout(getContext());
            images.setOrientation(LinearLayout.HORIZONTAL);
            for (int i = 0; i < quantity; i++) {
                ImageView image = new ImageView(getContext());
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
//                    getContext().getContentResolver().delete(
//                            getContext().getIntent().getData().buildUpon()
//                                    .appendEncodedPath("products/" + product_id)
//                                    .build(), null, null);
                }
            });
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.rowSpec = GridLayout.spec(0, 2);
            lp.topMargin = 8;
            products.addView(images, lp);
        }
        TextView amount = new TextView(getContext());
        amount.setTextSize(getContext().getResources().getDimension(R.dimen.font_size_xlarge));
        int padding = getContext().getResources().getDimensionPixelSize(R.dimen.padding_large);
        amount.setPadding(0, -padding, 0, -padding);
        amount.setText(String.valueOf(quantity));
        GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
        lp.rowSpec = GridLayout.spec(0, 2);
        products.addView(amount, lp);

        TextView x = new TextView(getContext());
        x.setTextSize(getContext().getResources().getDimension(R.dimen.font_size_small));
        x.setTextSize(15);
        x.setText("x ");
        lp = new GridLayout.LayoutParams();
        lp.rowSpec = GridLayout.spec(0, 2);
        lp.leftMargin = -2;
        products.addView(x, lp);

        TextView title = new TextView(getContext());
        title.setTextSize(getContext().getResources().getDimension(R.dimen.font_size_medium));
        title.setText(data.getString(6));
        title.setTypeface(null, Typeface.BOLD);
        title.setPadding(0, 0, 0, - getContext().getResources().getDimensionPixelSize(R.dimen.padding_small));
        lp = new GridLayout.LayoutParams();
        lp.columnSpec = GridLayout.spec(3, 2);
//        lp.topMargin = 27;
        products.addView(title, lp);

        TextView result = new TextView(getContext());
        result.setText(String.format("%.2f", total));
        result.setTypeface(null, Typeface.BOLD);
        result.setTextColor(getResources().getColor(android.R.color.black));
        result.setPadding(0, -padding, 0, 0);
        result.setTextSize(getContext().getResources().getDimension(R.dimen.font_size_large));
        lp = new GridLayout.LayoutParams();
        lp.rowSpec = GridLayout.spec(0, 2);
        lp.setGravity(Gravity.RIGHT);
        products.addView(result, lp);

        TextView details = new TextView(getContext());
        details.setTextColor(getResources().getColor(android.R.color.black));
        details.setText(String.format("%.2f", price) + "/St");
        details.setTextSize(getContext().getResources().getDimension(R.dimen.font_size_xsmall));
        lp = new GridLayout.LayoutParams();
        lp.columnSpec = GridLayout.spec(3);
        products.addView(details, lp);

        TextView eq = new TextView(getContext());
        eq.setText(" =");
        eq.setTextSize(getContext().getResources().getDimension(R.dimen.font_size_small));
        lp = new GridLayout.LayoutParams();
        lp.columnSpec = GridLayout.spec(4);
        lp.setGravity(Gravity.BOTTOM);
        lp.bottomMargin = 12;
        products.addView(eq, lp);


    }
}
