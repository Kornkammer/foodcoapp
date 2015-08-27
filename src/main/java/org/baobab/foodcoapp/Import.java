package org.baobab.foodcoapp;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

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

    public static SimpleDateFormat date = new SimpleDateFormat("dd.MM.yyyy");
    private static ArrayList<ContentValues> products;
    private static ArrayList<ContentValues> transactions;

    public static String file(AppCompatActivity ctx, Uri uri) {
        try {
            products = new ArrayList<ContentValues>();
            transactions = new ArrayList<ContentValues>();

            InputStream is = ctx.getContentResolver().openInputStream(ctx.getIntent().getData());
            CSVReader csv = new CSVReader(new BufferedReader(new InputStreamReader(is, "utf-8")), ';');

            String[] line = csv.readNext();
            while ((line = csv.readNext()) != null) {
                if (line.length == 69) {
                    storeProduct(ctx, line);
                } else if (line.length == 22) {
                    storeTransaction(ctx, line);
                } else {
                    Log.d("Foo", "Could not import line " + line.length + " " + line);
                }
            }
            if (products.size() > 0) {
                Log.d("Foo", "number of products to import " + products.size());
                int count = ctx.getContentResolver().bulkInsert(Uri.parse(
                            "content://org.baobab.foodcoapp/products"),
                        products.toArray(new ContentValues[products.size()]));
                Log.d("Foo", "imported " + count);
                return "Imported " + count + " products \n(out of " + products.size() + ")";
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return "Import Failed";
    }

    private static void storeProduct(Context ctx, String[] line) {
        ContentValues cv = new ContentValues();
        cv.put("ean", line[4]);
        cv.put("unit", "Stück");
        cv.put("title", line[6]);
        cv.put("price", Float.valueOf(line[37].replace(",", ".")));
        products.add(cv);
        Log.d("Bar", "+ store: " + line[4] + ": " + line[6] + " || " + line[7] + " - " + line[37]);
    }

    private static void storeTransaction(Context ctx, String[] line) throws ParseException {
        System.out.println(line[1]);
        System.out.println(date.format(date.parse(line[1])));
        System.out.println(line[3]);
        System.out.println(line[4]);
        System.out.println(line[19]);
    }
}
