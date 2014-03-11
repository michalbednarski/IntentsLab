package com.github.michalbednarski.intentslab.sandbox;

import android.os.BadParcelableException;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.Parcelable;

import com.github.michalbednarski.intentslab.Utils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * Created by mb on 01.02.14.
 */
public final class SandboxedObject implements Parcelable {
    /**
     * Enum describing type of object
     */
    public enum Type {
        AIDL(false, AidlHelper.class, new TypeChecker() {
            @Override
            public boolean check(Object object) {
                return object instanceof IInterface;
            }
        }),
        OBJECT_ARRAY(true, ObjectArrayHelper.class, new TypeChecker() {
            @Override
            public boolean check(Object object) {
                return object instanceof Object[];
            }
        }),
        OBJECT(true, ObjectHelper.class, new TypeChecker() {
            @Override
            public boolean check(Object object) {
                return true;
            }
        });



        Type(boolean mutable, Class<? extends Helper> helperClass, TypeChecker typeChecker) {
            mMutable = mutable;
            mHelperClass = helperClass;
            mTypeChecker = typeChecker;
        }

        public boolean isMutable() {
            return mMutable;
        }

        private final boolean mMutable;
        private final Class<? extends Helper> mHelperClass;
        private final TypeChecker mTypeChecker;
    }

    private final Type mType;
    private final Helper mHelper;

    /**
     * Constructor for wrapping new object
     */
    public SandboxedObject(Object object) {
        for (Type type : Type.values()) {
            if (type.mTypeChecker.check(object)) {
                mType = type;
                try {
                    mHelper = type.mHelperClass.getDeclaredConstructor(Object.class).newInstance(object);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                return;
            }
        }
        throw new AssertionError("No type matched");
    }

    /**
     * Constructor for loading from parcel
     */
    SandboxedObject(Parcel source) {
        try {
            mType = Type.values()[source.readInt()];
            mHelper = mType.mHelperClass.getDeclaredConstructor(Parcel.class).newInstance(source);
        } catch (Exception e) {
            throw new BadParcelableException(e);
        }
    }

    /**
     * Unwrap and object
     *
     * @param classLoader Class loader to use or null
     */
    public Object unwrap(ClassLoader classLoader) {
        return mHelper.unwrap(classLoader);
    }

    @Override
    public int describeContents() {
        return mHelper.hasFileDescriptors() ? CONTENTS_FILE_DESCRIPTOR : 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mType.ordinal());
        mHelper.writeToParcel(dest);
    }

    /**
     * Interface for checking type when wrapping object
     */
    private interface TypeChecker {
        boolean check(Object object);
    }

    /**
     * Interface for managing object
     */
    private interface Helper {
        /*
         * Wrap an object:
         *
         * Constructor(Object object);
         */

        /*
         * Read data from parcel (don't unwrap yet)
         *
         * Constructor(Parcel source);
         */

        /**
         * Write wrapped data to parcel
         */
        void writeToParcel(Parcel dest);

        /**
         * Unwrap object using following class loader
         */
        Object unwrap(ClassLoader classLoader);

        /**
         * True if data written to parcel will contain file descriptors
         */
        boolean hasFileDescriptors();
    }

    /**
     * Generic helper based on {@link android.os.Parcel#writeValue(Object)}
     */
    private static class ObjectHelper implements Helper {
        private final Parcel mParcelledData;
        private final int mParcelledDataLength;

        ObjectHelper(Object object) {
            mParcelledData = Parcel.obtain();
            mParcelledData.setDataPosition(0);
            mParcelledData.writeValue(object);
            mParcelledDataLength = mParcelledData.dataPosition();
        }

        ObjectHelper(Parcel source) {
            mParcelledDataLength = source.readInt();
            mParcelledData = Parcel.obtain();
            mParcelledData.setDataPosition(0);

            int offset = source.dataPosition();
            source.setDataPosition(offset + mParcelledDataLength);
            mParcelledData.appendFrom(source, offset, mParcelledDataLength);
        }

        @Override
        public void writeToParcel(Parcel dest) {
            dest.writeInt(mParcelledDataLength);
            dest.appendFrom(mParcelledData, 0, mParcelledDataLength);
        }

        @Override
        public Object unwrap(ClassLoader classLoader) {
            mParcelledData.setDataPosition(0);
            return mParcelledData.readValue(classLoader);
        }

        @Override
        public boolean hasFileDescriptors() {
            return mParcelledData.hasFileDescriptors();
        }
    }

    /**
     * Helper for array
     */
    private static class ObjectArrayHelper extends ObjectHelper {
        private final String mArrayClassName;

        ObjectArrayHelper(Object object) {
            super(object);
            mArrayClassName = object.getClass().getName();
        }

        ObjectArrayHelper(Parcel source) {
            super(source);
            mArrayClassName = source.readString();
        }

        @Override
        public void writeToParcel(Parcel dest) {
            super.writeToParcel(dest);
            dest.writeString(mArrayClassName);
        }

        @Override
        public Object unwrap(ClassLoader classLoader) {
            try {
                return Utils.deepCastArray(
                        (Object[]) super.unwrap(classLoader),
                        Class.forName(mArrayClassName, true, classLoader)
                );
            } catch (ClassNotFoundException e) {
                throw new BadParcelableException(e);
            }
        }
    }

    /**
     * Helper for wrapping Aidl interfaces
     */
    private static class AidlHelper implements Helper {
        private final String mAidlInterfaceClass;
        private final IBinder mAidlInterfaceBinder;

        AidlHelper(Object object) {
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
        }

        AidlHelper(Parcel source) {
            mAidlInterfaceClass = source.readString();
            mAidlInterfaceBinder = source.readStrongBinder();
        }

        @Override
        public void writeToParcel(Parcel dest) {
            dest.writeString(mAidlInterfaceClass);
            dest.writeStrongBinder(mAidlInterfaceBinder);
        }

        @Override
        public Object unwrap(ClassLoader classLoader) {
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
        }

        @Override
        public boolean hasFileDescriptors() {
            return false;
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
