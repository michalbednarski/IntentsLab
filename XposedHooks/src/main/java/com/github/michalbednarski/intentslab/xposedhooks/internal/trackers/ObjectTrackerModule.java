package com.github.michalbednarski.intentslab.xposedhooks.internal.trackers;

import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

import com.github.michalbednarski.intentslab.xposedhooks.internal.XHUtils;

import de.robv.android.xposed.XposedHelpers;

/**
 * Created by mb on 06.03.14.
 */
public abstract class ObjectTrackerModule<O, I extends IInterface> {
    private static boolean mHookParcelReadyCalled = false;
    private static final String FIELD_TRACKER = "XIntentsLab.ObjectTrackerTag";

    static final int MAGIC_PARCEL_INT = 0x80000000 | 'X' << 24 | 'I' << 16 | 'n' << 8 | 'L';

    /**
     * Make binder an tracker interface for this object.
     * Used by {@link #readTag(Object, android.os.Parcel)}
     */
    abstract I asInterface(IBinder binder);

    /**
     * Read IBinder from parcel, validate it and save to interface target object
     * so it can be later read with {@link #getTracker(Object)}
     */
    I readTag(O targetObject, Parcel parcel) {
        IBinder binder = parcel.readStrongBinder();
        try {
            if (binder.isBinderAlive() && XHUtils.getSystemInterface().isTrackingTagAllowed(binder)) {
                I anInterface = asInterface(binder);
                setTracker(targetObject, anInterface);
                return anInterface;
            }
        } catch (RemoteException ignored) {
            // Fall through
        }
        return null;
    }

    I getTracker(O target) {
        return (I) XposedHelpers.getAdditionalInstanceField(target, FIELD_TRACKER);
    }

    void setTracker(O target, I tag) {
        XposedHelpers.setAdditionalInstanceField(target, FIELD_TRACKER, tag);
    }

    public static void setTrackerStatic(Object target, Object tag) {
        XposedHelpers.setAdditionalInstanceField(target, FIELD_TRACKER, tag);
    }


}
