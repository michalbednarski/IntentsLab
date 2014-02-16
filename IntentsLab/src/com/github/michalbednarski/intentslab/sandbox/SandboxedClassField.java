package com.github.michalbednarski.intentslab.sandbox;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Sandboxed equivalent of {@link java.lang.reflect.Field}
 */
public class SandboxedClassField implements Parcelable {
    public SandboxedType type;
    public String name;
    public int modifiers;


    public SandboxedClassField(SandboxedType type, String name, int modifiers) {
        this.type = type;
        this.name = name;
        this.modifiers = modifiers;
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
        type.writeToParcel(dest, flags);
        dest.writeString(name);
        dest.writeInt(modifiers);
    }

    public static final Creator<SandboxedClassField> CREATOR = new Creator<SandboxedClassField>() {
        @Override
        public SandboxedClassField createFromParcel(Parcel source) {
            return new SandboxedClassField(
                    SandboxedType.CREATOR.createFromParcel(source),
                    source.readString(),
                    source.readInt()
            );
        }

        @Override
        public SandboxedClassField[] newArray(int size) {
            return new SandboxedClassField[size];
        }
    };
}
