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

import android.content.Context;
import android.os.IBinder;

import com.github.michalbednarski.intentslab.sandbox.ClassLoaderDescriptor;
import com.github.michalbednarski.intentslab.sandbox.IAidlInterface;
import com.github.michalbednarski.intentslab.sandbox.InvokeMethodResult;
import com.github.michalbednarski.intentslab.sandbox.SandboxManager;
import com.github.michalbednarski.intentslab.sandbox.SandboxedMethod;
import com.github.michalbednarski.intentslab.sandbox.SandboxedObject;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by mb on 21.07.14.
 */
public abstract class AidlInterface {
    private static final Pattern AOSP_MANUAL_AIDL_PATTERN = Pattern.compile("(android\\.(\\w+\\.)+)I(\\w+)");

    static void getAidlInterface(final IBinder binder, final ClassLoaderDescriptor classLoaderDescriptor, Context context, final BindServiceManager.Helper.AidlInterfaceMediator callback) {
        try {
            final String interfaceDescriptor = binder.getInterfaceDescriptor();

            // Try getting interface locally
            Class<?> stubClass = null;
            try {
                // Normal aidl-generated interface
                stubClass = Class.forName(interfaceDescriptor + "$Stub");
            } catch (ClassNotFoundException e) {
                // Some interfaces are built manually and have Native suffix
                // android.app.IActivityManager => android.app.ActivityManagerNative
                Matcher matcher = AOSP_MANUAL_AIDL_PATTERN.matcher(interfaceDescriptor);
                if (matcher.find()) {
                    try {
                        stubClass = Class.forName(matcher.group(1) + matcher.group(3) + "Native");
                    } catch (ClassNotFoundException ignored) {}
                }
            }

            // Get an interface
            if (stubClass != null) {
                Object anInterface = wrapInterface(stubClass, binder);
                callback.handleAidlReady(new LocalAidlInterface(
                        anInterface,
                        stubClass,
                        interfaceDescriptor
                ));
                return;
            }

            // Use sandboxed version
            callback.refSandbox();
            SandboxManager.initSandboxAndRunWhenReady(
                    context,
                    new Runnable() {
                        @Override
                        public void run() {
                            IAidlInterface sandboxedInterface = null;
                            try {
                                sandboxedInterface = SandboxManager.getSandbox().queryInterface(binder, classLoaderDescriptor, interfaceDescriptor);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            callback.handleAidlReady(
                                    sandboxedInterface == null ?
                                            null :
                                            new SandboxedAidlInterface(sandboxedInterface, interfaceDescriptor)
                            );
                        }
                    }
            );

        } catch (Exception e) {
            e.printStackTrace();
            callback.handleAidlReady(null);
        }
    }

    static Object wrapInterface(Class<?> stubClass, IBinder binder) throws Exception {
        Object anInterface = stubClass
                .getMethod("asInterface", IBinder.class)
                .invoke(null, binder);
        if (anInterface == null) {
            throw new NullPointerException();
        }
        return anInterface;
    }

    public abstract String getInterfaceName();

    public abstract SandboxedMethod[] getMethods();

    public abstract InvokeMethodResult invokeMethod(int methodNumber, SandboxedObject[] arguments);

    public abstract InvokeMethodResult invokeMethodUsingBinder(IBinder binder, int methodNumber, SandboxedObject[] arguments);

    boolean isReady() { return true; }
}
