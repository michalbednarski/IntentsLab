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

package com.github.michalbednarski.intentslab.runas;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;

/**
 * Receiver for querying for {@link IRemoteInterfaceRequest} instance
 */
public class RunAsInitReceiver extends BroadcastReceiver {
    static final String RESULT_EXTRA_REMOTE_INTERFACE_REQUEST = "_rir";
    private static RemoteInterfaceRequestImpl sRemoteInterfaceRequest;

    @Override
    public void onReceive(Context context, Intent intent) {
        // Create RemoteInterfaceRequest
        if (sRemoteInterfaceRequest == null) {
            sRemoteInterfaceRequest = new RemoteInterfaceRequestImpl();
        }

        // Post it as broadcast result
        Bundle extras = new Bundle();
        putBinderInBundle(extras, RESULT_EXTRA_REMOTE_INTERFACE_REQUEST, sRemoteInterfaceRequest);
        setResult(0, "", extras);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    public static void putBinderInBundle(Bundle bundle, String key, IBinder binder) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            bundle.putBinder(key, binder);
        } else {
            try {
                Bundle.class.getMethod("putIBinder", String.class, IBinder.class).invoke(bundle, key, binder);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static class RemoteInterfaceRequestImpl extends IRemoteInterfaceRequest.Stub {
        @Override
        public boolean setRemoteInterface(IRemoteInterface remoteInterface) throws RemoteException {
            if (remoteInterface == null) {
                return false;
            }
            return RunAsManager.registerRemote(Binder.getCallingUid(), remoteInterface);
        }
    }
}
