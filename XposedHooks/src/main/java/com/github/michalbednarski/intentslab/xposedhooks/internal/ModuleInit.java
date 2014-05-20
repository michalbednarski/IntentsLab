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

package com.github.michalbednarski.intentslab.xposedhooks.internal;

import android.content.Context;
import android.os.IBinder;

import com.github.michalbednarski.intentslab.xposedhooks.api.XIntentsLabStatic;
import com.github.michalbednarski.intentslab.xposedhooks.internal.apiimpl.XIntentsLabImpl;
import com.github.michalbednarski.intentslab.xposedhooks.internal.trackers.BundleTrackerModule;
import com.github.michalbednarski.intentslab.xposedhooks.internal.trackers.IntentTrackerModule;

import java.lang.reflect.Method;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Created by mb on 01.03.14.
 */
public class ModuleInit implements IXposedHookZygoteInit, IXposedHookLoadPackage {

    static final String SYSTEM_SERVICE_NAME = "xIntentsLab";

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        // Hook system server init to register our service
        XposedHelpers.findAndHookMethod(
                "com.android.server.am.ActivityManagerService",
                null,
                "main",
                int.class,
                new XC_MethodHook() {
                    private boolean mSystemInitialized = false;

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (mSystemInitialized) {
                            XposedBridge.log("System already initialized!");
                            return;
                        }
                        mSystemInitialized = true;

                        // Get context
                        Context context = (Context) param.getResult();

                        // Create and register service
                        SystemService systemService = new SystemService(context);
                        XposedHelpers.callStaticMethod(
                                XposedHelpers.findClass("android.os.ServiceManager", null),
                                "addService",
                                new Class<?>[] {String.class, IBinder.class},
                                SYSTEM_SERVICE_NAME,
                                systemService
                        );
                    }
                }
        );

        // Prepare object tracker
        (new IntentTrackerModule()).installHooks();
        (new BundleTrackerModule()).installHooks();
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        Class<?> xIntentsLabStaticClass;
        try {
            xIntentsLabStaticClass = XposedHelpers.findClass(
                    XIntentsLabStatic.class.getName(),
                    lpparam.classLoader
            );
        } catch (Throwable e) {
            // Package doesn't have XposedHooksApi library
            // Skip silently
            return;
        }
        XposedHelpers.findAndHookMethod(
                xIntentsLabStaticClass,
                "getInstance",
                new XC_MethodReplacement() {
                    private Object mInstance = null;

                    @Override
                    protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                        if (mInstance == null) {
                            Class<?> xIntentsLabInterface = ((Method) param.method).getReturnType();
                            mInstance = XHUtils.castInterface(new XIntentsLabImpl(), xIntentsLabInterface);
                        }
                        return mInstance;
                    }
                }
        );
    }
}
