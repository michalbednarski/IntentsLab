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

package com.github.michalbednarski.intentslab.browser;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.ComponentInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.os.AsyncTask;
import android.os.Looper;
import android.os.PatternMatcher;
import android.support.v4.util.ArrayMap;
import android.util.Log;

import com.github.michalbednarski.intentslab.BuildConfig;
import com.github.michalbednarski.intentslab.XmlViewerFragment;
import com.github.michalbednarski.intentslab.editor.IntentEditorConstants;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ExtendedPackageInfo {
    private static final String TAG = "IntentFilterScanner";
    public final String packageName;

    private ArrayList<Callback> mRunWhenReadyList = new ArrayList<Callback>(1);
    private ArrayMap<String, ExtendedComponentInfo> mComponents = new ArrayMap<String, ExtendedComponentInfo>();


    public static class ExtendedComponentInfo {
        public int componentType;
        public IntentFilter intentFilters[];
        public ComponentInfo systemComponentInfo;

        public String getPermission() {
            return componentType == IntentEditorConstants.SERVICE ?
                    ((ServiceInfo) systemComponentInfo).permission :
                    ((ActivityInfo) systemComponentInfo).permission;
        }
    }

    public ExtendedComponentInfo getComponentInfo(String componentName) {
        return mComponents.get(componentName);
    }

    private class ScanManifestTask extends AsyncTask<Object, Object, Object> {

        private static final String ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android";

        private Context mContext;
        private PackageInfo mPackageInfo;

        private ScanManifestTask(Context context) {
            mContext = context.getApplicationContext();
        }

        @Override
        protected Object doInBackground(Object... params) {
            try {
                PackageManager pm = mContext.getPackageManager();
                mPackageInfo = pm.getPackageInfo(packageName,
                        PackageManager.GET_ACTIVITIES |
                                PackageManager.GET_RECEIVERS |
                                PackageManager.GET_SERVICES |
                                PackageManager.GET_DISABLED_COMPONENTS |
                                PackageManager.GET_META_DATA);
                XmlPullParser manifest = XmlViewerFragment.getManifest(mContext, packageName);
                preScanPackageComponents(IntentEditorConstants.ACTIVITY, mPackageInfo.activities);
                preScanPackageComponents(IntentEditorConstants.BROADCAST, mPackageInfo.receivers);
                preScanPackageComponents(IntentEditorConstants.SERVICE, mPackageInfo.services);
                parseManifest(manifest);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
            return null;
        }

        private void preScanPackageComponents(int type, ComponentInfo[] componentsArray) {
            if (componentsArray != null) {
                for (ComponentInfo c : componentsArray) {
                    ExtendedComponentInfo x = new ExtendedComponentInfo();
                    x.componentType = type;
                    x.systemComponentInfo = c;
                    mComponents.put(c.name, x);
                }
            }
        }

        private ExtendedComponentInfo getComponentByNameFromManifest(/*PackageInfo app, */String componentName, int expectedComponentType) {
            if (componentName.charAt(0) == '.') {
                componentName = packageName + componentName;
            } else if (!componentName.contains(".")) {
                // TODO: is this documented? Some system apps rely on this
                componentName = packageName + "." + componentName;
            }

            ExtendedComponentInfo component = mComponents.get(componentName);
            if (component == null || component.componentType != expectedComponentType) {
                return null;
            }
            return component;
        }

        private void parseManifest(XmlPullParser manifest)
                throws IOException, XmlPullParserException {

            int token;
            while ((token = manifest.next()) != XmlPullParser.END_DOCUMENT) {
                switch (token) {
                    case XmlPullParser.START_TAG: {
                        String tagName = manifest.getName();
                        if (tagName.equals("activity") ||
                                tagName.equals("activity-alias")) {
                            parseComponent(manifest, IntentEditorConstants.ACTIVITY);
                        } else if (tagName.equals("receiver")) {
                            parseComponent(manifest, IntentEditorConstants.BROADCAST);
                        } else if (tagName.equals("service")) {
                            parseComponent(manifest, IntentEditorConstants.SERVICE);
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
         */
        private void parseComponent(XmlPullParser manifest, int componentType) throws XmlPullParserException, IOException {
            ExtendedComponentInfo component = getComponentByNameFromManifest(manifest.getAttributeValue(ANDROID_NAMESPACE, "name"), componentType);
            if (component == null) {
                return;
            }
            ArrayList<IntentFilter> filters = new ArrayList<IntentFilter>();
            IntentFilter currentFilter = null;
            int baseDepth = manifest.getDepth();
            int depth; // 1 = in component, 2 = in intent-filter
            int token;

            // Read XML tree
            while ((token = manifest.next()) != XmlPullParser.END_DOCUMENT &&
                    (depth = manifest.getDepth() - baseDepth) != 0) {
                if (token == XmlPullParser.START_TAG) {
                    String tagName = manifest.getName();
                    if (depth == 1) {
                        if (tagName.equals("intent-filter")) {
                            currentFilter = new IntentFilter();
                            filters.add(currentFilter);
                        } else {
                            skipTree(manifest);
                        }
                    } else if (depth == 2) {
                        // Inside <intent-filter>

                        // NOTE: there's IntentFilter#readFromXML, but it reads different
                        // format than this in AndroidManifest.xml
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

                            // <data android:pathPattern="">
                            String mimeType = manifest.getAttributeValue(ANDROID_NAMESPACE, "mimeType");
                            if (mimeType != null) {
                                try {
                                    currentFilter.addDataType(mimeType);
                                } catch (IntentFilter.MalformedMimeTypeException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }

                }
            }

            component.intentFilters = filters.toArray(new IntentFilter[filters.size()]);
        }

        private void skipTree(XmlPullParser parser) throws XmlPullParserException, IOException {
            int token;
            int baseDepth = parser.getDepth();
            while ((token = parser.next()) != XmlPullParser.END_DOCUMENT) {
                if (token == XmlPullParser.END_TAG && parser.getDepth() == baseDepth) {
                    break;
                }
            }
        }

        @Override
        protected void onPostExecute(Object result) {
            ArrayList<Callback> runWhenReadyList = mRunWhenReadyList;
            mRunWhenReadyList = null;
            for (Callback run : runWhenReadyList) {
                run.onPackageInfoAvailable(ExtendedPackageInfo.this);
            }
        }
    }

    public ExtendedComponentInfo[] getComponentsByType(int type) {
        ArrayList<ExtendedComponentInfo> matchingComponents = new ArrayList<ExtendedComponentInfo>();
        for (ExtendedComponentInfo componentInfo : mComponents.values()) {
            if (componentInfo.componentType == type) {
                matchingComponents.add(componentInfo);
            }
        }
        return matchingComponents.toArray(new ExtendedComponentInfo[matchingComponents.size()]);
    }

    private boolean isReady() {
        return mRunWhenReadyList == null;
    }

    private ExtendedPackageInfo(String packageName) {
        this.packageName = packageName;
    }

    /*
      Getting the instance for package
    */


    private static final ArrayMap<String, ExtendedPackageInfo> sPackageCache = new ArrayMap<String, ExtendedPackageInfo>();

    public interface Callback {
        void onPackageInfoAvailable(ExtendedPackageInfo extendedPackageInfo);
    }



    public static void getExtendedPackageInfo(Context context, String packageName, final Callback callback) {
        // Ensure this is called from main thread
        if (BuildConfig.DEBUG && Looper.myLooper() != Looper.getMainLooper()) {
            throw new AssertionError("getExtendedPackageInfo called off main thread");
        }

        // Prepare cache purging
        PurgeCacheReceiver.registerIfNeeded(context);

        // Get from cache
        ExtendedPackageInfo info;
        info = sPackageCache.get(packageName);

        // Create new if not ready
        boolean createNew = info == null;
        if (createNew) {
            info = new ExtendedPackageInfo(packageName);
            sPackageCache.put(packageName, info);
        }

        // Invoke or schedule callback
        if (info.isReady()) {
            // Info is ready, invoke callback immediately
            callback.onPackageInfoAvailable(info);
        } else {
            // Schedule our callback to be invoked when scan is ready
            info.mRunWhenReadyList.add(callback);

            // If we just created object, initialize it's scan
            if (createNew) {
                (info.new ScanManifestTask(context)).execute();
            }
        }
    }


    /*
      Loading of all package infos at once
    */
    private static ExtendedPackageInfo[] sAllPackageInfos = null;

    public interface AllCallback {
        void onAllPackagesInfosAvailable(ExtendedPackageInfo[] infos);
    }

    public static void getAllPackageInfos(Context context, final AllCallback callback) {
        // Ensure this is called from main thread
        if (BuildConfig.DEBUG && Looper.myLooper() != Looper.getMainLooper()) {
            throw new AssertionError("getExtendedPackageInfo called off main thread");
        }

        // Check for cached value
        if (sAllPackageInfos != null) {
            callback.onAllPackagesInfosAvailable(sAllPackageInfos);
            return;
        }

        // A closure...
        class L {
            int packagesToLoadLeft;
        }
        final L state = new L();

        // Get list of installed packages
        List<PackageInfo> installedPackages = context.getPackageManager().getInstalledPackages(0);
        state.packagesToLoadLeft = installedPackages.size();

        // Prepare result array
        final ExtendedPackageInfo[] allInfos = new ExtendedPackageInfo[installedPackages.size()];

        // Scan all packages
        int index = 0;
        for (PackageInfo installedPackage : installedPackages) {
            final int ii = index++;
            getExtendedPackageInfo(context, installedPackage.packageName, new Callback() {
                @Override
                public void onPackageInfoAvailable(ExtendedPackageInfo extendedPackageInfo) {
                    // Fill in result array
                    allInfos[ii] = extendedPackageInfo;

                    // If all package infos are ready
                    if (--state.packagesToLoadLeft == 0) {
                        // Save to cache
                        sAllPackageInfos = allInfos;

                        // Invoke callback
                        callback.onAllPackagesInfosAvailable(allInfos);
                    }
                }
            });
        }
    }

    /**
     * Receiver used to remove packages from cache when they are changed
     */
    private static class PurgeCacheReceiver extends BroadcastReceiver {

        private static boolean sRegistered = false;

        @Override
        public void onReceive(Context context, Intent intent) {
            sAllPackageInfos = null;
            sPackageCache.remove(intent.getData().getSchemeSpecificPart());
        }

        static void registerIfNeeded(Context context) {
            if (!sRegistered) {
                sRegistered = true;
                IntentFilter filter = new IntentFilter(Intent.ACTION_PACKAGE_ADDED);
                filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
                filter.addDataScheme("package");
                context.getApplicationContext().registerReceiver(new PurgeCacheReceiver(), filter);
            }
        }
    }
}
