

    # Firebase Cloud Messaging (FCM) Integration for Kivy (python-for-android)

This guide explains the required configuration for enabling Firebase Cloud Messaging (FCM) in a Kivy application built with Buildozer/python-for-android.

---

## 1. Android SDK Configuration

Configure your `buildozer.spec` file with the following parameters:

```ini
# Android SDK Configuration
android.api = 35
android.minapi = 24
android.sdk = 35
android.manifest.launch_mode = singleTop
android.permissions = android.permission.INTERNET, android.permission.POST_NOTIFICATIONS
```

---

## 2. Add the Firebase Configuration File

Download the **`google-services.json`** file from your Firebase project and paste in your generated android directory.

If your using huxkivy achetecture, copy the file into the root of your project directory where main.py is.

```
project/
├── google-services.json
├── main.py
├── buildozer.spec
└── ...
```


---

## 3. AndroidManifest.tmpl.xml

```xml
...
    <meta-data
        android:name="com.google.firebase.messaging.default_notification_icon"
        android:resource="@drawable/icon_reduced" />
    <meta-data
        android:name="com.google.firebase.messaging.default_notification_channel_id"
        android:value="high_importance"/>
...
    <service
        android:name="org.kivy.MyFirebaseMessagingService"
        android:exported="false">
        <intent-filter>
            <action android:name="com.google.firebase.MESSAGING_EVENT" />
        </intent-filter>
    </service>
```

---

## 4. build.tmpl.gradle

```ini
buildscript {
    repositories {
       google()
       mavenCentral()
    }
    dependencies {
        classpath 'com.android.tools.build:gradle:8.11.0'
        classpath 'com.google.gms:google-services:4.4.3'
    }
}

...

{% if is_library %}
apply plugin: 'com.android.library'
{% else %}
apply plugin: 'com.android.application'
{% endif %}
apply plugin: 'com.google.gms.google-services'

...

dependencies {
    implementation 'com.google.firebase:firebase-messaging'
    implementation platform('com.google.firebase:firebase-bom:34.16.0')
    implementation 'com.google.firebase:firebase-analytics'
    {%- for aar in aars %}
    implementation(name: '{{ aar }}', ext: 'aar')
    {%- endfor -%}
    {%- for jar in jars %}
    implementation files('src/main/libs/{{ jar }}')
    {%- endfor -%}
    {%- if args.depends -%}
    {%- for depend in args.depends %}
    implementation '{{ depend }}'
    {%- endfor %}
    {%- endif %}
    {% if args.presplash_lottie %}
    implementation 'com.airbnb.android:lottie:6.1.0'
    {%- endif %}
}
```

---

## 5. Update `PythonActivity`

Modify `PythonActivity.java` so that notification data is preserved whether the application is launched from a terminated state or is already running.

### Declare a static bundle

### imports

```java
import org.kivy.FCMNotificationChannel;
import org.kivy.NotificationBridge;
```

```java
public static Bundle mIntentBundle = null;
```

### Add resetMIntentBundle method to reset mIntentBundle after being read
```java
    public void resetMIntentBundle() {
        this.mIntentBundle = null;
    }
```

### Update `onCreate()`

```java
@Override
protected void onCreate(Bundle savedInstanceState) {

    ...
    FCMNotificationChannel.ensureChannel(this);

    if (getIntent() != null && getIntent().getExtras() != null && getIntent().getExtras().size() > 0) {
        mIntentBundle = getIntent().getExtras();
    }

    ...
}
```

### Update `onNewIntent()`

```java
@Override
protected void onNewIntent(Intent intent) {

    
    if (intent != null && intent.getExtras() != null && intent.getExtras().size() > 0) {
        mIntentBundle = intent.getExtras();
        NotificationBridge.sendItentMessage(mIntentBundle);
    }

    ...

}
```

---

## 6. Drawable

Make sure your notification icon is available in your drawable directory
```ini
res/drawable/icon_reduced.png
```

Then add the res/java directory in android project in buildozer.spec
```ini
android.add_src = java
android.add_resources = ./res
```

---

## 7. fcm_notification.py

- `get_fcm_notification_data` Gets the `PythonActivity.mIntentBundle` convert it to proper python dictionary and clear `PythonActivity.mIntentBundle` existing value, then return the dict.
- `get_fcm_notification_token` gets the fcm notification token. 
  It's recommended to sync token with server.

```python
def token_callback(self, token):
    print(token)
get_fcm_notification_token(token_callback)
```
- `NotificationListener` listen to realtime notification while the app is on foreground.
- `IntentMessageListener` Gets the notification data when user tab on notification while the application is on foreground.

---

## 8. Update your access token and api `onNewToken` method (Optional)
```java
String accessToken = "my_access_token";
String api = "https://your-domain.com/api/fcm-token/";
```
### Handle token in server (Django)

- urls.py
```python
from django.urls import path
from .views import register_fcm_token

urlpatterns = [
    path("api/fcm-token/", register_fcm_token, name="register-fcm-token"),
]
```

- views.py
```python
from rest_framework.decorators import api_view, permission_classes
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response
from rest_framework import status

from .models import DeviceToken  # fields: user (FK), token (unique), platform, updated_at


@api_view(["POST"])
@permission_classes([IsAuthenticated])
def register_fcm_token(request):
    token = request.data.get("token")
    if not token:
        return Response({"detail": "token is required"}, status=status.HTTP_400_BAD_REQUEST)

    # One row per token; if the token exists, reassign it to the current user
    DeviceToken.objects.update_or_create(
        token=token,
        defaults={"user": request.user, "platform": "android"},
    )
    return Response({"status": "ok"}, status=status.HTTP_200_OK)
```



---

## 9. Full Demo

```python
from kivy.app import App
from kivy.uix.button import Button
from kivy.uix.boxlayout import BoxLayout
from kivy.lang.builder import Builder
from kivy.properties import DictProperty, StringProperty
from kivy.clock import Clock
from fcm_notification import (
    IntentMessageListener,
    NotificationListener,
    get_fcm_notification_token,
    get_fcm_notification_data
)

from jnius import autoclass
from permissions import handle_android_permissions

KV = '''
BoxLayout:
    ScrollView:
        BoxLayout:
            orientation: "vertical"
            height: self.minimum_height
            TextInput:
                text: app.fcm_token
            Label:
                text: str(app.notification_data)
                font_size: 50
                text_size: self.width, None
                size_hint_y: None
                height: self.texture_size[1]
            Button:
                text: "Clear data"
                on_release: setattr(app, "notification_data", "")

'''


class NotificationApp(App):
    notification_data = DictProperty(rebind=True)
    fcm_token = StringProperty("")

    def on_start(self, **kwargs):
        self.notification_data = get_fcm_notification_data()
        self.dispatch("on_resume")

    def on_resume(self, **kwargs):
        self.notification_data = get_fcm_notification_data()
        PythonActivity = autoclass('org.kivy.android.PythonActivity')
        activity = PythonActivity.mActivity
        Clock.schedule_once(lambda _: activity.setAppIsReady(True))
    
    def notification_callback(self, json):
        self.notification_data = json
        PythonActivity = autoclass('org.kivy.android.PythonActivity')
        activity = PythonActivity.mActivity
        activity.resetMIntentBundle()
    
    def token_callback(self, token):
        self.fcm_token = token

    def build(self):
        get_fcm_notification_token(self.token_callback)
        NotificationBridge = autoclass("org.kivy.NotificationBridge")
        listener = NotificationListener(self.notification_callback)
        intent_msg_listener = IntentMessageListener(self.notification_callback)
        NotificationBridge.setListener(listener)
        NotificationBridge.setIntentMessageListener(intent_msg_listener)

        Clock.schedule_once(lambda dt: handle_android_permissions(), 7)
        return Builder.load_string(KV)

if __name__ == "__main__":
    NotificationApp().run()
```

---

## Why This Is Required

When a user taps a push notification:

- **Cold Start** (application is not running)
  - The notification extras are delivered through `onCreate()`.

- **Warm Start** (application is already running)
  - The notification extras are delivered through `onNewIntent()`.

Saving the extras in a static `Bundle` makes them accessible from the Python side of the application after the Kivy runtime has started.

---

## Result

After completing the above steps, your Kivy application will be able to:

- Receive Firebase Cloud Messaging (FCM) notifications.
- Access notification payload data from Python.
- Handle both cold-start and warm-start notification launches consistently.

```

```

## DEEP LINKING

- Register in PythonActivity onCreate

```java
if (uri != null) {
    Log.d("DeepLink", "URL: " + uri.toString());
    String screen = uri.getQueryParameter("screen");
    String id = uri.getQueryParameter("id");
    Log.d("DeepLink", "screen = " + screen);
    Log.d("DeepLink", "id = " + id);
}
```
- Add this to the supported Deeplink Activity
```xml
<!-- Deep Link -->
<intent-filter>
    <action android:name="android.intent.action.VIEW"/>
    <category android:name="android.intent.category.DEFAULT"/>
    <category android:name="android.intent.category.BROWSABLE"/>

    <!-- https://myapp.com/... -->
    <data
        android:scheme="kvc"
        android:host="myapp.com"/>

    <!-- http://myapp.com/... -->
    <data
        android:scheme="kvc"
        android:host="myapp.com"/>
</intent-filter>
```