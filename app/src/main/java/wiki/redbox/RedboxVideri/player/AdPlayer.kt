package wiki.redbox.RedboxVideri.player

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.view.View
import android.widget.ImageView
import android.widget.VideoView
import kotlinx.coroutines.*
import wiki.redbox.RedboxVideri.MainActivity
import wiki.redbox.RedboxVideri.R
import wiki.redbox.RedboxVideri.manager.AdManager
import wiki.redbox.RedboxVideri.model.Ad
import java.io.File

class AdPlayer(
    private val context: Context,
    private val onFirstAdStarted: (() -> Unit)? = null
) {

    private val ads = AdManager.getAds()
    private var currentIndex = 0
    private var isPlaying = false
    private var hasStarted = false

    private val videoView: VideoView by lazy { (context as? MainActivity)?.findViewById(R.id.adVideo)!! }
    private val imageView: ImageView by lazy { (context as? MainActivity)?.findViewById(R.id.adImage)!! }

    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    fun startPlaying() {
        isPlaying = true
        playNextAd()
    }

    private fun playNextAd() {
        if (!isPlaying || ads.isEmpty()) return

        val ad = ads[currentIndex]

        if (ad.isVideo) {
            showVideo(ad)
        } else {
            showImage(ad)
        }
    }

    private fun showVideo(ad: Ad) {
        imageView.visibility = View.GONE
        videoView.visibility = View.VISIBLE

        val file = File(context.filesDir, ad.path)
        videoView.setVideoURI(Uri.fromFile(file))

        videoView.setOnPreparedListener { mediaPlayer: MediaPlayer ->
            mediaPlayer.isLooping = false
            if (!hasStarted) {
                hasStarted = true
                onFirstAdStarted?.invoke()
            }
            videoView.start()
        }

        videoView.setOnCompletionListener {
            nextAd()
        }
    }

    private fun showImage(ad: Ad) {
        videoView.visibility = View.GONE
        imageView.visibility = View.VISIBLE

        val file = File(context.filesDir, ad.path)
        imageView.setImageURI(Uri.fromFile(file))

        if (!hasStarted) {
            hasStarted = true
            onFirstAdStarted?.invoke()
        }

        coroutineScope.launch {
            delay(ad.durationMs.toLong())
            nextAd()
        }
    }

    private fun nextAd() {
        currentIndex = (currentIndex + 1) % ads.size
        playNextAd()
    }

    fun stop() {
        isPlaying = false
        coroutineScope.cancel()
        videoView.stopPlayback()
    }
}