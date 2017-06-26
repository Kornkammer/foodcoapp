package org.baobab.foodcoapp.io;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import org.baobab.foodcoapp.util.Trace;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipOutputStream;

import au.com.bytecode.opencsv.CSVWriter;

import static org.baobab.foodcoapp.io.BackupExport.*;

public class Report {

    private final Context ctx;
    private final int year;
    private final File result;

    public Report(Context ctx, int year) {
        this.ctx = ctx;
        this.year = year;
        result = file(year + ".zip");
        try {
            ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(result)));
            eur(zos, year);
            zos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public File getZip() {
        return result;
    }

    private void eur(ZipOutputStream zos, int year) {
        try {
            String query = getTimeWindowQuery(year);
            Trace bank_in = new Trace(ctx, "bank", query, -1);
            Trace bank_out = new Trace(ctx, "bank", query, 1);
            File file = file(year + "_EUR.csv");
            File file2 = file(year + "_EUR_full.csv");
            CSVWriter csv = new CSVWriter(new FileWriter(file), ';');
            CSVWriter full = new CSVWriter(new FileWriter(file2), ';');

            csv.writeNext(new String[] { "Einnahme-Überschuss-Rechnung " + year });
            full.writeNext(new String[] { "Einnahme-Überschuss-Rechnung " + year });
            csv.writeNext(new String[] {});
            full.writeNext(new String[] {});
            csv.writeNext(new String[] { "Einnahmen", "Ausgaben" });
            full.writeNext(new String[] { "Vermögensübersicht", "", "Einnahmen", "Ausgaben" });
            csv.writeNext(new String[] {});
            full.writeNext(new String[] {});
            float income = 0;
            Trace.Txn bal = log(zos, csv, full, bank_out, bank_in, "einlagen", year, false);
            income += bal.saldo("einlagen");
            verify("einlagen", query, bal);
            full.writeNext(new String[] {});
            full.writeNext(new String[] {});
            full.writeNext(new String[] { "Ideeller Bereich", "", "Einnahmen", "Ausgaben" });
            full.writeNext(new String[] {});
            float bereich_in = 0;
            float bereich_out = 0;
            bal = log(zos, csv, full, bank_out, bank_in, "beiträge", year, false);
            income += bal.saldo("beiträge");
            bereich_in += bal.credit("beiträge");
            bereich_out += bal.debit("beiträge");
            verify("beiträge", query, bal);
            bal = log(zos, csv, full, bank_out, bank_in, "spenden", year, false);
            income += bal.saldo("spenden");
            bereich_in += bal.credit("spenden");
            bereich_out += bal.debit("spenden");
            verify("spenden", query, bal);
            float ideal = bereich_in + bereich_out;
            full.writeNext(new String[] { "Gewinn und Verlust ideeller Bereich", "", format(bereich_in), format(bereich_out), "", format(ideal)});
            full.writeNext(new String[] {});
            full.writeNext(new String[] {});
            full.writeNext(new String[] { "Zweckbetrieb", "", "Brutto-\nEinnahmen", "Brutto-\nAusgaben", "enthaltene\nUSt 7%", "enthaltene\nUSt 19%" });
            full.writeNext(new String[] {});
            bereich_in = 0;
            bereich_out = 0;
            bal = log(zos, csv, full, bank_out, bank_in, "korns", year, false);
            float korns = bal.saldo("korns");
            income += bal.saldo("korns");
            bereich_in += bal.saldo("korns");
            verify("mitglieder", query, bal);
            float expense = 0;
            bal = log(zos, csv, full, bank_in, bank_out, "inventar", year, true);
            expense += bal.saldo("inventar");
            bereich_out += bal.saldo("inventar");
            verify("inventar", query, bal);
            bal = log(zos, csv, full, bank_in, bank_out, "kosten", year, true);
            expense += bal.saldo("kosten");
            bereich_out += bal.saldo("kosten");
            verify("kosten", query, bal);
            bal = log(zos, csv, full, bank_in, bank_out, "lager", year, true);
            expense += bal.saldo("lager");
            bereich_out += bal.saldo("lager");
            verify("lager", query, bal);
            float zweck = bereich_in + bereich_out;
            full.writeNext(new String[] { "Verlust Zweckbetrieb", "", format(-bereich_in), format(-bereich_out), "", format(zweck)});
            full.writeNext(new String[] {});
            full.writeNext(new String[] {});
            full.writeNext(new String[] {});
            full.writeNext(new String[] { "Zusammenstellung"});
            full.writeNext(new String[] { "Gewinn ideeller Bereich", "", "", "", "", "", format(ideal)});
            full.writeNext(new String[] { "Verlust Zweckbetrieb", "", "", "", "", "", format(zweck)});
            full.writeNext(new String[] { "wirtschaftlicher Bereich (keine Betätigung)", "", "", "", ""});
            full.writeNext(new String[] { "Gesamt Ergebnis", "", "", "", "", "", format(ideal + zweck)});


            bal = log(zos, csv, full, bank_in, bank_out, "kasse", year, true);
            verify("kasse", query, bal);
            bal = log(zos, csv, full, bank_in, bank_out, "forderungen", year, true);
            expense += bal.saldo("forderungen");
            verify("forderungen", query, bal);
            bal = log(zos, csv, full, bank_in, bank_out, "verbindlichkeiten", year, true);
            income += bal.saldo("verbindlichkeiten");
            verify("verbindlichkeiten", query, bal);
            bal = log(zos, csv, full, bank_in, bank_out, "bank", year, true);
            verify("bank", query, bal);

            System.out.println("EINNAHMEN " + income * -1);
            System.out.println("AUSGABEN " + expense * -1);
            System.out.println("___________________________");
            System.out.println("ERGEBNIS " + (income + expense) * -1);
            System.out.println(".");

            if (Math.abs(bal.saldo("bank") - (income + expense)) > 0.01 ) {
                System.out.println("OH NOES! result NOT add up to bank saldo " + bal.saldo("bank"));
            } {
                System.out.println("Everything balanced :-)");
            }
            csv.close();
            full.close();
            zip(file, zos);
            zip(file2, zos);
            file.delete();
            file2.delete();
            zos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void verify(String account, String query, Trace.Txn bal) {
        Cursor check = ctx.getContentResolver().query(
                Uri.parse("content://org.baobab.foodcoapp/accounts?" + query), null, "guid IS ?", new String[] { account }, null);
        check.moveToFirst();
        System.out.println("saldo: " + check.getFloat(3));
        Cursor debit = ctx.getContentResolver().query(
                Uri.parse("content://org.baobab.foodcoapp/accounts?debit=true&" + query), null, "guid IS ?", new String[] { account }, null);
        debit.moveToFirst();
        System.out.println("debit: " + debit.getFloat(3));
        Cursor credit = ctx.getContentResolver().query(
                Uri.parse("content://org.baobab.foodcoapp/accounts?credit=true&" + query), null, "guid IS ?", new String[] { account }, null);
        credit.moveToFirst();
        System.out.println("credit: " + credit.getFloat(3));
        if (Math.abs(debit.getFloat(3) + credit.getFloat(3) - check.getFloat(3)) > 0.01) {
            System.out.println("WTF debit credit no add up");
        }
        if (Math.abs(bal.saldo(account) - check.getFloat(3)) > 0.01) {
            System.out.println("OH NO! trace no match saldo");
            Cursor prods = ctx.getContentResolver().query(
                    Uri.parse("content://org.baobab.foodcoapp/accounts/" + account +
                                    "/products?" + query), null, null, null, null);
            while (prods.moveToNext()) {
                System.out.println("  + " + prods.getString(7) + ": " + (prods.getFloat(4) * prods.getFloat(5)));
            }
        }
        System.out.println("=====================================");
        System.out.println(".");
        System.out.println(".");
    }

    private Trace.Txn log(ZipOutputStream zos, CSVWriter eur, CSVWriter full,
                                 Trace in, Trace out, String account,
                                 int year, boolean aktiva) throws IOException {
        Trace.Txn balance = new Trace.Txn();
        File log = file(year + "_" + account.replace("ä", "ae") + ".csv");
        CSVWriter csv = new CSVWriter(new FileWriter(log), ';', CSVWriter.NO_QUOTE_CHARACTER);
        String[] header = new String[7];
        header[0] = "Datum";
        header[1] = account.toUpperCase();
        header[3] = "7% USt";
        header[4] = "19% USt";
        header[6] = "Kommentar";
        csv.writeNext(header);

        //System.out.println("############## OUT COMBINE " + account);
        csv.writeNext(new String[] { "", "OUT COMBINE"});
        write(out.combine(account), account, balance, csv, aktiva);
        //System.out.println("############## OUT SPLIT " + account);
        csv.writeNext(new String[] { "", "OUT SPLIT"});
        write(out.split(account), account, balance, csv, aktiva);
        //System.out.println(".");

        //System.out.println("############## IN COMBINE " + account);
        csv.writeNext(new String[] { "", "IN COMBINE"});
        write(in.combine(account), account, balance, csv, aktiva);
        //System.out.println("############## IN SPLIT " + account);
        csv.writeNext(new String[] { "", "IN SPLIT"});
        write(in.split(account), account, balance, csv, aktiva);
        //System.out.println(".");
        csv.close();
        zip(year + "_EUR_log", log, zos);
        log.delete();

        String[] h = new String[5];
        h[0] = account.toUpperCase();
        if (aktiva) {
            h[2] = format( -balance.saldo(account));
        } else {
            h[1] = format(balance.credit(account));
            h[2] = format( -balance.debit(account));
        }
        eur.writeNext(h);

        if (account.equals("korns")) {
            full.writeNext(new String[] { "  GUTHABEN LEBENSMITTEL"});
            full.writeNext(new String[] { "  Einzahlungen", "", format(balance.credit(account)) });
            full.writeNext(new String[] { "  Auszahlungen", "", format(-balance.debit(account)) });
            full.writeNext(new String[] { "  bereits verzehrt (nur informativ)", "", "" });
            full.writeNext(new String[] { "  bestehendes Restguthaben (nur informativ)", "", "" });
            full.writeNext(new String[] { "Summe", "", format(-balance.saldo(account)) });
        } else {
            if (account.equals("lager")) {
                full.writeNext(new String[] { "WARENEINKAUF" });
            } else {
                full.writeNext(new String[] { account.toUpperCase() });
            }
            if (!account.equals("einlagen") && !account.equals("beiträge") && !account.equals("korns") && !account.equals("bank")) {
                float sum = 0;
                for (Trace.Prod p : balance.list(account)) {
                    if (p.value() != 0) {
                        if (aktiva) {
                            full.writeNext(new String[] { "   " + p.label(), "", "", format(-p.value()) });
                        } else {
                            full.writeNext(new String[] { "   " + p.label(), "", format(-p.value()) });
                        }
                        System.out.println( " * " + p);
                        sum += p.value();
                    }
                }
                if (Math.abs(sum - balance.saldo(account)) > 0.01) {
                    eur.writeNext(new String[] { "!!!!!!!!!!!!!!!!! GEHT NICHT AUF!  " });
                }
            }
            h = new String[5];
            h[0] = "Summe(n)";
            if (aktiva) {
                h[3] = format(-balance.saldo(account));
            } else {
                h[2] = format(balance.credit(account));
                h[3] = format( -balance.debit(account));
            }
            full.writeNext(h);
        }

        full.writeNext(new String[] {});

        for (Trace.Prod p : balance.list("forderungen")) {
            //System.out.println("FORDERUNG " + p);
        }
        System.out.println("SALDO " + account + ": " + format((aktiva? 1 : -1) * balance.saldo(account)));
        System.out.println("DEBIT " + account + ": " + format((aktiva? 1 : -1) * balance.debit(account)));
        System.out.println("CREDIT " + account + ": " + format((aktiva? 1 : -1) * balance.credit(account)));
        System.out.println(".......................\n");

        return balance;
    }

    private void write(List<Trace.Txn> txns, String account,
                              Trace.Txn balance, CSVWriter csv, boolean aktiva) {
        //System.out.println("ACCOUNTS: " + getAccounts(txns));
        if (getAccounts(txns).isEmpty()) return;
        for (Trace.Txn t : txns)  {
            float sum = 0;
            if (t.accounts.size() == 0) continue;
            if (balance.visited.containsKey(t.head.id + "")) {
                //System.out.println("another one of " + t.head);
                continue;
            }
            balance.visited.put(t.head.id + "", true);
            if (csv != null) csv.writeNext(new String[] {});
            writeRow(balance, t, t.head, account, csv, aktiva);
            for (Trace.Prod p : t.list(account)) {
                if (!(t.head.title.equals(p.title) &&
                        t.head.account.equals(p.account) &&
                        t.head.quantity() * p.quantity() > 0)) {
                    if (p.account.equals(account)) {
                        if (balance.visited.containsKey(p.id + "")) {
                            System.out.println("another SUB of " + t.head);
                            continue;
                        }
                        balance.visited.put(p.id + "", true);
                    }
                    sum += writeRow(balance, t, p, account, csv, aktiva);
                } else {
                    //System.out.println("  head again");
                }
            }
            for (Trace.Account a : t.accounts.values())
                if (!a.name.equals(account)) {
                    for (Trace.Prod p : t.list(a.name)) {
                        sum += writeRow(balance, t, p, account, csv, aktiva);
                    }
                }
            //System.out.println("************ sum " + (sum + t.head.value()));
            if (Math.abs(sum + t.head.value()) > 0.01) {
                System.out.println("GEHT NICHT AUF! head " + t.head + "  sum " + sum);
            }
        }
    }


    private float writeRow(Trace.Txn balance, Trace.Txn t, Trace.Prod p,
                                  String account, CSVWriter csv, boolean aktiva) {
        if (p.value() == 0) return 0;
        float sum = 0;
        String[] row = new String[7];
        row[0] = df.format(p.date);
        if (!row[0].contains(year + "")){
            System.out.println("ANOTHER YEAR!!! " + p);
        }
        if (p.account.equals(account)) {
            if ((aktiva? 1 : -1) * p.quantity() < 0 ||
                    (aktiva? 1 : -1) * balance.get(p).value() < 0) {
                row[1] = "   " + p.label();
                row[2] = format((aktiva? 1 : -1) * p.value());
                //System.out.println( " * INTERN " + p);
            } else {
                row[1] = p.label();
                row[2] = format((aktiva? 1 : -1) * p.value());
                //System.out.println( " * " + p);
            }
        } else {
            //System.out.println( " * " + p);
            row[5] = format((aktiva? -1 : 1) * p.value());
            row[6] = "  " + p;
        }
        if (csv != null) csv.writeNext(row);
        balance.add(p);
        sum += p.value();
        return sum;
    }

    private static String format(float val) {
        return String.format(Locale.ENGLISH, "%.2f", val);
    }

    private static List<String> getAccounts(List<Trace.Txn> txns) {
        HashSet<String> accounts = new HashSet<>();
        for (Trace.Txn t : txns)  {
            for (Trace.Account a : t.accounts.values())
                if (t.accounts.get(a.name) != null &&
                        t.accounts.get(a.name).saldo() != 0)
                    accounts.add(a.name);
        }
        return new ArrayList<>(accounts);
    }
}
