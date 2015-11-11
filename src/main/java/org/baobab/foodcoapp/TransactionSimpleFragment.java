package org.baobab.foodcoapp;

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
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import android.widget.Toast;

public class TransactionSimpleFragment extends Fragment
        implements LoaderManager.LoaderCallbacks<Cursor> {

    private TransactionView transaction;
    private View scrollView;
    private Cursor txn;

    @Override
    public View onCreateView(LayoutInflater flate, ViewGroup p, Bundle state) {
        scrollView = flate.inflate(R.layout.fragment_transaction, null, false);
        transaction = (TransactionView) scrollView.findViewById(R.id.transaction);
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
        });
        return scrollView;
    }

    abstract class NumberEditListener implements View.OnClickListener {

        abstract String text();

        abstract int inputType();

        abstract float quantity(float number);

        @Override
        public void onClick(final View v) {
            txn.moveToPosition((Integer) v.getTag());
            getFragmentManager().beginTransaction()
                    .replace(R.id.container, new NumberDialogFragment(
                            text(), txn.getFloat(4), inputType()) {
                        @Override
                        public void onNumber(float number) {
                            ContentValues cv = new ContentValues();
                            cv.put("quantity", quantity(number));
                            getActivity().getContentResolver()
                                    .update(getActivity().getIntent().getData().buildUpon()
                                            .appendEncodedPath("products/" + txn.getLong(0))
                                            .build(), cv, null, null);
                        }
                    }, "edit").addToBackStack("amount").commitAllowingStateLoss();
        }
    }

    public void load() {
        getActivity().getSupportLoaderManager().restartLoader(1, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(getActivity(),
                getActivity().getIntent().getData(),
                null, null, null, null);
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
        txn = data;
        transaction.populate(data);
        transaction.headersClickable(false);
        Cursor s = getActivity().getContentResolver().query(
                getActivity().getIntent().getData().buildUpon()
                        .appendEncodedPath("sum").build(), null, null, null, null);
        s.moveToFirst();
        final float sum = - s.getFloat(2);
        TextView ok = ((TextView) getActivity().findViewById(R.id.sum));
        ok.setText("Bezahlen " + String.format("%.2f", sum));
        ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (transactionValid()) {
                    startActivityForResult(new Intent(getActivity(),
                            LegitimateActivity.class)
                            .setData(getActivity().getIntent().getData())
                            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET), 42);
                }
            }
        });
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

    private boolean transactionValid() {
        Cursor t = getActivity().getContentResolver().query(
                getActivity().getIntent().getData(), null, null, null, null);
        String msg = "";
        if (t.getCount() == 0) return false;
        for (int i = 0; i < t.getCount(); i++) {
            t.moveToPosition(i);
            Cursor stocks = getActivity().getContentResolver().query(
                    Uri.parse("content://org.baobab.foodcoapp/accounts/" + t.getString(2) + "/products"),
                    null, "title IS '" + t.getString(7) + "'", null, null);
            int factor = 1;
            if (t.getString(10).equals("passiva")) {
                factor = -1;
            }
            if (stocks.getCount() > 0) {
                stocks.moveToFirst();
                if (factor * stocks.getInt(4) >= 0 && factor * stocks.getInt(4) + factor * t.getInt(4) < 0) {
                    msg += " - Nicht genug " + t.getString(12) + " auf " + t.getString(2) + "\n";
                }
            } else if (factor * t.getInt(4) < 0) {
                msg += " - Nicht genug " + t.getString(12) + " auf " + t.getString(2) + "\n";
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
                                MediaPlayer.create(getActivity(), R.raw.yay).start();
                                MediaPlayer.create(getActivity(), R.raw.chaching).start();
                                saveStatus("final");
                                Toast.makeText(getActivity(), "Verbucht :-)", Toast.LENGTH_SHORT).show();
                                ((PosActivity) getActivity()).resetTransaction();
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
            MediaPlayer.create(getActivity(), R.raw.chaching).start();
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    MediaPlayer.create(getActivity(), R.raw.yay).start();
                }
            }, 1000);
            saveStatus("final");
            Toast.makeText(getActivity(), "Verbucht :-)", Toast.LENGTH_SHORT).show();
            ((PoSimpleActivity) getActivity()).resetTransaction();
            load();
            startActivity(new Intent(getActivity(), TransactionsActivity.class)
                    .setData(Uri.parse("content://org.baobab.foodcoapp/accounts/" +
                            data.getStringExtra("guid") + "/transactions")));
        }
    }

    private void saveStatus(String status) {
        ContentValues cv = new ContentValues();
        cv.put("status", status);
        cv.put("stop", System.currentTimeMillis());
        getActivity().getContentResolver().update(
                getActivity().getIntent().getData(), cv, null, null);
    }

    private String emptyStocks(String account) {
        Cursor empty_stocks = getActivity().getContentResolver().query(
                Uri.parse("content://org.baobab.foodcoapp/accounts/" + account + "/products"),
                null, "stock < 0", null, null);
        System.out.println("empty " + empty_stocks.getCount());
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
