package com.example.geofencetest.main

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * メインのViewModel
 */
class MainViewModel(private val mainFragment : MainFragment) : ViewModel() {

    companion object {
        val TAG = MainViewModel::class.java.simpleName
    }

    class MainViewModelFactory(private val mainFragment : MainFragment) :
        ViewModelProvider.NewInstanceFactory() {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return MainViewModel(mainFragment) as T
        }
    }

    /**
     * 家を登録ボタンをクリックする
     */
    fun clickRegisterHomeButton() {
        Log.d(TAG, "ボタンがクリックされた")

        var isGranted = mainFragment.checkPermission()

        if (isGranted) {
            mainFragment.getLastLocationAndSetGeofence()
        }
    }
}