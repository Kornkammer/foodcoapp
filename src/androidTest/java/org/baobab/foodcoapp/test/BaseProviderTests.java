package org.baobab.foodcoapp.test;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.test.ProviderTestCase2;

import org.baobab.foodcoapp.LedgerProvider;


public class BaseProviderTests extends ProviderTestCase2<LedgerProvider> {

    static {
        LedgerProvider.AUTHORITY = "org.baobab.foodcoapp.test";
    }

    public BaseProviderTests() {
        super(LedgerProvider.class, "org.baobab.foodcoapp.test");
    }

    @NonNull
    public Cursor query(Uri uri, int assert_count) {
        return query(uri, null, null, assert_count);
    }

    @NonNull
    public Cursor query(String path, int assert_count) {
        return query(path, null, null, assert_count);
    }

    @NonNull
    public Cursor query(String p, String selection, int assert_count) {
        return query(Uri.parse("content://org.baobab.foodcoapp.test/" + p), selection, null, assert_count);
    }

    @NonNull
    public Cursor query(String p, String selection, String[] args, int assert_count) {
        return query(Uri.parse("content://org.baobab.foodcoapp.test/" + p), selection, args, assert_count);
    }

    public Cursor query(Uri uri, String selection, String[] args, int assert_count) {
        Cursor transactions = getMockContentResolver().query(uri, null, selection, args, null);
        assertEquals("number of results", assert_count, transactions.getCount());
        transactions.moveToFirst();
        return transactions;
    }

    public Uri createDummyAccount(String name) {
        return createDummyAccount(name, name, "passiva");
    }

    public Uri createDummyAccount(String name, String guid) {
        return createDummyAccount(name, guid, "passiva");
    }

    public Uri createDummyAccount(String name, String guid, String parent) {
        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("guid", guid);
        values.put("parent_guid", parent);
        return getMockContentResolver().insert(
                Uri.parse("content://org.baobab.foodcoapp.test/accounts"), values);
    }

    public Uri insertTransaction(String from_account, String to_account) {
        return insertTransaction(7, "final", from_account, to_account, 42.0f, 1.0f, "sth");
    }

    public Uri insertTransaction(String status, String from_account, String to_account) {
        return insertTransaction(7, status, from_account, to_account, 42.0f, 1.0f, "sth");
    }

    public Uri insertTransaction(String from_account, String to_account, float price, String title) {
        return insertTransaction(7, "final", from_account, to_account, 1.0f, price, title);
    }

    public Uri insertTransaction(int sessionId, String status, String from_account, String to_account, float amount) {
        return insertTransaction(sessionId, status, from_account, to_account, amount, 1.0F, "sth");
    }

    public Uri insertTransaction(int sessionId, String status, String from_account, String to_account, float amount, float price, String title) {
        ContentValues t = new ContentValues();
        t.put("session_id", sessionId);
        t.put("status", status);
        t.put("start", System.currentTimeMillis());
        Uri transaction = getMockContentResolver().insert(Uri.parse(
                "content://org.baobab.foodcoapp.test/transactions"), t);
        ContentValues b = new ContentValues();
        b.put("account_guid", from_account);
        b.put("product_id", 23);
        b.put("quantity", -amount);
        b.put("title", title);
        b.put("price", price);
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

    public int finalizeSession(long id) {
        return finalizeSession(Uri.parse(
                "content://org.baobab.foodcoapp.test/sessions/" + id));
    }

    public int finalizeSession(Uri uri) {
        ContentValues cv = new ContentValues();
        cv.put("status", "final");
        return getMockContentResolver().update(uri, cv, null, null);
    }
}