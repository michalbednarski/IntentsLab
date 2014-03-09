package com.github.michalbednarski.intentslab.xposedhooks.internal;

import com.github.michalbednarski.intentslab.xposedhooks.internal.IRefreshPermissionsCallback;

/**
 * Internal interface for system service
 *
 * This is not public api, use [IntentsLab]XposedHooksApi library instead
 * This interface won't be kept backwards-compatible
 */
interface IInternalInterface {
    boolean havePermission();

    oneway void refreshPermissions(IRefreshPermissionsCallback callback);

    PendingIntent getRequestPermissionPendingIntent(String packageName);

    void allowTrackingTag(IBinder tag);

    boolean isTrackingTagAllowed(IBinder tag);

    int getSystemPid();
}
