package com.github.michalbednarski.intentslab.runas;

import android.content.IIntentReceiver;
import android.content.Intent;
import android.os.Bundle;

/**
 * Class that provides android-version-independent {@link android.content.IIntentReceiver}
 */
public abstract class RealIIntentReceiver extends IIntentReceiver.Stub {

    public final void performReceive(
            Intent intent,
            int resultCode,
            String data,
            Bundle extras,
            boolean ordered,
            boolean sticky,
            int sendingUser
    ) {
        performReceive(intent, resultCode, data, extras, ordered);
    }

    public final void performReceive(
            Intent intent,
            int resultCode,
            String data,
            Bundle extras,
            boolean ordered,
            boolean sticky
    ) {
        performReceive(intent, resultCode, data, extras, ordered);
    }

    /**
     * Actual cross-android-version method that should be overridden
     */
    public abstract void performReceive(
            Intent intent,
            int resultCode,
            String data,
            Bundle extras,
            boolean ordered
    );
}
