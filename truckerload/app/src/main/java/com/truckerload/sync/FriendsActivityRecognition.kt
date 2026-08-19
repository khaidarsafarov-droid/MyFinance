package com.truckerload.sync

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionRequest
import com.google.android.gms.location.ActivityTransitionResult
import com.google.android.gms.location.DetectedActivity
import com.truckerload.data.preferences.FriendsLocationShareStore
import com.truckerload.domain.friends.FriendsLocationSharePolicy

object FriendsActivityRecognition {
    private const val REQUEST_CODE = 4722

    fun hasPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < 29) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACTIVITY_RECOGNITION,
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun register(context: Context) {
        val app = context.applicationContext
        if (!hasPermission(app)) return
        val request = ActivityTransitionRequest(
            listOf(
                DetectedActivity.STILL,
                DetectedActivity.IN_VEHICLE,
                DetectedActivity.ON_FOOT,
                DetectedActivity.WALKING,
                DetectedActivity.RUNNING,
                DetectedActivity.ON_BICYCLE,
            ).flatMap { type ->
                listOf(
                    ActivityTransition.Builder()
                        .setActivityType(type)
                        .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                        .build(),
                )
            },
        )
        runCatching {
            ActivityRecognition.getClient(app)
                .requestActivityTransitionUpdates(request, pendingIntent(app))
        }
    }

    fun unregister(context: Context) {
        val app = context.applicationContext
        runCatching {
            ActivityRecognition.getClient(app)
                .removeActivityTransitionUpdates(pendingIntent(app))
        }
    }

    fun pendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, FriendsActivityTransitionReceiver::class.java)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else 0
        return PendingIntent.getBroadcast(context, REQUEST_CODE, intent, flags)
    }
}

class FriendsActivityTransitionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null || !ActivityTransitionResult.hasResult(intent)) return
        val result = ActivityTransitionResult.extractResult(intent) ?: return
        val last = result.transitionEvents.lastOrNull() ?: return
        val motion = FriendsLocationSharePolicy.motionFromActivityType(last.activityType)
        val app = context.applicationContext
        FriendsLocationShareStore(app).setLastMotion(motion)
        FriendsLocationShareScheduler.sync(app)
    }
}
