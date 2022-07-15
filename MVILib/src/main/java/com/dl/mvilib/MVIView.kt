package com.dl.mvilib

import android.os.Handler
import android.os.Looper
import android.os.Message
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlin.reflect.KProperty1

// Set of [MavericksView identity hash codes that have a pending invalidate.
private val pendingInvalidates = HashSet<Int>()
private val handler = Handler(Looper.getMainLooper()) { message ->
    val view = message.obj as MVIView
    pendingInvalidates.remove(System.identityHashCode(view))
    if (view.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) view.invalidate()
    true
}

/**
 * If any callbacks to run [MavericksView.invalidate] have been posted with [MavericksView.postInvalidate]
 * then this cancels them.
 *
 * This may be useful if you have manually run [MavericksView.invalidate] and want to avoid the overhead
 * of having it run again on the next frame.
 */
fun MVIView.cancelPendingInvalidates() {
    val viewHashCode = System.identityHashCode(this)
    if (pendingInvalidates.remove(viewHashCode)) {
        // the hashcode is used as the "what" in the message
        handler.removeMessages(viewHashCode)
    }
}

interface MVIView : LifecycleOwner {
    /**
     * Override this to supply a globally unique id for this MvRxView. If your [MVIView] is being recreated due to
     * a lifecycle event (e.g. rotation) you should assign a consistent id. Likely this means you should save the id
     * in onSaveInstance state. The viewId will not be accessed until a subscribe method is called.
     * Accessing mvrxViewId before calling super.onCreate() will cause a crash.
     */
    val mvrxViewId: String
        get() = MVIViewInternalViewModel.MVIViewId

    val MVIViewInternalViewModel: MVIViewInternalViewModel
        get() = when (this) {
            is ViewModelStoreOwner -> ViewModelProvider(this).get(MVIViewInternalViewModel::class.java)
            else -> error(
                "If your MVIView is not a ViewModelStoreOwner, you must implement MVIViewInternalViewModel " +
                    "and return a MVIViewInternalViewModel that is unique to this view and persistent across its entire lifecycle."
            )
        }

    /**
     * Override this to handle any state changes from [MVIViewModel]s created through MvRx Fragment delegates.
     */
    fun invalidate()

    /**
     * The [LifecycleOwner] to use when making new subscriptions. You may want to return different owners depending
     * on what state your [MVIView] is in.
     *
     * Fragments are handled by the default implementation using their standard lifecycle owner for subscriptions made
     * in `onCreate` (cleared in `onDestroy`), or their _view_ lifecycle owner for subscriptions made in or
     * after `onCreateView` (cleared in `onDestroyView`). Using [Fragment.getViewLifecycleOwner] is necessary to
     * retrieve the view's lifecycle as early as `onCreateView`. This method throws an `IllegalStateException` when view
     * lifecycle is unavailable (eg. _before_ `onCreateView`) which is caught as a signal to fall back to the fragment's
     * standard lifecycle owner.
     *
     * For non-Fragments the default [subscriptionLifecycleOwner] is the same as the MvRxView's standard lifecycle owner.
     */
    val subscriptionLifecycleOwner: LifecycleOwner
        get() = try {
            (this as? Fragment)?.viewLifecycleOwner ?: this
        } catch (e: IllegalStateException) {
            this
        }

    fun postInvalidate() {
        if (pendingInvalidates.add(System.identityHashCode(this@MVIView))) {
            handler.sendMessage(Message.obtain(handler, System.identityHashCode(this@MVIView), this@MVIView))
        }
    }

    /**
     * Return a [UniqueOnly] delivery mode with a unique id for this fragment. In rare circumstances, if you
     * make two identical subscriptions with the same (or all) properties in this fragment, provide a customId
     * to avoid collisions.
     *
     * @param customId An additional custom id to identify this subscription. Only necessary if there are two subscriptions
     * in this fragment with exact same properties (i.e. two subscribes, or two selectSubscribes with the same properties).
     */
    fun uniqueOnly(customId: String? = null): UniqueOnly {
        return UniqueOnly(listOfNotNull(mvrxViewId, UniqueOnly::class.simpleName, customId).joinToString("_"))
    }

    /**
     * Subscribes to all state updates for the given viewModel.
     *
     * @param deliveryMode If [UniqueOnly] when this [MVIView] goes from a stopped to started lifecycle a state value
     * will only be emitted if the state changed. This is useful for transient views that should only
     * be shown once (toasts, poptarts), or logging. Most other views should use false, as when a view is destroyed
     * and recreated the previous state is necessary to recreate the view.
     *
     * Use [uniqueOnly] to automatically create a [UniqueOnly] mode with a unique id for this view.
     *
     * Default: [RedeliverOnStart].
     * @param action supports cooperative cancellation. The previous action will be cancelled if it is not completed before
     * the next one is emitted.
     */
    fun <S : BaseUIState> BaseViewModel<S>.onEach(
        deliveryMode: DeliveryMode = RedeliverOnStart,
        action: suspend (S) -> Unit
    ) =
        _internal(subscriptionLifecycleOwner, deliveryMode, action)

    /**
     * Subscribes to state changes for only a specific property and calls the action with
     * only that single property.
     *
     * @param deliveryMode If [UniqueOnly], when this [MavericksView] goes from a stopped to start lifecycle a state value
     * will only be emitted if the state changed. This is useful for transient views that should only
     * be shown once (toasts, poptarts), or logging. Most other views should use false, as when a view is destroyed
     * and recreated the previous state is necessary to recreate the view.
     *
     * Use [uniqueOnly] to automatically create a [UniqueOnly] mode with a unique id for this view.
     *
     * Default: [RedeliverOnStart].
     * @param action supports cooperative cancellation. The previous action will be cancelled if it is not completed before
     * the next one is emitted.
     */
    fun <S : BaseUIState, A> BaseViewModel<S>.onEach(
        prop1: KProperty1<S, A>,
        deliveryMode: DeliveryMode = RedeliverOnStart,
        action: suspend (A) -> Unit
    ) = _internal1(subscriptionLifecycleOwner, prop1, deliveryMode, action)

    /**
     * Subscribes to state changes for two properties.
     *
     * @param deliveryMode If [UniqueOnly], when this [MavericksView] goes from a stopped to start lifecycle a state value
     * will only be emitted if the state changed. This is useful for transient views that should only
     * be shown once (toasts, poptarts), or logging. Most other views should use false, as when a view is destroyed
     * and recreated the previous state is necessary to recreate the view.
     *
     * Use [uniqueOnly] to automatically create a [UniqueOnly] mode with a unique id for this view.
     *
     * Default: [RedeliverOnStart]
     * @param action supports cooperative cancellation. The previous action will be cancelled if it is not completed before
     * the next one is emitted..
     */
    fun <S : BaseUIState, A, B> BaseViewModel<S>.onEach(
        prop1: KProperty1<S, A>,
        prop2: KProperty1<S, B>,
        deliveryMode: DeliveryMode = RedeliverOnStart,
        action: suspend (A, B) -> Unit
    ) = _internal2(subscriptionLifecycleOwner, prop1, prop2, deliveryMode, action)

    /**
     * Subscribes to state changes for three properties.
     *
     * @param deliveryMode If [UniqueOnly], when this [MavericksView] goes from a stopped to start lifecycle a state value
     * will only be emitted if the state changed. This is useful for transient views that should only
     * be shown once (toasts, poptarts), or logging. Most other views should use false, as when a view is destroyed
     * and recreated the previous state is necessary to recreate the view.
     *
     * Use [uniqueOnly] to automatically create a [UniqueOnly] mode with a unique id for this view.
     *
     * Default: [RedeliverOnStart].
     * @param action supports cooperative cancellation. The previous action will be cancelled if it is not completed before
     * the next one is emitted.
     */
    fun <S : BaseUIState, A, B, C> BaseViewModel<S>.onEach(
        prop1: KProperty1<S, A>,
        prop2: KProperty1<S, B>,
        prop3: KProperty1<S, C>,
        deliveryMode: DeliveryMode = RedeliverOnStart,
        action: suspend (A, B, C) -> Unit
    ) = _internal3(subscriptionLifecycleOwner, prop1, prop2, prop3, deliveryMode, action)

    /**
     * Subscribes to state changes for four properties.
     *
     * @param deliveryMode If [UniqueOnly], when this [MavericksView] goes from a stopped to start lifecycle a state value
     * will only be emitted if the state changed. This is useful for transient views that should only
     * be shown once (toasts, poptarts), or logging. Most other views should use false, as when a view is destroyed
     * and recreated the previous state is necessary to recreate the view.
     *
     * Use [uniqueOnly] to automatically create a [UniqueOnly] mode with a unique id for this view.
     *
     * Default: [RedeliverOnStart].
     * @param action supports cooperative cancellation. The previous action will be cancelled if it is not completed before
     * the next one is emitted.
     */
    fun <S : BaseUIState, A, B, C, D> BaseViewModel<S>.onEach(
        prop1: KProperty1<S, A>,
        prop2: KProperty1<S, B>,
        prop3: KProperty1<S, C>,
        prop4: KProperty1<S, D>,
        deliveryMode: DeliveryMode = RedeliverOnStart,
        action: suspend (A, B, C, D) -> Unit
    ) = _internal4(subscriptionLifecycleOwner, prop1, prop2, prop3, prop4, deliveryMode, action)

    /**
     * Subscribes to state changes for five properties.
     *
     * @param deliveryMode If [UniqueOnly], when this [MavericksView] goes from a stopped to start lifecycle a state value
     * will only be emitted if the state changed. This is useful for transient views that should only
     * be shown once (toasts, poptarts), or logging. Most other views should use false, as when a view is destroyed
     * and recreated the previous state is necessary to recreate the view.
     *
     * Use [uniqueOnly] to automatically create a [UniqueOnly] mode with a unique id for this view.
     *
     * Default: [RedeliverOnStart].
     * @param action supports cooperative cancellation. The previous action will be cancelled if it is not completed before
     * the next one is emitted.
     */
    fun <S : BaseUIState, A, B, C, D, E> BaseViewModel<S>.onEach(
        prop1: KProperty1<S, A>,
        prop2: KProperty1<S, B>,
        prop3: KProperty1<S, C>,
        prop4: KProperty1<S, D>,
        prop5: KProperty1<S, E>,
        deliveryMode: DeliveryMode = RedeliverOnStart,
        action: suspend (A, B, C, D, E) -> Unit
    ) = _internal5(subscriptionLifecycleOwner, prop1, prop2, prop3, prop4, prop5, deliveryMode, action)

    /**
     * Subscribes to state changes for six properties.
     *
     * @param deliveryMode If [UniqueOnly], when this [MavericksView] goes from a stopped to start lifecycle a state value
     * will only be emitted if the state changed. This is useful for transient views that should only
     * be shown once (toasts, poptarts), or logging. Most other views should use false, as when a view is destroyed
     * and recreated the previous state is necessary to recreate the view.
     *
     * Use [uniqueOnly] to automatically create a [UniqueOnly] mode with a unique id for this view.
     *
     * Default: [RedeliverOnStart].
     * @param action supports cooperative cancellation. The previous action will be cancelled if it is not completed before
     * the next one is emitted.
     */
    fun <S : BaseUIState, A, B, C, D, E, F> BaseViewModel<S>.onEach(
        prop1: KProperty1<S, A>,
        prop2: KProperty1<S, B>,
        prop3: KProperty1<S, C>,
        prop4: KProperty1<S, D>,
        prop5: KProperty1<S, E>,
        prop6: KProperty1<S, F>,
        deliveryMode: DeliveryMode = RedeliverOnStart,
        action: suspend (A, B, C, D, E, F) -> Unit
    ) = _internal6(subscriptionLifecycleOwner, prop1, prop2, prop3, prop4, prop5, prop6, deliveryMode, action)

    /**
     * Subscribes to state changes for seven properties.
     *
     * @param deliveryMode If [UniqueOnly], when this [MavericksView] goes from a stopped to start lifecycle a state value
     * will only be emitted if the state changed. This is useful for transient views that should only
     * be shown once (toasts, poptarts), or logging. Most other views should use false, as when a view is destroyed
     * and recreated the previous state is necessary to recreate the view.
     *
     * Use [uniqueOnly] to automatically create a [UniqueOnly] mode with a unique id for this view.
     *
     * Default: [RedeliverOnStart].
     * @param action supports cooperative cancellation. The previous action will be cancelled if it is not completed before
     * the next one is emitted.
     */
    fun <S : BaseUIState, A, B, C, D, E, F, G> BaseViewModel<S>.onEach(
        prop1: KProperty1<S, A>,
        prop2: KProperty1<S, B>,
        prop3: KProperty1<S, C>,
        prop4: KProperty1<S, D>,
        prop5: KProperty1<S, E>,
        prop6: KProperty1<S, F>,
        prop7: KProperty1<S, G>,
        deliveryMode: DeliveryMode = RedeliverOnStart,
        action: suspend (A, B, C, D, E, F, G) -> Unit
    ) = _internal7(subscriptionLifecycleOwner, prop1, prop2, prop3, prop4, prop5, prop6, prop7, deliveryMode, action)

    /**
     * Subscribe to changes in an async property. There are optional parameters for onSuccess
     * and onFail which automatically unwrap the value or error.
     *
     * @param deliveryMode If [UniqueOnly], when this [MavericksView] goes from a stopped to start lifecycle a state value
     * will only be emitted if the state changed. This is useful for transient views that should only
     * be shown once (toasts, poptarts), or logging. Most other views should use false, as when a view is destroyed
     * and recreated the previous state is necessary to recreate the view.
     *
     * Use [uniqueOnly] to automatically create a [UniqueOnly] mode with a unique id for this view.
     *
     * Default: [RedeliverOnStart].
     * @param onFail supports cooperative cancellation. The previous action will be cancelled if it as not completed before
     * the next one is emitted.
     * @param onSuccess supports cooperative cancellation. The previous action will be cancelled if it as not completed before
     * the next one is emitted.
     */
    fun <S : BaseUIState, T> BaseViewModel<S>.onAsync(
        asyncProp: KProperty1<S, Async<T>>,
        deliveryMode: DeliveryMode = RedeliverOnStart,
        onFail: (suspend (Throwable) -> Unit)? = null,
        onSuccess: (suspend (T) -> Unit)? = null
    ) = _internalSF(subscriptionLifecycleOwner, asyncProp, deliveryMode, onFail, onSuccess)

    /**
     * Subscribes to the given Flow within the coroutine scope of the `subscriptionLifecycleOwner`, starting the flow only when the lifecycle
     * is started, and executing with the coroutine context of Mavericks' `subscriptionCoroutineContextOverride`. This can be utilized to create
     * customized subscriptions, for example, to drop the first element in the flow before continuing. This is intended to be used with
     * `viewmodel.stateflow`.
     *
     * @param deliveryMode If [UniqueOnly], when this [MavericksView] goes from a stopped to started lifecycle a value
     * will only be emitted if the value has changed. This is useful for transient views that should only
     * be shown once (toasts, poptarts), or logging. Most other views should use [RedeliverOnStart], as when a view is destroyed
     * and recreated the previous state is necessary to recreate the view.
     *
     * Use [uniqueOnly] to automatically create a [UniqueOnly] mode with a unique id for this view.
     *
     * @param action supports cooperative cancellation. The previous action will be cancelled if it is not completed before
     * the next one is emitted.
     */
    fun <T : Any?> Flow<T>.collectLatest(
        deliveryMode: DeliveryMode,
        action: suspend (T) -> Unit
    ): Job = MVIViewInternalViewModel.let {
        collectLatest(
            subscriptionLifecycleOwner,
            it.lastDeliveredStates,
            it.activeSubscriptions,
            deliveryMode,
            action
        )
    }
}
