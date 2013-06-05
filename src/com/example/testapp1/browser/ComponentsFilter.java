package com.example.testapp1.browser;

import android.os.Parcel;
import android.os.Parcelable;

public class ComponentsFilter implements Parcelable {

    public static final int TYPE_ACTIVITY = 1;
    public static final int TYPE_RECEIVER = 2;
    public static final int TYPE_SERVICE = 4;
    public static final int TYPE_CONTENT_PROVIDER = 8;
    public int type = TYPE_ACTIVITY;


    public static final int APP_TYPE_USER = 1;
    public static final int APP_TYPE_SYSTEM = 2;
    public int appType = APP_TYPE_USER;


    public static final int PROTECTION_WORLD_ACCESSIBLE = 1;
    public static final int PROTECTION_NORMAL = 2;
    public static final int PROTECTION_DANGEROUS = 4;
    public static final int PROTECTION_SIGNATURE = 8;
    public static final int PROTECTION_SYSTEM = 16;
    public static final int PROTECTION_DEVELOPMENT = 32;
    public static final int PROTECTION_UNEXPORTED = 64;
    public static final int PROTECTION_UNKNOWN = 128;

    public int protection = PROTECTION_WORLD_ACCESSIBLE;

    public static final int PROTECTION_ANY = 128 * 2 - 1;
    public static final int PROTECTION_ANY_PERMISSION =
            PROTECTION_NORMAL |
                    PROTECTION_DANGEROUS |
                    PROTECTION_SIGNATURE |
                    PROTECTION_SYSTEM |
                    PROTECTION_DEVELOPMENT |
                    PROTECTION_UNKNOWN;
    public static final int PROTECTION_ANY_OBTAINABLE =
            PROTECTION_WORLD_ACCESSIBLE |
                    PROTECTION_NORMAL |
                    PROTECTION_DANGEROUS;
    public static final int PROTECTION_ANY_EXPORTED =
            PROTECTION_ANY & ~PROTECTION_UNEXPORTED;


    public boolean requireMetaData = false;
    public String requireMetaDataSubstring = null;

    public boolean testWritePermissionForProviders = false;

    // Serializing to parcel
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(
                (requireMetaData ? 1 : 0) |
                (testWritePermissionForProviders ? 2 : 0)
        );
        dest.writeInt(type);
        dest.writeInt(appType);
        dest.writeInt(protection);
        if (requireMetaData) {
            dest.writeString(requireMetaDataSubstring);
        }
    }

    public static final Creator<ComponentsFilter> CREATOR = new Creator<ComponentsFilter>() {
        @Override
        public ComponentsFilter createFromParcel(Parcel source) {
            int flags = source.readInt();
            ComponentsFilter componentsFilter = new ComponentsFilter();
            componentsFilter.type = source.readInt();
            componentsFilter.appType = source.readInt();
            componentsFilter.protection = source.readInt();
            componentsFilter.requireMetaData = (flags & 1) != 0;
            if (componentsFilter.requireMetaData) {
                componentsFilter.requireMetaDataSubstring = source.readString();
            }
            componentsFilter.testWritePermissionForProviders = (flags & 2) != 0;
            return componentsFilter;
        }

        @Override
        public ComponentsFilter[] newArray(int size) {
            return new ComponentsFilter[size];
        }
    };
}
