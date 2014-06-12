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

package com.github.michalbednarski.intentslab.browser;

import android.content.IntentFilter;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Information about registered receiver
 */
public class RegisteredReceiverInfo implements Parcelable {
    public String processName;
    public int pid;
    public int uid;
    public IntentFilter[] intentFilters;
    public String[] filterPermissions;


    public String getOverallPermission() throws MixedPermissionsException {
        String firstPermission = filterPermissions[0];
        for (String permission : filterPermissions) {
            if (firstPermission == null ? permission != null : !firstPermission.equals(permission)) {
                throw new MixedPermissionsException();
            }
        }
        return firstPermission;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(processName);
        dest.writeInt(pid);
        dest.writeInt(uid);
        dest.writeInt(intentFilters.length);
        for (IntentFilter intentFilter : intentFilters) {
            intentFilter.writeToParcel(dest, flags);
        }
        dest.writeStringArray(filterPermissions);
    }

    public static final Creator<RegisteredReceiverInfo> CREATOR = new Creator<RegisteredReceiverInfo>() {
        @Override
        public RegisteredReceiverInfo createFromParcel(Parcel source) {
            RegisteredReceiverInfo receiverInfo = new RegisteredReceiverInfo();
            receiverInfo.processName = source.readString();
            receiverInfo.pid = source.readInt();
            receiverInfo.uid = source.readInt();
            int filterCount = source.readInt();
            IntentFilter[] filters = new IntentFilter[filterCount];
            for (int i = 0; i < filterCount; i++) {
                filters[i] = IntentFilter.CREATOR.createFromParcel(source);
            }
            receiverInfo.intentFilters = filters;
            receiverInfo.filterPermissions = source.createStringArray();
            return receiverInfo;
        }

        @Override
        public RegisteredReceiverInfo[] newArray(int size) {
            return new RegisteredReceiverInfo[size];
        }
    };

    public static class MixedPermissionsException extends Exception {}
}
