package org.kivy; // change to your package

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TokenSyncHelper {

    private static final String TAG = "TokenSyncHelper";
    private static final int TIMEOUT_MS = 15000;

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface Callback {
        void onSuccess(int statusCode, @Nullable String responseBody);

        void onError(int statusCode, @Nullable String errorMessage);
    }

    /**
     * Sends the FCM token to the server.
     *
     * @param targetUrl   full endpoint URL, e.g. https://your-domain.com/api/fcm-token/
     * @param accessToken user's access token (sent as "Authorization: Bearer ...")
     * @param fcmToken    the Firebase token from onNewToken
     * @param callback    optional; results are delivered on the main thread
     */
    public static void sendToken(@NonNull final String targetUrl,
                                 @NonNull final String accessToken,
                                 @NonNull final String fcmToken,
                                 @Nullable final Callback callback) {

        executor.execute(() -> {
            HttpURLConnection conn = null;
            try {
                JSONObject body = new JSONObject();
                body.put("token", fcmToken);

                conn = (HttpURLConnection) new URL(targetUrl).openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                conn.setRequestProperty("Accept", "application/json");
                conn.setRequestProperty("Authorization", "Bearer " + accessToken);
                conn.setConnectTimeout(TIMEOUT_MS);
                conn.setReadTimeout(TIMEOUT_MS);
                conn.setDoOutput(true);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(body.toString().getBytes(StandardCharsets.UTF_8));
                }

                int code = conn.getResponseCode();
                boolean ok = code >= 200 && code < 300;
                InputStream stream = ok ? conn.getInputStream() : conn.getErrorStream();
                String response = readStream(stream);

                Log.d(TAG, "Token sync response: " + code + " " + response);

                if (ok) {
                    postSuccess(callback, code, response);
                } else {
                    postError(callback, code, response); // e.g. 401 -> refresh access token and retry
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to send token", e);
                postError(callback, -1, e.getMessage());
            } finally {
                if (conn != null) conn.disconnect();
            }
        });
    }

    private static String readStream(@Nullable InputStream stream) {
        if (stream == null) return null;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private static void postSuccess(@Nullable Callback cb, int code, String body) {
        if (cb != null) mainHandler.post(() -> cb.onSuccess(code, body));
    }

    private static void postError(@Nullable Callback cb, int code, String msg) {
        if (cb != null) mainHandler.post(() -> cb.onError(code, msg));
    }
}