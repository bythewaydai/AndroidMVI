package com.dl.mvilib

import kotlinx.coroutines.flow.Flow

@InternalMavericksApi
interface MVIStateStore<S : Any> {
    val state: S
    val flow: Flow<S>
    fun get(block: (S) -> Unit)
    fun set(stateReducer: S.() -> S)
}
