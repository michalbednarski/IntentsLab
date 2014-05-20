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
