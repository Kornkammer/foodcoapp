package org.baobab.foodcoapp.io;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import org.baobab.foodcoapp.AccountActivity;
import org.baobab.foodcoapp.ImportActivity;

import java.io.IOException;
import java.text.ParseException;
import java.util.Date;

import au.com.bytecode.opencsv.CSVReader;

public class KnkImport implements ImportActivity.Importer {

    static final String TAG = AccountActivity.TAG;
    String msg = "";
    Context ctx;
    int count = 0;
    private Uri txn;
    private Uri session;
    private Date time;


    public KnkImport(Context ctx) {
        this.ctx = ctx;
    }

    @Override
    public int read(CSVReader csv) throws IOException {
        count = 0;
        String[] line;
        session = storeSession();
        while ((line = csv.readNext()) != null) {
            if (line[0].equals("")) {
            } else if (line[0].equals("KNK Version 0.1")) {
                Log.i(TAG, "TRANSACTION");
                try {
                    time = KnkExport.df.parse(line[2]);
                } catch (ParseException e) {
                    e.printStackTrace();
                    Log.e(TAG, "bad date parse fail: " + line[2]);
                    Toast.makeText(ctx, "bad date parse fail: " + line[2], Toast.LENGTH_LONG).show();
                }
                txn = storeTransaction(session, time, line[1]);
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
            sum += Float.valueOf(line[4].replace(",", ".")) * Float.valueOf(line[1].replace(",", "."));
            Log.d(TAG, "+ read: " + line[0] + " :: " + line[3] + ":    " +
                    line[1] + " " + line[2] + " x " + line[4] +
                    "      sum=" + sum);
            String product = null;
            String img = null;
            if (line[0].toLowerCase().equals("lager")) {
                Cursor p = findProduct(line);
                if (p != null) {
                    product = String.valueOf(p.getLong(0));
                    img = p.getString(4);
                }
            }
            storeTransactionItem(txn, line[0].toLowerCase(), product, img, line);
            return 1;
        } catch (Exception e) {
            Log.e(TAG, "Error reading line " + e.getMessage());
            return 0;
        }
    }

    private Uri storeTransaction(Uri session, Date time, String comment) {
        ContentValues t = new ContentValues();
        t.put("stop", System.currentTimeMillis());
        t.put("start", time.getTime());
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

    private ContentValues storeTransactionItem(Uri txn, String account, String product, String img, String[] line) {
        ContentValues cv = new ContentValues();
        cv.put("account_guid", account);
        if (account.equals("bank") || account.equals("kasse")) {
            cv.put("product_id", 1);
        } else if (!account.equals("inventar") && !account.equals("kosten") && !account.equals("lager")
                && !account.equals("einlagen") && !account.equals("beiträge") && !account.equals("spenden")
                && !account.equals("forderungen") && !account.equals("verbindlichkeiten")) {
            cv.put("product_id", 2);
        } else {
            cv.put("product_id", product != null? product : "3");
        }
        cv.put("title", line[3].trim());
        cv.put("quantity", Float.valueOf(line[1].replace(",", ".")));
        cv.put("unit", line[2].trim());
        cv.put("price", Float.valueOf(line[4].replace(",", ".")));
        if (img != null) cv.put("img", img);
        ctx.getContentResolver().insert(txn.buildUpon()
                .appendEncodedPath("products").build(), cv);
        return cv;
    }

    private Cursor findProduct(String[] line) {
        float price = Float.valueOf(line[4].replace(",", "."));
        Cursor p = ctx.getContentResolver().query(Uri.parse(
                        "content://org.baobab.foodcoapp/products"),
                null, "title IS ? AND ROUND(price, 2) = ROUND(?, 2)", new String[] {
                        line[3].trim(), String.valueOf(price) }, null);
        if (p.getCount() > 0) {
            p.moveToFirst();
            System.out.println(p.getString(7) + " product found");
            return p;
        } else {
            return null;
        }
    }
}
