package org.baobab.foodcoapp.test;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;


public class AccountsTests extends BaseProviderTests {

    public void testAccountBalance() {
        createDummyAccount("dummy");
        createDummyAccount("sub", "sub", "dummy");
        createDummyAccount("sub2", "sub2", "dummy");
        insertTransaction("kasse", "dummy");
        insertTransaction("kasse", "dummy");
        insertTransaction("kasse", "sub");
        insertTransaction("kasse", "sub");
        insertTransaction("kasse", "sub2");
        Cursor accounts = query("accounts/passiva/accounts", 6);
        accounts.moveToPosition(5);
        assertEquals("name", "dummy", accounts.getString(1));
        assertEquals("balance", 5 * 42.0, accounts.getDouble(3));
        accounts = query("accounts/dummy/accounts", 2);
        assertEquals("name", "sub", accounts.getString(1));
        assertEquals("balance", 84.0, accounts.getDouble(3));
        assertEquals("parent", "dummy", accounts.getString(4));
    }

    public void testAccountDebit() {
        createDummyAccount("dummy");
        createDummyAccount("sub", "sub", "dummy");
        createDummyAccount("sub2", "sub2", "dummy");
        insertTransaction("kasse", "dummy");
        insertTransaction("dummy", "kasse");
        insertTransaction("kasse", "sub");
        insertInventurTransaction("kasse", "kasse");
        insertInventurTransaction("sub", "sub");
        insertInventurTransaction("sub", "sub");
        insertTransaction("sub", "kasse");
        insertTransaction("kasse", "sub2");
        Cursor accounts = query("accounts?credit=true", 16);
        accounts.moveToPosition(13);
        assertEquals("name", "dummy", accounts.getString(1));
        assertEquals("balance", -84.0, accounts.getDouble(3));
        accounts.moveToPosition(6);
        assertEquals("name", "Kasse", accounts.getString(1));
        assertEquals("balance", 3 * -42.0, accounts.getDouble(3));
        accounts = query("accounts?debit=true", 16);
        accounts.moveToPosition(13);
        assertEquals("name", "dummy", accounts.getString(1));
        assertEquals("balance", 3 * 42.0, accounts.getDouble(3));
        accounts.moveToPosition(6);
        assertEquals("name", "Kasse", accounts.getString(1));
        assertEquals("balance", 2 * 42.0, accounts.getDouble(3));
        accounts = query("accounts/passiva/accounts?credit=true", 6);
        accounts.moveToPosition(5);
        assertEquals("name", "dummy", accounts.getString(1));
        assertEquals("balance", -84.0, accounts.getDouble(3));
        accounts = query("accounts/passiva/accounts?debit=true", 6);
        accounts.moveToPosition(5);
        assertEquals("name", "dummy", accounts.getString(1));
        assertEquals("balance", 3 * 42.0, accounts.getDouble(3));
        accounts = query("accounts/dummy/accounts?credit=true", 2);
        assertEquals("name", "sub", accounts.getString(1));
        assertEquals("balance", -42.0, accounts.getDouble(3));
        accounts = query("accounts/dummy/accounts?debit=true", 2);
        assertEquals("name", "sub", accounts.getString(1));
        assertEquals("balance", 42.0, accounts.getDouble(3));
        accounts = query("accounts/aktiva/accounts?debit=true", 6);
        accounts.moveToPosition(4);
        assertEquals("name", "Kasse", accounts.getString(1));
        assertEquals("balance", 2 * 42.0, accounts.getDouble(3));
        accounts = query("accounts/aktiva/accounts?credit=true", 6);
        accounts.moveToPosition(4);
        assertEquals("name", "Kasse", accounts.getString(1));
        assertEquals("balance", - 3 * 42.0, accounts.getDouble(3));
    }

    public void testInventur() {
        createDummyAccount("dummy");
        createDummyAccount("spenden");
        insertTransaction("kasse", "lager"); // 3 x einlagern
        insertTransaction("kasse", "lager");
        insertTransaction("kasse", "lager");
        insertTransaction("dummy", "kasse"); // 2 x einzahlen
        insertTransaction("dummy", "kasse");
        insertTransaction("lager", "dummy"); // 1 x einkaufen
        insertInventurTransaction("lager", "lager"); // inventur
        insertInventurTransaction("lager", "lager"); // inventur
        insertInventurTransaction("lager", "lager"); // inventur
        insertTransaction("spenden", "lager"); // inventur
        Cursor accounts = query("accounts?credit=true", 14);
        accounts.moveToPosition(4);
        assertEquals("name", "Lager", accounts.getString(1));
        assertEquals("balance", -42.0, accounts.getDouble(3)); // 1 x einkaufen
        accounts.moveToPosition(6);
        assertEquals("name", "Kasse", accounts.getString(1));
        assertEquals("balance", 3 * -42.0, accounts.getDouble(3)); // 3 x einlagern
        accounts.moveToPosition(12);
        assertEquals("name", "dummy", accounts.getString(1));
        assertEquals("balance", 2 * -42.0, accounts.getDouble(3)); // 2 x einzahlen
        accounts.moveToPosition(13);
        assertEquals("name", "spenden", accounts.getString(1));
        assertEquals("balance", -42.0, accounts.getDouble(3));

        accounts = query("accounts?debit=true", 14);
        accounts.moveToPosition(4);
        assertEquals("name", "Lager", accounts.getString(1));
        assertEquals("balance", 4 * 42.0, accounts.getDouble(3)); // 3 x einlagern + 1 x spende
        accounts.moveToPosition(6);
        assertEquals("name", "Kasse", accounts.getString(1));
        assertEquals("balance", 2 * 42.0, accounts.getDouble(3)); // 2 x einzahlen
        accounts.moveToPosition(12);
        assertEquals("name", "dummy", accounts.getString(1));
        assertEquals("balance", 1 * 42.0, accounts.getDouble(3)); // 1 x einkaufen
        accounts.moveToPosition(13);
        assertEquals("name", "spenden", accounts.getString(1));
        assertEquals("balance", 0.0, accounts.getDouble(3));

        accounts = query("accounts/passiva/accounts?credit=true", 6);
        accounts.moveToPosition(4);
        assertEquals("name", "dummy", accounts.getString(1));
        assertEquals("balance", 2 * -42.0, accounts.getDouble(3)); // 2 x einzahlen
        accounts.moveToNext();
        assertEquals("name", "spenden", accounts.getString(1));
        assertEquals("balance", 1 * -42.0, accounts.getDouble(3)); // 1 x einkaufen

        accounts = query("accounts/aktiva/accounts?credit=true", 6);
        accounts.moveToPosition(2);
        assertEquals("name", "Lager", accounts.getString(1));
        assertEquals("balance", 1 * -42.0, accounts.getDouble(3)); // 1 x einkaufen
        accounts.moveToPosition(4);
        assertEquals("name", "Kasse", accounts.getString(1));
        assertEquals("balance", 3 * -42.0, accounts.getDouble(3)); // 3 x einlagern

        accounts = query("accounts/passiva/accounts?debit=true", 6);
        accounts.moveToPosition(4);
        assertEquals("name", "dummy", accounts.getString(1));
        assertEquals("balance", 1 * 42.0, accounts.getDouble(3)); // 1 x einkaufen
        accounts.moveToNext();
        assertEquals("name", "spenden", accounts.getString(1));
        assertEquals("balance", 0.0, accounts.getDouble(3));

        accounts = query("accounts/aktiva/accounts?debit=true", 6);
        accounts.moveToPosition(2);
        assertEquals("name", "Lager", accounts.getString(1));
        assertEquals("balance", 4 * 42.0, accounts.getDouble(3)); // 3 x einlagern + 1 x spende
        accounts.moveToPosition(4);
        assertEquals("name", "Kasse", accounts.getString(1));
        assertEquals("balance", 2 * 42.0, accounts.getDouble(3)); // 2 x einzahlen
    }

    public void testEmptyAccount() {
        createDummyAccount("dummy");
        query("accounts/passiva/accounts", 6);
        Uri transaction = insertTransaction("draft", "lager", "dummy");
        ContentValues b = new ContentValues();
        b.put("account_guid", "dummy");
        b.put("unit", "zeug");
        b.put("quantity", 42);
        getMockContentResolver().insert(transaction.buildUpon()
                .appendEncodedPath("products").build(), b);
        query("accounts/passiva/accounts", 6);
    }

    public void testListAccounts() {
        query("accounts", 13);
        createDummyAccount("dummy");
        query("accounts", 14);
        query("accounts/aktiva/accounts", 6);
        query("accounts/passiva/accounts", 6);
    }

    public void testFindAccountByName() {
        createDummyAccount("dummy");
        createDummyAccount("another");
        query("accounts", "guid IS ?", new String[] {"dummy"}, 1);
        query("accounts/passiva/accounts", "name IS ?", new String[] {"foo"}, 0);
        query("accounts/passiva/accounts", "name IS ?", new String[] {"dummy"}, 1);
    }

    public void testAccountProducts() {
        createDummyAccount("dummy");
        insertTransaction(7, "final", "dummy", "lager", "Zeugs", 1f);
        insertTransaction(7, "final", "dummy", "lager", "Zeugs", 1f);
        Cursor c = query("accounts/lager/products", 1);
        insertTransaction(7, "final", "lager", "dummy", "Zeugs", 2f);
        query("accounts/lager/products", 0);
    }
}