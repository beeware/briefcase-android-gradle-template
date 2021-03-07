package org.beeware.android;

import android.content.Intent;

public interface IPythonApp {
    void onCreate();
    void onResume();
    void onStart();
    void onActivityResult(int requestCode, int resultCode, Intent data);
}
