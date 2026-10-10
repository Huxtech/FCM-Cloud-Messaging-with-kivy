package org.kivy;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

public final class FCMNotificationChannel {
    public static final String CHANNEL_ID = "high_importance";

    private FCMNotificationChannel() {}

    public static void ensureChannel(Context ctx) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Alerts", NotificationManager.IMPORTANCE_HIGH);
            channel.enableVibration(true);
            ctx.getSystemService(NotificationManager.class)
               .createNotificationChannel(channel);
        }
    }
}