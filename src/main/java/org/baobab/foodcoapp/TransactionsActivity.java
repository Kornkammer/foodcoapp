package org.baobab.foodcoapp;

import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import au.com.bytecode.opencsv.CSVReader;


public class TransactionsActivity extends AppCompatActivity {

    public static SimpleDateFormat date = new SimpleDateFormat("dd.MM.yyyy");
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transactions);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.container, TransactionListFragment.newInstance(
                        Uri.parse("content://org.baobab.foodcoapp/transactions")))
                .commit();
        try {
            if (getIntent().getScheme().equals("content")) {
                InputStream is = getContentResolver().openInputStream(getIntent().getData());
                CSVReader csv = new CSVReader(new BufferedReader(new InputStreamReader(is)), ';');
                String[] line = csv.readNext();
                System.out.println(line[3]);
                while ((line = csv.readNext()) != null) {
                    System.out.println(line[1]);
                    System.out.println(date.format(date.parse(line[1])));
                    System.out.println(line[3]);
                    System.out.println(line[4]);
                    System.out.println(line[19]);
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

}
