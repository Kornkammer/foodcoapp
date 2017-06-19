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
import java.util.ArrayList;
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
            //reports(ctx, zos, 0);
            exportKornumsatz(ctx, zos);
            exportFees(ctx, zos);
            zos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    private static class Sum {
        float target_in;
        float target_out;
        float intern_in;
        float intern_out;
        String[] row(String title, float in, float out) {
            String[] r = new String[]{ title, "", "", "", "",
                    (in != 0 ? String.format(Locale.ENGLISH, "%.2f", in) : ""),
                    (out != 0 ? String.format(Locale.ENGLISH, "%.2f", out) : "")};
            if (in > 0) {
                if (target_in != 0) {
                    r[1] = String.format(Locale.ENGLISH, "%.2f", target_in);
                }
                if (target_out != 0) {
                    r[2] = String.format(Locale.ENGLISH, "%.2f", target_out);
                }
                if (intern_in + intern_out > 0) {
                    r[3] = String.format(Locale.ENGLISH, "%.2f", intern_in + intern_out);
                } else {
                    r[4] = String.format(Locale.ENGLISH, "%.2f", intern_in + intern_out);
                }
            } else {
                if (target_in + target_out > 0) {
                    r[1] = String.format(Locale.ENGLISH, "%.2f", target_in + target_out);
                } else {
                    r[2] = String.format(Locale.ENGLISH, "%.2f", target_in + target_out);
                }
                if (intern_in + intern_out < 0) {
                    r[3] = String.format(Locale.ENGLISH, "%.2f", intern_in + intern_out);
                } else {
                    r[4] = String.format(Locale.ENGLISH, "%.2f", intern_in + intern_out);
                }
            }
            return r;
        }
    }
    private static Sum exportTransactionTrace(Context ctx, String title, String uri, ZipOutputStream zos, int year, int sign) throws IOException {
        return exportTransactionTrace(ctx, title, uri, zos, year, sign, "bank", false);
    }

    private static Sum exportTransactionTrace(Context ctx, String title, String uri, ZipOutputStream zos, int year, int sign, String target, boolean filter) throws IOException {
        System.out.println();
        System.out.println("TRACE " + title + " for " + target);
        System.out.println();
        File tmp = file((year != 0? year + "_" : "") + title + ".csv");
        CSVWriter csv = new CSVWriter(new FileWriter(tmp), ';', CSVWriter.NO_QUOTE_CHARACTER);
        csv.writeNext(new String[] {"TxnId", "Konto", "Datum", "Titel", "Menge", "Preis", "Summe", "Kommentar"});
        Cursor in = ctx.getContentResolver().query(Uri.parse(
                "content://org.baobab.foodcoapp/" + uri + getTimeWindowQuery(year)), null, null, null, null);
        String account = null;
        if (uri.split("/").length > 1) {
            account = uri.split("/")[1];
        }
        Sum sum = new Sum();
        ArrayList<String[]> internal = new ArrayList<>();
        while (in.moveToNext()) {
            System.out.println("txn " + in.getInt(0));
            ArrayList<Txn> tx = trace(ctx, in, (account != null? account : "mitglieder"), target);
            Txn l = tx.get(0);
            tx.remove(0);
            for (Prod p : l.products) {
                String[] row = new String[9];
                row[0] = "" + in.getLong(0);
                row[1] = in.getString(3);
                row[2] = df.format(in.getLong(2));
                row[3] = p.title;
                row[4] = String.format(Locale.ENGLISH, "%.3f", sign * p.quantity);
                row[6] = String.format(Locale.ENGLISH, "%.2f", sign * p.quantity * p.price);
                row[5] = String.format(Locale.ENGLISH, "%.2f", p.price);
                row[7] = in.getString(4).replace("\n", " | ") + " | " + l.comment;
                if (tx.size() > 0) {
                    csv.writeNext(row);
                    if (p.quantity > 0) {
                        sum.target_out += - p.quantity * p.price;
                    } else {
                        sum.target_in += - p.quantity * p.price;
                    }
                } else {
                    if (filter && (sign * p.quantity < 0 && tx.size() == 0)) {
                        continue;
                    }
                    if (p.quantity > 0) {
                        sum.intern_out += - p.quantity * p.price;
                    } else {
                        sum.intern_in += - p.quantity * p.price;
                    }
                    internal.add(row);
                }
            }
            for (Txn t : tx) {
                csv.writeNext(new String[] {"", "", "", "", "", "", "", df.format(t.time) + " " +
                        t.title + " " + String.format(Locale.ENGLISH, "%.2f", t.value) +
                        (t.id == in.getLong(0)? " " : " " + t.comment), df.format(t.time)});
                System.out.println("* found " + t.time + " : " + t.value + " " + t.comment);
            }
        }
        csv.writeNext(new String[] {});
        csv.writeNext(new String[] {});
        csv.writeNext(new String[] {});
        csv.writeNext(new String[] { "", "", "", "INTERNE BUCHUNGEN"});
        csv.writeNext(new String[] {});
        for (String[] r : internal) {
            csv.writeNext(r);
        }
        in.close();
        csv.close();
        zip((year != 0? year + "/" + year + "_eur_transactions" : "alltime/alltime_eur_transactions"), tmp, zos);
        tmp.delete();
        return sum;
    }

    static class Txn {
        int id;
        long time;
        float value;
        String title;
        String comment;
        ArrayList<Prod> products;
    }

    static class Prod {
        String title;
        float quantity;
        float price;
    }

    private static ArrayList<Txn> trace(Context ctx, Cursor t, String account, String target) {
        ArrayList<Txn> txns = new ArrayList<Txn>();
        Txn tx = new Txn();
        tx.comment = "";
        tx.products = new ArrayList<Prod>();
        txns.add(tx);
        return trace(ctx, t, null, 0, txns, account, target);
    }
    private static ArrayList<Txn> trace(Context ctx, Cursor t, String title, float price, ArrayList<Txn> txns, String account, String target) {
        Cursor prods = ctx.getContentResolver().query(Uri.parse(
                "content://org.baobab.foodcoapp/transactions/" + t.getLong(0) + "/products"), null, null, null, null);
        while (prods.moveToNext()) {
            //System.out.println("  " + title + "  " + prods.getString(2) +
            //        "  -> " + prods.getFloat(4) + " x " + prods.getFloat(5) + " :: " + prods.getString(7));
            if (prods.getString(2).equals(target)) {
                Txn tx = new Txn();
                tx.id = t.getInt(0);
                tx.time = t.getLong(2);
                tx.title = prods.getString(7);
                tx.value = prods.getFloat(4) * prods.getFloat(5); //t.getFloat(6);
                tx.comment = t.getString(4).replace("\n", " | ");
                txns.add(tx);
            } else {
                if (title == null && (account.equals(prods.getString(2)) ||
                                account.equals(prods.getString(10)))) {
                    Prod p = new Prod();
                    p.title = prods.getString(7);
                    p.quantity = prods.getFloat(4);
                    p.price = prods.getFloat(5);
                    txns.get(0).products.add(p);
                } else if (prods.getString(2).equals("forderungen") ||
                        prods.getString(2).equals("verbindlichkeiten")) {
                    if (price != 0 && price == prods.getFloat(4) * prods.getFloat(5)) continue;
                    Cursor orig = ctx.getContentResolver().query(Uri.parse(
                            "content://org.baobab.foodcoapp/accounts/" + prods.getString(2) +
                                    "/transactions"), null, "transactions.status IS 'final'" +
                            "AND title IS '" + prods.getString(7) +
                            "' AND ROUND(price, 2) = ROUND(" + prods.getFloat(5) + ", 2)", null, null);
                    while (orig.moveToNext()) {
                        if (prods.getFloat(4) * orig.getFloat(6) < 0) {
                            //System.out.println("  " + title + "    ### FOUND  " + orig.getFloat(6) + " | " + orig.getString(5));
                            trace(ctx, orig, (title != null ? title + " - " : "") + prods.getString(7), orig.getFloat(6), txns, account, target);
                        }
                    }
                    orig.close();
                }
            }
        }
        if (title != null) {
            txns.get(0).comment += title + " ==> ";
        }
        prods.close();
        return txns;
    }

    private static void exportFees(Context ctx, ZipOutputStream zos) throws IOException {
        Cursor pt = ctx.getContentResolver().query(
                Uri.parse("content://org.baobab.foodcoapp/accounts/mitglieder/fees"),
                null, null, null, null);
        File sales = file("mitgliedschaften.csv");
        exportAccountFees(ctx, pt, sales);
        zip(null, sales, zos);
        sales.delete();
    }

    static void transactions(final Context ctx, ZipOutputStream zos, int year) throws IOException, ParseException {
        File tmp = null;
        String selection = getTimeWindowSelection(year);
        Cursor all = ctx.getContentResolver().query(
                Uri.parse("content://org.baobab.foodcoapp/transactions"),
                null, selection, null, "transactions.start");
        if (year == 0) {
            if (all.getCount() == 0) return;
            all.moveToFirst();
            int first = Integer.parseInt(YEAR.format(all.getLong(2)));
            all.moveToLast();
            int last = Integer.parseInt(YEAR.format(all.getLong(2)));
            for (int y = first; y <= last; y++) {
                transactions(ctx, zos, y);
                //reports(ctx, zos, y);
            }
            all.moveToPosition(-1);
            tmp = file("alltime_transactions.csv");
        } else {
            tmp = file(year + "_transactions.csv");
        }
        exportTransactions(ctx, all, tmp);
        zip((year != 0? "" + year : null), tmp, zos);
        all.close();
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

    public static String getTimeWindowQuery(int year) {
        if (year > 0) {
            try {
                return "after=" + YEAR.parse("" + year).getTime() +
                        "&before=" + YEAR.parse("" + (year + 1)).getTime();
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        return "";
    }

    static HashMap<String, Cons> balances = new HashMap<>();
    static float feesOpenFromLastYear = 0;

    static void reports(final Context ctx, ZipOutputStream zos, int year) throws IOException {
        String timewindow = getTimeWindowQuery(year);
        Cursor accounts = ctx.getContentResolver().query(
                Uri.parse("content://org.baobab.foodcoapp/accounts/mitglieder/memberships?"
                        + timewindow), null, null, null, "guid, _id");
        System.out.println(accounts.getCount() + " memberships");
        File tmp;
        if (year > 0) {
            tmp = file(year + "_mitglieder.csv");
        } else {
            feesOpenFromLastYear = 0;
            tmp = file("alltime_mitglieder.csv");
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
                    sumSoll += soll;
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
                            (latest && result > 0? "" + result : ""), (latest && result <= 0? "" + (-result) : ""), "",
                            (year > 0 && standBegin.getCount() > 0 ?
                                    String.format(Locale.ENGLISH, "%.2f", (-1 * standBegin.getFloat(3))) : ""),
                            (credit.getCount() > 0? String.format(Locale.ENGLISH, "%.2f", (-1 * credit.getFloat(3))) : ""),
                            (debit.getCount() >0? String.format(Locale.ENGLISH, "%.2f", (-1 * debit.getFloat(3))) : ""),
                            (year > 0 && standBegin.getCount() > 0 ?
                                    String.format(Locale.ENGLISH, "%.2f", (-1 * standEnd.getFloat(3))) :
                                    String.format(Locale.ENGLISH, "%.2f", (-1 * accounts.getFloat(3)))),
                            (auslage.sum != 0 ? "" + auslage.sum : ""), auslage.dates});
                    if (standBegin != null) standBegin.close();
                    if (standEnd != null) standEnd.close();
                    credit.close();
                    debit.close();
                } else {
                    sumSoll += soll;
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

            exportInventur(ctx, zos, year);
            exportBalance(ctx, zos, "lager", year);
            exportBalance(ctx, zos, "kosten", year);
            exportBalance(ctx, zos, "inventar", year);
            report(ctx, zos, "bank", "bank_transactions.csv", (year != 0? year + "" : "alltime"), year);
            Sum einlagen_sum = exportTransactionTrace(ctx, "einlagen", "accounts/einlagen/transactions?", zos, year, -1);
            Sum inventar_sum = exportTransactionTrace(ctx, "inventar", "accounts/inventar/transactions?", zos, year, 1);
            Sum kosten_sum = exportTransactionTrace(ctx, "kosten", "accounts/kosten/transactions?", zos, year, 1);
            Sum beitr_sum = exportTransactionTrace(ctx, "fees", "accounts/beiträge/transactions?", zos, year, -1);
            Sum spenden_sum = exportTransactionTrace(ctx, "spenden", "accounts/spenden/transactions?", zos, year, -1);
            Sum lager_zugang = exportTransactionTrace(ctx, "waren_lager_zugang", "accounts/lager/transactions?debit=true&" , zos, year, 1);
            Sum lager_abgang = exportTransactionTrace(ctx, "waren_lager_abgang", "accounts/lager/transactions?credit=true&" , zos, year, -1, "kosten", true);
            Sum bank_abgang = exportTransactionTrace(ctx, "bank_abgang", "accounts/bank/transactions?credit=true&" , zos, year, -1);
            Sum bank_zugang = exportTransactionTrace(ctx, "bank_zugang", "accounts/bank/transactions?debit=true&" , zos, year, 1);
            Sum kasse_abgang = exportTransactionTrace(ctx, "kasse_abgang", "accounts/kasse/transactions?credit=true&" , zos, year, -1);
            Sum kasse_zugang_korns = exportTransactionTrace(ctx, "kasse_zugang_korns", "accounts/kasse/transactions?debit=true&title=Korns" , zos, year, 1);
            Sum korns_aufladen = exportTransactionTrace(ctx, "umsatz_korns_aufladen", "transactions?title=Korns&", zos, year, -1, "bank", true);
            Sum korns_ausgeben = exportTransactionTrace(ctx, "umsatz_korns_ausgeben", "transactions?title=Korns&", zos, year, 1, "lager", true);
            Sum kasse_sum = exportTransactionTrace(ctx, "kasse", "accounts/kasse/transactions?", zos, year, -1);

            csv.close();
            File eur;
            if (year > 0) {
                zip("" + year, tmp, zos);
                eur = file(year + "_eur.csv");
            } else {
                zip("alltime", tmp, zos);
                eur = file("alltime_eur.csv");
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
            csv.writeNext(new String[] {(year > 0? "Jahr " + y : "alle Jahre Stand " + df.format(before)),
                    ".      BANK ein", "aus", ".        INTERN ein", "aus", ".         GESAMT ein", "aus"});
            csv.writeNext(new String[] { } );
            Cursor anfang = getAccounts(ctx, "aktiva", "?before=" + after , "Bank");
            anfang.moveToFirst();
            if (anfang.getCount() > 0) {
                csv.writeNext(new String[] {"Bank Konto Stand 1. Jan " + y, String.format(Locale.ENGLISH, "%.2f", anfang.getFloat(3)), ""});
            } else {
                csv.writeNext(new String[] {"Bank Konto Stand 1. Jan " + y, String.format(Locale.ENGLISH, "%.2f", 0f), ""});
            }
            anfang.close();
            csv.writeNext(new String[] { } );
            Cursor einlagen = getAccounts(ctx, "passiva", "?" + timewindow, "Einlagen");
            einlagen.moveToFirst();
            csv.writeNext(einlagen_sum.row("Mitglieder Einlagen " + y, - einlagen.getFloat(3), 0));
            einlagen.close();
            Cursor anschaffungen = getAccounts(ctx, "aktiva", "?" + timewindow, "Inventar");
            anschaffungen.moveToFirst();
            csv.writeNext(inventar_sum.row("Anschaffung Inventar " + y, 0, - anschaffungen.getFloat(3)));
            anschaffungen.close();
            Cursor abschreibungen = getAccounts(ctx, "aktiva", "?credit=true&" + timewindow, "Inventar");
            abschreibungen.moveToFirst();
            //csv.writeNext(new String[] {"Abschreibung Inventar " + y, String.format(Locale.ENGLISH, "%.2f", -1 * abschreibungen.getFloat(3)), ""});
            abschreibungen.close();
            csv.writeNext(new String[] { } );

            csv.writeNext(beitr_sum.row("Mitglieder Beiträge " + y, sumSoll, 0));

            //csv.writeNext(new String[] { } );

            Cursor kosten = getAccounts(ctx, "aktiva", "?" + timewindow, "Kosten");
            kosten.moveToFirst();
            csv.writeNext(kosten_sum.row("Entstandene Kosten " + y, 0, - kosten.getFloat(3)));
            kosten.close();
            csv.writeNext(new String[] { } );

            Cursor spenden = getAccounts(ctx, "passiva", "?" + timewindow, "Spenden");
            spenden.moveToFirst();
            csv.writeNext(spenden_sum.row("Empfangene Spenden " + y, - spenden.getFloat(3), 0));
            spenden.close();
            csv.writeNext(new String[] { } );

            Cursor kasse = getAccounts(ctx, "aktiva", "?" + timewindow, "Kasse");
            kasse.moveToFirst();
            csv.writeNext(kasse_sum.row("Barkasse " + y, 0, - kasse.getFloat(3)));
            kasse.close();

            Cursor aufgeladen = getAccounts(ctx, "passiva", "?credit=true&" + timewindow, "Mitglieder");
            aufgeladen.moveToFirst();
            String[] row = korns_aufladen.row("Korns Guthaben Aufladung", - aufgeladen.getFloat(3), 0);
            //row[3] = String.format(Locale.ENGLISH, "%.2f", korns_aufladen.intern_in);
            //row[4] = String.format(Locale.ENGLISH, "%.2f", korns_aufladen.intern_out);
            csv.writeNext(row);
            aufgeladen.close();

            Cursor einkauf = getAccounts(ctx, "aktiva", "?debit=true&" + timewindow, "Lager");
            einkauf.moveToFirst();
            Cursor korns = getAccounts(ctx, "passiva", "?debit=true&" + timewindow, "Mitglieder");
            korns.moveToFirst();
            row = lager_zugang.row("Waren Einkauf (Lager Zugang)", 0, - einkauf.getFloat(3));
            //row[3] = String.format(Locale.ENGLISH, "%.2f", waren_einkauf_sum.intern_out);
            //row[4] = String.format(Locale.ENGLISH, "%.2f", waren_einkauf_sum.intern_in);
            csv.writeNext(row);
            einkauf.close();

            Cursor umsatz = getAccounts(ctx, "aktiva", "?credit=true&" + timewindow, "Lager");
            umsatz.moveToFirst();
            csv.writeNext(lager_abgang.row("Waren Verkauf (Lager Abgang)", umsatz.getFloat(3), 0));
            csv.writeNext(new String[] {"Waren Verkauf (Lager Abgang)", "", "",
                    String.format(Locale.ENGLISH, "%.2f", lager_abgang.target_in),
                    String.format(Locale.ENGLISH, "%.2f", lager_abgang.target_out), "",
                    String.format(Locale.ENGLISH, "%.2f", -1 * umsatz.getFloat(3)), "" });
            umsatz.close();
            csv.writeNext( new String[]{ "Korns Guthaben Umsatz", "", "",
                    String.format(Locale.ENGLISH, "%.2f", korns_ausgeben.target_out),
                    String.format(Locale.ENGLISH, "%.2f", korns_ausgeben.intern_out + korns_ausgeben.intern_in),
                    "", String.format(Locale.ENGLISH, "%.2f", - korns.getFloat(3)) });
            korns.close();

            csv.writeNext(new String[] { } );
            csv.writeNext(new String[] {"Bank zu",
                    String.format(Locale.ENGLISH, "%.2f", bank_zugang.target_in),
                    String.format(Locale.ENGLISH, "%.2f", bank_zugang.target_out),
                    String.format(Locale.ENGLISH, "%.2f", bank_zugang.intern_in),
                    String.format(Locale.ENGLISH, "%.2f", bank_zugang.intern_out),
                    "", "" });
            csv.writeNext(new String[] {"Bank ab",
                    String.format(Locale.ENGLISH, "%.2f", bank_abgang.target_in),
                    String.format(Locale.ENGLISH, "%.2f", bank_abgang.target_out),
                    String.format(Locale.ENGLISH, "%.2f", bank_abgang.intern_in),
                    String.format(Locale.ENGLISH, "%.2f", bank_abgang.intern_out),
                    "", "" });
            csv.writeNext(new String[] {"Kasse ab",
                    String.format(Locale.ENGLISH, "%.2f", kasse_abgang.target_in),
                    String.format(Locale.ENGLISH, "%.2f", kasse_abgang.target_out),
                    String.format(Locale.ENGLISH, "%.2f", kasse_abgang.intern_in),
                    String.format(Locale.ENGLISH, "%.2f", kasse_abgang.intern_out),
                    "", "" });
            csv.writeNext(new String[] {"Kasse zu",
                    String.format(Locale.ENGLISH, "%.2f", kasse_zugang_korns.target_in),
                    String.format(Locale.ENGLISH, "%.2f", kasse_zugang_korns.target_out),
                    String.format(Locale.ENGLISH, "%.2f", kasse_zugang_korns.intern_in),
                    String.format(Locale.ENGLISH, "%.2f", kasse_zugang_korns.intern_out),
                    "", "" });

            Cursor verb = getAccounts(ctx, "passiva", "?" + timewindow, "Verbindlichkeiten");
            Cursor ford = getAccounts(ctx, "aktiva", "?" + timewindow, "Forderungen");
            ford.moveToFirst();
            verb.moveToFirst();

            csv.writeNext(new String[] { } );
            csv.writeNext(new String[] { } );
            if (year > 0) {
                csv.writeNext(new String[] {"nachträglich für " + (year - 1), "", "", "", "",
                        String.format(Locale.ENGLISH, "%.2f", sumPostPaidThisY), ""});
                csv.writeNext(new String[] {"Vorschuss vom Vorjahr " + (year - 1), "", "", "", "",
                        "", "-" + String.format(Locale.ENGLISH, "%.2f", sumPrePaidBefore)});
            }
            csv.writeNext(new String[] {"vorausgezahlte Beiträge", "", "", "", "",
                    String.format(Locale.ENGLISH, "%.2f", sumPrePaidThisY), ""});
            csv.writeNext(new String[] {"ausstehende Beiträge " + y, "", "", "", "",
                    "", "-" + String.format(Locale.ENGLISH, "%.2f",
                    sumStillOpen - feesOpenFromLastYear + sumPostPaidThisY)});
            //csv.writeNext(new String[] {"eingegangen " + (year), String.format(Locale.ENGLISH, "%.2f", sumPaid), ""});
            feesOpenFromLastYear = sumStillOpen;

            csv.writeNext(new String[] {"offene Verbindlichkeiten " + y,
                    String.format(Locale.ENGLISH, "%.2f", - verb.getFloat(3)), "", "", "",
                    String.format(Locale.ENGLISH, "%.2f", - verb.getFloat(3))});
            verb.close();
            csv.writeNext(new String[] {"offene Forderungen " + y,
                    "", String.format(Locale.ENGLISH, "%.2f", - ford.getFloat(3)), "", "",
                    "", String.format(Locale.ENGLISH, "%.2f", - ford.getFloat(3))});
            ford.close();
            csv.writeNext(new String[] { } );


            csv.writeNext(new String[] { } );
            csv.writeNext(new String[] { } );
            csv.writeNext(new String[] { } );
            Cursor ende = getAccounts(ctx, "aktiva", "?before=" + before, "Bank");
            ende.moveToFirst();
            csv.writeNext(new String[] {"Bank Konto Stand " + df.format(before), String.format(Locale.ENGLISH, "%.2f", ende.getFloat(3)), ""});
            ende.close();
            csv.close();
            if (year > 0) {
                zip("" + year, eur, zos);
            } else {
                zip("alltime", eur, zos);
            }
            eur.delete();
        } catch (Exception e) {
            e.printStackTrace();
        }
        accounts.close();
    }

    private static void exportInventur(Context ctx, ZipOutputStream zos, int year) throws IOException, ParseException {
        long end = year == 0 || System.currentTimeMillis() < YEAR.parse("" + (year + 1)).getTime()?
                System.currentTimeMillis() : YEAR.parse("" + (year + 1)).getTime();
        String before = "?before=" + end;
        File tmp = file(new SimpleDateFormat("yyyy_MM_dd_", Locale.GERMAN).format(end) + "stand.csv");
        CSVWriter csv = new CSVWriter(new FileWriter(tmp), ';', CSVWriter.NO_QUOTE_CHARACTER);
        csv.writeNext(new String[] {"AKTIVA", "", "", "", "PASSIVA", "", "" });

        csv.writeNext(new String[] { } );
        csv.writeNext(new String[] {"INVENTAR", "", "", "", "EINLAGEN ", "", ""});
        Cursor einlagen = getAccounts(ctx, "passiva", before, "Einlagen");
        einlagen.moveToFirst();
        Cursor list = ctx.getContentResolver().query(
                Uri.parse("content://org.baobab.foodcoapp/accounts/inventar/products" + before),
                null, null, null, null);
        float sum = 0;
        while (list.moveToNext()) {
            csv.writeNext(new String[] {"   " + ((int) list.getFloat(4)) + " x " + list.getString(7) +
                    " a " + String.format(Locale.ENGLISH, "%.2f", list.getFloat(5)),
                    String.format(Locale.ENGLISH, "%.2f", list.getFloat(4) * list.getFloat(5))});
            sum += list.getFloat(4) * list.getFloat(5);
        }
        list.close();
        Cursor inv = getAccounts(ctx, "aktiva", before, "Inventar");
        inv.moveToFirst();
        csv.writeNext(new String[] {"", "", String.format(Locale.ENGLISH, "%.2f", inv.getFloat(3)),
                "", "", "", String.format(Locale.ENGLISH, "%.2f", -1 * einlagen.getFloat(3))});
        einlagen.close();
        inv.close();

        csv.writeNext(new String[] { } );
        csv.writeNext(new String[] {"LAGER BESTAND", "", "", "", "KORNS GUTHABEN ", "", ""});
        Cursor korns = getAccounts(ctx, "passiva", before, "Mitglieder");
        korns.moveToFirst();
        list = ctx.getContentResolver().query(
                Uri.parse("content://org.baobab.foodcoapp/accounts/lager/products?" + before),
                null, null, null, null);
        sum = 0;
        float kg = 0;
        while (list.moveToNext()) {
            //csv.writeNext(new String[] {"   # " + String.format(Locale.ENGLISH, "%.3f", list.getFloat(4)) + " " +
            //        list.getString(6) + " " + list.getString(7) + " a " + String.format(Locale.ENGLISH, "%.2f", list.getFloat(5)),
            //        String.format(Locale.ENGLISH, "%.2f", list.getFloat(4) * list.getFloat(5)), "", ""});
            sum += list.getFloat(4) * list.getFloat(5);
            kg += list.getFloat(4);
        }
        list.close();
        Cursor lager = getAccounts(ctx, "aktiva", before, "Lager");
        lager.moveToFirst();
        csv.writeNext(new String[] {"", "   " + kg + " kg a " +
                String.format(Locale.ENGLISH, "%.2f", sum / kg),
                String.format(Locale.ENGLISH, "%.2f", lager.getFloat(3)),
                "", "", "", String.format(Locale.ENGLISH, "%.2f", -1 * korns.getFloat(3))});
        korns.close();

        csv.writeNext(new String[] { } );
        csv.writeNext(new String[] {"BANK", "", "", "", "SPENDEN", ""});
        Cursor bank = getAccounts(ctx, "aktiva", before, "Bank");
        bank.moveToFirst();
        Cursor spenden = getAccounts(ctx, "passiva", before, "Spenden");
        spenden.moveToFirst();
        csv.writeNext(new String[] {"", "", String.format(Locale.ENGLISH, "%.2f", bank.getFloat(3)),
                "", "", "", String.format(Locale.ENGLISH, "%.2f", -1 * spenden.getFloat(3))});
        bank.close();
        spenden.close();

        csv.writeNext(new String[] { } );
        csv.writeNext(new String[] {"BAR KASSE", "", "", "", "BEITRAGS ÜBERSCHUSS", ""});
        Cursor kasse = getAccounts(ctx, "aktiva", before, "Kasse");
        kasse.moveToFirst();
        Cursor beitr = getAccounts(ctx, "passiva", before, "Beiträge");
        beitr.moveToFirst();
        Cursor kost = getAccounts(ctx, "aktiva", before, "Kosten");
        kost.moveToFirst();
        csv.writeNext(new String[] {"", "", String.format(Locale.ENGLISH, "%.2f", kasse.getFloat(3)),
                "", "", "", String.format(Locale.ENGLISH, "%.2f",
                        -1 * beitr.getFloat(3) - kost.getFloat(3))});
        bank.close();
        spenden.close();

        csv.writeNext(new String[] { } );
        csv.writeNext(new String[] {"FORDERUNGEN", "", "", "", "VERBINDLICHKEITEN", "", ""});
        Cursor f = ctx.getContentResolver().query(
                Uri.parse("content://org.baobab.foodcoapp/accounts/forderungen/products" + before),
                null, null, null, null);
        Cursor v = ctx.getContentResolver().query(
                Uri.parse("content://org.baobab.foodcoapp/accounts/verbindlichkeiten/products" + before),
                null, null, null, null);
        boolean another = true;
        while (f.moveToNext()) {
            boolean ve = v.moveToNext();
            csv.writeNext(new String[] {"   # " + f.getString(7),
                    String.format(Locale.ENGLISH, "%.2f", f.getFloat(4) * f.getFloat(5)), "", "",
                    (ve? "   # " + v.getString(7) : ""), (ve?
                    String.format(Locale.ENGLISH, "%.2f", - v.getFloat(4) * v.getFloat(5)) : "")});

        }
        while (v.moveToNext()) {
            csv.writeNext(new String[] {"", "", "", "", "   # " + v.getString(7),
                    String.format(Locale.ENGLISH, "%.2f", - v.getFloat(4) * v.getFloat(5)) });
        }
        list.close();
        Cursor ford = getAccounts(ctx, "aktiva", before, "Forderungen");
        ford.moveToFirst();
        Cursor verb = getAccounts(ctx, "passiva", before, "Verbindlichkeiten");
        verb.moveToFirst();

        csv.writeNext(new String[] {"", "", String.format(Locale.ENGLISH, "%.2f", ford.getFloat(3)),
                "", "", "", String.format(Locale.ENGLISH, "%.2f", -1 * verb.getFloat(3))});
        ford.close();
        verb.close();

        csv.close();
        zip((year == 0? "alltime" : year + ""), tmp, zos);
        tmp.delete();
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
        txns.close();
        return cons;
    }
    private static Cursor getTxns(Context ctx, String query, String title, String account) {
        Cursor txns = ctx.getContentResolver().query(
                Uri.parse("content://org.baobab.foodcoapp/accounts/" + account + "/transactions" + query),
                null, (title != null? "transactions.status IS 'final' AND title IS '" + title + "'" : null), null, null);
        return txns;
    }

    private static void report(Context ctx, ZipOutputStream zos, String guid, String name, int year) throws IOException {
        report(ctx, zos, guid, guid + "_" + name + ".csv",
                (year != 0? year + "/" + year : "alltime/alltime") + "_Umsatz", year);
    }

    private static void report(Context ctx, ZipOutputStream zos, String guid, String file, String dir, int year) throws IOException {
        Cursor account = ctx.getContentResolver().query(
                Uri.parse("content://org.baobab.foodcoapp/accounts/" +
                        guid + "/transactions"),
                null, getTimeWindowSelection(year), null, null);
        File csv = file(file);
        exportTransactions(ctx, account, csv);
        zip(dir, csv, zos);
        csv.delete();
        account.close();
    }

    static void exportBalance(final Context ctx, ZipOutputStream zos, String account, int year) throws IOException {
        knk(ctx, Uri.parse("content://org.baobab.foodcoapp/accounts/" + account + "/products?" + getTimeWindowQuery(year)),
                "(Be)Stand ", file((year != 0? year + "_" : "alltime_") + account + ".knk"), zos, year);
    }

    static void knk(final Context ctx, Uri uri, String title, File f, ZipOutputStream zos, int year) throws IOException {
        File stock = KnkExport.write(ctx, uri, title, new Date().getTime(), f);
        zip((year != 0? year + "" : "alltime"), stock, zos);
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
        int sign = 1;
        if (c.getString(9).equals("aktiva")) {
            //sign = c.getInt(8) < 0? -1 : 1;
        } else {
            //sign = c.getInt(8) > 0? -1 : 1;
        }
        if (sign * c.getFloat(6) > 0) {
            row[3] = String.format(Locale.ENGLISH, "%.2f", sign * c.getFloat(6));
        } else {
            row[4] = String.format(Locale.ENGLISH, "%.2f", sign * c.getFloat(6));
        }
        return row;
    }

    private static void exportKornumsatz(Context ctx, ZipOutputStream zos) throws IOException {
        File f = file("kornumsatz.csv");
        exportProductTransactions(ctx, f);
        zip(f, zos);
        f.delete();
    }

    private static File exportProductTransactions(final Context ctx, final File file) {
        Cursor c = ctx.getContentResolver().query(Uri.parse(
                "content://org.baobab.foodcoapp/products/transactions?"),
                null, null, null, null);
        CSVWriter out;
        try {
            HashMap<String, Float> stock = new HashMap<>();
            HashMap<String, Float> stock2 = new HashMap<>();
            out = new CSVWriter(new FileWriter(file), ';', CSVWriter.NO_QUOTE_CHARACTER);
            out.writeNext(new String[] { "Tag", "Menge", "Einheit", "Preis", "Produkt", "Summe", "Bestand Einheit", "Bestand Preis" });
            while (c.moveToNext()) {
                String[] row = new String[8];
                row[0] = c.getString(1);
                row[1] = String.format(Locale.ENGLISH, "%.3f", c.getFloat(2));
                row[2] = c.getString(3);
                row[3] = String.format(Locale.ENGLISH, "%.2f", c.getFloat(4));
                row[4] = c.getString(5);
                float sum = c.getFloat(2) * c.getFloat(4);
                row[5] = String.format(Locale.ENGLISH, "%.2f", sum);
                if (!stock.containsKey(c.getString(5))) {
                    stock.put(c.getString(5), c.getFloat(2));
                    stock2.put(c.getString(5), sum);
                } else {
                    stock.put(c.getString(5), stock.get(c.getString(5)) + c.getFloat(2));
                    stock2.put(c.getString(5), stock2.get(c.getString(5)) + sum);
                }
                //if (row[4].equals("Sonnenblumenkerne")) {
                //    System.out.println("ZEITSTEMPEL " + df.format(c.getLong(7)));
                //    Cursor l = ctx.getContentResolver().query(Uri.parse(
                //            "content://org.baobab.foodcoapp/accounts/lager/products/?before=" + (c.getLong(7) + 1000)),
                //            null, null, null, null);
                //    while (l.moveToNext()) {
                //        if (l.getString(7).equals("Sonnenblumenkerne")) {
                //            System.out.println("LAERBESTAND am " + c.getString(1) + " war " + l.getFloat(4));
                //        }
                //    }
                //    System.out.println("SONNENBLUMENKERNE " + c.getFloat(2) + " (" + c.getFloat(4) + ") -> " + stock.get(c.getString(5)));
                //    System.out.println("");
                //}
                row[6] = String.format(Locale.ENGLISH, "%.3f", stock.get(c.getString(5)));
                row[7] = String.format(Locale.ENGLISH, "%.2f", stock2.get(c.getString(5)));
                out.writeNext(row);
            }
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return file;
    }
    private static void exportLagerTransactions(final Context ctx, final Cursor c, final File file) {
        CSVWriter out;
        try {
            HashMap<String, Float> stock = new HashMap<>();
            out = new CSVWriter(new FileWriter(file), ';', CSVWriter.NO_QUOTE_CHARACTER);
            while (c.moveToNext()) {
                String[] row = new String[7];
                row[0] = c.getString(13);
                row[1] = String.format(Locale.ENGLISH, "%.3f", c.getFloat(8));
                row[2] = c.getString(15);
                row[3] = String.format(Locale.ENGLISH, "%.2f", c.getFloat(11));
                row[4] = c.getString(14);
                float sum = c.getFloat(8) * c.getFloat(11);
                row[5] = String.format(Locale.ENGLISH, "%.2f", sum);
                if (stock.get(c.getString(14)) == null) {
                    stock.put(c.getString(14), sum);
                } else {
                    stock.put(c.getString(14), stock.get(c.getString(14)) + sum);
                }
                row[6] = "" + stock.get(c.getString(14));
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
