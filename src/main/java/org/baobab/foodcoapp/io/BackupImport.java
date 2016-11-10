package org.baobab.foodcoapp.io;

import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import org.baobab.foodcoapp.AccountActivity;
import org.baobab.foodcoapp.ImportActivity;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import au.com.bytecode.opencsv.CSVReader;

public class BackupImport implements ImportActivity.Importer {

    private String msg;

    public String getDbFile() {
        return dbFile;
    }

    private String dbFile;

    public BackupImport load(Context ctx, Uri file) {

        ZipInputStream zis;
        try {
            zis = new ZipInputStream(new BufferedInputStream(
                    ctx.getContentResolver().openInputStream(file)));

            dbFile = zis.getNextEntry().getName();
            if (!dbFile.contains(".BAK")) {
                Log.e(AccountActivity.TAG, "No Backup db");
                return this;
            }
            File dest = new File(Environment.getDataDirectory(),
                    "//data//org.baobab.foodcoapp//databases//" + dbFile);

            BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(dest));
            int count;
            byte buf[] = new byte[2048];
            while ((count = zis.read(buf, 0, 2048)) != -1) {
                os.write(buf, 0, count);
            }
            os.close();
            zis.close();
            msg = "\n\nImformationsstand " + dbFile + " laden?\n\nrestore backup?\n \n";
            return this;
        } catch (Exception e) {
            Log.i(AccountActivity.TAG, "No Backup zip!");
            dbFile = null;
        }
        return this;
    }

    public static File file(String name) {
        return new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + name);
    }

    public static void zip(File file, ZipOutputStream zos) throws IOException {
        zip(null, file, zos);
    }

    public static void zip(String dir, File file, ZipOutputStream zos) throws IOException {
        zip(dir, file, file.getName(), zos);
    }

    public static void zip(String dir, File file, String name, ZipOutputStream zos) throws IOException {
        zos.putNextEntry(new ZipEntry((dir != null? dir + "/" : "") + name));
        BufferedInputStream in = new BufferedInputStream(new FileInputStream(file), 2048);
        int count;
        byte buf[] = new byte[2048];
        while ((count = in.read(buf, 0, 2048)) != -1) {
            zos.write(buf, 0, count);
        }
        zos.closeEntry();
    }

    @Override
    public int read(CSVReader csv) throws IOException {
        return 0;
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
