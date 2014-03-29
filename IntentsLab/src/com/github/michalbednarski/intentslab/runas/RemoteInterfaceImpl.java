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
}
