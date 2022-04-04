package org.beeware.android;

import android.content.res.AssetManager;
import android.util.Log;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Helpers {
    public static final String TAG = "Briefcase";

    public static void unpackAssetPrefix(AssetManager assets, String assetPrefix, File outputDir) throws IOException {
        Log.d(TAG, "Clearing out old assets");
        deleteRecursively(outputDir);

        String [] list = assets.list(assetPrefix);
        if (list == null) {
            throw new IOException("Unable to obtain asset list");
        }
        if (list.length == 0) {
            throw new IOException("No assets at prefix " + assetPrefix);
        }
        for (String file: list) {
            unpackAssetPath(assets, assetPrefix + "/" + file.toString(), assetPrefix.length(), outputDir);
        }
    }

    public static void ensureDirExists(File dir) throws IOException  {
        if (!dir.exists()) {
            Log.d(TAG, "Creating dir " + dir.getAbsolutePath());
            if (!dir.mkdirs()) {
                throw new IOException("Failed to mkdir " + dir.getAbsolutePath());
            }
        }
    }

    private static void copyAsset(InputStream source, File targetPath) throws IOException {
        Log.d(TAG, "Copying asset " + targetPath.getAbsolutePath());
        byte[] buffer = new byte[4096 * 64];
        int len = source.read(buffer);
        FileOutputStream target = new FileOutputStream(targetPath);
        while (len != -1) {
            target.write(buffer, 0, len);
            len = source.read(buffer);
        }
        target.close();
    }

    private static void unpackAssetPath(AssetManager assets, String assetPath, int assetPrefixLength, File outputDir) throws IOException {
        String [] subPaths = assets.list(assetPath);
        if (subPaths == null) {
            throw new IOException("Unable to list assets at path " + assetPath);
        }
        if (subPaths.length == 0) {
            // It's a file. Copy it.
            File outputFile = new File(outputDir.getAbsolutePath() + "/" + assetPath.substring(assetPrefixLength));
            ensureDirExists(outputFile.getParentFile());
            copyAsset(assets.open(assetPath), outputFile);
        } else {
            for (String subPath: subPaths) {
                unpackAssetPath(assets, assetPath + "/" + subPath, assetPrefixLength, outputDir);
            }
        }
    }

    private static boolean deleteRecursively(File dir) {
        Log.d(TAG, "Deleting " + dir.getAbsolutePath());
        File[] allContents = dir.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                deleteRecursively(file);
            }
        }
        return dir.delete();
    }

    public static void unzipTo(ZipInputStream inputStream, File outputDir) throws IOException {
        if (outputDir.exists()) {
            Log.d(TAG, "Clearing out old zip artefacts");
            deleteRecursively(outputDir);
        }
        ensureDirExists(outputDir);
        ZipEntry zipEntry = inputStream.getNextEntry();
        byte[] buf = new byte[1024 * 1024 * 4];
        while (zipEntry != null) {
            File outputFile = new File(outputDir.getAbsolutePath() + "/" + zipEntry);
            if (zipEntry.isDirectory()) {
                Log.d(TAG, "Unpacking dir " + outputFile.getAbsolutePath());
                if (!outputFile.mkdirs()) {
                    throw new IOException("Unable to mkdirs " + outputFile.getAbsolutePath());
                }
            } else {
                Log.d(TAG, "Unpacking file " + outputFile.getAbsolutePath());
                FileOutputStream fos = new FileOutputStream(outputFile.getAbsolutePath());
                int len = inputStream.read(buf);
                while (len > 0) {
                    fos.write(buf, 0, len);
                    len = inputStream.read(buf);
                }
                fos.close();
            }
            zipEntry = inputStream.getNextEntry();
        }
        inputStream.closeEntry();
        inputStream.close();
    }
}
