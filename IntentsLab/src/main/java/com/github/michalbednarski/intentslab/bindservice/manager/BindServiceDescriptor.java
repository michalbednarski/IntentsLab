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
import android.content.pm.ResolveInfo;
import android.os.Handler;
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

    // equals() and hashCode()
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


    // Actual binding to service
    private static class ConnectionManagerImpl implements ServiceConnection, Runnable {
        private final BindServiceManager.Helper.BindServiceMediator mCallback;

        private ConnectionManagerImpl(BindServiceManager.Helper.BindServiceMediator callback) {
            mCallback = callback;
        }

        @Override
        public void run() { // unbind
            mCallback.getContext().unbindService(this);
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mCallback.setPackageName(name.getPackageName());
            mCallback.dispatchBound(service);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {}
    }

    @Override
    void doBind(final BindServiceManager.Helper.BindServiceMediator callback) {
        Context context = callback.getContext();

        try {
            // Try using standard bindService()
            final ConnectionManagerImpl connection = new ConnectionManagerImpl(callback);
            context.bindService(mIntent, connection, Context.BIND_AUTO_CREATE);
            callback.registerUnbindRunnable(connection);

        } catch (SecurityException e) {
            // We don't have permission to bind this service,
            // Try using run-as mode
            try {
                // Get required permission
                ResolveInfo resolvedService = context.getPackageManager().resolveService(mIntent, 0);
                String permission = resolvedService.serviceInfo.permission;

                // Get remote interface with that permission
                final IRemoteInterface remoteInterface = RunAsManager.getRemoteInterfaceHavingPermission(context, permission);
                if (remoteInterface == null) {
                    throw new Exception("No interface with permission");
                }

                // Prepare sandbox to get service token
                callback.refSandbox();
                final Handler handler = new Handler();
                SandboxManager.initSandboxAndRunWhenReady(context, new Runnable() {
                    @Override
                    public void run() {
                        try {

                            // Call bindService() using remote interface
                            final IServiceConnection.Stub conn = new IServiceConnection.Stub() {
                                @Override
                                public void connected(final ComponentName name, final IBinder service) throws RemoteException {
                                    handler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            callback.setPackageName(name.getPackageName());
                                            callback.dispatchBound(service);
                                        }
                                    });
                                }
                            };
                            remoteInterface.bindService(
                                    SandboxManager.getSandbox().getApplicationToken(),
                                    mIntent,
                                    conn
                            );

                            // Register unbind callback
                            callback.registerUnbindRunnable(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        Object am =
                                                Class.forName("android.app.ActivityManagerNative")
                                                .getMethod("getDefault")
                                                .invoke(null);
                                        am.getClass()
                                                .getMethod("unbindService", IServiceConnection.class)
                                                .invoke(am, conn);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                            });
                        } catch (RemoteException e1) {
                            e1.printStackTrace();
                        }
                    }
                });
            } catch (Exception e1) {
                e1.printStackTrace();
                // TODO: send failure message
            }
        }
    }
}
