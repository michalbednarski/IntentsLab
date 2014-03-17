package com.github.michalbednarski.intentslab.xposedhooks.internal;

import android.os.Binder;
import android.os.Parcel;

/**
 * Lengths of things in parcel
 *
 * Getting parcel before system is ready for some reason later causes segfault
 * so these must be only used when you is sure that system is ready
 */
public class ParcelOffsets {

    public final int INT_IN_PARCEL_LENGTH;
    public final int BINDER_IN_PARCEL_LENGTH;

    private static ParcelOffsets sInstance = null;
    public static synchronized ParcelOffsets getInstance() {
        if (sInstance == null) {
            sInstance = new ParcelOffsets();
        }
        return sInstance;
    }

    private ParcelOffsets() {
        Parcel parcel = Parcel.obtain();

        parcel.setDataPosition(0);
        parcel.writeInt(0);
        INT_IN_PARCEL_LENGTH = parcel.dataPosition();

        parcel.setDataPosition(0);
        parcel.writeStrongBinder(new Binder());
        BINDER_IN_PARCEL_LENGTH = parcel.dataPosition();

        parcel.recycle();
    }
}
