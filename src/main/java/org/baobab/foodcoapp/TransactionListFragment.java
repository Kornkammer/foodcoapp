package org.baobab.foodcoapp;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;

public class TransactionListFragment extends ListFragment
        implements LoaderManager.LoaderCallbacks<Cursor> {

    static TransactionListFragment newInstance() {
        return newInstance(null);
    }

    static TransactionListFragment newInstance(Uri uri) {
        TransactionListFragment f = new TransactionListFragment();
        Bundle b = new Bundle();
        if (uri != null) {
            b.putParcelable("uri", uri);
        }
        f.setArguments(b);
        return f;
    }

    public TransactionListFragment() { }

    static SimpleDateFormat df = new SimpleDateFormat("dd/MM/yy");
    static SimpleDateFormat tf = new SimpleDateFormat("HH:mm");

    @Override
    public View onCreateView(LayoutInflater inf, ViewGroup p, Bundle state) {
        View view = inf.inflate(R.layout.fragment_transaction_list, p, false);

        setListAdapter(new CursorAdapter(getActivity(), null, true) {

                    @Override
                    public View newView(Context context, Cursor cursor, ViewGroup parent) {
                        return new TransactionView(getActivity());
                    }

                    @Override
                    public void bindView(View view, Context context, Cursor cursor) {
                        TransactionView v = (TransactionView) view;
                        v.id = cursor.getLong(0);
                        v.date.setText(df.format(new Date(cursor.getLong(2))));
                        v.time.setText(tf.format(new Date(cursor.getLong(2))));
                        v.who.setText(cursor.getString(3));
                        String sign;
                        if (cursor.getString(9).equals("aktiva")) {
                            if (cursor.getInt(8) < 0) {
                                sign = "-";
                                v.comment.setText("Einlagerung");
                            } else {
                                sign = "+";
                                v.comment.setText("Barverkauf");
                            }
                        } else {
                            if (cursor.getInt(8) < 0) {
                                sign = "+";
                                v.comment.setText("Einzahlung");
                            } else {
                                sign = "-";
                                v.comment.setText("Einkaufung");
                            }
                        }
                        if (!cursor.isNull(4)) {
                            v.comment.append("\n" + cursor.getString(4));
                        }
                        v.sum.setText(sign + String.format("%.2f", cursor.getFloat(6)));
                    }
                }
        );
        getLoaderManager().initLoader(0, null, this);
        return view;
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(getActivity(),
                (Uri) getArguments().getParcelable("uri"),
                null, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        ((CursorAdapter) getListAdapter()).swapCursor(data);
        getListView().setSelection(data.getCount() - 1);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }

    private class TransactionView extends LinearLayout implements View.OnClickListener {
        long id;
        final TextView date;
        final TextView time;
        final TextView who;
        final TextView sum;
        final TextView comment;
        private boolean expanded;

        public TransactionView(Context ctx) {
            super(ctx);
            setOrientation(VERTICAL);
            View.inflate(ctx, R.layout.view_transaction_list_item, this);
            date = (TextView) findViewById(R.id.date);
            time = (TextView) findViewById(R.id.time);
            who = (TextView) findViewById(R.id.who);
            sum = (TextView) findViewById(R.id.sum);
            comment = (TextView) findViewById(R.id.comment);
            setOnClickListener(this);
            setClickable(true);
        }

        @Override
        public void onClick(View v) {
            if (expanded) {
                removeViewAt(1);
            } else {
                org.baobab.foodcoapp.TransactionView transaction = new org.baobab.foodcoapp.TransactionView(getActivity());
                transaction.showImages(true);
                transaction.headersClickable(false);
                Cursor c = getActivity().getContentResolver().query(
                        Uri.parse("content://org.baobab.foodcoapp/transactions/" + id),
                        null, null, null, null);
                transaction.populate(c);
                ((LinearLayout) v).addView(transaction);
            }
            expanded = !expanded;
        }

    }

}
