package com.example.aptiready.ui.ads

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import com.example.aptiready.data.model.AdPlacement
import com.google.android.gms.ads.*

/** Owns one banner/listener. Paused or destroyed screens never issue a retry. */
class BannerAdWrapper(context: Context, private val canRequestAds: () -> Boolean) {
    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private val retry = AdRetryPolicy()
    private var adView: AdView? = null
    private var generation = 0
    private var host: ViewGroup? = null
    private var listener: View.OnLayoutChangeListener? = null
    private var retryTask: Runnable? = null
    private var placement: AdPlacement? = null
    private var onAdStateChanged: ((Boolean) -> Unit)? = null
    private var widthDp = 0
    private var loading = false
    private var loaded = false
    private var requestStartedAt = 0L
    private var paused = false

    fun loadAnchoredAdaptiveBanner(placement: AdPlacement, container: ViewGroup,
        onAdStateChanged: (Boolean) -> Unit) {
        if (!canRequestAds() || !AdConfig.isPlacementEnabled(placement)) { destroy(); return }
        if (host !== container || this.placement != placement) {
            destroy()
            host = container
            this.placement = placement
            listener = View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> measureAndLoad() }
            container.addOnLayoutChangeListener(listener)
        }
        this.onAdStateChanged = onAdStateChanged
        // Repeated state emissions must not hide an already loaded banner.
        if (loaded) {
            container.visibility = View.VISIBLE
            return
        }
        if (loading || paused) return
        container.visibility = View.INVISIBLE
        container.post { if (host === container) measureAndLoad() }
        container.requestLayout()
    }

    private fun measureAndLoad() {
        val container = host ?: return
        val currentPlacement = placement ?: return
        if (paused || !container.isAttachedToWindow || !canRequestAds() ||
            !AdConfig.isPlacementEnabled(currentPlacement)) return
        val width = ((container.width - container.paddingLeft - container.paddingRight) /
            container.resources.displayMetrics.density).toInt()
        if (loading && SystemClock.elapsedRealtime() - requestStartedAt >= 60_000L) {
            generation++
            loading = false
            clearAd()
            retry.recordFailure(SystemClock.elapsedRealtime(), true)
        }
        if (width <= 0 || (width == widthDp && (loading || loaded))) return
        if (!retry.canAttempt(SystemClock.elapsedRealtime())) { scheduleRetry(); return }
        cancelRetry()
        generation++
        val token = generation
        val configToken = AdConfig.revision.value
        clearAd()
        widthDp = width
        loading = true
        requestStartedAt = SystemClock.elapsedRealtime()
        fun valid() = token == generation && host === container && canRequestAds() &&
            configToken == AdConfig.revision.value && AdConfig.isPlacementEnabled(currentPlacement)
        val unitId = AdConfig.getAdUnitIdForPlacement(currentPlacement)
        AdLogger.d("BannerAdWrapper", "Requesting banner: placement=$currentPlacement, unitId=$unitId")
        val ad = AdView(container.context)
        adView = ad
        try {
            val shimmer = android.view.LayoutInflater.from(container.context)
                .inflate(com.example.aptiready.R.layout.layout_banner_ad_shimmer, container, false)
            container.removeAllViews()
            container.addView(shimmer)
            container.visibility = View.VISIBLE
            ad.adUnitId = unitId
            ad.setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(appContext, width))
            ad.adListener = object : AdListener() {
                override fun onAdLoaded() {
                    if (!valid() || adView !== ad) { ad.destroy(); return }
                    loading = false
                    loaded = true
                    cancelRetry()
                    retry.reset()
                    container.removeAllViews()
                    container.addView(ad)
                    container.visibility = View.VISIBLE
                    if (paused) ad.pause()
                    AdLogger.d("BannerAdWrapper", "Banner loaded: placement=$currentPlacement")
                    onAdStateChanged?.invoke(true)
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    AdLogger.w("BannerAdWrapper", "Banner load failed: placement=$currentPlacement, unitId=$unitId, activeRequest=${valid() && adView === ad}, $error")
                    if (!valid() || adView !== ad) return
                    loading = false
                    loaded = false
                    retry.recordFailure(SystemClock.elapsedRealtime(), error.code != AdRequest.ERROR_CODE_INVALID_REQUEST)
                    clearAd()
                    container.removeAllViews()
                    container.visibility = View.GONE
                    onAdStateChanged?.invoke(false)
                    scheduleRetry()
                }
            }
            ad.loadAd(AdRequest.Builder().build())
            scheduleRetry(60_000L)
        } catch (failure: Exception) {
            loading = false
            loaded = false
            clearAd()
            retry.recordFailure(SystemClock.elapsedRealtime(), true)
            container.removeAllViews()
            container.visibility = View.GONE
            AdLogger.w("BannerAdWrapper", "Banner request could not start: ${failure.message}")
            onAdStateChanged?.invoke(false)
            scheduleRetry()
        }
    }

    private fun scheduleRetry(minimumDelay: Long = 1_000L) {
        if (paused || retryTask != null || host == null) return
        val wait = retry.delayUntilAttempt(SystemClock.elapsedRealtime())
        if (wait == Long.MAX_VALUE) return
        val task = Runnable {
            retryTask = null
            val container = host ?: return@Runnable
            if (paused || !container.isAttachedToWindow || !canRequestAds()) return@Runnable
            container.visibility = View.INVISIBLE
            container.requestLayout()
            container.post { if (host === container) measureAndLoad() }
        }
        retryTask = task
        mainHandler.postDelayed(task, wait.coerceAtLeast(minimumDelay))
    }

    private fun cancelRetry() {
        retryTask?.let { mainHandler.removeCallbacks(it) }
        retryTask = null
    }

    private fun clearAd() {
        adView?.let { (it.parent as? ViewGroup)?.removeView(it); it.destroy() }
        adView = null
    }

    fun pause() {
        paused = true
        cancelRetry()
        adView?.pause()
    }

    fun resume() {
        paused = false
        if (canRequestAds()) {
            adView?.resume()
            if (!loaded && host != null) {
                val timeout = if (loading) (60_000L - (SystemClock.elapsedRealtime() - requestStartedAt)).coerceAtLeast(1_000L) else 1_000L
                scheduleRetry(timeout)
            }
        }
    }

    fun destroy() {
        generation++
        cancelRetry()
        listener?.let { host?.removeOnLayoutChangeListener(it) }
        listener = null
        clearAd()
        host?.removeAllViews()
        host?.visibility = View.GONE
        host = null
        placement = null
        onAdStateChanged = null
        widthDp = 0
        loading = false
        loaded = false
        retry.reset()
    }
}
