package org.beeware.android

import android.content.res.AssetManager
import android.util.Log
import java.io.*
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.zip.ZipInputStream

const val TAG = "Helpers"

fun makeExecutable(executable: File) {
    if (executable.exists()) {
        executable.setExecutable(true)
        executable.setReadable(true)
    } else {
        throw IOException("Executable file is missing. Aborting.")
    }
    // See if magical restorecon saves the day
    val pb =
            ProcessBuilder("restorecon", executable.absolutePath)
    println("Running restorecon...")
    val process = pb.start()
    val errCode = process.waitFor()
    println("Restorecon finished. result=${errCode}")
}

fun unpackAssetPrefix(assets: AssetManager, assetPrefix: String, outputDir: File) {
    Log.d(TAG, "Clearing out path ${outputDir.absolutePath}")
    outputDir.deleteRecursively()
    var list: Array<out String> = assets.list(assetPrefix)
            ?: throw IOException("Unable to unpack assets")
    if (list.isEmpty()) {
        throw IOException("No assets at prefix $assetPrefix")
    }
    for (file in list) {
        unpackAssetPath(assets, "${assetPrefix}/${file}", assetPrefix.length, outputDir)
    }
}

private fun ensureDir(dir: File) {
    if (!dir.exists()) {
        val success = dir.mkdirs()
        if (!success) {
            throw IOException("Failed to mkdir ${dir.absolutePath}")
        }
    }
}

private fun copy(source: InputStream, targetPath: File) {
    ensureDir(targetPath.parentFile!!)
    val buffer = ByteArray(4096 * 64)
    var len: Int = source.read(buffer)
    val target = FileOutputStream(targetPath)
    while (len != -1) {
        target.write(buffer, 0, len)
        len = source.read(buffer)
    }
    target.close()
    Log.d(TAG, "Created ${targetPath.absolutePath}")
}

private fun unpackAssetPath(assets: AssetManager, assetPath: String, assetPrefixLength: Int, outputDir: File) {
    var subPaths = assets.list("$assetPath")
            ?: throw IOException("Unable to list assets at path $assetPath/")
    if (subPaths.isEmpty()) {
        // It's a file. Copy it.
        val outputFile = File("${outputDir.absolutePath}/${assetPath.substring(assetPrefixLength)}")
        copy(assets.open(assetPath), outputFile)
    } else {
        for (subPath in subPaths) {
            unpackAssetPath(assets, "$assetPath/$subPath", assetPrefixLength, outputDir)
        }
    }
}

fun unzipTo(inputStream: ZipInputStream, outputDir: File) {
    if (outputDir.exists()) {
        Log.d(TAG, "deleting recursively")
        outputDir.deleteRecursively()
        Log.d(TAG, "deleting recursively done")
    }
    if (!outputDir.mkdirs()) {
        throw IOException("Unable to mkdir ${outputDir.absolutePath}")
    }
    var zipEntry = inputStream.nextEntry
    val buf = ByteArray(1024 * 1024 * 4)
    while (zipEntry != null) {
        val outputFile = File("${outputDir.absolutePath}/${zipEntry}")
        if (zipEntry.isDirectory) {
            Log.d(TAG, "creating dir ${outputFile.absolutePath}")
            val result = outputFile.mkdirs()
            if (!result) {
                Log.d("unzipTo", "mkdirs result = $result")
            }
            zipEntry = inputStream.nextEntry
            continue
        }
        Log.d(TAG, "about to create file ${outputFile.absolutePath}")
        val fos = FileOutputStream(outputFile.absolutePath)
        var len: Int
        while (inputStream.read(buf).also { len = it } > 0) {
            fos.write(buf, 0, len)
        }
        fos.close()
        zipEntry = inputStream.nextEntry
    }
    inputStream.closeEntry()
    inputStream.close()
}
