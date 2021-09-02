package com.example.travelplanner.utils

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer

class ObserverUtils {

    companion object {
        fun <T> LiveData<T>.observeOnce(lifecycleOwner: LifecycleOwner, observer: Observer<T>) {
            observe(
                lifecycleOwner,
                object : Observer<T> {
                    override fun onChanged(t: T?) {
                        observer.onChanged(t)
                        removeObserver(this)
                    }
                }
            )
        }
    }
}
