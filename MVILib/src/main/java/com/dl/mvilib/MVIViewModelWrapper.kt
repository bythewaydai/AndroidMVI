package com.dl.mvilib

import androidx.lifecycle.ViewModel

class MVIViewModelWrapper<VM : MVIViewModel<S>, S : MVIState>(val viewModel: VM) : ViewModel() {
    override fun onCleared() {
        super.onCleared()
        viewModel.onCleared()
    }
}
