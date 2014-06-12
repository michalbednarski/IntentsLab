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

import android.os.Bundle;
import android.os.RemoteException;

import com.github.michalbednarski.intentslab.editor.BundleAdapter;
import com.github.michalbednarski.intentslab.sandbox.ISandboxedBundle;
import com.github.michalbednarski.intentslab.sandbox.SandboxedObject;

import java.util.Set;

/**
 * Created by mb on 10.11.13.
 */
public class SandboxedBundleImpl extends ISandboxedBundle.Stub {

    private final ClassLoader mClassLoader;
    private Bundle mBundle;

    public SandboxedBundleImpl(Bundle bundle, ClassLoader classLoader) {
        bundle.setClassLoader(classLoader);
        mClassLoader = classLoader;
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
    public String getAsString(String key) throws RemoteException {
        return mBundle.get(key).toString();
    }

    @Override
    public SandboxedObject getWrapped(String key) throws RemoteException {
        return new SandboxedObject(mBundle.get(key));
    }

    @Override
    public void putWrapped(String key, SandboxedObject wrappedValue) throws RemoteException {
        BundleAdapter.putInBundle(mBundle, key, wrappedValue.unwrap(mClassLoader));
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
