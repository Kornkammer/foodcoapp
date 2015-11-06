package org.baobab.foodcoapp.test;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.test.ProviderTestCase2;

import org.baobab.foodcoapp.AccountingProvider;


public class BaseProviderTests extends ProviderTestCase2<AccountingProvider> {

    static {
        AccountingProvider.AUTHORITY = "org.baobab.foodcoapp.test";
    }

    public BaseProviderTests() {
        super(AccountingProvider.class, "org.baobab.foodcoapp.test");
    }

    @NonNull
    public Cursor query(String path, int assert_count) {
        return query(Uri.parse("content://org.baobab.foodcoapp.test/" + path), assert_count);
    }

    @NonNull
    public Cursor query(Uri uri, int assert_count) {
        Cursor transactions = getMockContentResolver().query(uri, null, null, null, null);
        assertEquals("number of results", assert_count, transactions.getCount());
        transactions.moveToFirst();
        return transactions;
    }

    public Uri createDummyAccount(String name) {
        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("guid", name);
        values.put("parent_guid", "passiva");
        return getMockContentResolver().insert(
                Uri.parse("content://org.baobab.foodcoapp.test/accounts/passiva/accounts"), values);
    }

    public Uri insertTransaction(String status, String from_account, String to_account) {
        return insertTransaction(1, status, from_account, to_account, 42.0f);
    }

    public Uri insertTransaction(String status, String from_account, String to_account, float amount) {
        return insertTransaction(1, status, from_account, to_account, amount);
    }

    public Uri insertTransaction(int sessionId, String status, String from_account, String to_account, float amount) {
        ContentValues t = new ContentValues();
        t.put("status", status);
        t.put("stop", 1);
        Uri transaction = getMockContentResolver().insert(Uri.parse(
                "content://org.baobab.foodcoapp.test/transactions"), t);
        ContentValues b = new ContentValues();
        b.put("account_guid", from_account);
        b.put("product_id", 23);
        b.put("quantity", -amount);
        b.put("price", 1.0);
        b.put("unit", "dinge");
        getMockContentResolver().insert(transaction.buildUpon()
                .appendEncodedPath("products").build(), b);
        b.put("account_guid", to_account);
        b.put("quantity", amount);
        b.put("unit", "sachen");
        getMockContentResolver().insert(transaction.buildUpon()
                .appendEncodedPath("products").build(), b);
        return transaction;
    }


}