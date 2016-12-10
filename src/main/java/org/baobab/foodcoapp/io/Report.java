package org.baobab.foodcoapp.io;

import android.content.Context;

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

    public Report(Context ctx, int year) {
        this.ctx = ctx;
        File result = file(year + ".zip");
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

    private void eur(ZipOutputStream zos, int year) {
        try {
            String query = getTimeWindowQuery(year);
            Trace bank_in = new Trace(ctx, "bank", query, -1);
            Trace bank_out = new Trace(ctx, "bank", query, 1);
            File file = file(year + "_EUR.csv");
            File file2 = file(year + "_EUR_full.csv");
            CSVWriter csv = new CSVWriter(new FileWriter(file), ';', CSVWriter.NO_QUOTE_CHARACTER);
            CSVWriter full = new CSVWriter(new FileWriter(file2), ';', CSVWriter.NO_QUOTE_CHARACTER);
            csv.writeNext(new String[] { "", "Einnahmen", "Ausgaben" });
            full.writeNext(new String[] { "", "", "Einnahmen", "Ausgaben" });
            log(zos, csv, full, bank_out, bank_in, "einlagen", year, false);
            log(zos, csv, full, bank_out, bank_in, "beiträge", year, false);
            log(zos, csv, full, bank_out, bank_in, "korns", year, false);
            log(zos, csv, full, bank_out, bank_in, "spenden", year, false);
            log(zos, csv, full, bank_in, bank_out, "inventar", year, true);
            log(zos, csv, full, bank_in, bank_out, "kosten", year, true);
            log(zos, csv, full, bank_in, bank_out, "lager", year, true);
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

    private static Trace.Txn log(ZipOutputStream zos, CSVWriter eur, CSVWriter full,
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
        System.out.println(".");
        System.out.println("############## IN COMBINE " + account);
        write(in.combine(account), account, balance, csv, aktiva);
        System.out.println(".");
        System.out.println("############## IN SPLIT " + account);
        write(in.split(account), account, balance, csv, aktiva);
        System.out.println(".");
        System.out.println("############## OUT COMBINE " + account);
        write(out.combine(account), account, balance, csv, aktiva);
        System.out.println(".");
        System.out.println("############## OUT SPLIT " + account);
        write(out.split(account), account, balance, csv, aktiva);
        csv.close();
        zip(year + "_EUR_log", log, zos);
        log.delete();
        System.out.println(account + ": " + format((aktiva? 1 : -1) * balance.saldo(account)));
        full.writeNext(new String[] { account.toUpperCase() });
        if (!account.equals("einlagen") && !account.equals("beiträge") && !account.equals("korns")) {
            float sum = 0;
            for (Trace.Prod p : balance.list(account)) {
                if (p.value() != 0) {
                    full.writeNext(new String[] { "   " + p.label(), format((aktiva? 1 : -1) * p.value()) });
                    System.out.println( " * " + p);
                    sum += p.value();
                }
            }
            if (Math.abs(sum - balance.saldo(account)) > 0.01) {
                eur.writeNext(new String[] { " GEHT NICHT AUF!  " });
            }
        }
        String[] h = new String[5];
        h[0] = account.toUpperCase();
        if (aktiva) {
            h[(aktiva? 2 : 1)] = format( -balance.saldo(account));
        } else {
            h[1] = format(balance.credit(account));
            h[2] = format( -balance.debit(account));
        }
        eur.writeNext(h);
        h = new String[5];
        if (aktiva) {
            h[(aktiva? 3 : 2)] = format(-balance.saldo(account));
        } else {
            h[2] = format(balance.credit(account));
            h[3] = format( -balance.debit(account));
        }
        full.writeNext(h);
        full.writeNext(new String[] {});
        return balance;
    }

    private static void write(List<Trace.Txn> txns, String account,
                              Trace.Txn balance, CSVWriter csv, boolean aktiva) {
        System.out.println("ACCOUNTS: " + getAccounts(txns));
        if (getAccounts(txns).isEmpty()) return;
        for (Trace.Txn t : txns)  {
            float sum = 0;
            if (t.accounts.size() == 0) continue;
            if (balance.visited.containsKey(t.head.id + "")) {
                System.out.println("another one of " + t.head);
                continue;
            }
            balance.visited.put(t.head.id + "", true);
            if (csv != null) csv.writeNext(new String[] {});
            writeRow(balance, t, t.head, account, csv, aktiva);
            for (Trace.Prod p : t.list(account)) {
                if (!(t.head.title.equals(p.title) &&
                        t.head.account.equals(p.account) &&
                        t.head.quantity() * p.quantity() > 0)) {
                    sum += writeRow(balance, t, p, account, csv, aktiva);
                } else {
                    System.out.println("  head again");
                }
            }
            for (Trace.Account a : t.accounts.values())
                if (!a.name.equals(account)) {
                    for (Trace.Prod p : t.list(a.name)) {
                        sum += writeRow(balance, t, p, account, csv, aktiva);
                    }
                }
            System.out.println("************ sum " + (sum + t.head.value()));
            if (Math.abs(sum + t.head.value()) > 0.01) {
                System.out.println("GEHT NICHT AUF! head " + t.head + "  sum " + sum);
            }
        }
        for (Trace.Prod p : balance.list("forderungen")) {
            System.out.println("FORDERUNG " + p);
        }
    }


    private static float writeRow(Trace.Txn balance, Trace.Txn t, Trace.Prod p,
                                  String account, CSVWriter csv, boolean aktiva) {
        if (p.value() == 0) return 0;
        float sum = 0;
        String[] row = new String[7];
        row[0] = df.format(p.date);
        if (p.account.equals(account)) {
            if ((aktiva? 1 : -1) * p.quantity() < 0 ||
                    (aktiva? 1 : -1) * balance.get(p).value() < 0) {
                row[1] = "   " + p.label();
                row[2] = format((aktiva? 1 : -1) * p.value());
                System.out.println( " * INTERN " + p);
            } else {
                row[1] = p.label();
                row[2] = format((aktiva? 1 : -1) * p.value());
                System.out.println( " * " + p);
            }
        } else {
            System.out.println( " * " + p);
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
