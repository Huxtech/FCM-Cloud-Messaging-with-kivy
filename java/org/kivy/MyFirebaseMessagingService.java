package org.kivy;

import android.util.Log;
import androidx.annotation.NonNull;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import org.json.JSONObject;
import org.kivy.NotificationBridge;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "MyFirebaseMsgService";

    // [START receive_message]
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        
        JSONObject jsonPayload = new JSONObject();
        try {
            jsonPayload.put("title",
                    remoteMessage.getNotification().getTitle());
            jsonPayload.put("body",
                    remoteMessage.getNotification().getBody());
            if (remoteMessage.getData().size() > 0){
                jsonPayload.put("data",
                        new JSONObject(remoteMessage.getData()));
            }
        } catch (Exception ignored) {
        }
        NotificationBridge.sendMessage(jsonPayload);
    }
    
    @Override
    public void onNewToken(@NonNull String token) {
        Log.d(TAG, "Refreshed token: " + token);
    }
}