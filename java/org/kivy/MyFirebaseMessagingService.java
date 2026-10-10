package org.kivy;

import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import org.kivy.FCMNotificationChannel;
import org.kivy.NotificationBridge;
import org.kivy.android.PythonActivity;
import org.kivy.TokenSyncHelper;
import org.firebase.cm.R;

public class MyFirebaseMessagingService extends FirebaseMessagingService {
    private static final String TAG = "FCM";

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);

        String accessToken = "my_access_token"; // Replace with your actual access token retrieval logic
        if (accessToken != null) {
            TokenSyncHelper.sendToken(
                    "https://your-domain.com/api/fcm-token/",
                    accessToken,
                    token,
                    new TokenSyncHelper.Callback() {
                        @Override
                        public void onSuccess(int statusCode, String responseBody) {
                            Log.d("FCM", "Token synced");
                        }

                        @Override
                        public void onError(int statusCode, String errorMessage) {
                            Log.e("FCM", "Sync failed: " + statusCode + " " + errorMessage);
                        }
                    });
        }
    }

    @Override
    public void onDeletedMessages() {
        super.onDeletedMessages();

        Log.d(TAG, "Some messages were deleted by FCM.");
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
        Map<String, String> data = message.getData();
        if (data.isEmpty()) return;

        FCMNotificationChannel.ensureChannel(this);
       
        String title = data.get("title");
        String body = data.get("body");
        String imageUrl = data.get("imageUrl");

        // Tap action: open MainActivity and pass ALL data along as extras
        Intent intent = new Intent(this, PythonActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        for (Map.Entry<String, String> e : data.entrySet()) {
            intent.putExtra(e.getKey(), e.getValue());
        }

        int notificationId = (int) (System.currentTimeMillis() & 0x7fffffff);
        PendingIntent pi = PendingIntent.getActivity(this, notificationId, intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this, FCMNotificationChannel.CHANNEL_ID)
                        .setSmallIcon(R.drawable.icon_reduced)
                        .setContentTitle(title)
                        .setContentText(body)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setDefaults(NotificationCompat.DEFAULT_ALL)
                        .setAutoCancel(true)
                        .setContentIntent(pi);

        Bitmap image = downloadBitmap(imageUrl);
        if (image != null) {
            builder.setLargeIcon(image)
                   .setStyle(new NotificationCompat.BigPictureStyle()
                           .bigPicture(image)
                           .bigLargeIcon((Bitmap) null)); // hide thumbnail when expanded
        } else {
            builder.setStyle(new NotificationCompat.BigTextStyle().bigText(body));
        }

        NotificationManagerCompat manager = NotificationManagerCompat.from(this);
        if (manager.areNotificationsEnabled()) {
            manager.notify(notificationId, builder.build());
        } 

        JSONObject jsonData = new JSONObject(data);
        NotificationBridge.sendMessage(jsonData);
    }

    // Safe to block: onMessageReceived runs on a background thread (~20s limit)
    private Bitmap downloadBitmap(String urlString) {
        if (urlString == null || urlString.trim().isEmpty()) return null;

        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(urlString).openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(8000);
            conn.setInstanceFollowRedirects(true);
            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) return null;

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try (InputStream in = conn.getInputStream()) {
                byte[] buf = new byte[8192];
                int n, total = 0;
                while ((n = in.read(buf)) != -1) {
                    total += n;
                    if (total > 5 * 1024 * 1024) return null; // 5 MB cap
                    out.write(buf, 0, n);
                }
            }
            byte[] bytes = out.toByteArray();

            // Downsample so a huge image can't cause an OutOfMemoryError
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(bytes, 0, bytes.length, opts);

            int sample = 1;
            while (opts.outWidth / sample > 1024) sample *= 2;

            opts.inJustDecodeBounds = false;
            opts.inSampleSize = sample;
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length, opts);
        } catch (Exception e) {
            Log.w(TAG, "Image download failed: " + e.getMessage());
            return null; // notification still shows, just without the image
        } finally {
            if (conn != null) conn.disconnect();
        }
    }
}