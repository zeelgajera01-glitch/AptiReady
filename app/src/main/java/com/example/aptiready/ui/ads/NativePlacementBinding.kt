package com.example.aptiready.ui.ads
import android.content.Context
import androidx.lifecycle.*
import com.example.aptiready.data.model.AdPlacement
import com.google.android.gms.ads.nativead.NativeAd
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/** View-lifecycle owner of one NativeAd. Adapters only borrow the ad for rendering. */
class NativePlacementBinding(
    context: Context,
    private val owner: LifecycleOwner,
    private val consent: ConsentManager,
    private val placement: AdPlacement,
    private val render: (NativeAd?) -> Unit,
    count: Flow<Int>
) : DefaultLifecycleObserver {
    private val loader = NativeAdLoader(context.applicationContext) { consent.canLoad(placement) }
    private var epoch = -1L
    private var eligibleCount = false
    private var closed = false
    init {
        owner.lifecycle.addObserver(this)
        owner.lifecycleScope.launch {
            combine(consent.adState, count, owner.lifecycle.currentStateFlow) { state, size, lifecycle ->
                Triple(state, size, lifecycle)
            }.collect { (state, size, lifecycle) ->
                if (closed) return@collect
                eligibleCount = size > 0 && AdEligibilityPolicy.isNativeAdListEligible(placement, size)
                if (epoch != state.generation || !eligibleCount || !consent.canLoad(placement)) {
                    render(null)
                    loader.destroy()
                    epoch = state.generation
                }
                if (eligibleCount && consent.canLoad(placement) && lifecycle.isAtLeast(Lifecycle.State.STARTED)) {
                    loader.loadNativeAdOnly(placement) { ad ->
                        if (!closed && eligibleCount && consent.canLoad(placement) &&
                            epoch == consent.adState.value.generation &&
                            owner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) render(ad)
                    }
                }
            }
        }
    }
    override fun onDestroy(owner: LifecycleOwner) {
        closed = true
        render(null)
        loader.destroy()
        owner.lifecycle.removeObserver(this)
    }
}
