package com.github.michalbednarski.intentslab.xposedhooks.api;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by mb on 10.05.14.
 */
public class ReadBundleEntryInfo implements Parcelable {

    public String name;
    public String methodName;
    public StackTraceElement[] stackTrace;



    public ReadBundleEntryInfo() { /* empty */ }



    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeString(methodName);
        writeStackTraceToParcel(stackTrace, dest);
    }

    private ReadBundleEntryInfo(Parcel source) {
        name = source.readString();
        methodName = source.readString();
        stackTrace = createStackTraceFromParcel(source);
    }

    public static final Creator<ReadBundleEntryInfo> CREATOR = new Creator<ReadBundleEntryInfo>() {
        @Override
        public ReadBundleEntryInfo createFromParcel(Parcel source) {
            return new ReadBundleEntryInfo(source);
        }

        @Override
        public ReadBundleEntryInfo[] newArray(int size) {
            return new ReadBundleEntryInfo[size];
        }
    };





    // Helpers for parcelling stack trace
    public static void writeStackTraceToParcel(StackTraceElement[] stackTrace, Parcel dest) {
        if (stackTrace != null) {
            dest.writeInt(stackTrace.length);
            for (StackTraceElement element : stackTrace) {
                dest.writeString(element.getClassName());
                dest.writeString(element.getMethodName());
                dest.writeString(element.getFileName());
                dest.writeInt(element.getLineNumber());
            }
        } else {
            dest.writeInt(-1);
        }
    }

    public static StackTraceElement[] createStackTraceFromParcel(Parcel source) {
        final int length = source.readInt();
        if (length == -1) {
            return null;
        }

        StackTraceElement[] stackTrace = new StackTraceElement[length];
        for (int i = 0; i < length; i++) {
            stackTrace[i] = new StackTraceElement(
                    source.readString(), // Class
                    source.readString(), // Method
                    source.readString(), // File
                    source.readInt() // Line
            );
        }
        return stackTrace;
    }
}
