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
