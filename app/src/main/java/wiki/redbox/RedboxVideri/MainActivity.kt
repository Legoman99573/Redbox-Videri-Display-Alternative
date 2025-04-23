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
import wiki.redbox.RedboxVideri.manager.AdManager
import wiki.redbox.RedboxVideri.player.AdPlayer

class MainActivity : AppCompatActivity() {

    private lateinit var adPlayer: AdPlayer
    private lateinit var statusText: TextView
    private lateinit var splashImageView: ImageView

    @SuppressLint("NewApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Force landscape orientation in case the manifest isn't enough
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

        statusText = findViewById(R.id.statusText)
        splashImageView = findViewById(R.id.splashImageView)

        splashImageView.visibility = View.VISIBLE
        splashImageView.setImageResource(R.drawable.splash)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            lifecycleScope.launch {
                updateStatus("Initializing...")
                delay(1000)

                updateStatus("Downloading ad list...")
                AdManager.initialize(this@MainActivity)
                AdManager.fetchAdsIfNeeded { progress, total ->
                    updateStatus("Downloading asset $progress of $total...")
                }

                updateStatus("Loading ads...")
                delay(1000)

                adPlayer = AdPlayer(this@MainActivity) {
                    runOnUiThread {
                        splashImageView.visibility = View.GONE
                        updateStatus("")
                    }
                }
                adPlayer.startPlaying()
            }
        } else {
            Thread {
                try {
                    runOnUiThread {
                        updateStatus("Initializing...")
                        splashImageView.setImageResource(R.drawable.splash)
                    }
                    Thread.sleep(1000)

                    runOnUiThread { updateStatus("Downloading ad list...") }
                    AdManager.initialize(this@MainActivity)

                    kotlinx.coroutines.runBlocking {
                        AdManager.fetchAdsIfNeeded { progress, total ->
                            runOnUiThread { updateStatus("Downloading asset $progress of $total...") }
                        }
                    }

                    runOnUiThread {
                        updateStatus("Loading ads...")
                    }
                    Thread.sleep(1000)

                    runOnUiThread {
                        adPlayer = AdPlayer(this@MainActivity) {
                            runOnUiThread {
                                splashImageView.visibility = View.GONE
                                updateStatus("")
                            }
                        }
                        adPlayer.startPlaying()
                    }

                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            }.start()
        }
    }

    private fun updateStatus(message: String) {
        runOnUiThread {
            statusText.text = message
        }
    }
}