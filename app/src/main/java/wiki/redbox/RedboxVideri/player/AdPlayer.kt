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

        if (ad.isVideo) {
            showVideo(ad.path)
        } else {
            showImage(ad.path, ad.durationMs.toLong())
        }
    }

    private fun showVideo(videoPath: String) {
        imageView.visibility = View.GONE
        videoView.visibility = View.VISIBLE

        videoView.setVideoURI(Uri.parse(videoPath))
        videoView.setOnCompletionListener {
            handler.postDelayed({ playNext() }, 1000)
        }
        videoView.start()
    }

    private fun showImage(imagePath: String, duration: Long) {
        videoView.stopPlayback()
        videoView.visibility = View.GONE
        imageView.visibility = View.VISIBLE

        Glide.with(context).load(imagePath).into(imageView)

        handler.postDelayed({ playNext() }, duration)
    }
}