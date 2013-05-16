package com.example.testapp1.browser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.Context;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.ComponentInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.os.AsyncTask;
import android.os.PatternMatcher;
import android.util.Log;

import com.example.testapp1.editor.IntentEditorConstants;

public class ExtendedPackageInfo {
	private static final String TAG = "IntentFilterScanner";
	private String mPackageName;
	private Context mContext;
	private ArrayList<Runnable> mRunWhenReadyList = new ArrayList<Runnable>(1);
	private HashMap<String, ExtendedComponentInfo> mComponents = new HashMap<String, ExtendedComponentInfo>();
	private PackageInfo mPackageInfo = null;

	public static class ExtendedComponentInfo {
		int componentType;
		IntentFilter intentFilters[];
		ComponentInfo systemComponentInfo;

		String getPermission() {
			return componentType == IntentEditorConstants.SERVICE ?
					((ServiceInfo)systemComponentInfo).permission :
					((ActivityInfo)systemComponentInfo).permission;
		}
	}

	public ExtendedComponentInfo getComponentInfo(String componentName) {
		return mComponents.get(componentName);
	}

	private class ScanManifestTask extends AsyncTask<Object, Object, Object> {

		private static final String ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android";

		@Override
		protected Object doInBackground(Object... params) {
			try {
				PackageManager pm = mContext.getPackageManager();
				if (mPackageInfo == null) {
					mPackageInfo = pm.getPackageInfo(mPackageName,
							PackageManager.GET_ACTIVITIES |
							PackageManager.GET_RECEIVERS |
							PackageManager.GET_SERVICES |
							PackageManager.GET_DISABLED_COMPONENTS);
				}
				XmlPullParser manifest =
						mContext
						.createPackageContext(mPackageName, 0)
						.getAssets()
						.openXmlResourceParser("AndroidManifest.xml");
				parseManifest(manifest, mPackageInfo);
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
			return null;
		}

		private ComponentInfo getComponentByName(/*PackageInfo app, */String componentName, ComponentInfo components[]) {
			if (componentName.charAt(0) == '.') {
				componentName = mPackageName + componentName;
			} else if (!componentName.contains(".")) {
				// TODO: is this documented? Some system apps rely on this
				Log.w(TAG, "Auto-extending android:name attribute using undocumented no-dot syntax: " + mPackageName + "/" + componentName);
				componentName = mPackageName + "." + componentName;
			}
			for (ComponentInfo component : components) {
				if (component.name.equals(componentName)) {
					return component;
				}
			}
			return null;
		}

		private void parseManifest(XmlPullParser manifest, PackageInfo app)
				throws IOException, XmlPullParserException {

			int token;
			while ((token = manifest.next()) != XmlPullParser.END_DOCUMENT) {
				switch (token) {
				case XmlPullParser.START_TAG:
					{
						String tagName = manifest.getName();
						if (tagName.equals("activity") ||
							tagName.equals("activity-alias")) {
							parseComponent(manifest, IntentEditorConstants.ACTIVITY, app.activities);
						} else if (tagName.equals("receiver")) {
							parseComponent(manifest, IntentEditorConstants.BROADCAST, app.receivers);
						} else if(tagName.equals("service")) {
							parseComponent(manifest, IntentEditorConstants.SERVICE, app.services);
						}
					}
					break;
				case XmlPullParser.END_TAG:
					break;
				}
			}

		}

		/*
		 * Parse contents of <activity|activity-alias|broadcast|service> tag
		 * Extracts intent filters and builds ExtendedComponentInfo
		 * */
		private void parseComponent(XmlPullParser manifest, int componentType, ComponentInfo componentsOfType[]) throws XmlPullParserException, IOException {
			ComponentInfo component = getComponentByName(manifest.getAttributeValue(ANDROID_NAMESPACE, "name"), componentsOfType);
			if (component == null) {
				return;
			}
			ArrayList<IntentFilter> filters = new ArrayList<IntentFilter>();
			IntentFilter currentFilter = null;
			int depth = 1; // 1 = in component, 2 = in intent-filter
			int token;
			while (depth != 0 && (token = manifest.next()) != XmlPullParser.END_DOCUMENT) {
				switch (token) {
				case XmlPullParser.START_TAG:
					{
						String tagName = manifest.getName();
						if (depth == 1) {
							if (tagName.equals("intent-filter")) {
								currentFilter = new IntentFilter();
								filters.add(currentFilter);
							} else {
								skipTree(manifest);
								break; // Before depth++;
							}
						} else if (depth == 2) {
							// Inside <intent-filter>

							// NOTE: there's IntentFilter#readFromXML, but it reads different form than those in AndroidManifest.xml
							if (tagName.equals("action")) {
								// <action android:name="">
								String action = manifest.getAttributeValue(ANDROID_NAMESPACE, "name");
								if (action != null) {
									currentFilter.addAction(action);
								} else {
									Log.w(TAG, "No action[name]");
								}
							} else if (tagName.equals("category")) {
								// <category android:name="">
								String category = manifest.getAttributeValue(ANDROID_NAMESPACE, "name");
								if (category != null) {
									currentFilter.addCategory(category);
								} else {
									Log.w(TAG, "No category[name]");
								}
							} else if (tagName.equals("data")) { // <data>
								// <data android:scheme="">
								String scheme = manifest.getAttributeValue(ANDROID_NAMESPACE, "scheme");
								if (scheme != null) {
									currentFilter.addDataScheme(scheme);
								}

								// <data android:host="" android:port="">
								String host = manifest.getAttributeValue(ANDROID_NAMESPACE, "host");
								String port = manifest.getAttributeValue(ANDROID_NAMESPACE, "port");
								if (host != null) {
									currentFilter.addDataAuthority(host, port);
								}

								// <data android:path="">
								String path = manifest.getAttributeValue(ANDROID_NAMESPACE, "path");
								if (path != null) {
									currentFilter.addDataPath(path, PatternMatcher.PATTERN_LITERAL);
								}

								// <data android:pathPrefix="">
								path = manifest.getAttributeValue(ANDROID_NAMESPACE, "pathPrefix");
								if (path != null) {
									currentFilter.addDataPath(path, PatternMatcher.PATTERN_PREFIX);
								}

								// <data android:pathPattern="">
								path = manifest.getAttributeValue(ANDROID_NAMESPACE, "pathPattern");
								if (path != null) {
									currentFilter.addDataPath(path, PatternMatcher.PATTERN_SIMPLE_GLOB);
								}
							}
						}
					}
					depth++;
					break;
				case XmlPullParser.END_TAG:
					depth--;
					break;
				}
			}

			ExtendedComponentInfo xComponent = new ExtendedComponentInfo();
			xComponent.componentType = componentType;
			xComponent.intentFilters = filters.toArray(new IntentFilter[filters.size()]);
			xComponent.systemComponentInfo = component;
			mComponents.put(component.name, xComponent);
		}

		private void skipTree(XmlPullParser parser) throws XmlPullParserException, IOException {
			int depth = 1;
			int token;
			while (depth != 0 && (token = parser.next()) != XmlPullParser.END_DOCUMENT) {
				switch (token) {
				case XmlPullParser.START_TAG:
					depth++;
					break;
				case XmlPullParser.END_TAG:
					depth--;
					break;
				}
			}
		}

		@Override
		protected void onPostExecute(Object result) {
			for (Runnable run : mRunWhenReadyList) {
				run.run();
			}
			mRunWhenReadyList = null;
		}
	}

	public void runWhenReady(Runnable run) {
		if (mRunWhenReadyList == null) {
			run.run();
		} else {
			mRunWhenReadyList.add(run);
		}
	}

	public ExtendedPackageInfo(Context context, PackageInfo basePackageInfo) {
		mContext = context;
		mPackageName = basePackageInfo.packageName;
		mPackageInfo = basePackageInfo;
		(new ScanManifestTask()).execute();
	}

	public ExtendedPackageInfo(Context context, String packageName) {
		mContext = context;
		mPackageName = packageName;
		(new ScanManifestTask()).execute();
	}
}
