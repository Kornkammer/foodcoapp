package org.baobab.foodcoapp.io;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import au.com.bytecode.opencsv.CSVWriter;

public class KnkExport {

    static final SimpleDateFormat df = new SimpleDateFormat("yyyy_MM_dd--HH:mm");

    public static File create(Context ctx, Uri txn) {
        File result = file("txn_" + txn.getLastPathSegment() + ".knk");
        Cursor t = ctx.getContentResolver().query(txn, null, null, null, null);
        if (t.getCount() == 0) {
            return result;
        }
        t.moveToFirst();
        return write(ctx, txn.buildUpon().appendEncodedPath("products").build(),
                t.getString(4), t.getLong(2), result);
    }

    public static File write(Context ctx, Uri products, String comment, long time, File result) {
        CSVWriter out;
        try {
            out = new CSVWriter(new FileWriter(result), ';', CSVWriter.NO_QUOTE_CHARACTER);
            out.writeNext(new String[] { "Konto", "Menge", "Einheit", "Titel", "Preis" });
            out.writeNext(new String[] { } );
            out.writeNext(new String[] { "KNK Version 0.1", comment, df.format(time) });

            Cursor p = ctx.getContentResolver().query(products, null, null, null, null);
            while (p.moveToNext()) {
                out.writeNext(new String[] {
                        p.getString(2),
                        String.format("%.2f", p.getFloat(4)),
                        p.getString(6),
                        p.getString(7),
                        String.format("%.2f", p.getFloat(5))});
            }
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    private static String[] writeTransaction(Cursor c) {
        int count = c.getColumnCount();
        String[] row = new String[count];
        row[0] = df.format(c.getLong(2));
        row[1] = c.getString(3);
        row[2] = c.getString(4);
        String sign;
        if (c.getString(9).equals("aktiva")) {
            sign = c.getInt(8) < 0? "-" : "+";
        } else {
            sign = c.getInt(8) > 0? "-" : "+";
        }
       row[3] = sign + String.format("%.2f", c.getFloat(6));
        return row;
    }

    public static File file(String name) {
        return new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + name);
    }

    public static void zip(File file, ZipOutputStream zos) throws IOException {
        zip(null, file, zos);
    }

    public static void zip(String dir, File file, ZipOutputStream zos) throws IOException {
        zip(dir, file, file.getName(), zos);
    }

    public static void zip(String dir, File file, String name, ZipOutputStream zos) throws IOException {
        zos.putNextEntry(new ZipEntry((dir != null? dir + "/" : "") + name));
        BufferedInputStream in = new BufferedInputStream(new FileInputStream(file), 2048);
        int count;
        byte buf[] = new byte[2048];
        while ((count = in.read(buf, 0, 2048)) != -1) {
            zos.write(buf, 0, count);
        }
        zos.closeEntry();
    }
}
