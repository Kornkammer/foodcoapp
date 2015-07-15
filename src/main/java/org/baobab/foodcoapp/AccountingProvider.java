package org.baobab.foodcoapp;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.net.Uri;
import android.util.Log;

import java.util.UUID;

public class AccountingProvider extends ContentProvider {

    private class DatabaseHelper extends SQLiteOpenHelper {

        static final String TAG = "Provider";

        public DatabaseHelper(Context context) {
            super(context, "foodcoapp.db", null, 1);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE products (" +
                    "_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "title TEXT, " +
                    "price FLOAT, " +
                    "unit TEXT, " +
                    "img TEXT" +
                    ");");
            db.execSQL("CREATE TABLE transactions (" +
                    "_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "session_id INTEGER, " +
                    "start INTEGER, " +
                    "stop INTEGER, " +
                    "status TEXT" +
                    ");");
            db.execSQL("CREATE TABLE transaction_products (" +
                    "_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "transaction_id INTEGER, " +
                    "account_guid TEXT, " +
                    "product_id INTEGER, " +
                    "quantity FLOAT, " +
                    "price FLOAT, " +
                    "unit TEXT, " +
                    "title TEXT, " +
                    "img TEXT " +
                    ");");
            db.execSQL("CREATE UNIQUE INDEX idx"
                    + " ON transaction_products (" +
                    "transaction_id, product_id);");
            db.execSQL("CREATE TABLE accounts (" +
                    "_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "parent_guid, " +
                    "guid TEXT, " +
                    "name TEXT, " +
                    "skr INTEGER," +
                    "status TEXT, " +
                    "contact TEXT, " +
                    "pin TEXT, " +
                    "qr TEXT" +
                    ");");
            db.execSQL("CREATE TABLE sessions (" +
                    "_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "account_guid TEXT, " +
                    "start INTEGER, " +
                    "stop INTEGER" +
                    ");");
            db.execSQL("INSERT INTO products (title, price, unit, img) VALUES ('Baola', 1.5, 'Stück', 'android.resource://org.baobab.foodcoapp/drawable/baola');");
            db.execSQL("INSERT INTO products (title, price, unit, img) VALUES ('Kaffee', 3.5, 'Stück', 'android.resource://org.baobab.foodcoapp/drawable/coffee');");
            db.execSQL("INSERT INTO products (title, price, unit, img) VALUES ('Keks', 0.5, 'Stück', 'android.resource://org.baobab.foodcoapp/drawable/cookie');");
            db.execSQL("INSERT INTO products (title) VALUES ('');");
            db.execSQL("INSERT INTO products (title) VALUES ('');");
            db.execSQL("INSERT INTO products (title) VALUES ('');");
            db.execSQL("INSERT INTO products (title) VALUES ('');");
            db.execSQL("INSERT INTO products (title) VALUES ('');");
            db.execSQL("INSERT INTO products (title, price, unit, img) VALUES ('Reis', 1.3, 'Kilo', 'android.resource://org.baobab.foodcoapp/drawable/rice');");
            db.execSQL("INSERT INTO products (title) VALUES ('');");
            db.execSQL("INSERT INTO products (title) VALUES ('');");
            db.execSQL("INSERT INTO products (title) VALUES ('');");
            db.execSQL("INSERT INTO products (title) VALUES ('');");
            db.execSQL("INSERT INTO products (title) VALUES ('');");
            db.execSQL("INSERT INTO products (title) VALUES ('');");
            db.execSQL("INSERT INTO products (title) VALUES ('');");
            db.execSQL("INSERT INTO products (title, price, img) VALUES ('Cash', 1, 'android.resource://org.baobab.foodcoapp/drawable/cash');");
            db.execSQL("INSERT INTO products (title, price, img) VALUES ('Credits', 1, 'android.resource://org.baobab.foodcoapp/drawable/coin');");
            db.execSQL("INSERT INTO accounts (_id, parent_guid, guid, name) VALUES (1, '', 'aktiva','Aktiva');");
            db.execSQL("INSERT INTO accounts (_id, parent_guid, guid, name) VALUES (2, '', 'passiva','Passiva');");
            db.execSQL("INSERT INTO accounts (_id, parent_guid, guid, name) VALUES (3, 'aktiva', 'lager','Lager');");
            db.execSQL("INSERT INTO accounts (_id, parent_guid, guid, name) VALUES (300000, 'aktiva', 'kasse','Kasse');");
//            db.execSQL("INSERT INTO accounts (_id, parent_guid, guid, name) VALUES (6, 'passiva', 'sepp','Sepp');");
//            db.execSQL("INSERT INTO accounts (_id, parent_guid, guid, name) VALUES (7, 'passiva', 'susi','Susi');");
//            db.execSQL("INSERT INTO accounts (_id, parent_guid, guid, name) VALUES (8, 'passiva', 'flo','Flo');");
            db.execSQL("INSERT INTO transactions (_id, status) VALUES (1, 'foo');");
            db.execSQL("INSERT INTO transaction_products (_id, account_guid) VALUES (1, 'lager');");
            Log.d(TAG, "created DB");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldV, int newV) {
        }
    }

    static final UriMatcher router = new UriMatcher(0);
    private static final int ACCOUNT = 0;
    private static final int ACCOUNTS = 1;
    private static final int SESSIONS = 2;
    private static final int PRODUCTS = 3;
    private static final int PRODUCT = 4;
    private static final int LEGITIMATE = 5;
    private static final int TRANSACTION = 6;
    private static final int TRANSACTIONS = 7;
    private static final int TRANSACTION_PRODUCT = 8;
    private static final int ACCOUNT_PRODUCTS = 9;

    private static final short SUM = 10;

    static {
        router.addURI("org.baobab.foodcoapp", "accounts/*", ACCOUNT);
        router.addURI("org.baobab.foodcoapp", "accounts", ACCOUNTS);
        router.addURI("org.baobab.foodcoapp", "accounts/*/accounts", ACCOUNTS);
        router.addURI("org.baobab.foodcoapp", "accounts/*/products", ACCOUNT_PRODUCTS);
        router.addURI("org.baobab.foodcoapp", "products", PRODUCTS);
        router.addURI("org.baobab.foodcoapp", "sessions", SESSIONS);
        router.addURI("org.baobab.foodcoapp", "products/#", PRODUCT);
        router.addURI("org.baobab.foodcoapp", "legitimate", LEGITIMATE);
        router.addURI("org.baobab.foodcoapp", "transactions", TRANSACTIONS);
        router.addURI("org.baobab.foodcoapp", "transactions/#", TRANSACTION);
        router.addURI("org.baobab.foodcoapp", "transactions/#/products/#", TRANSACTION_PRODUCT);
        router.addURI("org.baobab.foodcoapp", "transactions/#/sum", SUM);
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
                result = db.getReadableDatabase().query("products",
                        projection, selection, selectionArgs, null, null, null, "16");
                break;
            case PRODUCT:
                result = db.getReadableDatabase().query("products", projection,
                        "_id = ?", new String[] {uri.getLastPathSegment()},
                        null, null, null);
                break;
            case TRANSACTION:
                result = db.getReadableDatabase().rawQuery(
                        "SELECT * FROM transaction_products" +
                        " LEFT JOIN (" +
                                "SELECT _id, parent_guid, guid, name, max(_id) FROM accounts GROUP BY guid" +
                                ") AS accounts ON transaction_products.account_guid = accounts.guid " +
                        " WHERE transaction_id = ?" +
                        " GROUP BY accounts.guid, product_id" +
                        " ORDER BY accounts._id, transaction_products._id",
                        new String[] { uri.getLastPathSegment() });
                break;
            case SUM:
                result = db.getReadableDatabase().rawQuery(
                        "SELECT quantity, price, sum(quantity * price)" +
                        " FROM transaction_products" +
                        " WHERE transaction_id = ?",
                        new String[] { uri.getPathSegments().get(1) });
                break;
            case ACCOUNTS:
                String parent_guid = "0";
                if (uri.getPathSegments().size() > 1) {
                    parent_guid = uri.getPathSegments().get(1);
                }
                result = db.getReadableDatabase().rawQuery(
                        "SELECT accounts._id AS _id, name, guid, max(accounts._id)," +
                            " sum(txn.quantity * txn.price) AS balance, parent_guid" +
                        " FROM (SELECT _id, name, guid, max(_id), parent_guid" +
                                " FROM accounts GROUP BY guid" +
                                ") AS accounts" +
                        " LEFT OUTER JOIN (" +
                                "SELECT * FROM transaction_products" +
                                " LEFT OUTER JOIN transactions ON transaction_products.transaction_id = transactions._id" +
                                " WHERE transactions.status IS NOT 'draft'" +
                                ") AS txn ON txn.account_guid = accounts.guid" +
                        " WHERE parent_guid IS ?" +
                        " GROUP BY guid" +
                        (selection != null? " HAVING " + selection : "") +
                        " ORDER BY name",
                        new String[] { parent_guid });
                break;
            case ACCOUNT_PRODUCTS:
                String account_guid = "";
                if (uri.getPathSegments().size() > 1) {
                    account_guid = uri.getPathSegments().get(1);
                }
                result = db.getReadableDatabase().rawQuery(
                        "SELECT transaction_products._id, transaction_id, account_guid," +
                                " product_id, sum(quantity) AS stock, price, unit, title, img," +
                                " accounts._id, parent_guid, guid, name, quantity > 0 as credit" +
                        " FROM transaction_products" +
                        " LEFT JOIN (" +
                                "SELECT _id, guid, name, max(_id), parent_guid FROM accounts GROUP BY guid" +
                                ") AS accounts ON transaction_products.account_guid = accounts.guid" +
                        " LEFT JOIN transactions ON transaction_products.transaction_id = transactions._id" +
                        " WHERE account_guid IS ? AND transactions.status IS NOT 'draft'" +
                        " GROUP BY title, price" +
                        (selection != null? " HAVING " + selection : ""),
                        new String[] { account_guid });
                break;
            case ACCOUNT:
                result = db.getReadableDatabase().rawQuery(
                        "SELECT accounts._id AS _id, guid, name, contact, pin, qr, max(accounts._id), " +
                                "sum(transaction_products.quantity * transaction_products.price) " +
                        " FROM accounts" +
                        " LEFT OUTER JOIN transaction_products ON transaction_products.account_guid = accounts.guid" +
//                        " LEFT OUTER JOIN products ON transaction_products.product_id = products._id" +
                        " WHERE accounts.guid = ?" +
                        " GROUP BY guid",
                        new String[] { uri.getLastPathSegment() });
                break;
            case LEGITIMATE:
                result = db.getReadableDatabase().rawQuery(
                        "SELECT _id, status, max(accounts._id), guid FROM accounts " +
                                "WHERE pin IS ? OR qr IS ? GROUP BY guid",
                        new String[] { uri.getQueryParameter("pin"),
                                uri.getQueryParameter("pin")});
                break;
            case TRANSACTIONS:
                result = db.getReadableDatabase().rawQuery(
                        "SELECT transactions._id AS _id, session.name, transactions.stop, accounts.name, " +
                                "GROUP_CONCAT(quantity || ' x ' || title || ' a ' || price || ' = ' || (quantity * price), ',  \n'), " +
                                "sum(transaction_products.quantity * transaction_products.price)" +
                        " FROM transactions" +
                        " LEFT OUTER JOIN sessions ON transactions.session_id = sessions._id" +
                        " LEFT JOIN accounts AS session ON sessions.account_guid = session.guid" +
                        " JOIN transaction_products ON transaction_products.transaction_id = transactions._id" +
                                " LEFT JOIN (" +
                                "SELECT _id, guid, name, max(_id), parent_guid from accounts GROUP BY guid" +
                                ") AS accounts ON transaction_products.account_guid = accounts.guid" +
                        " WHERE transactions.status IS NOT 'draft'" +
                                (selection != null? " AND " + selection : "") +
                        " GROUP BY transactions._id" +
                        " ORDER BY transactions._id",
                        null);
                break;
        }
        result.setNotificationUri(getContext().getContentResolver(), uri);
        return result;
    }

    @Override
    public String getType(Uri uri) {
        switch (router.match(uri)) {
            case PRODUCT:
                return "org.baobab.foodcoapp/products";
            case ACCOUNT:
                return "org.baobab.foodcoapp/accounts";
        }
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        switch (router.match(uri)) {
            case TRANSACTION_PRODUCT:
                Cursor product = query(Uri.parse(
                        "content://org.baobab.foodcoapp/products/" + uri.getLastPathSegment()),
                        null, null, null, null);
                product.moveToFirst();
                double price = product.getFloat(2);
                double quantity = values.containsKey("quantity")?
                        values.getAsFloat("quantity") : 1.0;
                db.getWritableDatabase().execSQL(
                        "INSERT OR REPLACE INTO transaction_products" +
                                " (transaction_id, account_guid, product_id, title, price, unit, img, quantity)" +
                                " VALUES (?, ?, ?, ?, ?, ?, ?, " +
                                "COALESCE(" +
                                "(SELECT quantity FROM transaction_products" +
                                " WHERE transaction_id = ? AND product_id = ?)," +
                                "0) - ?);", new String[] {
                                uri.getPathSegments().get(1),
                                values.getAsString("account_guid"),
                                product.getString(0),
                                product.getString(1),
                                String.valueOf(price),
                                product.getString(3),
                                product.getString(4),
                                uri.getPathSegments().get(1),
                                uri.getLastPathSegment(),
                                String.valueOf(quantity) }
                );
                getContext().getContentResolver().notifyChange(uri, null);
                break;
            case ACCOUNTS:
                if (!values.containsKey("guid")) {
                    values.put("guid", UUID.randomUUID().toString());
                }
                if (uri.getPathSegments().size() > 1) {
                    values.put("parent_guid", uri.getPathSegments().get(1));
                }
                uri = ContentUris.withAppendedId(uri,
                    db.getWritableDatabase().insert("accounts", null, values));
                getContext().getContentResolver().notifyChange(uri, null);
                break;
            case TRANSACTIONS:
                if (values == null) {
                    values = new ContentValues();
                }
                if (!values.containsKey("session_id")) {
                    ContentValues b = new ContentValues();
                    b.put("account_guid", 1);
                    b.put("start", System.currentTimeMillis());
                    Uri session = insert(Uri.parse(
                            "content://org.baobab.foodcoapp/sessions"), b);
                    values.put("session_id", session.getLastPathSegment());
                }
                if (!values.containsKey("status")) {
                    values.put("status", "draft");
                }
                values.put("start", System.currentTimeMillis());
                uri = ContentUris.withAppendedId(uri,
                    db.getWritableDatabase().insert("transactions", null, values));
                break;
            case SESSIONS:
                uri = ContentUris.withAppendedId(uri,
                    db.getWritableDatabase().insert("sessions", null, values));
                break;
        }
        return uri;
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
                if (c.getCount() == 0) return -1;
                c.moveToFirst();
                if (c.getInt(4) < -1 && selection == null) {
                    ContentValues dec = new ContentValues();
                    dec.put("quantity", c.getInt(4) + 1);
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
        switch (router.match(uri)) {
            case ACCOUNTS:
                db.getWritableDatabase().update("accounts", values, selection, selectionArgs);
                break;
            case PRODUCT:
                db.getWritableDatabase().update("products", values, "_id = " + uri.getLastPathSegment(), null);
                break;
            case TRANSACTION:
                if (values.containsKey("quantity")) {
                    db.getWritableDatabase().execSQL(
                            "UPDATE transaction_products" +
                                " SET quantity = -1 * quantity" +
                            " WHERE transaction_id = ?",
                            new String[] { uri.getLastPathSegment() });
                } else {
                    db.getWritableDatabase().update("transactions", values, "_id =  + " + uri.getLastPathSegment(), null);
                }
                break;
            case TRANSACTION_PRODUCT:
                db.getWritableDatabase().update("transaction_products", values,
                        "transaction_id = ? AND product_id = ?",
                        new String[]{
                                uri.getPathSegments().get(1),
                                uri.getLastPathSegment() });
                break;
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return 0;
    }
}
