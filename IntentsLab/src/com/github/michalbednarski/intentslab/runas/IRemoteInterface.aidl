package com.github.michalbednarski.intentslab.runas;

//import java.lang.Throwable;
import android.app.IActivityController;
import android.app.IServiceConnection;

/**
 * Interface exposed by remote process for invoking methods on behalf of it from UI
 *
 * Note: This interface is used only for internal IPC and doesn't have to be backward-compatible
 */
interface IRemoteInterface {
    Bundle startActivity(in Intent intent, IBinder token, int requestCode);

    int bindService(IBinder sandboxApplicationToken, in Intent intent, IServiceConnection conn);

    void setActivityController(IActivityController controller);

    oneway void dumpServiceAsync(IBinder service, in ParcelFileDescriptor fd, in String[] args);
}
