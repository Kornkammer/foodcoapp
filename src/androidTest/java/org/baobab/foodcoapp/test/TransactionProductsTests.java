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
        b.put("title", "A product");
        b.put("unit", "piece");
        b.put("price", 50);
        getMockContentResolver().insert(transaction.buildUpon()
                .appendEncodedPath("products").build(), b);
        Cursor products = query(transaction
                .buildUpon().appendPath("products").build(), 3);
        assertEquals("lager", products.getString(11));
        assertEquals("quantity", -1.0, products.getDouble(4));
        // press button again
        getMockContentResolver().insert(transaction.buildUpon()
                .appendEncodedPath("products").build(), b);
        products = query(transaction
                .buildUpon().appendPath("products").build(), 3);
        assertEquals("quantity", -2.0, products.getDouble(4));
        // press button again
        b.put("price", 1000);
        getMockContentResolver().insert(transaction.buildUpon()
                .appendEncodedPath("products").build(), b);
        products = query(transaction
                .buildUpon().appendPath("products").build(), 4);
        assertEquals("quantity", -2.0, products.getDouble(4));
        assertEquals("price", 50.0, products.getDouble(5));
        products.moveToNext();
        assertEquals("quantity", -1.0, products.getDouble(4));
        assertEquals("price", 1000.0, products.getDouble(5));
    }

    public void testDecAmount() {
        createDummyAccount("dummy");
        Uri transaction = insertTransaction("lager", "dummy");
        getMockContentResolver().delete(transaction.buildUpon()
                .appendEncodedPath("accounts/lager/products/1").build(), null, null);
        Cursor products = query(transaction
                .buildUpon().appendPath("products").build(), 2);
        assertEquals("quantity", -41.0, products.getDouble(4));
        getMockContentResolver().delete(transaction.buildUpon()
                .appendEncodedPath("accounts/dummy/products/2").build(), null, null);
        products = query(transaction
                .buildUpon().appendPath("products").build(), 2);
        products.moveToLast();
        assertEquals("quantity", 41.0, products.getDouble(4));
    }

    public void testRemoveAmount() {
        createDummyAccount("dummy");
        Uri transaction = insertTransaction("lager", "dummy");
        getMockContentResolver().delete(transaction.buildUpon()
                .appendEncodedPath("accounts/lager/products/1").build(), "nix null", null);
        query(transaction.buildUpon().appendPath("products").build(), 1);
    }

    public void testEditAmount() {
        createDummyAccount("dummy");
        Uri transaction = insertTransaction("dummy", "kasse");
        ContentValues b = new ContentValues();
        b.put("account_guid", "lager");
        b.put("product_id", 55);
        b.put("title", "A product");
        b.put("unit", "piece");
        b.put("quantity", -4.0);
        getMockContentResolver().insert(transaction.buildUpon()
                .appendEncodedPath("products").build(), b);
        Cursor products = query(transaction
                .buildUpon().appendPath("products").build(), 3);
        assertEquals("lager", products.getString(11));
        assertEquals("quantity", -4.0, products.getDouble(4));
        // same product in another account
        b.put("account_guid", "inventar");
        getMockContentResolver().insert(transaction.buildUpon()
                .appendEncodedPath("products").build(), b);
        products = query(transaction
                .buildUpon().appendPath("products").build(), 4);
        assertEquals("quantity", -4.0, products.getDouble(4));
        // same product but other title
        b.put("quantity", 20);
        b.put("title", "B Product");
        getMockContentResolver().insert(transaction.buildUpon()
                .appendEncodedPath("products").build(), b);
        products = query(transaction
                .buildUpon().appendPath("products").build(), 5);
        assertEquals("quantity", 20.0, products.getDouble(4));
        products.moveToNext();
        assertEquals("quantity", -4.0, products.getDouble(4));
    }

    public void testAccountsOrder() {
        createDummyAccount("dummy");
        Uri transaction = insertTransaction("lager", "kasse");
        Cursor products = query(transaction
                .buildUpon().appendPath("products").build(), 2);
        assertEquals("lager", products.getString(11));
        transaction = insertTransaction("lager", "dummy");
        products = query(transaction
                .buildUpon().appendPath("products").build(), 2);
        assertEquals("lager", products.getString(11));
    }

    public void testInsertCashToRebalance() {
        createDummyAccount("dummy");
        Uri transaction = insertTransaction("dummy", "kasse");
        ContentValues b = new ContentValues();
        b.put("account_guid", "lager");
        b.put("product_id", 2);
        b.put("title", "Cash");
        b.put("price", 1);
        b.put("quantity", -5.5);
        getMockContentResolver().insert(transaction.buildUpon()
                .appendEncodedPath("products").build(), b);
        Cursor products = query(transaction
                .buildUpon().appendPath("products").build(), 3);
        assertEquals("lager", products.getString(11));
        assertEquals("quantity", -5.5, products.getDouble(4));

        b.put("quantity", -1.5);
        getMockContentResolver().insert(transaction.buildUpon()
                .appendEncodedPath("products").build(), b);
        products = query(transaction
                .buildUpon().appendPath("products").build(), 3);
        assertEquals("lager", products.getString(11));
        assertEquals("quantity", -7.0, products.getDouble(4));
    }
}