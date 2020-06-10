package org.beeware.android;

import android.content.Context;
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
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipInputStream;

import org.beeware.rubicon.Python;
import org.jetbrains.annotations.Nullable;

import static org.beeware.android.HelpersKt.unpackAssetPrefix;
import static org.beeware.android.HelpersKt.unzipTo;

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

    private File ensureDir(File dir) throws IOException {
        if (dir.exists()) {
            return dir;
        }
        boolean mkdirResult = dir.mkdirs();
        if (!mkdirResult) {
            throw new IOException("Unable to make directory " + dir.getAbsolutePath());
        }
        return dir;
    }

    private Map<String, File> getPythonPaths() throws IOException {
        Map paths = new HashMap<String, File>();
        File base = new File(getApplicationContext().getFilesDir().getAbsolutePath() + "/python/");

        // `stdlib` is used as PYTHONHOME.
        File stdlib = new File(base.getAbsolutePath() + "/stdlib/");
        ensureDir(stdlib);
        paths.put("stdlib", stdlib);

        // We cache the stdlib by checking the contents of this file.
        paths.put("stdlib-last-filename", new File(base.getAbsolutePath() + "/stdlib.last-filename"));

        // `rubicon_java` is used to the required Rubicon module.
        File rubicon_java = new File(base.getAbsolutePath() + "/rubicon-java/");
        ensureDir(rubicon_java);
        paths.put("rubicon_java", rubicon_java);

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
            throw new RuntimeException("Unable to find file matching pythonhome.* and " +
                                       abiZipSuffix);
        }
        File stdlibLastFilenamePath = paths.get("stdlib-last-filename");
        boolean cacheOk = false;
        if (stdlibLastFilenamePath.exists()) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(new
FileInputStream(stdlibLastFilenamePath), StandardCharsets.UTF_8));
            String stdlibLastFilename = reader.readLine();
            if (stdlibLastFilename.equals(pythonHomeZipFilename)) {
                cacheOk = true;
            }
        }
        if (cacheOk) {
            Log.d(TAG, "Skipping unpack of Python stdlib due to cache hit on " + pythonHomeZipFilename);
        } else {
            Log.d(TAG, "Unpacking Python stdlib due to cache miss on " + pythonHomeZipFilename);
            unzipTo(new ZipInputStream(this.getAssets().open(pythonHomeZipFilename)), pythonHome);
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(stdlibLastFilenamePath), StandardCharsets.UTF_8));
            writer.write(pythonHomeZipFilename, 0, pythonHomeZipFilename.length());
            writer.close();
        }

        File rubicon_java = paths.get("rubicon_java");
        Log.d(TAG, "Unpacking rubicon-java to " + rubicon_java.getAbsolutePath());
        unzipTo(new ZipInputStream(this.getAssets().open("rubicon.zip")), rubicon_java);

        File userCodeDir = paths.get("user_code");
        Log.d(TAG, "Unpacking Python assets to base dir " + userCodeDir.getAbsolutePath());
        unpackAssetPrefix(getAssets(), "python", userCodeDir);
        Log.d(TAG, "unpackPython() complete");
    }

    private void setPythonEnvVars(String pythonHome) throws IOException, ErrnoException {
        Log.d(TAG, "setPythonEnvVars() start");
        Log.v(TAG, "pythonHome=" + pythonHome);
        Context applicationContext = this.getApplicationContext();
        File cacheDir = applicationContext.getCacheDir();

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

        // `app` is the last item in the sysPath list.
        String sysPath = (pythonHome + "/lib/python3.7/") + ":" + paths.get("rubicon_java").getAbsolutePath() + ":"
                + paths.get("app_packages").getAbsolutePath() + ":" + paths.get("app").getAbsolutePath();
        if (Python.init(pythonHome, sysPath, null) != 0) {
            throw new Exception("Unable to start Python interpreter.");
        }
        Log.d(TAG, "Python.init() complete");

        // Run the app's main module, similar to `python -m`.
        Log.d(TAG, "Python.run() start");
        Python.run("{{cookiecutter.module_name}}", new String[0]);
        Log.d(TAG, "Python.run() end");
        Log.d(TAG, "startPython() end");
    }

    protected void onCreate(@Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreate() start");
        super.onCreate(savedInstanceState);
        this.captureStdoutStderr();
        Log.d(TAG, "onCreate(): captured stdout/stderr");
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

    private native boolean captureStdoutStderr();

    static {
        System.loadLibrary("native-lib");
    }
}
