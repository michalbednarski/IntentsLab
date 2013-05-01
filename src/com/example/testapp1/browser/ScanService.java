package com.example.testapp1.browser;

import java.util.List;

import org.xmlpull.v1.XmlPullParser;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.XmlResourceParser;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.IBinder;
import android.util.Log;

class ScanService extends Service {

	private class ScanTask extends AsyncTask<Object, Object, Object> {

		@Override
		protected Object doInBackground(Object... params) {
			// TODO Auto-generated method stub
			PackageManager pm = getPackageManager();
			List<ApplicationInfo> apps = pm.getInstalledApplications(0);
			int scannedApps = 0;
			int appsCount = apps.size();
			SQLiteDatabase db = AppsDB.getInstance(getApplicationContext()).getDatabase(true);
			for (ApplicationInfo app : apps) {
				try {
					String packageName = app.packageName;
					Context scannedAppContext = createPackageContext(packageName, 0);
					XmlResourceParser manifest = scannedAppContext.getAssets().openXmlResourceParser("AndroidManifest.xml");
					int token;
					while ((token = manifest.next()) != XmlPullParser.END_DOCUMENT) {

					}
					// TODO parse
					manifest.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
				scannedApps++;
				publishProgress();
			}
			return null;
		}

		@Override
		protected void onProgressUpdate(Object... values) {
			// TODO Auto-generated method stub
			super.onProgressUpdate(values);
		}

		@Override
		protected void onPostExecute(Object result) {
			sScanTask = null;
			stopSelf();
		}
	}

	private static ScanTask sScanTask = null;

	public boolean isRunning() {
		return sScanTask != null;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// TODO Auto-generated method stub
		if (sScanTask != null) {
			sScanTask = new ScanTask();
			sScanTask.execute();
		} else {
			Log.e("AppScanner", "Already running");
		}
		return START_NOT_STICKY;
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

}
