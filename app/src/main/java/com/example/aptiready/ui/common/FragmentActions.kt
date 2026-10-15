package com.example.aptiready.ui.common

import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.util.WeakHashMap

private val dialogs = WeakHashMap<Fragment, androidx.appcompat.app.AlertDialog>()

fun MaterialAlertDialogBuilder.showOnce(fragment: Fragment) {
    val owner = fragment.viewLifecycleOwnerLiveData.value ?: return
    if (fragment.parentFragmentManager.isStateSaved ||
        !owner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED) ||
        dialogs[fragment]?.isShowing == true) return
    val dialog = create()
    dialogs[fragment] = dialog
    val observer = object : DefaultLifecycleObserver {
        override fun onDestroy(owner: LifecycleOwner) { dialog.dismiss() }
    }
    owner.lifecycle.addObserver(observer)
    dialog.setOnDismissListener {
        if (dialogs[fragment] === dialog) dialogs.remove(fragment)
        owner.lifecycle.removeObserver(observer)
    }
    dialog.show()
}

fun Fragment.observeActionError(error: StateFlow<String?>) {
    viewLifecycleOwner.lifecycleScope.launch {
        viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            error.collect { message ->
                if (message != null) context?.let { Toast.makeText(it, message, Toast.LENGTH_LONG).show() }
            }
        }
    }
}
