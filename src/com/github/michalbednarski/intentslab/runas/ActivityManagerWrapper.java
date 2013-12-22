package com.github.michalbednarski.intentslab.runas;

import android.app.IActivityController;
import android.app.IServiceConnection;
import android.content.Context;
import android.content.IIntentReceiver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Class used for cleaner access to ActivityManagerNative
 */
@SuppressWarnings("unchecked")
class ActivityManagerWrapper {
    private static ActivityManagerWrapper sInstance = null;

    private final Class mAmClass;
    private final Object mAm;
    private final Class mIApplicationThreadClass;
    private final Method mIApplicationThreadAsInterface;

    private final CrossVersionReflectedMethod mStartActivityMethod;
    private final CrossVersionReflectedMethod mBroadcastIntentMethod;
    private final CrossVersionReflectedMethod mBindServiceMethod;
    private final CrossVersionReflectedMethod mSetActivityControllerMethod;



    private ActivityManagerWrapper() {
        try {
            mAmClass = Class.forName("android.app.ActivityManagerNative");
            mAm = mAmClass.getMethod("getDefault").invoke(null);
            mIApplicationThreadClass = Class.forName("android.app.IApplicationThread");
            mIApplicationThreadAsInterface = Class.forName("android.app.ApplicationThreadNative").getMethod("asInterface", IBinder.class);
            mStartActivityMethod =
                    new CrossVersionReflectedMethod(mAmClass)
                    .tryMethodVariant( // 4.3
                        "startActivity",
                        mIApplicationThreadClass,   "caller",           null,
                        String.class,               "callingPackage",   null,
                        Intent.class,               "intent",           null,
                        String.class,               "resolvedType",     null,
                        IBinder.class,              "resultTo",         null,
                        String.class,               "resultWho",        null,
                        int.class,                  "requestCode",      0,
                        int.class,                  "startFlags",       0,
                        String.class,               "profileFile",      null,
                        ParcelFileDescriptor.class, "profileFd",        null,
                        Bundle.class,               "options",          null
                    )
                    .tryMethodVariant( // < 4.3_r0.9 (pre commit f265ea)
                        "startActivity",
                        mIApplicationThreadClass,   "caller",           null,
                        Intent.class,               "intent",           null,
                        String.class,               "resolvedType",     null,
                        IBinder.class,              "resultTo",         null,
                        String.class,               "resultWho",        null,
                        int.class,                  "requestCode",      0,
                        int.class,                  "startFlags",       0,
                        String.class,               "profileFile",      null,
                        ParcelFileDescriptor.class, "profileFd",        null,
                        Bundle.class,               "options",          null
                    )
                    .tryMethodVariant( // < 4.1.1_r1 (pre commit a4972e)
                        "startActivity",
                        mIApplicationThreadClass,   "caller",           null,
                        Intent.class,               "intent",           null,
                        String.class,               "resolvedType",     null,
                        Uri[].class,                "grantedUriPermissions", null,
                        int.class,                  "grantedMode",      0,
                        IBinder.class,              "resultTo",         null,
                        String.class,               "resultWho",        null,
                        int.class,                  "requestCode",      0,
                        boolean.class,              "onlyIfNeeded",     false,
                        boolean.class,              "debug",            false,
                        boolean.class,              "openglTrace",      false,
                        String.class,               "profileFile",      null,
                        ParcelFileDescriptor.class, "profileFd",        null,
                        boolean.class,              "autoStopProfiler", false
                    )
                    .tryMethodVariant( // < 4.1.1_r1 (pre commit 92a8b22e7410e74e1cba1b856333116652af8a5c)
                        "startActivity",
                        mIApplicationThreadClass,   "caller",           null,
                        Intent.class,               "intent",           null,
                        String.class,               "resolvedType",     null,
                        Uri[].class,                "grantedUriPermissions", null,
                        int.class,                  "grantedMode",      0,
                        IBinder.class,              "resultTo",         null,
                        String.class,               "resultWho",        null,
                        int.class,                  "requestCode",      0,
                        boolean.class,              "onlyIfNeeded",     false,
                        boolean.class,              "debug",            false,
                        String.class,               "profileFile",      null,
                        ParcelFileDescriptor.class, "profileFd",        null,
                        boolean.class,              "autoStopProfiler", false
                    )
                    .tryMethodVariant( // < 4.0.1_r1 (pre commit 62f20ecf492d2b29881bba307c79ff55e68760e6)
                            "startActivity",
                            mIApplicationThreadClass, "caller", null,
                            Intent.class, "intent", null,
                            String.class, "resolvedType", null,
                            Uri[].class, "grantedUriPermissions", null,
                            int.class, "grantedMode", 0,
                            IBinder.class, "resultTo", null,
                            String.class, "resultWho", null,
                            int.class, "requestCode", 0,
                            boolean.class, "onlyIfNeeded", false,
                            boolean.class, "debug", false
                    );
            mBroadcastIntentMethod =
                    new CrossVersionReflectedMethod(mAmClass)
                    .tryMethodVariant( // 4.3
                            "broadcastIntent",
                            mIApplicationThreadClass, "caller", null,
                            Intent.class, "intent", null,
                            String.class, "resolvedType", null,
                            IIntentReceiver.class, "resultTo", null,
                            int.class, "resultCode", 0,
                            String.class, "resultData", null,
                            Bundle.class, "map", null,
                            String.class, "requiredPermission", null,
                            int.class, "appOp", 0,
                            boolean.class, "serialized", false,
                            boolean.class, "sticky", false,
                            int.class, "userId", 0
                    )
                    .tryMethodVariant( // < 4.3_r0.9
                        "broadcastIntent",
                        mIApplicationThreadClass,   "caller",           null,
                        Intent.class,               "intent",           null,
                        String.class,               "resolvedType",     null,
                        IIntentReceiver.class,      "resultTo",         null,
                        int.class,                  "resultCode",       0,
                        String.class,               "resultData",       null,
                        Bundle.class,               "map",              null,
                        String.class,               "requiredPermission", null,
                        boolean.class,              "serialized",       false,
                        boolean.class,              "sticky",           false,
                        int.class,                  "userId",           0
                    )
                    .tryMethodVariant( // < 4.1.1_r1
                        "broadcastIntent",
                        mIApplicationThreadClass,   "caller",           null,
                        Intent.class,               "intent",           null,
                        String.class,               "resolvedType",     null,
                        IIntentReceiver.class,      "resultTo",         null,
                        int.class,                  "resultCode",       0,
                        String.class,               "resultData",       null,
                        Bundle.class,               "map",              null,
                        String.class,               "requiredPermission", null,
                        boolean.class,              "serialized",       false,
                        boolean.class,              "sticky",           false
                    );
            mBindServiceMethod =
                    new CrossVersionReflectedMethod(mAmClass)
                    .tryMethodVariant( // 4.4
                        "bindService",
                        mIApplicationThreadClass,   "caller",           null,
                        IBinder.class,              "token",            null,
                        Intent.class,               "service",          null,
                        String.class,               "resolvedType",     null,
                        IServiceConnection.class,   "connection",       null,
                        int.class,                  "flags",            0,
                        int.class,                  "userId",           0
                    )
                    .tryMethodVariant( // pre 37ce3a8af6faab675319d0803b288ab1dddc76be ( < 4.1.1_r1 )
                        "bindService",
                        mIApplicationThreadClass,   "caller",           null,
                        IBinder.class,              "token",            null,
                        Intent.class,               "service",          null,
                        String.class,               "resolvedType",     null,
                        IServiceConnection.class,   "connection",       null,
                        int.class,                  "flags",            0
                    );
            mSetActivityControllerMethod =
                    new CrossVersionReflectedMethod(mAmClass)
                    .tryMethodVariant(
                            "setActivityController",
                            IActivityController.class, "watcher", null
                    );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static ActivityManagerWrapper get() {
        if (sInstance == null) {
            sInstance = new ActivityManagerWrapper();
        }
        return sInstance;
    }

    void startActivity(Object... argumentNamesAndValues) throws InvocationTargetException {
        mStartActivityMethod.invoke(mAm, argumentNamesAndValues);
    }


    void sendOrderedBroadcast(Intent intent, RealIIntentReceiver resultTo) {
        try {
            mBroadcastIntentMethod.invoke(
                    mAm,
                    "intent", intent,
                    "serialized", true,
                    "resultTo", resultTo
            );
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    int bindService(IBinder sandboxApplicationToken, Intent service, IServiceConnection connection) {
        try {
            return (Integer) mBindServiceMethod.invoke(
                    mAm,
                    "caller", mIApplicationThreadAsInterface.invoke(null, sandboxApplicationToken),
                    "service", service,
                    "connection", connection,
                    "flags", Context.BIND_AUTO_CREATE
            );
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    boolean setActivityController(IActivityController controller) {
        try {
            mSetActivityControllerMethod.invoke(mAm, "watcher", controller);
        } catch (InvocationTargetException e) {
            return false;
        }
        return true;
    }
}
