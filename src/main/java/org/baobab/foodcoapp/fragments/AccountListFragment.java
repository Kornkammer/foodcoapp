package org.baobab.foodcoapp.fragments;

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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.baobab.foodcoapp.BalanceActivity;
import org.baobab.foodcoapp.R;
import org.baobab.foodcoapp.TransactionView;
import org.baobab.foodcoapp.TransactionsActivity;


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
                return null;
            }

            @Override
            protected View newGroupView(Context context, Cursor cursor, boolean isExpanded, ViewGroup parent) {
                return new AccountView(getActivity());
            }

            @Override
            protected void bindGroupView(View view, Context context, Cursor cursor, boolean isExpanded) {
                ((AccountView) view).populate(cursor);
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
                    null, null, null, "_id DESC");
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (loader.getId() < 0) {
            adapter.changeCursor(data);
        } else {
            adapter.setChildrenCursor(loader.getId(), data);
            list.expandGroup(loader.getId());
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
        int pos;

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
            if (cursor.isNull(3)) {
                balance.setText("0.00");
            } else {
                balance.setText(String.format("%.2f", invert * cursor.getFloat(3)));
            }
            if (cursor.getString(4).equals("mitglieder")) {
                name.setText("   " + cursor.getString(1));
            } else {
                name.setText(cursor.getString(1));
            }
            guid = cursor.getString(2);
            if (guid.equals("mitglieder")) {
                ImageView icn = (ImageView) findViewById(R.id.indicator);
                icn.setVisibility(VISIBLE);
                if (expanded) {
                    icn.setImageResource(R.drawable.ic_menu_more);
                } else {
                    icn.setImageResource(R.drawable.ic_launcher);
                }
            } else {
                collapse();
            }
            pos = cursor.getPosition();
            id = cursor.getLong(0);
        }

        public void expand() {
            if (guid.equals("mitglieder")) {
                Bundle b = new Bundle();
                b.putString("group_guid", guid);
                getLoaderManager().restartLoader(pos, b, AccountListFragment.this);
                expanded = true;
                return;
            }
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
                if (guid.equals("mitglieder")) {
                    list.collapseGroup(pos);
                } else {
                    if (getChildCount() > 0) {
                        removeViewAt(1);
                    }
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
            String[] menu;
            if (editable && id > 150) {
                menu = new String[]{"Kontoumsätze", "Editieren"};
            } else if (guid.equals("mitglieder")) {
                menu = new String[]{"Kontoumsätze", "Mitglied hinzufügen"};
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
                                    if (guid.equals("mitglieder")) {
                                        getActivity().getSupportFragmentManager() .beginTransaction()
                                                .replace(R.id.container, AccountEditFragment.newInstance())
                                                .addToBackStack("add")
                                                .commit();
                                    } else {
                                        ((BalanceActivity) getActivity()).editAccount(
                                                Uri.parse("content://org.baobab.foodcoapp/accounts/" + guid));
                                    }
                                    break;
                            }
                        }
                    }).show();
            return false;
        }
    }
}
