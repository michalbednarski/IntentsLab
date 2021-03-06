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

package com.github.michalbednarski.intentslab;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;
import com.github.michalbednarski.intentslab.bindservice.SystemServicesDialog;
import com.github.michalbednarski.intentslab.browser.BrowseComponentsActivity;
import com.github.michalbednarski.intentslab.clipboard.ClipboardActivity;
import com.github.michalbednarski.intentslab.editor.IntentEditorActivity;
import com.github.michalbednarski.intentslab.editor.IntentEditorInterceptedActivity;
import com.github.michalbednarski.intentslab.providerlab.AdvancedQueryActivity;
import com.github.michalbednarski.intentslab.providerlab.proxy.LogViewerActivity;
import com.github.michalbednarski.intentslab.runas.RemoteEntryPoint;
import com.github.michalbednarski.intentslab.runas.RunAsInitReceiver;

import java.util.Random;

public class StartActivity extends FragmentActivity {

    private ComponentName mInterceptActivityComponentName;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        // Saved items list
        ListView savedItemsList = (ListView) findViewById(android.R.id.list);
        if (savedItemsList != null) {
            savedItemsList.setEmptyView(findViewById(android.R.id.empty));
            SavedItemsDatabase.getInstance(this).lazyAttachListAdapter(savedItemsList);
        }

        RemoteEntryPoint.ensureInstalled(this);

        // Intercept Intent Activity
        mInterceptActivityComponentName = new ComponentName(this, IntentEditorInterceptedActivity.class);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_start, menu);
        // TODO: move to menu resource
        menu.add("Proxy log (experimental)").setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                startActivity(new Intent(StartActivity.this, LogViewerActivity.class));
                return true;
            }
        });
        menu.add("RunAs local (experimental)").setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                (new Thread() {
                    @Override
                    public void run() {
                        RemoteEntryPoint.main(new String[] {
                                new ComponentName(StartActivity.this, RunAsInitReceiver.class).flattenToShortString()
                        });
                    }
                }).start();
                return true;
            }
        });
        menu.add("Activity Monitor (experimental)").setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                startActivity(new Intent(StartActivity.this, ActivityMonitorActivity.class));
                return true;
            }
        });
        menu.add("Current objects (experimental)").setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                startActivity(new Intent(StartActivity.this, ClipboardActivity.class));
                return true;
            }
        });
        menu.add("Another instance (experimental)").setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                startActivity(
                        new Intent(
                                StartActivity.this,
                                StartActivityMultitask.class
                        )
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
                        .setAction("mi" + new Random().nextLong())
                );
                return true;
            }
        });
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem interceptionOption = menu.findItem(R.id.interception);
        interceptionOption.setChecked(
                getPackageManager().getComponentEnabledSetting(mInterceptActivityComponentName) == PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        );
        Utils.updateLegacyCheckedIcon(interceptionOption);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch (item.getItemId()) {
            case R.id.menu_settings:
                startActivity(new Intent(this, PrefsActivity.class));
                return true;
            case R.id.interception: {
                final PackageManager packageManager = getPackageManager();
                boolean enable =
                        packageManager.getComponentEnabledSetting(mInterceptActivityComponentName)
                                != PackageManager.COMPONENT_ENABLED_STATE_ENABLED;

                packageManager.setComponentEnabledSetting(
                        mInterceptActivityComponentName,
                        enable ?
                                PackageManager.COMPONENT_ENABLED_STATE_ENABLED :
                                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        PackageManager.DONT_KILL_APP
                );

                Toast.makeText(this, enable ? getString(R.string.interception_enabled) : getString(R.string.interception_disabled), Toast.LENGTH_SHORT).show();
                ActivityCompat.invalidateOptionsMenu(this);
                return true;
            }
            case R.id.system_services:
                (new SystemServicesDialog()).show(getSupportFragmentManager(), "systemServices");
                return true;
            case R.id.provider_lab:
                startActivity(new Intent(StartActivity.this, AdvancedQueryActivity.class));
                return true;
    	}
    	return false;
    }

    // Button actions
    public void startNew(View view) {
    	startActivity(new Intent(this, IntentEditorActivity.class));
    }

    public void pickFromRecents(View view) {
    	startActivity(new Intent(this, PickRecentlyRunningActivity.class));
    }

    public void browseApps(View view) {
    	startActivity(new Intent(this, BrowseComponentsActivity.class));
    }

    public void catchBroadcast(View view) {
    	new ReceiveBroadcastDialog()
                .show(getSupportFragmentManager(), "ReceiveBroadcastDialog");
    }

    public void viewSavedItems(View view) {
        startActivity(new Intent(this, SavedItemsActivity.class));
    }
}
