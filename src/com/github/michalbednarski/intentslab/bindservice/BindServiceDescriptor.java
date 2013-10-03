package com.github.michalbednarski.intentslab.bindservice;

import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;

/**
* Created by mb on 02.10.13.
*/
public class BindServiceDescriptor implements Parcelable {

    Intent intent;
    // TODO: run-as

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(intent, 0);
    }

    public static final Creator<BindServiceDescriptor> CREATOR = new Creator<BindServiceDescriptor>() {
        @Override
        public BindServiceDescriptor createFromParcel(Parcel source) {
            final BindServiceDescriptor d = new BindServiceDescriptor();
            d.intent = source.readParcelable(null);
            return d;
        }

        @Override
        public BindServiceDescriptor[] newArray(int size) {
            return new BindServiceDescriptor[size];
        }
    };

    public boolean equals(Object o) {
        if (o instanceof BindServiceDescriptor) {
            BindServiceDescriptor b = (BindServiceDescriptor) o;
            return b.intent.filterEquals(b.intent);
        }
        return false;
    }

    public int hashCode() {
        return intent.filterHashCode();
    }
}
