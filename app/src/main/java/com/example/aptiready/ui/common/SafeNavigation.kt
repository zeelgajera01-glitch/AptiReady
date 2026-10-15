package com.example.aptiready.ui.common

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.FragmentNavigator

private fun Fragment.ownsDestination(): Boolean {
    val owner = viewLifecycleOwnerLiveData.value ?: return false
    if (!isAdded || parentFragmentManager.isStateSaved ||
        !owner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) return false
    val destination = findNavController().currentDestination as? FragmentNavigator.Destination
    return destination?.className == javaClass.name
}

/** Ignore queued taps and late callbacks after the originating screen has left. */
fun Fragment.navigateSafely(actionId: Int, args: Bundle? = null) {
    if (!ownsDestination()) return
    val controller = findNavController()
    if (controller.currentDestination?.getAction(actionId) == null) return
    controller.navigate(actionId, args)
}

fun Fragment.navigateUpSafely() {
    if (ownsDestination()) findNavController().navigateUp()
}

fun Fragment.popBackStackSafely(destinationId: Int, inclusive: Boolean) {
    if (ownsDestination()) findNavController().popBackStack(destinationId, inclusive)
}
