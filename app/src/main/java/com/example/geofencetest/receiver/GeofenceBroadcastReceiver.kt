package com.example.geofencetest.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.text.TextUtils
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.TaskStackBuilder
import com.example.geofencetest.MyApplication
import com.example.geofencetest.R
import com.example.geofencetest.main.MainActivity
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent


class GeofenceBroadcastReceiver : BroadcastReceiver() {

    companion object {
        val TAG = GeofenceBroadcastReceiver::class.java.simpleName
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        if (geofencingEvent.hasError()) {
            val errorMessage = GeofenceStatusCodes
                .getStatusCodeString(geofencingEvent.errorCode)
            Log.e(TAG, errorMessage)
            return
        }

        // Get the transition type.
        val geofenceTransition = geofencingEvent.geofenceTransition

        // Test that the reported transition was of interest.
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER
            || geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {

            // Get the geofences that were triggered. A single event can trigger
            // multiple geofences.
            val triggeringGeofences = geofencingEvent.triggeringGeofences

            // Get the transition details as a String.
            val geofenceTransitionDetails = getGeofenceTransitionDetails(
                geofenceTransition,
                triggeringGeofences
            )

            // Send notification and log the transition details.
            sendNotification(geofenceTransitionDetails!!)
            Log.i(TAG, geofenceTransitionDetails)
        } else {
            // Log the error.
            Log.e(
                TAG, MyApplication.instance.getString(
                    R.string.geofence_transition_invalid_type,
                    geofenceTransition
                )
            )
        }
    }

    /*
    参考
    https://github.com/dmutti/android-play-location/blob/master/Geofencing/app/src/main/java/com/google/android/gms/location/sample/geofencing/GeofenceTransitionsIntentService.java
     */

    /**
     * Gets transition details and returns them as a formatted string.
     *
     * @param geofenceTransition    The ID of the geofence transition.
     * @param triggeringGeofences   The geofence(s) triggered.
     * @return                      The transition details formatted as String.
     */
    private fun getGeofenceTransitionDetails(
        geofenceTransition: Int,
        triggeringGeofences: List<Geofence>
    ): String? {
        val geofenceTransitionString: String = getTransitionString(geofenceTransition)!!

        // Get the Ids of each geofence that was triggered.
        val triggeringGeofencesIdsList: ArrayList<String?> = ArrayList()
        for (geofence in triggeringGeofences) {
            triggeringGeofencesIdsList.add(geofence.requestId)
        }
        val triggeringGeofencesIdsString = TextUtils.join(", ", triggeringGeofencesIdsList)
        return "$geofenceTransitionString: $triggeringGeofencesIdsString"
    }

    /**
     * Maps geofence transition types to their human-readable equivalents.
     *
     * @param transitionType    A transition type constant defined in Geofence
     * @return                  A String indicating the type of transition
     */
    private fun getTransitionString(transitionType: Int): String? {
        return when (transitionType) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> MyApplication.instance.getString(R.string.geofence_transition_entered)
            Geofence.GEOFENCE_TRANSITION_EXIT -> MyApplication.instance.getString(R.string.geofence_transition_exited)
            else -> MyApplication.instance.getString(R.string.unknown_geofence_transition)
        }
    }

    /**
     * Posts a notification in the notification bar when a transition is detected.
     * If the user clicks the notification, control goes to the MainActivity.
     */
    private fun sendNotification(notificationDetails: String) {
        // Create an explicit content Intent that starts the main Activity.
        val notificationIntent = Intent(
            MyApplication.instance,
            MainActivity::class.java
        )

        // Construct a task stack.
        val stackBuilder: TaskStackBuilder = TaskStackBuilder.create(MyApplication.instance)

        // Add the main Activity to the task stack as the parent.
        stackBuilder.addParentStack(MainActivity::class.java)

        // Push the content Intent onto the stack.
        stackBuilder.addNextIntent(notificationIntent)

        // Get a PendingIntent containing the entire back stack.
        val notificationPendingIntent: PendingIntent =
            stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)!!

        // Get a notification builder that's compatible with platform versions >= 4
        val builder = NotificationCompat.Builder(MyApplication.instance)

        // Define the notification settings.
        builder.setSmallIcon(R.drawable.ic_launcher_foreground) // In a real app, you may want to use a library like Volley
            // to decode the Bitmap.
            .setLargeIcon(
                BitmapFactory.decodeResource(
                    MyApplication.instance.getResources(),
                    R.drawable.ic_launcher_foreground
                )
            )
            .setContentTitle(notificationDetails)
            .setContentText(MyApplication.instance.getString(R.string.geofence_transition_notification_text))
//            .setContentText(getString(R.string.geofence_transition_notification_text))
            .setContentIntent(notificationPendingIntent)

        // Dismiss notification once the user touches it.
        builder.setAutoCancel(true)

        // Get an instance of the Notification manager
        val mNotificationManager =
            MyApplication.instance.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "1"
            val channel = NotificationChannel(
                channelId,
                "Channel human readable title",
                NotificationManager.IMPORTANCE_HIGH
            )
            mNotificationManager!!.createNotificationChannel(channel)
            builder.setChannelId(channelId)
        }

        // Issue the notification
        mNotificationManager!!.notify(0, builder.build())
    }
}