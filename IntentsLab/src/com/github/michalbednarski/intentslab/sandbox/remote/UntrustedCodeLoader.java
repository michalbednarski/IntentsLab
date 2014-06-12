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
import android.content.pm.PackageManager;

import com.github.michalbednarski.intentslab.sandbox.ClassLoaderDescriptor;

import java.util.HashMap;

/**
 * Allows getting ClassLoader from {@link com.github.michalbednarski.intentslab.sandbox.ClassLoaderDescriptor}
 */
public class UntrustedCodeLoader {
    private final Service mContext;
    private final HashMap<ClassLoaderDescriptor, ClassLoader> mClassLoadersCache = new HashMap<ClassLoaderDescriptor, ClassLoader>();

    /**
     * Constructor, should be only called by {@link SandboxImpl}
     */
    UntrustedCodeLoader(Service context) {
        mContext = context;
    }

    public ClassLoader getClassLoader(ClassLoaderDescriptor descriptor) {
        // Look up in cache
        ClassLoader classLoader = mClassLoadersCache.get(descriptor);
        if (classLoader != null) {
            return classLoader;
        }

        // Create ClassLoader using package name
        if (descriptor.packageName != null) {
            try {
                classLoader = mContext.createPackageContext(descriptor.packageName, Context.CONTEXT_INCLUDE_CODE | Context.CONTEXT_IGNORE_SECURITY).getClassLoader();
            } catch (PackageManager.NameNotFoundException ignored) {}
        }

        // Add class loader to cache and return it
        if (classLoader != null) {
            mClassLoadersCache.put(descriptor, classLoader);
            return classLoader;
        }

        // Fallback to top class loader
        return mContext.getClassLoader();
    }
}
