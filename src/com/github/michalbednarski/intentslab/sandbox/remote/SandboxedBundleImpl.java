package com.github.michalbednarski.intentslab.sandbox.remote;

import android.app.Service;
import android.os.Bundle;
import android.os.RemoteException;
import com.github.michalbednarski.intentslab.editor.BundleAdapter;
import com.github.michalbednarski.intentslab.sandbox.ClassLoaderDescriptor;
import com.github.michalbednarski.intentslab.sandbox.ISandboxedBundle;
import com.github.michalbednarski.intentslab.sandbox.ParcelableValue;
import com.github.michalbednarski.intentslab.sandbox.SandboxManager;

import java.util.Set;

/**
 * Created by mb on 10.11.13.
 */
public class SandboxedBundleImpl extends ISandboxedBundle.Stub {

    private final ClassLoader mClassLoader;
    private Bundle mBundle;

    public SandboxedBundleImpl(Bundle bundle, ClassLoaderDescriptor classLoaderDescriptor, Service sandboxService) {
        mClassLoader = classLoaderDescriptor.getClassLoader(sandboxService);
        bundle.setClassLoader(mClassLoader);
        mBundle = bundle;
    }

    @Override
    public String[] getKeySet() throws RemoteException {
        final Set<String> keySet = mBundle.keySet();
        return keySet.toArray(new String[keySet.size()]);
    }

    @Override
    public boolean containsKey(String key) throws RemoteException {
        return mBundle.containsKey(key);
    }

    @Override
    public ParcelableValue get(String key) throws RemoteException {
        return new ParcelableValue(mBundle.get(key));
    }

    @Override
    public String getAsString(String key) throws RemoteException {
        return mBundle.get(key).toString();
    }

    @Override
    public Bundle getWrapped(String key) throws RemoteException {
        return SandboxManager.wrapObject(mBundle.get(key));
    }

    @Override
    public void put(String key, ParcelableValue value) throws RemoteException {
        BundleAdapter.putInBundle(mBundle, key, value.value);
    }

    @Override
    public void putWrapped(String key, Bundle wrappedValue) throws RemoteException {
        wrappedValue.setClassLoader(mClassLoader);
        BundleAdapter.putInBundle(mBundle, key, SandboxManager.unwrapObject(wrappedValue));
    }

    @Override
    public void remove(String key) throws RemoteException {
        mBundle.remove(key);
    }

    @Override
    public Bundle getBundle() throws RemoteException {
        return mBundle;
    }
}
