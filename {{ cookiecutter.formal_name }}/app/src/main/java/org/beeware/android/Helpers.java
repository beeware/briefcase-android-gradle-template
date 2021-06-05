package org.beeware.android;

import android.content.res.AssetManager;
import android.util.Log;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Helpers {
    public static final String TAG = "Helpers";

    public static void unpackAssetPrefix(AssetManager assets, String assetPrefix, File outputDir) throws IOException {
        Log.d(TAG, "Clearing out path " + outputDir.getAbsolutePath());
        deleteRecursively(outputDir);
        String [] list = assets.list(assetPrefix);
        if (list == null) {
            throw new IOException("Unable to unpack assets");
        }
        if (list.length == 0) {
            throw new IOException("No assets at prefix " + assetPrefix);
        }
        for (String file: list) {
            unpackAssetPath(assets, assetPrefix + file.toString(), assetPrefix.length(), outputDir);
        }
    }

    private static void ensureDir(File dir) throws IOException  {
        if (!dir.exists()) {
            boolean success = dir.mkdirs();
            if (!success) {
                throw new IOException("Failed to mkdir ${dir.getAbsolutePath()}");
            }
        }
    }

    private static void copy(InputStream source, File targetPath) throws IOException {
        ensureDir(targetPath.getParentFile());
        byte[] buffer = new byte[4096 * 64];
        int len = source.read(buffer);
        FileOutputStream target = new FileOutputStream(targetPath);
        while (len != -1) {
            target.write(buffer, 0, len);
            len = source.read(buffer);
        }
        target.close();
        Log.d(TAG, "Created " + targetPath.getAbsolutePath());
    }

    private static void unpackAssetPath(AssetManager assets, String assetPath, int assetPrefixLength, File outputDir) throws IOException {
        String [] subPaths = assets.list(assetPath);
        if (subPaths == null) {
            throw new IOException("Unable to list assets at path " + assetPath);
        }
        if (subPaths.length == 0) {
            // It's a file. Copy it.
            File outputFile = new File(outputDir.getAbsolutePath() + "/" + assetPath.substring(assetPrefixLength));
            copy(assets.open(assetPath), outputFile);
        } else {
            for (String subPath: subPaths) {
                unpackAssetPath(assets, assetPath + "/" + subPath, assetPrefixLength, outputDir);
            }
        }
    }

    private static boolean deleteRecursively(File dir) {
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
            Log.d(TAG, "deleting recursively");
            deleteRecursively(outputDir);
            Log.d(TAG, "deleting recursively done");
        }
        if (!outputDir.mkdirs()) {
            throw new IOException("Unable to mkdir " + outputDir.getAbsolutePath());
        }
        ZipEntry zipEntry = inputStream.getNextEntry();
        byte[] buf = new byte[1024 * 1024 * 4];
        while (zipEntry != null) {
            File outputFile = new File(outputDir.getAbsolutePath() + "/" + zipEntry);
            if (zipEntry.isDirectory()) {
                Log.d(TAG, "creating dir " + outputFile.getAbsolutePath());
                boolean result = outputFile.mkdirs();
                if (!result) {
                    Log.d("unzipTo", "mkdirs result = " + result);
                }
                zipEntry = inputStream.getNextEntry();
                continue;
            }
            Log.d(TAG, "about to create file " + outputFile.getAbsolutePath());
            FileOutputStream fos = new FileOutputStream(outputFile.getAbsolutePath());
            int len = inputStream.read(buf);
            while (len > 0) {
                fos.write(buf, 0, len);
                len = inputStream.read(buf);
            }
            fos.close();
            zipEntry = inputStream.getNextEntry();
        }
        inputStream.closeEntry();
        inputStream.close();
    }
}
