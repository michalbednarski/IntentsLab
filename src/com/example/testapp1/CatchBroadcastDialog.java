package com.example.testapp1;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;


public class CatchBroadcastDialog implements OnClickListener {

	Context mContext;
	TextView mActionTextView;

	CatchBroadcastDialog(Context context) {
		mContext = context;
	}

	void show() {
		AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
		View view = ((LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.dialog_catch_broadcast, null);
		mActionTextView = (TextView)view.findViewById(R.id.action);
		mActionTextView.setText(PreferenceManager
				.getDefaultSharedPreferences(mContext)
				.getString("lastcatchbroadcastaction", "")
				);
		builder.setView(view);
		builder.setTitle(R.string.title_activity_catch_broadcast);
		builder.setPositiveButton(R.string.register_receiver, this);
		if (CatchBroadcastService.sIsRunning) {
			builder.setNegativeButton(R.string.unregister_receiver, this);
		}
		builder.show();
	}

	@Override
	public void onClick(DialogInterface dialog, int which) {
		if (which == AlertDialog.BUTTON_NEGATIVE) {
			stopCatcher();
		} else {
			startCatcher();
		}
	}

	void startCatcher() {
		String action = mActionTextView.getText().toString();
		Utils.applyOrCommitPrefs(
			PreferenceManager
			.getDefaultSharedPreferences(mContext)
			.edit()
			.putString("lastcatchbroadcastaction", action)
			);
		Intent intent = new Intent(mContext, CatchBroadcastService.class);
		intent.putExtra("action", action);
		mContext.startService(intent);
	}

	void stopCatcher() {
		mContext.stopService(new Intent(mContext, CatchBroadcastService.class));
	}
}
