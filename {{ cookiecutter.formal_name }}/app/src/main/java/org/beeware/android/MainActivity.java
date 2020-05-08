package org.beeware.android;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.system.ErrnoException;
import android.system.Os;
import android.util.Log;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipInputStream;

import org.beeware.rubicon.Python;
import org.jetbrains.annotations.Nullable;

import static org.beeware.android.HelpersKt.makeExecutable;
import static org.beeware.android.HelpersKt.unpackAssetPrefix;
import static org.beeware.android.HelpersKt.unzipTo;

public class MainActivity extends AppCompatActivity {

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

    private Map<String, File> getPythonDirs() throws IOException {
        Map dirs = new HashMap<String, File>();
        File base = new File(getApplicationContext().getFilesDir().getAbsolutePath() + "/python/");

        // `stdlib` is used as PYTHONHOME.
        File stdlib = new File(base.getAbsolutePath() + "/stdlib/");
        ensureDir(stdlib);
        dirs.put("stdlib", stdlib);

        // `rubicon_java` is used to the required Rubicon module.
        File rubicon_java = new File(base.getAbsolutePath() + "/rubicon-java/");
        ensureDir(rubicon_java);
        dirs.put("rubicon_java", rubicon_java);

        // Put `user_code` into dirs so we can unpack the assets into it.
        dirs.put("user_code", new File(base.getAbsolutePath() + "/user_code/"));

        // `app` and `app_packages` store user code and user code dependencies,
        // respectively. These paths exist within the `python/` assets tree.
        File app = new File(base.getAbsolutePath() + "/user_code/app/");
        dirs.put("app", app);

        File app_packages = new File(base.getAbsolutePath() + "/user_code/app_packages/");
        dirs.put("app_packages", app_packages);
        return dirs;
    }

    private void unpackPython(Map<String, File> dirs) throws IOException {
        String myAbi = Build.SUPPORTED_ABIS[0];
        File pythonHome = dirs.get("stdlib");

        Log.d("unpackPython", "Unpacking Python with ABI " + myAbi + " to " + pythonHome.getAbsolutePath());
        unzipTo(new ZipInputStream(this.getAssets().open("pythonhome." + myAbi + ".zip")), pythonHome);
        makeExecutable(new File(pythonHome.getAbsolutePath() + "/bin/python3"));
        makeExecutable(new File(pythonHome.getAbsolutePath() + "/bin/python3.7"));
        File rubicon_java = dirs.get("rubicon_java");

        Log.d("unpackPython", "Unpacking rubicon-java to " + rubicon_java.getAbsolutePath());
        unzipTo(new ZipInputStream(this.getAssets().open("rubicon.zip")), rubicon_java);
        File userCodeDir = dirs.get("user_code");

        Log.d("unpackPython", "Unpacking Python assets to base dir " + userCodeDir.getAbsolutePath());
        unpackAssetPrefix(getAssets(), "python", userCodeDir);
    }

    private void setPythonEnvVars(String pythonHome) throws IOException, ErrnoException {
        Log.v("python home", pythonHome);
        Context applicationContext = this.getApplicationContext();
        File cacheDir = applicationContext.getCacheDir();

        Os.setenv("RUBICON_LIBRARY", this.getApplicationInfo().nativeLibraryDir + "/librubicon.so", true);
        Os.setenv("TMPDIR", cacheDir.getAbsolutePath(), true);
        Os.setenv("LD_LIBRARY_PATH", this.getApplicationInfo().nativeLibraryDir, true);
        Os.setenv("PYTHONHOME", pythonHome, true);
        Os.setenv("ACTIVITY_CLASS_NAME", "org/beeware/android/MainActivity", true);
    }

    private void startPython(Map<String, File> dirs) throws Exception {
        this.unpackPython(dirs);
        String pythonHome = dirs.get("stdlib").getAbsolutePath();
        this.setPythonEnvVars(pythonHome);

        // `app` is the last item in the sysPath list.
        String sysPath = (pythonHome + "/lib/python3.7/") + ":" + dirs.get("rubicon_java").getAbsolutePath() + ":"
                + dirs.get("app_packages").getAbsolutePath() + ":" + dirs.get("app").getAbsolutePath();
        if (Python.init(pythonHome, sysPath, null) != 0) {
            throw new Exception("Unable to start Python interpreter.");
        }

        // Run the app's main module, similar to `python -m`.
        Python.run("{{cookiecutter.module_name}}", new String[0]);
    }

    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.captureStdoutStderr();
        LinearLayout layout = new LinearLayout(this);
        this.setContentView(layout);
        singletonThis = this;
        try {
            Map<String, File> dirs = getPythonDirs();
            this.startPython(dirs);
        } catch (Exception e) {
            Log.e("MainActivity", "Failed to create Python app", e);
            return;
        }
        pythonApp.onCreate();
    }

    protected void onStart() {
        super.onStart();
        pythonApp.onStart();
    }

    protected void onResume() {
        super.onResume();
        pythonApp.onResume();
    }

    private native boolean captureStdoutStderr();

    static {
        System.loadLibrary("native-lib");
    }
}
