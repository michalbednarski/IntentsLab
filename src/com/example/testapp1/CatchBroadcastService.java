package com.example.testapp1;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.preference.PreferenceManager;

import com.example.testapp1.editor.IntentEditorActivity;

public class CatchBroadcastService extends Service {
	static boolean sIsRunning = false;
	private CatchBroadcastReceiver mReceiver = null;
	private boolean mGotBroadcast = false;

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		// Get action
		String action = intent.getStringExtra("action");
		if (action == null) {
			stopSelf();
			return START_NOT_STICKY;
		}

		// Flag us as running
		sIsRunning = true;

		// Setup receiver
		if (mReceiver != null) {
			// We were already started, clear old receiver
			unregisterReceiver(mReceiver);
		} else {
			mReceiver = new CatchBroadcastReceiver();
		}
		IntentFilter filter = new IntentFilter();
		filter.addAction(action);
		registerReceiver(mReceiver, filter);

		// Show notification
		showWaitingNotification(action);
		return START_NOT_STICKY;
	}
	
	private boolean isAutoEditEnabled() {
		return PreferenceManager.getDefaultSharedPreferences(this).getBoolean("autoeditbroadcast", false);
	}

	private NotificationManager getNotificationManager() {
		return (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
	}

	void showWaitingNotification(String action) {
		mGotBroadcast = false;

		Intent requeryAction = new Intent(this, CatchBroadcastActivity.class);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
				requeryAction, 0);

		String title = getResources().getString(R.string.waiting_for_broadcast);

		Notification notif = new Notification();
		notif.icon = R.drawable.ic_action_send;
		notif.flags = Notification.FLAG_ONGOING_EVENT;
		notif.tickerText = title;
		notif.setLatestEventInfo(this, title, action, contentIntent);
		getNotificationManager().notify(1, notif);
	}

	void showCaughtNotification(Intent receivedBroadcast, boolean isSticky) {
		mGotBroadcast = true;

		Intent runEditor = new Intent(this, IntentEditorActivity.class);
		runEditor.putExtra("intent", receivedBroadcast);
		runEditor.putExtra(IntentEditorActivity.EXTRA_DISPOSITION, IntentEditorActivity.DISPOSITION_BROADCAST);
		runEditor.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
					runEditor, 0);
		
		if (isSticky || isAutoEditEnabled()) {
			removeNotification();
			startActivity(runEditor);
		} else {
			String title = getResources().getString(R.string.got_broadcast);
	
			Notification notif = new Notification();
			notif.icon = R.drawable.ic_action_send;
			notif.flags = Notification.FLAG_AUTO_CANCEL;
			notif.tickerText = title;
			notif.setLatestEventInfo(this, title, receivedBroadcast.getAction(),
					contentIntent);
	
			getNotificationManager().notify(1, notif);
		}
	}

	void removeNotification() {
		getNotificationManager().cancel(1);
	}

	@Override
	public void onDestroy() {
		sIsRunning = false;
		unregisterReceiver(mReceiver);
		if (!mGotBroadcast) {
			removeNotification();
		}
		super.onDestroy();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	private class CatchBroadcastReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			showCaughtNotification(intent, isInitialStickyBroadcast());
			stopSelf(); // Stop my service, unregister receiver
		}
	}
}
