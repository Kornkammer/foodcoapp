package org.baobab.foodcoapp;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;

public class ProductEditActivity extends ActionBarActivity
        implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final int REQUEST_IMAGE_CAPTURE = 42;
    private static final String TAG = "POS";
    private ImageButton image;
    private EditText title;
    private EditText price;
    private TextView unit;
    private Uri img;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Edit product " + getIntent().getData().getLastPathSegment());
        setContentView(R.layout.activity_product_edit);
        image = (ImageButton) findViewById(R.id.image);
        title = (EditText) findViewById(R.id.title);
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
                } else {
                    unit.setText(R.string.weight);
                }
            }
        });
        getSupportLoaderManager().initLoader(0, null, this);
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
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(this, getIntent().getData(), null, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (data.moveToFirst()) {
            if (!data.isNull(1)) {
                title.setText(data.getString(1));
            }
            if (data.getFloat(2) != 0.0f) {
                price.setText(String.format("%.2f", data.getFloat(2)));
            }
            if (!data.isNull(4)) {
                img = Uri.parse(data.getString(4));
                image.setImageURI(img);
            }
            if (!data.isNull(3) && data.getString(3).equals(getString(R.string.weight))) {
                unit.setText(R.string.weight);
            } else {
                unit.setText(R.string.piece);
            }
        }
    }

    public void store() {
        if (title.getText().toString().equals("")) {
            Toast.makeText(this, "no title", Toast.LENGTH_LONG).show();
            return;
        }
        if (img == null) {
            Toast.makeText(this, "no image", Toast.LENGTH_LONG).show();
            return;
        }
        if (price.getText().toString().equals("")) {
            Toast.makeText(this, "no price", Toast.LENGTH_LONG).show();
            return;
        }
        if (Float.valueOf(price.getText().toString()) > 1000) {
            Toast.makeText(this, "Preis ist zu hoch!", Toast.LENGTH_LONG).show();
            return;
        }
        ContentValues cv = new ContentValues();
        cv.put("title", title.getText().toString());
        cv.put("img", img.toString());
        cv.put("price", Float.valueOf(price.getText().toString()));

        cv.put("unit", unit.getText().toString());
        getContentResolver().update(getIntent().getData(), cv, null, null);
        finish();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }

    private Uri storeImage(Bitmap bitmap) {
        try {Log.d(TAG, "dir " + getFilesDir());
            File dir = new File(getFilesDir(), "/pos");
            if (!dir.exists()) dir.mkdir();
            File file = new File(dir,
                    getIntent().getData().getLastPathSegment() +
                            System.currentTimeMillis() + ".jpg");
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
