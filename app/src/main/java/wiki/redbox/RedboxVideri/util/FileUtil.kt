package wiki.redbox.RedboxVideri.util

import android.content.Context
import android.widget.Toast
import java.io.File
import java.io.IOException
import java.net.URL

object FileUtil {

    fun saveAsset(context: Context, filename: String, content: String) {
        try {
            File(context.filesDir, filename).writeText(content)
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(context, "Failed to save $filename", Toast.LENGTH_SHORT).show()
        }
    }

    fun readAsset(context: Context, filename: String): String {
        val file = File(context.filesDir, filename)
        return try {
            if (file.exists()) file.readText() else ""
        } catch (e: IOException) {
            e.printStackTrace()
            ""
        }
    }

    fun downloadAssetIfNeeded(context: Context, assetUrl: String) {
        val filename = assetUrl.substringAfterLast("/")
        val file = File(context.filesDir, filename)

        if (!file.exists()) {
            try {
                URL(assetUrl).openStream().use { input ->
                    file.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
                Toast.makeText(context, "Failed to download $filename", Toast.LENGTH_SHORT).show()
            }
        }
    }
}