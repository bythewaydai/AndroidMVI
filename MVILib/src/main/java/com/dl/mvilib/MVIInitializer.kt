package com.dl.mvilib

import android.content.Context
import androidx.startup.Initializer

class MVIInitializer  : Initializer<MVI> {
    override fun create(context: Context): MVI {
        return MVI.initialize(context)
    }
    override fun dependencies(): List<Class<out Initializer<*>>> {
        // No dependencies on other libraries.
        return emptyList()
    }
}