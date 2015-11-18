package org.baobab.foodcoapp.io;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import org.baobab.foodcoapp.AccountActivity;
import org.baobab.foodcoapp.ImportActivity;

import java.io.IOException;

import au.com.bytecode.opencsv.CSVReader;

public class KnkAltImport implements ImportActivity.Importer {

    static final String TAG = AccountActivity.TAG;
    String msg = "";
    Context ctx;
    int count = 0;
    private Uri txn;
    private Uri session;
    private Long time;


    public KnkAltImport(Context ctx) {
        this.ctx = ctx;
    }

    @Override
    public int read(CSVReader csv) throws IOException {
        count = 0;
        String[] line;
        session = storeSession();
        while ((line = csv.readNext()) != null) {
            if (line[0].equals("")) {
            } else if (line.length == 2) {
                Log.i(TAG, "TRANSACTION");
                time = Long.parseLong(line[1]);
                txn = storeTransaction(session, time, line[0]);
                sum = 0;
                count++;
            } else {
                readLine(line);
            }
        }
        return count;
    }

    @Override
    public Uri getSession() {
        return session;
    }

    @Override
    public String getMsg() {
        return msg;
    }

    float sum;
    public int readLine(String[] line) {
        try {
            sum += Float.valueOf(line[3].replace(",", ".")) * Float.valueOf(line[2].replace(",", "."));
            Log.d(TAG, "+ read: " + line[0] + ": " + line[1] + ":    " +
                    line[3] + " " + (line.length == 5? line[4] : "") + " x " +line[2] + " EUR" +
                    "      sum=" + sum);
            Uri product = null;
            if (line[0].toLowerCase().equals("lager")) {
                product = findProduct(line);
            }
            storeTransactionItem(txn, line[0].toLowerCase(),
                    (product!=null? product.getLastPathSegment() : "3"), line);
            return 1;
        } catch (Exception e) {
            Log.e(TAG, "Error reading line " + e.getMessage());
            return 0;
        }
    }

    private Uri storeTransaction(Uri session, long time, String comment) {
        ContentValues t = new ContentValues();
        t.put("start", System.currentTimeMillis());
        t.put("stop", time);
        t.put("status", "draft");
        t.put("comment", comment);
        t.put("session_id", session.getLastPathSegment());
        return ctx.getContentResolver().insert(Uri.parse(
                "content://org.baobab.foodcoapp/transactions"), t);
    }

    private Uri storeSession() {
        ContentValues cv = new ContentValues();
        cv.put("start", System.currentTimeMillis());
        return ctx.getContentResolver().insert(Uri.parse(
                "content://org.baobab.foodcoapp/sessions"), cv);
    }

    private ContentValues storeTransactionItem(Uri txn, String account, String product_id, String[] line) {
        ContentValues cv = new ContentValues();
        cv.put("account_guid", account);
        cv.put("product_id", 3);
        cv.put("title", line[1]);
        cv.put("quantity", Float.valueOf(line[3].replace(",", ".")));
        if (line.length == 5) {
            cv.put("unit", line[4]);
        }
        if (!line[1].equals("")) {
            cv.put("price", Float.valueOf(line[2].replace(",", ".")));
        }
        ctx.getContentResolver().insert(txn.buildUpon()
                .appendEncodedPath("products").build(), cv);
        return cv;
    }

    private Uri findProduct(String[] line) {
        float price = Float.valueOf(line[2].replace(",", "."));
        Cursor p = ctx.getContentResolver().query(Uri.parse(
                        "content://org.baobab.foodcoapp/products"),
                null, "title IS ? AND price = ?", new String[] {
                        line[1], Float.toString(price) }, null);
        if (p.getCount() == 0) {
            Cursor s = ctx.getContentResolver().query(Uri.parse(
                            "content://org.baobab.foodcoapp/products"),
                    new String[] { "max(button)" }, null, null, null);
            s.moveToFirst();
            int button = s.getInt(0) + 1;
            ContentValues cv = new ContentValues();
            cv.put("title", line[1]);
            cv.put("button", button);
            cv.put("price", price);
            if (line.length == 5) {
                cv.put("unit", line[4]);
            }
            cv.put("img", "android.resource://org.baobab.foodcoapp/drawable/ic_korn");
            Uri uri = ctx.getContentResolver().insert(Uri.parse(
                    "content://org.baobab.foodcoapp/products"), cv);
            return uri;
        }
        return null;
    }

}
