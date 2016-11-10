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
import android.os.CancellationSignal;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class LedgerProvider extends ContentProvider {

    private class DatabaseHelper extends SQLiteOpenHelper {

        static final String TAG = "Provider";

        public DatabaseHelper(Context context, String db) {
            super(context, db, null, 6);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE products (" +
                    "_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "guid TEXT, " +
                    "variant TEXT, " +
                    "title TEXT, " +
                    "price FLOAT, " +
                    "tax INTEGER, " +
                    "amount INTEGER, " +
                    "unit TEXT, " +
                    "img TEXT," +
                    "button INTEGER," +
                    "ean TEXT UNIQUE," +
                    "origin TEXT," +
                    "status TEXT" +
                    ");");
            db.execSQL("CREATE TABLE transactions (" +
                    "_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "session_id INTEGER, " +
                    "start INTEGER, " +
                    "stop INTEGER, " +
                    "comment TEXT, " +
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
                    "transaction_id, account_guid, title, price, unit);");
            db.execSQL("CREATE TABLE accounts (" +
                    "_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "last_modified INTEGER, " +
                    "created_at INTEGER, " +
                    "fee INTEGER, " +
                    "parent_guid TEXT, " +
                    "guid TEXT, " +
                    "name TEXT, " +
                    "skr INTEGER," +
                    "status TEXT, " +
                    "contact TEXT, " +
                    "pin TEXT, " +
                    "qr TEXT " +
                    ");");
            db.execSQL("CREATE TABLE sessions (" +
                    "_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "account_guid TEXT, " +
                    "start INTEGER, " +
                    "stop INTEGER, " +
                    "comment TEXT, " +
                    "img TEXT" +
                    ");");
            db.execSQL("INSERT INTO products (title, price, img) VALUES ('Cash', 1, 'android.resource://org.baobab.foodcoapp/drawable/cash');");
            db.execSQL("INSERT INTO products (title, price, img) VALUES ('Korns', 1, 'android.resource://org.baobab.foodcoapp/drawable/ic_korn');");
            db.execSQL("INSERT INTO products (title) VALUES ('');");
            db.execSQL("INSERT INTO products (title) VALUES ('');");
            db.execSQL("INSERT INTO products (title) VALUES ('');");
            db.execSQL("INSERT INTO products (button, title, price, unit, img) VALUES (1, 'Baola', 1.5, 'Stück', 'android.resource://org.baobab.foodcoapp/drawable/baola');");
            db.execSQL("INSERT INTO products (button, title, price, unit, img) VALUES (2, 'Kaffee', 3.5, 'Liter', 'android.resource://org.baobab.foodcoapp/drawable/coffee');");
            db.execSQL("INSERT INTO products (button, title, price, unit, img) VALUES (3, 'Keks', 0.42, 'Kilo', 'android.resource://org.baobab.foodcoapp/drawable/cookie');");
            db.execSQL("INSERT INTO accounts (_id, parent_guid, guid, name) VALUES (1, '', 'aktiva','Aktiva');");
            db.execSQL("INSERT INTO accounts (_id, parent_guid, guid, name) VALUES (2, '', 'passiva','Passiva');");
            db.execSQL("INSERT INTO accounts (_id, parent_guid, guid, name) VALUES (10, 'aktiva', 'inventar','Inventar');");
            db.execSQL("INSERT INTO accounts (_id, parent_guid, guid, name) VALUES (20, 'aktiva', 'kosten','Kosten');");
            db.execSQL("INSERT INTO accounts (_id, parent_guid, guid, name) VALUES (30, 'aktiva', 'lager','Lager');");
            db.execSQL("INSERT INTO accounts (_id, parent_guid, guid, name) VALUES (40, 'aktiva', 'bank','Bank');");
            db.execSQL("INSERT INTO accounts (_id, parent_guid, guid, name) VALUES (50, 'aktiva', 'kasse','Kasse');");
            db.execSQL("INSERT INTO accounts (_id, parent_guid, guid, name) VALUES (60, 'aktiva', 'forderungen','Forderungen');");
            db.execSQL("INSERT INTO accounts (_id, parent_guid, guid, name) VALUES (110, 'passiva', 'einlagen','Einlagen');");
            db.execSQL("INSERT INTO accounts (_id, parent_guid, guid, name) VALUES (120, 'passiva', 'beiträge','Beiträge');");
            db.execSQL("INSERT INTO accounts (_id, parent_guid, guid, name) VALUES (130, 'passiva', 'spenden','Spenden');");
            db.execSQL("INSERT INTO accounts (_id, parent_guid, guid, name) VALUES (140, 'passiva', 'mitglieder','Mitglieder');");
            db.execSQL("INSERT INTO accounts (_id, parent_guid, guid, name) VALUES (150, 'passiva', 'verbindlichkeiten','Verbindlichkeiten');");
            Log.d(TAG, "created DB");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldV, int newV) {
            System.out.println("UPGRADE FROM " + oldV);
            if (oldV == 1) {
                db.execSQL("ALTER TABLE accounts" +
                        " ADD last_modified INTEGER;");
                db.execSQL("ALTER TABLE accounts" +
                        " ADD created_at INTEGER;");
                db.execSQL("ALTER TABLE accounts" +
                        " ADD fee INTEGER;");
                System.out.println("DB UPDATED !!!!!!!!!!! ");
            }
            if (oldV <= 4) {
                db.execSQL("DROP table products;");
                db.execSQL("CREATE TABLE products (" +
                        "_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "guid TEXT, " +
                        "variant TEXT, " +
                        "title TEXT, " +
                        "tax INTEGER, " +
                        "price FLOAT, " +
                        "amount INTEGER, " +
                        "unit TEXT, " +
                        "img TEXT," +
                        "button INTEGER," +
                        "ean TEXT UNIQUE," +
                        "origin TEXT," +
                        "status TEXT" +
                        ");");
                db.execSQL("INSERT INTO products (title, price, img) VALUES ('Cash', 1, 'android.resource://org.baobab.foodcoapp/drawable/cash');");
                db.execSQL("INSERT INTO products (title, price, img) VALUES ('Korns', 1, 'android.resource://org.baobab.foodcoapp/drawable/ic_korn');");
                db.execSQL("INSERT INTO products (title) VALUES ('');");
                db.execSQL("INSERT INTO products (title) VALUES ('');");
                db.execSQL("INSERT INTO products (title) VALUES ('');");
            }
        }
    }

    static final UriMatcher router = new UriMatcher(0);
    private static final int SUM = 1;
    private static final int ACCOUNT = 2;
    private static final int ACCOUNTS = 3;
    private static final int SESSION = 4;
    private static final int SESSIONS = 5;
    private static final int PRODUCTS = 6;
    private static final int PRODUCT = 7;
    private static final int LEGITIMATE = 8;
    private static final int TRANSACTION = 9;
    private static final int TRANSACTIONS = 10;
    private static final int ACCOUNT_PRODUCTS = 11;
    private static final int TRANSACTION_PRODUCTS = 12;
    private static final int LOAD = 0;

    public static String AUTHORITY = "org.baobab.foodcoapp";

    private DatabaseHelper db;
    private SQLiteStatement insert;

    @Override
    public boolean onCreate() {
        String file;
        try {
            file = PreferenceManager.getDefaultSharedPreferences(getContext())
                    .getString("db", "foodcoapp.db");
        } catch (Exception e) {
            file = "foodcoapp.db";
        }
        db = new DatabaseHelper(getContext(), file);
        router.addURI(AUTHORITY, "accounts/*", ACCOUNT);
        router.addURI(AUTHORITY, "accounts", ACCOUNTS);
        router.addURI(AUTHORITY, "accounts/*/accounts", ACCOUNTS);
        router.addURI(AUTHORITY, "accounts/*/memberships", ACCOUNTS);
        router.addURI(AUTHORITY, "accounts/*/products", ACCOUNT_PRODUCTS);
        router.addURI(AUTHORITY, "sessions/#/accounts/*/products", ACCOUNT_PRODUCTS);
        router.addURI(AUTHORITY, "products", PRODUCTS);
        router.addURI(AUTHORITY, "sessions", SESSIONS);
        router.addURI(AUTHORITY, "sessions/#", SESSION);
        router.addURI(AUTHORITY, "products/#", PRODUCT);
        router.addURI(AUTHORITY, "legitimate", LEGITIMATE);
        router.addURI(AUTHORITY, "transactions/#/sum", SUM);
        router.addURI(AUTHORITY, "transactions", TRANSACTIONS);
        router.addURI(AUTHORITY, "transactions/#", TRANSACTION);
        router.addURI(AUTHORITY, "sessions/#/transactions", TRANSACTIONS);
        router.addURI(AUTHORITY, "accounts/*/transactions", TRANSACTIONS);
        router.addURI(AUTHORITY, "accounts/*/transactions/*", TRANSACTIONS);
        router.addURI(AUTHORITY, "transactions/#/products", TRANSACTION_PRODUCTS);
        router.addURI(AUTHORITY, "transactions/#/products/#", TRANSACTION_PRODUCTS);
        router.addURI(AUTHORITY, "transactions/#/accounts/*/products/#", TRANSACTION_PRODUCTS);
        router.addURI(AUTHORITY, "load/*", LOAD);
        return false;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        Cursor result = null;
        switch (router.match(uri)) {
            case PRODUCTS:
                result = db.getReadableDatabase().rawQuery("SELECT " +
                        "0, 0, guid, MAX(_id), variant, price, unit, title, img, " +
                        "tax, amount, button, ean, origin, status TEXT " +
                        "FROM products " +
                        (selection != null? "WHERE " + selection : "") +
                        " GROUP BY guid " +
                        "HAVING status IS NOT 'deleted' " +
                        "ORDER BY title", selectionArgs);
                break;
            case PRODUCT:
                result = db.getReadableDatabase().query("products", new String[] {
                          "_id", "-1", "guid", "_id AS product_id", "0",
                                "price", "unit", "title", "img", "ean" }, "_id = ?",
                        new String[] { uri.getLastPathSegment() }, null, null, null);
                break;
            case TRANSACTION_PRODUCTS:
                result = db.getReadableDatabase().rawQuery(
                        "SELECT * FROM transaction_products" +
                        " LEFT JOIN (" +
                                "SELECT _id, parent_guid, guid, name, max(_id) FROM accounts GROUP BY guid" +
                                ") AS accounts ON transaction_products.account_guid = accounts.guid " +
                        " WHERE transaction_id = ?" +
                                (uri.getPathSegments().size() > 3?
                                " AND transaction_products._id = " + uri.getLastPathSegment() : "") +
                        " GROUP BY accounts.guid, title, price, unit" +
                        " ORDER BY accounts._id, transaction_products.quantity < 0, transaction_products._id",
                        new String[] { uri.getPathSegments().get(1) });
                break;
            case SUM:
                result = getTransactionSum(uri.getPathSegments().get(1));
                break;
            case ACCOUNTS:
                String parent_guid = "NULL";
                if (uri.getPathSegments().size() > 1) {
                    parent_guid = uri.getPathSegments().get(1);
                }
                result = db.getReadableDatabase().rawQuery(
                        "SELECT _id, name, guid, sum(height), parent_guid," +
                                " created_at, last_modified, status, fee " +
                        "FROM (" +
                            " SELECT accounts._id AS _id, accounts.name, accounts.guid," +
                                " height, accounts.parent_guid," +
                                " accounts.created_at, accounts.last_modified," +
                                "accounts.status, accounts.fee" +
                            " FROM (SELECT _id, name, guid, max(_id), parent_guid," +
                                    " created_at, last_modified, status, fee" +
                                " FROM accounts" +
                                (uri.getQueryParameter("before") != null?
                                        " WHERE created_at < " + uri.getQueryParameter("before") +
                                            " OR created_at IS NULL" : "") +
                                " GROUP BY guid" + (uri.getLastPathSegment().equals("memberships")?
                                        ", created_at, fee" : "") +
                            ") AS accounts" +
                            " LEFT JOIN (" +
                                "SELECT transactions._id, account_guid, sum(txn.quantity * txn.price) AS height" +
                                " FROM transaction_products AS txn" +
                                " JOIN transactions ON txn.transaction_id = transactions._id" +
                                " WHERE transactions.status IS 'final'" +
                                ((uri.getQueryParameter("after") != null)?
                                        " AND transactions.start >= " + uri.getQueryParameter("after") : "") +
                                ((uri.getQueryParameter("before") != null)?
                                        " AND transactions.start < " + uri.getQueryParameter("before") : "") +
                                " GROUP BY txn.account_guid, txn.transaction_id" +
                                (uri.getQueryParameter("debit") != null? " HAVING height > 0" : "") +
                                (uri.getQueryParameter("credit") != null? " HAVING height < 0" : "") +
                            ") AS txn_heights ON txn_heights.account_guid = accounts.guid" +
                            (uri.getPathSegments().size() > 1?
                            " WHERE accounts.parent_guid IS '" + parent_guid + "'" : "") +
                        " UNION ALL " +
                            "SELECT accounts._id AS _id, accounts.name, accounts.guid," +
                                " height, accounts.parent_guid," +
                                " accounts.created_at, accounts.last_modified," +
                                " accounts.status, accounts.fee" +
                            " FROM (SELECT _id, name, guid, max(_id), parent_guid," +
                                    " created_at, last_modified, status, fee" +
                                " FROM accounts" +
                                (uri.getQueryParameter("before") != null?
                                        " WHERE created_at < " + uri.getQueryParameter("before") +
                                                " OR created_at IS NULL" : "") +
                                " GROUP BY guid" + (uri.getLastPathSegment().equals("memberships")?
                                        ", created_at, fee" : "") +
                                ") AS accounts" +
                            " LEFT JOIN (SELECT _id, name, guid, max(_id), parent_guid," +
                                    " created_at, last_modified, status, fee" +
                                " FROM accounts" +
                                (uri.getQueryParameter("before") != null?
                                        " WHERE created_at < " + uri.getQueryParameter("before") +
                                                " OR created_at IS NULL" : "") +
                                " GROUP BY guid" + (uri.getLastPathSegment().equals("memberships")?
                                    ", created_at, fee" : "") +
                            ") AS children ON accounts.guid = children.parent_guid" +
                            " LEFT JOIN (" +
                                "SELECT transactions._id, account_guid, sum(txn.quantity * txn.price) AS height" +
                                " FROM transaction_products AS txn" +
                                " JOIN transactions ON txn.transaction_id = transactions._id" +
                                " WHERE transactions.status IS 'final'" +
                                ((uri.getQueryParameter("after") != null)?
                                        " AND transactions.start >= " + uri.getQueryParameter("after") : "") +
                                ((uri.getQueryParameter("before") != null)?
                                        " AND transactions.start < " + uri.getQueryParameter("before") : "") +
                                " GROUP BY txn.account_guid, txn.transaction_id" +
                                (uri.getQueryParameter("debit") != null? " HAVING height > 0" : "") +
                                (uri.getQueryParameter("credit") != null? " HAVING height < 0" : "") +
                            ") AS txn_heights ON txn_heights.account_guid = children.guid" +
                            (uri.getPathSegments().size() > 1?
                            " WHERE accounts.parent_guid IS '" + parent_guid + "'" : "") +
                        ")" +
                        (uri.getQueryParameter("before") != null?
                                " WHERE created_at < " + uri.getQueryParameter("before") +
                                    " OR created_at IS NULL" : "") +
                        " GROUP BY guid" + (uri.getLastPathSegment().equals("memberships")?
                                ", created_at, fee" : "") +
                        " HAVING " + (selection != null? selection : "1 = 1") +
                                (!uri.getLastPathSegment().equals("memberships")?
                                    " AND (status IS NOT 'deleted'" +
                                    (uri.getQueryParameter("after") != null?
                                        " OR created_at >= " + uri.getQueryParameter("after") + ")" :
                                        (uri.getQueryParameter("before") != null?
                                        " OR created_at < " + uri.getQueryParameter("before") + ")" : ")")) : "") +
                        (sortOrder != null? " ORDER BY " + sortOrder : " ORDER BY _id"),
                        (selectionArgs != null? selectionArgs : null));
                break;
            case ACCOUNT_PRODUCTS:
                String account_guid = "";
                if (uri.getPathSegments().size() == 3) {
                    account_guid = uri.getPathSegments().get(1);
                } else if (uri.getPathSegments().size() == 5) {
                    account_guid = uri.getPathSegments().get(3);
                }
                result = db.getReadableDatabase().rawQuery(
                        "SELECT transaction_products._id, transaction_id, account_guid," +
                                " product_id, sum(quantity) AS stock, price, unit, title, img," +
                                " accounts._id, parent_guid, guid, name, ROUND(price, 2) AS rounded, MAX(transactions._id)" +
                        " FROM transaction_products" +
                        " LEFT JOIN (" +
                                "SELECT _id, guid, name, max(_id), parent_guid FROM accounts GROUP BY guid" +
                                ") AS accounts ON transaction_products.account_guid = accounts.guid" +
                        " LEFT JOIN transactions ON transaction_products.transaction_id = transactions._id" +
                        " WHERE account_guid IS ? AND (transactions.status IS 'final'" +
                        (uri.getPathSegments().size() == 5? " OR transactions.session_id=" + uri.getPathSegments().get(1) + ")" : ")") +
                        " GROUP BY title, rounded, unit" +
                        " HAVING (stock <= -0.001 OR 0.001 <= stock)" +
                        (selection != null? " AND " + selection : ""),
                        new String[] { account_guid });
                break;
            case ACCOUNT:
                result = db.getReadableDatabase().rawQuery(
                        "SELECT accounts._id AS _id, guid, name, contact, pin, qr, max(accounts._id), " +
                                "sum(transaction_products.quantity * transaction_products.price), " +
                                "parent_guid, created_at, fee, status" +
                        " FROM accounts" +
                        " LEFT OUTER JOIN transaction_products ON transaction_products.account_guid = accounts.guid" +
//                        " LEFT OUTER JOIN products ON transaction_products.product_id = products._id" +
                        " WHERE accounts.guid = ?" +
                        " GROUP BY guid, created_at, fee",
                        new String[] { uri.getLastPathSegment() });
                break;
            case LEGITIMATE:
                result = db.getReadableDatabase().rawQuery(
                        "SELECT _id, status, max(accounts._id), guid, name FROM accounts " +
                                "WHERE pin IS ? OR qr IS ? GROUP BY guid",
                        new String[] { uri.getQueryParameter("pin"),
                                uri.getQueryParameter("pin")});
                break;
            case TRANSACTION:
                selection = "transaction_products.transaction_id = " + uri.getPathSegments().get(1);
            case TRANSACTIONS:
                if (uri.getPathSegments().get(0).equals("sessions")) {
                    selection = "session_id = " + uri.getPathSegments().get(1);
                } else if (selection == null) {
                    selection = "transactions.status IS 'final'";
                    List<String> args = new ArrayList<>();
                    if (uri.getQueryParameter("title") != null) {
                        selection += " AND title IS ?";
                        args.add(uri.getQueryParameter("title"));
                    }
                    if (uri.getQueryParameter("price") != null) {
                        selection += " AND price IS ?";
                        args.add(uri.getQueryParameter("price"));
                    }
                    selectionArgs = args.toArray(new String[args.size()]);
                }
                result = db.getReadableDatabase().rawQuery(
                        "SELECT transactions._id AS _id, session._id, transactions.start, accounts.name, transactions.comment, " +
                                "GROUP_CONCAT(accounts.guid, ',') AS involved_accounts, " +
                                (uri.getPathSegments().get(0).equals("accounts") ?
                                "sum(transaction_products.quantity * transaction_products.price * " +
                                    "(transaction_products.account_guid IS '" + uri.getPathSegments().get(1) + "')) AS height, "
                                    : "sum(abs(transaction_products.quantity) * transaction_products.price) / 2 AS height, ") +
                                "max(accounts._id), transaction_products.quantity, accounts.parent_guid," +
                                " transactions.status, transaction_products.price, transactions.status" +
                        " FROM transactions" +
                        " LEFT OUTER JOIN sessions ON transactions.session_id = sessions._id" +
                        " LEFT JOIN accounts AS session ON sessions.account_guid = session.guid" +
                        " JOIN transaction_products ON transaction_products.transaction_id = transactions._id" +
                        " LEFT JOIN (" +
                                "SELECT _id, guid, name, max(_id), parent_guid" +
                                " FROM accounts" +
                                (uri.getQueryParameter("before") != null?
                                        " WHERE last_modified < " + uri.getQueryParameter("before") +
                                                " OR last_modified IS NULL" : "") +
                                " GROUP BY guid" +
                        ") AS accounts ON transaction_products.account_guid = accounts.guid" +
                        " WHERE " + selection +
                                ((uri.getQueryParameter("after") != null)?
                                        " AND transactions.start >= " + uri.getQueryParameter("after") : "") +
                                ((uri.getQueryParameter("before") != null)?
                                        " AND transactions.start < " + uri.getQueryParameter("before") : "") +
                        " GROUP BY transactions._id" +
                        " HAVING height != 0" +
                        (uri.getQueryParameter("debit") != null? " AND height > 0" : "") +
                        (uri.getQueryParameter("credit") != null? " AND height < 0" : "") +
                        (uri.getPathSegments().get(0).equals("accounts") ?
                                " AND involved_accounts LIKE '%" + uri.getPathSegments().get(1) + "%'" : "") +
                        " ORDER BY " + (sortOrder != null? sortOrder : "transactions._id"),
                        selectionArgs);
                break;
        }
        result.setNotificationUri(getContext().getContentResolver(), uri);
        return result;
    }

    private Cursor getTransactionSum(String id) {
        return db.getReadableDatabase().rawQuery(
                "SELECT quantity, price, sum(quantity * price)" +
                " FROM transaction_products" +
                " WHERE transaction_id = ?",
                new String[] { id });
    }

    @Override
    public String getType(Uri uri) {
        switch (router.match(uri)) {
            case PRODUCT:
            case PRODUCTS:
            case TRANSACTION_PRODUCTS:
                return AUTHORITY + "/products";
            case ACCOUNT:
                return AUTHORITY + "/accounts";
            case TRANSACTION:
            case TRANSACTIONS:
                return AUTHORITY + "/transactions";
        }
        return null;
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {
        int count = 0;
        db.getWritableDatabase().beginTransaction();
        switch (router.match(uri)) {
            case PRODUCTS:
                for (ContentValues cv : values) {
                    long result = db.getWritableDatabase().insert("products", null, cv);
                    if (result != -1) {
                        count++;
                    }
                }
                break;
        }
        db.getWritableDatabase().setTransactionSuccessful();
        db.getWritableDatabase().endTransaction();
        return count;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        switch (router.match(uri)) {
            case LOAD:
                PreferenceManager.getDefaultSharedPreferences(getContext()).edit()
                        .putString("db", uri.getLastPathSegment()).commit();
                db = new DatabaseHelper(getContext(), uri.getLastPathSegment());
                break;
            case PRODUCTS:
                if (!values.containsKey("guid")) {
                    values.put("guid", UUID.randomUUID().toString());
                }
                uri = ContentUris.withAppendedId(uri,
                        db.getWritableDatabase().insert("products", null, values));
                getContext().getContentResolver().notifyChange(uri, null);
            break;
            case TRANSACTION_PRODUCTS:
                boolean cash = false;
                if (!values.containsKey("unit") || (
                        values.containsKey("product_id") &&
                        values.getAsInteger("product_id") == 2)) {
                    cash = true;
                    values.put("unit", "Stück");
                }
                values.put("transaction_id", uri.getPathSegments().get(1));
                if (!values.containsKey("title")) values.put("title", "");
                if (!values.containsKey("price")) values.put("price", 1);
                values.put("price", values.getAsString("price"));

                String where = "transaction_id = ? AND account_guid = ? " +
                        "AND title IS ? AND price = ? AND unit IS ?";
                String[] whereArgs = new String[] {
                        uri.getPathSegments().get(1),
                        values.getAsString("account_guid"),
                        values.getAsString("title"),
                        values.getAsString("price"),
                        values.getAsString("unit") };
                Cursor existing = db.getWritableDatabase().query(
                        "transaction_products", new String[] {"quantity"},
                        where, whereArgs, null, null, null);
                if (existing.getCount() > 0) {
                    existing.moveToFirst();
                    if (!values.containsKey("quantity")) {
                        values.put("quantity", existing.getFloat(0) - 1);
                    } else if (cash) {
                        values.put("quantity", existing.getFloat(0) + values.getAsFloat("quantity"));
                    }
                    db.getWritableDatabase().update("transaction_products", values, where, whereArgs);
                } else {
                    if (!values.containsKey("quantity")) {
                        values.put("quantity", -1);
                    }
                    db.getWritableDatabase().insert("transaction_products", null, values);
                }
                getContext().getContentResolver().notifyChange(uri, null);
                break;
            case ACCOUNTS:
                if (!values.containsKey("guid")) {
                    values.put("guid", UUID.randomUUID().toString());
                }
                if (uri.getPathSegments().size() > 1) {
                    values.put("parent_guid", uri.getPathSegments().get(1));
                }
                if (!values.containsKey("parent_guid")) {
                    values.put("parent_guid", "passiva");
                }
                db.getWritableDatabase().insert("accounts", null, values);
                uri = Uri.withAppendedPath(uri, values.getAsString("guid"));
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
                            "content://" + AUTHORITY + "/sessions"), b);
                    values.put("session_id", session.getLastPathSegment());
                }
                if (!values.containsKey("status")) {
                    values.put("status", "draft");
                }
                if (!values.containsKey("start")) {
                    values.put("start", System.currentTimeMillis());
                }
                if (!values.containsKey("stop")) {
                    values.put("stop", System.currentTimeMillis());
                }
                if (!values.containsKey("comment")) {
                    values.put("comment", "Kommentar");
                }
                long id = db.getWritableDatabase().insert("transactions", null, values);
                if (id != -1) {
                    uri = ContentUris.withAppendedId(uri, id);
                } else {
                    uri = null;
                }
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
            case PRODUCT:
                db.getWritableDatabase().delete("products", "_id = ?",
                        new String[] { uri.getLastPathSegment() });
                getContext().getContentResolver().notifyChange(
                        Uri.parse("content://" + AUTHORITY + "/products" ), null);
                break;
            case TRANSACTION:
                db.getWritableDatabase().delete("transactions", "_id = ?",
                        new String[] { uri.getLastPathSegment() });
                break;
            case TRANSACTION_PRODUCTS:
                boolean completeDelete = selection != null;
                selection = "transaction_id = ? AND account_guid IS ? AND transaction_products._id = ?";
                selectionArgs = new String[] {
                        uri.getPathSegments().get(1),
                        uri.getPathSegments().get(3),
                        uri.getLastPathSegment() };
                Cursor c = db.getReadableDatabase().query("transaction_products",
                        null, selection, selectionArgs, null, null, null);
                if (c.getCount() == 0) return -1;
                c.moveToFirst();
                if (completeDelete || (-1 <= c.getFloat(4) && c.getFloat(4) <= 1)) {
                    db.getWritableDatabase().delete(
                            "transaction_products", selection, selectionArgs);
                } else {
                    ContentValues dec = new ContentValues();
                    if (c.getFloat(4) < 0) {
                        dec.put("quantity", c.getFloat(4) + 1);
                    } else {
                        dec.put("quantity", c.getFloat(4) - 1);
                    }
                    db.getWritableDatabase().update(
                            "transaction_products", dec, selection, selectionArgs);
                }
                getContext().getContentResolver().notifyChange(Uri.parse("content://" + AUTHORITY +
                        "/transactions/" + uri.getPathSegments().get(1) + "/products"), null);
                break;
        }
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        int result = 0;
        switch (router.match(uri)) {
            case ACCOUNTS:
                db.getWritableDatabase().update("accounts", values, selection, selectionArgs);
                break;
            case PRODUCT:
                db.getWritableDatabase().update("products", values, "_id = " + uri.getLastPathSegment(), null);
                break;
            case TRANSACTION_PRODUCTS:
                db.getWritableDatabase().update("transaction_products",
                        values, "_id = " + uri.getLastPathSegment(), null);
                break;
            case TRANSACTION:
                if (values.containsKey("quantity")) {
                    db.getWritableDatabase().execSQL(
                            "UPDATE transaction_products" +
                                " SET quantity = -1 * quantity" +
                            " WHERE transaction_id = ?",
                            new String[] { uri.getLastPathSegment() });
                } else {
                    if (values.containsKey("status") &&
                            values.getAsString("status").equals("final")) {
                        if (isTransactionValid(uri.getLastPathSegment())) {
                            result = db.getWritableDatabase().update("transactions", values,
                                    "_id = " + uri.getLastPathSegment(), null);
                            result = 1;
                        }
                    }
                }
                break;
            case SESSION:
                String sessionLog = values.getAsString("session_log");
                values.remove("session_log");
                Cursor txns = query(uri.buildUpon().appendPath(
                        "transactions").build(), null, null, null, null);
                db.getWritableDatabase().beginTransaction();
                int alreadyExisting = 0;
                int invalidNotZeroSum = 0;
                while (txns.moveToNext()) {
                    if (!exists(txns.getString(0), txns.getString(2), txns.getString(4))) {
                        if (zerSum(txns.getString(0))) {
                            result += db.getWritableDatabase().update("transactions", values,
                                    "_id = " + txns.getString(0), null);
                        } else {
                            invalidNotZeroSum++;
                        }
                    } else {
                        alreadyExisting++;
                    }
                }
                Log.d(AccountActivity.TAG, "already existing txns: " + alreadyExisting);
                Log.d(AccountActivity.TAG, "invalid non-zero-sum: " + invalidNotZeroSum);
                if (invalidNotZeroSum == 0) {
                    db.getWritableDatabase().setTransactionSuccessful();
                } else {
                    Log.e(AccountActivity.TAG, "roll back! should never happen! ");
                    result = 0;
                }
                db.getWritableDatabase().endTransaction();
                values = new ContentValues();
                values.put("comment", sessionLog +
                    "\nalready existing txns: " + alreadyExisting +
                    "\ninvalid non-zero-sum: " + invalidNotZeroSum);
                db.getWritableDatabase().update("sessions", values,
                        "_id = " + uri.getPathSegments().get(1), null);
                break;
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return result;
    }

    private boolean isTransactionValid(String id) {
        Cursor txn = db.getReadableDatabase().query("transactions", null,
                "_id = " + id, null, null, null, null);
        txn.moveToFirst();
        return zerSum(id) && !exists(txn.getString(0), txn.getString(3), txn.getString(4));
    }

    private boolean zerSum(String id) {
        Cursor sum = getTransactionSum(id);
        sum.moveToFirst();
        return -0.01 < sum.getFloat(2) && sum.getFloat(2) < 0.01;
    }

    private boolean exists(String id, String start, String comment) {
        Cursor c = db.getReadableDatabase().query("transactions", null,
                "status IS 'final' AND start = ? AND comment IS ?",
                new String[] { start, comment }, null, null, null);
        return c.getCount() > 0;
    }
}
