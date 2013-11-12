package com.github.michalbednarski.intentslab.sandbox;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * {@link java.lang.Class} that can be used across processes
 */
public class SandboxedType implements Parcelable {
    static final Class<?>[] PRIMITIVE_TYPES = new Class[] {
            Boolean.TYPE,
            Byte.TYPE,
            Character.TYPE,
            Short.TYPE,
            Integer.TYPE,
            Long.TYPE,
            Float.TYPE,
            Double.TYPE,
            Void.TYPE
    };

    public Class<?> aClass;
    public String typeName;


    public SandboxedType(Class<?> aClass) {
        this.aClass = aClass;
    }

    public static SandboxedType[] wrapClassesArray(Class<?>[] classes) {
        final int j = classes.length;
        SandboxedType[] types = new SandboxedType[j];
        for (int i = 0; i < j; i++) {
            types[i] = new SandboxedType(classes[i]);
        }
        return types;
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
        // Primitive type?
        for (int j = 0; j < SandboxedType.PRIMITIVE_TYPES.length; j++) {
            if (aClass == SandboxedType.PRIMITIVE_TYPES[j]) {
                dest.writeInt(j);
                return;
            }
        }

        // Write type
        dest.writeInt(-1);
        dest.writeString(typeName != null ? typeName : aClass.getName());
    }

    SandboxedType(Parcel source) {
        int typeId = source.readInt();

        // Primitive type?
        if (typeId >= 0) {
            aClass = SandboxedType.PRIMITIVE_TYPES[typeId];
        } else {
            // Not primitive
            typeName = source.readString();
            try {
                aClass =  Class.forName(typeName);
            } catch (ClassNotFoundException e) {
                aClass = ISandboxedObject.class;
            }
        }
    }

    public static final Creator<SandboxedType> CREATOR = new Creator<SandboxedType>() {
        @Override
        public SandboxedType createFromParcel(Parcel source) {
            return new SandboxedType(source);
        }

        @Override
        public SandboxedType[] newArray(int size) {
            return new SandboxedType[size];
        }
    };
}
