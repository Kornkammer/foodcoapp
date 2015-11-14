package org.baobab.foodcoapp;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorTreeAdapter;
import android.widget.ExpandableListView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;


public class AccountListFragment extends Fragment
        implements LoaderManager.LoaderCallbacks<Cursor> {

    private Uri uri;
    private int invert = 1;
    private boolean editable;
    private ExpandableListView list;
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
            public long getGroupId(int groupPosition) {
                return groupPosition;
            }

            @Override
            protected Cursor getChildrenCursor(Cursor groupCursor) {
                Bundle b = new Bundle();
                b.putString("group_guid", groupCursor.getString(2));
                getLoaderManager().restartLoader(groupCursor.getPosition(), b, AccountListFragment.this);
                return null;
            }

            @Override
            protected View newGroupView(Context context, Cursor cursor, boolean isExpanded, ViewGroup parent) {
                return new AccountView(getActivity());
            }

            @Override
            protected void bindGroupView(View view, Context context, Cursor cursor, boolean isExpanded) {
                AccountView a = ((AccountView) view);
                if (cursor.isNull(3)) {
                    a.balance.setText("0.00");
                } else {
                    a.balance.setText(
                            String.format("%.2f", invert * cursor.getFloat(3)));
                }
                a.name.setText(cursor.getString(1));
                a.guid = cursor.getString(2);
                a.id = cursor.getLong(0);
                a.collapse();
            }

            @Override
            protected View newChildView(Context context, Cursor cursor, boolean isLastChild, ViewGroup parent) {
                return new AccountView(getActivity());
            }

            @Override
            protected void bindChildView(View view, Context context, Cursor cursor, boolean isLastChild) {
                ((AccountView) view).populate(cursor);
            }
        };
        list = (ExpandableListView) view.findViewById(android.R.id.list);
        list.setGroupIndicator(null);
        list.setAdapter(adapter);
    }

    public AccountListFragment setUri(String uri, boolean invert) {
        this.invert = invert? -1 : 1;
        this.uri = Uri.parse(uri);
        getLoaderManager().initLoader(-1, null, this);
        return this;
    }

    public AccountListFragment setEditable(boolean editable) {
        this.editable = editable;
        return this;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if (id < 0) {
            return new CursorLoader(getActivity(), uri, null, null, null, null);
        } else {
            return new CursorLoader(getActivity(), Uri.parse(
                    "content://org.baobab.foodcoapp/accounts/" +
                            args.getString("group_guid") + "/accounts"),
                    null, null, null, null);
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (loader.getId() < 0) {
            adapter.changeCursor(data);
        } else {
//            if (data.getCount() == 0) {
//                ((AccountView) list.getChildAt(loader.getId()
//                        - list.getFirstVisiblePosition())).expand();
//            } else {
                adapter.setChildrenCursor(loader.getId(), data);
//            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }

    private class AccountView extends LinearLayout implements View.OnClickListener, View.OnLongClickListener {
        private boolean expanded;
        final TextView balance;
        final TextView name;
        String guid;
        long id;

        public AccountView(Context ctx) {
            super(ctx);
            setOrientation(VERTICAL);
            View.inflate(ctx, R.layout.view_account_list_item, this);
            balance = (TextView) findViewById(R.id.balance);
            name = (TextView) findViewById(R.id.name);
            findViewById(R.id.container)
                    .setBackgroundResource(R.drawable.background_translucent);
            findViewById(R.id.container).setOnClickListener(this);
            findViewById(R.id.container).setOnLongClickListener(this);
        }

        public void populate(Cursor cursor) {
            guid = cursor.getString(2);
            if (cursor.isNull(3)) {
                balance.setText("0.00");
            } else {
                balance.setText(String.format("%.2f", cursor.getFloat(3)));
            }
            name.setText(cursor.getString(1));
        }

        public void expand() {
            TransactionView transaction = new TransactionView(getActivity());
            Cursor c = getActivity().getContentResolver().query(
                    Uri.parse("content://org.baobab.foodcoapp/accounts/" + guid + "/products"),
                    null, null, null, null);
            transaction.setColumnWidth(R.dimen.column_medium);
            transaction.headersClickable(false);
            transaction.showHeaders(false);
            transaction.showImages(false);
            transaction.setOnTitleClick(new OnClickListener() {

                @Override
                public void onClick(final View v) {
                    String[] menu = new String[]{"Kontoumsätze", "Umbuchen"};
                    new AlertDialog.Builder(getActivity())
                            .setItems(menu, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    switch (which) {
                                        case 0:
                                            startActivity(new Intent(getActivity(), TransactionsActivity.class)
                                                    .setData(Uri.parse("content://org.baobab.foodcoapp/transactions/" + v.getId())));
                                            break;
                                        case 1:
                                            Toast.makeText(getActivity(), "Ja des wiad spannend!", Toast.LENGTH_LONG).show();
                                            break;
                                    }
                                }
                            }).show();
                }
            });
            transaction.populate(c);
            LayoutParams lp = new LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.topMargin = -23;
            lp.bottomMargin = 42;
            addView(transaction, lp);
            expanded = true;
            findViewById(R.id.container).setClickable(true);
            findViewById(R.id.container).setOnClickListener(this);
        }

        public void collapse() {
            if (expanded) {
                removeViewAt(1);
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
            String[] menu;
            if (editable && id > 150) {
                menu = new String[]{"Kontoumsätze", "Editieren"};
            } else {
                menu = new String[]{"Kontoumsätze"};
            }
            new AlertDialog.Builder(getActivity())
                    .setItems(menu, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            switch (which) {
                                case 0:
                                    startActivity(new Intent(getActivity(), TransactionsActivity.class)
                                            .setData(Uri.parse("content://org.baobab.foodcoapp/accounts/" +
                                                    guid + "/transactions")));
                                    break;
                                case 1:
                                    ((AccountActivity) getActivity()).editAccount(
                                    Uri.parse("content://org.baobab.foodcoapp/accounts/" + guid));
                                    break;
                            }
                        }
                    }).show();
            return false;
        }
    }
}
