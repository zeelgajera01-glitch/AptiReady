package com.example.aptiready.ui.ads
import android.content.Context
import android.view.View
import android.view.ViewGroup
import com.example.aptiready.data.model.AdPlacement
import com.google.android.gms.ads.*

class BannerAdWrapper(context: Context, private val canRequestAds: () -> Boolean) {
    private val appContext = context.applicationContext
    private var adView: AdView? = null
    private var generation = 0
    private var host: ViewGroup? = null
    private var listener: View.OnLayoutChangeListener? = null
    private var widthDp = 0
    private var nextRetryAt = 0L

    fun loadAnchoredAdaptiveBanner(placement: AdPlacement, container: ViewGroup,
        onAdStateChanged: (Boolean) -> Unit) {
        if (!canRequestAds() || !AdConfig.isPlacementEnabled(placement)) { destroy(); return }
        if (host !== container) {
            nextRetryAt = 0L
            destroy()
            host = container
        } else if (android.os.SystemClock.elapsedRealtime() < nextRetryAt) {
            return
        }
        val token = generation
        val configToken = AdConfig.revision.value
        fun valid() = token == generation && canRequestAds() &&
            configToken == AdConfig.revision.value && AdConfig.isPlacementEnabled(placement)
        fun measureAndLoad() {
            if (!valid()) return
            val width = ((container.width - container.paddingLeft - container.paddingRight) /
                container.resources.displayMetrics.density).toInt()
            if (width <= 0 || width == widthDp) return
            widthDp = width
            adView?.let { (it.parent as? ViewGroup)?.removeView(it); it.destroy() }
            val ad = AdView(appContext)
            adView = ad
            ad.adUnitId = AdConfig.getAdUnitIdForPlacement(placement)
            ad.setAdSize(
                AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(
                    appContext,
                    width
                )
            )
            ad.adListener = object : AdListener() {
                override fun onAdLoaded() {
                    if (!valid() || adView !== ad) { ad.destroy(); return }
                    container.removeAllViews()
                    container.addView(ad)
                    container.visibility = View.VISIBLE
                    onAdStateChanged(true)
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    if (!valid() || adView !== ad) return

                    nextRetryAt =
                        android.os.SystemClock.elapsedRealtime() + 5_000L

                    this@BannerAdWrapper.destroy()
                    onAdStateChanged(false)
                }
            }
            if (valid()) ad.loadAd(AdRequest.Builder().build()) else ad.destroy()
        }
        listener = View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> measureAndLoad() }
        container.addOnLayoutChangeListener(listener)
        container.visibility = View.INVISIBLE // Measure the real container, never the screen width.
        container.post { if (valid()) measureAndLoad() }
        container.requestLayout()
    }
    fun pause() { adView?.pause() }
    fun resume() { if (canRequestAds()) adView?.resume() }
    fun destroy() {
        generation++
        listener?.let { host?.removeOnLayoutChangeListener(it) }
        listener = null
        adView?.let { (it.parent as? ViewGroup)?.removeView(it); it.destroy() }
        host?.visibility = View.GONE
        host = null
        adView = null
        widthDp = 0
    }
}
