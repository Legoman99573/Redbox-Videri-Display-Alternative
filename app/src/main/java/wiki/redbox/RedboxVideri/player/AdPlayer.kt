package wiki.redbox.RedboxVideri.player

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ImageView
import android.widget.VideoView
import com.bumptech.glide.Glide
import wiki.redbox.RedboxVideri.MainActivity
import wiki.redbox.RedboxVideri.R
import wiki.redbox.RedboxVideri.manager.AdManager
import java.io.File

class AdPlayer(private val context: Context) {

    private var ads = AdManager.getAds()
    private var currentIndex = 0
    private val handler = Handler(Looper.getMainLooper())

    private lateinit var imageView: ImageView
    private lateinit var videoView: VideoView

    fun startPlaying() {
        imageView = (context as MainActivity).findViewById(R.id.adImage)
        videoView = context.findViewById(R.id.adVideo)
        playNext()
    }

    private fun playNext() {
        if (ads.isEmpty()) return

        val ad = ads[currentIndex]
        currentIndex = (currentIndex + 1) % ads.size

        val file = File(context.filesDir, ad.path.substringAfterLast("/"))
        if (!file.exists()) {
            // Skip missing file
            handler.postDelayed({ playNext() }, 1000)
            return
        }

        if (ad.isVideo) {
            showVideo(file)
        } else {
            showImage(file, ad.durationMs.toLong())
        }
    }

    private fun showVideo(file: File) {
        imageView.visibility = View.GONE
        videoView.visibility = View.VISIBLE

        val uri = Uri.fromFile(file)
        videoView.setVideoURI(uri)
        videoView.setOnCompletionListener {
            handler.postDelayed({ playNext() }, 1000)
        }
        videoView.start()
    }

    private fun showImage(file: File, duration: Long) {
        videoView.stopPlayback()
        videoView.visibility = View.GONE
        imageView.visibility = View.VISIBLE

        Glide.with(context)
            .load(file)
            .into(imageView)

        handler.postDelayed({ playNext() }, duration)
    }
}