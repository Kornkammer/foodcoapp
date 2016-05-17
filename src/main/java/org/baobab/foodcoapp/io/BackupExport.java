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
import java.util.Locale;
import java.util.TimeZone;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import au.com.bytecode.opencsv.CSVWriter;

public class BackupExport {

    public static SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy", Locale.GERMAN);
    public static SimpleDateFormat YEAR = new SimpleDateFormat("yyyy", Locale.GERMAN);
    static {
        df.setTimeZone(TimeZone.getTimeZone("Europe/Berlin"));
        YEAR.setTimeZone(TimeZone.getTimeZone("Europe/Berlin"));
    }


    public static File create(Context ctx, String id) {

        OutputStream os;
        ZipOutputStream zos;
        File result = file(id + ".zip");
        try {
            os = new FileOutputStream(result);
            zos = new ZipOutputStream(new BufferedOutputStream(os));
            String date = new SimpleDateFormat("yyyy_MM_dd--HH:mm", Locale.GERMAN).format(new Date());

            String db = PreferenceManager.getDefaultSharedPreferences(ctx)
                    .getString("db", "foodcoapp.db");
            zip(null, new File(Environment.getDataDirectory(),
                    "//data//org.baobab.foodcoapp//databases//" + db),
                    "Knk_" + date + ".BAK", zos);
            transactions(ctx, zos, 0);
            reports(ctx, zos, 0);
            lager(ctx, zos, "lager");
            lager(ctx, zos, "kosten");
            lager(ctx, zos, "inventar");

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

    private static String getTimeWindowSelection(int year) {
        String selection = "transactions.status IS 'final'";
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
        File tmp;
        if (year > 0) {
            tmp = file(year + "_mitglieder.csv");
        } else {
            tmp = file("mitglieder.csv");
        }
        CSVWriter csv;
        try {
            csv = new CSVWriter(new FileWriter(tmp), ';', CSVWriter.NO_QUOTE_CHARACTER);
            csv.writeNext(new String[] {
                    "Nr", "Name", "Beitritt",
                    "Einlage", "",
                    "Beitrag", "", (year > 0? "Stand 1. Jan" : ""), "" + year, "soll",
                    "gezahlt", "Stand " + (year == 0 || System.currentTimeMillis() < YEAR.parse("" + (year + 1)).getTime()?
                           df.format(System.currentTimeMillis()) :  "31. Dez " + year ),
                    "Korns", (year > 0? "Stand 1. Jan " : ""), "ein", "aus", "gut",
                    "Einlage zurück", "" });
            csv.writeNext(new String[] { } );
            float sumPrePaidBefore = 0;
            float sumPostPaidThisY = 0;
            float sumSoll = 0;
            float sumPaid = 0;
            float sumPrePaidThisY = 0;
            float sumStillOpen = 0;
            while (accounts.moveToNext()) {
                String guid = accounts.getString(2);
                String name = accounts.getString(1);
                report(ctx, zos, guid, name, year);
                Cons einlage = getContribution(ctx, "?credit=true", name, "einlagen");
                Cons auslage = getContribution(ctx, "?debit=true", name, "einlagen");
                long joined = accounts.getLong(5);
                int fee = accounts.getInt(8);
                if (fee == 0) fee = 9;
                int days = (int) ((System.currentTimeMillis() - joined) / 86400000);
                float preDays = 0;
                float gut = 0;
                Cons paid = getContribution(ctx, "?" + timewindow, name, "beiträge");
                Cursor standBegin = null;
                Cursor standEnd = null;
                if (year > 0) {
                    try {
                        long after = YEAR.parse("" + year).getTime();
                        long before = YEAR.parse("" + (year + 1)).getTime();
                        days = (int) (((Math.min(System.currentTimeMillis(), before) - Math.max(after, joined)) / 86400000));
                        preDays = Math.max(0, after - joined) / 86400000;
                        Cons prePaid = getContribution(ctx, "?before=" + after, name, "beiträge");
                        gut = prePaid.sum - preDays * fee * 12f/365;
                        Cons duePaid = getContribution(ctx, "?before=" + before, name, "beiträge");
                        standBegin = getMembers(ctx, "?before=" + after, name);
                        standBegin.moveToFirst();
                        standEnd = getMembers(ctx, "?before=" + before, name);
                        standEnd.moveToFirst();
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }
                int soll = Math.round(days * fee * 12f/365);
                float result = gut + paid.sum - soll;
                String balance;
                int resultDays = Math.round(result / (fee * 12f/365));
                if (result < 0) {
                    sumStillOpen += (-1 * result);
                    balance = "noch (" + (-1 * resultDays) + " Tage) " + Math.round(-1 * result) + "€ offen";
                } else {
                    balance = "noch (" + resultDays + " Tage) " + Math.round(result) + "€ gut";
                    sumPrePaidThisY += result;
                }
                if (gut > 0) {
                    sumPrePaidBefore += gut;
                } else {
                    // actually paid this year???
                    sumPostPaidThisY += (-1 * gut);
                }
                sumSoll += soll;
                sumPaid += paid.sum;
                Cursor debit = getMembers(ctx, "?debit=true&" + timewindow, name);
                debit.moveToFirst();
                Cursor credit = getMembers(ctx, "?credit=true&" + timewindow, name);
                credit.moveToFirst();

                csv.writeNext(new String[] { guid, name, df.format(joined),
                        "" + einlage.sum, einlage.dates, "" + fee, "€/Monat",
                        (year > 0 && gut != 0? "" + Math.round(gut) : ""),
                        days + " Tage", "" + soll, "" + paid.sum, balance, "",
                        (year > 0 && standBegin.getCount() > 0?
                            String.format(Locale.ENGLISH, "%.2f", (-1 * standBegin.getFloat(3))) : "0"),
                            String.format(Locale.ENGLISH, "%.2f", (-1 * credit.getFloat(3))),
                            String.format(Locale.ENGLISH, "%.2f", (-1 * debit.getFloat(3))),
                        (year > 0 && standBegin.getCount() > 0?
                            String.format(Locale.ENGLISH, "%.2f", (-1 * standEnd.getFloat(3))) :
                                String.format(Locale.ENGLISH, "%.2f", (-1 * accounts.getFloat(3)))),
                        (auslage.sum != 0? "" + auslage.sum : ""), auslage.dates } );
            }
            csv.close();
            File eur;
            if (year > 0) {
                zip("" + year, tmp, zos);
                eur = file(year + "_eur.csv");
            } else {
                zip(null, tmp, zos);
                eur = file("eur.csv");
            }
            tmp.delete();
            long after;
            long before;
            if (year > 0) {
                System.out.println("year " + year);
                after = YEAR.parse("" + year).getTime();
                before = YEAR.parse("" + (year + 1)).getTime();
                if (System.currentTimeMillis() < before) {
                    before = System.currentTimeMillis();
                }
            } else {
                System.out.println("all time");
                after = 0;
                before = System.currentTimeMillis();
            }
            String y = (year > 0? "" + year : "");
            csv = new CSVWriter(new FileWriter(eur), ';', CSVWriter.NO_QUOTE_CHARACTER);
            csv.writeNext(new String[] {(year > 0? "Jahr " + y : "alle Jahre Stand " + df.format(before)), "Einnahmen", "Ausgaben"});
            csv.writeNext(new String[] { } );
            Cursor anfang = getAccounts(ctx, "aktiva", "?before=" + after , "Bank");
            anfang.moveToFirst();
            csv.writeNext(new String[] {"Bank Konto Stand 1. Jan " + y, String.format(Locale.ENGLISH, "%.2f", anfang.getFloat(3)), ""});
            csv.writeNext(new String[] { } );
            Cursor einlagen = getAccounts(ctx, "passiva", "?" + timewindow, "Einlagen");
            einlagen.moveToFirst();
            csv.writeNext(new String[] {"Mitglieder Einlagen " + y, String.format(Locale.ENGLISH, "%.2f", -1 * einlagen.getFloat(3)), ""});
            Cursor anschaffungen = getAccounts(ctx, "aktiva", "?debit=true&" + timewindow, "Inventar");
            anschaffungen.moveToFirst();
            csv.writeNext(new String[] {"Anschaffung Inventar " + y, "", "-" + String.format(Locale.ENGLISH, "%.2f", anschaffungen.getFloat(3))});
            Cursor abschreibungen = getAccounts(ctx, "aktiva", "?credit=true&" + timewindow, "Inventar");
            abschreibungen.moveToFirst();
            csv.writeNext(new String[] {"Abschreibung Inventar " + y, String.format(Locale.ENGLISH, "%.2f", -1 * abschreibungen.getFloat(3)), ""});
            csv.writeNext(new String[] { } );

            csv.writeNext(new String[] {"Mitglieder Beiträge " + y, String.format(Locale.ENGLISH, "%.2f", sumSoll), ""});
            if (year > 0) {
                csv.writeNext(new String[] {"davon im Vorraus " + (year - 1), "", "-" + String.format(Locale.ENGLISH, "%.2f", sumPrePaidBefore)});
                csv.writeNext(new String[] {"nachträglich für " + (year - 1), String.format(Locale.ENGLISH, "%.2f", sumPostPaidThisY), ""});
            }
            csv.writeNext(new String[] {"Vorschuss Beiträge", String.format(Locale.ENGLISH, "%.2f", sumPrePaidThisY), ""});
            csv.writeNext(new String[] {"ausstehende Beiträge " + y, "", "-" + String.format(Locale.ENGLISH, "%.2f", sumStillOpen)});
            csv.writeNext(new String[] { } );
            //csv.writeNext(new String[] {"eingegangen " + (year), String.format(Locale.ENGLISH, "%.2f", sumPaid), ""});
            //csv.writeNext(new String[] { } );

            Cursor kosten = getAccounts(ctx, "aktiva", "?" + timewindow, "Kosten");
            kosten.moveToFirst();
            csv.writeNext(new String[] {"Kosten " + y, "", "-" + String.format(Locale.ENGLISH, "%.2f", kosten.getFloat(3))});
            csv.writeNext(new String[] { } );

            Cursor in = getTxns(ctx, "?debit=true&" + timewindow, null, "lager");
            while (in.moveToNext()) {
                System.out.println(in.getFloat(6));
                String[] a = in.getString(5).split(",");
                Cursor prods = ctx.getContentResolver().query(Uri.parse(
                        "content://org.baobab.foodcoapp/transactions/" + in.getInt(0) + "/products"), null, null, null, null);
                while (prods.moveToNext()) {
                    System.out.println("-> " + prods.getString(2) + " :: " + prods.getString(7));
                    if (!prods.getString(2).equals("lager")) {
                        Cursor orig = getTxns(ctx, "?" + timewindow, prods.getString(7), prods.getString(2));
                        while (orig.moveToNext()) {
                            System.out.println("  ### " + orig.getFloat(6) + " | " + orig.getString(5));
                        }
                    }
                }

            }

            Cursor einkauf = getAccounts(ctx, "aktiva", "?debit=true&" + timewindow, "Lager");
            einkauf.moveToFirst();
            csv.writeNext(new String[] {"Waren Einkauf (Lager Zugang)", "", "-" + String.format(Locale.ENGLISH, "%.2f", einkauf.getFloat(3))});
            Cursor umsatz = getAccounts(ctx, "aktiva", "?credit=true&" + timewindow, "Lager");
            umsatz.moveToFirst();
            csv.writeNext(new String[] {"Waren Umsatz (Lager Abgang)", String.format(Locale.ENGLISH, "%.2f", -1 * umsatz.getFloat(3)), ""});
            Cursor lager = getAccounts(ctx, "aktiva", "?=" + timewindow, "Lager");
            lager.moveToFirst();
            csv.writeNext(new String[] {"Waren Vorrat (Lager Bestand)", String.format(Locale.ENGLISH, "%.2f", lager.getFloat(3)), ""});
            csv.writeNext(new String[] { } );

            Cursor aufgeladen = getAccounts(ctx, "passiva", "?credit=true&" + timewindow, "Mitglieder");
            aufgeladen.moveToFirst();
            csv.writeNext(new String[] {"Korns Aufladung", String.format(Locale.ENGLISH, "%.2f", -1 * aufgeladen.getFloat(3)), ""});
            Cursor korns = getAccounts(ctx, "passiva", "?debit=true&" + timewindow, "Mitglieder");
            korns.moveToFirst();
            csv.writeNext(new String[] {"Korns Einkäufe", "", String.format(Locale.ENGLISH, "%.2f", -1 * korns.getFloat(3))});
            Cursor credits = getAccounts(ctx, "passiva", "?=" + timewindow, "Mitglieder");
            credits.moveToFirst();
            csv.writeNext(new String[] {"Korns Guthaben", "", String.format(Locale.ENGLISH, "%.2f", credits.getFloat(3))});

            csv.writeNext(new String[] { } );
            csv.writeNext(new String[] { } );
            csv.writeNext(new String[] { } );
            Cursor ende = getAccounts(ctx, "aktiva", "?before=" + before, "Bank");
            ende.moveToFirst();
            csv.writeNext(new String[] {"Bank Konto Stand " + df.format(before), String.format(Locale.ENGLISH, "%.2f", ende.getFloat(3)), ""});
            csv.close();
            if (year > 0) {
                zip("" + year, eur, zos);
            } else {
                zip(null, eur, zos);
            }
            eur.delete();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Cursor getMembers(Context ctx, String query, String name) {
        return getAccounts(ctx, "mitglieder", query, name);
    }

    private static Cursor getAccounts(Context ctx, String account, String query, String name) {
        return ctx.getContentResolver().query(
                    Uri.parse("content://org.baobab.foodcoapp/accounts/" + account + "/accounts" + query),
                    null, (name != null? "name IS '" + name + "'" : null), null, null);
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
                null, (title != null? "transactions.status IS 'final' AND title IS '" + title + "'" : null), null, null);
        return txns;
    }

    private static void report(Context ctx, ZipOutputStream zos, String guid, String name, int year) throws IOException {
        Cursor account = ctx.getContentResolver().query(
                Uri.parse("content://org.baobab.foodcoapp/accounts/" +
                        guid + "/transactions"),
                null, getTimeWindowSelection(year), null, null);
        File csv = file(guid + "_" + name + ".csv");
        exportTransactions(ctx, account, csv);
        if (year > 0) {
            zip(year + "/" + year + "_Umsatz", csv, zos);
        } else {
            zip("Umsatz", csv, zos);
        }
        csv.delete();
        account.close();
    }

    static void lager(final Context ctx, ZipOutputStream zos, String account) throws IOException {
        File stock = KnkExport.write(ctx,
                Uri.parse("content://org.baobab.foodcoapp/accounts/" + account + "/products"),
                "(Be)Stand ", new Date().getTime(), file(account + ".knk"));
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
        row[2] = c.getString(4).replace("\n", " | ");
        String sign;
        if (c.getString(9).equals("aktiva")) {
            sign = c.getInt(8) < 0? "-" : "+";
        } else {
            sign = c.getInt(8) > 0? "-" : "+";
        }
        row[3] = sign + String.format(Locale.ENGLISH, "%.2f", c.getFloat(6));
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
