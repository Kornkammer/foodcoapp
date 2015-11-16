package org.baobab.foodcoapp.view;

import android.content.Context;
import android.util.TypedValue;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.baobab.foodcoapp.R;

import java.util.Locale;

public class DecimalView extends LinearLayout {

    private final TextView amount;
    private final TextView decimals;
    private final TextView point;

    public DecimalView(Context context, final OnClickListener onClick) {
        super(context);
        setOrientation(LinearLayout.HORIZONTAL);
        setBackgroundResource(R.drawable.background_translucent);

        amount = new TextView(getContext());
        amount.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.font_size_xlarge));
        int large = getContext().getResources().getDimensionPixelSize(R.dimen.padding_large);
        int small = getContext().getResources().getDimensionPixelSize(R.dimen.padding_small);
        amount.setTextColor(getResources().getColor(R.color.xlight_blue));
        amount.setPadding(0, -large, 0, -large);
        amount.setText("     ");
        addView(amount);

        point = new TextView(getContext());
        point.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.font_size_xlarge));
        point.setTextColor(getResources().getColor(R.color.xlight_blue));
        point.setPadding(-5, -large, -5, -large);
        point.setVisibility(GONE);
        if (Locale.getDefault().equals(Locale.GERMANY)) {
            point.setText(",");
        } else {
            point.setText(".");
        }
        addView(point);

        decimals = new TextView(getContext());
        decimals.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.font_size_medium));
        decimals.setTextColor(getResources().getColor(R.color.xlight_blue));
        decimals.setPadding(0, -small, 3, -small);
        addView(decimals);

        if (onClick != null) {
            setFocusable(true);
            setClickable(true);
            setOnClickListener(onClick);
        }
    }

    public void setTextSize(int size) {
        amount.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(size));
    }

    public void setNumber(float number) {
        amount.setText(String.valueOf((int) Math.abs(number)));
        if (number % 1 == 0) {
            point.setVisibility(GONE);
            decimals.setVisibility(GONE);
        } else {
            point.setVisibility(VISIBLE);
            decimals.setVisibility(VISIBLE);
            String dec = String.valueOf(number);
            dec = dec.substring(dec.indexOf(".") + 1);
            dec = dec.substring(0, Math.min(3, dec.length()));
            dec = dec.replaceAll("0+$", "");
            decimals.setText(dec);
        }
    }
}
