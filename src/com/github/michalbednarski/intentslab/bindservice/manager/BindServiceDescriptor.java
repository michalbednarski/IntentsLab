package com.github.michalbednarski.intentslab.bindservice.manager;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.Parcel;
import com.github.michalbednarski.intentslab.Utils;
import com.github.michalbednarski.intentslab.sandbox.SandboxManager;

/**
 * Service descriptor for services obtained through {@link Context#bindService(Intent, ServiceConnection, int)}
 */
public class BindServiceDescriptor extends ServiceDescriptor {

    private final Intent mIntent;

    public BindServiceDescriptor(Intent intent) {
        mIntent = intent;
    }

    @Override
    public String getTitle() {
        String action = mIntent.getAction();
        boolean hasAction = !Utils.stringEmptyOrNull(action);
        ComponentName component = mIntent.getComponent();
        boolean hasComponent = component != null;
        return Utils.afterLastDot((!hasAction && hasComponent) ? component.getClassName() : action);
    }

    // Parcelable
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(mIntent, 0);
    }

    public static final Creator<ServiceDescriptor> CREATOR = new Creator<ServiceDescriptor>() {
        @Override
        public ServiceDescriptor createFromParcel(Parcel source) {
            return new BindServiceDescriptor(source.<Intent>readParcelable(null));
        }

        @Override
        public ServiceDescriptor[] newArray(int size) {
            return new ServiceDescriptor[size];
        }
    };

    public boolean equals(Object o) {
        if (o instanceof BindServiceDescriptor) {
            BindServiceDescriptor b = (BindServiceDescriptor) o;
            return b.mIntent.filterEquals(b.mIntent);
        }
        return false;
    }

    public int hashCode() {
        return mIntent.filterHashCode();
    }

    @Override
    ConnectionManager getConnectionManager() {
        return new ConnectionManagerImpl(mIntent);
    }

    private static class ConnectionManagerImpl extends ConnectionManager implements ServiceConnection {

        private final Intent mIntent;

        public ConnectionManagerImpl(Intent intent) {
            mIntent = intent;
        }


        @Override
        void bind() {
            SandboxManager.getService().bindService(mIntent, this, Context.BIND_AUTO_CREATE);
        }

        @Override
        void unbind() {
            SandboxManager.getService().unbindService(this);
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mHelper.mPackageName = name.getPackageName();
            mHelper.dispatchBound(service);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mHelper.dispatchUnbound();
        }
    }
}
