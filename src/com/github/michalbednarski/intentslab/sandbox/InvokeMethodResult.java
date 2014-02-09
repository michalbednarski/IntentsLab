package com.github.michalbednarski.intentslab.sandbox;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Result of {@link IAidlInterface#invokeMethod(int, SandboxedMethodArguments)}
 */
public class InvokeMethodResult implements Parcelable {
    public SandboxedObject sandboxedReturnValue;
    public String returnValueAsString;
    public String exception;


    @Override
    public int describeContents() {
        return sandboxedReturnValue instanceof Parcelable ? sandboxedReturnValue.describeContents() : 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
         if (exception != null) {
            dest.writeInt(2);
            dest.writeString(exception);
        } else if (sandboxedReturnValue != null) {
            dest.writeInt(1);
            sandboxedReturnValue.writeToParcel(dest, 0);
            dest.writeString(returnValueAsString);
        } else {
            dest.writeInt(0);
        }
    }

    public static final Creator<InvokeMethodResult> CREATOR = new Creator<InvokeMethodResult>() {
        @Override
        public InvokeMethodResult createFromParcel(Parcel source) {
            InvokeMethodResult result = new InvokeMethodResult();
            switch (source.readInt()) {
                case 1:
                    result.sandboxedReturnValue = SandboxedObject.CREATOR.createFromParcel(source);
                    result.returnValueAsString = source.readString();
                    break;
                case 2:
                    result.exception = source.readString();
                    break;
            }
            return result;
        }

        @Override
        public InvokeMethodResult[] newArray(int size) {
            return new InvokeMethodResult[size];
        }
    };
}
