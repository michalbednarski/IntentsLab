package com.github.michalbednarski.intentslab.xposedhooks.internal.apiimpl;

import android.content.Intent;
import android.os.Bundle;
import android.os.RemoteException;

import com.github.michalbednarski.intentslab.xposedhooks.api.BundleTracker;
import com.github.michalbednarski.intentslab.xposedhooks.api.IntentTracker;
import com.github.michalbednarski.intentslab.xposedhooks.internal.IIntentTracker;
import com.github.michalbednarski.intentslab.xposedhooks.internal.XHUtils;
import com.github.michalbednarski.intentslab.xposedhooks.internal.trackers.ObjectTrackerModule;

import de.robv.android.xposed.XposedHelpers;

/**
 * Created by mb on 06.03.14.
 */
class IntentTrackerImpl extends BaseTrackerImpl implements IntentTracker {
    private boolean mActionRead = false;
    private final BundleTrackerImpl mExtrasTracker = new BundleTrackerImpl();

    private IIntentTracker.Stub mStub = new IIntentTracker.Stub() {
        @Override
        public void reportActionRead() throws RemoteException {
            synchronized (IntentTrackerImpl.this) {
                int callingPid = getCallingPid();
                if (
                        !mActionRead && // Not read already
                        (callingPid != 0 || getCallingUid() != 2000) && // System/buggy way
                        callingPid != XIntentsLabImpl.getSystemPid()) { // System
                    mActionRead = true;
                    dispatchUpdate();
                }
            }
        }
    };

    IntentTrackerImpl() {
        try {
            XHUtils.getSystemInterface().allowTrackingTag(mStub.asBinder());
        } catch (RemoteException e) {
            // System haven't our module?!
            throw new RuntimeException(e);
        }
    }

    @Override
    public Intent tagIntent(Intent intent) {
        intent = new Intent(intent);

        // Tag extras
        Bundle extras = (Bundle) XposedHelpers.getObjectField(intent, "mExtras");
        if (extras == null) {
            extras = new Bundle();
        }
        mExtrasTracker.tagBundleInner(extras);
        XposedHelpers.setObjectField(intent, "mExtras", extras);

        // Tag intent itself
        ObjectTrackerModule.setTrackerStatic(intent, mStub);

        return intent;
    }

    @Override
    public boolean actionRead() {
        return mActionRead;
    }

    @Override
    public BundleTracker getExtrasTracker() {
        return mExtrasTracker;
    }
}
