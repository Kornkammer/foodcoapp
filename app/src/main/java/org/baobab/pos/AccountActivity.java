package org.baobab.pos;

import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;


public class AccountActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().setHomeButtonEnabled(true);
        setContentView(R.layout.activity_accounts);

        getWindow().setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.xdark_blue)));
        ((AccountListFragment) getSupportFragmentManager().findFragmentById(R.id.active))
                .setUri("content://org.baobab.pos/accounts/1/accounts");
        ((AccountListFragment) getSupportFragmentManager().findFragmentById(R.id.passive))
                .setUri("content://org.baobab.pos/accounts/2/accounts");
        if (getIntent().getData() != null && getIntent().getData().toString().startsWith("content://org.baobab.pos/accounts/")) {
            editAccount(getIntent().getData());
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
