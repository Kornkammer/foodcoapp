package org.baobab.foodcoapp.fragments;

import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Vibrator;
import android.support.design.widget.Snackbar;
import android.support.v4.content.Loader;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import org.baobab.foodcoapp.ProductEditActivity;
import org.baobab.foodcoapp.R;

import java.util.ArrayList;

public class TransactionEditFragment extends TransactionFragment {

    @Override
    public void enableEdit(boolean allowNegative) {
        super.enableEdit(allowNegative);
        transaction.setOnTitleClick(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                int position = (Integer) v.getTag();
                String accountGuid = "lager";
                String id = "";
                if (position > -1) {
                    txn.moveToPosition((Integer) v.getTag());
                    id = "/" + txn.getInt(0);
                    accountGuid =  txn.getString(2);
                }
                startActivity(new Intent(getActivity(), ProductEditActivity.class)
                        .setData(getActivity().getIntent().getData().buildUpon()
                                .appendEncodedPath("products" + id).build())
                        .putExtra("account_guid", accountGuid).putExtra("price", sum));
            }
        });
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        super.onLoadFinished(loader, data);
        transaction.headersClickable(true);
        TextView ok = ((TextView) getActivity().findViewById(R.id.sum));
        TextView header = ((TextView) getActivity().findViewById(R.id.header));
        if (sum < 0.01 && sum > -0.01) {
            header.setText("");
            header.setBackgroundResource(R.drawable.background_translucent);
            ok.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_ok, 0, 0, 0);
            ok.setText("");
        } else {
            ok.setCompoundDrawablesWithIntrinsicBounds(0,0,0,0);
            ok.setText(String.format("%.2f", sum));
            if (sum < 0) {
                header.setText("Wechselgeld");
                header.setBackgroundResource(R.drawable.background_red);
            } else {
                header.setText("zu Bezahlen");
                header.setTextColor(getResources().getColor(R.color.medium_green));
                header.setBackgroundResource(R.drawable.background_green);
            }
        }
    }

    @Override
    public void onClick(View v) {
        if (!editable) {
            Toast.makeText(getActivity(), "Unveränderbare Geschichte!", Toast.LENGTH_LONG).show();
            return;
        }
        if (sum < -0.01) {
            ((Vibrator) getActivity().getSystemService(Context.VIBRATOR_SERVICE)).vibrate(200);
            MediaPlayer.create(getActivity(), R.raw.error_3).start();
            Toast.makeText(getActivity(), "Wechselgeld " + sum, Toast.LENGTH_SHORT).show();
        } else if (sum > 0.01) {
            ((Vibrator) getActivity().getSystemService(Context.VIBRATOR_SERVICE)).vibrate(300);
            MediaPlayer.create(getActivity(), R.raw.error_4).start();
            Toast.makeText(getActivity(), "Noch " + sum + " offen!", Toast.LENGTH_LONG).show();
        } else {
            if (transactionValid()) {
                storeNewProducts();
            }
        }
    }

    private void storeNewProducts() {
        String msg = "";
        txn.moveToPosition(-1);
        final ArrayList<ContentValues> newProducts = new ArrayList<>();
        while (txn.moveToNext()) {
            if (txn.getFloat(4) > 0) {
                if (!txn.getString(2).equals("lager")) continue;
                Cursor p = getActivity().getContentResolver().query(
                        Uri.parse("content://org.baobab.foodcoapp/products"),
                        null, "title IS '" + txn.getString(7) +
                                "' AND ROUND(price, 2) = ROUND(" + txn.getFloat(5) + ", 2)", null, null);
                if (p.getCount() == 0) {
                    msg += "\n + " + txn.getString(7) + " " +
                            String.format("%.2f", txn.getFloat(5)) +
                            "€ pro " + txn.getString(6);
                    ContentValues cv = new ContentValues();
                    cv.put("title", txn.getString(7));
                    cv.put("price", txn.getFloat(5));
                    cv.put("unit", txn.getString(6));
                    cv.put("img", txn.getString(8));
                    cv.put("amount", 1);
                    cv.put("tax", -19);
                    newProducts.add(cv);
                }
            }
        }
        if (newProducts.size() > 0) {
            new AlertDialog.Builder(getActivity())
                    .setTitle("Neue Produkte ins Sortiment?")
                    .setMessage(msg)
                    .setPositiveButton(R.string.btn_yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            for (ContentValues v : newProducts) {
                                getActivity().getContentResolver().insert(
                                        Uri.parse("content://org.baobab.foodcoapp/products"), v);
                            }
                            Snackbar.make(getView(), newProducts.size()
                                    + " neue Produkte gespeichert", Snackbar.LENGTH_LONG).show();
                            removeEmptyProducts();
                        }
                    }).setNegativeButton(R.string.btn_no, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    removeEmptyProducts();
                }
            }).show();
        } else {
            removeEmptyProducts();
        }
    }

    private void removeEmptyProducts() {
        String msg = "";
        txn.moveToPosition(-1);
        final ArrayList<ContentValues> emptyProducts = new ArrayList<>();
        while (txn.moveToNext()) {
            if (!txn.getString(2).equals("lager")) continue;
            if (txn.getFloat(4) > 0) continue;
            Cursor stocks = getActivity().getContentResolver().query(
                    Uri.parse("content://org.baobab.foodcoapp/accounts/lager/products"),
                    null, "title IS '" + txn.getString(7) + "' AND rounded = ROUND(" + txn.getFloat(5) + ", 2)", null, null);
            if (stocks.getCount() > 0) {
                stocks.moveToFirst();
                if (stocks.getFloat(4) >= 0.001 && stocks.getFloat(4) + txn.getFloat(4) <= 0) {
                    Cursor p = getActivity().getContentResolver().query(
                            Uri.parse("content://org.baobab.foodcoapp/products"),
                            null, "title IS '" + txn.getString(7) +
                                    "' AND ROUND(price, 2) = ROUND(" + txn.getFloat(5)+ ", 2)", null, null);
                    if (p.getCount() > 0) {
                        p.moveToFirst();
                        msg += "\n - " + txn.getString(7) + " " +
                                String.format("%.2f", txn.getFloat(5)) +
                                "€/" + txn.getString(6) + " -> " +
                                "Lagerbestand wird " + (stocks.getFloat(4) + txn.getFloat(4));
                        ContentValues cv = new ContentValues();
                        cv.put("guid", p.getString(2));
                        cv.put("status", "deleted");
                        emptyProducts.add(cv);
                    }
                }
            }
        }
        if (emptyProducts.size() > 0) {
            new AlertDialog.Builder(getActivity())
                    .setTitle("Produkte aus dem Sortiment nehmen?")
                    .setMessage(msg)
                    .setPositiveButton(R.string.btn_yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            for (ContentValues v : emptyProducts) {
                                getActivity().getContentResolver().insert(
                                        Uri.parse("content://org.baobab.foodcoapp/products"), v);
                            }
                            Snackbar.make(getView(), emptyProducts.size()
                                    + " Produkte entfernt", Snackbar.LENGTH_LONG).show();
                            saveStatus("final", "PowerBuchung:");
                        }
                    }).setNegativeButton(R.string.btn_no, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    saveStatus("final", "PowerBuchung:");
                }
            })
                    .show();
        } else {
            saveStatus("final", "PowerBuchung:");
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }

}
