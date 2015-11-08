package org.baobab.foodcoapp;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.util.Log;

public class BnnImporter implements Import.Importer {

    private final Context ctx;

    public BnnImporter(Context ctx) {
        this.ctx = ctx;
    }

    @Override
    public ContentValues read(String[] line) {
        Log.d("Bar", "+ read: " + line[4] + ": " + line[6] + " || " + line[7] + " - " + line[37]);
        ContentValues cv = new ContentValues();
        cv.put("ean", line[4]);
        cv.put("unit", "Stück");
        cv.put("title", line[6]);
        cv.put("price", Float.valueOf(line[37].replace(",", ".")));
        return cv;
    }

    @Override
    public Import.Result store(ContentValues[] values) {
        int count = ctx.getContentResolver().bulkInsert(Uri.parse(
                "content://org.baobab.foodcoapp/products"), values);
        Import.Result result = new Import.Result();
        result.msg = "Imported " + count + " (out of " + values.length + " products)";
        return result;
    }
}
