package org.baobab.foodcoapp;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

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
            msg = "Could not read " + (lines.size() - count) + " transactions";
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

    static final Pattern nameWith = Pattern.compile("[a-zA-Z�]+[\\-\\s]{1}[a-zA-Z�]+");
    static final Pattern name = Pattern.compile("[a-zA-Z�]+");
    static final Pattern guid = Pattern.compile("\\d+");

    public ContentValues readLine(String[] line) {
        try {
            long time = date.parse(line[1]).getTime();
            float amount = NumberFormat.getInstance().parse(line[19]).floatValue();
            if (amount > 0) {
                String vwz = line[5] + line[6] + line[7] + line[8] ;
                String comment = "Bankeingang:\n\n" + line[3] + "\nVWZ: " + vwz;
                Account account = findAccount(vwz);
                Uri transaction = storeTransaction(time, comment);
                storeBankCash(transaction, amount);
                if (account != null && account.guid != null && account.err == null) {
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
                                storeTransactionItem(transaction, "forderungen", sum, title);
                                amount += sum;
                            }
                        }
                        if (amount > 0) { // rest guthaben
                            storeTransactionItem(transaction, account.guid, - amount, "Credits");
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
                    Iterator<Long> iter = findOpenTransactions("forderungen", "title LIKE 'Bar%'");
                    while (iter.hasNext()) {
                        Cursor txn = query("forderungen", "transactions._id =" + iter.next());
                        txn.moveToNext();
                        float sum = txn.getFloat(8) * txn.getFloat(11);
                        if (amount + sum >= 0) { // quantity negative after groupBy from users perspective
                            storeTransactionItem(transaction, "forderungen",
                                    sum, "Bar " + txn.getString(3));
                            amount += sum;
                        }
                    }
                    if (amount > 0) { // rest barkasse (should never happen!)
                        storeTransactionItem(transaction, "verbindlichkeiten", - amount, "Barkasse");
                    }
                } else if (vwz.toLowerCase().contains("spende")) {
                    storeTransactionItem(transaction, "spenden", - amount, "Spende");
                } else {
                    storeTransactionItem(transaction, "verbindlichkeiten", - amount, vwz);
                }
                count++;
                return new ContentValues();
            } else { // amount < 0
                String vwz1 = line[9] + line[10];
                String vwz2 = line[11] + line[12] + line[13] + line[14];
                String comment = "Bankausgang:\n\n" + (line[3] != null? line[3]:"") +
                                    "\nVWZ: " + (vwz1 != null? vwz1+"\n" : "") + vwz2;
                Matcher m = vwz2Pattern.matcher(vwz2);
                Account account = findAccount(vwz2);
                if (m.matches()) {
                    Uri transaction = storeTransaction(time, comment);
                    storeBankCash(transaction, amount);
                    if (account != null && account.guid != null && account.err == null) {
                        storeTransactionItem(transaction, account.guid, -amount, m.group(2));
                        amount = 0;
                    } else {
                        Iterator<Long> iter = findOpenTransactions("verbindlichkeiten", "title IS '" + vwz2 + "'");
                        if (iter.hasNext()) {
                            Cursor txn = query("verbindlichkeiten", "transactions._id =" + iter.next());
                            txn.moveToFirst();
                            float sum = txn.getFloat(8) * txn.getFloat(11);
                            if (sum == amount) {
                                storeTransactionItem(transaction, "verbindlichkeiten", -amount, vwz2);
                                amount = 0;
                            }
                        }
                    }
                    if (amount < 0) { // still
                        storeTransactionItem(transaction, "forderungen", -amount, vwz2);
                        amount = 0;
                    }
                } else if (line[4].equals("Kontof�hrung") && line[5].contains("Abrechnung vom")) {
                    Uri transaction = storeTransaction(time, comment + "\nKontoführungsgebühren");
                    storeBankCash(transaction, amount);
                    storeTransactionItem(transaction, "kosten", -amount, "Kontogebühren");
                    amount = 0;
                }
                if (amount < 0) { // still
                    if (account == null) {
                        account = findAccount(vwz1);
                    }
                    if (account != null && (vwz1.toLowerCase().contains("auslage")
                            || vwz2.toLowerCase().contains("auslage"))) {
                            Uri transaction = storeTransaction(time, comment);
                            storeBankCash(transaction, amount);
                            storeTransactionItem(transaction, "einlagen", -amount, account.name);
                    } else {
                        Uri transaction = storeTransaction(time, comment + "\nVWZ nicht erkannt");
                        storeBankCash(transaction, amount);
                        if (vwz2.equals("")) {
                            storeTransactionItem(transaction, "forderungen", -amount, vwz1);
                        } else if (vwz1.equals("")) {
                            storeTransactionItem(transaction, "forderungen", -amount, vwz2);
                        } else {
                            storeTransactionItem(transaction, "forderungen", -amount, vwz1 + " \n" + vwz2);
                        }
                    }
                }
            }
            count++;
            return new ContentValues();
        } catch (ParseException e) {
            e.printStackTrace();
            msg += "\nError! " + e.getMessage();
            return null;
        }
    }

    private Iterator<Long> findOpenTransactions(String guid, String selection) {
        Cursor products = ctx.getContentResolver().query(Uri.parse(
                        "content://" + AUTHORITY + "/accounts/" + guid + "/products"),
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
        ContentValues b = new ContentValues();
        b.put("account_guid", "bank");
        b.put("product_id", 1);
        b.put("title", "Cash");
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

    private void storeTransactionItem(Uri transaction, String account, float amount, String title) {
        ContentValues b = new ContentValues();
        b.put("account_guid", account);
        b.put("product_id", 2);
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
    private Account findAccount(String name, String guid) {
        Account a = findAccountBy("name", name);
        if (a.guid == null) {
            a = findAccountBy("guid", name);
            if (a.guid == null) {
                a.err = "Unbekanntes Mitglied";
            }
            if (!guid.equals("")) {
                Account b = findAccountBy("name", guid);
                if (b.guid != null) {
                    if (!b.guid.equals(a.guid)) {
                        a.err = "Falsche MitgliedsNr";
                    }
                } else {
                    b = findAccountBy("guid", guid);
                    if (b.guid != null) {
                        if (!b.guid.equals(a.guid)) {
                            a.err = "Falscher MitgliedsName";
                        }
                    } else { // there is second param that is not found
                        a.err = "Falscher MitgliedsName";
                    }
                }
            }
            return a;
        }
        if (a.guid != null && !guid.equals("")) {
            Account b = findAccountBy("guid", guid);
            if (!a.guid.equals(b.guid)) {
                a.err = "Falsche MitgliedsNr";
            }
        }
        return a;
    }


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
            System.out.println(i + column + ": " + g.group());
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
