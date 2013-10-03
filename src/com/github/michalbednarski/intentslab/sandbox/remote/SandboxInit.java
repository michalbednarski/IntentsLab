package com.github.michalbednarski.intentslab.sandbox.remote;

import android.app.Service;
import android.os.IBinder;

/**
 * Entry point for remote sandbox service
 */
public class SandboxInit {
    /**
     * This is called by sandbox service in onBind
     */
    public static IBinder sandboxServiceOnBind(Service service) {
        return new SandboxImpl(service);
    }
}
