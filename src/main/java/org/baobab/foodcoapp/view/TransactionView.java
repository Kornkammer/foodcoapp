package org.baobab.foodcoapp.view;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.Uri;
import android.support.annotation.NonNull;
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

import org.baobab.foodcoapp.R;


// yes, this IS a mess :/

public class TransactionView extends GridLayout {

    private OnClickListener onAmountClick;
    private OnClickListener onTitleClick;
    private OnClickListener onSumClick;
    private TextView header;
    private String account;
    private double sum;
    private boolean positive;
    private boolean showImages = true;
    private boolean showHeaders = true;
    private boolean headersClickable = true;
    private int columnWidth = R.dimen.column_title;

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

    public void setOnTitleClick(OnClickListener onTitleClick) {
        this.onTitleClick = onTitleClick;
    }

    public void setOnSumClick(OnClickListener onSumClick) {
        this.onSumClick = onSumClick;
    }

    public void showImages(boolean showImages) {
        this.showImages = showImages;
    }

    public void showHeaders(boolean showHeaders) {
        this.showHeaders = showHeaders;
    }

    public void headersClickable(boolean headersClickable) {
        this.headersClickable = headersClickable;
    }

    public void setColumnWidth(int width) {
        columnWidth = width;
    }

    public void populate(Cursor data) {
        removeAllViews();
        data.moveToPosition(-1);
        account = "";
        sum = 0.0;
        while (data.moveToNext()) {
            String accountGuid = data.getString(2);
            float quantity = data.getFloat(4);
            if (!account.equals(accountGuid) || positive != quantity < 0) {
                account = accountGuid;
                positive = quantity < 0;
                if (showHeaders) {
                    showHeaders(data, quantity);
                }
            }
            String title;
            if (data.getColumnCount() == 14 && data.getString(7).equals("Korns")) {
                title = data.getString(12);
            } else {
                title = data.getString(7);
            }
            addProduct(data.getPosition(), data.getInt(1), data.getInt(3), accountGuid,
                    quantity, data.getString(6), data.getFloat(5), title,  data.getString(8));
        }
//        addProduct(-1, -1, 3, "lager", 42.235f, "Kilo", 0, "", "android.resource://org.baobab.foodcoapp/drawable/ic_menu_add");
    }

    private void addProduct(int position, int transactionId, int productId, String accountGuid,
                            float quantity, String unit, float price, String title, String img) {
        sum -= (quantity * price);

        images(accountGuid, productId, quantity, img);
        amount(quantity, productId, position);
        unit(quantity, productId, unit);

        title(title, position, transactionId);
        sum(quantity, productId, position, price);
        details(productId, price, unit);
    }


    private void showHeaders(Cursor data, float quantity) {
        header = new TextView(getContext());
        LayoutParams lp = new LayoutParams();
        lp.columnSpec = GridLayout.spec(0, 6);
        lp.setGravity(Gravity.FILL_HORIZONTAL);
        final long transactionProductId = data.getLong(0);
        FrameLayout f = new FrameLayout(getContext());
        if (data.getLong(9) <= 150 ) {
            if (quantity < 0) {
                if (account.equals("lager") || account.equals("kasse")) {
                    header.setText("aus " + data.getString(12) + " raus");
                } else if (account.equals("forderungen")) {
                    header.setText("Forderung begleichen");
                } else if (account.equals("bank")) {
                    header.setText("von Bank abheben");
                } else if (account.equals("kosten")) {
                    header.setText("Kosten umlegen");
                } else if (account.equals("inventar")) {
                    header.setText("Inventar abschreiben");
                } else if (account.equals("verbindlichkeiten")) {
                    header.setText("Verbindlich bleiben");
                } else if (account.equals("spenden")) {
                    header.setText("Spenden annehmen");
                } else {
                    header.setText("auf Konto " + data.getString(12)  + " gutschreiben");
                }
                header.setTextColor(getResources().getColor(R.color.medium_red));
                f.setBackgroundResource(R.drawable.background_red);
            } else {
                if (account.equals("lager") || account.equals("kasse")) {
                    header.setText("in " + data.getString(12) + " rein");
                } else if (account.equals("forderungen")) {
                    header.setText("Forderung öffnen");
                } else if (account.equals("bank")) {
                    header.setText("auf Bank einzahlen");
                } else if (account.equals("kosten")) {
                    header.setText("Kosten ansammeln");
                } else if (account.equals("inventar")) {
                    header.setText("Inventar anschaffen");
                } else if (account.equals("verbindlichkeiten")) {
                    header.setText("Verbindlichkeit begleichen");
                } else {
                    header.setText("von Konto " + data.getString(12)  + " abbuchen");
                }
                header.setTextColor(getResources().getColor(R.color.medium_green));
                f.setBackgroundResource(R.drawable.background_green);
            }
        } else {
            if (quantity < 0) {
                header.setText("auf Konto gutschreiben");
                header.setTextColor(getResources().getColor(R.color.medium_red));
                f.setBackgroundResource(R.drawable.background_red);
            } else {
                header.setText("von Konto abbuchen");
                header.setTextColor(getResources().getColor(R.color.medium_green));
                f.setBackgroundResource(R.drawable.background_green);
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
                                                    .appendEncodedPath("products/" + transactionProductId)
                                                    .build(), cv, null, null);
                                }
                            }).show();
                    return true;
                }
            });
        }
    }


    private void images(final String accountGuid, final int productId, float quantity, String path) {
        LinearLayout images = new LinearLayout(getContext());
        if (showImages || productId < 3) {
            Uri img;
            int imgWidth = getContext().getResources().getDimensionPixelSize(R.dimen.img_width);
            if (path != null) {
                img = Uri.parse(path);
            } else {
                imgWidth = imgWidth / 2;
                switch (productId) {
                    case 1:
                        img = Uri.parse("android.resource://org.baobab.foodcoapp/drawable/cash");
                        break;
                    case 2:
                        img = Uri.parse("android.resource://org.baobab.foodcoapp/drawable/ic_launcher");
                        break;
                    default:
                        img = Uri.parse("android.resource://org.baobab.foodcoapp/drawable/ic_menu_moreoverflow");
                }
            }
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
                int width = imgWidth;
                int height = getContext().getResources().getDimensionPixelSize(R.dimen.img_height);
                double factor = (2.0 - (1.0 / numberOfImages)) / numberOfImages;
                images.addView(image, new LinearLayout.LayoutParams((int) (width * factor ), height));
            }
            if (onAmountClick != null) {
                images.setClickable(true);
                images.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        getContext().getContentResolver().delete(
                                ((FragmentActivity) getContext()).getIntent().getData().buildUpon()
                                        .appendEncodedPath("accounts/" + accountGuid +
                                                "/products/" + productId).build(), null, null);
                    }
                });
                images.setOnLongClickListener(new OnLongClickListener() {

                    @Override
                    public boolean onLongClick(View v) {
                        getContext().getContentResolver().delete(
                                ((FragmentActivity) getContext()).getIntent().getData().buildUpon()
                                        .appendEncodedPath("accounts/" + accountGuid +
                                                "/products/" + productId).build(), "all", null);
                        return false;
                    }
                });
            }
        }
        LayoutParams lp = new LayoutParams();
        lp.rowSpec = GridLayout.spec(0, 2);
        lp.topMargin = getContext().getResources().getDimensionPixelSize(R.dimen.padding_small);
        addView(images, lp);
    }


    private DecimalView amount(float quantity, int productId, int position) {
        DecimalView amount = new DecimalView(getContext(), onAmountClick);
        if (productId < 3) {
            amount.setVisibility(INVISIBLE);
        } else {
            if (Math.abs(quantity) < 1.0) {
                amount.setNumber(quantity * 1000);
            } else {
                amount.setNumber(quantity);
            }
        }
        LayoutParams lp = new LayoutParams();
        lp.rowSpec = GridLayout.spec(0, 2);
        lp.setGravity(Gravity.RIGHT);
        addView(amount, lp);
        amount.setId(productId);
        amount.setTag(position);
        if (quantity >= 100) { // more than 100 in stock
            amount.setNumber((int) quantity); // cut off decimals
            if (quantity >= 1000) {
                amount.setTextSize(R.dimen.font_size_large);
                ((LayoutParams) amount.getLayoutParams()).topMargin = getContext().getResources().getDimensionPixelSize(R.dimen.padding_xlarge);
            }
        }
        return amount;
    }

    private void unit(float quantity, int productId, String unit) {
        TextView x = new TextView(getContext());
        x.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.font_size_small));
        if (productId < 3) {
            x.setVisibility(GONE);
        } else if (unit != null && unit.equals(getContext().getString(R.string.weight))) {
            if (Math.abs(quantity) < 1) {
                x.setText("g ");
            } else {
                x.setText("kg");
            }
        } else if (unit != null && unit.equals(getContext().getString(R.string.volume))) {
            if (Math.abs(quantity) < 1) {
                x.setText("ml ");
            } else {
                x.setText("L ");
            }
        } else {
            x.setText("x ");
        }
        x.setTextColor(getResources().getColor(R.color.light_blue));
        LayoutParams lp = new LayoutParams();
        lp.rowSpec = GridLayout.spec(0, 2);
        lp.setGravity(Gravity.CENTER);
        lp.leftMargin = -2;
        addView(x, lp);
    }

    private void title(String name, int position, int transactionId) {
        LayoutParams lp;TextView title = new TextView(getContext());
        title.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.font_size_medium));
        title.setText(name);
        title.setTypeface(null, Typeface.BOLD);
        title.setPadding(0, getContext().getResources().getDimensionPixelSize(R.dimen.padding_xsmall), 0, 0);
        title.setTextColor(getResources().getColor(R.color.xlight_blue));
        title.setEllipsize(TextUtils.TruncateAt.MARQUEE);
        title.setMaxLines(1);
        FrameLayout f = new FrameLayout(getContext());
        f.addView(title);
        if (onTitleClick != null) {
            f.setClickable(true);
            f.setFocusable(true);
            f.setId(transactionId);
            f.setTag(position);
            f.setOnClickListener(onTitleClick);
        }
        f.setBackgroundResource(R.drawable.background_translucent);
        lp = new LayoutParams();
        lp.columnSpec = GridLayout.spec(3, 2, 3);
        lp.topMargin = getContext().getResources().getDimensionPixelSize(R.dimen.padding_xsmall);
        lp.width = getContext().getResources().getDimensionPixelSize(columnWidth);
        addView(f, lp);
    }


    private void sum(float quantity, int productId, int position, float price) {
        TextView sum = new TextView(getContext());
        if (price == 0) return;
        sum.setText(String.format("%.2f", Math.abs(quantity * price)));
        sum.setTypeface(null, Typeface.BOLD);
        if (quantity < 0) {
            sum.setTextColor(getResources().getColor(R.color.xdark_red));
        } else {
            sum.setTextColor(getResources().getColor(R.color.xdark_green));
        }
        sum.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.font_size_large));
        FrameLayout f = new FrameLayout(getContext());
        f.addView(sum);
        if (onSumClick != null) {
            f.setClickable(true);
            f.setId(productId);
            f.setTag(position);
            f.setOnClickListener(onSumClick);
            f.setBackgroundResource(R.drawable.background_translucent);
        }
        LayoutParams lp = new LayoutParams();
        lp.rowSpec = GridLayout.spec(0, 2);
        lp.leftMargin = getContext().getResources().getDimensionPixelSize(R.dimen.padding_xsmall);
        lp.bottomMargin = - getContext().getResources().getDimensionPixelSize(R.dimen.padding_xxsmall);
        lp.setGravity(Gravity.RIGHT|Gravity.BOTTOM);
        addView(f, lp);
    }


    private void details(int productId, float price, String unit) {
        if (price == 0) return;
        LayoutParams lp;TextView details = new TextView(getContext());
        details.setTextColor(getResources().getColor(android.R.color.black));
        if (productId > 5 && unit != null &&
                unit.equals(getContext().getString(R.string.weight))) {
            details.setText(String.format("%.2f", price) + "/kg");
        } else if (productId > 5 && unit != null &&
                unit.equals(getContext().getString(R.string.volume))) {
            details.setText(String.format("%.2f", price) + "/L");
        } else {
            details.setText(String.format("%.2f", price) + "/St");
        }
        details.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.font_size_xxsmall));
        lp = new LayoutParams();
        lp.topMargin = - getContext().getResources().getDimensionPixelSize(R.dimen.padding_xlarge);
        lp.columnSpec = GridLayout.spec(3);
        addView(details, lp);

        TextView eq = new TextView(getContext());
        eq.setText(" =");
        eq.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.font_size_small));
        eq.setTextColor(getResources().getColor(R.color.light_blue));
        lp = new LayoutParams();
        lp.columnSpec = GridLayout.spec(4);
        lp.topMargin = - getContext().getResources().getDimensionPixelSize(R.dimen.padding_xlarge);
        lp.setGravity(Gravity.RIGHT);
        addView(eq, lp);
    }
}
