package com.github.michalbednarski.intentslab.sandbox;

import android.os.BadParcelableException;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.Parcelable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * Created by mb on 01.02.14.
 */
public final class SandboxedObject implements Parcelable {

    private final Parcel mParcelledData;
    private final int mParcelledDataLength;
    private final String mAidlInterfaceClass;
    private final IBinder mAidlInterfaceBinder;

    public SandboxedObject(Object object) {
        if (object instanceof IInterface) {
            mAidlInterfaceBinder = ((IInterface) object).asBinder();
            Class<?> aClass = object.getClass();
            do {
                try {
                    String className = aClass.getName();
                    if (className.endsWith("$Stub$Proxy")) {
                        aClass = Class.forName(
                                className.substring(0, className.length() - 6),
                                true,
                                aClass.getClassLoader()
                        );
                        break;
                    } else if (className.endsWith("$Stub")) {
                        break;
                    }
                } catch (Exception ignored) {}
            } while ((aClass = aClass.getSuperclass()) != null);
            mAidlInterfaceClass = aClass.getName();

            mParcelledData = null;
            mParcelledDataLength = 0;
        } else {
            mParcelledData = Parcel.obtain();
            mParcelledData.setDataPosition(0);
            mParcelledData.writeValue(object);
            mParcelledDataLength = mParcelledData.dataPosition();

            mAidlInterfaceBinder = null;
            mAidlInterfaceClass = null;
        }
    }

    SandboxedObject(Parcel source) {
        mAidlInterfaceClass = source.readString();
        if (mAidlInterfaceClass != null) {
            mAidlInterfaceBinder = source.readStrongBinder();

            mParcelledData = null;
            mParcelledDataLength = 0;
        } else {
            mParcelledDataLength = source.readInt();
            mParcelledData = Parcel.obtain();
            mParcelledData.setDataPosition(0);

            int offset = source.dataPosition();
            source.setDataPosition(offset + mParcelledDataLength);
            mParcelledData.appendFrom(source, offset, mParcelledDataLength);

            mAidlInterfaceBinder = null;
        }
    }

    public Object unwrap(ClassLoader classLoader) {
        if (mAidlInterfaceClass != null) {
            Class<?> aClass;
            try {
                aClass = Class.forName(mAidlInterfaceClass, true, classLoader);
            } catch (ClassNotFoundException e) {
                throw new BadParcelableException(e);
            }
            try {
                Method asInterfaceMethod = aClass.getMethod("asInterface", IBinder.class);
                return asInterfaceMethod.invoke(null, mAidlInterfaceBinder);
            } catch (Exception ignored) {}
            try {
                Constructor<?> constructor = aClass.getDeclaredConstructor(IBinder.class);
                constructor.setAccessible(true);
                return constructor.newInstance(mAidlInterfaceBinder);
            } catch (Exception e) {
                throw new BadParcelableException(e);
            }
        } else {
            mParcelledData.setDataPosition(0);
            return mParcelledData.readValue(classLoader);
        }
    }

    @Override
    public int describeContents() {
        return mParcelledData != null && mParcelledData.hasFileDescriptors() ? CONTENTS_FILE_DESCRIPTOR : 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mAidlInterfaceClass);
        if (mAidlInterfaceClass != null) {
            dest.writeStrongBinder(mAidlInterfaceBinder);
        } else {
            dest.writeInt(mParcelledDataLength);
            dest.appendFrom(mParcelledData, 0, mParcelledDataLength);
        }
    }

    public static final Creator<SandboxedObject> CREATOR = new Creator<SandboxedObject>() {
        @Override
        public SandboxedObject createFromParcel(Parcel source) {
            return new SandboxedObject(source);
        }

        @Override
        public SandboxedObject[] newArray(int size) {
            return new SandboxedObject[size];
        }
    };
}
