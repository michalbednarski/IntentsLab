package com.github.michalbednarski.intentslab.sandbox;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Description of ClassLoader to be used to load classes of AIDL interfaces and sandboxed objects
 */
public class ClassLoaderDescriptor implements Parcelable {
    private String mPackageName;

    public ClassLoaderDescriptor(String packageName) {
        mPackageName = packageName;
    }

    public ClassLoader getClassLoader(Context topContext) {
        if (mPackageName != null) {
            try {
                return topContext.createPackageContext(mPackageName, Context.CONTEXT_INCLUDE_CODE | Context.CONTEXT_IGNORE_SECURITY).getClassLoader();
            } catch (PackageManager.NameNotFoundException ignored) {}
        }
        return topContext.getClassLoader();
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
        dest.writeString(mPackageName);
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
