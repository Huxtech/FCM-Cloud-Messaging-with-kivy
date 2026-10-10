import firebase_admin

from firebase_admin import credentials
from firebase_admin import messaging


# --------------------------------------------------
# Firebase initialization
# --------------------------------------------------

cred = credentials.Certificate(
    "kivy-cloud-messaging-acf85-firebase-adminsdk-fbsvc-3cb1222f57.json"
)

firebase_admin.initialize_app(cred)


# --------------------------------------------------
# FCM data-only message
# --------------------------------------------------

def send_notification(
    token: str,
    title: str,
    body: str,
    image_url: str | None = None,
    data: dict | None = None,
):
    # FCM requires every value in "data" to be a string
    payload = {str(k): str(v) for k, v in (data or {}).items()}

    # These three keys must match what MyFcmService reads
    payload["title"] = title
    payload["body"] = body
    if image_url:
        payload["imageUrl"] = image_url

    message = messaging.Message(
        token=token,

        # NO notification block: this makes it a data-only message,
        # so onMessageReceived() runs in every app state
        data=payload,

        android=messaging.AndroidConfig(
            priority="high",
            ttl=3600,  # drop the message if it can't be delivered within 1 hour
        ),
    )

    response = messaging.send(message)

    print("Message sent successfully.")
    print("Message ID:", response)

    return response


# --------------------------------------------------
# Send message
# --------------------------------------------------

if __name__ == "__main__":

    FCM_TOKEN = "fi66DOwyR_qWNJwaJ7n7UK:APA91bG9BtjbKaPsbvbNLEHSvFAMHvrC8PqGvaGn0dszZ4P2QGaI3KblZ8OCgUUg9_LGhytfpxTVZw13owIAvW6G30m57RTVtiuCTtXhmIYWxH0aZ7G148Q"

    send_notification(
        token=FCM_TOKEN,
        title="Sporty Target 4",
        body="Your prediction is ready.",
        image_url="https://img.magnific.com/free-photo/closeup-shot-beautiful-butterfly-with-interesting-textures-orange-petaled-flower_181624-7640.jpg",  # must be https, or use None

        data={
            "type": "prediction 55",
            "prediction_id": "12348",
            "screen": "prediction_detail",
        },
    )