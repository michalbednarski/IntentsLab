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

import android.os.IBinder;
import android.os.RemoteException;

import com.github.michalbednarski.intentslab.sandbox.IAidlInterface;
import com.github.michalbednarski.intentslab.sandbox.InvokeMethodResult;
import com.github.michalbednarski.intentslab.sandbox.SandboxedMethod;
import com.github.michalbednarski.intentslab.sandbox.SandboxedObject;

/**
 * Created by mb on 01.10.13.
 */
public class SandboxedAidlInterfaceImpl extends IAidlInterface.Stub {

    private final LocalAidlInterface mWrappedInterface;

    public SandboxedAidlInterfaceImpl(IBinder binder, ClassLoader classLoader, String interfaceDescriptor) throws RemoteException, UnknownInterfaceException {
        // Get interface name
        try {
            // Load interface class
            Class<?> stubClass;
            stubClass = classLoader.loadClass(interfaceDescriptor + "$Stub");

            Object object = AidlInterface.wrapInterface(stubClass, binder);
            mWrappedInterface = new LocalAidlInterface(object, stubClass, null);

        } catch (Exception e) {
            throw new UnknownInterfaceException();
        }
    }

    @Override
    public SandboxedMethod[] getMethods() throws RemoteException {
        return mWrappedInterface.getMethods();
    }

    @Override
    public InvokeMethodResult invokeMethod(int methodNumber, SandboxedObject[] arguments) throws RemoteException {
        return mWrappedInterface.invokeMethod(methodNumber, arguments);
    }

    @Override
    public InvokeMethodResult invokeMethodUsingBinder(IBinder binder, int methodNumber, SandboxedObject[] arguments) throws RemoteException {
        return mWrappedInterface.invokeMethodUsingBinder(binder, methodNumber, arguments);
    }

    public static class UnknownInterfaceException extends Exception {}
}
