package org.baobab.foodcoapp.io;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;

import org.baobab.foodcoapp.ImportActivity;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;

public class BackupImport implements ImportActivity.Importer {

    private String msg;

    public BackupImport load(Context ctx, Uri file) {

        ZipInputStream zis;
        try {
            zis = new ZipInputStream(new BufferedInputStream(
                    ctx.getContentResolver().openInputStream(file)));

            String name = zis.getNextEntry().getName();
            if (!name.contains(".BAK")) {
                msg = "No Backup file!";
                return this;
            }
            File dest = new File(Environment.getDataDirectory(),
                    "//data//org.baobab.foodcoapp//databases//" + name);

            BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(dest));
            int count;
            byte buf[] = new byte[2048];
            while ((count = zis.read(buf, 0, 2048)) != -1) {
                os.write(buf, 0, count);
            }
            os.close();
            ctx.getContentResolver().insert(Uri.parse(
                    "content://org.baobab.foodcoapp/load/" + name), null);
            zis.close();
            msg = "Imformationsstand " + name + " geladen";
        } catch (Exception e) {
            e.printStackTrace();
            msg = "No Backup file!";
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
