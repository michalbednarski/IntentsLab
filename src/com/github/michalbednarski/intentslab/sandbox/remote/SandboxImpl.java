package com.github.michalbednarski.intentslab.sandbox.remote;

import android.app.Service;
import android.content.Context;
import android.content.ContextWrapper;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.RemoteException;
import com.github.michalbednarski.intentslab.sandbox.ClassLoaderDescriptor;
import com.github.michalbednarski.intentslab.sandbox.IAidlInterface;
import com.github.michalbednarski.intentslab.sandbox.ISandbox;
import com.github.michalbednarski.intentslab.sandbox.ISandboxedBundle;
import com.github.michalbednarski.intentslab.sandbox.ISandboxedObject;
import com.github.michalbednarski.intentslab.sandbox.SandboxManager;

import java.lang.reflect.Field;

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

    @Override
    public ISandboxedObject sandboxObject(Bundle wrappedObject, ClassLoaderDescriptor classLoaderDescriptor) throws RemoteException {

        final ClassLoader classLoader = classLoaderDescriptor.getClassLoader(mService);
        wrappedObject.setClassLoader(classLoader);
        return new SandboxedObjectImpl(SandboxManager.unwrapObject(wrappedObject), classLoader);
    }

    @Override
    public IBinder getApplicationToken() throws RemoteException {
        try {
            // Find ContextImpl
            Context context = mService;
            while (context instanceof ContextWrapper) {
                context = ((ContextWrapper) context).getBaseContext();
            }

            // Get ContextImpl.mMainThread
            Field mainThreadField = Class.forName("android.app.ContextImpl").getDeclaredField("mMainThread");
            mainThreadField.setAccessible(true);
            Object mainThread = mainThreadField.get(context);

            // Get application thread and return it as binder
            Object applicationThread = Class.forName("android.app.ActivityThread").getMethod("getApplicationThread").invoke(mainThread);
            return ((IInterface) applicationThread).asBinder();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
