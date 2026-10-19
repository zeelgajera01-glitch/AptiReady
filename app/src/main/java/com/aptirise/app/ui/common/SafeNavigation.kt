package com.aptirise.app.ui.common

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.FragmentNavigator
import com.aptirise.app.R
import com.aptirise.app.ui.navigation.ActivityRoutes
import com.aptirise.app.ui.navigation.ScreenActivity

private fun Fragment.ownsDestination(): Boolean {
    val owner = viewLifecycleOwnerLiveData.value ?: return false
    if (!isAdded || requireActivity().isFinishing || parentFragmentManager.isStateSaved ||
        !owner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) return false
    val destination = findNavController().currentDestination as? FragmentNavigator.Destination
    return destination?.className == javaClass.name
}

/** Ignore queued taps and late callbacks after the originating screen has left. */
fun Fragment.navigateSafely(actionId: Int, args: Bundle? = null) {
    if (!ownsDestination()) return
    val controller = findNavController()
    val action = controller.currentDestination?.getAction(actionId) ?: return
    val host = activity as? ScreenActivity
    if (host == null) {
        controller.navigate(actionId, args)
        return
    }
    val arguments = Bundle().apply {
        action.defaultArguments?.let { putAll(it) }
        args?.let { putAll(it) }
    }
    host.openScreen(action.destinationId, arguments,
        replace = action.navOptions?.isPopUpToInclusive() == true && action.navOptions?.popUpToId == host.screenId,
        clearTask = false)
}

fun Fragment.navigateUpSafely() {
    if (!ownsDestination()) return
    (activity as? ScreenActivity)?.goBack() ?: findNavController().navigateUp()
}

fun Fragment.popBackStackSafely(destinationId: Int, inclusive: Boolean) {
    if (!ownsDestination()) return
    val host = activity as? ScreenActivity
    if (host != null) {
        if (inclusive && host.screenId == destinationId) host.goBack()
        else host.returnToScreen(destinationId)
    } else findNavController().popBackStack(destinationId, inclusive)
}
