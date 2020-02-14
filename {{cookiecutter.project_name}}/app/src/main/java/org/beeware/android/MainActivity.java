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
     * We store the MainActivity instance on the *class* so that we can easily access it from Python.
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

    private String getPythonBasePath() throws IOException {
        return this.getPythonBaseDir().getAbsolutePath();
    }

    private File getPythonBaseDir() throws IOException {
        return ensureDir(new File(getApplicationContext().getFilesDir().getAbsolutePath() + "/python/"));
    }

    private File getUserCodeDir() throws IOException {
        return ensureDir(new File(getApplicationContext().getFilesDir().getAbsolutePath() + "/user-code"));
    }

    private File getRubiconJavaInstallDir() throws IOException {
        return ensureDir(new File(getPythonBasePath() + "/rubicon-java/"));
    }


    private void unpackPython() throws IOException {
        String myAbi = Build.SUPPORTED_ABIS[0];
        Log.e("unpackPython", "abi is " + myAbi);
        unzipTo(new ZipInputStream(this.getAssets().open("pythonhome." + myAbi + ".zip")), this.getPythonBaseDir());
        unzipTo(new ZipInputStream(this.getAssets().open("rubicon.zip")), getRubiconJavaInstallDir());
        unpackAssetPrefix(getAssets(), "python", getUserCodeDir());
        makeExecutable(new File(this.getPythonBaseDir() + "/bin/python3"));
        makeExecutable(new File(this.getPythonBaseDir() + "/bin/python3.7"));
    }

    private void setPythonEnvVars() throws IOException, ErrnoException {
        Os.setenv("RUBICON_LIBRARY", this.getApplicationInfo().nativeLibraryDir + "/librubicon.so", true);
        Log.v("python home", this.getPythonBasePath());
        Context applicationContext = this.getApplicationContext();
        File cacheDir = applicationContext.getCacheDir();
        Os.setenv("TMPDIR", cacheDir.getAbsolutePath(), true);
        Os.setenv("LD_LIBRARY_PATH", this.getApplicationInfo().nativeLibraryDir, true);
        Os.setenv("PYTHONHOME", this.getPythonBasePath(), true);
        Os.setenv("ACTIVITY_CLASS_NAME", "org/beeware/android/MainActivity", true);
    }

    private void startPython() throws Exception {
        this.setPythonEnvVars();
        this.unpackPython();
        if (Python.init(this.getPythonBasePath(),
                this.getRubiconJavaInstallDir().getAbsolutePath() + ":" + this.getUserCodeDir().getAbsolutePath(),
                null) != 0) {
            throw new Exception("Unable to start Python interpreter.");
        }
        // Store the code in a file because Python.run() takes a filename.
        // We can't run app/__main__.py directly because it uses package-relative imports.
        String fullFilename = this.getPythonBasePath() + "/start_app.py";
        FileOutputStream fos = new FileOutputStream(fullFilename);
        fos.write("import {{cookiecutter.python_app_name}}.__main__".getBytes());
        fos.close();
        Python.run(fullFilename, new String[0]);
    }

    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.captureStdoutStderr();
        LinearLayout layout = new LinearLayout(this);
        this.setContentView(layout);
        singletonThis = this;
        try {
            this.startPython();
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
