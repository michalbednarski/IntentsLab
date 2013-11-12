package com.github.michalbednarski.intentslab.sandbox.remote;

import android.app.Service;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import com.github.michalbednarski.intentslab.Utils;
import com.github.michalbednarski.intentslab.sandbox.ClassLoaderDescriptor;
import com.github.michalbednarski.intentslab.sandbox.IAidlInterface;
import com.github.michalbednarski.intentslab.sandbox.ISandboxedObject;
import com.github.michalbednarski.intentslab.sandbox.SandboxedMethod;
import com.github.michalbednarski.intentslab.sandbox.SandboxedMethodArguments;
import com.github.michalbednarski.intentslab.sandbox.SandboxedType;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;

/**
 * Created by mb on 01.10.13.
 */
class SandboxedAidlInterfaceImpl extends IAidlInterface.Stub {
    private final Object mObject;
    private final String mInterfaceDescriptor;
    private final SandboxedMethod[] mSandboxedMethods;
    private final Method[] mMethods;

    SandboxedAidlInterfaceImpl(IBinder binder, ClassLoaderDescriptor fromPackage, Service service) throws RemoteException, UnknownInterfaceException {
        // Get interface name
        mInterfaceDescriptor = binder.getInterfaceDescriptor();
        if (Utils.stringEmptyOrNull(mInterfaceDescriptor)) {
            throw new UnknownInterfaceException();
        }
        try {
            // Load interface class
            final ClassLoader classLoader = fromPackage.getClassLoader(service);
            final Class<?> stub = classLoader.loadClass(mInterfaceDescriptor + "$Stub");
            mObject = stub.getMethod("asInterface", IBinder.class).invoke(null, binder);

            // Get it's methods
            final Method[] methods = stub.getMethods();
            ArrayList<SandboxedMethod> sandboxedMethods = new ArrayList<SandboxedMethod>();
            ArrayList<Method> filteredMethods = new ArrayList<Method>();
            for (Method method : methods) {
                if (Modifier.isAbstract(method.getModifiers())) {
                    SandboxedMethod sandboxedMethod = new SandboxedMethod();
                    sandboxedMethod.methodNumber = sandboxedMethods.size();
                    sandboxedMethod.name = method.getName();
                    sandboxedMethod.argumentTypes = SandboxedType.wrapClassesArray(method.getParameterTypes());

                    sandboxedMethods.add(sandboxedMethod);
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

    @Override
    public ISandboxedObject invokeMethod(int methodNumber, SandboxedMethodArguments remoteObjects, Bundle outExtras) throws RemoteException {
        Object[] arguments = new Object[remoteObjects.arguments.length];

        for (int i = 0; i < remoteObjects.arguments.length; i++) {
            //
            Object arg = remoteObjects.arguments[i];
            if (arg instanceof ISandboxedObject) {
                // TODO: unwrap object
            } else if (arg instanceof IAidlInterface) {
                arg = ((SandboxedAidlInterfaceImpl) arg).mObject;
            }
            arguments[i] = arg;
        }
        try {
            return new SandboxedObjectImpl(mMethods[methodNumber].invoke(mObject, arguments));
        } catch (InvocationTargetException e) {
            outExtras.putSerializable("targetException", e.getTargetException());
        } catch (Exception e) {
            outExtras.putSerializable("exception", e);
        }
        return null;
    }

    static class UnknownInterfaceException extends Exception {}
}
