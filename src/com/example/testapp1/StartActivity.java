package com.example.testapp1;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.example.testapp1.editor.IntentEditorActivity;

public class StartActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_start, menu);
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
    
    public void startNew(View view) {
    	startActivity(new Intent(this, IntentEditorActivity.class));
    }
    
    public void pickFromRecents(View view) {
    	startActivity(new Intent(this, PickRecentlyRunningActivity.class));
    }
    
    // TODO: browse apps
    
    public void catchBroadcast(View view) {
    	startActivity(new Intent(this, CatchBroadcastActivity.class));
    }
}
