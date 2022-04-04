package org.beeware.android;

import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;

public interface IPythonApp {
    void onCreate();
    void onResume();
    void onStart();
    void onActivityResult(int requestCode, int resultCode, Intent data);
    public boolean onOptionsItemSelected(MenuItem menuitem);
    public boolean onPrepareOptionsMenu(Menu menu);
}
