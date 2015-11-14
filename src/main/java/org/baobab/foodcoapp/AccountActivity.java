package org.baobab.foodcoapp;

import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;


public class AccountActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_accounts);
        getSupportActionBar().hide();

        getWindow().setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.xdark_blue)));
        ((AccountListFragment) getSupportFragmentManager().findFragmentById(R.id.active))
                .setUri("content://org.baobab.foodcoapp/accounts/aktiva/accounts", false)
                .setEditable(false);
        ((AccountListFragment) getSupportFragmentManager().findFragmentById(R.id.passive))
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
