package org.baobab.foodcoapp;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

import au.com.bytecode.opencsv.CSVReader;

public class Import {

    public interface Importer {
        ContentValues read(String[] line);
        Result store(ContentValues[] values);
    }

    static public class Result {
        public String msg;
        public Uri session;

    }

    static final String TAG = PosActivity.TAG;

    public static Result file(AppCompatActivity ctx, Uri uri) {
        ArrayList<Object> values = new ArrayList<>();
        Result result = new Result();
        Importer importer = null;
        try {
            InputStream is = ctx.getContentResolver().openInputStream(uri);
            CSVReader csv = new CSVReader(new BufferedReader(new InputStreamReader(is, "utf-8")), ';');

            String[] line = csv.readNext();
            if (line.length == 69) {
                importer = new BnnImporter(ctx);
            } else if (line.length == 22) {
                importer = new GlsImporter(ctx);
            } else {
                Log.d(TAG, "No idea how to import " + line.length + " -> " + line);
                result.msg = "No idea how to import this :/";
            }
            if (importer != null) {
                while ((line = csv.readNext()) != null) {
                    ContentValues cv = importer.read(line);
                    if (cv != null) {
                        values.add(cv);
                    } else {
                        Log.d(TAG, "Could not import line " + line.length + " -> " + line);
                    }
                }
                if (values.size() > 0) {
                    Log.d(TAG, "number of values to import " + values.size());
                    result = importer.store(values.toArray(new ContentValues[values.size()]));
                    Log.d(TAG, result.msg);
                } else {
                    result.msg = "Could not import " + importer.getClass().getSimpleName();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error " + e);
            e.printStackTrace();
            result.msg += "\nImport Failed! " + e.getMessage();
            return result;
        }
        return result;
    }

}
