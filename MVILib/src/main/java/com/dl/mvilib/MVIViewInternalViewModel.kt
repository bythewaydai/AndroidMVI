package com.dl.mvilib

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class MVIViewInternalViewModel(state: SavedStateHandle) : ViewModel() {
    internal val lastDeliveredStates: ConcurrentHashMap<String, Any?> = ConcurrentHashMap<String, Any?>()
    internal val activeSubscriptions: MutableSet<String> = mutableSetOf()
    internal val MVIViewId = state[PERSISTED_VIEW_ID_KEY] ?: generateUniqueId().also { id ->
        state[PERSISTED_VIEW_ID_KEY] = id
    }

    private fun generateUniqueId() = "MVIView_" + UUID.randomUUID().toString()

    companion object {
        private const val PERSISTED_VIEW_ID_KEY = "mavericks:persisted_view_id"
    }
}
