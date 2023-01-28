package com.dl.androidmvi

import com.dl.androidmvi.databinding.ActivityFirstBinding
import com.dl.mvilib.*


data class CounterState(@PersistState val count: Int = 0) : MVIState

class CounterViewModel(state: CounterState) : MVIViewModel<CounterState>(state) {

    fun incrementCount() = setState { copy(count = count + 1) }
}

class FirstActivity : MVIBaseActivity<ActivityFirstBinding>(),MVIView {


    override fun invalidate() {
        TODO("Not yet implemented")
    }
}