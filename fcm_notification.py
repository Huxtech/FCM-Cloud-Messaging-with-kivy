from jnius import autoclass, java_method, PythonJavaClass
from kivy.clock import Clock
import json

def get_fcm_notification_data()-> dict:
    PythonActivity = autoclass('org.kivy.android.PythonActivity')
    bundle = PythonActivity.mIntentBundle
    notification_data = {}
    if bundle is not None:
        for key in bundle.keySet():
            notification_data[key] = bundle.get(key)
    else:
        print("onResume: bundle is None")
    def clear_bundle():
        PythonActivity.mActivity.resetMIntentBundle()
    Clock.schedule_once(lambda x: clear_bundle(), 1)
    return notification_data

def get_fcm_notification_token(callback):
    FirebaseMessaging = autoclass('com.google.firebase.messaging.FirebaseMessaging')
    FirebaseMessaging.getInstance().getToken().addOnSuccessListener(TokenListener(callback))

class TokenListener(PythonJavaClass):
    __javainterfaces__ = ['com/google/android/gms/tasks/OnSuccessListener']
    __javacontext__ = "app"
    
    def __init__(self, callback):
        super().__init__()
        self.callback = callback

    @java_method("(Ljava/lang/Object;)V")
    def onSuccess(self, token):
        # print(token)
        Clock.schedule_once(lambda dt: self.callback(token), 0)
        
class NotificationListener(PythonJavaClass):
    __javainterfaces__ = ["org/kivy/NotificationBridge$MessageListener"]
    __javacontext__ = "app"
    
    def __init__(self, callback):
        super().__init__()
        self.callback = callback

    @java_method("(Lorg/json/JSONObject;)V")
    def onMessage(self, json_obj):
        
        if json_obj is not None:
            json_str = json_obj.toString()
            py_data = json.loads(json_str)
        else:
            py_data = {}

        Clock.schedule_once(lambda dt: self.callback(py_data), 0)
        
class IntentMessageListener(PythonJavaClass):
    __javainterfaces__ = ["org/kivy/NotificationBridge$IntentMessageListener"]
    __javacontext__ = "app"
    
    def __init__(self, callback):
        super().__init__()
        self.callback = callback

    @java_method("(Landroid/os/Bundle;)V")
    def onIntentMessage(self, bundle):
        notification_data = {}
        if bundle is not None:
            for key in bundle.keySet():
                notification_data[key] = bundle.get(key)
        
            Clock.schedule_once(lambda dt: self.callback(notification_data), 0)
