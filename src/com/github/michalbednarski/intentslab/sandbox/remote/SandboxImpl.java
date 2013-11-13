package com.github.michalbednarski.intentslab.sandbox.remote;

import android.app.Service;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import com.github.michalbednarski.intentslab.sandbox.ClassLoaderDescriptor;
import com.github.michalbednarski.intentslab.sandbox.IAidlInterface;
import com.github.michalbednarski.intentslab.sandbox.ISandbox;
import com.github.michalbednarski.intentslab.sandbox.ISandboxedBundle;

/**
 * Created by mb on 01.10.13.
 */
class SandboxImpl extends ISandbox.Stub {
    final Service mService;

    SandboxImpl(Service service) {
        mService = service;
    }

    @Override
    public IAidlInterface queryInterface(IBinder binder, ClassLoaderDescriptor fromPackage) throws RemoteException {
        try {
            return new SandboxedAidlInterfaceImpl(binder, fromPackage, mService);
        } catch (SandboxedAidlInterfaceImpl.UnknownInterfaceException e) {
            return null;
        }
    }

    @Override
    public ISandboxedBundle sandboxBundle(Bundle bundle, ClassLoaderDescriptor classLoaderDescriptor) throws RemoteException {
        return new SandboxedBundleImpl(bundle, classLoaderDescriptor, mService);
    }
}
