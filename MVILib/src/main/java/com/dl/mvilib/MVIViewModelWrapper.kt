package com.dl.mvilib

import androidx.lifecycle.ViewModel

class MVIViewModelWrapper<VM : BaseViewModel<S>, S : BaseUIState>(val viewModel: VM) : ViewModel() {
    override fun onCleared() {
        super.onCleared()
        viewModel.onCleared()
    }
}
