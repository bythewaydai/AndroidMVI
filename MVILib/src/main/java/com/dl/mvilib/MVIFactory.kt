package com.dl.mvilib

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

internal class MVIFactory<VM : MVIViewModel<S>, S : MVIState>(
    private val viewModelClass: Class<out VM>,
    private val stateClass: Class<out S>,
    private val viewModelContext: ViewModelContext,
    private val key: String,
    private val stateRestorer: StateRestorer<VM, S>?,
    private val forExistingViewModel: Boolean = false,
    private val initialStateFactory: MVIStateFactory<VM, S> = RealMavericksStateFactory()
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (stateRestorer == null && forExistingViewModel) {
            throw ViewModelDoesNotExistException(viewModelClass, viewModelContext, key)
        }

        val viewModel = createViewModel(
            viewModelClass,
            stateClass,
            viewModelContext,
            stateRestorer,
            initialStateFactory
        )
        return viewModel as T
    }
}

@Suppress("UNCHECKED_CAST")
private fun <VM : MVIViewModel<S>, S : MVIState> createViewModel(
    declaredViewModelClass: Class<out VM>,
    declaredStateClass: Class<out S>,
    viewModelContext: ViewModelContext,
    stateRestorer: StateRestorer<VM, S>?,
    initialStateFactory: MVIStateFactory<VM, S>
): MVIViewModelWrapper<VM, S> {
    val initialState = initialStateFactory.createInitialState(declaredViewModelClass, declaredStateClass, viewModelContext, stateRestorer)

    val viewModelClass = stateRestorer?.viewModelClass ?: declaredViewModelClass
    val stateClass = stateRestorer?.stateClass ?: declaredStateClass

    val factoryViewModel = viewModelClass.factoryCompanion()?.let { factoryClass ->
        try {
            factoryClass.getMethod("create", ViewModelContext::class.java, MVIState::class.java)
                .invoke(factoryClass.instance(), viewModelContext, initialState) as VM?
        } catch (exception: NoSuchMethodException) {
            // Check for JvmStatic method.
            viewModelClass.getMethod("create", ViewModelContext::class.java, MVIState::class.java)
                .invoke(null, viewModelContext, initialState) as VM?
        }
    }
    val viewModel = requireNotNull(factoryViewModel ?: createDefaultViewModel(viewModelClass, initialState)) {
        if (viewModelClass.constructors.firstOrNull()?.parameterTypes?.size?.let { it > 1 } == true) {
            "${viewModelClass.simpleName} takes dependencies other than initialState. " +
                "It must have companion object implementing ${MavericksViewModelFactory::class.java.simpleName} " +
                "with a create method returning a non-null ViewModel."
        } else {
            "${viewModelClass::class.java.simpleName} must have primary constructor with a " +
                "single non-optional parameter that takes initial state of ${stateClass.simpleName}."
        }
    }
    return MVIViewModelWrapper(viewModel)
}

@Suppress("UNCHECKED_CAST", "NestedBlockDepth")
private fun <VM : MVIViewModel<S>, S : MVIState> createDefaultViewModel(viewModelClass: Class<VM>, state: S): VM? {
    // If we are checking for a default ViewModel, we expect only a single default constructor. Any other case
    // is a misconfiguration and we will throw an appropriate error under further inspection.
    if (viewModelClass.constructors.size == 1) {
        val primaryConstructor = viewModelClass.constructors[0]
        if (primaryConstructor.parameterTypes.size == 1 && primaryConstructor.parameterTypes[0].isAssignableFrom(state::class.java)) {
            if (!primaryConstructor.isAccessible) {
                try {
                    primaryConstructor.isAccessible = true
                } catch (e: SecurityException) {
                    throw IllegalStateException("ViewModel class is not public and MvRx could not make the primary constructor accessible.", e)
                }
            }
            return primaryConstructor?.newInstance(state) as? VM
        }
    }
    return null
}

@InternalMavericksApi
open class ViewModelDoesNotExistException(message: String) : IllegalStateException(message) {
    constructor(
        viewModelClass: Class<*>,
        viewModelContext: ViewModelContext,
        key: String
    ) : this("ViewModel of type ${viewModelClass.name} for ${viewModelContext.owner}[$key] does not exist yet!")
}
