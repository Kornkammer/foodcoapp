package org.baobab.pos;

import android.database.Cursor;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.GridLayout;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class TransactionFragment extends Fragment
        implements LoaderManager.LoaderCallbacks<Cursor> {

    private float sum;
    private TransactionView transaction;

    @Override
    public View onCreateView(LayoutInflater flate, ViewGroup p, Bundle state) {
        View frame = flate.inflate(R.layout.fragment_transaction, null, false);
        transaction = (TransactionView) frame.findViewById(R.id.transaction);
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
        transaction.populate(data);
    }


    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }

}
