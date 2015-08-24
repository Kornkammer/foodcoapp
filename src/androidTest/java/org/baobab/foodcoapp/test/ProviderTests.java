package org.baobab.foodcoapp.test;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.test.ProviderTestCase2;

import org.baobab.foodcoapp.AccountingProvider;


public class ProviderTests extends ProviderTestCase2<AccountingProvider> {

    public ProviderTests() {
        super(AccountingProvider.class, "org.baobab.foodcoapp");
    }

    public void testAccountBalance() {
        createDummyAccount();
        insertTransaction("final");
        Cursor accounts = getMockContentResolver().query(Uri.parse(
                "content://org.baobab.foodcoapp/accounts/passiva/accounts"), null, null, null, null);
        accounts.moveToFirst();
        assertEquals("name", "Dummy", accounts.getString(1));
        assertEquals("balance", -42.0, accounts.getDouble(4));
    }

    public void testEmptyAccount() {
        createDummyAccount();
        Cursor accounts = getMockContentResolver().query(Uri.parse(
                "content://org.baobab.foodcoapp/accounts/passiva/accounts"), null, null, null, null);
        assertEquals("one account", 1, accounts.getCount());
        Uri transaction = insertTransaction("draft");
        ContentValues b = new ContentValues();
        b.put("account_guid", "dummid");
        b.put("quantity", 42);
        getMockContentResolver().insert(transaction.buildUpon()
                        .appendEncodedPath("products/2").build(), b);
        accounts = getMockContentResolver().query(Uri.parse(
                "content://org.baobab.foodcoapp/accounts/passiva/accounts"), null, null, null, null);
        assertEquals("one account", 1, accounts.getCount());
    }

    public void testFindAccountByName() {
        createDummyAccount();
        Cursor accounts = getMockContentResolver().query(Uri.parse(
                "content://org.baobab.foodcoapp/accounts/passiva/accounts"), null, "name IS 'foo'", null, null);
        assertEquals("no account", 0, accounts.getCount());
        accounts = getMockContentResolver().query(Uri.parse(
                "content://org.baobab.foodcoapp/accounts/passiva/accounts"), null, "name IS 'Dummy'", null, null);
        assertEquals("one account", 1, accounts.getCount());
    }

    private Uri createDummyAccount() {
        ContentValues values = new ContentValues();
        values.put("name", "Dummy");
        values.put("guid", "dummid");
        values.put("parent_guid", "passiva");
        return getMockContentResolver().insert(
                Uri.parse("content://org.baobab.foodcoapp/accounts/passiva/accounts"), values);
    }

    private Uri insertTransaction(String status) {
        ContentValues t = new ContentValues();
        t.put("status", status);
        Uri transaction = getMockContentResolver().insert(Uri.parse(
                "content://org.baobab.foodcoapp/transactions"), t);
        ContentValues b = new ContentValues();
        b.put("account_guid", "dummid");
        b.put("quantity", 42);
        getMockContentResolver().insert(transaction.buildUpon()
                .appendEncodedPath("products/2").build(), b);
        return transaction;
    }


}