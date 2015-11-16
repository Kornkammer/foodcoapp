package org.baobab.foodcoapp.io;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import au.com.bytecode.opencsv.CSVWriter;

public class BackupExport {

    static SimpleDateFormat df = new SimpleDateFormat();

    public static File create(Context ctx, String id) {

        OutputStream os;
        ZipOutputStream zos;
        File result = file(id + ".zip");
        try {
            os = new FileOutputStream(result);
            zos = new ZipOutputStream(new BufferedOutputStream(os));
            String date = new SimpleDateFormat("yyyy_MM_dd--HH:mm").format(new Date());

            zip(null, new File(Environment.getDataDirectory(),
                    "//data//org.baobab.foodcoapp//databases//foodcoapp.db"),
                    "Knk_" + date + ".BAK", zos);
            transactions(ctx, zos);
            reports(ctx, zos);

            zos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    static void transactions(final Context ctx, ZipOutputStream zos) throws IOException {
        Cursor all = ctx.getContentResolver().query(
                Uri.parse("content://org.baobab.foodcoapp/transactions"),
                null, null, null, null);
        File out = file("transactions.csv");
        exportTransactions(ctx, all, out);
        zip(null, out, zos);
        out.delete();
        all.close();
    }

    static void reports(final Context ctx, ZipOutputStream zos) throws IOException {
        Cursor accounts = ctx.getContentResolver().query(
                Uri.parse("content://org.baobab.foodcoapp/accounts/mitglieder/accounts"),
                null, null, null, null);
        for (int i = 0; i < accounts.getCount(); i++) {
            accounts.moveToPosition(i);
            Cursor account = ctx.getContentResolver().query(
                    Uri.parse("content://org.baobab.foodcoapp/accounts/" +
                            accounts.getString(2) + "/transactions"),
                    null, null, null, null);
            File csv = file(accounts.getString(1) + "_" + accounts.getString(2) + ".csv");
            exportTransactions(ctx, account, csv);
            zip("Kontoumsätze", csv, zos);
            csv.delete();
            account.close();
        }

    }

    private static void exportTransactions(final Context ctx, final Cursor c, final File file) {
        CSVWriter out;
        try {
            out = new CSVWriter(new FileWriter(file));
            while (c.moveToNext()) {
                out.writeNext(writeTransaction(c));
            }
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
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
