package com.example.nativeadvancedexample

import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import com.example.nativeadvancedexample.databinding.ActivityMainBinding
import com.example.nativeadvancedexample.databinding.AdUnifiedBinding
import com.google.android.gms.ads.*
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView

private const val TAG = "MainActivity"
const val ADMOB_AD_UNIT_ID = "ca-app-pub-3940256099942544/2247696110"
var currentNativeAd: NativeAd? = null

class MainActivity : AppCompatActivity() {
    private lateinit var mainActivityBinding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainActivityBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(mainActivityBinding.root)

        // Log the Mobile Ads SDK version.
        Log.d(TAG, "Google Mobile Ads SDK Version: " + MobileAds.getVersion())

        // Initialize the Mobile Ads SDK.
        MobileAds.initialize(this) {}

        mainActivityBinding.refreshButton.setOnClickListener { refreshAd() }

        refreshAd()
    }

    /**
     * Populates a [NativeAdView] object with data from a given [NativeAd].
     *
     * @param nativeAd the object containing the ad's assets
     * @param unifiedAdBinding the binding object of the layout that has NativeAdView as the root view
     */
    private fun populateNativeAdView(nativeAd: NativeAd, unifiedAdBinding: AdUnifiedBinding) {
        val nativeAdView = unifiedAdBinding.root

        // Set the media view.
        nativeAdView.mediaView = unifiedAdBinding.adMedia

        // Set other ad assets.
        nativeAdView.headlineView = unifiedAdBinding.adHeadline
        nativeAdView.bodyView = unifiedAdBinding.adBody
        nativeAdView.callToActionView = unifiedAdBinding.adCallToAction
        nativeAdView.iconView = unifiedAdBinding.adAppIcon
        nativeAdView.priceView = unifiedAdBinding.adPrice
        nativeAdView.starRatingView = unifiedAdBinding.adStars
        nativeAdView.storeView = unifiedAdBinding.adStore
        nativeAdView.advertiserView = unifiedAdBinding.adAdvertiser

        // The headline and media content are guaranteed to be in every UnifiedNativeAd.
        unifiedAdBinding.adHeadline.text = nativeAd.headline
        nativeAd.mediaContent?.let { unifiedAdBinding.adMedia.setMediaContent(it) }

        // These assets aren't guaranteed to be in every UnifiedNativeAd, so it's important to
        // check before trying to display them.
        if (nativeAd.body == null) {
            unifiedAdBinding.adBody.visibility = View.INVISIBLE
        } else {
            unifiedAdBinding.adBody.visibility = View.VISIBLE
            unifiedAdBinding.adBody.text = nativeAd.body
        }

        if (nativeAd.callToAction == null) {
            unifiedAdBinding.adCallToAction.visibility = View.INVISIBLE
        } else {
            unifiedAdBinding.adCallToAction.visibility = View.VISIBLE
            unifiedAdBinding.adCallToAction.text = nativeAd.callToAction
        }

        if (nativeAd.icon == null) {
            unifiedAdBinding.adAppIcon.visibility = View.GONE
        } else {
            unifiedAdBinding.adAppIcon.setImageDrawable(nativeAd.icon?.drawable)
            unifiedAdBinding.adAppIcon.visibility = View.VISIBLE
        }

        if (nativeAd.price == null) {
            unifiedAdBinding.adPrice.visibility = View.INVISIBLE
        } else {
            unifiedAdBinding.adPrice.visibility = View.VISIBLE
            unifiedAdBinding.adPrice.text = nativeAd.price
        }

        if (nativeAd.store == null) {
            unifiedAdBinding.adStore.visibility = View.INVISIBLE
        } else {
            unifiedAdBinding.adStore.visibility = View.VISIBLE
            unifiedAdBinding.adStore.text = nativeAd.store
        }

        if (nativeAd.starRating == null) {
            unifiedAdBinding.adStars.visibility = View.INVISIBLE
        } else {
            unifiedAdBinding.adStars.rating = nativeAd.starRating!!.toFloat()
            unifiedAdBinding.adStars.visibility = View.VISIBLE
        }

        if (nativeAd.advertiser == null) {
            unifiedAdBinding.adAdvertiser.visibility = View.INVISIBLE
        } else {
            unifiedAdBinding.adAdvertiser.text = nativeAd.advertiser
            unifiedAdBinding.adAdvertiser.visibility = View.VISIBLE
        }

        // This method tells the Google Mobile Ads SDK that you have finished populating your
        // native ad view with this native ad.
        nativeAdView.setNativeAd(nativeAd)

        // Get the video controller for the ad. One will always be provided, even if the ad doesn't
        // have a video asset.
        val mediaContent = nativeAd.mediaContent
        val vc = mediaContent?.videoController

        // Updates the UI to say whether or not this ad has a video asset.
        if (vc != null && mediaContent.hasVideoContent()) {
            // Create a new VideoLifecycleCallbacks object and pass it to the VideoController. The
            // VideoController will call methods on this object when events occur in the video
            // lifecycle.
            vc.videoLifecycleCallbacks =
                object : VideoController.VideoLifecycleCallbacks() {
                    override fun onVideoEnd() {
                        // Publishers should allow native ads to complete video playback before
                        // refreshing or replacing them with another ad in the same UI location.
                        mainActivityBinding.refreshButton.isEnabled = true
                        mainActivityBinding.videostatusText.text = "Video status: Video playback has ended."
                        super.onVideoEnd()
                    }
                }
        } else {
            mainActivityBinding.videostatusText.text = "Video status: Ad does not contain a video asset."
            mainActivityBinding.refreshButton.isEnabled = true
        }
    }

    /**
     * Creates a request for a new native ad based on the boolean parameters and calls the
     * corresponding "populate" method when one is successfully returned.
     */
    private fun refreshAd() {
        mainActivityBinding.refreshButton.isEnabled = false

        val builder = AdLoader.Builder(this, ADMOB_AD_UNIT_ID)

        builder.forNativeAd { nativeAd ->
            // OnUnifiedNativeAdLoadedListener implementation.
            // If this callback occurs after the activity is destroyed, you must call
            // destroy and return or you may get a memory leak.
            var activityDestroyed = false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                activityDestroyed = isDestroyed
            }
            if (activityDestroyed || isFinishing || isChangingConfigurations) {
                nativeAd.destroy()
                return@forNativeAd
            }
            // You must call destroy on old ads when you are done with them,
            // otherwise you will have a memory leak.
            currentNativeAd?.destroy()
            currentNativeAd = nativeAd
            val unifiedAdBinding = AdUnifiedBinding.inflate(layoutInflater)
            populateNativeAdView(nativeAd, unifiedAdBinding)
            mainActivityBinding.adFrame.removeAllViews()
            mainActivityBinding.adFrame.addView(unifiedAdBinding.root)
        }

        val videoOptions =
            VideoOptions.Builder().setStartMuted(mainActivityBinding.startMutedCheckbox.isChecked).build()

        val adOptions = NativeAdOptions.Builder().setVideoOptions(videoOptions).build()

        builder.withNativeAdOptions(adOptions)

        val adLoader =
            builder
                .withAdListener(
                    object : AdListener() {
                        override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                            val error =
                                """
           domain: ${loadAdError.domain}, code: ${loadAdError.code}, message: ${loadAdError.message}
          """"
                            mainActivityBinding.refreshButton.isEnabled = true
                            Toast.makeText(
                                this@MainActivity,
                                "Failed to load native ad with error $error",
                                Toast.LENGTH_SHORT
                            )
                                .show()
                        }
                    }
                )
                .build()

        adLoader.loadAd(AdRequest.Builder().build())

        mainActivityBinding.videostatusText.text = ""
    }

    override fun onDestroy() {
        currentNativeAd?.destroy()
        super.onDestroy()
    }
}