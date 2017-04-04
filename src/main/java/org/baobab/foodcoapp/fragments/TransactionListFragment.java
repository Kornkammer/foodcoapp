package org.baobab.foodcoapp.fragments;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.support.v7.app.AlertDialog;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ExpandableListView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.baobab.foodcoapp.AccountActivity;
import org.baobab.foodcoapp.BrowseActivity;
import org.baobab.foodcoapp.R;
import org.baobab.foodcoapp.TraceActivity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TransactionListFragment extends ListFragment
        implements LoaderManager.LoaderCallbacks<Cursor> {

    private boolean showSum = true;

    public static TransactionListFragment newInstance() {
        return newInstance(null);
    }

    public static TransactionListFragment newInstance(Uri uri) {
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
                    sign = cursor.getFloat(8) < 0 ? "-" : "+";
                } else {
                    sign = cursor.getFloat(8) > 0 ? "-" : "+";
                }
                if (!cursor.isNull(4)) {
                    v.comment.setText(cursor.getString(4));
                } else {
                    v.comment.setText("");
                }
                if (showSum) {
                    v.sum.setText(sign + String.format("%.2f", Math.abs(cursor.getFloat(6))));
                }
                v.collapse();
            }
        });
        if (((Uri) getArguments().getParcelable("uri")).getQueryParameter("title") != null) {
            showSum = false;
        }
        getLoaderManager().initLoader(0, null, this);
        return view;
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

    private class TransactionView extends LinearLayout implements View.OnClickListener, View.OnLongClickListener {
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
            findViewById(R.id.container).setOnClickListener(this);
            findViewById(R.id.container).setOnLongClickListener(this);
        }

        public void expand() {
            org.baobab.foodcoapp.view.TransactionView transaction = new org.baobab.foodcoapp.view.TransactionView(getActivity());
            transaction.showImages(true);
            transaction.headersClickable(false);
            final Cursor c = getActivity().getContentResolver().query(
                    Uri.parse("content://org.baobab.foodcoapp/transactions/" + id + "/products"),
                    null, null, null, null);
            transaction.setColumnWidth(R.dimen.column_small);
            transaction.headersClickable(false);
            transaction.showHeaders(true);
            transaction.setOnTitleClick(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    c.moveToPosition((Integer) v.getTag());
                    Toast.makeText(getContext(), c.getString(7), Toast.LENGTH_SHORT).show();
                }
            });
            transaction.setOnTitleLongClick(new View.OnLongClickListener() {

                @Override
                public boolean onLongClick(View v) {
                    c.moveToPosition((Integer) v.getTag());
                    final String title = c.getString(7);
                    final float price = c.getFloat(5);
                    String[] menu = new String[]{"Kontoumsätze", "Trace..."};
                    new AlertDialog.Builder(getContext())
                            .setItems(menu, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    switch (which) {
                                        case 0:
                                            startActivity(new Intent(getContext(), BrowseActivity.class)
                                                    .setData(Uri.parse("content://org.baobab.foodcoapp/transactions")
                                                            .buildUpon().appendQueryParameter("title", title)
                                                            .appendQueryParameter("price",
                                                                    String.format(Locale.ENGLISH, "%.2f", price)).build()));
                                            break;
                                        case 1:
                                            startActivity(new Intent(getActivity(), TraceActivity.class)
                                                    .setData(Uri.parse("content://org.baobab.foodcoapp/transactions")
                                                            .buildUpon().appendQueryParameter("title", title)
                                                            .appendQueryParameter("price", String.valueOf(price)).build()));
                                            break;
                                    }
                                }
                            }).show();
                    return true;
                }
            });
            transaction.populate(c);
            LayoutParams lp = new LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = 96;
            addView(transaction, lp);
            expanded = true;
        }

        public void collapse() {
            if (expanded) {
                if (getChildCount() > 1) {
                    removeViewAt(1);
                }
            }
            expanded = false;
        }
        @Override
        public void onClick(View v) {
            if (expanded) {
                collapse();
            } else {
                expand();
            }
        }

        @Override
        public boolean onLongClick(View v) {
            startActivity(new Intent(Intent.ACTION_EDIT, Uri.parse(
                    "content://org.baobab.foodcoapp/transactions/" + id)));
            return true;
        }
    }

}
