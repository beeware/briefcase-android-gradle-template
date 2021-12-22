package org.beeware.android;

import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;

public interface IPythonWin {
    void onCreate();
    void onResume();
    void onStart();
    void onDestory();
    void setNative(WinActivity winActivity);
    void onActivityResult(int requestCode, int resultCode, Intent data);
    public boolean onOptionsItemSelected(MenuItem menuitem);
    public boolean onPrepareOptionsMenu(Menu menu);
}
