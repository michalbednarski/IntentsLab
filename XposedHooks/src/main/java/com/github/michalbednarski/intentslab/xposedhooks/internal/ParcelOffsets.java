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
