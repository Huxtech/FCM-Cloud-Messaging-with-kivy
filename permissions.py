from jnius import autoclass


Environment = autoclass('android.os.Environment')
Context = autoclass('android.content.Context')
PythonActivity = autoclass('org.kivy.android.PythonActivity')
Uri = autoclass('android.net.Uri')
Intent = autoclass('android.content.Intent')
Settings = autoclass('android.provider.Settings')




def get_not_granted_permissions(perms):
    from android.permissions import check_permission
    not_granted = []
    for perm in perms:
        if check_permission(perm) != True:
            not_granted.append(perm)
    return not_granted


def handle_android_permissions():
    from android.permissions import Permission, request_permissions
    
    def callback(permissions, results):
        granted_permissions = [perm for perm, res in zip(permissions, results) if res]
        denied_permissions = [perm for perm, res in zip(permissions, results) if not res]

        if denied_permissions:
            print('Denied permissions:', denied_permissions)

        elif granted_permissions:
            print('Got all permissions')
        else:
            print('No permissions were granted or denied')

    perms = [
        Permission.POST_NOTIFICATIONS,
    ]

    not_granted = get_not_granted_permissions(perms)
    if not_granted:
        request_permissions(not_granted, callback)

