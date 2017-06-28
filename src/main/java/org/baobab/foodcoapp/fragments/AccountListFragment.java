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
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.baobab.foodcoapp.AccountActivity;
import org.baobab.foodcoapp.BalanceActivity;
import org.baobab.foodcoapp.R;
import org.baobab.foodcoapp.TraceActivity;
import org.baobab.foodcoapp.io.BackupExport;
import org.baobab.foodcoapp.view.TransactionView;
import org.baobab.foodcoapp.BrowseActivity;

import java.util.Date;
import java.util.Locale;


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
        if (data.isClosed()) return;
        if (loader.getId() < 0) {
            adapter.changeCursor(data);
        } else {
            adapter.setChildrenCursor(loader.getId(), data);
            list.expandGroup(loader.getId());
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        if (loader.getId() < 0) {
            adapter.changeCursor(null);
        } else {
            adapter.setChildrenCursor(loader.getId(), null);
        }
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
            if (cursor.isClosed()) return;
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
            final Cursor products = getActivity().getContentResolver().query(
                    Uri.parse("content://org.baobab.foodcoapp/accounts/" + guid + "/products"),
                    null, null, null, null);
            transaction.setColumnWidth(R.dimen.column_small);
            transaction.headersClickable(false);
            transaction.showHeaders(false);
            transaction.showImages(false);
            transaction.setOnTitleClick(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    products.moveToPosition((Integer) v.getTag());
                    Toast.makeText(getContext(), products.getString(7), Toast.LENGTH_SHORT).show();
                }
            });
            transaction.setOnTitleLongClick(new OnLongClickListener() {

                @Override
                public boolean onLongClick(View v) {
                    products.moveToPosition((Integer) v.getTag());
                    final String account = products.getString(2);
                    final String title = products.getString(7);
                    final float amount = products.getFloat(4);
                    final float price = products.getFloat(5);
                    final String unit = products.getString(6);
                    String[] menu = new String[]{"Kontoumsätze", "Umbuchen", "Trace..."};
                    new AlertDialog.Builder(getActivity())
                            .setItems(menu, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    switch (which) {
                                        case 0:
                                            startActivity(new Intent(getActivity(), BrowseActivity.class)
                                                    .setData(Uri.parse("content://org.baobab.foodcoapp/transactions")
                                                    .buildUpon().appendQueryParameter("title", title)
                                                            .appendQueryParameter("price",
                                                                    String.format(Locale.ENGLISH, "%.2f", price)).build()));
                                            break;
                                        case 1:
                                            Intent intent = new Intent(getActivity(), AccountActivity.class)
                                                    .putExtra("title", title).putExtra("account", account)
                                                    .putExtra("amount", amount).putExtra("price", price)
                                                    .putExtra("unit", unit);
                                            Cursor prods = getContext().getContentResolver().query(
                                                    Uri.parse("content://org.baobab.foodcoapp/transactions/" +
                                                            products.getLong(1) + "/products"), null, null, null, null);
                                            while (prods.moveToNext()) {
                                                if (prods.getString(2).equals("bank")) {
                                                    Cursor txn = getContext().getContentResolver().query(
                                                            Uri.parse("content://org.baobab.foodcoapp/transactions/" +
                                                            prods.getLong(1)), null, null, null, null);
                                                    txn.moveToFirst();
                                                    long time = txn.getLong(2);
                                                    intent.putExtra("time", time); // Zeitpunkt "Vereinnahmumg"
                                                }
                                            }
                                            startActivity(intent);
                                            break;
                                        case 2:
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
            transaction.setOnAmountClick(new OnClickListener() {

                @Override
                public void onClick(final View v) {
                    products.moveToPosition((Integer) v.getTag());
                    final String account = products.getString(2);
                    final String title = products.getString(7);
                    final float amount = products.getFloat(4);
                    final float price = products.getFloat(5);
                    final String unit = products.getString(6);
                    final String img = products.getString(8);
                    AlertDialog number = new AlertDialog.Builder(getActivity())
                            .setView(R.layout.fragment_dialog_number)
                            .setPositiveButton("Korrektur buchen", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    float n = Float.valueOf(
                                            ((EditText) ((AlertDialog) dialog).findViewById(R.id.number))
                                                    .getText().toString().replace(",", "."));
                                    startActivity(new Intent(getActivity(), AccountActivity.class)
                                            .putExtra("title", title).putExtra("account", account)
                                            .putExtra("amount", amount -n).putExtra("price", price)
                                            .putExtra("unit", unit).putExtra("img", img));
                                }
                            }).show();
                    ((TextView) number.findViewById(R.id.message)).setText("Auf Ist setzen");
                }
            });
            transaction.setDecimals(1);
            transaction.populate(products);
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
                    if (getChildCount() > 1) {
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
                menu = new String[]{ "Umsätze", "Einzahlungen", "Einkäufe", "Editieren"};
            } else if (guid.equals("mitglieder")) {
                menu = new String[]{ "Umsätze", "Einzahlungen", "Einkäufe", "Mitglied hinzufügen"};
            } else {
                menu = new String[]{ "Umsätze", "Zugänge", "Abgänge"};
            }
            new AlertDialog.Builder(getActivity())
                    .setItems(menu, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            switch (which) {
                                case 0:
                                    startActivity(new Intent(getActivity(), BrowseActivity.class)
                                            .setData(Uri.parse("content://org.baobab.foodcoapp/accounts/" +
                                                    guid + "/transactions")));
                                    break;
                                case 1:
                                    startActivity(new Intent(getActivity(), BrowseActivity.class)
                                            .setData(Uri.parse("content://org.baobab.foodcoapp/accounts/" +
                                                    guid + "/transactions?" + (invert == 1? "debit=true" : "credit=true"))));
                                    break;
                                case 2:
                                    startActivity(new Intent(getActivity(), BrowseActivity.class)
                                            .setData(Uri.parse("content://org.baobab.foodcoapp/accounts/" +
                                                    guid + "/transactions?" + (invert == 1? "credit=true" : "debit=true"))));
                                    break;
                                case 3:
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
