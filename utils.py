from jnius import autoclass, java_method, PythonJavaClass
from kivy.clock import Clock
import json

def get_token():
    FirebaseMessaging = autoclass('com.google.firebase.messaging.FirebaseMessaging')
    FirebaseMessaging.getInstance().getToken().addOnSuccessListener(MyTokenListener())

class MyTokenListener(PythonJavaClass):
    __javainterfaces__ = ['com/google/android/gms/tasks/OnSuccessListener']
    __javacontext__ = "app"

    @java_method("(Ljava/lang/Object;)V")
    def onSuccess(self, s):
        print(s)
        
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
