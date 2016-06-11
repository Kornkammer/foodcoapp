package org.baobab.foodcoapp.view;

import android.content.Context;
import android.util.TypedValue;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.baobab.foodcoapp.R;

import java.util.Locale;

public class DecimalView extends LinearLayout {

    public final TextView amount;
    private final TextView decimals;
    private final TextView point;
    private final int length;

    public DecimalView(Context context, int length, final OnClickListener onClick) {
        super(context);
        this.length = length;
        setOrientation(LinearLayout.HORIZONTAL);
        setBackgroundResource(R.drawable.background_translucent);

        amount = new TextView(getContext());
        amount.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.font_size_xlarge));
        int large = getContext().getResources().getDimensionPixelSize(R.dimen.padding_large);
        int small = getContext().getResources().getDimensionPixelSize(R.dimen.padding_small);
        amount.setPadding(0, -large, 0, -large);
        amount.setText("     ");
        addView(amount);

        point = new TextView(getContext());
        point.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.font_size_xlarge));
        point.setPadding(-5, -large, -5, -large);
        point.setVisibility(INVISIBLE);
        if (Locale.getDefault().equals(Locale.GERMANY)) {
            point.setText(",");
        } else {
            point.setText(".");
        }
        addView(point);

        decimals = new TextView(getContext());
        decimals.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.font_size_medium));
        decimals.setPadding(0, -small, 3, -small+3);
        addView(decimals);

        if (onClick != null) {
            setFocusable(true);
            setClickable(true);
            setOnClickListener(onClick);
        }
        setColor(R.color.xlight_blue);
    }

    public void setTextSize(int size) {
        decimals.setTextSize(TypedValue.COMPLEX_UNIT_PX, (float) (getResources().getDimension(size) * 0.7));
        point.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(size));
        amount.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(size));
        amount.setPadding(0, (int) (- getResources().getDimension(size) * 0.2),
                0, (int) (- getResources().getDimension(size) * 0.2));
    }

    public void setNumber(float number) {
        amount.setText(String.valueOf((int) Math.abs(number)));
        String dec = "";
        if (Math.abs(number) % 1 < 0.01) {
            point.setVisibility(INVISIBLE);
            decimals.setVisibility(INVISIBLE);
        } else {
            point.setVisibility(VISIBLE);
            decimals.setVisibility(VISIBLE);
            dec = String.valueOf(number);
            dec = dec.substring(dec.indexOf(".") + 1);
            dec = dec.substring(0, Math.min(length, dec.length()));
            dec = dec.replaceAll("0+$", "");
        }
        while (dec.length() < length) dec += "0";
        decimals.setText(dec);
    }

    public void setColor(int color) {
        amount.setTextColor(getResources().getColor(color));
        point.setTextColor(getResources().getColor(color));
        decimals.setTextColor(getResources().getColor(color));
    }
}
