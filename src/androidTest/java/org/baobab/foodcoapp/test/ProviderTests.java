package org.baobab.foodcoapp.test;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.test.ProviderTestCase2;

import org.baobab.foodcoapp.AccountingProvider;


public class ProviderTests extends ProviderTestCase2<AccountingProvider> {

    static {
        AccountingProvider.AUTHORITY = "org.baobab.foodcoapp.test";
    }

    public ProviderTests() {
        super(AccountingProvider.class, "org.baobab.foodcoapp.test");
    }

    public void testAccountBalance() {
        createDummyAccount("dummy");
        insertTransaction("final", "kasse", "dummy");
        Cursor accounts = query("accounts/passiva/accounts", 4);
        accounts.moveToPosition(3);
        assertEquals("name", "dummy", accounts.getString(1));
        assertEquals("balance", 42.0, accounts.getDouble(4));
    }

    public void testEmptyAccount() {
        createDummyAccount("dummy");
        query("accounts/passiva/accounts", 4);
        Uri transaction = insertTransaction("draft", "lager", "dummy");
        ContentValues b = new ContentValues();
        b.put("account_guid", "dummy");
        b.put("unit", "zeug");
        b.put("quantity", 42);
        getMockContentResolver().insert(transaction.buildUpon()
                .appendEncodedPath("products").build(), b);
        query("accounts/passiva/accounts", 4);
    }

    public void testListAccounts() {
        query("accounts", 11);
        createDummyAccount("dummy");
        query("accounts", 12);
        query("accounts/aktiva/accounts", 6);
        query("accounts/passiva/accounts", 4);
    }

    public void testFindAccountByName() {
        createDummyAccount("dummy");
        createDummyAccount("another");
        Cursor accounts = getMockContentResolver().query(Uri.parse(
                "content://org.baobab.foodcoapp.test/accounts/passiva/accounts"), null, "name IS 'foo'", null, null);
        assertEquals("no account", 0, accounts.getCount());
        accounts = getMockContentResolver().query(Uri.parse(
                "content://org.baobab.foodcoapp.test/accounts/passiva/accounts"), null, "name IS 'dummy'", null, null);
        assertEquals("one account", 1, accounts.getCount());
        accounts = getMockContentResolver().query(Uri.parse(
                "content://org.baobab.foodcoapp.test/accounts"), null, "guid IS 'dummy'", null, null);
        assertEquals("one account", 1, accounts.getCount());
    }

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

    public void testInsertProduct() {
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
        // edit amount
        b.put("quantity", -4.0);
        getMockContentResolver().insert(transaction.buildUpon()
                .appendEncodedPath("products").build(), b);
        products = query(transaction, 3);
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

    @NonNull
    private Cursor query(String path, int assert_count) {
        return query(Uri.parse("content://org.baobab.foodcoapp.test/" + path), assert_count);
    }

    @NonNull
    private Cursor query(Uri uri, int assert_count) {
        Cursor transactions = getMockContentResolver().query(uri, null, null, null, null);
        assertEquals("number of results", assert_count, transactions.getCount());
        transactions.moveToFirst();
        return transactions;
    }

    private Uri createDummyAccount(String name) {
        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("guid", name);
        values.put("parent_guid", "passiva");
        return getMockContentResolver().insert(
                Uri.parse("content://org.baobab.foodcoapp.test/accounts/passiva/accounts"), values);
    }

    private Uri insertTransaction(String status, String from_account, String to_account) {
        ContentValues t = new ContentValues();
        t.put("status", status);
        t.put("stop", 1);
        Uri transaction = getMockContentResolver().insert(Uri.parse(
                "content://org.baobab.foodcoapp.test/transactions"), t);
        ContentValues b = new ContentValues();
        b.put("account_guid", from_account);
        b.put("product_id", 23);
        b.put("quantity", -42);
        b.put("price", 1.0);
        b.put("unit", "dinge");
        getMockContentResolver().insert(transaction.buildUpon()
                .appendEncodedPath("products").build(), b);
        b = new ContentValues();
        b.put("account_guid", to_account);
        b.put("quantity", 21);
        b.put("price", 2.0);
        b.put("unit", "sachen");
        getMockContentResolver().insert(transaction.buildUpon()
                .appendEncodedPath("products").build(), b);
        return transaction;
    }


}