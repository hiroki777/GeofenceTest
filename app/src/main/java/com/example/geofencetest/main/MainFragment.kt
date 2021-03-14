package com.example.geofencetest.main

import android.Manifest
import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.geofencetest.R
import com.example.geofencetest.databinding.MainFragmentBinding
import com.example.geofencetest.receiver.GeofenceBroadcastReceiver
import com.google.android.gms.location.*
import java.util.*

/**
 * メインのフラグメント
 */
class MainFragment : Fragment() {

    // region companion

    companion object {
        fun newInstance() = MainFragment()
        const val PERMISSION_REQUEST_CODE = 1001
        val TAG = MainFragment::class.java.simpleName

        // 参考 https://github.com/dmutti/android-play-location/blob/master/Geofencing/app/src/main/java/com/google/android/gms/location/sample/geofencing/Constants.java

        /**
         * Used to set an expiration time for a geofence. After this amount of time Location Services
         * stops tracking the geofence.
         */
        private const val GEOFENCE_EXPIRATION_IN_HOURS: Long = 12

        /**
         * For this sample, geofences expire after twelve hours.
         */
        val GEOFENCE_EXPIRATION_IN_MILLISECONDS: Long = GEOFENCE_EXPIRATION_IN_HOURS * 60 * 60 * 1000

        const val GEOFENCE_RADIUS_IN_METERS = 100f // 100メートル
    }

    // endregion

    // region プロパティ

    private lateinit var viewModel: MainViewModel
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    lateinit var geofencingClient: GeofencingClient

    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(activity, GeofenceBroadcastReceiver::class.java)
        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when calling
        // addGeofences() and removeGeofences().
        PendingIntent.getBroadcast(activity, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    // endregion

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

        val mainFragmentBinding : MainFragmentBinding = DataBindingUtil.setContentView(
            activity as Activity,
            R.layout.main_fragment
        )
        mainFragmentBinding.viewmodel = viewModel
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
                    getLastLocationAndSetGeofence()
                } else {
                    Log.d(TAG, "許可を得られなかった")
                }
            }
        }
    }

    // region public

    /**
     * パーミッションを確認する
     *
     * @return 処理結果(true : 権限が許可されている | false : 権限が許可されていない)
     */
     fun checkPermission(): Boolean {
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
     * get latest location and set geofence
     */
    fun getLastLocationAndSetGeofence() {
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
                setGeofence(location?.latitude!!, location?.longitude)
            }
    }

    // endregion

    // region private

    /**
     * Geofenceのリクエストを取得する
     */
    private fun getGeofencingRequest(latitude: Double, longitude: Double): GeofencingRequest {

        var geofenceList = mutableListOf<Geofence>()

        geofenceList.add(
            Geofence.Builder()
                // Set the request ID of the geofence. This is a string to identify this
                /*
            一意なIDを生成する
            https://pisuke-code.com/web-create-absolutely-unique-id/
             */
                .setRequestId(UUID.randomUUID().toString())

                // Set the circular region of this geofence.
                .setCircularRegion(
                    latitude,
                    longitude,
                    GEOFENCE_RADIUS_IN_METERS
                )

                // Set the expiration duration of the geofence. This geofence gets automatically
                // removed after this period of time.
                .setExpirationDuration(GEOFENCE_EXPIRATION_IN_MILLISECONDS)

                // Set the transition types of interest. Alerts are only generated for these
                // transition. We track entry and exit transitions in this sample.
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)

                // Create the geofence.
                .build()
        )

        return GeofencingRequest.Builder().apply {
            setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            addGeofences(geofenceList)
        }.build()
    }

    /**
     * Geofenceの設定をする
     */
    private fun setGeofence(latitude: Double, longitude: Double) {
        geofencingClient = LocationServices.getGeofencingClient(activity)

        if (ActivityCompat.checkSelfPermission(
                activity as Activity,
                Manifest.permission.ACCESS_FINE_LOCATION
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
        /*
        成功させるには「High accuracy」をチェックする必要がある
        https://stackoverflow.com/questions/19082482/error-adding-geofences-in-android-status-code-1000
         */
        geofencingClient?.addGeofences(getGeofencingRequest(latitude, longitude), geofencePendingIntent)?.run {
            addOnSuccessListener {
                // Geofences added
                Log.d(TAG, "geofenceの追加が成功した")
            }
            addOnFailureListener {
                // Failed to add geofences
                Log.d(TAG, "geofenceの追加が失敗した")
            }
        }
    }

    // endregion

}