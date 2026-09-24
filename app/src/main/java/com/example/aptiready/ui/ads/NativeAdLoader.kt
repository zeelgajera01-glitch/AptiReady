package com.example.aptiready.ui.ads
import android.content.Context
import android.os.SystemClock
import android.view.View
import com.example.aptiready.data.model.AdPlacement
import com.example.aptiready.databinding.NativeAdViewBinding
import com.google.android.gms.ads.*
import com.google.android.gms.ads.nativead.NativeAd

class NativeAdLoader(context: Context, private val canRequestAds: () -> Boolean) {
    private val appContext = context.applicationContext
    private var currentNativeAd: NativeAd? = null
    private var generation = 0
    private var loading = false
    private var nextRetry = 0L
    fun loadAndBindNativeAd(placement: AdPlacement, binding: NativeAdViewBinding,
        onAdStateChanged: (Boolean) -> Unit) {
        loadNativeAdOnly(placement) { ad ->
            if (ad != null) populateNativeAdView(ad, binding)
            binding.root.visibility = if (ad == null) View.GONE else View.VISIBLE
            onAdStateChanged(ad != null)
        }
    }
    fun loadNativeAdOnly(placement: AdPlacement, onAdLoaded: (NativeAd?) -> Unit) {
        if (!canRequestAds() || !AdConfig.isPlacementEnabled(placement)) return
        currentNativeAd?.let { onAdLoaded(it); return }
        if (loading || SystemClock.elapsedRealtime() < nextRetry) return
        loading = true
        val token = ++generation
        val configToken = AdConfig.revision.value
        fun valid() = token == generation && configToken == AdConfig.revision.value &&
            canRequestAds() && AdConfig.isPlacementEnabled(placement)
        AdLoader.Builder(appContext, AdConfig.getAdUnitIdForPlacement(placement))
            .forNativeAd { ad ->
                if (!valid()) { ad.destroy(); return@forNativeAd }
                loading = false
                currentNativeAd = ad
                onAdLoaded(ad)
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    if (!valid()) return
                    loading = false
                    nextRetry = SystemClock.elapsedRealtime() + 30_000L
                    onAdLoaded(null)
                }
            }).build().loadAd(AdRequest.Builder().build())
    }
    fun populateNativeAdView(nativeAd: NativeAd, binding: NativeAdViewBinding) {
        val nativeAdView = binding.nativeAdView

        // 1. Headline (Required)
        nativeAdView.headlineView = binding.adHeadline
        binding.adHeadline.text = nativeAd.headline ?: "Sponsored"

        // 2. Body (Optional)
        nativeAdView.bodyView = binding.adBody
        if (!nativeAd.body.isNullOrBlank()) {
            binding.adBody.text = nativeAd.body
            binding.adBody.visibility = View.VISIBLE
        } else {
            binding.adBody.visibility = View.GONE
        }

        // 3. Call To Action (Optional)
        nativeAdView.callToActionView = binding.adCallToAction
        if (!nativeAd.callToAction.isNullOrBlank()) {
            binding.adCallToAction.text = nativeAd.callToAction
            binding.adCallToAction.visibility = View.VISIBLE
        } else {
            binding.adCallToAction.visibility = View.GONE
        }

        // 4. Icon (Optional)
        nativeAdView.iconView = binding.adAppIcon
        val icon = nativeAd.icon
        if (icon?.drawable != null) {
            binding.adAppIcon.setImageDrawable(icon.drawable)
            binding.adAppIcon.visibility = View.VISIBLE
        } else {
            binding.adAppIcon.setImageDrawable(null)
            binding.adAppIcon.visibility = View.GONE
        }

        // 5. Advertiser (Optional)
        nativeAdView.advertiserView = binding.adAdvertiser
        if (!nativeAd.advertiser.isNullOrBlank()) {
            binding.adAdvertiser.text = nativeAd.advertiser
            binding.adAdvertiser.visibility = View.VISIBLE
        } else {
            binding.adAdvertiser.text = "Sponsored"
            binding.adAdvertiser.visibility = View.VISIBLE
        }

        // 6. MediaView (Optional)
        nativeAdView.mediaView = binding.adMedia
        val mediaContent = nativeAd.mediaContent
        if (mediaContent != null) {
            binding.adMedia.mediaContent = mediaContent
            binding.adMedia.visibility = View.VISIBLE
        } else {
            binding.adMedia.visibility = View.GONE
        }

        // 7. Register NativeAd object with NativeAdView
        nativeAdView.setNativeAd(nativeAd)
    }


    fun destroy() {
        generation++
        loading = false
        nextRetry = 0
        currentNativeAd?.destroy()
        currentNativeAd = null
    }
}
