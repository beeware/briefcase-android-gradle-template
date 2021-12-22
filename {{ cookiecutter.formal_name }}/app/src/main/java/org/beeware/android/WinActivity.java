package org.beeware.android;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import {{ cookiecutter.package_name }}.{{ cookiecutter.module_name }}.R;

public class WinActivity extends AppCompatActivity {
    // To profile app launch, use `adb -s MainActivity`; look for "onCreate() start" and "onResume() completed".
    protected String TAG = "WindowActivity";
    public IPythonWin pythonWin=null;
    public void setPythonWin(IPythonWin win) {
        pythonWin = win;
    }

    protected long pythonWinId = 0;
    public void setPythonWinId(long id){
        pythonWinId = id;
    }
    public long getPythonWinId(){
        return pythonWinId;
    }
    protected void onCreate(Bundle savedInstanceState){
        if (pythonWin == null) {
            if (pythonWinId == 0) {
                Log.d(TAG, "onCreate() start");
                pythonWinId = (Long)
                        getIntent().getSerializableExtra("pythonWinId");
                setTheme(R.style.AppTheme);
            }
            MainActivity.getPythonApp().getPythonWinById(this);
        }
        super.onCreate(savedInstanceState);
        Log.d(TAG, "user code onCreate() start");
        pythonWin.setNative(this);
        pythonWin.onCreate();
        Log.d(TAG, "user code onCreate() complete");
        Log.d(TAG, "onCreate() complete");
    }

    protected void onStart() {
        Log.d(TAG, "onStart() start");
        super.onStart();
        pythonWin.onStart();
        Log.d(TAG, "onStart() complete");
    }

    protected void onResume() {
        Log.d(TAG, "onResume() start");
        super.onResume();
        pythonWin.onResume();
        Log.d(TAG, "onResume() complete");
    }

    protected void onDestroy(){
        super.onDestroy();
        pythonWin.onDestory();
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        Log.d(TAG, "onActivityResult() start");
        super.onActivityResult(requestCode, resultCode, data);
        pythonWin.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult() complete");
    }

    public boolean onOptionsItemSelected(MenuItem menuitem) {
        boolean result;
        Log.d(TAG, "onOptionsItemSelected() start");
        result = pythonWin.onOptionsItemSelected(menuitem);
        Log.d(TAG, "onOptionsItemSelected() complete");
        return result;
    }

    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean result;
        Log.d(TAG, "onPrepareOptionsMenu() start");
        result = pythonWin.onPrepareOptionsMenu(menu);
        Log.d(TAG, "onPrepareOptionsMenu() complete");
        return result;
    }

    public void newActivity(long id){
        System.out.println("new activity!");
        startActivity(new Intent(this, WinActivity.class).putExtra("pythonWinId",id));
    }

    static {
        System.loadLibrary("native-lib");
    }
}
