package wiki.redbox.RedboxVideri.manager

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkInfo
import android.os.Build
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

        // Download the assets (ads)
        val allAds = ads.toList()
        val total = allAds.size

        allAds.forEachIndexed { index, ad ->
            try {
                withContext(Dispatchers.IO) {
                    FileUtil.downloadAssetIfNeeded(context, ad.path)
                }
                onProgress?.invoke(index + 1, total)
            } catch (e: Exception) {
                e.printStackTrace() // Log and continue
            }
        }
    }

    private fun parseJson(json: String) {
        val root = JSONObject(json)
        ads.clear()

        fun parseMedia(jsonObject: JSONObject, isVideo: Boolean) {
            for (key in jsonObject.keys()) {
                val item = jsonObject.getJSONObject(key)
                val id = item.getInt("id")
                val path = item.getString("path")
                val durationMs = if (isVideo) {
                    item.getInt("durationMs")
                } else {
                    val durationObj = item.getJSONObject("duration")
                    val unit = durationObj.getString("unit")
                    val value = durationObj.getInt("duration")
                    if (unit == "seconds") value * 1000 else value
                }
                ads.add(Ad(id, path, durationMs, isVideo))
            }
        }

        root.optJSONObject("images")?.let { parseMedia(it, isVideo = false) }
        root.optJSONObject("videos")?.let { parseMedia(it, isVideo = true) }
    }

    @Suppress("DEPRECATION")
    private fun isConnectedToWifi(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        } else {
            val networkInfo: NetworkInfo? = connectivityManager.activeNetworkInfo
            networkInfo?.type == ConnectivityManager.TYPE_WIFI && networkInfo.isConnected
        }
    }

    fun getAds(): List<Ad> = ads
}