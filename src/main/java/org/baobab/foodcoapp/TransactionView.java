package org.baobab.foodcoapp;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.Uri;
import android.support.v4.app.FragmentActivity;
import android.support.v4.widget.CursorAdapter;
import android.support.v7.widget.GridLayout;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;


// yes, this is a mess :/

public class TransactionView extends GridLayout {

    OnClickListener onAmountClick;
    OnClickListener onSumClick;
    TextView header;
    String account;
    double sum;
    boolean positive;
    boolean showImages = true;
    private boolean headersClickable = true;

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

    public void setOnSumClick(OnClickListener onSumClick) {
        this.onSumClick = onSumClick;
    }

    public void showImages(boolean showImages) {
        this.showImages = showImages;
    }

    public void headersClickable(boolean headersClickable) {
        this.headersClickable = headersClickable;
    }

    public void populate(Cursor data) {
        removeAllViews();
        data.moveToPosition(-1);
        account = "";
        sum = 0.0;
        while (data.moveToNext()) {
            addProduct(data);
        }
    }

    private void addProduct(Cursor data) {
        final long product_id = data.getLong(3);
        float quantity = - data.getFloat(4);
        float price = data.getFloat(5);
        float total = quantity * price;
        sum += (quantity * price);
        if (!account.equals(data.getString(2)) || positive != quantity > 0) {
            account = data.getString(2);
            positive = quantity > 0;
            header = new TextView(getContext());
//            header.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.font_size_small));
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.columnSpec = GridLayout.spec(0, 6);
            lp.setGravity(Gravity.FILL_HORIZONTAL);
            FrameLayout f = new FrameLayout(getContext());
            if (onAmountClick != null) {
                if (data.getString(10).equals("aktiva")) {
                    if (quantity > 0) {
                        if (account.equals("lager") || account.equals("kasse")) {
                            header.setText("aus " + data.getString(12) + " raus");
                        } else if (account.equals("forderungen")) {
                            header.setText("Forderung begleichen");
                        } else if (account.equals("forderungen")) {
                            header.setText("von Bank abheben");
                        } else if (account.equals("kosten")) {
                            header.setText("Kosten umlegen");
                        } else if (account.equals("inventar")) {
                            header.setText("Inventar abschreiben");
                        }
                        header.setTextColor(getResources().getColor(R.color.medium_red));
                        f.setBackgroundResource(R.drawable.background_red);
                    } else {
                        if (account.equals("lager") || account.equals("kasse")) {
                            header.setText("in " + data.getString(12) + " rein");
                        } else if (account.equals("forderungen")) {
                            header.setText("offene Forderung");
                        } else if (account.equals("bank")) {
                            header.setText("auf Bank einzahlen");
                        } else if (account.equals("kosten")) {
                            header.setText("Kosten ausgeben");
                        } else if (account.equals("inventar")) {
                            header.setText("Inventar anschaffen");
                        }
                        header.setTextColor(getResources().getColor(R.color.medium_green));
                        f.setBackgroundResource(R.drawable.background_green);
                    }
                } else {
                    if (quantity > 0) {
                        header.setText("auf Konto gutschreiben");
                        header.setTextColor(getResources().getColor(R.color.medium_red));
                        f.setBackgroundResource(R.drawable.background_red);
                    } else {
                        header.setText("von Konto abbuchen");
                        header.setTextColor(getResources().getColor(R.color.medium_green));
                        f.setBackgroundResource(R.drawable.background_green);
                    }
                }
            }
            ViewGroup.LayoutParams p = new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT );
            lp.bottomMargin = - getResources().getDimensionPixelSize(R.dimen.padding_xsmall);
            f.addView(header, p);
            addView(f, lp);
            f.setId(data.getInt(3));
            f.setTag(String.valueOf(quantity));
            if (headersClickable) {
                f.setClickable(true);
                f.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ContentValues cv = new ContentValues();
                        cv.put("quantity", -1);
                        getContext().getContentResolver()
                                .update(((FragmentActivity) getContext())
                                        .getIntent().getData(), cv, null, null);
                    }
                });
                f.setOnLongClickListener(new OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        final Cursor accounts = getContext().getContentResolver().query(
                                Uri.parse("content://org.baobab.foodcoapp/accounts"),
                                null, "parent_guid <> ''", null, null);
                        new AlertDialog.Builder(getContext())
                                .setAdapter(new CursorAdapter(getContext(), accounts, false) {
                                    @Override
                                    public View newView(Context context, Cursor cursor, ViewGroup parent) {
                                        TextView v = new TextView(getContext());
                                        v.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.font_size_large));
                                        v.setTypeface(null, Typeface.BOLD);
                                        return v;
                                    }

                                    @Override
                                    public void bindView(View view, Context context, Cursor cursor) {
                                        ((TextView) view).setText(cursor.getString(1));
                                    }
                                }, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int pos) {
                                        accounts.moveToPosition(pos);
                                        ContentValues cv = new ContentValues();
                                        cv.put("account_guid", accounts.getString(2));
                                        getContext().getContentResolver()
                                                .update(((FragmentActivity) getContext())
                                                        .getIntent().getData().buildUpon()
                                                        .appendEncodedPath("products/" + product_id)
                                                                .build(), cv, null, null);
                                    }
                                }).show();
                        return true;
                    }
                });
            }
        }
        LinearLayout images = new LinearLayout(getContext());
        if (!data.isNull(8) && showImages) {
            Uri img = Uri.parse(data.getString(8));
            images.setOrientation(LinearLayout.HORIZONTAL);
            int numberOfImages = account.equals("lager")? (int) Math.abs(quantity) : 1;
            if (numberOfImages > 23) {
                numberOfImages = 23;
            }
            if (numberOfImages < 1) {
                numberOfImages = 1;
            }
            for (int i = 0; i < numberOfImages; i++) {
                ImageView image = new ImageView(getContext());
                image.setImageURI(img);
                image.setPadding(2, 2, 2, 2);
                image.setScaleType(ImageView.ScaleType.FIT_XY);
                int width = getContext().getResources().getDimensionPixelSize(R.dimen.img_width);
                int height = getContext().getResources().getDimensionPixelSize(R.dimen.img_height);
                double factor = (2.0 - (1.0 / numberOfImages)) / numberOfImages;
                images.addView(image, new LinearLayout.LayoutParams((int) (width * factor ), height));
            }
            images.setClickable(true);
            images.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    getContext().getContentResolver().delete(
                            ((FragmentActivity) getContext()).getIntent().getData().buildUpon()
                                    .appendEncodedPath("products/" + product_id)
                                    .build(), null, null);
                }
            });
            images.setOnLongClickListener(new OnLongClickListener() {

                @Override
                public boolean onLongClick(View v) {
                    getContext().getContentResolver().delete(
                            ((FragmentActivity) getContext()).getIntent().getData().buildUpon()
                                    .appendEncodedPath("products/" + product_id)
                                    .build(), "", null);
                    return false;
                }
            });
        }
        GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
        lp.rowSpec = GridLayout.spec(0, 2);
        lp.topMargin = getContext().getResources().getDimensionPixelSize(R.dimen.padding_small);
        addView(images, lp);

        DecimalView amount = new DecimalView(getContext(), onAmountClick);
        if (data.getLong(3) > 5) {
            if (Math.abs(quantity) < 1.0) {
                amount.setNumber(quantity * 1000);
            } else {
                amount.setNumber(quantity);
            }
        } else {
            amount.setVisibility(View.GONE);
        }
        lp = new GridLayout.LayoutParams();
        lp.rowSpec = GridLayout.spec(0, 2);
        lp.setGravity(Gravity.RIGHT);
        addView(amount, lp);
        amount.setId(data.getInt(3));
        amount.setTag(String.valueOf(quantity));

        TextView x = new TextView(getContext());
        x.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.font_size_small));
        if (data.getLong(3) > 5 && !data.isNull(6) &&
                data.getString(6).equals(getContext().getString(R.string.weight))) {
            if (Math.abs(quantity) < 1) {
                x.setText("g ");
            } else {
                x.setText("kg");
            }
        } else if (data.getLong(3) > 5 && !data.isNull(6) &&
                data.getString(6).equals(getContext().getString(R.string.volume))) {
            if (Math.abs(quantity) < 1) {
                x.setText("ml ");
            } else {
                x.setText("L ");
            }
        } else {
            x.setText("x ");
        }
        x.setTextColor(getResources().getColor(R.color.light_blue));
        if (data.getLong(3) <= 5) {
            x.setVisibility(GONE);
        }
        lp = new GridLayout.LayoutParams();
        lp.rowSpec = GridLayout.spec(0, 2);
        lp.setGravity(Gravity.CENTER);
        lp.leftMargin = -2;
        addView(x, lp);

        TextView title = new TextView(getContext());
        title.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.font_size_medium));
        if (data.getString(10).equals("aktiva")) {
            title.setText(data.getString(7));
        } else {
            title.setText(data.getString(12));
        }
        title.setTypeface(null, Typeface.BOLD);
        title.setPadding(0, getContext().getResources().getDimensionPixelSize(R.dimen.padding_xsmall), 0, 0);
        title.setTextColor(getResources().getColor(R.color.xlight_blue));
        if (quantity <= -100) { // more than 100 in stock
            amount.setNumber((int) quantity); // cut off decimals
            if (quantity <= -1000) {
                amount.setTextSize(R.dimen.font_size_large);
                ((LayoutParams) amount.getLayoutParams()).topMargin = getContext().getResources().getDimensionPixelSize(R.dimen.padding_xlarge);
            }
        }
        title.setEllipsize(TextUtils.TruncateAt.END);
        title.setMaxLines(1);
        lp = new GridLayout.LayoutParams();
        lp.columnSpec = GridLayout.spec(3, 2, 3);
        lp.topMargin = getContext().getResources().getDimensionPixelSize(R.dimen.padding_xsmall);
        lp.width = getContext().getResources().getDimensionPixelSize(R.dimen.column_medium);
        ;
        addView(title, lp);

        TextView sum = new TextView(getContext());
        sum.setText(String.format("%.2f", Math.abs(total)));
        sum.setTypeface(null, Typeface.BOLD);
        if (quantity > 0) {
            sum.setTextColor(getResources().getColor(R.color.xdark_red));
        } else {
            sum.setTextColor(getResources().getColor(R.color.xdark_green));
        }
        sum.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.font_size_large));
        FrameLayout f = new FrameLayout(getContext());
        f.addView(sum);
        f.setClickable(true);
        f.setId(data.getInt(3));
        f.setTag(String.valueOf(total));
        if (onSumClick != null) {
            f.setOnClickListener(onSumClick);
        }
        lp = new GridLayout.LayoutParams();
        lp.rowSpec = GridLayout.spec(0, 2);
        lp.leftMargin = getContext().getResources().getDimensionPixelSize(R.dimen.padding_xsmall);
        lp.bottomMargin = - getContext().getResources().getDimensionPixelSize(R.dimen.padding_xxsmall);
        lp.setGravity(Gravity.RIGHT|Gravity.BOTTOM);
        addView(f, lp);

        TextView details = new TextView(getContext());
        details.setTextColor(getResources().getColor(android.R.color.black));
        if (data.getLong(3) > 5 && !data.isNull(6) &&
                data.getString(6).equals(getContext().getString(R.string.weight))) {
            details.setText(String.format("%.2f", price) + "/kg");
        } else if (data.getLong(3) > 5 && !data.isNull(6) &&
                data.getString(6).equals(getContext().getString(R.string.volume))) {
            details.setText(String.format("%.2f", price) + "/L");
        } else {
            details.setText(String.format("%.2f", price) + "/St");
        }
        details.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.font_size_xxsmall));
        lp = new GridLayout.LayoutParams();
        lp.topMargin = - getContext().getResources().getDimensionPixelSize(R.dimen.padding_xlarge);
        lp.columnSpec = GridLayout.spec(3);
        addView(details, lp);

        TextView eq = new TextView(getContext());
        eq.setText(" =");
        eq.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.font_size_small));
        eq.setTextColor(getResources().getColor(R.color.light_blue));
        lp = new GridLayout.LayoutParams();
        lp.columnSpec = GridLayout.spec(4);
        lp.topMargin = - getContext().getResources().getDimensionPixelSize(R.dimen.padding_xlarge);
        lp.setGravity(Gravity.RIGHT);
        addView(eq, lp);

    }
}
