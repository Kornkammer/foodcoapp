package org.baobab.foodcoapp.test;

import android.content.ContentResolver;
import android.content.Context;
import android.test.mock.MockContext;

import org.baobab.foodcoapp.LedgerProvider;
import org.baobab.foodcoapp.io.BackupExport;
import org.baobab.foodcoapp.io.Report;
import org.baobab.foodcoapp.util.Trace;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import au.com.bytecode.opencsv.CSVWriter;

public class TraceTests extends BaseProviderTests {

    static {
        LedgerProvider.TEST_DB = "test.db";
    }

    Context ctx = new MockContext() {
        @Override
        public ContentResolver getContentResolver() {
            return getMockContentResolver();
        }
    };

    public void testFoo() {


        //Trace.Txn bank_in = Trace.search(getContext(), "bank", 0, null, -1, 0,null, null);
        //Trace.Txn bank_out = Trace.search(getContext(), "bank", 0, null, 1, 0,null, null);
        //System.out.println("sum " + sum);
        //sum = 0;
        //for (Trace.Prod p : bank_in.list("einlagen")) {
        //    System.out.println("   + " + p.title + ": " + (p.quantity() * p.price));
        //    sum += (p.quantity() * p.price);
        //}
        //
        //System.out.println("sum " + sum);
        //System.out.println("BANK EINGÄNGE");
        //printCredit(bank_in, bank_out, "spenden");
        //printCredit(bank_in, bank_out, "einlagen");
        //printCredit(bank_in, bank_out, "beiträge");
        //printCredit(bank_in, bank_out, "mitglieder");
        //printCredit(bank_in, bank_out, "verbindlichkeiten");
        //System.out.println();
        //System.out.println("BANK AUSGÄNGE");
        //printDebit(bank_in, bank_out, "inventar");
        //printDebit(bank_in, bank_out, "kosten");
        //printDebit(bank_in, bank_out, "lager");
        //printDebit(bank_in, bank_out, "kasse");
        //printDebit(bank_in, bank_out, "forderungen");
        System.out.println();
        System.out.println();
        System.out.println("**********************");

//        String query = BackupExport.getTimeWindowQuery(2015);
//                Trace bank_in = new Trace(getContext(), "bank", query, -1);
//        Trace bank_out = new Trace(getContext(), "bank", query, 1);

        //export(bank, "beiträge");
        //export(bank, "inventar");
        //verifyAccount(bank_out, bank_in, "inventar", 1920.87f, 0);
        //verifyAccount(bank_out, bank_in,  "kosten", 240.33f, 0);
        //verifyAccount(bank_out, bank_in,  "lager", 3325.78f, 0);
        //
        //verifyAccount(bank_in, bank_out, "einlagen", -1820f, 80);
        //verifyAccount(bank_in, bank_out, "beiträge", -1597f, 129);

        //txnLog(bank_out, "inventar");

        //export(bank_out, "lager");

        new Report(getContext(), 2016);
    }


    private void verifyAccount(Trace fund, Trace refund, String account, float saldo, float resaldo) {
        Trace.Txn balance = export(fund, account);
        System.out.println("bank -> " + account + " :: " + fund.split.saldo(account) + " + " + fund.combine.saldo(account) + " = balance: " + balance.saldo(account));
        assertEquals("bank -> " + account, fund.split.saldo(account) + fund.combine.saldo(account), balance.saldo(account));
        assertEquals("saldo " + account, saldo, balance.saldo(account));
        //Trace.Txn rebalance = export(refund, account);
        System.out.println("refund " + refund.split.saldo(account));
        System.out.println("refund " + refund.combine.saldo(account));
    }

    private void assertEquals(String msg, float a, float b) {
        assertTrue(msg + " is " + b + " but should be " + a, Math.abs(a - b) < 0.01);
    }

    private Trace.Txn export(Trace bank, String account) {
        Trace.Txn balance = new Trace.Txn();
        System.out.println("**********************");
        System.out.println("**** COMBINE ****************** " + account);
        //print(bank.combine(account), account, balance, null);
        //print(bank.split("forderungen"), "forderungen", balance);
        //System.out.println("**********************");
        //print(bank.split("verbindlichkeiten"), "verbindlichkeiten", balance);
        System.out.println("**********************");
        System.out.println("**** SPLIT ****************** " + account);
        //print(bank.split(account), account, balance, null);
        for (Trace.Prod p : balance.list(account)) {
            if (p.value() != 0) System.out.println( " * " + p);
        }
        return balance;
    }



    private float print(Trace.Txn balance, Trace.Txn t, String account) {
        float sum = 0;
        if (!account.equals("lager")) {
            for (Trace.Prod p : t.list(account)) {
                if (p.value() != 0) {
                    System.out.println( " * " + p);
                    balance.add(p);
                    sum += p.value();
                }
            }
        } else {
            Trace.Prod o = new Trace.Prod("aktiva", account, t.saldo(account));
            if (t.list(account).size() == 1)
                o = t.list(account).get(0);
            if (o.value() != 0)
                System.out.println(" + " + o);
            sum += o.value();
            balance.add(o);
        }
        return sum;
    }








    private void row(CSVWriter out, Trace.Prod p, int level, Map<String, Boolean> seen) {
        if (level > 1) return;
        if (seen.containsKey(p.key())) return;
        seen.put(p.key(), true);
        for (Trace.Prod i : p.inputs) {
            String[] row = new String[12];
            if (i.name.equals("Kosten")) {
                row[9] = String.format(Locale.ENGLISH, "%.2f", i.quantity() * i.price);
                row[1] = indent(level) + i.title + " " + i.levelIn + " " + i.levelOut;
            } else if (i.name.equals("Bank")) {
                row[11] = indent(level) + i.title + " " + i.levelIn + " " + i.levelOut;
                row[10] = String.format(Locale.ENGLISH, "%.2f", i.quantity() * i.price);
            } else {
                row[1] = indent(level) + i.title + " " + i.levelIn + " " + i.levelOut;
                row[3 + level] = String.format(Locale.ENGLISH, "%.2f", i.quantity() * i.price);
            }
            out.writeNext(row);
            row(out, i, level + 1, seen);
        }
    }

    private void printCredit(Trace.Txn in, Trace.Txn out, String account) {
        System.out.println(" # " + account + ": " +
                (- in.saldo(account) + out.debit(account)) +
                " - " + out.debit(account) + " = " + (- in.saldo(account)));
    }
    private void printDebit(Trace.Txn in, Trace.Txn out, String account) {
        System.out.println(" # " + account + ": " +
                (out.saldo(account) + in.credit(account)) +
                " - " + in.credit(account) + " = " + out.saldo(account));
    }


    String indent(int level) {
        String s = "";
        for (int i = 0; i < level; i++) s += " ";
        return s + level + " ";
    }

}
