package org.baobab.pos;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import android.widget.Toast;

public class TransactionFragment extends Fragment
        implements LoaderManager.LoaderCallbacks<Cursor> {

    private TransactionView transaction;

    @Override
    public View onCreateView(LayoutInflater flate, ViewGroup p, Bundle state) {
        View frame = flate.inflate(R.layout.fragment_transaction, null, false);
        transaction = (TransactionView) frame.findViewById(R.id.transaction);
        transaction.setOnAmountClick(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                getFragmentManager().beginTransaction()
                        .replace(v.getId(), new NumberDialogFragment(
                                "Wie viel?", (String) v.getTag()) {
                            @Override
                            public void onNumber(float number) {
                                ContentValues cv = new ContentValues();
                                cv.put("quantity", number);
                                getActivity().getContentResolver().update(
                                        getActivity().getIntent().getData().buildUpon()
                                            .appendEncodedPath("products/" + v.getId())
                                            .build(), cv, null, null);
                            }
                        }, "amount")
                        .addToBackStack("amount").commitAllowingStateLoss();
            }
        });
        return frame;
    }

    public void load() {
        getActivity().getSupportLoaderManager().restartLoader(1, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(getActivity(),
                getActivity().getIntent().getData(),
                null, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (getFragmentManager().getBackStackEntryCount() > 0) {
            new Handler().post(new Runnable() {
                @Override
                public void run() {
                    getFragmentManager().popBackStack();
                }
            });
            ((InputMethodManager) getActivity()
                    .getSystemService(Context.INPUT_METHOD_SERVICE))
                    .hideSoftInputFromWindow(getView().getWindowToken(), 0);
        }
        transaction.populate(data);
        Cursor s = getActivity().getContentResolver().query(
                getActivity().getIntent().getData().buildUpon()
                        .appendEncodedPath("sum").build(), null, null, null, null);
        s.moveToFirst();
        final float sum = - s.getFloat(2);
        TextView ok = ((TextView) ((FragmentActivity) getActivity())
                .findViewById(R.id.sum));
        TextView header = ((TextView) ((FragmentActivity) getActivity())
                .findViewById(R.id.header));
        if (sum == 0.0) {
            header.setText("");
            header.setBackgroundResource(R.drawable.background_translucent);
            ok.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_launcher, 0, 0, 0);
            ok.setText("");
        } else {
            ok.setCompoundDrawablesWithIntrinsicBounds(0,0,0,0);
            ok.setText(String.format("%.2f", sum));
            if (sum < 0) {
                header.setText("Wechselgeld");
                header.setBackgroundResource(R.drawable.background_red);
            } else {
                header.setText("zu Bezahlen");
                header.setTextColor(getResources().getColor(R.color.medium_green));
                header.setBackgroundResource(R.drawable.background_green);
            }
        }
        ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (sum <= 0.0) {
                    MediaPlayer.create(getActivity(), R.raw.yay).start();
                    MediaPlayer.create(getActivity(), R.raw.chaching).start();
                    ContentValues cv = new ContentValues();
                    cv.put("status", "final");
                    cv.put("stop", System.currentTimeMillis());
                    getActivity().getContentResolver().update(
                            getActivity().getIntent().getData(), cv, null, null);
                    ((PosActivity) getActivity()).resetTransaction();
                    load();
                    if (sum == 0.0) {
                        Toast.makeText(getActivity(), "Verbucht :-)", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getActivity(), "Wechselgeld " + sum, Toast.LENGTH_SHORT).show();
                    }
                } else if (sum > 0) {
                    Toast.makeText(getActivity(), "Noch " + sum + " offen!", Toast.LENGTH_LONG).show();
                } else {

                }
            }
        });
    }


    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }

}
