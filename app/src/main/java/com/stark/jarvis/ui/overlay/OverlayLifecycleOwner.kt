package com.stark.jarvis.ui.overlay

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner

/**
 * A standalone owner of [Lifecycle], [ViewModelStore] and [SavedStateRegistry] so
 * a [androidx.compose.ui.platform.ComposeView] can run **outside** an Activity —
 * i.e. attached directly to the [android.view.WindowManager] by a Service.
 *
 * Compose requires all three "ViewTree" owners to be present on the host view or
 * it throws at composition time. Drive the state transitions to mirror the
 * overlay's real visibility.
 */
class OverlayLifecycleOwner :
    LifecycleOwner,
    ViewModelStoreOwner,
    SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = savedStateController.savedStateRegistry

    /** Call right before adding the view to the window. */
    fun onCreate() {
        savedStateController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
    }

    /** Call once the view is attached and should be actively composing. */
    fun onResume() {
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
    }

    /** Call after removing the view from the window; clears ViewModels. */
    fun onDestroy() {
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        store.clear()
    }
}
