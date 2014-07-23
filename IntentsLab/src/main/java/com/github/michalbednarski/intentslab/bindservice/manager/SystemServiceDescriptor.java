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

package com.github.michalbednarski.intentslab.bindservice.manager;

import android.os.IBinder;
import android.os.Parcel;

/**
 * Service descriptor for system services.
 * See android.os.ServiceManager
 */
public class SystemServiceDescriptor extends ServiceDescriptor {
    final String mServiceName;

    public SystemServiceDescriptor(String serviceName) {
        mServiceName = serviceName;
    }

    @Override
    public String getTitle() {
        return mServiceName;
    }

    @Override
    void doBind(BindServiceManager.Helper.BindServiceMediator callback) {
        IBinder service = getSystemService(mServiceName);
        if (service != null) {
            callback.dispatchBound(service);
        }
    }

    public static IBinder getSystemService(String serviceName) {
        try {
            Class<?> serviceManager = Class.forName("android.os.ServiceManager");
            return (IBinder) serviceManager.getMethod("getService", String.class).invoke(null, serviceName);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // Parcelable
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mServiceName);
    }

    public static final Creator<SystemServiceDescriptor> CREATOR = new Creator<SystemServiceDescriptor>() {
        @Override
        public SystemServiceDescriptor createFromParcel(Parcel source) {
            return new SystemServiceDescriptor(source.readString());
        }

        @Override
        public SystemServiceDescriptor[] newArray(int size) {
            return new SystemServiceDescriptor[size];
        }
    };

    // equals and hashCode
    @Override
    public boolean equals(Object o) {
        if (o instanceof SystemServiceDescriptor) {
            return mServiceName.equals(((SystemServiceDescriptor) o).mServiceName);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return mServiceName.hashCode();
    }
}
