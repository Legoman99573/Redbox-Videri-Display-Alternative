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
    private lateinit var splashImageView: ImageView // Added for splash screen display

    @SuppressLint("NewApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Force landscape orientation in case the manifest isn't enough
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        setContentView(R.layout.activity_main)

        // Remove action bar (if not already done in manifest)
        supportActionBar?.hide()

        // Hide the navigation bar and make the app fullscreen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_IMMERSIVE
                            or View.SYSTEM_UI_FLAG_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    )
        } else {
            window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    )
        }

        // Initialize UI elements
        statusText = findViewById(R.id.statusText)
        splashImageView = findViewById(R.id.splashImageView) // Add this to the layout

        // Show splash screen initially
        splashImageView.visibility = View.VISIBLE

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            lifecycleScope.launch {
                updateStatus("Initializing...")

                // Show splash screen
                splashImageView.setImageResource(R.drawable.splash)

                delay(1000)

                updateStatus("Downloading ad list...")
                AdManager.initialize(this@MainActivity)
                AdManager.fetchAdsIfNeeded { progress, total ->
                    updateStatus("Downloading asset $progress of $total...")
                }

                updateStatus("Loading ads...")
                delay(1000)

                // Ads finished, hide splash image and start playing ads
                splashImageView.visibility = View.GONE // Hide splash screen
                updateStatus("")

                adPlayer = AdPlayer(this@MainActivity)
                adPlayer.startPlaying()
            }
        } else {
            // For versions below Android 6, manually manage background tasks using Thread
            Thread {
                try {
                    runOnUiThread { updateStatus("Initializing...") }
                    runOnUiThread { splashImageView.setImageResource(R.drawable.splash) } // Show splash

                    Thread.sleep(1000)

                    runOnUiThread { updateStatus("Downloading ad list...") }
                    AdManager.initialize(this@MainActivity)

                    kotlinx.coroutines.runBlocking {
                        AdManager.fetchAdsIfNeeded { progress, total ->
                            runOnUiThread { updateStatus("Downloading asset $progress of $total...") }
                        }
                    }

                    runOnUiThread { updateStatus("Loading ads...") }
                    Thread.sleep(1000)

                    // Ads finished, hide splash image and start playing ads
                    runOnUiThread {
                        splashImageView.visibility = View.GONE // Hide splash screen
                        updateStatus("")
                    }

                    runOnUiThread {
                        adPlayer = AdPlayer(this@MainActivity)
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
