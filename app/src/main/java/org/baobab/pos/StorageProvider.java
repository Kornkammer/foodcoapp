package org.baobab.pos;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.net.Uri;
import android.util.Log;

public class StorageProvider extends ContentProvider {

    private static final String TAG = "POS";

    private class DatabaseHelper extends SQLiteOpenHelper {

        static final String TAG = "Provider";

        public DatabaseHelper(Context context) {
            super(context, "pos.db", null, 3);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE products (" +
                    "_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "title TEXT, " +
                    "path TEXT, " +
                    "price FLOAT" +
                    ");");
            db.execSQL("CREATE TABLE transactions (" +
                    "_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "account_id INTEGER, " +
                    "timestamp INTEGER" +
                    ");");
            db.execSQL("CREATE TABLE transaction_products (" +
                    "_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "transaction_id INTEGER, " +
                    "product_id INTEGER, " +
                    "quantity INTEGER" +
                    ");");
            db.execSQL("CREATE UNIQUE INDEX idx"
                    + " ON transaction_products (" +
                    "transaction_id, product_id);");

            db.execSQL("INSERT INTO products (title) VALUES ('');");
            db.execSQL("INSERT INTO products (title) VALUES ('');");
            db.execSQL("INSERT INTO products (title) VALUES ('');");
            db.execSQL("INSERT INTO products (title) VALUES ('');");
            db.execSQL("INSERT INTO products (title) VALUES ('');");
            db.execSQL("INSERT INTO products (title) VALUES ('');");
            db.execSQL("INSERT INTO products (title) VALUES ('');");
            db.execSQL("INSERT INTO products (title) VALUES ('');");
            db.execSQL("INSERT INTO products (title) VALUES ('');");
            db.execSQL("INSERT INTO products (title) VALUES ('');");
            db.execSQL("INSERT INTO products (title) VALUES ('');");
            db.execSQL("INSERT INTO products (title) VALUES ('');");
            db.execSQL("INSERT INTO transactions (timestamp) VALUES (1);");
            //db.execSQL("INSERT INTO transaction_products VALUES (1, 1, 1, 1);");
            //db.execSQL("INSERT INTO transaction_products VALUES (2, 1, 2, 2);");
            //db.execSQL("INSERT INTO transaction_products VALUES (3, 1, 3, 3);");
            Log.d(TAG, "created DB");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldV, int newV) {
            if (oldV == 2 && newV == 3) {
                db.execSQL("INSERT INTO products (title) VALUES ('');");
                db.execSQL("INSERT INTO products (title) VALUES ('');");
                db.execSQL("INSERT INTO products (title) VALUES ('');");
                db.execSQL("INSERT INTO products (title) VALUES ('');");
            }
        }
    }

    static final UriMatcher router = new UriMatcher(0);
    private static final int PRODUCTS = 1;
    private static final int PRODUCT = 2;
    private static final int TRANSACTION = 3;
    private static final int TRANSACTION_PRODUCT = 4;
    private static final short SUM = 5;

    static {
        router.addURI("org.baobab.pos", "products", PRODUCTS);
        router.addURI("org.baobab.pos", "products/#", PRODUCT);
        router.addURI("org.baobab.pos", "transactions/#", TRANSACTION);
        router.addURI("org.baobab.pos", "transactions/#/products/#", TRANSACTION_PRODUCT);
        router.addURI("org.baobab.pos", "transactions/#/sum", SUM);
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
        Cursor result = null;
        switch (router.match(uri)) {
            case PRODUCTS:
                result = db.getReadableDatabase().query("products", projection, selection, selectionArgs, null, null, null);
                break;
            case PRODUCT:
                result = db.getReadableDatabase().query("products", projection,
                        "_id = ?", new String[] {uri.getLastPathSegment()},
                        null, null, null);
                break;
            case TRANSACTION:
                result = db.getReadableDatabase().query(
                        "transaction_products JOIN products" +
                        " ON transaction_products.product_id = products._id", null,
                        "transaction_id = ?",
                        new String[] {uri.getLastPathSegment()},
                        null, null, null);
                break;
            case SUM:
                result = db.getReadableDatabase().rawQuery(
                        "SELECT quantity, price, sum(quantity * price)" +
                        " FROM transaction_products JOIN products" +
                        " ON transaction_products.product_id = products._id" +
                        " WHERE transaction_id = ?",
                        new String[] { uri.getPathSegments().get(1) });
                break;
        }
        result.setNotificationUri(getContext().getContentResolver(), uri);
        return result;
    }

    @Override
    public String getType(Uri uri) {
        return "org.baobab.pos/products";
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        switch (router.match(uri)) {
            case TRANSACTION_PRODUCT:
                db.getWritableDatabase().execSQL(
                        "INSERT OR REPLACE INTO transaction_products" +
                                " (transaction_id, product_id, quantity) " +
                                "VALUES (?, ?, " +
                                "COALESCE(" +
                                "(SELECT quantity FROM transaction_products" +
                                " WHERE transaction_id = ? AND product_id = ?)," +
                                "0) + 1);", new String[]{
                                uri.getPathSegments().get(1),
                                uri.getLastPathSegment(),
                                uri.getPathSegments().get(1),
                                uri.getLastPathSegment()}
                );
                getContext().getContentResolver().notifyChange(uri, null);
                break;
        }
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        switch (router.match(uri)) {
            case TRANSACTION_PRODUCT:
                Cursor c = db.getReadableDatabase().query("transaction_products", null,
                        "transaction_id =? AND product_id = ?",
                        new String[]{
                                uri.getPathSegments().get(1),
                                uri.getLastPathSegment()},
                        null, null, null);
                c.moveToFirst();
                if (c.getInt(3) > 1) {
                    ContentValues dec = new ContentValues();
                    dec.put("quantity", c.getInt(3) - 1);
                    db.getWritableDatabase().update("transaction_products", dec,
                            "transaction_id = ? AND product_id = ?",
                            new String[]{
                                    uri.getPathSegments().get(1),
                                    uri.getLastPathSegment() });
                } else {
                    db.getWritableDatabase().delete("transaction_products",
                            "transaction_id = ? AND product_id = ?",
                            new String[]{
                                    uri.getPathSegments().get(1),
                                    uri.getLastPathSegment()}
                    );
                }
                getContext().getContentResolver().notifyChange(uri, null);

                break;
        }
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        db.getWritableDatabase().update("products", values, "_id = " + uri.getLastPathSegment(), null);
        getContext().getContentResolver().notifyChange(uri, null);
        return 0;
    }
}
