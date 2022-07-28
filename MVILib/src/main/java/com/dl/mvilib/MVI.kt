package com.dl.mvilib

import android.content.Context

object MVI {

    /**
     * A factory that provides the Lazy ViewModels created for Mavericks extension functions,
     * such as [activityViewModel].
     *
     * Each time a ViewModel is accessed via one of these extension functions this factory
     * creates the Kotlin property delegate that wraps the ViewModel.
     *
     * The default implementation [DefaultViewModelDelegateFactory] is fine for general usage,
     * but a custom factory may be provided to assist with testing, or if you want control
     * over how and when ViewModels and their State are created.
     */
    // 给viewModel提供一个工厂，用来创建viewModel，且是懒加载，根据生命周期来
    var viewModelDelegateFactory: ViewModelDelegateFactory = DefaultViewModelDelegateFactory()

    /**
     * A factory for creating a [MavericksViewModelConfig] for each ViewModel.
     *
     * You MUST provide an instance here before creating any viewmodels. You can do this when
     * your application is created via the [initialize] helper.
     *
     * This allows you to specify whether Mavericks should run in debug mode or not. Additionally, it
     * allows custom state stores or execution behavior for the ViewModel, which can be helpful
     * for testing.
     */
    // viewmodel 的配置工厂
    var viewModelConfigFactory: MVIViewModelConfigFactory
        set(value) {
            _viewModelConfigFactory = value
        }
        get() {
            return _viewModelConfigFactory ?: error("You must initialize Mavericks. Add Mavericks.initialize(...) to your Application.onCreate().")
        }
    private var _viewModelConfigFactory: MVIViewModelConfigFactory? = null

    /**
     * A helper for setting [viewModelConfigFactory] based on whether the app was built in debug mode or not.
     */
    fun initialize(
        context: Context,
        viewModelConfigFactory: MVIViewModelConfigFactory? = null,
        viewModelDelegateFactory: ViewModelDelegateFactory? = null
    ):MVI {
        initialize(context.isDebuggable(), viewModelConfigFactory, viewModelDelegateFactory)
        return this
    }

    /**
     * A helper for setting [viewModelConfigFactory] with the given debug mode.
     */
    fun initialize(
        debugMode: Boolean,
        viewModelConfigFactory: MVIViewModelConfigFactory? = null,
        viewModelDelegateFactory: ViewModelDelegateFactory? = null
    ) {
        _viewModelConfigFactory = viewModelConfigFactory ?: MVIViewModelConfigFactory(debugMode = debugMode)
        this.viewModelDelegateFactory = when {
            viewModelDelegateFactory != null -> viewModelDelegateFactory
            this.viewModelDelegateFactory !is DefaultViewModelDelegateFactory -> DefaultViewModelDelegateFactory()
            else -> this.viewModelDelegateFactory
        }
    }
}
