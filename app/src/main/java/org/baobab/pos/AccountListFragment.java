package org.baobab.pos;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CursorTreeAdapter;
import android.widget.ExpandableListView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;


public class AccountListFragment extends Fragment
        implements LoaderManager.LoaderCallbacks<Cursor> {

    private Uri uri;
    private CursorTreeAdapter adapter;

    public AccountListFragment() { }

    @Override
    public View onCreateView(LayoutInflater inf, ViewGroup p, Bundle state) {
        return inf.inflate(R.layout.fragment_account_list, p, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        adapter = new CursorTreeAdapter(null, getActivity(), true) {

            @Override
            protected Cursor getChildrenCursor(Cursor groupCursor) {
                Bundle b = new Bundle();
                b.putLong("group_id", groupCursor.getLong(0));
                getLoaderManager().restartLoader(groupCursor.getPosition(), b, AccountListFragment.this);
                return null;
            }

            @Override
            protected View newGroupView(Context context, Cursor cursor, boolean isExpanded, ViewGroup parent) {
                return new AccountView(getActivity());
            }

            @Override
            protected void bindGroupView(View view, Context context, Cursor cursor, boolean isExpanded) {
                if (cursor.isNull(3)) {
                    ((AccountView) view).balance.setText("0.00");
                } else {
                    ((AccountView) view).balance.setText(
                            String.format("%.2f", cursor.getFloat(3)));
                }
                ((AccountView) view).name.setText(cursor.getString(1));
                ((AccountView) view).id = cursor.getLong(2);
            }

            @Override
            protected View newChildView(Context context, Cursor cursor, boolean isLastChild, ViewGroup parent) {
                return new AccountView(getActivity());
            }

            @Override
            protected void bindChildView(View view, Context context, Cursor cursor, boolean isLastChild) {
                ((AccountView) view).populate(cursor);
                ((AccountView) view).expand();
            }
        };
        ((ExpandableListView) view.findViewById(android.R.id.list)).setAdapter(adapter);
//        registerForContextMenu(getListView());

        ((ExpandableListView) view.findViewById(android.R.id.list))
                .setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        Log.d("POS", "foo");
                    }
                });

    }

    public void setUri(String uri) {
        this.uri = Uri.parse(uri);
        getLoaderManager().initLoader(-1, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if (id < 0) {
            return new CursorLoader(getActivity(), uri, null, null, null, null);
        } else {
            return new CursorLoader(getActivity(), Uri.parse(
                    "content://org.baobab.pos/accounts/" +
                            args.getLong("group_id") + "/accounts"),
                    null, null, null, null);
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (loader.getId() < 0) {
            adapter.changeCursor(data);
        } else {
            if (data.getCount() == 0) {
                ((AccountView)((ExpandableListView) getView()
                        .findViewById(android.R.id.list))
                        .getChildAt(loader.getId())).expand();
            } else {
                adapter.setChildrenCursor(loader.getId(), data);
            }
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        ((AccountActivity) getActivity()).editAccount(
                Uri.parse("content://org.baobab.pos/accounts/"
                        + ((AdapterView.AdapterContextMenuInfo) menuInfo).id));
    }

//    @Override
//    public void onListItemClick(ListView l, View v, int position, long id) {
//        super.onListItemClick(l, v, position, id);
//    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }

    private class AccountView extends LinearLayout {
        private boolean expanded;
        final TextView balance;
        final TextView name;
        long id;

        public AccountView(Context ctx) {
            super(ctx);
            setOrientation(VERTICAL);
            View.inflate(ctx, R.layout.account_list_item, this);
            balance = (TextView) findViewById(R.id.balance);
            name = (TextView) findViewById(R.id.name);
        }

        public void expand() {
            setClickable(true);
            setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (expanded) {
                       removeViewAt(1);
                    } else {
                        TransactionView transaction = new TransactionView(getActivity());
                        Cursor c = getActivity().getContentResolver().query(
                                Uri.parse("content://org.baobab.pos/accounts/" + id + "/products"),
                                null, null, null, null);
                        transaction.populate(c);
                        ((LinearLayout) v).addView(transaction);
                    }
                    expanded = !expanded;
                }
            });
        }

        public void populate(Cursor cursor) {
            id = cursor.getLong(2);
            if (cursor.isNull(3)) {
                balance.setText("0.00");
            } else {
                balance.setText(String.format("%.2f", cursor.getFloat(3)));
            }
            name.setText(cursor.getString(1));
        }
    }
}
