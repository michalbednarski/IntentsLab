package com.github.michalbednarski.intentslab.sandbox;

import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.NotificationCompat;
import com.github.michalbednarski.intentslab.R;
import com.github.michalbednarski.intentslab.clipboard.ClipboardActivity;

import java.util.ArrayList;

/**
 * Created by mb on 30.09.13.
 */
public class SandboxManager {

    private static final String SANDBOX_PACKAGE = "com.github.michalbednarski.intentslab.sandbox";
    private static final String SANDBOX_SERVICE_CLASS = "com.github.michalbednarski.intentslab.sandbox.SandboxService";
    private static final String RESET_RECEIVER_CLASS = "com.github.michalbednarski.intentslab.sandbox.ResetReceiver";



    /**
     * Disallow instantiate
     */
    private SandboxManager() {}


    private static ISandbox sSandbox = null;
    private static AService sService = null;
    private static ArrayList<Runnable> sSandboxReadyCallbacks = new ArrayList<Runnable>();


    public static boolean isReady() {
        return sSandbox != null && sSandbox.asBinder().isBinderAlive();
    }

    public static void initSandboxAndRunWhenReady(Context context, Runnable whenReady) {
        if (isReady()) {
            whenReady.run();
        } else {
            sSandboxReadyCallbacks.add(whenReady);
            context.startService(new Intent(context, AService.class));
        }
    }

    public static void resetSandbox(Context context) {
        context.sendBroadcast(new Intent().setClassName(SANDBOX_PACKAGE, RESET_RECEIVER_CLASS));
    }

    public static boolean isSandboxInstalled(Context context) {
        try {
            context.getPackageManager().getPackageInfo(SANDBOX_PACKAGE, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    public static void requestSandboxInstall(FragmentActivity activity) {
        (new SandboxInstallRequestDialog()).show(activity.getSupportFragmentManager(), "sandboxInstallRequest");
    }


    public static Service getService() {
        return sService;
    }

    public static ISandbox getSandbox() {
        return sSandbox;
    }

    private static int sRefCount = 0;
    public static void refSandbox() {
        sRefCount++;
    }

    public static void unrefSandbox() {
        sRefCount--;
        assert sRefCount >= 0;
    }



    /**
     * Service for keeping sandbox bound
     */
    public static class AService extends Service {

        @Override
        public int onStartCommand(Intent intent, int flags, int startId) {
            if (sService == null) {
                sService = this;
            }
            if (isReady()) {
                return START_NOT_STICKY;
            }
            startForeground(235,
                    new NotificationCompat.Builder(this)
                            .setSmallIcon(R.drawable.ic_launcher)
                            .setContentText("[Bound services]")
                            .setContentIntent(PendingIntent.getActivity(
                                    this,
                                    0,
                                    new Intent(this, ClipboardActivity.class),
                                    0
                            ))
                            .build());

            bindService(new Intent().setClassName(SANDBOX_PACKAGE, SANDBOX_SERVICE_CLASS), new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName name, IBinder service) {
                    sSandbox = ISandbox.Stub.asInterface(service);
                    for (Runnable sandboxReadyCallback : sSandboxReadyCallbacks) {
                        sandboxReadyCallback.run();
                    }
                    sSandboxReadyCallbacks.clear();
                }

                @Override
                public void onServiceDisconnected(ComponentName name) {
                    sSandbox = null;
                    stopSelf();
                }
            }, BIND_AUTO_CREATE);

            return START_NOT_STICKY;
        }

        @Override
        public void onDestroy() {
            sService = null;
        }

        public IBinder onBind(Intent intent) {
            return null;
        }
    }
}
