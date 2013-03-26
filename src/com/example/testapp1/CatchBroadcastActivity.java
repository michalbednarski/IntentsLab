package com.example.testapp1;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.View;
import android.widget.TextView;

public class CatchBroadcastActivity extends Activity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_catch_broadcast);
		if (!CatchBroadcastService.sIsRunning) {
			findViewById(R.id.unregister).setVisibility(View.GONE);
		}
		((TextView)findViewById(R.id.action)).setText(PreferenceManager
			.getDefaultSharedPreferences(this)
			.getString("lastcatchbroadcastaction", "")
			);
	}

	public void startCatcher(View view) {
		String action = ((TextView)findViewById(R.id.action)).getText().toString();
		PreferenceManager
			.getDefaultSharedPreferences(this)
			.edit()
			.putString("lastcatchbroadcastaction", action)
			.apply();
		Intent intent = new Intent(this, CatchBroadcastService.class);
		intent.putExtra("action", action);
		startService(intent);
		finish();
	}
	
	public void stopCatcher(View view) {
		stopService(new Intent(this, CatchBroadcastService.class));
		finish();
	}
}
