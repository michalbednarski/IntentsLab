package com.github.michalbednarski.intentslab.clipboard;

import android.support.v4.util.ArrayMap;

import com.github.michalbednarski.intentslab.sandbox.SandboxedObject;
import com.github.michalbednarski.intentslab.valueeditors.framework.EditorLauncher;

/**
 * Created by mb on 15.02.14.
 */
public class ClipboardService /*extends Service*/ {
    static ArrayMap<String, SandboxedObject> sObjects = new ArrayMap<String, SandboxedObject>();

    public static void saveLocalObject(String name, Object object) {
        saveSandboxedObject(name, new SandboxedObject(object));
        ClipboardItemsFragment.refreshAll();
    }

    public static void saveSandboxedObject(String name, SandboxedObject object) {
        // Save as sandboxed
        sObjects.put(name, object);
        ClipboardItemsFragment.refreshAll();
    }

    static final EditorLauncher.EditorLauncherWithSandboxCallback EDITOR_LAUNCHER_CALLBACK = new EditorLauncher.EditorLauncherWithSandboxCallback() {
        @Override
        public void onSandboxedEditorResult(String key, SandboxedObject newWrappedValue) {
            saveSandboxedObject(key, newWrappedValue);
        }

        @Override
        public void onEditorResult(String key, Object newValue) {
            saveLocalObject(key, newValue);
        }
    };

    /*@Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }*/
}
