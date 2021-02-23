package com.example.geofencetest.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class MainViewModel(private val mainFragment : MainFragment) : ViewModel() {

    class MainViewModelFactory(private val mainFragment : MainFragment) :
        ViewModelProvider.NewInstanceFactory() {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return MainViewModel(mainFragment) as T
        }
    }
}