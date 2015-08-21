package com.github.michalbednarski.intentslab.appinfo;

import android.content.Context;
import android.content.IntentFilter;
import android.os.PatternMatcher;
import android.util.Log;

import com.github.michalbednarski.intentslab.XmlViewerFragment;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

/**
 * Helper class for loading intent filters for package from it's manifest,
 * as they're not provided by system api
 */
class ScanManifestTask {
    private static final String TAG = "ScanManifestTask2";

    private static final String ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android";

    static void parseInstalledPackage(Context context, MyPackageInfoImpl packageInfo) {
        try {
            XmlPullParser manifest = XmlViewerFragment.getManifest(context, packageInfo.mPackageName);
            parseManifest(manifest, packageInfo);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // We set this flag even if we fail so we're not trying to load them again
        packageInfo.mIntentFiltersLoaded = true;
    }

    private static void parseManifest(XmlPullParser manifest, MyPackageInfoImpl mPackageInfo)
            throws IOException, XmlPullParserException {

        int token;
        while ((token = manifest.next()) != XmlPullParser.END_DOCUMENT) {
            switch (token) {
                case XmlPullParser.START_TAG: {
                    String tagName = manifest.getName();
                    if (tagName.equals("activity") ||
                            tagName.equals("activity-alias")) {
                        parseComponent(manifest, mPackageInfo.mPackageName, mPackageInfo.mActivities);
                    } else if (tagName.equals("receiver")) {
                        parseComponent(manifest, mPackageInfo.mPackageName, mPackageInfo.mReceivers);
                    } else if (tagName.equals("service")) {
                        parseComponent(manifest, mPackageInfo.mPackageName, mPackageInfo.mServices);
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
    private static void parseComponent(XmlPullParser manifest, String packageName, Map<String, MyComponentInfoImpl> componentMap) throws XmlPullParserException, IOException {
        // Get MyComponentInfoImpl to fill in
        String componentName = manifest.getAttributeValue(ANDROID_NAMESPACE, "name");
        MyComponentInfoImpl component = getComponentByNameFromManifest(packageName, componentName, componentMap);
        if (component == null) {
            return;
        }

        // Prepare for parsing
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

        component.mIntentFilters = filters.toArray(new IntentFilter[filters.size()]);
    }

    private static MyComponentInfoImpl getComponentByNameFromManifest(String packageName, String componentName, Map<String, MyComponentInfoImpl> componentMap) {
        if (componentName.charAt(0) == '.') {
            componentName = packageName + componentName;
        } else if (!componentName.contains(".")) {
            // TODO: is this documented? Some system apps rely on this
            componentName = packageName + "." + componentName;
        }

        return componentMap.get(componentName);
    }

    private static void skipTree(XmlPullParser parser) throws XmlPullParserException, IOException {
        int token;
        int baseDepth = parser.getDepth();
        while ((token = parser.next()) != XmlPullParser.END_DOCUMENT) {
            if (token == XmlPullParser.END_TAG && parser.getDepth() == baseDepth) {
                break;
            }
        }
    }
}
