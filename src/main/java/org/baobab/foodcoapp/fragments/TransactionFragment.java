package org.baobab.foodcoapp.fragments;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.baobab.foodcoapp.AccountActivity;
import org.baobab.foodcoapp.BrowseActivity;
import org.baobab.foodcoapp.CheckoutActivity;
import org.baobab.foodcoapp.LegitimateActivity;
import org.baobab.foodcoapp.R;
import org.baobab.foodcoapp.view.TransactionView;

public class TransactionFragment extends Fragment
        implements LoaderManager.LoaderCallbacks<Cursor>, View.OnClickListener {

    TransactionView transaction;
    ScrollView scrollView;
    Cursor txn;
    float sum;
    boolean editable;

    @Override
    public View onCreateView(LayoutInflater flate, ViewGroup p, Bundle state) {
        scrollView = (ScrollView) flate.inflate(R.layout.fragment_transaction, null, false);
        transaction = (TransactionView) scrollView.findViewById(R.id.transaction_view);
        return scrollView;
    }

    public void enableEdit(final boolean allowNegative) {
        transaction.setOnAmountClick(new NumberEditListener() {
            @Override
            String text() {
                return "Wie viel " + txn.getString(6) + " " + txn.getString(7);
            }

            @Override
            int inputType() {
                if (!txn.isNull(6) && txn.getString(6).equals(getString(R.string.piece))) {
                    return InputType.TYPE_CLASS_NUMBER;
                } else {
                    return InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL;
                }
            }

            @Override
            float quantity(float number) {
                return number;
            }

            @Override
            String chars() {
                if (!txn.isNull(6) && txn.getString(6).equals(getString(R.string.piece))) {
                    if (allowNegative) {
                        return "-0123456789";
                    } else {
                        return "0123456789";
                    }
                } else {
                    if (allowNegative) {
                        return "-0123456789.,";
                    } else {
                        return "0123456789.,";
                    }
                }
            }
        });
        transaction.setOnSumClick(new NumberEditListener() {
            @Override
            String text() {
                return txn.getString(7) + " für wie viel Cash?";
            }

            @Override
            int inputType() {
                return InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL;
            }

            @Override
            float quantity(float number) {
                if (!txn.isNull(6) && txn.getString(6).equals(getString(R.string.piece))) {
                    return (int) (number / txn.getFloat(5));
                } else {
                    return number / txn.getFloat(5);
                }
            }

            @Override
            String chars() {
                return "-0123456789.,";
            }
        });
        transaction.headersClickable(true);
        editable = true;
    }

    public void setUneditable() {
        transaction.headersClickable(false);
        transaction.setOnAmountClick(null);
        transaction.setOnTitleClick(null);
        transaction.setOnSumClick(null);
        editable = false;
    }

    abstract class NumberEditListener implements View.OnClickListener {

        abstract String text();

        abstract int inputType();

        abstract float quantity(float number);

        abstract String chars();

        @Override
        public void onClick(final View v) {
            if ((Integer) v.getTag() < 0) return;
            txn.moveToPosition((Integer) v.getTag());
            getFragmentManager().beginTransaction()
                    .replace(R.id.container, new NumberDialogFragment(
                            text(), txn.getFloat(4), inputType(), chars()) {
                        @Override
                        public void onNumber(float number) {
                            ContentValues cv = new ContentValues();
                            cv.put("quantity", quantity(number));
                            getActivity().getContentResolver()
                                    .update(getActivity().getIntent().getData().buildUpon()
                                            .appendEncodedPath("products/" + txn.getLong(0))
                                            .build(), cv, null, null);
                        }
                    }, "amount").addToBackStack("amount").commit();
        }
    }

    public void load() {
        if (getActivity() != null) {
            getActivity().getSupportLoaderManager().initLoader(1, null, this);
        }
    }

    public void reload() {
        if (getActivity() != null) {
            getActivity().getSupportLoaderManager().destroyLoader(1);
            getActivity().getSupportLoaderManager().restartLoader(1, null, this);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if (getActivity() != null && getActivity().getIntent().getData() != null) {
            return new CursorLoader(getActivity(),
                    getActivity().getIntent().getData()
                            .buildUpon().appendPath("products").build(),
                    null, null, null, null);
        } else {
            Log.e(AccountActivity.TAG, "how is this possible?");
            return null;
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (getFragmentManager().getBackStackEntryCount() > 0) {
            new Handler().post(new Runnable() {
                @Override
                public void run() {
                    getFragmentManager().popBackStack();
                }
            });
            ((InputMethodManager) getActivity()
                    .getSystemService(Context.INPUT_METHOD_SERVICE))
                    .hideSoftInputFromWindow(getView().getWindowToken(), 0);
        }
        scrollView.post(new Runnable() {
            @Override
            public void run() {
                scrollView.fullScroll(ScrollView.FOCUS_DOWN);
            }
        });
        txn = data;
        transaction.populate(data);
        transaction.headersClickable(false);
        Cursor s = getActivity().getContentResolver().query(
                getActivity().getIntent().getData().buildUpon()
                        .appendEncodedPath("sum").build(), null, null, null, null);
        s.moveToFirst();
        sum = - s.getFloat(2);
        TextView ok = ((TextView) getActivity().findViewById(R.id.sum));
        ok.setText("Bezahlen " + String.format("%.2f", sum));
        ok.setOnClickListener(this);
        ok.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (transactionValid()) {
                    startActivity(new Intent(getActivity(),
                            LegitimateActivity.class)
                            .setData(getActivity().getIntent().getData())
                            .putExtra("SCAN", true)
                            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET));
                }
                return true;
            }
        });
    }

    @Override
    public void onClick(View v) {
        if (transactionValid()) {
            startActivityForResult(new Intent(getActivity(), LegitimateActivity.class)
                    .setData(getActivity().getIntent().getData())
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET), 42);
        }
    }

    boolean transactionValid() {
        String msg = "";
        Cursor t = txn;
        t.moveToPosition(-1);
        if (t.getCount() == 0) return false;
        for (int i = 0; i < t.getCount(); i++) {
            t.moveToPosition(i);
            int factor = 1;
            System.out.println(t.getString(10) + " -> " + t.getString(2) + " -> " + t.getString(12) + " foo " + t.getInt(3) + " : " + t.getString(7));
            if (t.getString(2).equals("bank") || t.getString(2).equals("kasse") || t.getString(2).equals("spenden")) {
                if (t.getInt(3) != 1 || !t.getString(7).equals("Cash") || t.getFloat(5) != 1) {
                    msg += " * auf Konto " + t.getString(12) + " kann nur Cash verbucht werden\n";
                }
            } else if (t.getString(10).equals("mitglieder")) {
                factor = -1;
                if (t.getInt(3) != 2 || !t.getString(7).equals("Korns") || t.getFloat(5) != 1) {
                    msg += " * auf Konto " + t.getString(12) + " kann nur Korns verbucht werden\n";
                }
            } else if (t.getString(2).equals("einlagen") || t.getString(2).equals("beiträge")) {
                factor = -1;
                Cursor members = getActivity().getContentResolver().query(
                        Uri.parse("content://org.baobab.foodcoapp/accounts/mitglieder/accounts"),
                        null, "status is NOT 'deleted' AND name IS '" + t.getString(7) + "'", null, null);
                if (members.getCount() != 1) {
                    msg += " # kein Mitglied '" + t.getString(7) + "' gefunden\n";
                }
            }
            Cursor stocks = getActivity().getContentResolver().query(
                    Uri.parse("content://org.baobab.foodcoapp/accounts/" + t.getString(2) + "/products"),
                    null, "title IS '" + t.getString(7) + "' AND rounded = ROUND(" + t.getFloat(5) + ", 2)", null, null);
            if (t.getString(10).equals("passiva")){
                factor = -1;
            }
            if (stocks.getCount() > 0) {
                stocks.moveToFirst();
                if (factor * stocks.getInt(4) >= 0 && factor * stocks.getInt(4) + factor * t.getInt(4) < 0) {
                    msg += " - Nicht genug " + t.getString(7) + " auf " + t.getString(12) + "\n";
                }
            } else if (factor * t.getInt(4) < 0) {
                msg += " - Nicht genug " + t.getString(7) + " auf " + t.getString(12) + "\n";
            }
        }
        if (msg.length() > 0) {
            if (PreferenceManager.getDefaultSharedPreferences(getActivity())
                    .getBoolean("allow_negative_stocks", false)) {
                msg = "Wirklich ins Minus buchen?\n\n" + msg;
                new AlertDialog.Builder(getActivity())
                        .setMessage(msg)
                        .setPositiveButton("Ja, Genau!", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                ((Vibrator) getActivity().getSystemService(Context.VIBRATOR_SERVICE)).vibrate(100);
                                MediaPlayer.create(getActivity(), R.raw.yay).start();
                                MediaPlayer.create(getActivity(), R.raw.chaching).start();
                                saveStatus("final", "Einkauf:");
                                Toast.makeText(getActivity(), "Verbucht :-)", Toast.LENGTH_SHORT).show();
                                ((CheckoutActivity) getActivity()).resetTransaction();
                                load();
                            }
                        }).setNegativeButton("Abbrechen", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                }).show();
                Toast.makeText(getActivity(), msg, Toast.LENGTH_LONG).show();
                MediaPlayer.create(getActivity(), R.raw.error_1).start();
                return false;
            } else {
                msg = "Keine Buchung ins Minus erlaubt!\n\n" + msg;
                ((Vibrator) getActivity().getSystemService(Context.VIBRATOR_SERVICE)).vibrate(500);
                Toast.makeText(getActivity(), msg, Toast.LENGTH_LONG).show();
                MediaPlayer.create(getActivity(), R.raw.error_2).start();
            }
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == getActivity().RESULT_OK && requestCode == 42) {
            if (transactionValid()) {
                if (saveStatus("final", "Einkauf:")) {
                    startActivity(new Intent(getActivity(), BrowseActivity.class)
                            .setData(Uri.parse("content://org.baobab.foodcoapp/accounts/" +
                                    data.getStringExtra("guid") + "/transactions")));
                }
            }
        }
    }

    boolean saveStatus(String status, String comment) {
        ContentValues cv = new ContentValues();
        cv.put("status", status);
        if (((CheckoutActivity) getActivity()).comment != null &&
                !((CheckoutActivity) getActivity()).comment.equals("")) {
            cv.put("comment", ((CheckoutActivity) getActivity()).comment);
        } else {
            cv.put("comment", comment);
        }
        if (((CheckoutActivity) getActivity()).time != 0) {
            cv.put("start", ((CheckoutActivity) getActivity()).time);
        } else {
            cv.put("start", System.currentTimeMillis());
        }
        cv.put("stop", System.currentTimeMillis());
        int result = getActivity().getContentResolver().update(
                getActivity().getIntent().getData(), cv, null, null);
        if (result > 0) {
            ((Vibrator) getActivity().getSystemService(Context.VIBRATOR_SERVICE)).vibrate(100);
            MediaPlayer.create(getActivity(), R.raw.chaching).start();
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    MediaPlayer.create(getActivity(), R.raw.yay).start();
                }
            }, 1000);
            Toast.makeText(getActivity(), "Verbucht :-)", Toast.LENGTH_SHORT).show();
            ((CheckoutActivity) getActivity()).resetTransaction();
            return true;
        } else {
            Toast.makeText(getActivity(), "Transaktion gibts schon!", Toast.LENGTH_LONG).show();
        }
        return false;
    }

    private String emptyStocks(String account) {
        Cursor empty_stocks = getActivity().getContentResolver().query(
                Uri.parse("content://org.baobab.foodcoapp/accounts/" + account + "/products"),
                null, "stock < 0", null, null);
        String msg = "";
        for (int i = 0; i < empty_stocks.getCount(); i++) {
            empty_stocks.moveToPosition(i);
            msg += " - nicht genug " + empty_stocks.getString(7) + " in " + empty_stocks.getString(2) + "\n";
        }
        return msg;
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }
}