package com.github.michalbednarski.intentslab.sandbox;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by mb on 07.10.13.
 */
public class ParcelableValue implements Parcelable {
    public Object value;

    public ParcelableValue() {
    }

    public ParcelableValue(Object value) {
        this.value = value;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeValue(value);
    }

    public static final Creator<ParcelableValue> CREATOR = new Creator<ParcelableValue>() {
        @Override
        public ParcelableValue createFromParcel(Parcel source) {
            final ParcelableValue parcelableValue = new ParcelableValue();
            parcelableValue.value = source.readValue(null);
            return parcelableValue;
        }

        @Override
        public ParcelableValue[] newArray(int size) {
            return new ParcelableValue[size];
        }
    };
}
