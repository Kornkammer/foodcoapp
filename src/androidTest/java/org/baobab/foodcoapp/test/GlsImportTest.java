package org.baobab.foodcoapp.test;


import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.test.mock.MockContext;

import org.baobab.foodcoapp.io.GlsImport;

import java.text.NumberFormat;
import java.util.Date;
import java.util.Locale;


public class GlsImportTest extends BaseProviderTests {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        createDummyAccount("Susi", "0815");
        createDummyAccount("Albert", "1234");
        createDummyAccount("CheckR-WG", "1111");
        createDummyAccount("Coole Leute", "2222");
    }


    public void testAuftraggeberLandetImKommentar() {
        read(gls().who("Susanne Meier").vwz1("Beitrag Susi").amount(50));
        assertTransaction("Susanne Meier", 2);
    }

    public void testEmpfängerLandetImKommentar() {
        read(gls().who("Xaver Hupfinger").vwz1("Auslagen zurück").amount(-50));
        assertTransaction("Xaver Hupfinger", 2);
    }

    public void testSpenden() {
        read(gls().vwz1("Spende").amount(20));
        assertTransaction("Spenden", "Spende", 20, "VWZ: Spende");
        read(gls().vwz1("Spenden hier").amount(20));
        assertTransaction("Spenden", "Spende", 20, "VWZ: Spenden hier");
        read(gls().vwz1("eine kleine spende").amount(5));
        assertTransaction("Spenden", "Spende", 5, "VWZ: eine kleine spende");
        read(gls().vwz1("nochmal ").vwz2("Spenden Kornkammer").amount(20));
        assertTransaction("Spenden", "Spende", 20, "nochmal Spenden Kornkammer");
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
        assertEinlage("Einlage-Susi-0827"); // falsche MitgliedsNr
        assertEinlage("Einlage Susi 0827"); // falsche MitgliedsNr
        assertEinlage("Einlage: 0827 - Susi"); // falsche MitgliedsNr
        assertEinlage("Einlage-Susssi-0815"); // falscher Mitgliedsname
        assertEinlage("Einlage - 0815 Susssli"); // falscher Mitgliedsname
        assertEinlage("Einlage: 0815 : Susssli"); // falscher Mitgliedsname
        assertVerbindlichkeit("MaWoasEsNed: DaSchaugHer", "");
    }

    public void testNameMitBesonderenZeichen() {
        assertEinlage("Einlage CheckR-WG", "CheckR-WG");
        assertEinlage("Einlage Coole Leute", "Coole Leute");
        assertEinlage("Einlage-Coole Leute", "Coole Leute");
        assertEinlage("Einlage f�r CheckR-WG", "CheckR-WG");
        assertEinlage("Einlage f�r-CheckR-WG", "CheckR-WG");
        assertEinlage("Einlage - Coole Leute", "Coole Leute");
        assertEinlage("Einlage f�r die CheckR-WG", "CheckR-WG");
        assertEinlage("Einlage f�r die tolle CheckR-WG", "CheckR-WG");
        assertEinlage("Einlage f�r die tolle CheckR-WG geht auch", "CheckR-WG");
    }

    public void testBeitrag() {
        assertTrägtBei("0815 Mitgliederbeitrag 1.");
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
    }

    public void testSchluesselwortOhneMitglied() {
        assertVerbindlichkeit("seltsame Einlage von Unbekannt", "Einlage", "Kein Mitglied gefunden");
        assertVerbindlichkeit("noch Guthaben von Irgendwem", "Einzahlung", "Kein Mitglied gefunden");
        assertVerbindlichkeit("oder 1. mal Prepaid euro 30", "Einzahlung", "Kein Mitglied gefunden");
        assertVerbindlichkeit(" und ein Beitrag von Niemandem", "Beitrag", "Kein Mitglied gefunden");
        assertVerbindlichkeit("vwz ganz ohne Sinn", "vwz ganz ohne Sinn", "Kein Mitglied gefunden");
    }

    public void testForderungBegleichen() {
        insertTransaction("0815", "forderungen", 20, "Bank Susi"); // offen
        insertTransaction("0815", "forderungen", 20, "Bank Andere"); // offen
        insertTransaction("0815", "forderungen", 20, "andere Forderung");
        assertBegleichtForderung("Einzahlung - Susi", 20);
        assertTrue(importer.getMsg().contains("Forderung beglichen: Bank Susi -> 20.00"));
        insertTransaction("0815", "forderungen", 50, "Bank Susi"); // offen
        assertBegleichtForderung("Einzahlung - Susi", 50);
        assertTrue(importer.getMsg().contains("Forderung beglichen: Bank Susi -> 50.00"));
        insertTransaction("0815", "forderungen", 50, "Bank Susi"); // offen
        assertBegleichtForderung("Einzahlung - Susi", 50);
        assertTrue(importer.getMsg().contains("Forderung beglichen: Bank Susi -> 50.00"));
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
        assertTransactionItem("0815", "Susi", "Korns", -10, 1.0f, items);
    }

    public void testForderungBegleichenMehrfach() {
        insertTransaction("0815", "forderungen", 40, "Bank Susi");
        importer = new GlsImport(ctx);
        importer.readLine(gls().vwz1("Einzahlung - Susi").amount(40).line); // überweisen
        importer.readLine(gls().vwz1("Einzahlung - Susi").amount(60).line); // überweisen
        finalizeSession(importer.getSession());
        Cursor transactions = query(importer.getSession()
                .buildUpon().appendPath("transactions").build(), 2);
        assertEquals("booked", "final", transactions.getString(12));
        Cursor items = query("transactions/" + transactions.getLong(0) + "/products", 2);
        assertTransactionItem("bank", "Bank", "Cash", 40, 1.0f, items);
        items.moveToNext();
        assertTransactionItem("forderungen", "Forderungen", "Bank Susi", -1.0f, 40, items);
        transactions.moveToNext();
        assertEquals("booked", "final", transactions.getString(12));
        items = query("transactions/" + transactions.getLong(0) + "/products", 2);
        assertTransactionItem("bank", "Bank", "Cash", 60, 1.0f, items);
        items.moveToNext();
        assertTransactionItem("0815", "Susi", "Korns", -60, 1.0f, items);
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
        assertTrue(importer.getMsg().contains("Forderung beglichen: Bar Albert -> 20.00"));
        assertTrue(importer.getMsg().contains("Forderung beglichen: Bar Susi -> 10.00"));
        assertTrue(importer.getMsg().contains("Forderung beglichen: Bar Susi -> 30.00"));
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

    public void testAuslagen() {
        read(gls().vwz5("Auslage 0815").amount(-100));
        Cursor items = assertTransaction("Auslage", 2);
        assertTransactionItem("bank", "Bank", "Cash", -100, 1, items);
        items.moveToNext();
        assertTransactionItem("einlagen", "Einlagen", "Susi", 1.0f, 100, items);
    }

    public void testAuszahlung() {
        read(gls().who("Auszahlung").vwz1("01.07/15.50 UHR GLS Freiburg").amount(-300));
        Cursor items = assertTransaction("Auszahlung", 2);
        assertTransactionItem("bank", "Bank", "Cash", -300, 1, items);
        items.moveToNext();
        assertTransactionItem("forderungen", "Forderungen", "Auszahlung", 1.0f, 300, items);
    }

    public void testAuszahlungBegleichtVerbindlichkeit() {
        insertTransaction("verbindlichkeiten", "inventar", 300, "Auszahlung");
        read(gls().who("Auszahlung").vwz1("blabla").amount(-300));
        Cursor items = assertTransaction("Auszahlung", 2);
        assertTransactionItem("bank", "Bank", "Cash", -300, 1, items);
        items.moveToNext();
        assertTransactionItem("verbindlichkeiten", "Verbindlichkeiten", "Auszahlung", 1.0f, 300, items);
        assertTrue(importer.getMsg().contains("Verbindlichkeit beglichen: Auszahlung -> 300.00"));
    }

    public void testKontofuehrungsgebuehren() {
        read(gls().booking("Kontof�hrung").vwz1("Abrechnung vom  30.10.2015").amount(-7.32));
        Cursor items = assertTransaction("Kontoführungsgebühren", 2);
        assertTransactionItem("kosten", "Kosten", "Kontogebühren", 7.32f, 1, items);
        items.moveToNext();
        assertTransactionItem("bank", "Bank", "Cash", -7.32f, 1.0f, items);
        read(gls().booking("Kontoführung").vwz1("Abrechnung vom  30.10.2015").amount(-7.32));
        items = assertTransaction("Kontoführungsgebühren", 2);
        assertTransactionItem("kosten", "Kosten", "Kontogebühren", 7.32f, 1, items);
        items.moveToNext();
        assertTransactionItem("bank", "Bank", "Cash", -7.32f, 1.0f, items);
    }

    public void testStandardUeberweisung() {
        read(gls().vwz1("BIC:A1").vwz2("IBAN:DE1")
                .vwz3("Datum: 13.10.15 Zeit: 21:38").vwz4("KD 0012 TAN 345")
                .vwz5("Baumaterial").amount(-29));
        Cursor items = assertTransaction("Baumaterial", 2);
        assertTransactionItem("bank", "Bank", "Cash", -29, 1, items);
        items.moveToNext();
        assertTransactionItem("forderungen", "Forderungen", "Baumaterial", 1, 29, items);
    }

    public void testStandardUeberweisungOhneVwz() {
        read(gls().vwz1("BIC:A1").vwz2("IBAN:DE1")
                .vwz3("Datum: 13.10.15 Zeit: 21:38").vwz4("KD 0012 TAN 345")
                .who("Xaver").amount(-29));
        Cursor items = assertTransaction("Xaver", 2);
        assertTransactionItem("bank", "Bank", "Cash", -29, 1, items);
        items.moveToNext();
        assertTransactionItem("forderungen", "Forderungen", "Xaver", 1, 29, items);
    }

    public void testAnschaffung() {
        assertBooking("Inventar:Tisch", "Inventar", "Tisch", 3.50f, "Rechnung XY");
        assertBooking("Inventar: Kiste", "Inventar", "Kiste", 5.50f, "Rechnung abc");
        assertBooking("Inventar - Stift", "Inventar", "Stift", 1.5f, "Rechnung 234");
        assertBooking("Inventar, tolle+Sache", "Inventar", "tolle+Sache", 3.50f, "Rechnung XY");
        assertBooking("Inventar: auch mit-strich", "Inventar", "auch mit-strich", 3, "Rechnung");
    }

    public void testAusgaben() {
        assertBooking("Kosten:Miete April", "Kosten", "Miete April", 350, "Dauerauftrag..");
        assertBooking("Kosten: Strom und Wasser", "Kosten", "Strom und Wasser", 34, "XYZABC12");
        assertBooking("Kosten - Versand/Transport", "Kosten", "Versand/Transport", 1.5f, "usw..");
    }

    public void testRechnungBegleichen() {
        insertTransaction("verbindlichkeiten", "lager", 23.42f, "RefNr 123"); // offen
        read(gls().vwz5("RefNr 123").vwz6("blabla").amount(-23.42));
        assertBookingTxn("Verbindlichkeiten", "RefNr 123", 23.42f, "blabla");
        assertTrue(importer.getMsg().contains("Verbindlichkeit beglichen: " + "RefNr 123" + " -> 23.42"));

        insertTransaction("verbindlichkeiten", "lager", 23.42f, "RefNr 123"); // offen
        read(gls().vwz5("RefNr 123").vwz7("blabla").amount(-23.42));
        assertBookingTxn("Verbindlichkeiten", "RefNr 123", 23.42f, "blabla");
        assertTrue(importer.getMsg().contains("Verbindlichkeit beglichen: " + "RefNr 123" + " -> 23.42"));

        insertTransaction("verbindlichkeiten", "lager", 23.42f, "RefNr 123"); // offen
        read(gls().vwz5("blabla").vwz7("RefNr 123").amount(-23.42));
        assertBookingTxn("Verbindlichkeiten", "RefNr 123", 23.42f, "blabla");
        assertTrue(importer.getMsg().contains("Verbindlichkeit beglichen: " + "RefNr 123" + " -> 23.42"));
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
        assertCreditTransaction("0815", account, "Korns", "bank", amount, "VWZ: " + vwz);
    }

    private void assertNichtGebucht() {
        finalizeSession(importer.getSession());
        Cursor txn = query(importer.getSession().buildUpon().appendPath("transactions").build(), 1);
        assertEquals("not booked", "draft", txn.getString(12));
    }

    private void assertEinlage(String verwendungszeck1) {
        assertEinlage(verwendungszeck1, "Susi");
    }

    private void assertEinlage(String verwendungszeck1, String title) {
        read(gls().vwz1(verwendungszeck1).amount(105));
        assertTransaction("Einlagen", title, 105, "VWZ: " + verwendungszeck1);
    }

    private void assertTrägtBei(String verwendungszeck1) {
        read(gls().vwz1(verwendungszeck1).amount(23.42));
        assertTransaction("Beiträge", "Susi", 23.42f, "VWZ: " + verwendungszeck1);
    }

    private void assertZahltEin(String verwendungszeck1) {
        read(gls().vwz1(verwendungszeck1).amount(42.23));
        assertCreditTransaction("0815", "Susi", "Korns", "bank", 42.23f, "VWZ: " + verwendungszeck1);
    }

    private void assertZahltEin(String vwz1, String vwz2) {
        read(gls().vwz1(vwz1).vwz2(vwz2).amount(42.23));
        assertCreditTransaction("0815", "Susi", "Korns", "bank", 42.23f, "VWZ: " + vwz1);
    }

    private void assertVerbindlichkeit(String vwz, String comment) {
        assertVerbindlichkeit(vwz, vwz, comment);
    }

    private void assertVerbindlichkeit(String vwz, String title, String comment) {
        read(gls().vwz1(vwz).amount(42.23));
        assertTransaction("Verbindlichkeiten", title, 42.23f, comment);
    }

    private void assertBegleichtForderung(String vwz, float amount) {
        read(gls().vwz1(vwz).amount(amount));
        assertTransaction("Forderungen", "Bank Susi", amount, "VWZ: " + vwz);
    }

    private void assertBooking(String vwz2, String name, String title, float amount, String comment) {
        read(gls().vwz5(vwz2).vwz6(comment).amount(-amount));
        assertBookingTxn(name, title, amount, comment);
        read(gls().vwz5(comment).vwz6(vwz2).amount(-amount));
        assertBookingTxn(name, title, amount, comment);
        read(gls().vwz5(comment).vwz7(vwz2).amount(-amount));
        assertBookingTxn(name, title, amount, comment);
        read(gls().vwz5(comment.substring(0, 5)).vwz6(comment.substring(5, comment.length()))
                .vwz7(vwz2.substring(0, 5)).vwz8(vwz2.substring(5, vwz2.length()))
                .amount(-amount));
        assertBookingTxn(name, title, amount, comment);
    }

    private void assertBookingTxn(String name, String title, float amount, String comment) {
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

    private void assertTransaction(String fromName,  String fromTitle, float amount, String comment) {
        assertTransaction(fromName.toLowerCase(), fromName, fromTitle, "bank", amount, comment);
    }

    private void assertCreditTransaction(String fromGuid, String fromName,  String fromTitle,
                                   String toGuid, float amount, String comment) {
        Cursor items = assertTransaction(comment, 2);
        assertTransactionItem(toGuid, "Bank", "Cash", amount, 1.0f, items);
        items.moveToNext();
        assertTransactionItem(fromGuid, fromName, fromTitle, -amount, 1, items);
    }

    private void assertTransaction(String fromGuid, String fromName,  String fromTitle,
                                   String toGuid, float amount, String comment) {
        Cursor items = assertTransaction(comment, 2);
        assertTransactionItem(toGuid, "Bank", "Cash", amount, 1.0f, items);
        items.moveToNext();
        assertTransactionItem(fromGuid, fromName, fromTitle, -1.0f, amount, items);
    }

    private Cursor assertTransaction(String comment, int assertCount) {
        finalizeSession(importer.getSession());
        Cursor transactions = query(importer.getSession()
                .buildUpon().appendPath("transactions").build(), 1);
        assertTrue(transactions.getString(4) + "' should contain '" + comment,
                transactions.getString(4).contains(comment));
        assertEquals("booked", "final", transactions.getString(12));
        return query("transactions/" + transactions.getLong(0) + "/products", assertCount);
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
    private GlsImport importer;

    private void read(Gls gls) {
        importer = new GlsImport(ctx);
        importer.readLine(gls.line);
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

        public Gls who(String who) {
            line[3] = who;
            return this;
        }

        public Gls booking(String booking) {
            line[4] = booking;
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
            line[19] = NumberFormat.getInstance(Locale.GERMANY).format(amount);
            return this;
        }
    }
}