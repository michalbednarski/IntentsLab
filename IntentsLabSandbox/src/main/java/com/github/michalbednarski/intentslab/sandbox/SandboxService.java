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

package com.github.michalbednarski.intentslab.sandbox;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.IBinder;
import android.widget.Toast;

/**
 * Created by mb on 28.09.13.
 */
public class SandboxService extends Service {
    private static final String HOST_PACKAGE_NAME = "com.github.michalbednarski.intentslab";
    private static IBinder sBinder = null;
    private static boolean sUninstallReceiverRegistered = false;

    @Override
    public IBinder onBind(Intent intent) {
        // Monitor removal/update of host app and exit in that case
        // To avoid being out-of-sync
        if (!sUninstallReceiverRegistered) {
            sUninstallReceiverRegistered = true;
            IntentFilter intentFilter = new IntentFilter(Intent.ACTION_PACKAGE_REMOVED);
            intentFilter.addDataScheme("package");
            intentFilter.addDataAuthority(HOST_PACKAGE_NAME, null);
            getApplicationContext().registerReceiver(new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    (new Handler()).post(new Runnable() {
                        @Override
                        public void run() {
                            System.exit(0);
                        }
                    });
                }
            }, intentFilter);
        }

        // Return cached binder if we have one
        if (sBinder != null) {
            return sBinder;
        }

        // Load code from host app and return provided by it binder
        try {
            final Context packageContext = createPackageContext(HOST_PACKAGE_NAME, CONTEXT_INCLUDE_CODE | CONTEXT_IGNORE_SECURITY);
            final Class<?> aClass = packageContext.getClassLoader().loadClass("com.github.michalbednarski.intentslab.sandbox.remote.SandboxInit");
            return (sBinder = (IBinder) aClass.getMethod("sandboxServiceOnBind", Service.class).invoke(null, this));
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "IntentsLab sandbox error: see log", Toast.LENGTH_SHORT).show();
            return null;
        }
    }
}
