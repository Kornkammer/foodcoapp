package org.baobab.foodcoapp.io;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
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


    public static File create(Context ctx, String id, String guid) {
        File result = file(id + ".zip");
        try {
            ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(result)));
            report(ctx, zos, guid, "kontoauszug", 0);
            zos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    public static File createBackup(Context ctx, String id) {

        ZipOutputStream zos;
        File result = file("Backup_" + id + ".zip");
        try {
            zos = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(result)));
            String date = new SimpleDateFormat("yyyy_MM_dd--HH:mm", Locale.GERMAN).format(new Date());
            String db = PreferenceManager.getDefaultSharedPreferences(ctx)
                    .getString("db", "foodcoapp.db");
            zip(null, new File(Environment.getDataDirectory(),
                    "//data//org.baobab.foodcoapp//databases//" + db),
                    "Knk_" + date + ".BAK", zos);
            zos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    public static File createReports(Context ctx, String id) {

        ZipOutputStream zos;
        File result = file("Berichte_" + id + ".zip");
        try {
            zos = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(result)));
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
            exportSales(ctx, zos);
            exportFees(ctx, zos);
            zos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    private static void exportSales(Context ctx, ZipOutputStream zos) throws IOException {
        Cursor pt = ctx.getContentResolver().query(
                Uri.parse("content://org.baobab.foodcoapp/products/transactions"),
                null, null, null, null);
        File sales = file("kornumsatz.csv");
        exportProductTransactions(ctx, pt, sales);
        zip(null, sales, zos);
        sales.delete();
    }

    private static void exportFees(Context ctx, ZipOutputStream zos) throws IOException {
        Cursor pt = ctx.getContentResolver().query(
                Uri.parse("content://org.baobab.foodcoapp/accounts/mitglieder/fees"),
                null, null, null, null);
        File sales = file("beitraege.csv");
        exportAccountFees(ctx, pt, sales);
        zip(null, sales, zos);
        sales.delete();
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

    static HashMap<String, Cons> balances = new HashMap<>();

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
        Cursor accounts = ctx.getContentResolver().query(
                Uri.parse("content://org.baobab.foodcoapp/accounts/mitglieder/memberships?"
                        + timewindow), null, null, null, "guid, _id");
        System.out.println(accounts.getCount() + " memberships");
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
                           df.format(System.currentTimeMillis()) :  "31. Dez " + year ), "gut", "offen",
                    "Korns", (year > 0? "Stand 1. Jan " : ""), "ein", "aus", "gut",
                    "Einlage zurück", "" });
            csv.writeNext(new String[] { } );
            float sumPrePaidBefore = 0;
            float sumPostPaidThisY = 0;
            float sumSoll = 0;
            float sumPaid = 0;
            Cons paid = null;
            Cons prePaid = null;
            float sumPrePaidThisY = 0;
            float sumStillOpen = 0;
            String guid = "";
            float result = 0;
            boolean latest = true;
            boolean relevant = false;
            while (accounts.moveToNext()) {
                boolean another = false;
                String name = accounts.getString(1);
                if (accounts.getString(2).equals(guid)) {
                    System.out.println("  # another membership for " + guid + " " + name);
                    another = true;
                } else {
                    if (result < 0) {
                        sumStillOpen -= result;
                    } else {
                        sumPrePaidThisY += result;
                    }
                    Cons r = new Cons();
                    r.sum = result;
                    balances.put(year + guid, r);
                    result = 0;
                    latest = true;
                    prePaid = null;
                    relevant = false;
                    guid = accounts.getString(2);
                    System.out.println("# " + guid + " " + name);
                }
                if (!another) {
                    report(ctx, zos, guid, name, year);
                }
                Cons einlage = getContribution(ctx, "?credit=true", name, "einlagen");
                Cons auslage = getContribution(ctx, "?debit=true", name, "einlagen");
                long joined = accounts.getLong(5);
                int fee = accounts.getInt(8);
                long ended = System.currentTimeMillis();
                if (accounts.moveToNext()) {
                    if (accounts.getString(2).equals(guid)) {
                        ended = accounts.getLong(5);
                        latest = false;
                    } else {
                        latest = true;
                    }
                } else {
                    latest = true;
                }
                accounts.moveToPrevious();
                int days = Math.round(((float) (ended - joined)) / 86400000);
                float preDays = 0;
                Cursor standBegin = null;
                Cursor standEnd = null;
                if (year > 0) {
                    try {
                        long after = YEAR.parse("" + year).getTime();
                        long before = YEAR.parse("" + (year + 1)).getTime();
                        if (prePaid == null) {
                            prePaid = balances.get((year - 1) + guid);
                            if (prePaid == null) prePaid = new Cons();
                        }
                        int thisDays = Math.round((((float) (Math.min(ended, before) - Math.max(after, joined))) / 86400000));
                        days = thisDays;
                        standBegin = getMembers(ctx, "?before=" + after, name);
                        standBegin.moveToFirst();
                        standEnd = getMembers(ctx, "?before=" + before, name);
                        standEnd.moveToFirst();
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                } else {
                    prePaid = new Cons();
                }
                float soll = days * Math.max(0, fee) * 12f/365;
                sumSoll += soll;
                if (!relevant) {
                    paid = getContribution(ctx, "?" + timewindow, name, "beiträge");
                    result = prePaid.sum + paid.sum - soll;
                    sumPaid += paid.sum;
                    if (prePaid.sum > 0) {
                        sumPrePaidBefore += prePaid.sum;
                    } else {
                        // actually paid this year???
                        sumPostPaidThisY += Math.min( - prePaid.sum, paid.sum);
                    }
                    if (days < 0 || (fee == -1 && prePaid.sum == 0)) {
                        System.out.println("no relevance for " + year);
                        continue;
                    }
                    relevant = true;
                    Cursor debit = getMembers(ctx, "?debit=true&" + timewindow, name);
                    debit.moveToFirst();
                    Cursor credit = getMembers(ctx, "?credit=true&" + timewindow, name);
                    credit.moveToFirst();

                    csv.writeNext(new String[]{guid, name, df.format(joined),
                            "" + einlage.sum, einlage.dates, (fee > 0? "" + fee : ""),
                            (!accounts.isNull(7) && accounts.getString(7).equals("deleted")? "nicht mehr" : "€/Monat"),
                            (year > 0 && prePaid.sum != 0 ? "" + String.format(Locale.ENGLISH, "%.2f", prePaid.sum) : ""),
                            days + " Tage", "" + String.format(Locale.ENGLISH, "%.2f", soll),
                            "" + String.format(Locale.ENGLISH, "%.2f", paid.sum), (latest? computeBalance(result, fee) : ""),
                            (latest && result > 0? "" + result : ""), (latest && result <= 0? "" + (-result) : ""),
                            (year > 0 && standBegin.getCount() > 0 ?
                                    String.format(Locale.ENGLISH, "%.2f", (-1 * standBegin.getFloat(3))) : ""),
                            (credit.getCount() > 0? String.format(Locale.ENGLISH, "%.2f", (-1 * credit.getFloat(3))) : ""),
                            (debit.getCount() >0? String.format(Locale.ENGLISH, "%.2f", (-1 * debit.getFloat(3))) : ""),
                            (year > 0 && standBegin.getCount() > 0 ?
                                    String.format(Locale.ENGLISH, "%.2f", (-1 * standEnd.getFloat(3))) :
                                    String.format(Locale.ENGLISH, "%.2f", (-1 * accounts.getFloat(3)))),
                            (auslage.sum != 0 ? "" + auslage.sum : ""), auslage.dates});
                } else {
                    result -= soll;
                    if (!accounts.isNull(7) && accounts.getString(7).equals("deleted")) {
                        csv.writeNext(new String[] { guid, name, df.format(joined), "",
                                "nicht mehr dabei", "", "", "", "", "", "",
                                (latest? computeBalance(result, fee) : ""),
                                (latest && result > 0? "" + result : ""),
                                (latest && result <= 0? "" + (-result) : ""),
                                "", "", "", "", "", ""});
                    } else {
                        csv.writeNext(new String[]{guid, name, df.format(joined), "", "",
                                "" + fee, "€/Monat", "", days + " Tage",
                                "" + String.format(Locale.ENGLISH, "%.2f", soll),
                                "", (latest? computeBalance(result, fee) : ""),
                                (latest && result > 0? "" + result : ""),
                                (latest && result <= 0? "" + (-result) : ""),
                                "", "", "", "", "", ""});
                    }
                }
            }
            if (result < 0) {
                sumStillOpen -= result;
            } else {
                sumPrePaidThisY += result;
            }
            Cons r = new Cons();
            r.sum = result;
            balances.put(year + guid, r);

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
            if (anfang.getCount() > 0) {
                csv.writeNext(new String[] {"Bank Konto Stand 1. Jan " + y, String.format(Locale.ENGLISH, "%.2f", anfang.getFloat(3)), ""});
            } else {
                csv.writeNext(new String[] {"Bank Konto Stand 1. Jan " + y, String.format(Locale.ENGLISH, "%.2f", 0f), ""});
            }
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

            //csv.writeNext(new String[] { } );

            Cursor kosten = getAccounts(ctx, "aktiva", "?" + timewindow, "Kosten");
            kosten.moveToFirst();
            csv.writeNext(new String[] {"Entstandene Kosten " + y, "", "-" + String.format(Locale.ENGLISH, "%.2f", kosten.getFloat(3))});

            Cursor spenden = getAccounts(ctx, "passiva", "?" + timewindow, "Spenden");
            spenden.moveToFirst();
            csv.writeNext(new String[] {"Empfangene Spenden " + y, String.format(Locale.ENGLISH, "%.2f", - spenden.getFloat(3)), ""});
            csv.writeNext(new String[] { } );

            Cursor aufgeladen = getAccounts(ctx, "passiva", "?credit=true&" + timewindow, "Mitglieder");
            aufgeladen.moveToFirst();
            csv.writeNext(new String[] {"Korns Aufladung", String.format(Locale.ENGLISH, "%.2f", -1 * aufgeladen.getFloat(3)), ""});
            Cursor einkauf = getAccounts(ctx, "aktiva", "?debit=true&" + timewindow, "Lager");
            einkauf.moveToFirst();
            csv.writeNext(new String[] {"Waren Einkauf (Lager Zugang)", "", "-" + String.format(Locale.ENGLISH, "%.2f", einkauf.getFloat(3))});

            Cursor umsatz = getAccounts(ctx, "aktiva", "?credit=true&" + timewindow, "Lager");
            umsatz.moveToFirst();
            csv.writeNext(new String[] {"Waren Umsatz (Lager Abgang)", String.format(Locale.ENGLISH, "%.2f", -1 * umsatz.getFloat(3)), ""});
            Cursor korns = getAccounts(ctx, "passiva", "?debit=true&" + timewindow, "Mitglieder");
            korns.moveToFirst();
            csv.writeNext(new String[] {"Korns Umsatz", "", String.format(Locale.ENGLISH, "%.2f", -1 * korns.getFloat(3))});

            /*
            Cursor lager = getAccounts(ctx, "aktiva", "?=" + timewindow, "Lager");
            lager.moveToFirst();
            csv.writeNext(new String[] {"Waren Vorrat (Lager Bestand)", String.format(Locale.ENGLISH, "%.2f", lager.getFloat(3)), ""});
            Cursor credits = getAccounts(ctx, "passiva", "?=" + timewindow, "Mitglieder");
            credits.moveToFirst();
            csv.writeNext(new String[] {"Korns Guthaben", "", String.format(Locale.ENGLISH, "%.2f", credits.getFloat(3))});
*/

            csv.writeNext(new String[] { } );
            csv.writeNext(new String[] { } );
            if (year > 0) {
                csv.writeNext(new String[] {"nachträglich für " + (year - 1), String.format(Locale.ENGLISH, "%.2f", sumPostPaidThisY), ""});
                csv.writeNext(new String[] {"Vorschuss vom Vorjahr " + (year - 1), "", "-" + String.format(Locale.ENGLISH, "%.2f", sumPrePaidBefore)});
            }
            csv.writeNext(new String[] {"vorausgezahlte Beiträge", String.format(Locale.ENGLISH, "%.2f", sumPrePaidThisY), ""});
            csv.writeNext(new String[] {"ausstehende Beiträge " + y, "", "-" + String.format(Locale.ENGLISH, "%.2f", sumStillOpen)});
            //csv.writeNext(new String[] {"eingegangen " + (year), String.format(Locale.ENGLISH, "%.2f", sumPaid), ""});

            Cursor verb = getAccounts(ctx, "passiva", "?" + timewindow, "Verbindlichkeiten");
            verb.moveToFirst();
            csv.writeNext(new String[] {"offene Verbindlichkeiten " + y, String.format(Locale.ENGLISH, "%.2f", - verb.getFloat(3)), ""});
            Cursor ford = getAccounts(ctx, "aktiva", "?" + timewindow, "Forderungen");
            ford.moveToFirst();
            csv.writeNext(new String[] {"offene Forderungen " + y, "", String.format(Locale.ENGLISH, "%.2f", - ford.getFloat(3))});
            csv.writeNext(new String[] { } );

            Cursor kasse = getAccounts(ctx, "aktiva", "?" + timewindow, "Kasse");
            kasse.moveToFirst();
            csv.writeNext(new String[] {"Barkasse " + y, "", "-" + String.format(Locale.ENGLISH, "%.2f", kasse.getFloat(3))});


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

    @NonNull
    public static String computeBalance(float result, int fee) {
        String balance;
        int resultDays = Math.round(result / (fee * 12f/365));
        if (result < 0) {
            balance = " = " + String.format(Locale.ENGLISH, "%.2f", -1 * result) +
                    "€" + (fee != 0? " (" + (-1 * resultDays) + " Tage)" : "") + " offen";
        } else if (result > 0) {
            balance = " = " + String.format(Locale.ENGLISH, "%.2f", result) +
                    "€" + (fee != 0? " (" + resultDays + " Tage)" : "") + " gut";
        } else {
            balance = " Beiträge ausgeglichen";
        }
        return balance;
    }

    private static Cursor getMembers(Context ctx, String query, String name) {
        return getAccounts(ctx, "mitglieder", query, name);
    }

    private static Cursor getAccounts(Context ctx, String account, String query, String name) {
        return ctx.getContentResolver().query(
                    Uri.parse("content://org.baobab.foodcoapp/accounts/" + account + "/accounts" + query),
                    null, (name != null? "name IS '" + name + "'" : null), null, null);
    }

    public static class Cons {
        public String dates = "";
        public float sum;
    }
    public static Cons getContribution(Context ctx, String query, String title, String account) {
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
        knk(ctx, Uri.parse("content://org.baobab.foodcoapp/accounts/" + account + "/products"),
                "(Be)Stand ", file(account + ".knk"), zos);
    }

    static void knk(final Context ctx, Uri uri, String title, File f, ZipOutputStream zos) throws IOException {
        File stock = KnkExport.write(ctx, uri, title, new Date().getTime(), f);
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

    private static void exportProductTransactions(final Context ctx, final Cursor c, final File file) {
        CSVWriter out;
        try {
            out = new CSVWriter(new FileWriter(file), ';', CSVWriter.NO_QUOTE_CHARACTER);
            while (c.moveToNext()) {
                String[] row = new String[6];
                row[0] = c.getString(1);
                row[1] = String.format(Locale.ENGLISH, "%.3f", - c.getFloat(2));
                row[2] = c.getString(3);
                row[3] = String.format(Locale.ENGLISH, "%.2f", c.getFloat(4));
                row[4] = c.getString(5);
                row[5] = String.format(Locale.ENGLISH, "%.2f", - c.getFloat(6));
                out.writeNext(row);
            }
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void exportAccountFees(final Context ctx, final Cursor c, final File file) {
        CSVWriter out;
        try {
            out = new CSVWriter(new FileWriter(file), ';', CSVWriter.NO_QUOTE_CHARACTER);
            out.writeNext(new String[] { "Nr", "Name", "Datum", "Beitrag", "Mitglieder"});
            HashMap<String, Integer> fees = new HashMap<>();
            while (c.moveToNext()) {
                String[] row = new String[5];
                row[0] = c.getString(5);
                row[1] = c.getString(6);
                row[2] = df.format(c.getLong(2));
                if (fees.get(c.getString(5)) == null) {
                    row[3] = "" + c.getInt(3);
                    row[4] = "1";
                } else {
                    if (c.isNull(8) || !c.getString(8).equals("deleted")) {
                        int diff = c.getInt(3) - fees.get(c.getString(5));
                        if (diff == 0) {
                            continue;
                        }
                        row[3] = "" + diff;
                        row[4] = "0";
                    } else {
                        row[3] = "" + ( - fees.get(c.getString(5)));
                        row[4] = "-1";
                    }
                }
                fees.put(c.getString(5), c.getInt(3));
                out.writeNext(row);
            }
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
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
