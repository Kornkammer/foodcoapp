package org.baobab.foodcoapp.fragments;


import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.method.DigitsKeyListener;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.baobab.foodcoapp.R;

public abstract class NumberDialogFragment extends DialogFragment {

    private EditText number;
    private final String msg;
    private final float value;
    private final int inputType;

    public NumberDialogFragment(String msg, float value, int inputType) {
        this.msg = msg;
        this.value = value;
        this.inputType = inputType;
    }

    @Override
    public View onCreateView(LayoutInflater flate, ViewGroup container, Bundle savedInstanceState) {
        return flate.inflate(R.layout.fragment_dialog_number, null, false);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ((TextView) view.findViewById(R.id.message)).setText(msg);
        number = (EditText) view.findViewById(R.id.number);
        if (value % 1 == 0) {
            number.setText(String.valueOf((int) Math.abs(value)));
        } else {
            number.setText(String.format("%.3f", Math.abs(value)));
        }
        number.setInputType(inputType);
        number.setKeyListener(DigitsKeyListener.getInstance("-0123456789.,"));
        number.selectAll();
        number.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                finish();
                return true;
            }
        });
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        number.requestFocus();
    }

    @Override
    public void onResume() {
        super.onResume();
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (getResources().getConfiguration().hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_NO) {
            Toast.makeText(getActivity(), "Switch hardware keyboard OFF", Toast.LENGTH_LONG).show();
            imm.showInputMethodPicker();
        } else {
            imm.showSoftInput(number, InputMethodManager.SHOW_FORCED);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        ((InputMethodManager) getActivity()
                .getSystemService(Context.INPUT_METHOD_SERVICE))
                .hideSoftInputFromWindow(number.getWindowToken(), 0);
    }

    private void finish() {
        try {
            float n = Float.valueOf(number.getText().toString().replace(",", "."));
            if (value < 0) {
                n = n * -1;
            }
            if (Math.abs(n) > 1000) {
                Toast.makeText(getActivity(), "So viel gibts ja gar nicht!", Toast.LENGTH_LONG).show();
                return;
            }
            n = (float) (Math.round(n * 1000) / 1000.0d);
            onNumber(n);
        } catch (NumberFormatException e) {
            Log.d("System.err", e.getMessage());
        }
        getFragmentManager().popBackStack();
    }

    public abstract void onNumber(float number);
}
