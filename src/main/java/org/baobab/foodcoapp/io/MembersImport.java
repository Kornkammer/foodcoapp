package org.baobab.foodcoapp.io;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.util.Log;

import org.baobab.foodcoapp.AccountActivity;
import org.baobab.foodcoapp.ImportActivity;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;

import au.com.bytecode.opencsv.CSVReader;

public class MembersImport implements ImportActivity.Importer {

    static final String TAG = AccountActivity.TAG;
    ArrayList<ContentValues> values = new ArrayList<>();
    String msg = "";
    Context ctx;

    public MembersImport(Context ctx) {
        this.ctx = ctx;
    }

    @Override
    public int read(CSVReader csv) throws IOException {
        int count = 0;
        String[] line;
        csv.readNext();
        while ((line = csv.readNext()) != null) {
            System.out.println(line[0] + line[1] + ": " + line[2] + " - " + line[5]);
            ContentValues v = new ContentValues();
            try {
                v.put("guid", line[0]);
                v.put("fee", line[5]);
                v.put("created_at", BackupExport.df.parse(line[2]).getTime());
                values.add(v);
                System.out.println("guid " + line[0] + " - fee " + line[5] + " - date " + line[2]);
            } catch (ParseException e) {
                Log.e(TAG, line[1] + ": could not parse " + line[2]);
                e.printStackTrace();
            }
            count++;
        }
        msg = "IMPORT Mitglieder Accounts:\n" +
                "Are you sure you know what you do?\n" +
                "This will overwrite " + values.size() + " member accounts!\n\n”";
        return 1;
    }

    public String update(Context ctx) {
        for (int i = 0; i < values.size(); i++) {
            ContentValues v = values.get(i);
            System.out.println("update " + v);
            ctx.getContentResolver().update(Uri.parse(
                    "content://org.baobab.foodcoapp/accounts/"), v,
                    "guid IS ?", new String[] { v.getAsString("guid") });
        }
        return "";
    }

    @Override
    public Uri getSession() {
        return null;
    }

    @Override
    public String getMsg() {
        return msg;
    }

}
