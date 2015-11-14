package org.baobab.foodcoapp;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import java.io.IOException;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import au.com.bytecode.opencsv.CSVReader;

public class GlsImport implements ImportActivity.Importer {

    public static SimpleDateFormat date = new SimpleDateFormat("dd.MM.yyyy");
    public static String AUTHORITY = "org.baobab.foodcoapp";
    private final Context ctx;
    private String msg = "";
    public final Uri uri;
    private int count = 0;
    String lineMsges = "";

    public GlsImport(Context ctx) {
        this.ctx = ctx;
        ContentValues cv = new ContentValues();
        cv.put("start", System.currentTimeMillis());
        uri = ctx.getContentResolver().insert(Uri.parse(
                "content://" + AUTHORITY + "/sessions"), cv);
    }

    @Override
    public int read(CSVReader csv) throws IOException {
        List<String[]> lines = csv.readAll();
        for (int i = lines.size()-1; i >= 0; i--) {
            readLine(lines.get(i));
        }
        if (lines.size() != count) {
            msg = "Could not read " + (lines.size() - count) + " transactions!" + "\n\n" + msg;
        }
        return count;
    }

    @Override
    public String getMsg() {
        return msg;
    }

    @Override
    public Uri getSession() {
        return uri;
    }

    static final Pattern vwz1 = Pattern.compile(
            "^(Einlage|einlage|Mitgliedsbeitrag|mitgliedsbeitrag|Beitrag|beitrag" +
                    "|Einzahlung|einzahlung|Guthaben|guthaben|Barkasse|barkasse)" +
                    "[-:,\\s]*([\\da-zA-Z]*)([-:,\\s]*|$)([\\da-zA-Z]*)([-:,\\s]*|$).*");

    static final Pattern vwz2Pattern = Pattern.compile("^([^-:\\s]*)[-:\\s]+(.*)([-:\\s]*|$)+.*");

    public void readLine(String[] line) {
        try {
            lineMsges = "";
            long time = date.parse(line[1]).getTime();
            float amount = NumberFormat.getInstance().parse(line[19]).floatValue();
//            Log.d(PosActivity.TAG, "reading line: " + line[5] + line[6] + line[7] + line[8] + " (amount=" + amount + ")");
            if (amount > 0) {
                String vwz = line[5] + line[6] + line[7] + line[8];
                String comment = "Bankeingang:\n\n" + line[3] + "\nVWZ: " + vwz;
                Account account = findAccount(vwz);
                if (account != null && account.guid != null && account.err == null) {
                    Uri transaction = storeTransaction(time, comment);
                    storeBankCash(transaction, amount);
                    if (vwz.toLowerCase().contains("einzahlung") ||
                                vwz.toLowerCase().contains("guthaben") ||
                                vwz.toLowerCase().contains("prepaid")) {
                        String title = "Bank " + account.name;
                        Iterator<Long> iter = findOpenTransactions("forderungen", "title IS '" + title + "'");
                        while (iter.hasNext()) {
                            Cursor txn = query("forderungen", "transactions._id =" + iter.next());
                            txn.moveToFirst();
                            float sum = txn.getFloat(8) * txn.getFloat(11);
                            if (amount + sum >= 0) { // quantity negative after groupBy from users perspective
                                lineMsges += "\nForderung beglichen: " + title + " -> " + String.format("%.2f", -sum);
                                storeTransactionItem(transaction, "forderungen", sum, title);
                                amount += sum;
                            }
                        }
                        if (amount > 0) { // rest guthaben
                            storeTransactionKorn(transaction, account.guid, -amount, "Korns");
                        }
                    } else if (vwz.toLowerCase().contains("mitgliedsbeitrag") ||
                                vwz.toLowerCase().contains("mitgliederbeitrag") ||
                                vwz.toLowerCase().contains("beitrag")) {
                        storeTransactionItem(transaction, "beiträge", - amount, account.name);
                    } else if (vwz.toLowerCase().contains("einlage")) {
                        storeTransactionItem(transaction, "einlagen", - amount, account.name);
                    } else if (vwz.toLowerCase().contains("spende")) {
                        storeTransactionItem(transaction, "spenden", - amount, "Spende");
                    } else { // found account but no keyword
                        storeTransactionItem(transaction, "verbindlichkeiten", - amount, account.name);
                    }
                } else if (vwz.toLowerCase().contains("barkasse")) {
                    Uri transaction = storeTransaction(time, comment);
                    storeBankCash(transaction, amount);
                    Iterator<Long> iter = findOpenTransactions("forderungen", "title LIKE 'Bar%'");
                    while (iter.hasNext()) {
                        Cursor txn = query("forderungen", "transactions._id =" + iter.next());
                        txn.moveToNext();
                        float sum = txn.getFloat(8) * txn.getFloat(11);
                        if (amount + sum >= 0) { // quantity negative after groupBy from users perspective
                            lineMsges += "\nForderung beglichen: Bar " + txn.getString(3) + " -> " + String.format("%.2f", -sum);
                            storeTransactionItem(transaction, "forderungen",
                                    sum, "Bar " + txn.getString(3));
                            amount += sum;
                        }
                    }
                    if (amount > 0) { // rest barkasse (should never happen!)
                        lineMsges += "\n Komischer Rest von Barkasse Einzahlung -> " +String.format("%.2f", amount);
                        storeTransactionItem(transaction, "verbindlichkeiten", - amount, "Barkasse");
                    }
                } else if (vwz.toLowerCase().contains("spende")) {
                    Uri transaction = storeTransaction(time, comment);
                    storeBankCash(transaction, amount);
                    storeTransactionItem(transaction, "spenden", - amount, "Spende");
                } else {
                    comment += "\nKein Mitglied gefunden";
                    Uri transaction = storeTransaction(time, comment);
                    storeBankCash(transaction, amount);
                    if (vwz.toLowerCase().contains("einzahlung") ||
                            vwz.toLowerCase().contains("guthaben") ||
                            vwz.toLowerCase().contains("prepaid")) {
                        storeTransactionItem(transaction, "verbindlichkeiten", - amount, "Einzahlung");
                    } else if (vwz.toLowerCase().contains("mitgliedsbeitrag") ||
                            vwz.toLowerCase().contains("mitgliederbeitrag") ||
                            vwz.toLowerCase().contains("beitrag")) {
                        storeTransactionItem(transaction, "verbindlichkeiten", - amount, "Beitrag");
                    } else if (vwz.toLowerCase().contains("einlage")) {
                        storeTransactionItem(transaction, "verbindlichkeiten", - amount, "Einlage");
                    } else if (vwz.toLowerCase().contains("spende")) {
                        storeTransactionItem(transaction, "verbindlichkeiten", - amount, "Spende");
                    } else { // no account and no keyword
                        storeTransactionItem(transaction, "verbindlichkeiten", - amount, vwz);
                    }
                }
                count++;
                msg += lineMsges;
                return;
            } else { // amount < 0
                String vwz1 = line[9] + line[10];
                String vwz2 = line[11] + line[12] + line[13] + line[14];
                String comment = "Bankausgang:\n\n" + (!line[3].equals("")? line[3]:"") +
                                    "\nVWZ: " + (!vwz1.equals("")? vwz1+"\n" : "") +
                                        (!vwz2.equals("")? vwz2+"\n" : "");
                if (line[3].equals("Auszahlung")) {
                    comment = comment + "\nVWZ " + line[5] + " " + line[6];
                    if (!settleOpenPayable("Auszahlung", amount, time, comment)) {
                        Uri transaction = storeTransaction(time, comment);
                        storeBankCash(transaction, amount);
                        storeTransactionItem(transaction, "forderungen", -amount, "Auszahlung");
                    }
                } else if (line[4].contains("Kontof�hrung") || line[4].contains("Kontoführung")) {
                    Uri transaction = storeTransaction(time, comment + "\nKontoführungsgebühren");
                    storeBankCash(transaction, amount);
                    storeBankCash(transaction, -amount, "kosten", "Kontogebühren");
                } else {
                    String text = line[9] + " " + line[10] + " " + vwz2;
                    if (text.toLowerCase().contains("auslage")) {
                        Account account = findAccount(vwz1);
                        if (account != null) {
                            Uri transaction = storeTransaction(time, comment);
                            storeBankCash(transaction, amount);
                            storeTransactionItem(transaction, "einlagen", -amount, account.name);
                            amount = 0;
                        }
                    }
                    if (amount < 0) {
                        if (findBookingInstruction(time, amount, comment, line[9])) {
                        } else if (findBookingInstruction(time, amount, comment, line[10])) {
                        } else if (findBookingInstruction(time, amount, comment, text)) {
                        } else {
                            if (!settleOpenPayable(line[9], amount, time, comment)
                                    && !settleOpenPayable(vwz1, amount, time, comment)
                                    && !settleOpenPayable(vwz2, amount, time, comment)) {

                                Uri transaction = storeTransaction(time, comment + "\nVWZ nicht erkannt");
                                storeBankCash(transaction, amount);
                                if (!vwz2.equals("")) {
                                    storeTransactionItem(transaction, "forderungen", -amount, vwz2);
                                } else if (!vwz1.equals("")) {
                                    storeTransactionItem(transaction, "forderungen", -amount, vwz1);
                                } else {
                                    storeTransactionItem(transaction, "forderungen", -amount, line[3]);
                                }
                            }
                        }
                    }
                }
            }
            count++;
            msg += lineMsges;
        } catch (ParseException e) {
            e.printStackTrace();
            msg += "\nError! " + e.getMessage();
        }
    }

    static final Pattern pattern =  Pattern.compile(".*([Kk]osten|[Ii]nventar)[-:,N\\s]+(.*)");
    private boolean findBookingInstruction(long time, float amount, String comment, String text) {
        Matcher m = pattern.matcher(text);
        if (m.matches()) {
            Uri transaction = storeTransaction(time, comment);
            storeBankCash(transaction, amount);
            storeTransactionItem(transaction, m.group(1).toLowerCase(), -amount, m.group(2));
            return true;
        }
        return false;
    }

    private boolean settleOpenPayable(String title, float amount, long time, String comment) {
        Iterator<Long> iter = findOpenTransactions("verbindlichkeiten", "title IS '" + title + "'");
        while (iter.hasNext()) {
            Cursor txn = query("verbindlichkeiten", "transactions._id =" + iter.next());
            txn.moveToFirst();
            float sum = txn.getFloat(8) * txn.getFloat(11);
            if (sum == amount) {
                Uri transaction = storeTransaction(time, comment);
                storeBankCash(transaction, amount);
                storeTransactionItem(transaction, "verbindlichkeiten", -amount, title);
                lineMsges += "\nVerbindlichkeit beglichen: " + title + " -> " + String.format("%.2f", -amount);
                return true;
            }
        }
        return false;
    }

    private Iterator<Long> findOpenTransactions(String guid, String selection) {
        Cursor products = ctx.getContentResolver().query(uri.buildUpon()
                        .appendEncodedPath("accounts/" + guid + "/products").build(),
                            null, selection, null , null);
        TreeSet<Long> ids = new TreeSet<>();
        while (products.moveToNext()) {
            Cursor txns = query(guid, "title IS '" + products.getString(7) + "'" +
                " AND price = " + products.getFloat(5) + // select before groupBy
                (products.getFloat(4) > 0? " AND quantity > 0" : " AND quantity < 0"));
            txns.moveToLast();
            for (int i = 0; i < Math.abs(products.getFloat(4)); i++) {
                ids.add(txns.getLong(0));
                if (!txns.isFirst()) {
                    txns.moveToPrevious();
                }
            }
        }
        return ids.iterator();
    }

    private Cursor query(String guid, String selection) {
        return ctx.getContentResolver().query(Uri.parse(
                        "content://" + AUTHORITY + "/accounts/" + guid + "/transactions"),
                null, selection + " AND transactions.status IS NOT 'draft'", null , null);
    }

    private void storeBankCash(Uri transaction, float amount) {
        storeBankCash(transaction, amount, "bank", "Cash");

    }
    private void storeBankCash(Uri transaction, float amount, String guid, String title) {
        ContentValues b = new ContentValues();
        b.put("account_guid", guid);
        b.put("product_id", 1);
        b.put("title", title);
        b.put("quantity", amount);
        b.put("price", 1.0);
        ctx.getContentResolver().insert(transaction.buildUpon()
                .appendEncodedPath("products").build(), b);
    }

    private Uri storeTransaction(long time, String comment) {
        ContentValues t = new ContentValues();
        t.put("session_id", uri.getLastPathSegment());
        t.put("stop", time);
        t.put("status", "draft");
        t.put("comment", comment);
        return ctx.getContentResolver().insert(Uri.parse(
                "content://" + AUTHORITY + "/transactions"), t);
    }

    private void storeTransactionKorn(Uri transaction, String account, float amount, String title) {
        if (title == null || title.equals("")) title = "Unbekannt";
        ContentValues b = new ContentValues();
        b.put("account_guid", account);
        b.put("product_id", 2);
        b.put("title", title);
        b.put("quantity", amount);
        b.put("price", 1.0f);
        ctx.getContentResolver().insert(transaction.buildUpon()
                .appendEncodedPath("products").build(), b);
    }

    private void storeTransactionItem(Uri transaction, String account, float amount, String title) {
        if (title == null || title.equals("")) title = "Unbekannt";
        ContentValues b = new ContentValues();
        b.put("account_guid", account);
        b.put("product_id", 3);
        b.put("title", title);
        b.put("quantity", amount > 0? 1: -1);
        b.put("price", Math.abs(amount));
        ctx.getContentResolver().insert(transaction.buildUpon()
                .appendEncodedPath("products").build(), b);
    }

    static class Account {
        String guid;
        String name;
        String err;
    }

    static final Pattern nameWith = Pattern.compile("[a-zA-Z�]+[\\-\\s]{1}[a-zA-Z�]+");
    static final Pattern name = Pattern.compile("[a-zA-Z�]+");
    static final Pattern guid = Pattern.compile("\\d+");

    private Account findAccount(String vwz) {
        Account account = findAccountBy("guid", guid, vwz);
        if (account == null) {
            account = findAccountBy("name", name, vwz);
        }
        if (account == null) {
            account = findAccountBy("name", nameWith, vwz);
        }
        if (account == null) {
            if (vwz.contains(" ")) {
                account = findAccountBy("name", nameWith,
                        vwz.substring(vwz.indexOf(" ")));
            }
        }
        if (account == null) {
            if (vwz.contains("-")) {
                account = findAccountBy("name", nameWith,
                        vwz.substring(vwz.indexOf("-")));
            }
        }
        return account;
    }

    private Account findAccountBy(String column, Pattern pattern, String vwz) {
        Account account = null;
        Matcher g = pattern.matcher(vwz);
        int i = 0;
        while (g.find()) {
            i++;
            account = findAccountBy(column, g.group());
            if (account != null) break;
        }
        return account;
    }

    private Account findAccountBy(String column, String value) {
        Cursor accounts = ctx.getContentResolver().query(Uri.parse(
                        "content://" + AUTHORITY + "/accounts"), null,
                "UPPER(" + column + ") IS UPPER(?)", new String[] { value }, null);
        if (accounts.getCount() == 1) {
            accounts.moveToFirst();
            Account a = new Account();
            a.name = accounts.getString(1);
            a.guid = accounts.getString(2);
            return a;
        }
        return null;
    }
}
