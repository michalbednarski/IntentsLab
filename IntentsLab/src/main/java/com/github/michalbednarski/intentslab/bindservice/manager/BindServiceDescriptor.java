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

package com.github.michalbednarski.intentslab.bindservice.manager;

import android.app.IServiceConnection;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;

import com.github.michalbednarski.intentslab.Utils;
import com.github.michalbednarski.intentslab.runas.IRemoteInterface;
import com.github.michalbednarski.intentslab.runas.RunAsManager;
import com.github.michalbednarski.intentslab.sandbox.SandboxManager;

/**
 * Service descriptor for services obtained through {@link Context#bindService(Intent, ServiceConnection, int)}
 */
public class BindServiceDescriptor extends ServiceDescriptor {

    private final Intent mIntent;

    public BindServiceDescriptor(Intent intent) {
        mIntent = intent;
    }

    @Override
    public String getTitle() {
        String action = mIntent.getAction();
        boolean hasAction = !Utils.stringEmptyOrNull(action);
        ComponentName component = mIntent.getComponent();
        boolean hasComponent = component != null;
        return Utils.afterLastDot((!hasAction && hasComponent) ? component.getClassName() : action);
    }

    // Parcelable
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(mIntent, 0);
    }

    public static final Creator<ServiceDescriptor> CREATOR = new Creator<ServiceDescriptor>() {
        @Override
        public ServiceDescriptor createFromParcel(Parcel source) {
            return new BindServiceDescriptor(source.<Intent>readParcelable(null));
        }

        @Override
        public ServiceDescriptor[] newArray(int size) {
            return new ServiceDescriptor[size];
        }
    };

    public boolean equals(Object o) {
        if (o instanceof BindServiceDescriptor) {
            BindServiceDescriptor b = (BindServiceDescriptor) o;
            return b.mIntent.filterEquals(b.mIntent);
        }
        return false;
    }

    public int hashCode() {
        return mIntent.filterHashCode();
    }

    @Override
    ConnectionManager getConnectionManager() {
        return new ConnectionManagerImpl(mIntent);
    }

    private static class ConnectionManagerImpl extends ConnectionManager implements ServiceConnection {

        private final Intent mIntent;

        public ConnectionManagerImpl(Intent intent) {
            mIntent = intent;
        }


        @Override
        void bind() {
            try {
                SandboxManager.getService().bindService(mIntent, this, Context.BIND_AUTO_CREATE);
            } catch (SecurityException e) {
                try {
                    IRemoteInterface remoteInterface = RunAsManager.getRemoteInterfaceForSystemDebuggingCommands();
                    remoteInterface.bindService(
                            SandboxManager.getSandbox().getApplicationToken(),
                            mIntent,
                            new IServiceConnection.Stub() {
                                @Override
                                public void connected(ComponentName name, IBinder service) throws RemoteException {
                                    onServiceConnected(name, service);
                                }
                            }
                    );
                } catch (RemoteException e1) {
                    e1.printStackTrace();
                }
            }
        }

        @Override
        void unbind() {
            SandboxManager.getService().unbindService(this);
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mHelper.mPackageName = name.getPackageName();
            mHelper.dispatchBound(service);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {}
    }
}
