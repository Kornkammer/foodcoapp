package org.baobab.pos;

import android.content.Context;
import android.support.v7.widget.GridLayout;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.DecimalFormat;

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
        point.setPadding(-small, -large, -small, -large);
        point.setVisibility(GONE);
        point.setText(".");
        addView(point);

        decimals = new TextView(getContext());
        decimals.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.font_size_medium));
        decimals.setTextColor(getResources().getColor(R.color.xlight_blue));
        decimals.setPadding(0, -large, 3, -small);
        addView(decimals);

        setFocusable(true);
        setClickable(true);
        if (onClick != null) {
            setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    onClick.onClick(v);
                }
            });
        }
    }

    public void setNumber(float number) {
        amount.setText(String.valueOf((int) Math.abs(number)));
        if (number % 1 == 0) {
            point.setVisibility(GONE);
            decimals.setVisibility(GONE);
        } else {
            point.setVisibility(VISIBLE);
            decimals.setVisibility(VISIBLE);
            DecimalFormat df = new DecimalFormat("0.000");
            String d = df.format(Math.abs(number));
            d = d.substring(d.indexOf(".") +1).replaceAll("0+$", "");
            decimals.setText(d);
//            decimals.setText(String.valueOf((int) ((Math.abs(number) % 1) * 1000)));
        }
    }
}
