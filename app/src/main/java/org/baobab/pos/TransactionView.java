package org.baobab.pos;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.Uri;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.GridLayout;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class TransactionView extends GridLayout {

    OnClickListener onAmountClick;
    double sum;

    public TransactionView(Context context) {
        this(context, null);
    }

    public TransactionView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setColumnCount(6);
        setRowCount(2);
    }

    public void setOnAmountClick(OnClickListener onAmountClick) {
        this.onAmountClick = onAmountClick;
    }

    public void populate(Cursor data) {
        removeAllViews();
        data.moveToPosition(-1);
        sum = 0.0;
        while (data.moveToNext()) {
            addProduct(data);
        }
        ((TextView) ((FragmentActivity) getContext())
                .findViewById(R.id.sum))
                .setText(String.format("%.2f", sum));
        ((TextView) ((FragmentActivity) getContext())
                .findViewById(R.id.total))
                .setText(String.format("%.2f", sum));
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
                int width = getContext().getResources().getDimensionPixelSize(R.dimen.img_width);
                int height = getContext().getResources().getDimensionPixelSize(R.dimen.img_height);
                double factor = (2.0 - (1.0 / quantity)) / quantity;
                images.addView(image,
                        new LinearLayout.LayoutParams((int) (width * factor ), height));
            }
            images.setClickable(true);
            final long product_id = data.getLong(3);
            images.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    getContext().getContentResolver().delete(
                            ((FragmentActivity) getContext()).getIntent().getData().buildUpon()
                                    .appendEncodedPath("products/" + product_id)
                                    .build(), null, null);
                }
            });
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.rowSpec = GridLayout.spec(0, 2);
            lp.topMargin = getContext().getResources().getDimensionPixelSize(R.dimen.padding_medium);
            addView(images, lp);
        }
        TextView amount = new TextView(getContext());
        amount.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.font_size_xlarge));
        int padding = getContext().getResources().getDimensionPixelSize(R.dimen.padding_large);
        amount.setPadding(0, -padding, 0, -padding);
        amount.setText(String.valueOf(quantity));
        GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
        lp.rowSpec = GridLayout.spec(0, 2);
        lp.setGravity(Gravity.RIGHT);
        amount.setBackgroundResource(R.drawable.background_translucent);
        amount.setTextColor(getResources().getColor(R.color.xlight_blue));
        FrameLayout f = new FrameLayout(getContext());
        f.addView(amount);
        addView(f, lp);
        f.setClickable(true);
        f.setId(data.getInt(3));
        f.setTag(String.valueOf(quantity));
        f.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onAmountClick.onClick(v);
            }
        });

        TextView x = new TextView(getContext());
        x.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.font_size_small));
        x.setText("x ");
        x.setTextColor(getResources().getColor(R.color.light_blue));
        lp = new GridLayout.LayoutParams();
        lp.rowSpec = GridLayout.spec(0, 2);
        lp.setGravity(Gravity.CENTER);
        lp.leftMargin = -2;
        addView(x, lp);

        TextView title = new TextView(getContext());
        title.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.font_size_medium));
        title.setText(data.getString(6));
        title.setTypeface(null, Typeface.BOLD);
        title.setPadding(0, 1, 0, -getContext().getResources().getDimensionPixelSize(R.dimen.padding_small));
        title.setTextColor(getResources().getColor(R.color.xlight_blue));
        lp = new GridLayout.LayoutParams();
        lp.columnSpec = GridLayout.spec(3, 2);
        lp.topMargin = getContext().getResources().getDimensionPixelSize(R.dimen.padding_xsmall);
        addView(title, lp);

        TextView result = new TextView(getContext());
        result.setText(String.format("%.2f", total));
        result.setTypeface(null, Typeface.BOLD);
        result.setTextColor(getResources().getColor(R.color.xdark_green));
        result.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.font_size_large));
        lp = new GridLayout.LayoutParams();
        lp.rowSpec = GridLayout.spec(0, 2);
        lp.leftMargin = getContext().getResources().getDimensionPixelSize(R.dimen.padding_xsmall);
        lp.bottomMargin = - getContext().getResources().getDimensionPixelSize(R.dimen.padding_xsmall);
        lp.setGravity(Gravity.RIGHT|Gravity.BOTTOM);
        addView(result, lp);

        TextView details = new TextView(getContext());
        details.setTextColor(getResources().getColor(android.R.color.black));
        details.setText(String.format("%.2f", price) + "/St");
        details.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.font_size_xsmall));
        lp = new GridLayout.LayoutParams();
        lp.topMargin = - getContext().getResources().getDimensionPixelSize(R.dimen.padding_small);
        lp.columnSpec = GridLayout.spec(3);
        addView(details, lp);

        TextView eq = new TextView(getContext());
        eq.setText(" =");
        eq.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.font_size_small));
        eq.setTextColor(getResources().getColor(R.color.light_blue));
        lp = new GridLayout.LayoutParams();
        lp.columnSpec = GridLayout.spec(4);
        lp.topMargin = - getContext().getResources().getDimensionPixelSize(R.dimen.padding_xsmall);
        lp.setGravity(Gravity.RIGHT);
        addView(eq, lp);


    }
}
