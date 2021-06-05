package org.beeware.android;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.system.ErrnoException;
import android.system.Os;
import android.util.Log;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipInputStream;

import org.beeware.rubicon.Python;

import {{ cookiecutter.package_name }}.{{ cookiecutter.module_name }}.R;

import static org.beeware.android.Helpers.ensureDirExists;
import static org.beeware.android.Helpers.unpackAssetPrefix;
import static org.beeware.android.Helpers.unzipTo;

public class MainActivity extends AppCompatActivity {

    // To profile app launch, use `adb -s MainActivity`; look for "onCreate() start" and "onResume() completed".
    private String TAG = "MainActivity";
    private static IPythonApp pythonApp;

    /**
     * This method is called by `app.__main__` over JNI in Python when the BeeWare
     * app launches.
     *
     * @param app
     */
    @SuppressWarnings("unused")
    public static void setPythonApp(IPythonApp app) {
        pythonApp = app;
    }

    /**
     * We store the MainActivity instance on the *class* so that we can easily
     * access it from Python.
     */
    public static MainActivity singletonThis;

    private Map<String, File> getPythonPaths() throws IOException {
        Map paths = new HashMap<String, File>();
        File base = new File(getApplicationContext().getFilesDir().getAbsolutePath() + "/python/");

        // `stdlib` is used as PYTHONHOME.
        File stdlib = new File(base.getAbsolutePath() + "/stdlib/");
        ensureDirExists(stdlib);
        paths.put("stdlib", stdlib);

        // We cache the stdlib by checking the contents of this file.
        paths.put("stdlib-last-filename", new File(base.getAbsolutePath() + "/stdlib.last-filename"));

        // Put `user_code` into paths so we can unpack the assets into it.
        paths.put("user_code", new File(base.getAbsolutePath() + "/user_code/"));

        // `app` and `app_packages` store user code and user code dependencies,
        // respectively. These paths exist within the `python/` assets tree.
        File app = new File(base.getAbsolutePath() + "/user_code/app/");
        paths.put("app", app);

        File app_packages = new File(base.getAbsolutePath() + "/user_code/app_packages/");
        paths.put("app_packages", app_packages);
        return paths;
    }

    private void unpackPython(Map<String, File> paths) throws IOException {
        // Try to find `lastUpdateTime` on disk; compare it to actual `lastUpdateTime` from package manager.
        // https://developer.android.com/reference/android/content/pm/PackageInfo.html#lastUpdateTime
        Context context = this.getApplicationContext();
        File lastUpdateTimeFile = new File(context.getCacheDir(), "last-update-time");
        String storedLastUpdateTime = null;
        String actualLastUpdateTime = null;

        if (lastUpdateTimeFile.exists()) {
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                    new FileInputStream(lastUpdateTimeFile), StandardCharsets.UTF_8
                )
            );
            storedLastUpdateTime = reader.readLine();
        }
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            actualLastUpdateTime = String.valueOf(packageInfo.lastUpdateTime);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Unable to find package; using default actualLastUpdateTime");
        }
        if (storedLastUpdateTime != null && storedLastUpdateTime.equals(actualLastUpdateTime)) {
            Log.d(TAG, "unpackPython() complete: Exiting early due to lastUpdateTime match: " + storedLastUpdateTime);
            return;
        }

        String myAbi = Build.SUPPORTED_ABIS[0];
        File pythonHome = paths.get("stdlib");

        // Get list of assets under the stdlib/ directory, filtering for our ABI.
        String[] stdlibAssets = this.getAssets().list("stdlib");
        String pythonHomeZipFilename = null;
        String abiZipSuffix = myAbi + ".zip";
        for (int i = 0; i < stdlibAssets.length; i++) {
            String asset = stdlibAssets[i];
            if (asset.startsWith("pythonhome.") && asset.endsWith(abiZipSuffix)) {
                pythonHomeZipFilename = "stdlib/" + asset;
                break;
            }
        }
        // Unpack stdlib, except if it's missing, abort; and if we already unpacked a
        // file of the same name, then skip it. That way, the filename can serve as
        // a cache identifier.
        if (pythonHomeZipFilename == null) {
            throw new RuntimeException(
                "Unable to find file matching pythonhome.* and " + abiZipSuffix
            );
        }
        File stdlibLastFilenamePath = paths.get("stdlib-last-filename");
        boolean cacheOk = false;
        if (stdlibLastFilenamePath.exists()) {
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                    new FileInputStream(stdlibLastFilenamePath), StandardCharsets.UTF_8
                )
            );
            String stdlibLastFilename = reader.readLine();
            if (stdlibLastFilename.equals(pythonHomeZipFilename)) {
                cacheOk = true;
            }
        }
        if (cacheOk) {
            Log.d(TAG, "Python stdlib already exists for " + pythonHomeZipFilename);
        } else {
            Log.d(TAG, "Unpacking Python stdlib " + pythonHomeZipFilename);
            unzipTo(new ZipInputStream(this.getAssets().open(pythonHomeZipFilename)), pythonHome);
            BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(
                    new FileOutputStream(stdlibLastFilenamePath), StandardCharsets.UTF_8
                )
            );
            writer.write(pythonHomeZipFilename, 0, pythonHomeZipFilename.length());
            writer.close();
        }

        File userCodeDir = paths.get("user_code");
        Log.d(TAG, "Unpacking Python assets to " + userCodeDir.getAbsolutePath());
        unpackAssetPrefix(getAssets(), "python", userCodeDir);
        if (actualLastUpdateTime != null) {
            Log.d(TAG, "Replacing old lastUpdateTime = " + storedLastUpdateTime + " with actualLastUpdateTime = " + actualLastUpdateTime);
            BufferedWriter timeWriter = new BufferedWriter(
                new OutputStreamWriter(
                    new FileOutputStream(lastUpdateTimeFile), StandardCharsets.UTF_8
                )
            );
            timeWriter.write(actualLastUpdateTime, 0, actualLastUpdateTime.length());
            timeWriter.close();
        }
        Log.d(TAG, "unpackPython() complete");
    }

    private void setPythonEnvVars(String pythonHome) throws IOException, ErrnoException {
        Log.d(TAG, "setPythonEnvVars() start");
        Log.v(TAG, "pythonHome=" + pythonHome);
        Context applicationContext = this.getApplicationContext();
        File cacheDir = applicationContext.getCacheDir();

        // Set stdout and stderr to be unbuffered. We are overriding stdout/stderr and would
        // prefer to avoid delays.
        Os.setenv("PYTHONUNBUFFERED", "1", true);

        // Tell rubicon-java's Python code where to find the C library, to access it via ctypes.
        Os.setenv("RUBICON_LIBRARY", this.getApplicationInfo().nativeLibraryDir + "/librubicon.so", true);
        Os.setenv("TMPDIR", cacheDir.getAbsolutePath(), true);
        Os.setenv("LD_LIBRARY_PATH", this.getApplicationInfo().nativeLibraryDir, true);
        Os.setenv("PYTHONHOME", pythonHome, true);
        Os.setenv("ACTIVITY_CLASS_NAME", "org/beeware/android/MainActivity", true);
        Log.d(TAG, "setPythonEnvVars() complete");
    }

    private void startPython(Map<String, File> paths) throws Exception {
        this.unpackPython(paths);
        String pythonHome = paths.get("stdlib").getAbsolutePath();
        this.setPythonEnvVars(pythonHome);

        Log.d(TAG, "Computing Python version.");
        // Compute Python version number so that we can make sure it's first on sys.path when we
        // configure sys.path with Python.init().
        String[] libpythons = new File(this.getApplicationInfo().nativeLibraryDir).list(
                new FilenameFilter() {
                    @Override
                    public boolean accept(File file, String s) {
                        return s.startsWith("libpython") && s.endsWith(".so");
                    }
                }
        );
        if (libpythons.length == 0) {
            throw new Exception("Unable to compute Python version");
        }
        String pythonVersion = libpythons[0].replace("libpython", "").replaceAll("m*.so", "");
        Log.d(TAG, "Computed Python version: " + pythonVersion);

        // `app` is the last item in the sysPath list.
        String sysPath = (pythonHome + "/lib/python" + pythonVersion + "/") + ":"
                + paths.get("app_packages").getAbsolutePath() + ":" + paths.get("app").getAbsolutePath();
        if (Python.init(pythonHome, sysPath, null) != 0) {
            throw new Exception("Unable to start Python interpreter.");
        }
        Log.d(TAG, "Python.init() complete");

        // Run the app's main module, similar to `python -m`.
        Log.d(TAG, "Python.run() start");
        Python.run("{{ cookiecutter.module_name }}", new String[0]);
        Log.d(TAG, "Python.run() end");
        Log.d(TAG, "startPython() end");
    }

    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate() start");
        this.captureStdoutStderr();
        Log.d(TAG, "onCreate(): captured stdout/stderr");
        // Change away from the splash screen theme to the app theme.
        setTheme(R.style.AppTheme);
        super.onCreate(savedInstanceState);
        LinearLayout layout = new LinearLayout(this);
        this.setContentView(layout);
        singletonThis = this;
        try {
            Map<String, File> paths = getPythonPaths();
            this.startPython(paths);
        } catch (Exception e) {
            Log.e(TAG, "Failed to create Python app", e);
            return;
        }
        Log.d(TAG, "user code onCreate() start");
        pythonApp.onCreate();
        Log.d(TAG, "user code onCreate() complete");
        Log.d(TAG, "onCreate() complete");
    }

    protected void onStart() {
        Log.d(TAG, "onStart() start");
        super.onStart();
        pythonApp.onStart();
        Log.d(TAG, "onStart() complete");
    }

    protected void onResume() {
        Log.d(TAG, "onResume() start");
        super.onResume();
        pythonApp.onResume();
        Log.d(TAG, "onResume() complete");
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        Log.d(TAG, "onActivityResult() start");
        super.onActivityResult(requestCode, resultCode, data);
        pythonApp.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult() complete");
    }

    private native boolean captureStdoutStderr();

    static {
        System.loadLibrary("native-lib");
    }
}
