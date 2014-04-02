package com.github.michalbednarski.intentslab.sandbox;

import android.os.IInterface;
import android.os.Parcel;
import android.os.Parcelable;

import com.github.michalbednarski.intentslab.Utils;

import java.util.Arrays;

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

    public final Class<?> aClass;
    public final String typeName;
    public final Type type;

    public enum Type {
        UNKNOWN,
        PRIMITIVE,
        OBJECT,
        AIDL_INTERFACE
    }


    public SandboxedType(Class<?> aClass) {
        this.aClass = aClass;
        this.typeName = aClass.getName();
        for (int i = 0; i < PRIMITIVE_TYPES.length; i++) {
            if (aClass == PRIMITIVE_TYPES[i]) {
                type = Type.PRIMITIVE;
                return;
            }
        }
        type = Arrays.asList(aClass.getInterfaces()).contains(IInterface.class) ? Type.AIDL_INTERFACE : Type.OBJECT;
    }

    public static SandboxedType[] wrapClassesArray(Class<?>[] classes) {
        final int j = classes.length;
        SandboxedType[] types = new SandboxedType[j];
        for (int i = 0; i < j; i++) {
            types[i] = new SandboxedType(classes[i]);
        }
        return types;
    }

    public String toString() {
        return typeName;
    }

    public Object getDefaultValue() {
        if (type == Type.PRIMITIVE) {
            return Utils.getDefaultValueForPrimitveClass(aClass);
        }
        return null;
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
        dest.writeInt(type.ordinal());

        // Primitive type?
        if (type == Type.PRIMITIVE) {
            for (int j = 0; j < SandboxedType.PRIMITIVE_TYPES.length; j++) {
                if (aClass == SandboxedType.PRIMITIVE_TYPES[j]) {
                    dest.writeInt(j);
                    return;
                }
            }
            assert false; // Should never happen
        }

        // Write type
        dest.writeString(typeName);
    }

    SandboxedType(Parcel source) {
        type = Type.values()[source.readInt()];

        // Primitive type?
        if (type == Type.PRIMITIVE) {
            aClass = SandboxedType.PRIMITIVE_TYPES[source.readInt()];
            typeName = aClass.getName();
        } else {
            // Not primitive
            typeName = source.readString();
            Class<?> tmpClass = null;
            try {
                tmpClass = Class.forName(typeName);
            } catch (ClassNotFoundException ignored) {}
            aClass = tmpClass;
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
