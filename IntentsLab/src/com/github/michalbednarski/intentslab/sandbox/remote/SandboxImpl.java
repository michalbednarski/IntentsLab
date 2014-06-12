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
import com.github.michalbednarski.intentslab.sandbox.SandboxedObject;

import java.lang.reflect.Field;

/**
 * Created by mb on 01.10.13.
 */
class SandboxImpl extends ISandbox.Stub {
    private final Service mService;
    private final UntrustedCodeLoader mUntrustedCodeLoader;

    SandboxImpl(Service service) {
        mService = service;
        mUntrustedCodeLoader = new UntrustedCodeLoader(service);
    }

    @Override
    public IAidlInterface queryInterface(IBinder binder, ClassLoaderDescriptor classLoaderDescriptor) throws RemoteException {
        try {
            return new SandboxedAidlInterfaceImpl(binder, mUntrustedCodeLoader.getClassLoader(classLoaderDescriptor));
        } catch (SandboxedAidlInterfaceImpl.UnknownInterfaceException e) {
            return null;
        }
    }

    @Override
    public ISandboxedBundle sandboxBundle(Bundle bundle, ClassLoaderDescriptor classLoaderDescriptor) throws RemoteException {
        return new SandboxedBundleImpl(bundle, mUntrustedCodeLoader.getClassLoader(classLoaderDescriptor));
    }

    @Override
    public ISandboxedObject sandboxObject(SandboxedObject wrappedObject, ClassLoaderDescriptor classLoaderDescriptor) throws RemoteException {
        final ClassLoader classLoader = mUntrustedCodeLoader.getClassLoader(classLoaderDescriptor);
        return new SandboxedObjectImpl(wrappedObject.unwrap(classLoader), classLoader);
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
