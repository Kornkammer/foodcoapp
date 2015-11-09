package org.baobab.foodcoapp.test;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;


public class AccountsTests extends BaseProviderTests {

    public void testAccountBalance() {
        createDummyAccount("dummy");
        insertTransaction("kasse", "dummy");
        Cursor accounts = query("accounts/passiva/accounts", 5);
        accounts.moveToPosition(4);
        assertEquals("name", "dummy", accounts.getString(1));
        assertEquals("balance", 42.0, accounts.getDouble(4));
    }

    public void testEmptyAccount() {
        createDummyAccount("dummy");
        query("accounts/passiva/accounts", 5);
        Uri transaction = insertTransaction("draft", "lager", "dummy");
        ContentValues b = new ContentValues();
        b.put("account_guid", "dummy");
        b.put("unit", "zeug");
        b.put("quantity", 42);
        getMockContentResolver().insert(transaction.buildUpon()
                .appendEncodedPath("products").build(), b);
        query("accounts/passiva/accounts", 5);
    }

    public void testListAccounts() {
        query("accounts", 12);
        createDummyAccount("dummy");
        query("accounts", 13);
        query("accounts/aktiva/accounts", 6);
        query("accounts/passiva/accounts", 5);
    }

    public void testFindAccountByName() {
        createDummyAccount("dummy");
        createDummyAccount("another");
        query("accounts", "guid IS ?", new String[] {"dummy"}, 1);
        query("accounts/passiva/accounts", "name IS ?", new String[] {"foo"}, 0);
        query("accounts/passiva/accounts", "name IS ?", new String[] {"dummy"}, 1);
    }

}