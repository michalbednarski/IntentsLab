package com.github.michalbednarski.intentslab.sandbox;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by mb on 30.09.13.
 */
public class SandboxedMethodArguments implements Parcelable {
    public final Object[] arguments;
    //
    public SandboxedMethodArguments(int count) {
        arguments = new Object[count];
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(arguments.length);
        for (int i = 0; i < arguments.length; i++) {
            Object arg = arguments[i];
            if (arg instanceof ISandboxedObject) {
                dest.writeInt(1);
                dest.writeStrongBinder(((ISandboxedObject) arg).asBinder());
            } else if (arg instanceof IAidlInterface) {
                dest.writeInt(2);
                dest.writeStrongBinder(((IAidlInterface) arg).asBinder());
            } else {
                dest.writeInt(0);
                dest.writeValue(arg);
            }
        }
    }

    public static final Creator<SandboxedMethodArguments> CREATOR = new Creator<SandboxedMethodArguments>() {
        @Override
        public SandboxedMethodArguments createFromParcel(Parcel source) {
            final SandboxedMethodArguments sma = new SandboxedMethodArguments(source.readInt());
            for (int i = 0; i < sma.arguments.length; i++) {
                switch (source.readInt()) {
                    case 1:
                        sma.arguments[i] = ISandboxedObject.Stub.asInterface(source.readStrongBinder());
                        break;
                    case 2:
                        sma.arguments[i] = IAidlInterface.Stub.asInterface(source.readStrongBinder());
                        break;
                    case 0:
                        sma.arguments[i] = source.readValue(null);
                        break;
                }
            }
            return sma;
        }

        @Override
        public SandboxedMethodArguments[] newArray(int size) {
            return new SandboxedMethodArguments[size];
        }
    };
}
