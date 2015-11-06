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
        insertTransaction("final", "dummy", "kasse");
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
        insertTransaction("final", "lager", "dummy");
        Cursor transactions = query("accounts/dummy/transactions", 1);
        assertEquals("who", "dummy", transactions.getString(3));
        assertEquals("sum", 42.0, transactions.getDouble(6));
        assertEquals("Einkaufung", true, transactions.getInt(8) > 0);
        assertEquals("passiva", "passiva", transactions.getString(9));
        assertTrue("involved accounts", transactions.getString(5).contains("lager"));
    }

    public void testKontoauszug() {
        createDummyAccount("dummy");
        createDummyAccount("another");
        insertTransaction("final", "dummy", "kasse");
        insertTransaction("final", "lager", "dummy");
        insertTransaction("final", "lager", "another");
        query("accounts/dummy/transactions", 2);
    }

    public void testTransactionAccountsOrder() {
        createDummyAccount("dummy");
        Uri transaction = insertTransaction("final", "lager", "kasse");
        Cursor products = query(transaction, 2);
        assertEquals("lager", products.getString(11));
        transaction = insertTransaction("final", "lager", "dummy");
        products = query(transaction, 2);
        assertEquals("lager", products.getString(11));
    }

    public void testIncTransactionProducts() {
        createDummyAccount("dummy");
        Uri transaction = insertTransaction("final", "dummy", "kasse");
        ContentValues b = new ContentValues();
        b.put("account_guid", "lager");
        b.put("product_id", 55);
        b.put("unit", "piece");
        getMockContentResolver().insert(transaction.buildUpon()
                .appendEncodedPath("products").build(), b);
        Cursor products = query(transaction, 3);
        assertEquals("lager", products.getString(11));
        assertEquals("quantity", -1.0, products.getDouble(4));
        // press button again
        getMockContentResolver().insert(transaction.buildUpon()
                .appendEncodedPath("products").build(), b);
        products = query(transaction, 3);
        assertEquals("quantity", -2.0, products.getDouble(4));
    }

    public void testDecTransactionProducts() {
        createDummyAccount("dummy");
        Uri transaction = insertTransaction("final", "lager", "dummy");
        getMockContentResolver().delete(transaction.buildUpon()
                .appendEncodedPath("accounts/lager/products/23").build(), null, null);
        Cursor products = query(transaction, 2);
        assertEquals("quantity", -41.0, products.getDouble(4));
        getMockContentResolver().delete(transaction.buildUpon()
                .appendEncodedPath("accounts/dummy/products/23").build(), null, null);
        products = query(transaction, 2);
        products.moveToLast();
        assertEquals("quantity", 41.0, products.getDouble(4));
    }

    public void testRemoveProductsFromTransaction() {
        createDummyAccount("dummy");
        Uri transaction = insertTransaction("final", "lager", "dummy");
        getMockContentResolver().delete(transaction.buildUpon()
                .appendEncodedPath("accounts/lager/products/23").build(), "nix null", null);
        query(transaction, 1);
    }

    public void testEditTransactionProductAmount() {
        createDummyAccount("dummy");
        Uri transaction = insertTransaction("final", "dummy", "kasse");
        ContentValues b = new ContentValues();
        b.put("account_guid", "lager");
        b.put("product_id", 55);
        b.put("unit", "piece");
        b.put("quantity", -4.0);
        getMockContentResolver().insert(transaction.buildUpon()
                .appendEncodedPath("products").build(), b);
        Cursor products = query(transaction, 3);
        assertEquals("lager", products.getString(11));
        assertEquals("quantity", -4.0, products.getDouble(4));
        // same product in another account
        b.put("account_guid", "inventar");
        getMockContentResolver().insert(transaction.buildUpon()
                .appendEncodedPath("products").build(), b);
        products = query(transaction, 4);
        assertEquals("quantity", -4.0, products.getDouble(4));
    }

    public void testInsertCashToBalance() {
        createDummyAccount("dummy");
        Uri transaction = insertTransaction("final", "dummy", "kasse");
        ContentValues b = new ContentValues();
        b.put("account_guid", "lager");
        b.put("product_id", 2);
        b.put("quantity", -5.5);
        getMockContentResolver().insert(transaction.buildUpon()
                .appendEncodedPath("products").build(), b);
        Cursor products = query(transaction, 3);
        assertEquals("lager", products.getString(11));
        assertEquals("quantity", -5.5, products.getDouble(4));

        b.put("quantity", -1.5);
        getMockContentResolver().insert(transaction.buildUpon()
                .appendEncodedPath("products").build(), b);
        products = query(transaction, 3);
        assertEquals("lager", products.getString(11));
        assertEquals("quantity", -7.0, products.getDouble(4));
    }
}