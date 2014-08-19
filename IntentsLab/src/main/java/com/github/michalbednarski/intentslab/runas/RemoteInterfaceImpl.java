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

import android.app.IActivityController;
import android.app.IServiceConnection;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

/**
 * Class providing remote interface
 */
class RemoteInterfaceImpl extends IRemoteInterface.Stub {

    @Override
    public Bundle startActivity(Intent intent, IBinder token, int requestCode) throws RemoteException {
        Bundle result = new Bundle();
        try {
            ActivityManagerWrapper.get().startActivity(
                    "intent", intent,
                    "resultTo", token,
                    "requestCode", requestCode
            );
        } catch (InvocationTargetException e) {
            result.putSerializable("exception", e.getTargetException());
        }
        return result;
    }

    @Override
    public int bindService(IBinder sandboxApplicationToken, Intent intent, IServiceConnection conn) throws RemoteException {
        return ActivityManagerWrapper.get().bindService(sandboxApplicationToken, intent, conn);
    }

    @Override
    public void setActivityController(IActivityController controller) throws RemoteException {
        ActivityManagerWrapper.get().setActivityController(controller);
    }

    @Override
    public void dumpServiceAsync(IBinder service, ParcelFileDescriptor fd, String[] args) throws RemoteException {
        // This is oneway call and therefore it's already async so we behave normally here
        service.dump(fd.getFileDescriptor(), args);
        try {
            fd.close();
        } catch (IOException e) {
            // ignore
            e.printStackTrace();
        }
    }

    @Override
    public IBinder createOneShotProxyBinder(final IBinder binder) {
        return new Binder() {
            boolean mDone = false;

            @Override
            protected synchronized boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
                if (mDone) {
                    throw new SecurityException("One shot proxy binder already used");
                }
                mDone = true;
                return binder.transact(code, data, reply, flags);
            }
        };
    }

    @Override
    public Intent[] getRecentTasks() throws RemoteException {
        return ActivityManagerWrapper.get().getRecentTasks();
    }
}
