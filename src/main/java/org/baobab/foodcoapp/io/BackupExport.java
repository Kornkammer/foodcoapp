package org.baobab.foodcoapp.io;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.preference.PreferenceManager;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import au.com.bytecode.opencsv.CSVWriter;

public class BackupExport {

    public static SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy");
   public static SimpleDateFormat YEAR = new SimpleDateFormat("yyyy");


    public static File create(Context ctx, String id) {

        OutputStream os;
        ZipOutputStream zos;
        File result = file(id + ".zip");
        try {
            os = new FileOutputStream(result);
            zos = new ZipOutputStream(new BufferedOutputStream(os));
            String date = new SimpleDateFormat("yyyy_MM_dd--HH:mm").format(new Date());

            String db = PreferenceManager.getDefaultSharedPreferences(ctx)
                    .getString("db", "foodcoapp.db");
            zip(null, new File(Environment.getDataDirectory(),
                    "//data//org.baobab.foodcoapp//databases//" + db),
                    "Knk_" + date + ".BAK", zos);
            transactions(ctx, zos, 0);
            reports(ctx, zos);
            lager(ctx, zos);

            zos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    static void transactions(final Context ctx, ZipOutputStream zos, int year) throws IOException {
        File tmp = null;
        String selection = getTimeWindowSelection(year);
        Cursor all = ctx.getContentResolver().query(
                Uri.parse("content://org.baobab.foodcoapp/transactions"),
                null, selection, null, "transactions.start");
        if (year > 0) {
            tmp = file(year + "_transactions.csv");
            exportTransactions(ctx, all, tmp);
            zip("" + year, tmp, zos);
        } else {
            all.moveToFirst();
            int first = Integer.parseInt(YEAR.format(all.getLong(2)));
            all.moveToLast();
            int last = Integer.parseInt(YEAR.format(all.getLong(2)));
            for (int y = first; y <= last; y++) {
                transactions(ctx, zos, y);
                reports(ctx, zos, y);
            }
            all.moveToPosition(-1);
            tmp = file("all_transactions.csv");
            exportTransactions(ctx, all, tmp);
            zip(null, tmp, zos);
        }
        tmp.delete();
        all.close();
    }

    @NonNull
    private static String getTimeWindowSelection(int year) {
        String selection = "transactions.status IS NOT 'draft'";
        if (year > 0) {
            try {
                long from = YEAR.parse("" + year).getTime();
                long to = YEAR.parse("" + (year + 1)).getTime();
                selection += " AND " + from + " <= transactions.start AND transactions.start < " + to;
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        return selection;
    }

    static void reports(final Context ctx, ZipOutputStream zos, int year) throws IOException {
        String timewindow = "";
        if (year > 0) {
            try {
                timewindow = "after=" + YEAR.parse("" + year).getTime() +
                        "&before=" + YEAR.parse("" + (year + 1)).getTime();
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        Cursor accounts = getMembers(ctx, "?" + timewindow, null);
        File tmp = file("mitglieder.csv");
        CSVWriter csv;
        try {
            csv = new CSVWriter(new FileWriter(tmp), ';', CSVWriter.NO_QUOTE_CHARACTER);
            csv.writeNext(new String[] {
                    "Nr", "Name", "Beitritt",
                    "Einlage", "",
                    "Beitrag", "",
                    "soll", "ist", "",
                    "Korns", "ein", "aus", "gut",
                    "Einlage zurück", "" });
            csv.writeNext(new String[] { } );
            while (accounts.moveToNext()) {
                String guid = accounts.getString(2);
                String name = accounts.getString(1);
                report(ctx, zos, guid, name, year);
                Cons einlage = getContribution(ctx, "?credit=true", name, "einlagen");
                Cons auslage = getContribution(ctx, "?debit=true", name, "einlagen");
                Cons beitrag = getContribution(ctx, "", name, "beiträge");
                long joined = accounts.getLong(5);
                int fee = accounts.getInt(8);
                if (fee == 0) fee = 9;
                int days = (int) ((System.currentTimeMillis() - joined) / 86400000);
                float pre = 0;
                float post = 0;
                if (year > 0) {
                    try {
                        long after = YEAR.parse("" + year).getTime();
                        long before = YEAR.parse("" + (year + 1)).getTime();
                        days = (int) (((Math.min(System.currentTimeMillis(), before) - Math.max(after, joined)) / 86400000));
                        post = Math.max(0, System.currentTimeMillis() - before) / 86400000;
                        pre = Math.max(0, after - joined) / 86400000;
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }
                int soll = Math.round(days * fee * 12f/365f);
                int ist = Math.round(Math.min(soll, Math.max(0, beitrag.sum - (pre * fee * 12f/365))));
                String balance = " :-)";
                if (ist != soll) {
                    if (ist > soll) {
                        balance = "noch " + Math.round((ist - soll) / (fee * 12f/365)) + " Tage gut";
                    } else {
                        balance = "noch " + Math.round(soll - ist) + "€ offen";
                    }
                }
                Cursor debit = getMembers(ctx, "?debit=true" + timewindow, name);
                debit.moveToFirst();
                Cursor credit = getMembers(ctx, "?credit=true" + timewindow, name);
                credit.moveToFirst();

                csv.writeNext(new String[] { guid, name, df.format(joined),
                        "" + einlage.sum, einlage.dates,
                        "" + fee, "€/Mon an " + days + " Tagen",
                        "" + soll, "" + ist, balance, "",
                            String.format("%.2f", (-1 * credit.getFloat(3))),
                            String.format("%.2f", (-1 * debit.getFloat(3))),
                            String.format("%.2f", (-1 * accounts.getFloat(3))),
                        "" + auslage.sum, auslage.dates } );
            }
            csv.close();
            if (year > 0) {
                zip("" + year, tmp, zos);
            } else {
                zip(null, tmp, zos);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static Cursor getMembers(Context ctx, String query, String name) {
        return ctx.getContentResolver().query(
                    Uri.parse("content://org.baobab.foodcoapp/accounts/mitglieder/accounts" + query),
                    null, (name != null? "name is '" + name + "'" : null), null, null);
    }

    private static class Cons {
        String dates = "";
        float sum;
    }
    private static Cons getContribution(Context ctx, String query, String title, String account) {
        Cons cons = new Cons();
        Cursor txns = getTxns(ctx, query, title, account);
        if (txns.getCount() > 0) cons.dates = "€ am ";
        while (txns.moveToNext()) {
            cons.sum += (-1 * txns.getFloat(6));
            cons.dates += df.format(txns.getLong(2)) + ", ";
        }
        return cons;
    }
    private static Cursor getTxns(Context ctx, String query, String title, String account) {
        Cursor txns = ctx.getContentResolver().query(
                Uri.parse("content://org.baobab.foodcoapp/accounts/" + account + "/transactions" + query),
                null, "transactions.status IS NOT 'draft' AND title IS '" + title + "'", null, null);
        return txns;
    }

    private static void report(Context ctx, ZipOutputStream zos, String guid, String name, int year) throws IOException {
        Cursor account = ctx.getContentResolver().query(
                Uri.parse("content://org.baobab.foodcoapp/accounts/" +
                        guid + "/transactions"),
                null, getTimeWindowSelection(year), null, null);
        File csv = file(guid + "_" + name + "_transactions.csv");
        exportTransactions(ctx, account, csv);
        if (year > 0) {
            zip(year + "//Umsatz", csv, zos);
        } else {
            zip("Umsatz", csv, zos);
        }
        csv.delete();
        account.close();
    }

    static void lager(final Context ctx, ZipOutputStream zos) throws IOException {
        File stock = KnkExport.write(ctx,
                Uri.parse("content://org.baobab.foodcoapp/accounts/lager/products"),
                "Inventur", new Date().getTime(), file("lagerbestand.knk"));
        zip(null, stock, zos);
        stock.delete();
    }

    private static void exportTransactions(final Context ctx, final Cursor c, final File file) {
        CSVWriter out;
        try {
            out = new CSVWriter(new FileWriter(file), ';', CSVWriter.NO_QUOTE_CHARACTER);
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
