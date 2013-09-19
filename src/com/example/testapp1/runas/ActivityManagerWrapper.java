package com.example.testapp1.runas;

import android.content.IIntentReceiver;
import android.content.Intent;
import android.os.Bundle;

import java.lang.reflect.InvocationTargetException;

/**
 * Class used for cleaner access to ActivityManagerNative
 */
@SuppressWarnings("unchecked")
class ActivityManagerWrapper {
    private static ActivityManagerWrapper sInstance = null;

    private final Class mAmClass;
    private final Object mAm;
    private final Class mIApplicationThreadClass;
    private Object mIApplicationThread = null;

    private final CrossVersionReflectedMethod mBroadcastIntentMethod;



    private ActivityManagerWrapper() {
        try {
            mAmClass = Class.forName("android.app.ActivityManagerNative");
            mAm = mAmClass.getMethod("getDefault").invoke(null);
            mIApplicationThreadClass = Class.forName("android.app.IApplicationThread");
            mBroadcastIntentMethod =
                    new CrossVersionReflectedMethod(mAmClass)
                    .tryMethodVariant( // 4.3
                        "broadcastIntent",
                        mIApplicationThreadClass,   "caller",           null,
                        Intent.class,               "intent",           null,
                        String.class,               "resolvedType",     null,
                        IIntentReceiver.class,      "resultTo",         null,
                        int.class,                  "resultCode",       0,
                        String.class,               "resultData",       null,
                        Bundle.class,               "map",              null,
                        String.class,               "requiredPermission", null,
                        int.class,                  "appOp",            0,
                        boolean.class,              "serialized",       false,
                        boolean.class,              "sticky",           false,
                        int.class,                  "userId",           0
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
}
