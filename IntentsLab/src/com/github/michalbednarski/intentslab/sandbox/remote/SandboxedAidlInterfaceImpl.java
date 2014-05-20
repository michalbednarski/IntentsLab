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
import android.os.IBinder;
import android.os.RemoteException;

import com.github.michalbednarski.intentslab.Utils;
import com.github.michalbednarski.intentslab.sandbox.ClassLoaderDescriptor;
import com.github.michalbednarski.intentslab.sandbox.IAidlInterface;
import com.github.michalbednarski.intentslab.sandbox.InvokeMethodResult;
import com.github.michalbednarski.intentslab.sandbox.SandboxedMethod;
import com.github.michalbednarski.intentslab.sandbox.SandboxedObject;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by mb on 01.10.13.
 */
class SandboxedAidlInterfaceImpl extends IAidlInterface.Stub {
    private static final Pattern AOSP_MANUAL_AIDL_PATTERN = Pattern.compile("(android\\.(\\w+\\.)+)I(\\w+)");
    private final Object mObject;
    private final String mInterfaceDescriptor;
    private final SandboxedMethod[] mSandboxedMethods;
    private final Method[] mMethods;
    private final ClassLoader mClassLoader;
    private final Method mAsInterfaceMethod;

    SandboxedAidlInterfaceImpl(IBinder binder, ClassLoaderDescriptor fromPackage, Service service) throws RemoteException, UnknownInterfaceException {
        // Get interface name
        mInterfaceDescriptor = binder.getInterfaceDescriptor();
        if (Utils.stringEmptyOrNull(mInterfaceDescriptor)) {
            throw new UnknownInterfaceException();
        }
        try {
            // Load interface class
            mClassLoader = fromPackage.getClassLoader(service);
            Class<?> stub = null;
            try {
                stub = mClassLoader.loadClass(mInterfaceDescriptor + "$Stub");
            } catch (ClassNotFoundException e) {
                Matcher matcher = AOSP_MANUAL_AIDL_PATTERN.matcher(mInterfaceDescriptor);
                if (matcher.find()) {
                    stub = mClassLoader.loadClass(matcher.group(1) + matcher.group(3) + "Native");

                }
            }


            mAsInterfaceMethod = stub.getMethod("asInterface", IBinder.class);
            mObject = mAsInterfaceMethod.invoke(null, binder);

            // Get it's methods
            final Method[] methods = stub.getMethods();
            ArrayList<SandboxedMethod> sandboxedMethods = new ArrayList<SandboxedMethod>();
            ArrayList<Method> filteredMethods = new ArrayList<Method>();
            for (Method method : methods) {
                if (Modifier.isAbstract(method.getModifiers())) {
                    sandboxedMethods.add(new SandboxedMethod(method));
                    filteredMethods.add(method);
                }
            }
            mSandboxedMethods = sandboxedMethods.toArray(new SandboxedMethod[sandboxedMethods.size()]);
            mMethods = filteredMethods.toArray(new Method[filteredMethods.size()]);
        } catch (Exception e) {
            throw new UnknownInterfaceException();
        }
    }

    static boolean isAidlInterfaceClass(Class<?> aClass) {
        return aClass.getName().endsWith("$Stub$Proxy");
    }

    @Override
    public String getInterfaceName() throws RemoteException {
        return mInterfaceDescriptor;
    }

    @Override
    public SandboxedMethod[] getMethods() throws RemoteException {
        return mSandboxedMethods;
    }


    private InvokeMethodResult doInvokeMethod(Object object, int methodNumber, SandboxedObject[] wrappedArguments) throws RemoteException {
        InvokeMethodResult requestResult = new InvokeMethodResult();

        // Unwrap arguments
        Object[] arguments = new Object[wrappedArguments.length];
        for (int i = 0; i < wrappedArguments.length; i++) {
            arguments[i] = wrappedArguments[i].unwrap(mClassLoader);
        }

        // Invoke method
        try {
            Object result = mMethods[methodNumber].invoke(object, arguments);
            requestResult.sandboxedReturnValue = new SandboxedObject(result);
            requestResult.returnValueAsString = String.valueOf(result);
        } catch (InvocationTargetException e) {
            requestResult.exception = Utils.describeException(e.getTargetException());
        } catch (Exception e) {
            requestResult.exception = "[Internal] " + Utils.describeException(e);
        }
        return requestResult;
    }

    @Override
    public InvokeMethodResult invokeMethod(int methodNumber, SandboxedObject[] arguments) throws RemoteException {
        return doInvokeMethod(mObject, methodNumber, arguments);
    }

    @Override
    public InvokeMethodResult invokeMethodUsingBinder(IBinder binder, int methodNumber, SandboxedObject[] arguments) throws RemoteException {
        final Object aidlInterface;
        try {
            aidlInterface = mAsInterfaceMethod.invoke(null, binder);
        } catch (Exception e) {
            final InvokeMethodResult result = new InvokeMethodResult();
            result.exception = "[Run-As mode] " + Utils.describeException(e);
            return result;
        }
        return doInvokeMethod(aidlInterface, methodNumber, arguments);
    }


    static class UnknownInterfaceException extends Exception {}
}
