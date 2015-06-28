package org.baobab.foodcoapp;


import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public abstract class NumberDialogFragment extends DialogFragment {

    private final String msg;
    private final String value;

    public NumberDialogFragment(String msg, String value) {
        this.msg = msg;
        this.value = value;
    }

    @Override
    public View onCreateView(LayoutInflater flate, ViewGroup container, Bundle savedInstanceState) {
        View frame = flate.inflate(R.layout.fragment_dialog_number, null, false);
        return frame;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ((TextView) view.findViewById(R.id.message)).setText(msg);
        EditText number = (EditText) view.findViewById(R.id.number);
        number.setText("" + Math.abs(Float.valueOf(value)));
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
        ((InputMethodManager) getActivity()
                .getSystemService(Context.INPUT_METHOD_SERVICE))
                .showSoftInput(number, InputMethodManager.SHOW_FORCED);
    }

    private void finish() {
        EditText number = (EditText) getView().findViewById(R.id.number);
        try {
            float n = Float.valueOf(number.getText().toString());
            if (!value.startsWith("-")) {
                n = n * -1;
            }
            if (Math.abs(n) > 1000) {
                Toast.makeText(getActivity(), "So viel gibts ja gar nicht!", Toast.LENGTH_LONG).show();
                return;
            }
            onNumber(n);
        } catch (NumberFormatException e) {
            Log.d("System.err", e.getMessage());
        }
        getFragmentManager().popBackStack();
        ((InputMethodManager) getActivity()
                .getSystemService(Context.INPUT_METHOD_SERVICE))
                .hideSoftInputFromWindow(number.getWindowToken(), 0);
    }

    public abstract void onNumber(float number);
}
