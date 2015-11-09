package org.baobab.foodcoapp.test;


import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.test.mock.MockContext;

import org.baobab.foodcoapp.GlsImport;

import java.text.NumberFormat;
import java.util.Date;


public class GlsImportTest extends BaseProviderTests {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        createDummyAccount("Susi", "0815");
        createDummyAccount("Albert", "1234");
    }

    public void testEinlage() {
        assertEinlage("Einlage Susi");
        assertEinlage("0815 Einlage");
        assertEinlage("Einlage-susi");
        assertEinlage("Einlage: Susi");
        assertEinlage("Einlage - Susi");
        assertEinlage("einlage Susi");
        assertEinlage("einlage-Susi");
        assertEinlage("einlage: Susi");
        assertEinlage("einlage - SUSI");
        assertEinlage("Einlage 0815");
        assertEinlage("Einlage-0815");
        assertEinlage("Einlage: 0815");
        assertEinlage("Einlage - 0815");
        assertEinlage("einlage 0815");
        assertEinlage("einlage-0815");
        assertEinlage("einlage: 0815");
        assertEinlage("einlage - 0815");
        assertEinlage("Einlage-Susi-0815");
        assertEinlage("Einlage: Susi-0815, 0815");
        assertEinlage("einlage - Susi - 0815 0815");
        assertEinlage("Einlage-0815-Susi");
        assertEinlage("Einlage: 0815-Susi");
        assertEinlage("einlage - - 0815 : Susi");
        assertEinlage("Einlage Susi-0815 und noch liebe Grüße");
        assertVerbindlichkeit("Einlage Xaver", "Unbekanntes Mitglied");
        assertVerbindlichkeit("Einlage 76345", "Unbekanntes Mitglied");
        assertEinlage("Einlage-Susi-0827"); // falsche MitgliedsNr
        assertEinlage("Einlage Susi 0827"); // falsche MitgliedsNr
        assertEinlage("Einlage: 0827 - Susi"); // falsche MitgliedsNr
        assertEinlage("Einlage-Susssi-0815"); // falscher Mitgliedsname
        assertEinlage("Einlage - 0815 Susssli"); // falscher Mitgliedsname
        assertEinlage("Einlage: 0815 : Susssli"); // falscher Mitgliedsname
        assertVerbindlichkeit("MaWoasEsNed: DaSchaugHer", "Unbekanntes Mitglied");
    }

    public void testBeitrag() {
        assertTrägtBei("0915 Mitgliederbeitrag 1.");
        assertTrägtBei("Mitgliedsbeitrag Susi");
        assertTrägtBei("Mitgliedsbeitrag:Susi");
        assertTrägtBei("Mitgliedsbeitrag-Susi");
        assertTrägtBei("Mitgliedsbeitrag-0815");
        assertTrägtBei("Mitgliedsbeitrag: Susi");
        assertTrägtBei("Mitgliedsbeitrag : Susi");
        assertTrägtBei("Mitgliedsbeitrag : 0815");
        assertTrägtBei("Mitgliedsbeitrag - Susi");
        assertTrägtBei("Mitgliedsbeitrag-Susi: 0815");
        assertTrägtBei("Mitgliedsbeitrag - 0815: Susi");
        assertTrägtBei("Beitrag Susi");
        assertTrägtBei("Beitrag:Susi");
        assertTrägtBei("Beitrag: Susi");
        assertTrägtBei("Beitrag : Susi");
        assertTrägtBei("Beitrag - Susi");
        assertVerbindlichkeit("Guthaben 7345", "Unbekanntes Mitglied");
    }

    public void testEinzahlung() {
        assertZahltEin("Einzahlung 0734, Susi");
        assertZahltEin("einzahlung mitgl.nr 0815,");
        assertVerbindlichkeit("40 euro, 0815, susi", "Susi", "40 euro, 0815, susi"); // kein Schlüsselwort
        assertZahltEin("Einzahlung - Mitgliedsnumme", "r 0815, Account Susi"); // split in zwei VWZ
        assertZahltEin("1. Einzahlung, 0815, sus", "i bla bla"); // split in zwei VWZ
        assertZahltEin("EINZAHLUNG, 0815, SUSI");
        assertZahltEin("EINZAHLUNG, 0815");
        assertZahltEin("Prepaid Mitgliedsnr 0815");
        assertZahltEin("Einzahlung Susi");
        assertZahltEin("Einzahlung:Susi");
        assertZahltEin("Einzahlung:0815");
        assertZahltEin("Einzahlung-Susi");
        assertZahltEin("Einzahlung: Susi");
        assertZahltEin("Einzahlung : Susi");
        assertZahltEin("Einzahlung - Susi");
        assertZahltEin("Einzahlung Susi 0815");
        assertZahltEin("Einzahlung: 0815 - Susi");
        assertZahltEin("Guthaben Susi");
        assertZahltEin("Guthaben:Susi");
        assertZahltEin("Guthaben: Susi");
        assertZahltEin("Guthaben : Susi");
        assertZahltEin("Guthaben - Susi");
        assertVerbindlichkeit("Guthaben 7345", "Unbekanntes Mitglied");
    }

    public void testForderungBegleichen() {
        insertTransaction("0815", "forderungen", 20, "Bank Susi"); // offen
        insertTransaction("0815", "forderungen", 20, "Bank Andere"); // offen
        insertTransaction("0815", "forderungen", 20, "andere Forderung");
        assertBegleichtForderung("Einzahlung - Susi", 20);
        insertTransaction("0815", "forderungen", 50, "Bank Susi"); // offen
        assertBegleichtForderung("Einzahlung - Susi", 50);
        insertTransaction("0815", "forderungen", 50, "Bank Susi"); // offen
        assertBegleichtForderung("Einzahlung - Susi", 50);
    }

    public void testForderungBegleichenMitRestguthaben() {
        insertTransaction("0815", "forderungen", 10, "Bank Susi");
        insertTransaction("0815", "forderungen", 20, "Bank Susi");
        insertTransaction("0815", "forderungen", 10, "Bank Susi");
        read(gls().vwz1("Einzahlung - Susi").amount(50)); // überweisen
        Cursor items = assertTransaction("Einzahlung - Susi", 4);
        assertTransactionItem("bank", "Bank", "Cash", 50, 1.0f, items);
        items.moveToNext();
        assertTransactionItem("forderungen", "Forderungen", "Bank Susi", -2.0f, 10, items);
        items.moveToNext();
        assertTransactionItem("forderungen", "Forderungen", "Bank Susi", -1.0f, 20, items);
        items.moveToNext();
        assertTransactionItem("0815", "Susi", "Credits", -1.0f, 10, items);
    }

    public void testBarEinzahlung() {
        insertTransaction("1234", "forderungen", 20, "Bar Albert"); // 1
        insertTransaction("0815", "forderungen", 30, "Bar Susi");   // 2
        insertTransaction("1234", "forderungen", 20, "Bar Albert"); // 3
        insertTransaction("0815", "forderungen", 10, "Bar Susi");   // 4
        // kasse wird entleert und eingezahlt. Inzwischen gibt es weitere
        insertTransaction("1234", "forderungen", 50, "Bar Albert"); // 5
        insertTransaction("0815", "forderungen", 30, "Bar Susi");   // 6
        read(gls().vwz1("Barkasse").amount(90)); // überweisung kommt an
        Cursor items = assertTransaction("Barkasse", 5);
        assertTransactionItem("bank", "Bank", "Cash", 90, 1.0f, items);
        items.moveToNext();
        assertTransactionItem("forderungen", "Forderungen", "Bar Albert", -2.0f, 20, items);
        items.moveToNext();
        assertTransactionItem("forderungen", "Forderungen", "Bar Susi", -1.0f, 10, items);
        items.moveToNext();
        assertTransactionItem("forderungen", "Forderungen", "Bar Susi", -1.0f, 30, items);
        items.moveToNext();
        assertTransactionItem("verbindlichkeiten", "Verbindlichkeiten", "Barkasse", -1.0f, 10, items);
        read(gls().vwz1("Barkasse").amount(100)); // neue überweisung kommt an
        items = assertTransaction("Barkasse", 4); // begleicht die zwei weiteren
        assertTransactionItem("bank", "Bank", "Cash", 100, 1.0f, items);
        items.moveToNext();
        assertTransactionItem("forderungen", "Forderungen", "Bar Albert", -1.0f, 50, items);
        items.moveToNext();
        assertTransactionItem("forderungen", "Forderungen", "Bar Susi", -1.0f, 30, items);
        items.moveToNext();
        assertTransactionItem("verbindlichkeiten", "Verbindlichkeiten", "Barkasse", -1.0f, 20, items);
    }

    public void testAnschaffung() {
        assertBooking("Inventar:Tisch", "Inventar", "Tisch", 3.50f, "Rechnung XY");
        assertBooking("Inventar: Kiste", "Inventar", "Kiste", 5.50f, "Rechnung abc");
        assertBooking("Inventar - Stift", "Inventar", "Stift", 1.5f, "Rechnung 234");
        assertBooking("Inventar: tolle Sache", "Inventar", "tolle Sache", 3.50f, "Rechnung XY");
        assertBooking("Inventar: auch mit-strich", "Inventar", "auch mit-strich", 3, "Rechnung");
    }

    public void testAusgaben() {
        assertBooking("Kosten:Miete April", "Kosten", "Miete April", 350, "Dauerauftrag..");
        assertBooking("Kosten: Strom und Wasser", "Kosten", "Strom und Wasser", 34, "XYZABC12");
        assertBooking("Kosten - Versand/Transport", "Kosten", "Versand/Transport", 1.5f, "usw..");
    }

    public void testRechnungBegleichen() {
        insertTransaction("verbindlichkeiten", "lager", 100, "Lieferung 123"); // offen
        assertBooking("Lieferung 123", "Verbindlichkeiten", "Lieferung 123", 100, "bezahlt");
        insertTransaction("verbindlichkeiten", "lager", 42.23f, "Lieferung 123"); // offen
        assertBooking("Lieferung 123", "Verbindlichkeiten", "Lieferung 123", 42.23f, "bezahlt");
        insertTransaction("verbindlichkeiten", "lager", 30, "Lieferung 123"); // offen
        // Verbindlichkeit nicht begleichen, sondern wenn nicht zugeordnet, dann neue Forderung
        assertBooking("Lieferung 123", "Forderungen", "Lieferung 123", 50, "bezahlt"); // zuviel
    }

    public void testIdempotency() { // selber Tag mit selbem VWZ geht nicht
        read(gls().date("01.05.2015").vwz1("Einzahlung - Susi").amount(50));
        assertGebucht("Susi", 50, "Einzahlung - Susi");
        read(gls().date("01.05.2015").vwz1("Einzahlung - Susi").amount(50)); // selbe Buchung
        assertNichtGebucht();
        read(gls().date("01.05.2015").vwz1("Einzahlung - Susi").amount(20)); // anderer Betrag
        assertNichtGebucht();
        read(gls().date("02.05.2015").vwz1("Einzahlung - Susi").amount(50)); // anderer Tag
        assertGebucht("Susi", 50, "Einzahlung - Susi");
        read(gls().date("01.05.2015").vwz1("Guthaben : Susi").amount(50)); // anderer VWZ
        assertGebucht("Susi", 50, "Guthaben : Susi");
    }





    // HELPER

    private void assertGebucht(String account, float amount, String vwz) {
        assertTransaction("0815", account, "Credits", "bank", amount, "VWZ: " + vwz);
    }

    private void assertNichtGebucht() {
        finalizeSession(importer.getSession());
        Cursor txn = query(importer.getSession().buildUpon().appendPath("transactions").build(), 1);
        assertEquals("not booked", "draft", txn.getString(12));
    }

    private void assertEinlage(String verwendungszeck1) {
        read(gls().vwz1(verwendungszeck1).amount(105));
        assertTransaction("einlagen", "Einlagen", "Susi", "bank", 105, "VWZ: " + verwendungszeck1);
    }

    private void assertTrägtBei(String verwendungszeck1) {
        read(gls().vwz1(verwendungszeck1).amount(23.42));
        assertTransaction("beiträge", "Beiträge", "Susi", "bank", 23.42f, "VWZ: " + verwendungszeck1);
    }

    private void assertZahltEin(String verwendungszeck1) {
        read(gls().vwz1(verwendungszeck1).amount(42.23));
        assertTransaction("0815", "Susi", "Credits", "bank", 42.23f, "VWZ: " + verwendungszeck1);
    }

    private void assertVerbindlichkeit(String vwz, String comment) {
        read(gls().vwz1(vwz).amount(42.23));
        assertTransaction("verbindlichkeiten", "Verbindlichkeiten", vwz, "bank", 42.23f, comment);
    }

    private void assertBegleichtForderung(String vwz, float amount) {
        read(gls().vwz1(vwz).amount(amount));
        assertTransaction("forderungen", "Forderungen", "Bank Susi", "bank", amount, "VWZ: " + vwz);
    }

    private void assertBooking(String vwz2, String name, String title, float amount, String comment) {
        read(gls().vwz1(comment).vwz2(vwz2).amount(-amount));
        Cursor items = assertTransaction(comment, 2);
        if (items.getString(11).equals("bank")) { // account order
            assertTransactionItem("bank", "Bank", "Cash", -amount, 1.0f, items);
            items.moveToNext();
            assertTransactionItem(name.toLowerCase(), name, title, 1.0f, amount, items);
        } else {
            assertTransactionItem(name.toLowerCase(), name, title, 1.0f, amount, items);
            items.moveToNext();
            assertTransactionItem("bank", "Bank", "Cash", -amount, 1.0f, items);
        }
    }

    private void assertTransaction(String from_account, String fromAccountName,  String fromTitle,
                                   String to_account, float amount, String comment) {
        Cursor items = assertTransaction(comment, 2);
        assertTransactionItem(to_account, "Bank", "Cash", amount, 1.0f, items);
        items.moveToNext();
        assertTransactionItem(from_account, fromAccountName, fromTitle, -1.0f, amount, items);
    }
    private Cursor assertTransaction(String comment, int assertCount) {
        finalizeSession(importer.getSession());
        Cursor transactions = query(importer.getSession()
                .buildUpon().appendPath("transactions").build(), 1);
        assertTrue(transactions.getString(4) + "' should contain '" + comment,
                transactions.getString(4).contains(comment));
        assertEquals("booked", "final", transactions.getString(12));
        return query("transactions/" + transactions.getLong(0), assertCount);
    }

    private void assertTransactionItem(String guid, String name, String title, float quantity, float price, Cursor items) {
        assertEquals(guid, items.getString(11));
        assertEquals(name, items.getString(12));
        assertEquals(title, items.getString(7));
        assertEquals(quantity, items.getFloat(4));
        assertEquals(price, items.getFloat(5));
    }



    Context ctx = new MockContext() {
        @Override
        public ContentResolver getContentResolver() {
            return getMockContentResolver();
        }
    };

    static { GlsImport.AUTHORITY = "org.baobab.foodcoapp.test"; }
    private ContentValues[] values = new ContentValues[1];
    private GlsImport importer;

    private void read(Gls gls) {
        importer = new GlsImport(ctx);
        values[0] = importer.readLine(gls.line);
    }

    long time = System.currentTimeMillis();
    private Gls gls() {
        return new Gls().date(GlsImport.date.format(new Date(time+=87000000)));
    }

    class Gls {
        public String[] line = new String[] {"", "01.02.2015", "", "", "",
                "", "", "", "", "", "", "", "", "", "", "", "", "", "", "20.3"};

        public Gls date(String date) {
            line[0] = date;
            line[1] = date;
            return this;
        }

        public Gls vwz1(String vwz1) {
            line[5] = vwz1;
            return this;
        }

        public Gls vwz2(String vwz2) {
            line[6] = vwz2;
            return this;
        }

        public Gls vwz3(String vwz2) {
            line[7] = vwz2;
            return this;
        }

        public Gls vwz4(String vwz2) {
            line[8] = vwz2;
            return this;
        }

        public Gls vwz5(String vwz2) {
            line[9] = vwz2;
            return this;
        }

        public Gls vwz6(String vwz2) {
            line[10] = vwz2;
            return this;
        }

        public Gls vwz7(String vwz2) {
            line[11] = vwz2;
            return this;
        }

        public Gls vwz8(String vwz2) {
            line[12] = vwz2;
            return this;
        }

        public Gls vwz9(String vwz2) {
            line[13] = vwz2;
            return this;
        }

        public Gls vwz10(String vwz2) {
            line[14] = vwz2;
            return this;
        }

        public Gls amount(double amount) {
            line[19] = NumberFormat.getInstance().format(amount);
            return this;
        }
    }
}