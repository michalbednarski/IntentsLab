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

package com.github.michalbednarski.intentslab.sandbox;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Description of ClassLoader to be used to load classes of AIDL interfaces and sandboxed objects
 */
public final class ClassLoaderDescriptor implements Parcelable {
    public String packageName;

    public ClassLoaderDescriptor(String packageName) {
        this.packageName = packageName;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ClassLoaderDescriptor that = (ClassLoaderDescriptor) o;

        if (packageName != null ? !packageName.equals(that.packageName) : that.packageName != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        return packageName != null ? packageName.hashCode() : 0;
    }


    /*
     *
     * Parcelable
     *
     */

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(packageName);
    }

    public static final Creator<ClassLoaderDescriptor> CREATOR = new Creator<ClassLoaderDescriptor>() {
        @Override
        public ClassLoaderDescriptor createFromParcel(Parcel source) {
            return new ClassLoaderDescriptor(source.readString());
        }

        @Override
        public ClassLoaderDescriptor[] newArray(int size) {
            return new ClassLoaderDescriptor[size];
        }
    };
}
