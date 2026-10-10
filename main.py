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