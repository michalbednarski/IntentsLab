package com.github.michalbednarski.intentslab.clipboard;

import com.github.michalbednarski.intentslab.sandbox.SandboxedObject;

import java.util.ArrayList;

/**
 * Created by mb on 15.02.14.
 */
public class ClipboardService /*extends Service*/ {

    static final ArrayList<Object> sLocalObjects = new ArrayList<Object>();
    static final ArrayList<SandboxedObject> sSandboxedObjects = new ArrayList<SandboxedObject>();

    public static void saveLocalObject(Object object) {
        sLocalObjects.add(object);
        ClipboardItemsFragment.refreshAll();
    }

    public static void saveSandboxedObject(SandboxedObject object) {
        // Try to unwrap
        Object localObject = null;
        try {
            localObject = object.unwrap(null);
        } catch (Exception ignored) {}
        if (localObject != null) {
            saveLocalObject(localObject);
            return;
        }

        // Save as sandboxed
        sSandboxedObjects.add(object);
        ClipboardItemsFragment.refreshAll();
    }

    /*@Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }*/
}
