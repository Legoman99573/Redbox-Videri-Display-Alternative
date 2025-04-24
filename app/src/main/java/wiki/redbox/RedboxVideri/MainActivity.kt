package wiki.redbox.RedboxVideri

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import wiki.redbox.RedboxVideri.manager.AdManager
import wiki.redbox.RedboxVideri.player.AdPlayer

class MainActivity : AppCompatActivity() {

    private lateinit var adPlayer: AdPlayer
    private lateinit var statusText: TextView
    private lateinit var splashImageView: ImageView

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        setContentView(R.layout.activity_main)

        supportActionBar?.hide()

        window.decorView.systemUiVisibility = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            View.SYSTEM_UI_FLAG_IMMERSIVE or
                    View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        } else {
            View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        }

        // Initialize UI elements
        statusText = findViewById(R.id.statusText)
        splashImageView = findViewById(R.id.splashImageView)

        showSplashScreen()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // For Android 6.0 and above: use coroutines
            lifecycleScope.launch(Dispatchers.Main) {
                updateStatus("Initializing...")

                delay(1000)
                updateStatus("Downloading ad list...")

                AdManager.initialize(this@MainActivity)
                AdManager.fetchAdsIfNeeded { progress, total ->
                    updateStatus("Downloading asset $progress of $total...")
                }

                updateStatus("Loading ads...")
                delay(1000)

                startAdPlayback()
            }
        } else {
            // For Android 5.x and below: use traditional threading
            Thread {
                try {
                    runOnUiThread {
                        updateStatus("Initializing...")
                        showSplashImage()
                    }

                    Thread.sleep(1000)

                    runOnUiThread { updateStatus("Downloading ad list...") }

                    AdManager.initialize(this@MainActivity)
                    runBlocking {
                        AdManager.fetchAdsIfNeeded { progress, total ->
                            runOnUiThread {
                                updateStatus("Downloading asset $progress of $total...")
                            }
                        }
                    }

                    runOnUiThread {
                        updateStatus("Loading ads...")
                        Thread.sleep(1000)

                        startAdPlayback()
                    }
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            }.start()
        }
    }

    private fun startAdPlayback() {
        adPlayer = AdPlayer(this@MainActivity)
        adPlayer.startPlaying()
        splashImageView.visibility = View.GONE
        updateStatus("")
    }

    private fun showSplashScreen() {
        splashImageView.setImageResource(R.drawable.splash)
        splashImageView.visibility = View.VISIBLE
    }

    private fun showSplashImage() {
        splashImageView.post {
            splashImageView.setImageResource(R.drawable.splash)
            splashImageView.visibility = View.VISIBLE
        }
    }

    private fun updateStatus(message: String) {
        statusText.text = message
    }
}

