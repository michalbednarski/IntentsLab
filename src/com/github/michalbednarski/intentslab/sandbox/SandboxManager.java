package com.github.michalbednarski.intentslab.sandbox;

import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.BadParcelableException;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcelable;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.NotificationCompat;
import com.github.michalbednarski.intentslab.R;
import com.github.michalbednarski.intentslab.bindservice.BindServiceManager;
import com.github.michalbednarski.intentslab.bindservice.BoundServicesListActivity;
import com.github.michalbednarski.intentslab.editor.BundleAdapter;
import com.github.michalbednarski.intentslab.runas.RunAsInitReceiver;

import java.io.Serializable;
import java.lang.reflect.Constructor;
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
                BindServiceManager.executePendingBindServices(this);
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
                                    new Intent(this, BoundServicesListActivity.class),
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


    private static boolean hasIBinderConstructor(Object o) {
        try {
            o.getClass().getDeclaredConstructor(IBinder.class);
        } catch (NoSuchMethodException e) {
            return false;
        }
        return true;
    }

    private static final String KEY_WRAPPED_VALUE = "v";
    private static final String KEY_WRAPPED_VALUE_TYPE = "t";
    private static final String KEY_WRAPPED_CLASS_NAME = "c";
    private static final int WRAPPED_VALUE_TYPE_AIDL = 1;

    public static Bundle wrapObject(Object o) {
        Bundle b = new Bundle();
        if (o instanceof IInterface && !(o instanceof Parcelable) && !(o instanceof Serializable) && hasIBinderConstructor(o)) {
            b.putInt(KEY_WRAPPED_VALUE_TYPE, WRAPPED_VALUE_TYPE_AIDL);
            String name = o.getClass().getName();
            b.putString(KEY_WRAPPED_CLASS_NAME, name);
            RunAsInitReceiver.putBinderInBundle(b, KEY_WRAPPED_VALUE, ((IInterface) o).asBinder());
        } else {
            BundleAdapter.putInBundle(b, KEY_WRAPPED_VALUE, o);
        }
        return b;
    }

    public static Object unwrapObject(Bundle b) {
        return unwrapObject(b, null);
    }

    public static Object unwrapObject(Bundle b, ClassLoader classLoader) {
        if (classLoader != null) {
            b.setClassLoader(classLoader);
        }
        int type = b.getInt(KEY_WRAPPED_VALUE_TYPE);
        if (type == WRAPPED_VALUE_TYPE_AIDL) {
            try {
                if (classLoader == null) {
                    throw new CannotLoadAidlException("There is no ClassLoader");
                }
                Constructor<?> constructor =
                        classLoader
                        .loadClass(b.getString(KEY_WRAPPED_CLASS_NAME))
                        .getDeclaredConstructor(IBinder.class);
                constructor.setAccessible(true);
                return constructor.newInstance(b.get(KEY_WRAPPED_VALUE));
            } catch (ClassNotFoundException e) {
                throw new CannotLoadAidlException(e);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return b.get(KEY_WRAPPED_VALUE);
    }

    public static class CannotLoadAidlException extends BadParcelableException {
        CannotLoadAidlException(Exception cause) {
            super(cause);
        }

        public CannotLoadAidlException(String message) {
            super(message);
        }
    }
}
