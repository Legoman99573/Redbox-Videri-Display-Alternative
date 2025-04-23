package wiki.redbox.RedboxVideri.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.io.IOException
import java.net.URL

object FileUtil {

    private const val REQUEST_CODE_WRITE_STORAGE = 100

    // Save file to internal storage
    fun saveAsset(context: Context, filename: String, content: String) {
        File(context.filesDir, filename).writeText(content)
    }

    // Read file from internal storage
    fun readAsset(context: Context, filename: String): String {
        val file = File(context.filesDir, filename)
        return if (file.exists()) file.readText() else ""
    }

    // Download asset if needed
    fun downloadAssetIfNeeded(context: Context, assetUrl: String) {
        val filename = assetUrl.substringAfterLast("/")
        val file = File(context.filesDir, filename)

        // Request storage permission if needed
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !hasWritePermission(context)) {
            requestWritePermission(context)
            return
        }

        if (!file.exists()) {
            try {
                val input = URL(assetUrl).openStream()
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    // Check if the app has write permission
    private fun hasWritePermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        } else {
            true // For older versions, permissions are granted at install time
        }
    }

    // Request write permission
    private fun requestWritePermission(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(
                context as android.app.Activity,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                REQUEST_CODE_WRITE_STORAGE
            )
        }
    }
}
