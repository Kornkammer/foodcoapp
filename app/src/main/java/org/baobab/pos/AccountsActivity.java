package org.baobab.pos;

import android.net.Uri;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;


public class AccountsActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActionBar().setHomeButtonEnabled(true);
        setContentView(R.layout.activity_accounts);
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
