package org.baobab.foodcoapp.fragments;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import org.baobab.foodcoapp.AccountActivity;
import org.baobab.foodcoapp.ProductEditActivity;
import org.baobab.foodcoapp.R;

public class TransactionEditFragment extends TransactionFragment {

    @Override
    public View onCreateView(LayoutInflater flate, ViewGroup p, Bundle state) {
        View view = super.onCreateView(flate, p, state);
        transaction.setOnTitleClick(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                int position = (Integer) v.getTag();
                String accountGuid = "lager";
                String id = "";
                if (position > -1) {
                    txn.moveToPosition((Integer) v.getTag());
                    id = "/" + txn.getInt(0);
                    accountGuid =  txn.getString(2);
                }
                startActivity(new Intent(getActivity(), ProductEditActivity.class)
                        .setData(getActivity().getIntent().getData().buildUpon()
                                .appendEncodedPath("products" + id).build())
                                .putExtra("account_guid", accountGuid));
            }
        });
        return view;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        super.onLoadFinished(loader, data);
        transaction.headersClickable(true);
        TextView ok = ((TextView) getActivity().findViewById(R.id.sum));
        TextView header = ((TextView) getActivity().findViewById(R.id.header));
        if (sum < 0.01 && sum > -0.01) {
            header.setText("");
            header.setBackgroundResource(R.drawable.background_translucent);
            ok.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_ok, 0, 0, 0);
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
    }

    @Override
    public void onClick(View v) {
        if (sum < -0.01) {
            ((Vibrator) getActivity().getSystemService(Context.VIBRATOR_SERVICE)).vibrate(200);
            MediaPlayer.create(getActivity(), R.raw.error_3).start();
            Toast.makeText(getActivity(), "Wechselgeld " + sum, Toast.LENGTH_SHORT).show();
        } else if (sum > 0.01) {
            ((Vibrator) getActivity().getSystemService(Context.VIBRATOR_SERVICE)).vibrate(300);
            MediaPlayer.create(getActivity(), R.raw.error_4).start();
            Toast.makeText(getActivity(), "Noch " + sum + " offen!", Toast.LENGTH_LONG).show();
        } else {
            if (transactionValid()) {
                ((Vibrator) getActivity().getSystemService(Context.VIBRATOR_SERVICE)).vibrate(100);
                MediaPlayer.create(getActivity(), R.raw.chaching).start();
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        MediaPlayer.create(getActivity(), R.raw.yay).start();
                    }
                }, 700);
                saveStatus("final", "PowerBuchung:");
                Toast.makeText(getActivity(), "Verbucht :-)", Toast.LENGTH_SHORT).show();
                ((AccountActivity) getActivity()).resetTransaction();
                load();
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }

}
