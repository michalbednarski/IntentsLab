package com.github.michalbednarski.intentslab;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;
import com.github.michalbednarski.intentslab.bindservice.manager.BindServiceManager;
import com.github.michalbednarski.intentslab.bindservice.manager.SystemServiceDescriptor;
import com.github.michalbednarski.intentslab.browser.BrowseComponentsActivity;
import com.github.michalbednarski.intentslab.editor.IntentEditorActivity;
import com.github.michalbednarski.intentslab.editor.IntentEditorInterceptedActivity;
import com.github.michalbednarski.intentslab.providerlab.AdvancedQueryActivity;
import com.github.michalbednarski.intentslab.providerlab.proxy.LogViewerActivity;
import com.github.michalbednarski.intentslab.runas.RemoteEntryPoint;
import com.github.michalbednarski.intentslab.runas.RunAsInitReceiver;

public class StartActivity extends Activity {

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
        menu.add("System service (experimental)").setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                BindServiceManager.prepareBinderAndShowUI(StartActivity.this, new SystemServiceDescriptor("package"));
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
    	(new CatchBroadcastDialog(this)).show();
    }

    public void viewSavedItems(View view) {
        startActivity(new Intent(this, SavedItemsActivity.class));
    }
}
