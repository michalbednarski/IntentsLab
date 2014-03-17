package com.github.michalbednarski.intentslab.xposedhooks.internal;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by mb on 14.03.14.
 */
public class StackTraceWrapper implements Parcelable {
    public final StackTraceElement[] stackTrace;

    public StackTraceWrapper(StackTraceElement[] stackTrace) {
        this.stackTrace = stackTrace;
    }

    private StackTraceWrapper(Parcel source) {
        final int length = source.readInt();
        if (length == -1) {
            this.stackTrace = null;
            return;
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
        this.stackTrace = stackTrace;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        if (stackTrace == null) {
            dest.writeInt(-1);
            return;
        }
        dest.writeInt(stackTrace.length);
        for (StackTraceElement element : stackTrace) {
            dest.writeString(element.getClassName());
            dest.writeString(element.getMethodName());
            dest.writeString(element.getFileName());
            dest.writeInt(element.getLineNumber());
        }
    }

    public static final Creator<StackTraceWrapper> CREATOR = new Creator<StackTraceWrapper>() {
        @Override
        public StackTraceWrapper createFromParcel(Parcel source) {
            return new StackTraceWrapper(source);
        }

        @Override
        public StackTraceWrapper[] newArray(int size) {
            return new StackTraceWrapper[size];
        }
    };
}
