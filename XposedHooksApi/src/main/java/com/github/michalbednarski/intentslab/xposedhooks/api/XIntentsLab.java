package com.github.michalbednarski.intentslab.xposedhooks.api;

import android.app.PendingIntent;

/**
 * Api for [IntentsLab]XposedHooks module
 */
public interface XIntentsLab {
    /**
     * Check if we have permission to use module
     */
    boolean havePermission();

    /**
     * Get PendingIntent for requesting permission
     *
     * When used with
     * {@link android.app.Activity#startIntentSenderForResult(android.content.IntentSender, int, android.content.Intent, int, int, int)}
     * result code will be {@link android.app.Activity#RESULT_OK} if permission was granted
     */
    PendingIntent getRequestPermissionIntent(String packageName);



    /**
     * True if module supports object tracking
     */
    boolean supportsObjectTracking();

    /**
     * Create an {@link IntentTracker}
     */
    IntentTracker createIntentTracker();
}
