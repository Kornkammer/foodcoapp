package org.baobab.foodcoapp;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;

import au.com.bytecode.opencsv.CSVReader;

public class BnnImport implements ImportActivity.Importer {

    static final String TAG = AccountActivity.TAG;
    ArrayList<ContentValues> values;
    String msg = "";
    Context ctx;

    public BnnImport(Context ctx) {
        this.ctx = ctx;
    }

    @Override
    public int read(CSVReader csv) throws IOException {
        int count = 0;
        String[] line;
        values = new ArrayList<>();
        while ((line = csv.readNext()) != null) {
            ContentValues product = readLine(line);
            if (product != null) {
                values.add(product);
            }
            count++;
        }
        if (values.size() != count) {
            msg += "Could not read " + (count - values.size()) + " products (of " + count +")\n";
        }
        return store();
    }

    @Override
    public Uri getSession() {
        return null;
    }

    @Override
    public String getMsg() {
        return msg;
    }

    public int store() {
        int count = ctx.getContentResolver().bulkInsert(Uri.parse(
                        "content://org.baobab.foodcoapp/products"),
                values.toArray(new ContentValues[values.size()]));
        msg = "\nImported " + count + " (out of " + values.size() + " products)\n";
        return count;
    }

    public ContentValues readLine(String[] line) {
        try {
            Log.d(TAG, "+ read: " + line[4] + ": " + line[6] + " || " + line[7] + " - " + line[37]);
            ContentValues cv = new ContentValues();
            cv.put("ean", line[4]);
            cv.put("unit", "Stück");
            cv.put("title", line[6]);
            cv.put("price", Float.valueOf(line[37].replace(",", ".")));
            return cv;
        } catch (Exception e) {
            Log.e(TAG, "Error reading line " + e.getMessage());
            return null;
        }
    }

}
