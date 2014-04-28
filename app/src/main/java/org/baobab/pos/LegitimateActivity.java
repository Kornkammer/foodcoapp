package org.baobab.pos;

import android.annotation.TargetApi;
import android.content.Intent;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class LegitimateActivity extends FragmentActivity {

    private MediaPlayer win;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_legitimate);
        win = MediaPlayer.create(this, R.raw.cashregister);
        ((EditText) findViewById(R.id.pin)).setOnEditorActionListener(
                new EditText.OnEditorActionListener() {
                    @Override
                    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                        findViewById(R.id.result).setVisibility(View.INVISIBLE);
                        if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                                actionId == EditorInfo.IME_ACTION_DONE ||
                                event.getAction() == KeyEvent.ACTION_DOWN &&
                                        event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                            if (!((EditText) v).getText().toString().equals("0160")) {
                                findViewById(R.id.result).setVisibility(View.VISIBLE);
                                ((Vibrator) getSystemService(VIBRATOR_SERVICE)).vibrate(1400);
                                return false;
                            }
                            win.start();
                            ((Vibrator) getSystemService(VIBRATOR_SERVICE)).vibrate(70);
                            float sum = 1.1f;
                            Cursor c = getContentResolver().query(
                                    getIntent().getData().buildUpon()
                                    .appendPath("sum").build(),
                                    null, null, null, null);
                            if (c.moveToFirst()) {
                                System.out.println("SUM " + c.getFloat(2));
                                sum = c.getFloat(2);
                            }
                            PreferenceManager.getDefaultSharedPreferences(
                                    LegitimateActivity.this).edit()
                                    .putFloat("balance", PreferenceManager
                                            .getDefaultSharedPreferences(
                                                    LegitimateActivity.this)
                                    .getFloat("balance", 100f) - sum).commit();
                            startActivity(new Intent(LegitimateActivity.this,
                                    WinActivity.class));
                            return true;
                        }
                        return false;
                    }
                });
    }

    @Override
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    protected void onStart() {
        super.onStart();
        if (Build.VERSION.SDK_INT < 16) {
            getWindow().setFlags(
                    WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_FULLSCREEN);
        }
    }
}
