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
    private val slot = AdLoadSlot<NativeAd>(SystemClock::elapsedRealtime) { it.destroy() }
    private var callback: ((NativeAd?) -> Unit)? = null
    fun showShimmer(binding: NativeAdViewBinding, show: Boolean) {
        val shimmerView = binding.shimmerContainer as? com.facebook.shimmer.ShimmerFrameLayout
        if (show) {
            shimmerView?.startShimmer()
            shimmerView?.visibility = View.VISIBLE
            binding.cardNativeAd.visibility = View.GONE
        } else {
            shimmerView?.stopShimmer()
            shimmerView?.visibility = View.GONE
        }
    }

    fun loadAndBindNativeAd(placement: AdPlacement, binding: NativeAdViewBinding,
        onAdStateChanged: (Boolean) -> Unit) {
        showShimmer(binding, true)
        binding.root.visibility = View.VISIBLE
        loadFreshNativeAd(placement) { ad ->
            if (ad != null) {
                populateNativeAdView(ad, binding)
            } else {
                showShimmer(binding, false)
                binding.root.visibility = View.GONE
            }
            onAdStateChanged(ad != null)
        }
    }

    fun loadFreshNativeAd(placement: AdPlacement, onAdLoaded: (NativeAd?) -> Unit) {
        // Returning to a screen reuses a valid ad and preserves failed-request backoff.
        loadNativeAdOnly(placement, onAdLoaded)
    }

    fun loadNativeAdOnly(placement: AdPlacement, onAdLoaded: (NativeAd?) -> Unit) {
        if (!canRequestAds() || !AdConfig.isPlacementEnabled(placement)) {
            onAdLoaded(null)
            return
        }
        val unitId = AdConfig.getAdUnitIdForPlacement(placement)
        val key = "$unitId:${AdConfig.revision.value}"
        slot.cached(key)?.let { onAdLoaded(it); return }
        callback = onAdLoaded
        val token = slot.begin(key) ?: run { onAdLoaded(null); return }
        val config = AdConfig.revision.value
        fun allowed() = canRequestAds() && AdConfig.isPlacementEnabled(placement) && config == AdConfig.revision.value
        AdLogger.d("NativeAdLoader", "Requesting native: placement=$placement, unitId=$unitId")
        try {
            AdLoader.Builder(appContext, unitId).forNativeAd { ad ->
                if (!allowed()) { ad.destroy(); slot.fail(token, true); return@forNativeAd }
                if (slot.accept(token, ad)) {
                    AdLogger.d("NativeAdLoader", "Native loaded: placement=$placement, response=${ad.responseInfo}")
                    callback?.invoke(ad)
                }
            }.withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    AdLogger.w("NativeAdLoader", "Native failed: placement=$placement, unitId=$unitId, $error")
                    if (slot.fail(token, error.code != AdRequest.ERROR_CODE_INVALID_REQUEST)) callback?.invoke(null)
                }
            }).build().loadAd(AdRequest.Builder().build())
        } catch (failure: Exception) {
            if (slot.fail(token, true)) callback?.invoke(null)
            AdLogger.w("NativeAdLoader", "Native request failed: ${failure.message}")
        }
    }
    fun populateNativeAdView(nativeAd: NativeAd, binding: NativeAdViewBinding) {
        showShimmer(binding, false)
        binding.cardNativeAd.visibility = View.VISIBLE

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
        callback = null
        slot.clear()
    }
}
