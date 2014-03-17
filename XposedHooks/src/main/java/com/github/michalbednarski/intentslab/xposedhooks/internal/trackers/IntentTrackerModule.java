package com.github.michalbednarski.intentslab.xposedhooks.internal.trackers;

import android.content.Intent;
import android.os.IBinder;
import android.os.Parcel;

import com.github.michalbednarski.intentslab.xposedhooks.internal.IIntentTracker;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * Hooks for tracking intent usage
 */
public class IntentTrackerModule extends ObjectTrackerModule<Intent, IIntentTracker> {

    @Override
    IIntentTracker asInterface(IBinder binder) {
        return IIntentTracker.Stub.asInterface(binder);
    }

    public void installHooks() throws Throwable {
        // Read and write
        XposedHelpers.findAndHookMethod(Intent.class, "readFromParcel", Parcel.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                readTag(
                        (Intent) param.thisObject,
                        (Parcel) param.args[0]
                );
            }
        });

        XposedHelpers.findAndHookMethod(Intent.class, "writeToParcel", Parcel.class, int.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                writeTag(
                        (Intent) param.thisObject,
                        (Parcel) param.args[0]
                );
            }
        });

        // Copy constructor (also used by Intent#clone())
        XposedBridge.hookMethod(Intent.class.getDeclaredConstructor(Intent.class), new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Intent oldIntent = (Intent) param.args[0];
                Intent newIntent = (Intent) param.thisObject;
                IIntentTracker tracker = getTracker(oldIntent);
                if (tracker != null) {
                    setTracker(newIntent, tracker);
                }
            }
        });

        // Intent#cloneFilter()
        XposedBridge.hookMethod(Intent.class.getDeclaredConstructor(Intent.class, boolean.class), new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Intent oldIntent = (Intent) param.args[0];
                Intent newIntent = (Intent) param.thisObject;
                IIntentTracker tracker = getTracker(oldIntent);
                if (tracker != null) {
                    setTracker(newIntent, tracker);
                }
            }
        });

        // Action reporting
        XposedHelpers.findAndHookMethod(Intent.class, "getAction", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                IIntentTracker tracker = getTracker((Intent) param.thisObject);
                if (tracker != null) {
                    try {
                        tracker.reportActionRead();
                    } catch (Exception ignored) {}
                }
            }
        });
    }

}
