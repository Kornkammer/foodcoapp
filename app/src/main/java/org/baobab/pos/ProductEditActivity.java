package org.baobab.pos;

import android.annotation.TargetApi;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;

public class ProductEditActivity extends FragmentActivity implements LoaderManager.LoaderCallbacks<Cursor>, View.OnClickListener {

    private static final int REQUEST_IMAGE_CAPTURE = 42;
    private static final String TAG = "POS";
    private ImageButton image;
    private EditText title;
    private EditText price;
    private Button done;
    private File imageFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Edit product " + getIntent().getData().getLastPathSegment());
        setContentView(R.layout.activity_product_edit);
        image = (ImageButton) findViewById(R.id.image);
        title = (EditText) findViewById(R.id.title);
        price = (EditText) findViewById(R.id.price);
        done = (Button) findViewById(R.id.done);
        image.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                }
            }
        });
        done.setOnClickListener(this);
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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bitmap bitmap = (Bitmap) data.getExtras().get("data");
            imageFile = storeImage(bitmap);
            image.setImageURI(Uri.fromFile(imageFile));
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
            if (!data.isNull(2)) {
                imageFile = new File(data.getString(2));
                image.setImageURI(Uri.fromFile(imageFile));
            }
            if (data.getFloat(3) != 0.0f) {
                price.setText(String.format("%.2f", data.getFloat(3)));
            }
        }
    }

    @Override
    public void onClick(View v) {
        if (title.getText().toString().equals("")) {
            Toast.makeText(this, "no title", Toast.LENGTH_LONG).show();
            return;
        }
        if (imageFile == null) {
            Toast.makeText(this, "no image", Toast.LENGTH_LONG).show();
            return;
        }
        if (price.getText().toString().equals("")) {
            Toast.makeText(this, "no price", Toast.LENGTH_LONG).show();
            return;
        }
        ContentValues cv = new ContentValues();
        cv.put("title", title.getText().toString());
        cv.put("path", imageFile.getAbsolutePath());
        cv.put("price", Float.valueOf(price.getText().toString()));
        getContentResolver().update(getIntent().getData(), cv, null, null);
        finish();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }

    private File storeImage(Bitmap bitmap) {
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
            return file;
        } catch (Exception e) {
            Log.e(TAG, "error storing file " + e.toString());
        }
        return null;
    }
}
