/*
 * IntentsLab - Android app for playing with Intents and Binder IPC
 * Copyright (C) 2014 Michał Bednarski
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

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
