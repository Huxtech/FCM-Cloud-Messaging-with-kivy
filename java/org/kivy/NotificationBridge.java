package org.kivy;

import java.util.LinkedList;
import java.util.Queue;
import org.json.JSONObject;
import android.os.Bundle;

public class NotificationBridge {

    public interface MessageListener {
        void onMessage(JSONObject json);
    }

    public interface IntentMessageListener {
        void onIntentMessage(Bundle mIntentBundle);
    }

    private static volatile MessageListener listener;
    private static volatile IntentMessageListener intentListener;
    private static final Queue<JSONObject> pendingMessages = new LinkedList<>();
    
    // Register Python listener.
    public static synchronized void setIntentMessageListener(IntentMessageListener eventListener) {
        intentListener = eventListener;
    }

    public static synchronized void setListener(MessageListener eventListener) {
        listener = eventListener;
        // Deliver queued messages
        while (!pendingMessages.isEmpty()) {
            listener.onMessage(
                    pendingMessages.poll()
            );
        }
    }
    
    // Remove listener.
    public static synchronized void removeListener() {
        listener = null;
    }
    
    // Send message from Java.
    public static synchronized void sendMessage(JSONObject json) {
        if (listener != null) {
            listener.onMessage(json);
        } else {
            pendingMessages.add(json);
        }
    }

    public static synchronized void sendItentMessage(Bundle mIntentBundle) {
        if (intentListener != null) {
            intentListener.onIntentMessage(mIntentBundle);
        }
    }
}

