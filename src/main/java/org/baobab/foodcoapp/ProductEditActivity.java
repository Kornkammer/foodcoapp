package org.baobab.foodcoapp;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.DialogFragment;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.design.widget.Snackbar;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import org.baobab.foodcoapp.util.Nfc;

import java.io.File;
import java.io.FileOutputStream;

public class ProductEditActivity extends AppCompatActivity
        implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final int REQUEST_IMAGE_CAPTURE = 42;
    private static final String TAG = "POS";
    private ImageButton image;
    private EditText title;
    private EditText price;
    private TextView unit;
    private EditText ean;
    private Uri img;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_edit);
        image = (ImageButton) findViewById(R.id.image);
        title = (EditText) findViewById(R.id.title);
        ean = (EditText) findViewById(R.id.ean);
        price = (EditText) findViewById(R.id.price);
        unit = (TextView) findViewById(R.id.unit);
        image.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                }
            }
        });
        unit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (unit.getText().toString().equals(getString(R.string.weight))) {
                    unit.setText(R.string.piece);
                } else if (unit.getText().toString().equals(getString(R.string.piece))) {
                    unit.setText(R.string.volume);
                } else {
                    unit.setText(R.string.weight);
                }
            }
        });
        if (!getIntent().getDataString().endsWith("products")) {
            getSupportLoaderManager().initLoader(0, null, this);
        } else {
            setTitle(getString(R.string.neues) + " " + getString(R.string.product));
            unit.setText(R.string.piece);
            price.setText(String.format("%.2f", Math.abs(getIntent().getFloatExtra("price", 0))));
        }
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (getResources().getConfiguration().hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_NO) {
            Toast.makeText(this, "Switch hardware keyboard OFF", Toast.LENGTH_LONG).show();
            imm.showInputMethodPicker();
        }
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

    @Override
    protected void onResume() {
        super.onResume();
        Nfc.resume(this, NfcAdapter.ACTION_TAG_DISCOVERED, null);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent.getAction().equals(NfcAdapter.ACTION_TAG_DISCOVERED)) {
            String code = ean.getText().toString();
            if (code.equals("")) {
                code = String.valueOf(System.currentTimeMillis()).substring(5);
                ean.setText(code);
            }
            if (Nfc.writeTag(intent, title.getText().toString() + ": " + code)) {
                Toast.makeText(this, "assigned " + code, Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getSupportActionBar().setDisplayOptions(
                ActionBar.DISPLAY_SHOW_CUSTOM,
                ActionBar.DISPLAY_SHOW_CUSTOM | ActionBar.DISPLAY_SHOW_HOME
                        | ActionBar.DISPLAY_SHOW_TITLE);
        View bar = ((LayoutInflater) getSupportActionBar().getThemedContext()
                .getSystemService(Activity.LAYOUT_INFLATER_SERVICE))
                .inflate(R.layout.actionbar_done_discard, null, false);
        getSupportActionBar().setCustomView(bar,
                new ActionBar.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT)
        );
        bar.findViewById(R.id.actionbar_cancel).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        finish();
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
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bitmap bitmap = (Bitmap) data.getExtras().get("data");
            img = storeImage(bitmap);
            image.setImageURI(img);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Nfc.pause(this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(this, getIntent().getData(), null, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, final Cursor data) {
        if (data.moveToFirst()) {
            setTitle(getString(R.string.edit) + " " + getString(R.string.product) + " " + data.getString(7));
            if (!data.isNull(7)) {
                title.setText(data.getString(7));
            }
            if (data.getFloat(5) != 0.0f) {
                price.setText(String.format("%.2f", data.getFloat(5)));
            }
            if (!data.isNull(8)) {
                img = Uri.parse(data.getString(8));
                image.setImageURI(img);
            }
            if (!data.isNull(6) && data.getString(6).equals(getString(R.string.weight))) {
                unit.setText(R.string.weight);
            } else if (!data.isNull(6) && data.getString(6).equals(getString(R.string.volume))) {
                unit.setText(R.string.volume);
            } else {
                unit.setText(R.string.piece);
            }
            if (!data.isNull(9)) {
                ean.setText(data.getString(9));
            }
            if (!getIntent().hasExtra("account_guid")) {
                findViewById(R.id.delete).setVisibility(View.VISIBLE);
                findViewById(R.id.delete).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        new AlertDialog.Builder(ProductEditActivity.this)
                                .setTitle(title.getText().toString() + " aus dem Sortiment nehmen?")
                                .setPositiveButton(R.string.btn_yes, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        Cursor stocks = getContentResolver().query(
                                                Uri.parse("content://org.baobab.foodcoapp/accounts/lager/products"),
                                                null, "title IS '" + data.getString(7) + "' AND rounded = ROUND(" + data.getFloat(5) + ", 2)", null, null);
                                        if (stocks.getCount() > 0) {
                                            stocks.moveToFirst();
                                            Snackbar.make(title, "Produkt kann nicht entfernt werden.\n" +
                                                    "noch " + stocks.getFloat(4) + " " + data.getString(6) +
                                                    " auf Lager", Snackbar.LENGTH_LONG).show();
                                        } else {
                                            ContentValues cv = new ContentValues();
                                            cv.put("guid", data.getString(2));
                                            cv.put("status", "deleted");
                                            getContentResolver().insert(
                                                    Uri.parse("content://org.baobab.foodcoapp/products"), cv);
                                            finish();
                                        }
                                    }
                                }).setNegativeButton(R.string.btn_no, null)
                        .show();
                    }
                });
            }
        }
    }

    public void store() {
        if (title.getText().toString().equals("")) {
            Toast.makeText(this, "no title", Toast.LENGTH_LONG).show();
            return;
        }
        if (price.getText().toString().equals("")) {
            Toast.makeText(this, "no price", Toast.LENGTH_LONG).show();
            return;
        }
        try {
            float p = Float.parseFloat(price.getText().toString().replace(",", "."));
            if (p > 10000) {
                Toast.makeText(this, "Preis ist zu hoch!", Toast.LENGTH_LONG).show();
                return;
            }
            ContentValues cv = new ContentValues();
            cv.put("title", title.getText().toString().trim());
            if (img != null) {
                cv.put("img", img.toString());
            }
            if (getIntent().hasExtra("account_guid")) {
                cv.put("account_guid", getIntent().getStringExtra("account_guid"));
                cv.put("product_id", 3);
            } else {
                if (!ean.getText().toString().equals("")) {
                    cv.put("ean", ean.getText().toString());
                }
            }
            cv.put("price", p);
            cv.put("unit", unit.getText().toString());
            if (Math.abs(getIntent().getFloatExtra("price", 0)) == p) {
                if (getIntent().getFloatExtra("price", 0) > 0) {
                    cv.put("account_guid", "kosten");
                    cv.put("quantity", 1);
                    cv.put("product_id", 3);
                } else {
                    cv.put("account_guid", "spenden");
                    cv.put("unit", "Stück");
                    cv.put("product_id", 1);
                    cv.put("quantity", -p);
                    cv.put("price", 1);
                }
            } else {
                if (getIntent().getFloatExtra("price", 0) < 0) {
                    cv.put("quantity", 1);
                    cv.put("product_id", 3);
                }
            }

            if (getIntent().hasExtra("button")) {
                cv.put("button", getIntent().getIntExtra("button", 0));
            }
            if (getIntent().getDataString().endsWith("products")) {
                getContentResolver().insert(getIntent().getData(), cv);
            } else {
                getContentResolver().update(getIntent().getData(), cv, null, null);
            }
            finish();
        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }

    private Uri storeImage(Bitmap bitmap) {
        try {
            File dir = new File(getFilesDir(), "/pos");
            if (!dir.exists()) dir.mkdir();
            String product_id = "0";
            if (getIntent().getData() != null) {
                product_id = getIntent().getData().getLastPathSegment();
            }
            File file = new File(dir,
                    product_id + "_" + System.currentTimeMillis() + ".jpg");
            if (file.exists()) {
                file.delete();
            }
            FileOutputStream out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
            out.close();
            return Uri.fromFile(file);
        } catch (Exception e) {
            Log.e(TAG, "error storing file " + e.toString());
        }
        return null;
    }
}
