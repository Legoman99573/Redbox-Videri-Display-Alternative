package wiki.redbox.RedboxVideri.manager

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkInfo
import android.os.Build
import androidx.annotation.RequiresApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import wiki.redbox.RedboxVideri.model.Ad
import wiki.redbox.RedboxVideri.util.FileUtil
import java.net.URL

object AdManager {
    private var applicationContext: Context? = null
    private const val JSON_URL = "https://redboxapi.com/videri-ads/ads.json"

    private val ads = mutableListOf<Ad>()

    fun initialize(context: Context) {
        applicationContext = context.applicationContext
    }

    suspend fun fetchAdsIfNeeded(onProgress: ((current: Int, total: Int) -> Unit)? = null) {
        val context = applicationContext ?: return

        // Ensure device is connected to Wifi before downloading
        if (!isConnectedToWifi(context)) return

        val currentJson = try {
            FileUtil.readAsset(context, "ads_cache.json")
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }

        val newJson = try {
            withContext(Dispatchers.IO) {
                URL(JSON_URL).readText()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }

        try {
            if (newJson.isNotBlank() && newJson != currentJson) {
                FileUtil.saveAsset(context, "ads_cache.json", newJson)
                parseJson(newJson)
            } else if (ads.isEmpty() && currentJson.isNotBlank()) {
                parseJson(currentJson)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return
        }

        // Download the assets (ads) in the background
        val allAds = ads.toList()
        val total = allAds.size

        allAds.forEachIndexed { index, ad ->
            try {
                withContext(Dispatchers.IO) {
                    FileUtil.downloadAssetIfNeeded(context, ad.path)
                }
                onProgress?.invoke(index + 1, total)
            } catch (e: Exception) {
                e.printStackTrace() // Log and skip to the next ad
            }
        }
    }

    private fun parseJson(json: String) {
        val root = JSONObject(json)
        ads.clear()
        val images = root.getJSONObject("images")
        val videos = root.getJSONObject("videos")

        // Parse images
        for (key in images.keys()) {
            val item = images.getJSONObject(key)
            val duration = item.getJSONObject("duration")
            val durationMs = if (duration.getString("unit") == "seconds") {
                duration.getInt("duration") * 1000
            } else {
                duration.getInt("duration")
            }
            ads.add(Ad(item.getInt("id"), item.getString("path"), durationMs, isVideo = false))
        }

        // Parse videos
        for (key in videos.keys()) {
            val item = videos.getJSONObject(key)
            ads.add(Ad(item.getInt("id"), item.getString("path"), item.getInt("durationMs"), isVideo = true))
        }
    }

    // Check if the device is connected to Wi-Fi (compatible with both new and old API versions)
    @Suppress("DEPRECATION")
    private fun isConnectedToWifi(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        // For Android 6 (API 23) and above, use NetworkCapabilities
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        } else {
            // For older versions, use NetworkInfo
            val networkInfo: NetworkInfo? = connectivityManager.activeNetworkInfo
            return networkInfo?.type == ConnectivityManager.TYPE_WIFI && networkInfo.isConnected
        }
    }

    fun getAds(): List<Ad> = ads
}
