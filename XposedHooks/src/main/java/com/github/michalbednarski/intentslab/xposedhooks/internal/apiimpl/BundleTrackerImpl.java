/*
 * IntentsLab - Android app for playing with Intents and Binder IPC
 * Copyright (C) 2014 Michał Bednarski
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

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
