package com.github.michalbednarski.intentslab.sandbox.remote;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

/**
 * Sandbox service for running as isolaredProcess on versions where it's supported
 */
public class IsolatedService extends Service {
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return new SandboxImpl(this);
    }
}
