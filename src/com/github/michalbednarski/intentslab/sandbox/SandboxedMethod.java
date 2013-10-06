package com.github.michalbednarski.intentslab.sandbox;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by mb on 30.09.13.
 */
public class SandboxedMethod implements Parcelable {
    private static final Class<?>[] PRIMITIVE_TYPES = new Class[] {
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

    public int methodNumber;
    public String name;
    public Class<?>[] argumentTypes;


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(methodNumber);
        dest.writeString(name);

        int argumentsCount = argumentTypes.length;
        dest.writeInt(argumentsCount);
        a:
        for (int i = 0; i < argumentsCount; i++) {
            Class<?> arg = argumentTypes[i];

            // Primitive type?
            for (int j = 0; j < PRIMITIVE_TYPES.length; j++) {
                if (arg == PRIMITIVE_TYPES[j]) {
                    dest.writeInt(j);
                    continue a;
                }
            }

            // Write type
            dest.writeInt(-1);
            dest.writeString(arg.getName());
        }
    }

    public static final Creator<SandboxedMethod> CREATOR = new Creator<SandboxedMethod>() {
        @Override
        public SandboxedMethod createFromParcel(Parcel source) {
            SandboxedMethod sm = new SandboxedMethod();
            sm.methodNumber = source.readInt();
            sm.name = source.readString();

            int argumentCount = source.readInt();
            sm.argumentTypes = new Class[argumentCount];
            for (int i = 0; i < argumentCount; i++) {
                int typeId = source.readInt();

                // Primitive type?
                if (typeId >= 0) {
                    sm.argumentTypes[i] = PRIMITIVE_TYPES[typeId];
                } else {
                    try {
                        sm.argumentTypes[i] = Class.forName(source.readString());
                    } catch (ClassNotFoundException e) {
                        sm.argumentTypes[i] = ISandboxedObject.class;
                    }
                }
            }
            return sm;
        }

        @Override
        public SandboxedMethod[] newArray(int size) {
            return new SandboxedMethod[size];
        }
    };
}
