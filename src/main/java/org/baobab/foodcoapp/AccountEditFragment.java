package org.baobab.foodcoapp;


import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.provider.ContactsContract;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.util.Random;

public class AccountEditFragment extends Fragment
        implements LoaderManager.LoaderCallbacks<Cursor> {

    public static final int CONTACT = 1;
    private static final int SCAN = 0;

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
        } else {
            String guid = generateGUID();
            ((EditText) view.findViewById(R.id.guid)).setText(guid);
            view.findViewById(R.id.guid).setEnabled(true);
        }
        getView().findViewById(R.id.scan).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Barcode.scan(AccountEditFragment.this, "QR_CODE_MODE");
            }
        });
        setHasOptionsMenu(true);
    }

    public String generateGUID() {
        String guid = String.valueOf(rand());
        if (exists(guid)) {
            return generateGUID();
        } else {
            return guid;
        }
    }

    private boolean exists(String guid) {
        Cursor a =  getActivity().getContentResolver().query(Uri.parse(
                        "content://org.baobab.foodcoapp/accounts/" + guid),
                        null, null, null, null);
        return a.getCount() > 0;
    }

    public int rand() {
        Random r = new Random( System.currentTimeMillis() );
        return (1 + r.nextInt(2)) * 10000 + r.nextInt(10000);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        final ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
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
                ((EditText) getView().findViewById(R.id.guid)).setText(data.getString(1));
                getActivity().setTitle("Edit " + data.getString(2));
                if (!data.isNull(4)) {
                    getArguments().putString("pin", data.getString(4));
                }
                if (data.isNull(5)) {
                    getArguments().putString("qr", data.getString(5));
                    ((Button) getView().findViewById(R.id.scan)).setText("***");
                }
                ((TextView) getView().findViewById(R.id.name))
                    .setText(data.getString(2));
                ((TextView) getView().findViewById(R.id.pin))
                    .setText(data.getString(4));
                ((TextView) getView().findViewById(R.id.pin2))
                    .setText(data.getString(4));

                if (!data.isNull(3)) {
                    getArguments().putString("contact", data.getString(3));
                    getLoaderManager().initLoader(1, null, this);
                }
                if (!data.isNull(1)) {
                    getArguments().putString("parent_guid", data.getString(1));
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
        String name = ((EditText) getView().findViewById(R.id.name)).getText().toString();
        Cursor accounts = getActivity().getContentResolver().query(Uri.parse(
                "content://org.baobab.foodcoapp/accounts/passiva/accounts"), null,
                "name IS '"+name+"'", null, null);
        if (accounts.getCount() > 0) {
            accounts.moveToFirst();
            if (!accounts.getString(2).equals(getArguments().getString("guid"))) {
                Snackbar.make(getView().findViewById(R.id.name),
                        "Name vergeben!", Snackbar.LENGTH_LONG).show();
                return;
            }
        }
        if (!name.equals("")) {
            values.put("name", name);
        } else {
            values.put("name", values.getAsString("guid"));
        }
        String pin = ((EditText) getView().findViewById(R.id.pin)).getText().toString();
        String pin2 = ((EditText) getView().findViewById(R.id.pin2)).getText().toString();
        if (pin.equals("") || pin2.equals("")) {
            MediaPlayer.create(getActivity(), R.raw.error_1).start();
            ((Vibrator) getActivity().getSystemService(Context.VIBRATOR_SERVICE)).vibrate(150);
            Snackbar.make(getView().findViewById(R.id.pin),
                    "Pin brauchts!", Snackbar.LENGTH_LONG).show();
            return;
        } else if (!pin.equals(pin2)) {
            MediaPlayer.create(getActivity(), R.raw.error_2).start();
            ((Vibrator) getActivity().getSystemService(Context.VIBRATOR_SERVICE)).vibrate(250);
            Snackbar.make(getView().findViewById(R.id.pin2),
                    "pins nicht gleich", Snackbar.LENGTH_LONG).show();
            return;
        }
        String hash = Crypt.hash(pin, getActivity());
        if (getArguments().containsKey("pin") &&
                getArguments().getString("pin") != null &&
                getArguments().getString("pin").equals(pin)) {
            values.put("pin", pin); // keep
        }  else {
            if (lockAccountIfAlreadyTaken(hash)) {
                MediaPlayer.create(getActivity(), R.raw.error_3).start();
                ((Vibrator) getActivity().getSystemService(Context.VIBRATOR_SERVICE)).vibrate(1000);
                Snackbar.make(getView(), "PIN gibts schon!!!", Snackbar.LENGTH_LONG).show();
                return;
            }
            values.put("pin", hash);
        }
        if (getArguments().containsKey("guid")) {
            archivePreviousVersions();
            values.put("guid", getArguments().getString("guid"));
        } else {
            String guid = ((EditText) getView().findViewById(R.id.guid)).getText().toString();
            if (exists(guid)) {
                MediaPlayer.create(getActivity(), R.raw.error_4).start();
                ((Vibrator) getActivity().getSystemService(Context.VIBRATOR_SERVICE)).vibrate(500);
                Snackbar.make(getView(), "MitgliedsNr vergeben", Snackbar.LENGTH_LONG).show();
                return;
            } else {
                values.put("guid", guid);
            }
        }
        if (getArguments().containsKey("parent_guid")) {
            values.put("parent_guid", getArguments().getString("guid"));
        }
        if (getArguments().containsKey("contact")) {
            values.put("contact", getArguments().getString("contact"));
        }
        values.put("status", "foo");
        if (getArguments().containsKey("qr") &&
                getArguments().getString("qr") != null) {
            if (lockAccountIfAlreadyTaken(getArguments().getString("qr"))) {
                MediaPlayer.create(getActivity(), R.raw.error_4).start();
                ((Vibrator) getActivity().getSystemService(Context.VIBRATOR_SERVICE)).vibrate(500);
                Snackbar.make(getView(), "QR code gibts schon!!!", Snackbar.LENGTH_LONG).show();
                return;
            }
            values.put("qr", getArguments().getString("qr"));
        }
        getActivity().getContentResolver().insert(
                Uri.parse("content://org.baobab.foodcoapp/accounts/passiva/accounts"),
                values);
        Snackbar.make(getView(), "Gespeichert", Snackbar.LENGTH_SHORT).show();
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
