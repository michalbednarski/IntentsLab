package com.github.michalbednarski.intentslab.bindservice.manager;

import android.os.IBinder;
import android.os.Parcel;

/**
 * Service descriptor for system services.
 * See android.os.ServiceManager
 */
public class SystemServiceDescriptor extends ServiceDescriptor {
    final String mServiceName;

    public SystemServiceDescriptor(String serviceName) {
        mServiceName = serviceName;
    }

    @Override
    ConnectionManager getConnectionManager() {
        return new ConnectionManagerImpl();
    }

    @Override
    public String getTitle() {
        return mServiceName;
    }

    class ConnectionManagerImpl extends ConnectionManager {

        @Override
        void bind() {
            IBinder service = null;
            try {
                Class<?> serviceManager = Class.forName("android.os.ServiceManager");
                service = (IBinder) serviceManager.getMethod("getService", String.class).invoke(null, mServiceName);
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (service != null) {
                mHelper.dispatchBound(service);
            }
        }

        @Override
        void unbind() {}
    }

    // Parcelable
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mServiceName);
    }

    public static final Creator<SystemServiceDescriptor> CREATOR = new Creator<SystemServiceDescriptor>() {
        @Override
        public SystemServiceDescriptor createFromParcel(Parcel source) {
            return new SystemServiceDescriptor(source.readString());
        }

        @Override
        public SystemServiceDescriptor[] newArray(int size) {
            return new SystemServiceDescriptor[size];
        }
    };

    // equals and hashCode
    @Override
    public boolean equals(Object o) {
        if (o instanceof SystemServiceDescriptor) {
            return mServiceName.equals(((SystemServiceDescriptor) o).mServiceName);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return mServiceName.hashCode();
    }
}
