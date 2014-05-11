package com.github.michalbednarski.intentslab.xposedhooks.internal.apiimpl;

import android.os.Bundle;
import android.os.RemoteException;

import com.github.michalbednarski.intentslab.xposedhooks.api.BundleTracker;
import com.github.michalbednarski.intentslab.xposedhooks.api.ReadBundleEntryInfo;
import com.github.michalbednarski.intentslab.xposedhooks.internal.IBundleTracker;
import com.github.michalbednarski.intentslab.xposedhooks.internal.XHUtils;
import com.github.michalbednarski.intentslab.xposedhooks.internal.trackers.ObjectTrackerModule;

import java.util.HashSet;

/**
 * API implementation for Bundle Tracker
 */
public class BundleTrackerImpl extends BaseTrackerImpl implements BundleTracker {
    private final HashSet<ReadBundleEntryInfo> mReadExtras = new HashSet<ReadBundleEntryInfo>();
    private final HashSet<String> mReadExtrasLegacy = new HashSet<String>();


    private final IBundleTracker.Stub mStub = new IBundleTracker.Stub() {
        @Override
        public void reportRead(ReadBundleEntryInfo info) throws RemoteException {
            synchronized (mReadExtrasLegacy) {
                mReadExtras.add(info);
                mReadExtrasLegacy.add(info.name);
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
        return mReadExtrasLegacy.toArray(new String[mReadExtrasLegacy.size()]);
    }

    @Override
    public ReadBundleEntryInfo[] getReadEntries() {
        return mReadExtras.toArray(new ReadBundleEntryInfo[mReadExtras.size()]);
    }
}
