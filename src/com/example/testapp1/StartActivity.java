package com.example.testapp1;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.example.testapp1.browser.BrowseAppsActivity;
import com.example.testapp1.browser.ExtendedPackageInfo;
import com.example.testapp1.editor.IntentEditorActivity;
import com.example.testapp1.providerlab.ProviderLabActivity;

public class StartActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_start, menu);
        menu.add("EXPERIMENTAL").setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                startActivity(new Intent(StartActivity.this, ProviderLabActivity.class));
                return true;
            }
        });
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch (item.getItemId()) {
    	case R.id.menu_settings:
    		startActivity(new Intent(this, PrefsActivity.class));
    		break;
    	}
    	return super.onOptionsItemSelected(item);
    }

    // Button actions
    public void startNew(View view) {
    	startActivity(new Intent(this, IntentEditorActivity.class));
    }

    public void pickFromRecents(View view) {
    	startActivity(new Intent(this, PickRecentlyRunningActivity.class));
    }

    public void browseApps(View view) {
    	startActivity(new Intent(this, BrowseAppsActivity.class));
    }

    public void catchBroadcast(View view) {
    	(new CatchBroadcastDialog(this)).show();
    }

    public void startAppScanner(View view) {
    	//startService(new Intent(this, ScanService.class));
    	//new IntentFilterScanner(this, "com.android.phone");
    	new ExtendedPackageInfo(this, "com.example.testapp1");
    }
}
