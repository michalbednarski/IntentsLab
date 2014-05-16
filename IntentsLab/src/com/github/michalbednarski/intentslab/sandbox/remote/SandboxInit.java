package com.github.michalbednarski.intentslab.sandbox.remote;

import android.app.Service;
import android.os.IBinder;

/**
 * Entry point for remote sandbox service
 */
public class SandboxInit {
    private static boolean sOkayToLoadUntrustedCode = false;

    /**
     * This is called by sandbox service in onBind
     */
    public static IBinder sandboxServiceOnBind(Service service) {
        sOkayToLoadUntrustedCode = true;
        return new SandboxImpl(service);
    }

    public static void ensureItsOkayToLoadUntrustedCode() {
        if (!sOkayToLoadUntrustedCode) {
            throw new SecurityException("Not allowed to run untrusted code in this process");
        }
    }
}
