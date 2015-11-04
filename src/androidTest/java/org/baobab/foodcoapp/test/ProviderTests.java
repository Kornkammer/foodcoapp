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
        b.put("quantity", 42);
        getMockContentResolver().insert(transaction.buildUpon()
                .appendEncodedPath("products/2").build(), b);
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

    public void testKontoauszug() {
        createDummyAccount("dummy");
        createDummyAccount("another");
        insertTransaction("final", "lager", "dummy");
        insertTransaction("final", "lager", "another");
        Cursor transactions = query("accounts/dummy/transactions", 1);
        assertEquals("time", 1, transactions.getLong(2));
        assertEquals("who", "dummy", transactions.getString(3));
        assertEquals("sum", 42.0, transactions.getDouble(6));
        assertEquals("passiva", "passiva", transactions.getString(9));
        assertEquals("Bilanzerniedrigung", true, transactions.getInt(8) < 0);
        assertEquals("involved accounts", "dummy,lager", transactions.getString(5));
    }

    @NonNull
    private Cursor query(String path, int assert_count) {
        Cursor transactions = getMockContentResolver().query(Uri.parse(
                "content://org.baobab.foodcoapp.test/" + path), null, null, null, null);
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
        b.put("quantity", -42);
        getMockContentResolver().insert(transaction.buildUpon()
                .appendEncodedPath("products/5").build(), b);
        b = new ContentValues();
        b.put("account_guid", to_account);
        b.put("quantity", 42);
        getMockContentResolver().insert(transaction.buildUpon()
                .appendEncodedPath("products/2").build(), b);
        return transaction;
    }


}