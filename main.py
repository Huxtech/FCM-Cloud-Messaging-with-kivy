from kivy.app import App
from kivy.uix.button import Button
from kivy.uix.boxlayout import BoxLayout
from kivy.lang.builder import Builder
from kivy.properties import DictProperty
from kivy.clock import Clock
from utils import NotificationListener

from jnius import autoclass
from permissions import handle_android_permissions

KV = '''
#:import utils utils
BoxLayout:
    orientation: "vertical"
    ScrollableLabel:
        text: str(app.notification_data)
        font_size: 50
        text_size: self.width, None
        size_hint_y: None
        height: self.texture_size[1]
    Button:
        text: "get token"
        on_release: utils.get_token()

<ScrollableLabel@Label+ScrollView>

'''


class NotificationApp(App):
    notification_data = DictProperty(rebind=True)

    def on_start(self, **kwargs):
        PythonActivity = autoclass('org.kivy.android.PythonActivity')
        bundle = PythonActivity.mIntentBundle
        if bundle is not None:
            for key in bundle.keySet():
                self.notification_data[key] = bundle.get(key)
        else:
            print("onStart: bundle is None")
        self.dispatch("on_resume")

    def on_resume(self, **kwargs):
        PythonActivity = autoclass('org.kivy.android.PythonActivity')
        bundle = PythonActivity.mIntentBundle
        if bundle is not None:
            for key in bundle.keySet():
                self.notification_data[key] = bundle.get(key)
        else:
            print("onResume: bundle is None")
        activity = PythonActivity.mActivity
        Clock.schedule_once(lambda _: activity.setAppIsReady(True))
    
    
    def callback(self, json):
        self.notification_data = json

    def build(self):
        NotificationBridge = autoclass("org.kivy.NotificationBridge")
        listener = NotificationListener(self.callback)
        NotificationBridge.setListener(listener)

        Clock.schedule_once(lambda dt: handle_android_permissions(), 7)
        return Builder.load_string(KV)

if __name__ == "__main__":
    NotificationApp().run()