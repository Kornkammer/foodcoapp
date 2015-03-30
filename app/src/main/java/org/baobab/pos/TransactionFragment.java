package org.baobab.pos;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;

public class TransactionFragment extends Fragment
        implements LoaderManager.LoaderCallbacks<Cursor> {

    private TransactionView transaction;

    @Override
    public View onCreateView(LayoutInflater flate, ViewGroup p, Bundle state) {
        View frame = flate.inflate(R.layout.fragment_transaction, null, false);
        transaction = (TransactionView) frame.findViewById(R.id.transaction);
        transaction.setOnAmountClick(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                getFragmentManager().beginTransaction()
                        .replace(v.getId(), new NumberDialogFragment(
                                "Wie viel?", (String) v.getTag()) {
                            @Override
                            public void onNumber(float number) {
                                ContentValues cv = new ContentValues();
                                cv.put("quantity", number);
                                getActivity().getContentResolver().update(
                                        getActivity().getIntent().getData().buildUpon()
                                            .appendEncodedPath("products/" + v.getId())
                                            .build(), cv, null, null);
                            }
                        }, "amount")
                        .addToBackStack("amount").commitAllowingStateLoss();
            }
        });
        return frame;
    }

    public void load() {
        getActivity().getSupportLoaderManager().initLoader(1, null, this);
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
        transaction.populate(data);
    }


    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }

}
