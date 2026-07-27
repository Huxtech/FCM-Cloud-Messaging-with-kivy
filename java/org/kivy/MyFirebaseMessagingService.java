package org.kivy;

// import android.app.NotificationChannel;
// import android.app.NotificationManager;
// import android.app.PendingIntent;
// import android.content.Context;
// import android.content.Intent;
// import android.media.RingtoneManager;
// import android.net.Uri;
// import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
// import androidx.core.app.NotificationCompat;
// import androidx.work.OneTimeWorkRequest;
// import androidx.work.WorkManager;
// import androidx.work.Worker;
// import androidx.work.WorkerParameters;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "MyFirebaseMsgService";

    // [START receive_message]
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        
    }
    
    @Override
    public void onNewToken(@NonNull String token) {
        Log.d(TAG, "Refreshed token: " + token);
    }
}