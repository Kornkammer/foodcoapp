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
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setContentView(R.layout.activity_accounts);

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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.accounts, menu);
        MenuItem add = menu.findItem(R.id.add);
        getSupportActionBar().setDisplayOptions(
                ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_SHOW_TITLE);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        int id = item.getItemId();
        switch (item.getItemId()) {
            case R.id.add:
                getSupportFragmentManager() .beginTransaction()
                        .replace(R.id.container, AccountEditFragment.newInstance())
                        .addToBackStack("add")
                        .commit();
                break;
            case android.R.id.home:
                finish();
        }
        if (id == R.id.add) {
        }
        return super.onOptionsItemSelected(item);
    }
}
