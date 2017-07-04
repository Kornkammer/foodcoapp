package org.baobab.foodcoapp;

import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;

import org.baobab.foodcoapp.fragments.AccountEditFragment;
import org.baobab.foodcoapp.fragments.AccountListFragment;
import org.baobab.foodcoapp.io.BackupExport;

import java.text.ParseException;

import static org.baobab.foodcoapp.io.BackupExport.YEAR;


public class BalanceActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_accounts);
        getSupportActionBar().hide();
        String query = "";
        if (getIntent().hasExtra("year")) {
            int year = getIntent().getIntExtra("year", 0);
            try {
                System.out.println(YEAR.parse(2017 + "").getTime() - 5000);
                query = "after=" + YEAR.parse("" + year).getTime() +
                        "&before=" + (YEAR.parse("" + (year + 1)).getTime());
            } catch (ParseException e) {
                e.printStackTrace();
            }
            getSupportActionBar().setTitle("Bilanz : Jahr " + year);
            getSupportActionBar().show();
        }
        getWindow().setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.xdark_blue)));
        ((AccountListFragment) getSupportFragmentManager().findFragmentById(R.id.active))
                .setTimeWindow(query)
                .setUri("content://org.baobab.foodcoapp/accounts/aktiva/accounts", false)
                .setEditable(false);
        ((AccountListFragment) getSupportFragmentManager().findFragmentById(R.id.passive))
                .setTimeWindow(query)
                .setUri("content://org.baobab.foodcoapp/accounts/passiva/accounts", true)
                .setEditable(true);
        if (getIntent().getData() != null && getIntent().getData().toString().startsWith("content://org.baobab.foodcoapp/accounts/")) {
            editAccount(getIntent().getData());
            getSupportFragmentManager().addOnBackStackChangedListener(new FragmentManager.OnBackStackChangedListener() {
                @Override
                public void onBackStackChanged() {
                    if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
                        finish();
                    }
                }
            });
        }
    }

    public void editAccount(Uri uri) {
        getSupportFragmentManager() .beginTransaction()
                .replace(R.id.container, AccountEditFragment.newInstance(uri))
                .addToBackStack("add")
                .commit();
    }
}
