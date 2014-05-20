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

package com.github.michalbednarski.intentslab.xposedhooks.app;

import android.app.Activity;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.RemoteException;
import android.view.View;
import android.widget.Toast;

import com.github.michalbednarski.intentslab.xposedhooks.R;
import com.github.michalbednarski.intentslab.xposedhooks.internal.IRefreshPermissionsCallback;
import com.github.michalbednarski.intentslab.xposedhooks.internal.XHUtils;

/**
 * Activity used for requesting permission to use this module
 */
public class RequestPermissionActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Parse action
        String[] splitAction = getIntent().getAction().split("\\|", 2);
        int uid = Integer.parseInt(splitAction[0]);
        String packageName = splitAction.length == 1 ? null : splitAction[1];


        // Get package info
        final AllowedAppsDb db = AllowedAppsDb.getInstance(this);
        final PackageInfo packageInfo;
        try {
            PackageManager packageManager = getPackageManager();
            if (packageName == null) {
                packageName = packageManager.getPackagesForUid(uid)[0];
            }

            packageInfo = packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES);
            if (packageInfo.applicationInfo.uid != uid) {
                throw new PackageManager.NameNotFoundException("Uid mismatch");
            }
        } catch (Exception e) {
            Toast.makeText(this, "Invalid package", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Prepare view
        setContentView(R.layout.permission_request);

        findViewById(R.id.cancel_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        findViewById(R.id.ok_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                db.allowApp(packageInfo);
                db.saveIfNeeded();
                try {
                    XHUtils.getSystemInterface().refreshPermissions(new IRefreshPermissionsCallback.Stub(){
                        @Override
                        public void refreshDone() throws RemoteException {
                            setResult(RESULT_OK);
                            finish();
                        }
                    });
                } catch (RemoteException e) {
                    throw new RuntimeException("Module not installed or system crashed", e);
                }
            }
        });
    }
}
