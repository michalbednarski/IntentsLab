package com.github.michalbednarski.intentslab.sandbox.remote;

import android.app.Service;
import android.os.IBinder;
import android.os.RemoteException;
import com.github.michalbednarski.intentslab.sandbox.IAidlInterface;
import com.github.michalbednarski.intentslab.sandbox.ISandbox;

/**
 * Created by mb on 01.10.13.
 */
class SandboxImpl extends ISandbox.Stub {
    final Service mService;

    SandboxImpl(Service service) {
        mService = service;
    }

    @Override
    public IAidlInterface queryInterface(IBinder binder, String fromPackage) throws RemoteException {
        try {
            return new SandboxedAidlInterfaceImpl(binder, fromPackage, mService);
        } catch (SandboxedAidlInterfaceImpl.UnknownInterfaceException e) {
            return null;
        }
    }
}
