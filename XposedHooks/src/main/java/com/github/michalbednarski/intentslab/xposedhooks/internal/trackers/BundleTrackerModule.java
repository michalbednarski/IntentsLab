package com.github.michalbednarski.intentslab.xposedhooks.internal.trackers;

import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;

import com.github.michalbednarski.intentslab.xposedhooks.internal.IBundleTracker;
import com.github.michalbednarski.intentslab.xposedhooks.internal.ParcelOffsets;
import com.github.michalbednarski.intentslab.xposedhooks.internal.XHUtils;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * Hooks for tracking Bundle use (including Intent extras)
 */
public class BundleTrackerModule extends ObjectTrackerModule<Bundle, IBundleTracker> {

    @Override
    IBundleTracker asInterface(IBinder binder) {
        return IBundleTracker.Stub.asInterface(binder);
    }

    public void installHooks() throws Exception {
        // Read/write
        XposedHelpers.findAndHookMethod(Bundle.class, "readFromParcelInner", Parcel.class, int.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                // Try to read signature and tracker binder
                readTag((Bundle) param.thisObject, (Parcel) param.args[0]);
            }
        });
        /*
         * Writing bundle to parcel:
         *
         *   Normally saved bundle:
         *   | length | parcelled data |
         *
         *   Inside beforeHookedMethod() for writeToParcel():
         *   [MAGIC_PARCEL_INT | binder] | [length | parcelled data - will be written by original method]
         *   [   ^-space allocated     ] ^-at end of beforeHookedMethod()
         *   ^                           |
         *   \-- writePos                \--readPos
         *
         *   Bundle with injected tag:
         *   length | MAGIC_PARCEL_INT | binder | parcelled data |
         *     ^         ^^^ data inserted ^^^
         *     |
         *     \-- Read from end and written at beginning in afterHookedMethod()
         */
        class WriteToParcelHookState implements Serializable {
            int readPos;
            int writePos;
            IBinder binder;
        }
        XposedHelpers.findAndHookMethod(Bundle.class, "writeToParcel", Parcel.class, int.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                IBundleTracker tracker = getTracker((Bundle) param.thisObject);
                if (tracker != null) {
                    ParcelOffsets offsets = ParcelOffsets.getInstance();
                    Parcel dest = (Parcel) param.args[0];
                    WriteToParcelHookState state = new WriteToParcelHookState();
                    state.binder = tracker.asBinder();

                    // Skip length of int+binder saving read/writePos
                    int origPosition = dest.dataPosition();
                    state.writePos = origPosition;
                    int newPosition =
                            origPosition
                            + offsets.INT_IN_PARCEL_LENGTH // MAGIC_PARCEL_INT
                            + offsets.BINDER_IN_PARCEL_LENGTH;  // Tracker binder
                    state.readPos = newPosition;
                    dest.setDataPosition(newPosition);

                    // Pass data to afterHookedMethod
                    param.setObjectExtra("XIntentsLab.tracker.bundle.state", state);
                }
            }

            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                WriteToParcelHookState state = (WriteToParcelHookState) param.getObjectExtra("XIntentsLab.tracker.bundle.state");
                if (state != null) {
                    // Get parcel and save current position
                    Parcel dest = (Parcel) param.args[0];
                    int origPosition = dest.dataPosition();

                    // Read originally written length
                    dest.setDataPosition(state.readPos);
                    int length = dest.readInt();

                    // Write our data
                    dest.setDataPosition(state.writePos);
                    dest.writeInt(length);
                    dest.writeInt(MAGIC_PARCEL_INT);
                    dest.writeStrongBinder(state.binder);

                    // Restore position
                    dest.setDataPosition(origPosition);
                }
            }
        });

        // Copy constructor (also used by clone())
        XposedBridge.hookMethod(Bundle.class.getDeclaredConstructor(Bundle.class), new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Bundle oldBundle = (Bundle) param.args[0];
                IBundleTracker tracker = getTracker(oldBundle);
                if (tracker != null) {
                    Bundle newBundle = (Bundle) param.thisObject;
                    setTracker(newBundle, tracker);
                }
            }
        });

        // Read reporting (get*())
        for (Method method : Bundle.class.getDeclaredMethods()) {
            Class<?>[] parameterTypes = method.getParameterTypes();
            int parameterCount = parameterTypes.length;
            Class<?> returnType = method.getReturnType();
            if (
                    (parameterCount == 1 || parameterCount == 2) && // 1-2 parameters
                    parameterTypes[0] == String.class && // first is String
                    (parameterCount == 1 || parameterTypes[1] == returnType) && // second if exist is same as return type
                    returnType != Void.TYPE && // method has return type (not void)
                    (method.getModifiers() & (Modifier.PUBLIC | Modifier.STATIC)) == Modifier.PUBLIC && // public and not static
                    method.getName().startsWith("get") // name starts with "get"
                    ) {
                final String methodName = method.getName();
                XposedBridge.hookMethod(method, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        IBundleTracker tracker = getTracker((Bundle) param.thisObject);
                        if (tracker != null) {
                            try {
                                tracker.reportRead(
                                        (String) param.args[0],
                                        methodName,
                                        XHUtils.getHookedMethodWrappedStackTrace()
                                );
                            } catch (RemoteException e) {
                                e.printStackTrace(); // Probably tracker is dead
                            }
                        }
                    }
                });
            }
        }
    }
}
