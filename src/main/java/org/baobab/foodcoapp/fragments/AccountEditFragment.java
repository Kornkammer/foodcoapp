package org.baobab.foodcoapp.fragments;


import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
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
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.baobab.foodcoapp.R;
import org.baobab.foodcoapp.io.BackupExport;
import org.baobab.foodcoapp.util.Barcode;
import org.baobab.foodcoapp.util.Crypt;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Random;

public class AccountEditFragment extends Fragment
        implements LoaderManager.LoaderCallbacks<Cursor> {

    public static final int CONTACT = 1;
    private static final int SCAN = 0;
    private float soll;
    private BackupExport.Cons paid;
    private int fee;
    private long begin;

    public static AccountEditFragment newInstance() {
        AccountEditFragment f = new AccountEditFragment();
        Bundle b = new Bundle();
        b.putString("parent_guid", "mitglieder");
        f.setArguments(b);
        return f;
    }

    public static AccountEditFragment newInstance(Uri uri) {
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
        LinearLayout mem = ((LinearLayout) getView().findViewById(R.id.memberships));
        if (getArguments().containsKey("uri")) {
            getLoaderManager().initLoader(0, null, this);
        } else {
            String guid = generateGUID();
            view.findViewById(R.id.guid).setEnabled(true);
            ((EditText) view.findViewById(R.id.guid)).setText(guid);
            getArguments().putLong("created_at", System.currentTimeMillis());
            getArguments().putLong("fee", 0);
            TextView r = new TextView(getActivity());
            r.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.font_size_large));
            r.setText(R.string.monthly_fee);
            mem.addView(r);
        }
        mem.setOnClickListener(new View.OnClickListener() {
            public int year;
            public int month;
            public int day;

            @Override
            public void onClick(View v) {
                final View dia = getActivity().getLayoutInflater()
                        .inflate(R.layout.dialog_membership, null, false);
                final AlertDialog dialog = new AlertDialog.Builder(getActivity())
                        .setView(dia)
                        .show();
                final Calendar c = Calendar.getInstance();
                day = c.get(Calendar.DAY_OF_MONTH);
                month = c.get(Calendar.MONTH);
                year = c.get(Calendar.YEAR);
                final TextView date = (TextView) dia.findViewById(R.id.since);
                date.setText("ab " + new SimpleDateFormat("dd/MM/yyyy").format(c.getTimeInMillis()));
                date.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        new DatePickerDialog(getActivity(), new DatePickerDialog.OnDateSetListener() {
                            @Override
                            public void onDateSet(DatePicker view, int y, int m, int d) {
                                day = c.get(Calendar.DAY_OF_MONTH);
                                month = c.get(Calendar.MONTH);
                                year = c.get(Calendar.YEAR);
                                c.set(Calendar.DAY_OF_MONTH, d);
                                c.set(Calendar.MONTH, m);
                                c.set(Calendar.YEAR, y);
                                date.setText("ab " + new SimpleDateFormat("dd/MM/yyyy").format(c.getTimeInMillis()));
                                if (((Button) dia.findViewById(R.id.ok)).getText()
                                        .equals(getString(R.string.terminate))) {
                                    int days = Math.round(((float) (c.getTimeInMillis() - begin)) / 86400000);
                                    ((TextView) dia.findViewById(R.id.unit)).setText(BackupExport.computeBalance(paid.sum
                                            - (soll + days * Math.max(0, fee) * 12f/365), fee));
                                }
                            }
                        }, year, month, day).show();
                    }
                });
                dia.findViewById(R.id.ok).setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        if (c.getTimeInMillis() < getArguments().getLong("created_at")) {
                            ((Vibrator) getActivity().getSystemService(Context.VIBRATOR_SERVICE)).vibrate(500);
                            Snackbar.make(getView(), "Datum muss später als " +
                                    new SimpleDateFormat("dd/MM/yyyy", Locale.GERMAN)
                                            .format(getArguments().getLong("created_at")) + " sein!",
                                    Snackbar.LENGTH_LONG).show();
                            return;
                        }
                        getArguments().putLong("created_at", c.getTimeInMillis());
                        if (((Button) v).getText().equals(getString(R.string.change))) {
                            getArguments().putInt("fee", Integer.valueOf(((EditText)
                                    dia.findViewById(R.id.fee)).getText().toString()));
                        } else if (((Button) v).getText().equals(getString(R.string.terminate))) {
                            getArguments().putInt("fee", -1);
                            getArguments().putString("status", "deleted");
                        }
                        if (store()) {
                            dialog.dismiss();
                            getLoaderManager().restartLoader(0, null, AccountEditFragment.this);
                        }
                    }
                });
                dia.findViewById(R.id.terminate).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (!getArguments().containsKey("guid")) {
                            ((Vibrator) getActivity().getSystemService(Context.VIBRATOR_SERVICE)).vibrate(500);
                            Snackbar.make(getView(), "Mitglied gibts noch gar nicht ", Snackbar.LENGTH_LONG).show();
                            return;
                        }
                        if (((Button) v).getText().equals(getString(R.string.terminate))) {
                            dia.findViewById(R.id.fee).setVisibility(View.GONE);
                            ((Button) dia.findViewById(R.id.ok)).setText(R.string.terminate);
                            ((Button) dia.findViewById(R.id.terminate)).setText(R.string.btn_no);
                            ((TextView) dia.findViewById(R.id.message)).setText(R.string.terminate_membership);
                            int days = Math.round(((float) (c.getTimeInMillis() - begin)) / 86400000);
                            ((TextView) dia.findViewById(R.id.unit)).setText(BackupExport.computeBalance(paid.sum
                                    - (soll + days * Math.max(0, fee) * 12f/365), fee));
                        } else {
                            dialog.dismiss();
                        }

                    }
                });
            }
        });
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
    public void onResume() {
        super.onResume();
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (getResources().getConfiguration().hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_NO) {
            Toast.makeText(getActivity(), "Switch hardware keyboard OFF", Toast.LENGTH_LONG).show();
            imm.showInputMethodPicker();
        } else {
//            imm.showSoftInput(getView().findViewById(R.id.name), InputMethodManager.SHOW_FORCED);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        ((InputMethodManager) getActivity()
                .getSystemService(Context.INPUT_METHOD_SERVICE))
                .hideSoftInputFromWindow(((TextView) getView().findViewById(R.id.name)).getWindowToken(), 0);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        final ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        actionBar.show();
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
                        if (store()) {
                            getActivity().getSupportFragmentManager().popBackStack();
                            ((AppCompatActivity) getActivity()).getSupportActionBar().hide();
                        }
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
        switch (loader.getId()) {
            case 0:
                LinearLayout mem = ((LinearLayout) getView().findViewById(R.id.memberships));
                mem.removeAllViews();
                soll = 0;
                begin = 0;
                fee = 0;
                while (data.moveToNext()) {
                    if (data.getInt(10) == 0) continue;
                    if (fee >= 0) {
                        int days = Math.round(((float) (data.getLong(9) - begin)) / 86400000);
                        soll += days * Math.max(0, fee) * 12f/365;
                    }
                    View r = getActivity().getLayoutInflater().inflate(R.layout.membership_row, null, false);
                    mem.addView(r);
                    begin = data.getLong(9);
                    ((TextView) r.findViewById(R.id.created_at))
                            .setText(getString(R.string.since, new SimpleDateFormat("dd/MM/yyyy",
                                    Locale.GERMAN).format(begin)));
                    if (!data.isNull(7) && data.getString(11).equals("deleted")) {
                        ((TextView) r.findViewById(R.id.fee)).setText("nicht mehr");
                    } else {
                        fee = data.getInt(10);
                        ((TextView) r.findViewById(R.id.fee))
                                .setText(getString(R.string.fee_per_month, fee));
                    }
                }
                data.moveToLast();
                if (mem.getChildCount() == 0) {
                    TextView r = new TextView(getActivity());
                    r.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.font_size_large));
                    r.setText(R.string.monthly_fee);
                    mem.addView(r);
                } else if (fee >= 0) {
                    int days = Math.round(((float) (System.currentTimeMillis() - begin)) / 86400000);
                    paid = BackupExport.getContribution(getActivity(),
                            "", data.getString(2), "beiträge");
                    System.out.println(data.getString(2) + " soll " + soll + " and paid " + paid.sum );
                    TextView b = new TextView(getActivity());
                    b.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.font_size_medium));
                    b.setText("Beiträge gezahlt " + String.format(Locale.ENGLISH, "%.2f", paid.sum) +
                            "€  -  " + String.format(Locale.ENGLISH, "%.2f", soll) + "€ soll");
                    mem.addView(b);
                    TextView r = new TextView(getActivity());
                    r.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.font_size_large));
                    r.setText(BackupExport.computeBalance(paid.sum
                            - (soll + days * Math.max(0, fee) * 12f/365), fee));
                    mem.addView(r);
                }
                final ScrollView scrollView = (ScrollView) getView().findViewById(R.id.scroll);
                scrollView.post(new Runnable() {
                    @Override
                    public void run() {
                        scrollView.fullScroll(ScrollView.FOCUS_DOWN);
                    }
                });
                getArguments().putLong("created_at", data.getLong(9));
                getArguments().putInt("fee", data.getInt(10));
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
                    getArguments().putString("parent_guid", data.getString(8));
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

    public boolean store() {
        ContentValues values = new ContentValues();
        String guid = ((EditText) getView().findViewById(R.id.guid)).getText().toString();
        if (getArguments().containsKey("guid")) {
            archivePreviousVersions();
            values.put("guid", getArguments().getString("guid"));
            values.put("created_at", getArguments().getLong("created_at"));
        } else {
            if (exists(guid)) {
                MediaPlayer.create(getActivity(), R.raw.error_4).start();
                ((Vibrator) getActivity().getSystemService(Context.VIBRATOR_SERVICE)).vibrate(500);
                Snackbar.make(getView(), "MitgliedsNr vergeben", Snackbar.LENGTH_LONG).show();
                return false;
            } else {
                values.put("guid", guid);
                values.put("created_at", System.currentTimeMillis());
            }
        }
        String name = ((EditText) getView().findViewById(R.id.name)).getText().toString();
        Cursor accounts = getActivity().getContentResolver().query(Uri.parse(
                "content://org.baobab.foodcoapp/accounts/passiva/accounts"), null,
                "name IS '"+name+"'", null, null);
        if (accounts.getCount() > 0) {
            accounts.moveToFirst();
            if (!accounts.getString(2).equals(getArguments().getString("guid"))) {
                Snackbar.make(getView().findViewById(R.id.name),
                        "Name vergeben!", Snackbar.LENGTH_LONG).show();
                return false;
            }
        }
        if (!name.equals("")) {
            values.put("name", name);
        } else {
            values.put("name", guid);
        }
        String pin = ((EditText) getView().findViewById(R.id.pin)).getText().toString();
        String pin2 = ((EditText) getView().findViewById(R.id.pin2)).getText().toString();
        if (pin.equals("") || pin2.equals("")) {
            MediaPlayer.create(getActivity(), R.raw.error_1).start();
            ((Vibrator) getActivity().getSystemService(Context.VIBRATOR_SERVICE)).vibrate(150);
            Snackbar.make(getView().findViewById(R.id.pin),
                    "Pin brauchts!", Snackbar.LENGTH_LONG).show();
            return false;
        } else if (!pin.equals(pin2)) {
            MediaPlayer.create(getActivity(), R.raw.error_2).start();
            ((Vibrator) getActivity().getSystemService(Context.VIBRATOR_SERVICE)).vibrate(250);
            Snackbar.make(getView().findViewById(R.id.pin2),
                    "pins nicht gleich", Snackbar.LENGTH_LONG).show();
            return false;
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
                return false;
            }
            values.put("pin", hash);
        }
        if (getArguments().containsKey("parent_guid")) {
            values.put("parent_guid", getArguments().getString("parent_guid"));
        }
        if (getArguments().containsKey("contact")) {
            values.put("contact", getArguments().getString("contact"));
        }
        if (getArguments().containsKey("status")) {
            values.put("status", getArguments().getString("status"));
        } else {
            values.put("status", "foo");
        }
        if (getArguments().containsKey("qr") &&
                getArguments().getString("qr") != null) {
            if (lockAccountIfAlreadyTaken(getArguments().getString("qr"))) {
                MediaPlayer.create(getActivity(), R.raw.error_4).start();
                ((Vibrator) getActivity().getSystemService(Context.VIBRATOR_SERVICE)).vibrate(500);
                Snackbar.make(getView(), "QR code gibts schon!!!", Snackbar.LENGTH_LONG).show();
                return false;
            }
            values.put("qr", getArguments().getString("qr"));
        }
        values.put("fee", getArguments().getInt("fee"));
        values.put("last_modified", System.currentTimeMillis());
        Uri uri = getActivity().getContentResolver().insert(
                Uri.parse("content://org.baobab.foodcoapp/accounts"), values);
        getArguments().putString("uri", uri.toString());
        Snackbar.make(getView(), "Gespeichert", Snackbar.LENGTH_SHORT).show();
        return true;
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
