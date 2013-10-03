package com.github.michalbednarski.intentslab.sandbox;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by mb on 30.09.13.
 */
public class SandboxedMethod implements Parcelable {

    public int methodNumber;
    public String name;
    public String[] argumentTypes;
    public byte[] argumentTypesFlags;


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(methodNumber);
        dest.writeString(name);
        dest.writeStringArray(argumentTypes);
        dest.writeByteArray(argumentTypesFlags);
    }

    public static final Creator<SandboxedMethod> CREATOR = new Creator<SandboxedMethod>() {
        @Override
        public SandboxedMethod createFromParcel(Parcel source) {
            SandboxedMethod sm = new SandboxedMethod();
            sm.methodNumber = source.readInt();
            sm.name = source.readString();
            sm.argumentTypes = source.createStringArray();
            sm.argumentTypesFlags = source.createByteArray();
            return sm;
        }

        @Override
        public SandboxedMethod[] newArray(int size) {
            return new SandboxedMethod[size];
        }
    };
}
