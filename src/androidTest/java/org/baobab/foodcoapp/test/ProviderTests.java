package org.baobab.foodcoapp.test;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
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
        insertTransaction("final", "dummy");
        Cursor accounts = getMockContentResolver().query(Uri.parse(
                "content://org.baobab.foodcoapp.test/accounts/passiva/accounts"), null, null, null, null);
        accounts.moveToPosition(3);
        assertEquals("name", "dummy", accounts.getString(1));
        assertEquals("balance", -42.0, accounts.getDouble(4));
    }

    public void testEmptyAccount() {
        createDummyAccount("dummy");
        Cursor accounts = getMockContentResolver().query(Uri.parse(
                "content://org.baobab.foodcoapp.test/accounts/passiva/accounts"), null, null, null, null);
        assertEquals("one account", 4, accounts.getCount());
        Uri transaction = insertTransaction("draft", "dummy");
        ContentValues b = new ContentValues();
        b.put("account_guid", "dummy");
        b.put("quantity", 42);
        getMockContentResolver().insert(transaction.buildUpon()
                .appendEncodedPath("products/2").build(), b);
        accounts = getMockContentResolver().query(Uri.parse(
                "content://org.baobab.foodcoapp.test/accounts/passiva/accounts"), null, null, null, null);
        assertEquals("one account", 4, accounts.getCount());
    }

    public void testListAccounts() {
        createDummyAccount("dummy");
        Cursor accounts = getMockContentResolver().query(Uri.parse(
                "content://org.baobab.foodcoapp.test/accounts"), null, null, null, null);
        assertEquals("twelve accounts in total", 12, accounts.getCount());
        accounts = getMockContentResolver().query(Uri.parse(
                "content://org.baobab.foodcoapp.test/accounts/aktiva/accounts"), null, null, null, null);
        assertEquals("no aktiva account", 6, accounts.getCount());
        accounts = getMockContentResolver().query(Uri.parse(
                "content://org.baobab.foodcoapp.test/accounts/passiva/accounts"), null, null, null, null);
        assertEquals("one passiva account", 4, accounts.getCount());
    }

    public void testFindAccountByName() {
        createDummyAccount("dummy");
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

    public void testKontoauszug() {
        createDummyAccount("dummy");
        createDummyAccount("another");
        insertTransaction("final", "dummy");
        insertTransaction("final", "another");
        Cursor transactions = getMockContentResolver().query(Uri.parse(
                "content://org.baobab.foodcoapp.test/accounts/dummy/transactions"), null, null, null, null);
        assertEquals("one transaction", 1, transactions.getCount());
        transactions.moveToFirst();
        assertEquals("time", 1, transactions.getLong(2));
        assertEquals("who", "dummy", transactions.getString(3));
        assertEquals("sum", 42.0, transactions.getDouble(6));
        assertEquals("Bilanzerniedrigung", true, transactions.getInt(8) < 0);
    }

    private Uri createDummyAccount(String name) {
        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("guid", name);
        values.put("parent_guid", "passiva");
        return getMockContentResolver().insert(
                Uri.parse("content://org.baobab.foodcoapp.test/accounts/passiva/accounts"), values);
    }

    private Uri insertTransaction(String status, String account) {
        ContentValues t = new ContentValues();
        t.put("status", status);
        t.put("stop", 1);
        Uri transaction = getMockContentResolver().insert(Uri.parse(
                "content://org.baobab.foodcoapp.test/transactions"), t);
        ContentValues b = new ContentValues();
        b.put("account_guid", "lager");
        b.put("quantity", -42);
        getMockContentResolver().insert(transaction.buildUpon()
                .appendEncodedPath("products/5").build(), b);
        b = new ContentValues();
        b.put("account_guid", account);
        b.put("quantity", 42);
        getMockContentResolver().insert(transaction.buildUpon()
                .appendEncodedPath("products/2").build(), b);
        return transaction;
    }


}