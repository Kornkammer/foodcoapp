package org.baobab.pos;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;

import au.com.bytecode.opencsv.CSVWriter;

public class Export {

    static SimpleDateFormat df = new SimpleDateFormat();

    static void csv(final Context ctx) {
        new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground (Void...params){
                CSVWriter file;
                try {
                    file = new CSVWriter(new FileWriter(file()));
                    Cursor c = ctx.getContentResolver().query(
                            Uri.parse("content://org.baobab.pos/transactions"),
                            null, null, null, null);
                    while (c.moveToNext()) {
                        file.writeNext(writeTransaction(c));
                    }
                    file.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }
        }.execute();
    }

    private static String[] writeRow(Cursor c) {
        int count = c.getColumnCount();
        String[] row = new String[count];
        for (int i = 0;i < count; i++) {
            row[i] = c.getString(i);
        }
        return row;
    }

    private static String[] writeTransaction(Cursor c) {
        int count = c.getColumnCount();
        String[] row = new String[count];
        row[0] = c.getString(0);
        row[1] = c.getString(1);
        row[2] = df.format(c.getLong(2));
        row[3] = c.getString(3);
        row[4] = c.getString(4);
        row[5] = c.getString(5);
        return row;
    }

    public static File file() {
        return new File(Environment.getExternalStorageDirectory().getAbsolutePath()
                + "/transactions.csv");
    }
}
