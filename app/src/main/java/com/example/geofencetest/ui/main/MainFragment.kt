package com.example.geofencetest.ui.main

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import com.example.geofencetest.R
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

class MainFragment : Fragment() {

    companion object {
        fun newInstance() = MainFragment()
        const val PERMISSION_REQUEST_CODE = 1001
        val TAG = MainFragment::class.java.simpleName
    }

    private lateinit var viewModel: MainViewModel
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.main_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        // viewModelの生成
        viewModel = ViewModelProvider(
            this,
            MainViewModel.MainViewModelFactory(this)
        ).get(MainViewModel::class.java)
        var isGranted = checkPermission()

        if (isGranted) {
            getLastLocation()
        }
    }

    /**
     * 権限許可依頼のコールバック
     */
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            PERMISSION_REQUEST_CODE -> {
                if (grantResults.isEmpty()) {
                    throw RuntimeException("Empty permission result")
                }
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // 現在地ボタンを有効にする
                    Log.d(TAG, "許可を得られた")
                    getLastLocation()
                } else {
                    Log.d(TAG, "許可を得られなかった")
                }
            }
        }
    }

    /**
     * パーミッションを確認する
     *
     * @return 処理結果(true : 権限が許可されている | false : 権限が許可されていない)
     */
    private fun checkPermission(): Boolean {
        if (ActivityCompat.checkSelfPermission(
                activity?.applicationContext!!,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(
                activity?.applicationContext!!,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // 権限が許可されていない場合はリクエストする
            requestPermissions(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                PERMISSION_REQUEST_CODE
            )
            return false
        }
        return true
    }

    /**
     * 直近の緯度・経度を取得する
     */
    private fun getLastLocation() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(activity)
        if (ActivityCompat.checkSelfPermission(
                activity?.applicationContext!!,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                activity?.applicationContext!!,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                Log.d(
                    TAG,
                    "latitude : " + location?.latitude + " " + "longitude : " + location?.longitude
                )
            }
    }


}