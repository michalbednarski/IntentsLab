package com.github.michalbednarski.intentslab.xposedhooks.internal.trackers;

import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

import com.github.michalbednarski.intentslab.xposedhooks.internal.XHUtils;

import de.robv.android.xposed.XposedHelpers;

/**
 * Base class with utilities for object tracking
 */
public abstract class ObjectTrackerModule<O, I extends IInterface> {
    private static final String FIELD_TRACKER = "XIntentsLab.ObjectTrackerTag";

    static final int MAGIC_PARCEL_INT = 0x80000000 | 'X' << 24 | 'I' << 16 | 'n' << 8 | 'L';

    /**
     * Make binder an tracker interface for this object.
     * Used by {@link #readTag(Object, android.os.Parcel)}
     */
    abstract I asInterface(IBinder binder);

    /**
     * Check if parcel starts with MAGIC_PARCEL_INT and if so
     * read IBinder from it, validate it and save to interface target object
     * so it can be later read with {@link #getTracker(Object)}
     */
    I readTag(O targetObject, Parcel parcel) {
        int origPosition = parcel.dataPosition();

        // Check signature
        if (parcel.readInt() != MAGIC_PARCEL_INT) {
            parcel.setDataPosition(origPosition);
            return null;
        }

        // Read binder
        IBinder binder = parcel.readStrongBinder();
        if (binder == null) {
            // This wasn't really binder, just accidentally we matched MAGIC_PARCEL_INT
            parcel.setDataPosition(origPosition);
            return null;
        }

        // Verify if binder is trusted and add tag
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

    /**
     * Write tag so it can be later read by readTag,
     * automatically getting tag for object
     */
    void writeTag(O object, Parcel dest) {
        I tracker = getTracker(object);
        writeTag(tracker, dest);
    }

    /**
     * Write tag so it can be later read by readTag
     */
    void writeTag(I tracker, Parcel dest) {
        if (tracker != null) {
            dest.writeInt(MAGIC_PARCEL_INT);
            dest.writeStrongBinder(tracker.asBinder());
        }
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
