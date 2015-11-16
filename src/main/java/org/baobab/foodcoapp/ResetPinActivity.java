package org.baobab.foodcoapp;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

public class ResetPinActivity extends AppCompatActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean("rotate_screen", false)) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
        getSupportActionBar().setHomeButtonEnabled(true);
        setContentView(R.layout.activity_reset_pin);
        ((EditText) findViewById(R.id.name)).setOnEditorActionListener(
                new EditText.OnEditorActionListener() {
                    @Override
                    public boolean onEditorAction(TextView view, int id, KeyEvent e) {
                        if (id == EditorInfo.IME_ACTION_SEARCH ||
                                id == EditorInfo.IME_ACTION_DONE ||
                                e.getAction() == KeyEvent.ACTION_DOWN &&
                                        e.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                            String name = ((EditText) view).getText().toString();
                            Cursor accounts = getContentResolver().query(Uri.parse(
                                    "content://org.baobab.foodcoapp/accounts/mitglieder/accounts"),
                                    null, "name IS '" + name + "'", null, null);
                            if (accounts.getCount() > 0) {
                                accounts.moveToFirst();
                                startActivity(new Intent(ResetPinActivity.this,
                                        BalanceActivity.class).setData(Uri.parse(
                                        "content://org.baobab.foodcoapp/accounts/"
                                                + accounts.getString(2))));
                                finish();
                            } else {
                                Snackbar.make(findViewById(R.id.name), "Not Found!",
                                        Snackbar.LENGTH_LONG).show();
                            }
                        }
                        return false;
                    }
                });
    }
}
