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

import com.github.michalbednarski.intentslab.Utils;
import com.github.michalbednarski.intentslab.sandbox.InvokeMethodResult;
import com.github.michalbednarski.intentslab.sandbox.SandboxedMethod;
import com.github.michalbednarski.intentslab.sandbox.SandboxedObject;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;

/**
 * Created by mb on 21.07.14.
 */
class LocalAidlInterface extends AidlInterface {

    private final Object mAnInterface;
    private final Class mStubClass;
    private final String mInterfaceDescriptor;

    private final SandboxedMethod[] mSandboxedMethods;
    private final Method[] mMethods;

    LocalAidlInterface(Object anInterface, Class stubClass, String interfaceDescriptor) {

        mAnInterface = anInterface;
        mStubClass = stubClass;
        mInterfaceDescriptor = interfaceDescriptor;

        // Get it's methods
        final Method[] methods = mStubClass.getMethods();
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
    }

    @Override
    public String getInterfaceName() {
        return mInterfaceDescriptor;
    }

    private InvokeMethodResult doInvokeMethod(Object object, int methodNumber, SandboxedObject[] wrappedArguments) {
        InvokeMethodResult requestResult = new InvokeMethodResult();

        // Unwrap arguments
        Object[] arguments = new Object[wrappedArguments.length];
        for (int i = 0; i < wrappedArguments.length; i++) {
            arguments[i] = wrappedArguments[i].unwrap(object.getClass().getClassLoader());
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
    public SandboxedMethod[] getMethods() {
        return mSandboxedMethods;
    }

    @Override
    public InvokeMethodResult invokeMethod(int methodNumber, SandboxedObject[] arguments) {
        return doInvokeMethod(mAnInterface, methodNumber, arguments);
    }

    @Override
    public InvokeMethodResult invokeMethodUsingBinder(IBinder binder, int methodNumber, SandboxedObject[] arguments) {
        Object anInterface;
        try {
            anInterface = wrapInterface(mStubClass, binder);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return doInvokeMethod(anInterface, methodNumber, arguments);
    }
}
