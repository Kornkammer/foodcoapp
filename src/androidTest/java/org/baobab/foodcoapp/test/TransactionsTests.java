package org.baobab.foodcoapp.test;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.test.ProviderTestCase2;

import org.baobab.foodcoapp.AccountingProvider;


public class TransactionsTests extends BaseProviderTests {

    public void testDeposit() { // Bilanzerhöhung
        createDummyAccount("dummy");
        insertTransaction("dummy", "kasse");
        Cursor transactions = query("accounts/dummy/transactions", 1);
        assertEquals("time", 1, transactions.getLong(2));
        assertEquals("sum", 42.0, transactions.getDouble(6));
        assertEquals("who", "dummy", transactions.getString(3));
        assertEquals("Einzahlung", true, transactions.getInt(8) < 0);
        assertEquals("passiva", "passiva", transactions.getString(9));
        assertTrue("involved accounts", transactions.getString(5).contains("kasse"));
    }

    public void testWithdraw() { // Bilanzerniedrigung
        createDummyAccount("dummy");
        insertTransaction("lager", "dummy");
        Cursor transactions = query("accounts/dummy/transactions", 1);
        assertEquals("who", "dummy", transactions.getString(3));
        assertEquals("sum", 42.0, transactions.getDouble(6));
        assertEquals("Einkaufung", true, transactions.getInt(8) > 0);
        assertEquals("passiva", "passiva", transactions.getString(9));
        assertTrue("involved accounts", transactions.getString(5).contains("lager"));
    }

    public void testSessionTransactions() {
        createDummyAccount("dummy");
        insertTransaction(5, "final", "lager", "kasse", 20f);
        insertTransaction(5, "final", "lager", "kasse", 44f);
        Cursor transactions = query("sessions/5/transactions", 2);
    }

    public void testKontoauszug() {
        createDummyAccount("dummy");
        createDummyAccount("another");
        insertTransaction("dummy", "kasse");
        insertTransaction("lager", "dummy");
        insertTransaction("lager", "another");
        query("accounts/dummy/transactions", 2);
    }

    public void testForderungen() { // subquery to display open/closed
        createDummyAccount("dummy");
        insertTransaction("dummy", "forderungen", 42f, "Bar Dumm"); // like deposit
        Cursor transactions = query("accounts/dummy/transactions", 1); // kontoauszug
        assertEquals("sum", 42.0, transactions.getDouble(6));
        assertEquals("who", "dummy", transactions.getString(3));
        Cursor products = query("accounts/forderungen/products", "title IS 'Bar Dumm'", 1); // open
        assertEquals("amount", 1f, products.getFloat(4));
        assertEquals("price", 42f, products.getFloat(5));
        assertEquals("title", "Bar Dumm", products.getString(7));
        insertTransaction("forderungen", "bank", 42f, "Bar Dumm"); // begleichen
        query("accounts/forderungen/products", "title IS 'Bar Dumm'", 0);
    }
}