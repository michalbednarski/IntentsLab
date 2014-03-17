package com.github.michalbednarski.intentslab.xposedhooks.internal.apiimpl;

import android.os.Bundle;
import android.os.RemoteException;

import com.github.michalbednarski.intentslab.xposedhooks.api.BundleTracker;
import com.github.michalbednarski.intentslab.xposedhooks.internal.IBundleTracker;
import com.github.michalbednarski.intentslab.xposedhooks.internal.StackTraceWrapper;
import com.github.michalbednarski.intentslab.xposedhooks.internal.XHUtils;
import com.github.michalbednarski.intentslab.xposedhooks.internal.trackers.ObjectTrackerModule;

import java.util.HashSet;

/**
 * API implementation for Bundle Tracker
 */
public class BundleTrackerImpl extends BaseTrackerImpl implements BundleTracker {
    private final HashSet<String> mReadExtras = new HashSet<String>();


    private final IBundleTracker.Stub mStub = new IBundleTracker.Stub() {
        @Override
        public void reportRead(String itemName, String methodName, StackTraceWrapper wrappedStackTrace) throws RemoteException {
            synchronized (mReadExtras) {
                mReadExtras.add(itemName);
                dispatchUpdate();
            }
        }
    };

    BundleTrackerImpl() {
        try {
            XHUtils.getSystemInterface().allowTrackingTag(mStub);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Set tag for bundle without cloning it
     */
    void tagBundleInner(Bundle bundle) {
        ObjectTrackerModule.setTrackerStatic(bundle, mStub);
    }

    @Override
    public String[] getExtrasRead() {
        return mReadExtras.toArray(new String[mReadExtras.size()]);
    }
}
