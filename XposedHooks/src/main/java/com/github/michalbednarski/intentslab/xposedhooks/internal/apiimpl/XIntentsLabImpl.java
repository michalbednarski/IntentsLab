package com.github.michalbednarski.intentslab.xposedhooks.internal.apiimpl;

import android.app.PendingIntent;
import android.os.RemoteException;

import com.github.michalbednarski.intentslab.xposedhooks.api.IntentTracker;
import com.github.michalbednarski.intentslab.xposedhooks.api.XIntentsLab;
import com.github.michalbednarski.intentslab.xposedhooks.internal.IInternalInterface;
import com.github.michalbednarski.intentslab.xposedhooks.internal.XHUtils;

/**
 * Created by mb on 01.03.14.
 */
public class XIntentsLabImpl implements XIntentsLab {

    private IInternalInterface mInterface = XHUtils.getSystemInterface();

    private static int sSystemPid = 0;
    static int getSystemPid() {
        if (sSystemPid == 0) {
            try {
                sSystemPid = XHUtils.getSystemInterface().getSystemPid();
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
        }
        return sSystemPid;
    }


    @Override
    public boolean havePermission() {
        try {
            return mInterface.havePermission();
        } catch (RemoteException e) {
            throw new RuntimeException("Service error");
        }
    }

    @Override
    public PendingIntent getRequestPermissionIntent(String packageName) {
        try {
            return mInterface.getRequestPermissionPendingIntent(packageName);
        } catch (RemoteException e) {
            throw new RuntimeException("Service error");
        }
    }

    @Override
    public boolean supportsObjectTracking() {
        return true;
    }

    @Override
    public IntentTracker createIntentTracker() {
        return new IntentTrackerImpl();
    }
}
