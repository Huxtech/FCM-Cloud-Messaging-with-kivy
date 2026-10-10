

    # Firebase Cloud Messaging (FCM) Integration for Kivy (python-for-android)

This guide explains the required configuration for enabling Firebase Cloud Messaging (FCM) in a Kivy application built with Buildozer/python-for-android.

---

## 1. Android SDK Configuration

Configure your `buildozer.spec` file with the following Android SDK versions:

```ini
# Android SDK Configuration
android.api = 35
android.minapi = 24
android.sdk = 35
```

---

## 2. Add the Firebase Configuration File

Download the **`google-services.json`** file from your Firebase project.

Copy the file into the root of the generated Android project so that it is included during the build process.

```
android/
├── google-services.json
├── build.gradle
├── AndroidManifest.xml
└── ...
```

> **Note**
>
> Ensure that your build templates and Android manifest are configured to include the Firebase SDK and Google Services configuration.

---

## 3. Configure the Android Project

Apply the required Firebase dependencies and Gradle configuration to your Android build templates.

This includes:

- Firebase BoM
- Firebase Messaging
- Firebase Analytics (optional)
- Google Services configuration (if applicable)
- Apply same intergration on AndroidManifest and build templates

---

## 4. Update `PythonActivity`

Modify `PythonActivity.java` so that notification data is preserved whether the application is launched from a terminated state or is already running.

### Declare a static bundle

```java
public static Bundle mIntentBundle = null;
```

### Update `onCreate()`

```java
@Override
protected void onCreate(Bundle savedInstanceState) {

    ...

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
    }

    ...

}
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