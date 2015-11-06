package org.baobab.foodcoapp.test;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;


public class TransactionProductsTests extends BaseProviderTests {

    public void testIncAmount() {
        createDummyAccount("dummy");
        Uri transaction = insertTransaction("dummy", "kasse");
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

    public void testDecAmount() {
        createDummyAccount("dummy");
        Uri transaction = insertTransaction("lager", "dummy");
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

    public void testRemoveAmount() {
        createDummyAccount("dummy");
        Uri transaction = insertTransaction("lager", "dummy");
        getMockContentResolver().delete(transaction.buildUpon()
                .appendEncodedPath("accounts/lager/products/23").build(), "nix null", null);
        query(transaction, 1);
    }

    public void testEditAmount() {
        createDummyAccount("dummy");
        Uri transaction = insertTransaction("dummy", "kasse");
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

    public void testAccountsOrder() {
        createDummyAccount("dummy");
        Uri transaction = insertTransaction("lager", "kasse");
        Cursor products = query(transaction, 2);
        assertEquals("lager", products.getString(11));
        transaction = insertTransaction("lager", "dummy");
        products = query(transaction, 2);
        assertEquals("lager", products.getString(11));
    }

    public void testInsertCashToRebalance() {
        createDummyAccount("dummy");
        Uri transaction = insertTransaction("dummy", "kasse");
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