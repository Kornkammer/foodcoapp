package org.baobab.foodcoapp.test;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;


public class TransactionsTests extends BaseProviderTests {

    public void testGetSingleTransaction() {
        createDummyAccount("dummy");
        Uri uri = insertTransaction("dummy", "kasse");
        Cursor t = query(uri, 1);
        assertEquals("who", "dummy", t.getString(3));
        assertEquals("amount", -42f, t.getFloat(8));
        assertEquals("price", 1f, t.getFloat(11));
        assertTrue("involved accounts", t.getString(5).contains("kasse"));
    }

    public void testDeposit() { // Bilanzerhöhung
        createDummyAccount("dummy");
        insertTransaction("dummy", "kasse");
        Cursor transactions = query("accounts/dummy/transactions", 1);
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

    public void testQueryTransaction() {
        createDummyAccount("dummy");
        insertTransaction("dummy", "forderungen", 25, "foo");
        insertTransaction("dummy", "forderungen", 25, "bar");
        insertTransaction("dummy", "forderungen", 42.23f, "foo");
        insertTransaction("dummy", "forderungen", 25, "foo bar");
        query("transactions?price=42.23", 1);
        query("transactions?price=25", 3);
        query("transactions?title=baz", 0);
        query("transactions?title=bar", 1);
        query("transactions?title=foo", 2);
        query("transactions?title=foo%20bar", 1);
        query("transactions?title=foo&price=10", 0);
        Cursor t = query("transactions?title=foo&price=25", 1);
        assertEquals("sum", 25.0f, t.getFloat(6));
    }

    public void testFinalizeTransaction() {
        createDummyAccount("dummy");
        Uri txn = insertTransaction(5, "draft", "lager", "kasse", 20f);
        ContentValues cv = new ContentValues();
        cv.put("status", "final");
        getMockContentResolver().update(txn, cv, null, null);
        Cursor t = query("transactions", "transactions._id = " + txn.getLastPathSegment(), 1);
        assertEquals("final", t.getString(10));
    }

    public void testFinalizeInvalidTransaction() {
        createDummyAccount("dummy");
        Uri txn = insertInvalidTransaction(50);
        ContentValues cv = new ContentValues();
        cv.put("status", "final");
        getMockContentResolver().update(txn, cv, null, null);
        Cursor t = query("transactions", "transactions._id = " + txn.getLastPathSegment(), 1);
        assertEquals("draft", t.getString(10));
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

    public void testFindLatestTransactions() {
        createDummyAccount("dummy");
        insertTransaction(9, "draft", "dummy", "kasse", 1, 30, "Bar Dumm");
        insertTransaction(9, "final", "dummy", "kasse", 1, 30, "Bar Dumm");
        insertTransaction(9, "final", "dummy", "kasse", 1, 50, "Bar Dumm");
        insertTransaction(9, "final", "dummy", "kasse", 1, 50, "Bar Dumm");
        query("accounts/dummy/transactions", 3);
        Cursor txns = query("accounts/dummy/transactions", "title IS 'Bar Dumm'", 4);
        txns.moveToLast();
        assertEquals(4, txns.getLong(0));
        txns.moveToPrevious();
        assertEquals(3, txns.getLong(0));
    }

    public void testSessionTransactions() {
        createDummyAccount("dummy");
        insertTransaction(5, "draft", "lager", "kasse", 20f);
        insertTransaction(5, "draft", "lager", "kasse", 44f);
        Cursor transactions = query("sessions/5/transactions", 2);
        assertEquals("draft", transactions.getString(10));
        int result = finalizeSession(5);
        assertEquals("two txns updated", 2, result);
        transactions = query("sessions/5/transactions", 2);
        assertEquals("final", transactions.getString(10));
        transactions.moveToNext();
        assertEquals("final", transactions.getString(10));
    }

    public void testInvalidSessionTransactions() {
        createDummyAccount("dummy");
        insertTransaction(5, "draft", "lager", "kasse", 20f);
        insertInvalidTransaction(30);
        int result = finalizeSession(5);
        assertEquals("no txns updated", 0, result);
        Cursor transactions = query("sessions/5/transactions", 2);
        assertEquals("draft", transactions.getString(10));
        transactions.moveToNext();
        assertEquals("draft", transactions.getString(10));
    }

    private Uri insertInvalidTransaction(float amount) {
        Uri txn = insertTransaction(5, "draft", "lager", "kasse", amount);
        ContentValues b = new ContentValues();
        b.put("account_guid", "dummy");
        b.put("quantity", -1);
        b.put("title", "Fehlbuchunug");
        b.put("unit", "dinge");
        b.put("price", 20);
        getMockContentResolver().insert(txn.buildUpon()
                .appendEncodedPath("products").build(), b);
        return txn;
    }
}