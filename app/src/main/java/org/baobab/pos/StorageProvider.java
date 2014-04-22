package org.baobab.pos;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.net.Uri;
import android.util.Log;

public class StorageProvider extends ContentProvider {

    private class DatabaseHelper extends SQLiteOpenHelper {

        static final String TAG = "Provider";

        public DatabaseHelper(Context context) {
            super(context, "pos.db", null, 2);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE products (" +
                    "_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "title TEXT" +
                    ");");
            db.execSQL("INSERT INTO products (title) VALUES ('empty');");
            db.execSQL("INSERT INTO products (title) VALUES ('empty');");
            db.execSQL("INSERT INTO products (title) VALUES ('empty');");
            db.execSQL("INSERT INTO products (title) VALUES ('empty');");
            db.execSQL("INSERT INTO products (title) VALUES ('empty');");
            db.execSQL("INSERT INTO products (title) VALUES ('empty');");
            db.execSQL("INSERT INTO products (title) VALUES ('empty');");
            db.execSQL("INSERT INTO products (title) VALUES ('empty');");
            db.execSQL("INSERT INTO products (title) VALUES ('empty');");
            Log.d(TAG, "created DB");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldV, int newV) { }
    }



    private DatabaseHelper db;
    private SQLiteStatement insert;

    @Override
    public boolean onCreate() {
        db = new DatabaseHelper(getContext());
        return false;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        Cursor result = db.getReadableDatabase().query("products", projection, selection, selectionArgs, null, null, null);
        return result;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }
}
