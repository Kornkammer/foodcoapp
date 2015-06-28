package org.baobab.foodcoapp;


import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.provider.ContactsContract;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class AccountEditFragment extends Fragment
        implements LoaderManager.LoaderCallbacks<Cursor> {

    public static final int CONTACT = 1;
    private static final int SCAN = 2;

    static AccountEditFragment newInstance() {
        AccountEditFragment f = new AccountEditFragment();
        Bundle b = new Bundle();
        f.setArguments(b);
        return f;
    }

    static AccountEditFragment newInstance(Uri uri) {
        AccountEditFragment f = new AccountEditFragment();
        Bundle b = new Bundle();
        b.putString("uri", uri.toString());
        f.setArguments(b);
        return f;
    }

    @Override
    public View onCreateView(LayoutInflater inf, ViewGroup p, Bundle state) {
        return inf.inflate(R.layout.fragment_account_edit, p, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.contact).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(
                        new Intent(Intent.ACTION_INSERT_OR_EDIT)
                                .setType(ContactsContract.Contacts.CONTENT_ITEM_TYPE)
                        .putExtra("finishActivityOnSaveCompleted", true), CONTACT
                );
            }
        });
        //view.findViewById(R.guid.contact).setOnLongClickListener(new View.OnLongClickListener() {
        //    @Override
        //    public boolean onLongClick(View v) {
        //        startActivityForResult(
        //                new Intent(Intent.ACTION_PICK)
        //                        .setType(ContactsContract.Contacts.CONTENT_TYPE), 0);
        //        return true;
        //    }
        //});
        if (getArguments().containsKey("uri")) {
            getLoaderManager().initLoader(0, null, this);
        }
        getView().findViewById(R.id.scan).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(
                        "com.google.zxing.client.android.SCAN");
                intent.putExtra("SCAN_MODE", "QR_CODE_MODE");
                startActivityForResult(intent, SCAN);
            }
        });
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        final ActionBar actionBar = ((ActionBarActivity) getActivity()).getSupportActionBar();
        menu.findItem(R.id.add).setVisible(false);
        actionBar.setDisplayOptions(
                ActionBar.DISPLAY_SHOW_CUSTOM,
                ActionBar.DISPLAY_SHOW_CUSTOM | ActionBar.DISPLAY_SHOW_HOME
                        | ActionBar.DISPLAY_SHOW_TITLE
        );
        View bar = ((LayoutInflater) actionBar.getThemedContext()
                .getSystemService(Activity.LAYOUT_INFLATER_SERVICE))
                .inflate(R.layout.actionbar_done_discard, null, false);
        actionBar.setCustomView(bar,
                new ActionBar.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT)
        );
        bar.findViewById(R.id.actionbar_cancel).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        getActivity().getSupportFragmentManager().popBackStack();
                    }
                }
        );
        bar.findViewById(R.id.actionbar_done).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        store();
                    }
                }
        );
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK) return;
        switch (requestCode) {
            case CONTACT:
                getArguments().putString("contact",
                        data.getData().toString());
                getLoaderManager().initLoader(1, null, this);
                break;
            case SCAN:
                getArguments().putString("qr", Crypt.hash(
                        data.getStringExtra("SCAN_RESULT"), getActivity()));
                ((Button) getView().findViewById(R.id.scan)).setText("***");
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case 0:
                return new CursorLoader(getActivity(),
                        Uri.parse(getArguments().getString("uri")),
                        null, null, null, null);
            case 1:
                if (getArguments().containsKey("contact")) {
                    return new CursorLoader(getActivity(),
                            Uri.parse(getArguments().getString("contact")),
                            null, null, null, null);
                }
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        data.moveToFirst();
        switch (loader.getId()) {
            case 0:
                getArguments().putString("guid", data.getString(1));
                getActivity().setTitle("Edit " + data.getString(2));
                if (!data.isNull(4)) {
                    getArguments().putString("pin", data.getString(4));
                }
                if (data.isNull(5)) {
                    getArguments().putString("qr", data.getString(5));
                }
                ((TextView) getView().findViewById(R.id.name))
                    .setText(data.getString(2));
                ((TextView) getView().findViewById(R.id.pin))
                    .setText(data.getString(4));
                ((TextView) getView().findViewById(R.id.pin2))
                    .setText(data.getString(4));
                ((Button) getView().findViewById(R.id.scan)).setText("***");

                if (!data.isNull(3)) {
                    getArguments().putString("contact", data.getString(3));
                    getLoaderManager().initLoader(1, null, this);
                }
                if (!data.isNull(1)) {
                    getArguments().putString("parent_guid", data.getString(1));
                    getLoaderManager().initLoader(1, null, this);
                }
                break;
            case 1:
                if (data.getCount() > 0) {
                    ((TextView) getView().findViewById(R.id.contact))
                            .setText(data.getString(data.getColumnIndex(
                                    ContactsContract.Contacts.DISPLAY_NAME)));
                    if (((TextView) getView().findViewById(R.id.name)).getText().toString().equals("")) {
                        ((TextView) getView().findViewById(R.id.name))
                                .setText(data.getString(data.getColumnIndex(
                                        ContactsContract.Contacts.DISPLAY_NAME)));
                    }
                } else {

                }
        }
    }

    public void store() {
        ContentValues values = new ContentValues();
        String pin = ((EditText) getView().findViewById(R.id.pin)).getText().toString();
        String pin2 = ((EditText) getView().findViewById(R.id.pin2)).getText().toString();
        if (pin.equals("") || pin2.equals("")) {
            ((Vibrator) getActivity().getSystemService(Context.VIBRATOR_SERVICE)).vibrate(150);
            Toast.makeText(getActivity(), "Pin brauchts!", Toast.LENGTH_LONG).show();
            return;
        } else if (!pin.equals(pin2)) {
            ((Vibrator) getActivity().getSystemService(Context.VIBRATOR_SERVICE)).vibrate(250);
            Toast.makeText(getActivity(), "pins nicht gleich", Toast.LENGTH_LONG).show();
            return;
        }
        String hash = Crypt.hash(pin, getActivity());
        Log.d("ui", "pin: " + pin);
        Log.d("ui", "hash: " + hash);
        Log.d("ui", "field: " + getArguments().getString("pin"));
        if (getArguments().containsKey("pin") &&
                getArguments().getString("pin") != null &&
                getArguments().getString("pin").equals(pin)) {
            values.put("pin", pin); // keep
        }  else {
            if (lockAccountIfAlreadyTaken(hash)) {
                Toast.makeText(getActivity(), "PIN gibts schon!!!", Toast.LENGTH_LONG).show();
                return;
            }
            values.put("pin", hash);
        }
        if (getArguments().containsKey("guid")) {
            archivePreviousVersions();
            values.put("guid", getArguments().getString("guid"));
        }
        if (getArguments().containsKey("parent_guid")) {
            archivePreviousVersions();
            values.put("parent_guid", getArguments().getString("guid"));
        }
        if (getArguments().containsKey("contact")) {
            values.put("contact", getArguments().getString("contact"));
        }
        String name = ((EditText) getView().findViewById(R.id.name)).getText().toString();
        if (!name.equals("")) {
            values.put("name", name);
        } else {
            values.put("name", values.getAsString("guid"));
        }
        values.put("status", "foo");
        if (getArguments().containsKey("qr") &&
                getArguments().getString("qr") != null) {
            if (lockAccountIfAlreadyTaken(getArguments().getString("qr"))) {
                Toast.makeText(getActivity(), "QR code gibts schon!!!", Toast.LENGTH_LONG).show();
                return;
            }
            values.put("qr", getArguments().getString("qr"));
        }
        getActivity().getContentResolver().insert(
                Uri.parse("content://org.baobab.foodcoapp/accounts/passiva/accounts"),
                values);
        Toast.makeText(getActivity(), "Gespeichert", Toast.LENGTH_SHORT).show();
        getActivity().getSupportFragmentManager().popBackStack();
        return;
    }

    private void archivePreviousVersions() {
        ContentValues v = new ContentValues();
        v.put("status", "archive");
        getActivity().getContentResolver().update(
                Uri.parse("content://org.baobab.foodcoapp/accounts"), v,
                "guid IS ?", new String[]{getArguments().getString("guid")});
    }

    private boolean lockAccountIfAlreadyTaken(String pin) {
        Cursor auth = getActivity().getContentResolver().query(Uri.parse(
                        "content://org.baobab.foodcoapp/legitimate?pin=" + pin),
                null, null, null, null);
        if (auth.getCount() > 0) {
            auth.moveToFirst();
            if (getArguments().containsKey("guid") &&
                auth.getString(3).equals(getArguments().getString("guid"))) {
                return false;
            }
            ContentValues v = new ContentValues();
            v.put("status", "locked");
            getActivity().getContentResolver().update(Uri.parse(
                            "content://org.baobab.foodcoapp/accounts"), v,
                    "_id = " + auth.getLong(0), null);
            ((Vibrator) getActivity().getSystemService(Context.VIBRATOR_SERVICE)).vibrate(150);
            ((Vibrator) getActivity().getSystemService(Context.VIBRATOR_SERVICE)).vibrate(150);
            ((Vibrator) getActivity().getSystemService(Context.VIBRATOR_SERVICE)).vibrate(150);
            return true;
        }
        return false;
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }
}
