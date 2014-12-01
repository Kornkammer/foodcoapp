package org.baobab.pos;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;


public class AccountListFragment extends ListFragment
        implements LoaderManager.LoaderCallbacks<Cursor> {

    public AccountListFragment() { }

    @Override
    public View onCreateView(LayoutInflater inf, ViewGroup p, Bundle state) {
        return inf.inflate(R.layout.fragment_account_list, p, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setListAdapter(
                new CursorAdapter(getActivity(), null, true) {

                    @Override
                    public View newView(Context context, Cursor cursor, ViewGroup parent) {
                        return new AccountView(getActivity());
                    }

                    @Override
                    public void bindView(View view, Context context, Cursor cursor) {
                        if (cursor.isNull(3)) {
                            ((AccountView) view).balance.setText("0.00");
                        } else {
                            ((AccountView) view).balance.setText(
                                    String.format("%.2f", cursor.getFloat(3)));
                        }
                        ((AccountView) view).name.setText(cursor.getString(1));
                        ((AccountView) view).id = cursor.getLong(2);
                    }
                }
        );
        registerForContextMenu(getListView());
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        ((AccountsActivity) getActivity()).editAccount(
                Uri.parse("content://org.baobab.pos/accounts/"
                        + ((AdapterView.AdapterContextMenuInfo) menuInfo).id));
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(getActivity(),
                Uri.parse("content://org.baobab.pos/accounts"),
                null, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        ((CursorAdapter) getListAdapter()).swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }

    private class AccountView extends LinearLayout {
        final TextView balance;
        final TextView name;
        long id;

        public AccountView(Context ctx) {
            super(ctx);
            setOrientation(HORIZONTAL);
            View.inflate(ctx, R.layout.account_list_item, this);
            balance = (TextView) findViewById(R.id.balance);
            name = (TextView) findViewById(R.id.name);
        }
    }
}
