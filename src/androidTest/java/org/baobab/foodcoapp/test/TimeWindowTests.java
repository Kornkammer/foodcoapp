package org.baobab.foodcoapp.test;

import android.database.Cursor;


public class TimeWindowTests extends BaseProviderTests {

    private long year1;
    private long year2;
    private long year3;
    private long year4;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        createDummyAccount("members");
        year1 = System.currentTimeMillis();
        createDummyAccount("a", "a", "members", "foo", year1, year1, 9);
        createDummyAccount("b", "b", "members", "foo", year1, year1, 9);
        insertTransaction("kasse", "a");
        insertTransaction("kasse", "b");
        insertTransaction("b", "kasse");
        insertTransaction("kasse", "b");
        Cursor a = query("accounts/members/accounts", 2);
        assertEquals("balance", 42.0, a.getDouble(3));

        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        year2 = System.currentTimeMillis();
        insertTransaction("kasse", "a");
        insertTransaction("a", "kasse");
        insertTransaction("kasse", "a");
        a = query("accounts/members/accounts", 2);
        assertEquals("balance", 84.0, a.getDouble(3));
        createDummyAccount("c", "c", "members", "foo", year2, year2, 9);
        insertTransaction("kasse", "c");
        createDummyAccount("a", "a", "members", "deleted", year1, year2, 9);
        query("accounts/members/accounts", 2);

        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        year3 = System.currentTimeMillis();
        insertTransaction("kasse", "c");
        insertTransaction("b", "kasse");

        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        year4 = System.currentTimeMillis();
    }

    public void testBalance() {
        Cursor a = query("accounts/members/accounts?after=" + year1 + "&before=" + year2, 2);
        assertEquals("balance b", 42.0, a.getDouble(3));
        a.moveToNext();
        assertEquals("balance a", 42.0, a.getDouble(3));
        a = query("accounts/members/accounts?before=" + year2, 2);
        assertEquals("balance b", 42.0, a.getDouble(3));
        a.moveToNext();
        assertEquals("balance a", 42.0, a.getDouble(3));

        a = query("accounts/members/accounts?after=" + year2 + "&before=" + year3, 3);
        assertEquals("balance b", 0.0, a.getDouble(3));
        a.moveToLast();
        assertEquals("balance a", 42.0, a.getDouble(3));

        a = query("accounts/members/accounts?after=" + year3 + "&before=" + year4, 2);
        assertEquals("balance b", -42.0, a.getDouble(3));
        a.moveToLast();
        assertEquals("balance c", 42.0, a.getDouble(3));

        a = query("accounts/members/accounts?after=" + year1 + "&before=" + year3, 3);
        a.moveToLast();
        assertEquals("balance a", 84.0, a.getDouble(3));
        a = query("accounts/members/accounts?before=" + year3, 3);
        a.moveToLast();
        assertEquals("balance a", 84.0, a.getDouble(3));

        a = query("accounts/members/accounts?after=" + year2 + "&before=" + year4, 3);
        a.moveToNext();
        assertEquals("balance c", 84.0, a.getDouble(3));
        a.moveToLast();
        assertEquals("balance a", 42.0, a.getDouble(3));
        a = query("accounts/members/accounts?after=" + year2, 3);
        a.moveToNext();
        assertEquals("balance c", 84.0, a.getDouble(3));
        a.moveToLast();
        assertEquals("balance a", 42.0, a.getDouble(3));
    }

    public void testStandardAccounts() {
        assertKasse("accounts/aktiva/accounts?debit=true&after=" + year1 + "&before=" + year2, 42.0);
        assertKasse("accounts/aktiva/accounts?credit=true&after=" + year1 + "&before=" + year2, -126.0);
        assertKasse("accounts/aktiva/accounts?after=" + year1 + "&before=" + year2, -84.0);
        assertKasse("accounts/aktiva/accounts?debit=true&after=" + year1 + "&before=" + year2, 42.0);
        assertKasse("accounts/aktiva/accounts?credit=true&after=" + year1 + "&before=" + year2, -126.0);
        assertKasse("accounts/aktiva/accounts?before=" + year2, -84.0);
        assertKasse("accounts/aktiva/accounts?after=" + year2 + "&before=" + year3, -84.0);
        assertKasse("accounts/aktiva/accounts?after=" + year1 + "&before=" + year3, -168.0);
        assertKasse("accounts/aktiva/accounts?before=" + year3, -168.0);
        assertKasse("accounts/aktiva/accounts?debit=true&before=" + year3, 84.0);
        assertKasse("accounts/aktiva/accounts?credit==true&before=" + year3, -252.0);
        assertKasse("accounts/aktiva/accounts?after=" + year3 + "&before=" + year4, 0);
        assertKasse("accounts/aktiva/accounts?debit=true&after=" + year3 + "&before=" + year4, 42.0);
        assertKasse("accounts/aktiva/accounts?credit=true&after=" + year3 + "&before=" + year4, -42.0);
        assertKasse("accounts/aktiva/accounts?after=" + year2 + "&before=" + year4, -84.0);
    }

    private void assertKasse(String uri, double balance) {
        Cursor a = query(uri, 6);
        a.moveToPosition(4);
        assertEquals("Kasse", a.getString(1));
        assertEquals("balance Kasse", balance, a.getDouble(3));
    }

    public void testDebitCredit() {
        Cursor a = query("accounts/members/accounts?debit=true&after=" + year1 + "&before=" + year2, 2);
        assertEquals("debit a", 42.0, a.getDouble(3));
        a.moveToNext();
        assertEquals("debit b", 84.0, a.getDouble(3));
        a = query("accounts/members/accounts?credit=true&after=" + year1 + "&before=" + year2, 2);
        assertEquals("credit a", 0.0, a.getDouble(3));
        a.moveToNext();
        assertEquals("b", a.getString(1));
        assertEquals("credit b", -42.0, a.getDouble(3));

        a = query("accounts/members/accounts?debit=true&after=" + year2 + "&before=" + year3, 3);
        assertEquals("debit b", 0.0, a.getDouble(3));
        a.moveToLast();
        assertEquals("debit a", 84.0, a.getDouble(3));
        a = query("accounts/members/accounts?credit=true&after=" + year2 + "&before=" + year3, 3);
        assertEquals("credit b", 0.0, a.getDouble(3));
        a.moveToLast();
        assertEquals("credit a", -42.0, a.getDouble(3));

        a = query("accounts/members/accounts?credit=true&after=" + year3 + "&before=" + year4, 2);
        assertEquals("credit b", -42.0, a.getDouble(3));
        a.moveToLast();
        assertEquals("credit c", 0.0, a.getDouble(3));

        a = query("accounts/members/accounts?debit=true&after=" + year1 + "&before=" + year3, 3);
        a.moveToLast();
        assertEquals("debit a", 126.0, a.getDouble(3));

        a = query("accounts/members/accounts?credit=true&after=" + year2 + "&before=" + year4, 3);
        assertEquals("credit b", -42.0, a.getDouble(3));
        a.moveToNext();
        assertEquals("credit c", 0.0, a.getDouble(3));
        a.moveToLast();
        assertEquals("credit a", -42.0, a.getDouble(3));
    }

    public void testTransactions() {
        assertTxns("accounts/kasse/transactions", 10, -168);

        assertTxns("accounts/kasse/transactions?before=" + year2, 4, -84);
        assertTxns("accounts/a/transactions?before=" + year2, 1, 42);
        assertTxns("accounts/b/transactions?before=" + year2, 3, 42);
        assertTxns("accounts/a/transactions?debit=true&before=" + year2, 1, 42);
        assertTxns("accounts/b/transactions?credit=true&before=" + year2, 1, -42);
        assertTxns("accounts/b/transactions?debit=true&before=" + year2, 2, 84);

        assertTxns("accounts/kasse/transactions?after=" + year1 + "&before=" + year2, 4, -84);
        assertTxns("accounts/a/transactions?after=" + year1 + "&before=" + year2, 1, 42);
        assertTxns("accounts/b/transactions?after=" + year1 + "&before=" + year2, 3, 42);
        assertTxns("accounts/a/transactions?debit=true&after=" + year1 + "&before=" + year2, 1, 42);
        assertTxns("accounts/b/transactions?credit=true&after=" + year1 + "&before=" + year2, 1, -42);
        assertTxns("accounts/b/transactions?debit=true&after=" + year1 + "&before=" + year2, 2, 84);

        assertTxns("accounts/kasse/transactions?after=" + year2 + "&before=" + year3, 4, -84);
        assertTxns("accounts/a/transactions?after=" + year2 + "&before=" + year3, 3, 42);
        assertTxns("accounts/a/transactions?debit=true&after=" + year2 + "&before=" + year3, 2, 84);
        assertTxns("accounts/a/transactions?credit=true&after=" + year2 + "&before=" + year3, 1, -42);
        assertTxns("accounts/c/transactions?after=" + year2 + "&before=" + year3, 1, 42);
        assertTxns("accounts/c/transactions?debit=true&after=" + year2 + "&before=" + year3, 1, 42);

        assertTxns("accounts/kasse/transactions?after=" + year1 + "&before=" + year3, 8, -168);
        assertTxns("accounts/a/transactions?after=" + year1 + "&before=" + year3, 4, 84);
        assertTxns("accounts/a/transactions?before=" + year3, 4, 84);
        assertTxns("accounts/a/transactions?after=" + year1 + "&before=" + year3, 4, 84);
        assertTxns("accounts/a/transactions?after=" + year2 + "&before=" + year4, 3, 42);
        assertTxns("accounts/c/transactions?after=" + year2 + "&before=" + year4, 2, 84);
    }

    private void assertTxns(String uri, int count, float sum) {
        Cursor t = query(uri, count);
        float s = t.getFloat(6);
        while (t.moveToNext()) {
            s += t.getFloat(6);
        }
        assertEquals(uri + " txn sum", sum, s);
    }
}