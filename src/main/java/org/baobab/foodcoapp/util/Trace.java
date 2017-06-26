package org.baobab.foodcoapp.util;


import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.support.annotation.Nullable;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class Trace {

    private final Context ctx;
    public final Txn split;
    public final Txn combine;

    public Trace(Context context, String account, String query, int forward) {
        ctx = context;
        split = new Txn();
        combine = new Txn();
        trace("content://org.baobab.foodcoapp/accounts/" +
                account + "/transactions?", query, forward, 0, null);
    }

    public Trace(Context context, String title, float price, String query, int forward) {
        ctx = context;
        split = new Txn();
        combine = new Txn();
        trace(null, title, price, query, forward, 0, null);
    }

    public List<Txn> split(String account) {
        return walk(account, split);
    }

    public List<Txn> combine(String account) {
        return walk(account, combine);
    }

    private List<Txn> walk(String account, Txn t) {
        HashMap<String, Trace.Prod> seen = new HashMap<>();
        ArrayList<Txn> result = new ArrayList<>();
        float sum = 0;
        for (Trace.Prod p : t.list(account)) {
            //System.out.println(" # " + p + "   = " + (p.in*p.price) + " - " + (p.out*p.price) + "  || " + p.date);
            if (p.quantity() == 0) continue;
            Trace.Txn txn = collect(p, 0, seen, null);
            txn.head = p;
            result.add(txn);
            sum += p.value();
        }
        float found = 0;
        for (Trace.Prod p : seen.values()) found += p.value();
        if (Math.abs(found) > 0.01) {
            System.out.println("SEEN SUM " + found);
        }
        return result;
    }

    private Trace.Txn collect(Trace.Prod p, int level, Map<String, Prod> seen, Trace.Txn txn) {
        if (p.quantity() == 0) return txn;
        if (level > 42) return txn;
        if (txn == null) {
            txn = new Trace.Txn();
            seen.put(p.id + "", p);
        } else {
            if (seen.containsKey(p.id + "")) return txn;
            seen.put(p.id + "", p);
            //System.out.println(" " + indent(level) + " # " + p + " (" + p.inputs.size() + ") ");
            txn.add(p);
        }
        if (p.base.products.get(p.key()) != null && p.base.products.get(p.key()).size() > 0) {
            //System.out.println(" " + indent(level) + "   next " + p.base.products.get(p.key()));
            for (Trace.Prod o : p.base.products.get(p.key())) {
                collect(o, level + 1, seen, txn);
            }
        }
        //System.out.println(" " + indent(level) + "   relax " + p);
        for (Trace.Prod o : p.siblings()) {
            collect(o, level + 1, seen, txn);
        }
        return txn;
    }

    public void trace(String account, String title, float price, String query, int forward, int level, Prod parent) {
        //System.out.println(" " + indent(level) + " TRACE " + title + "  " + price);
        trace(Uri.parse("content://org.baobab.foodcoapp" + (account != null? "/accounts/" + account : "") +
                "/transactions?" ).buildUpon().appendQueryParameter("title", title)
                .appendQueryParameter("price", String.format(Locale.ENGLISH, "%.2f", price))
                .build().toString(), query, forward, level, parent);
    }


    public void trace(String uri, String query, int forward, int level, Prod parent) {

        Cursor orig = ctx.getContentResolver().query(Uri.parse(uri + "&" + query), null, null, null, null);
        while (orig.moveToNext()) {
            if (level == 0) {
                if (orig.getFloat(6) * forward > 0) continue;
                //System.out.println(level + " TRACE " + orig.getString(4));
            }
            Cursor prods = ctx.getContentResolver().query(Uri.parse(
                    "content://org.baobab.foodcoapp/transactions/" +
                            orig.getLong(0) + "/products"), null, null, null, null);
            ArrayList<Prod> siblings = new ArrayList<>();
            float sum = 0;
            float maxIn = 0;
            float maxOut = 0;
            while (prods.moveToNext()) {

                Prod p = new Prod(prods, orig, level);
                siblings.add(p);
                //System.out.println("  " + indent(level) + " ADD " + "  " + p + (parent != null? "   parent= " + parent : ""));
                if (p.quantity() > 0) {
                    maxOut = Math.max(maxOut, p.value());
                    sum++;
                } else {
                    maxIn = Math.max(maxIn, - p.value());
                    sum--;
                }
            }
            Txn txn = ( (sum * forward < 0 || (sum == 0 && maxIn < maxOut))) ? combine : split;
            if (txn.visited.containsKey(orig.getString(0))) continue;
            txn.visited.put(orig.getString(0), true);
            for (Prod p : siblings) {
                for (Prod s : siblings) {
                    if (s.id != p.id) {
                        p.inputs.add(s);
                        s.inputs.add(p);
                    }
                }

                txn.add(p);
                p.base = txn.accounts.get(p.account);
                if (!p.account.equals("bank") && !p.parent_guid.equals("mitglieder")
                        && !p.account.equals("lager") && !p.account.equals("kasse")) {
                    //System.out.println("  " + indent(level) + " TRACE " + "  " + p + (parent != null? "   parent= " + parent : ""));
                    trace(p.account, p.title, p.price, query, forward, level + 1, p);
                }
            }
            for (Prod p : siblings) {

            }
        }
    }


    String indent(int level) {
        String s = "";
        for (int i = 0; i < level; i++) s += " ";
        return s + level;
    }

    public static class Account {
        public String name;
        public HashMap<String, List<Prod>> products;
        public Account(String n) {
            products = new HashMap<>();
            name = n;
        }
        void add(Prod p) {
            if (!products.containsKey(p.key())) {
                products.put(p.key(), new ArrayList<Prod>());
            }
            for (Prod i : products.get(p.key())) {
                i.next = p;
                p.next = i;
            }
            products.get(p.key()).add(p);
        }

        private String key(Prod p) {
            return p.title + String.format(Locale.ENGLISH, "%.2f", p.price);
        }

        public float debit() {
            float s = 0;
            for (List<Prod> l : products.values())
                for (Prod p : l)
                    s += p.in * p.price;
            return s;
        }
        public float credit() {
            float s = 0;
            for (List<Prod> l : products.values())
                for (Prod p : l)
                    s += p.out * p.price;
            return s;
        }
        public float saldo() {
            return debit() - credit();
        }

        public Prod get(Prod prod) {
            List<Prod> l = products.get(prod.key());
            if (l != null) {
                return reduce(l);
            } else return prod;
        }

        public List<Prod> out() {
            List<Prod> res = new ArrayList<>();
            for (List<Prod> l : products.values()) {
                res.add(reduce(l));
            }
            return res;
        }

        @Nullable
        private Prod reduce(List<Prod> l) {
            Prod a = null;
            for (Prod p : l) {
                if (a == null) a = new Prod(p);
                else {
                    a.in += p.in;
                    a.out += p.out;
                    a.levelIn = p.levelIn;
                    a.levelOut = p.levelOut;
                    a.inputs.addAll(p.inputs);
                    a.outputs.addAll(p.outputs);
                    a.next = p;
                }
            }
            a.base = this;
            return a;
        }
    }

    public static class Txn {
        public ArrayList<Prod> lookup;
        public HashMap<String, Boolean> visited;
        public HashMap<String, Account> accounts;
        public Prod head;

        public Txn() {
            lookup = new ArrayList<>();
            accounts = new HashMap<>();
            visited = new HashMap<>();
        }

        public void add(Prod p) {
            if (!accounts.containsKey(p.account)) {
                accounts.put(p.account, new Account(p.account));
            }
            accounts.get(p.account).add(p);
            p.idx = lookup.size();
            lookup.add(p);
        }

        public float debit(String account) {
            if (accounts.containsKey(account)) {
                return accounts.get(account).debit();
            } else {
                return 0;
            }
        }

        public float credit(String account) {
            if (accounts.containsKey(account)) {
                return accounts.get(account).credit();
            } else {
                return 0;
            }
        }

        public float saldo(String account) {
            if (accounts.containsKey(account)) {
                return accounts.get(account).saldo();
            } else {
                return 0;
            }
        }

        public Prod get(Prod p) {
            if (accounts.containsKey(p.account)) {
                return accounts.get(p.account).get(p);
            } else {
                return new Prod(p.parent_guid, p.account, 0);
            }
        }

        public Cursor toCursor() {
            MatrixCursor c = new MatrixCursor(new String[15]);
            ArrayList<Account> l = new ArrayList<>(accounts.values());
            Collections.sort(l, new Comparator<Account>() {
                @Override
                public int compare(Account a1, Account a2) {
                    return (a1.saldo() < a2.saldo()? 1 : -1);
                }
            });
            float sum = 0;
            for (Account a : l) {
                //System.out.println(" ### " + a.name + ": " + a.debit() + " - " + a.credit() + " = " + a.saldo());
                sum += a.saldo();

                for (Prod p : list(a.name)) {
                    String[] row = new String[15];
                    row[1] = p.idx + "";
                    row[2] = p.account;
                    row[7] = p.title;
                    row[3] = p.product_id + "";
                    row[4] = p.quantity() + "";
                    row[5] = p.price + "";
                    row[6] = p.unit;
                    row[8] = p.img;
                    row[10] = p.parent_guid;
                    row[11] = p.guid;
                    row[12] = p.name;
                    if (p.in == p.out) {
                        row[13] = "-1";
                        row[4] = "-" + p.in;
                    }
                    c.addRow(row);
                    //if (p.quantity() > 0)
                    //System.out.println(p.title + " " + p.quantity() + " x " + p.price);
                }
            }
            System.out.println("=================");
            System.out.println("SUM " + sum);
            return c;
        }

        public List<Prod> list(String account) {
            if (!accounts.containsKey(account)) {
                return new ArrayList<>();
            } else {
                List<Prod> list = accounts.get(account).out();
                Collections.sort(list, new Comparator<Prod>() {
                    @Override
                    public int compare(Prod p1, Prod p2) {
                        if (p2.quantity() * p2.price < p1.quantity() * p1.price)
                            return -1;
                        else if (p2.quantity() * p2.price > p1.quantity() * p1.price)
                            return 1;
                        else {
                            if (p2.id < p1.id)
                                return -1;
                            else if (p2.id > p1.id)
                                return 1;
                            else
                                return 0;
                        }
                    }
                });
                return list;
            }
        }

    }

    public static class Prod {
        public long id;
        public long date;
        public float in;
        public float out;
        public float price;
        public String title;
        public String account;
        public String unit;
        public String img;
        public String parent_guid;
        public long product_id;
        public String guid;
        public String name;
        public int idx;
        public Txn origin;
        public int levelIn = 0;
        public int levelOut = 0;
        public Prod parent;
        public List<Prod> outputs;
        public List<Prod> inputs;
        public Prod next;
        public Account base;

        public Prod(Cursor prods, Cursor orig, int level) {
            id = prods.getLong(0);
            date = orig.getLong(2);
            img = prods.getString(8);
            unit = prods.getString(6);
            guid = prods.getString(11);
            name = prods.getString(12);
            title = prods.getString(7);
            account = prods.getString(2);
            parent_guid = prods.getString(10);
            if (account.equals("bank")) {
                if (orig.getString(4).length() > 15) {
                    title = orig.getString(4).substring(4)
                            .replace("VWZ nicht erkannt", "")
                            .replaceAll("\\n", "|")
                    //.replaceAll("VWZ: |", " ")
                    ;
                } else {
                    title = orig.getString(4);
                }
            }
            if (title.equals("Korns")) {
                title = name;
                name = "Guthaben";
            }
            //if (Pattern.matches("\\d\\d\\d\\d\\d", account)) {
            if (parent_guid.equals("mitglieder")) {
                account = "korns";
            }
            if (prods.getFloat(4) > 0) {
                in = prods.getFloat(4);
                levelIn = level;
            } else {
                out = - prods.getFloat(4);
                levelOut = level;
            }
            price = prods.getFloat(5);
            product_id = prods.getLong(3);
            inputs = new ArrayList<>();
            outputs = new ArrayList<>();
        }

        public Prod(String parent, String acc, float val) {
            parent_guid = parent;
            account = acc;
            title = acc;
            name = acc;
            if (val > 0)
                in = 1;
            else
                out = 1;
            price = Math.abs(val);
            inputs = new ArrayList<>();
            outputs = new ArrayList<>();
        }

        public List<Prod> siblings() {
            List<Prod> res = new ArrayList<>();
            Collections.sort(res, new Comparator<Prod>() {
                public int compare(Prod p1, Prod p2) {
                    if (p2.quantity() * p2.price < p1.quantity() * p1.price)
                        return 1;
                    else if (p2.quantity() * p2.price > p1.quantity() * p1.price)
                        return -1;
                    else return 0;
                }
            });
            return inputs;
        }

        public String key() {
            return title + String.format(Locale.ENGLISH, "%.2f", price);
        }

        public String label() {
            if (parent_guid.equals("aktiva")) {
                if (Math.abs(quantity()) != 1 && price != 1) {
                    if (unit.equals("Stück"))
                        return ((int) quantity()) + " x " + title + " (" + String.format(Locale.ENGLISH, "%.2f", price) + ")";
                    else
                        return ((int) quantity()) + " " + unit + " " + title + " (" + String.format(Locale.ENGLISH, "%.2f", price) + ")";
                } else return title;
            } else return title;
        }

        @Override
        public String toString() {
            if (account.equals("bank"))
                return name + title + ": " + value();
            else if (parent_guid.equals("aktiva"))
                return name + " : " + title + ": " + value();
            else
                return name + " : " + title + ": " + (-1 * value());
        }

        public float quantity() {
            return in - out;
        }

        public float value() {
            return quantity() * price;
        }

        public Prod(Prod p) {
            id = p.id;
            in = p.in;
            out = p.out;
            price = p.price;
            date = p.date;
            title = p.title;
            account = p.account;
            unit = p.unit;
            img = p.img;
            parent_guid = p.parent_guid;
            product_id = p.product_id;
            guid = p.guid;
            name = p.name;
            idx = p.idx;
            origin = p.origin;
            outputs = p.outputs;
            levelIn = p.levelIn;
            levelOut = p.levelOut;
            inputs = p.inputs;
            parent = p.parent;
            next = p.next;
        }

    }
}
