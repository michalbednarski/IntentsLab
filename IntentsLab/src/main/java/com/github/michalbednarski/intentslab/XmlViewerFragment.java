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

package com.github.michalbednarski.intentslab;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.XmlResourceParser;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import org.xmlpull.v1.XmlPullParser;

import java.io.IOException;
import java.lang.reflect.Method;

public class XmlViewerFragment extends TextFragment {
    public static final String ARG_PACKAGE_NAME = "packageName__Arg";
    public static final String ARG_RESOURCE_ID = "resIdArg";

    private ReserializeXmlTask mTask = null;

    public static XmlViewerFragment create(String packageName, int resourceId) {
        Bundle arguments = new Bundle();
        arguments.putString(ARG_PACKAGE_NAME, packageName);
        arguments.putInt(ARG_RESOURCE_ID, resourceId);
        final XmlViewerFragment fragment = new XmlViewerFragment();
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        mTask = new ReserializeXmlTask();
        mTask.execute();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mTask != null) {
            mTask.cancel(true);
            mTask = null;
        }
    }

    private class ReserializeXmlTask extends AsyncTask<Object, Object, CharSequence[]> {
        private String mPackageName;
        private int mResourceId;

        private XmlPreviewBuilder mXmlPreviewBuilder = null;
        private Context mApplicationContext;

        @Override
        protected void onPreExecute() {
            final Bundle arguments = getArguments();
            mPackageName = arguments.getString(ARG_PACKAGE_NAME);
            mResourceId = arguments.getInt(ARG_RESOURCE_ID);

            mXmlPreviewBuilder = new XmlPreviewBuilder(getActivity());
            mApplicationContext = getActivity().getApplicationContext();
        }


        @Override
        protected CharSequence[] doInBackground(Object... args) {

            XmlResourceParser parser;
            try {
                // Get resource xml parser
                Context scannedAppContext = mApplicationContext.createPackageContext(mPackageName, 0);
                if (mResourceId != 0) {
                    parser = scannedAppContext.getResources().getXml(mResourceId);
                } else {
                    parser = getManifest(mApplicationContext, mPackageName);
                }

                // Parse and reserialize xml
                int token;
                while ((token = parser.next()) != XmlPullParser.END_DOCUMENT) {
                    if (isCancelled()) {
                        return null;
                    }
                    switch (token) {
                        case XmlPullParser.START_TAG:
                            mXmlPreviewBuilder.openTag(parser.getName());

                            // Attributes
                            for (int i = 0, attrCount = parser.getAttributeCount(); i < attrCount; i++) {
                                mXmlPreviewBuilder.attrFromResourceParser(parser, i, scannedAppContext, attrCount == 1);
                            }
                            break;

                        case XmlPullParser.END_TAG:
                            mXmlPreviewBuilder.endTag(parser.getName());
                            break;

                        case XmlPullParser.TEXT:
                            mXmlPreviewBuilder.text(parser.getText());
                            break;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                mXmlPreviewBuilder.showException(e);
            }

            return null;
        }

        @Override
        protected void onPostExecute(CharSequence[] text) {
            mTask = null;
            publishText(mXmlPreviewBuilder.getText());
        }
    }



    private static boolean sThemeManifestBugProbed = false;
    private static boolean sHasThemeManifestBug;

    /**
     * Check if device has theme manifest bug, that is {@link android.content.res.AssetManager#openXmlResourceParser(String)}
     * will return manifest of device theme instead of app
     */
    private static boolean probeManifestThemeBug(Context context) {
        XmlResourceParser parser = null;
        try {
            // Open parser
            parser = context.getAssets().openXmlResourceParser("AndroidManifest.xml");

            // Find first start tag
            int token;
            do {
                token = parser.next();
            } while (token != XmlPullParser.START_TAG && token != XmlPullParser.END_DOCUMENT);

            // Ensure it's <manifest>
            if (!"manifest".equals(parser.getName())) {
                return true; // Not our manifest - Enable workaround
            }

            // Find package attribute
            for (int i = 0, attributeCount = parser.getAttributeCount(); i < attributeCount; i++) {
                if ("package".equals(parser.getAttributeName(i))) {
                    // Check if it's value is expected package name
                    return !context.getPackageName().equals(parser.getAttributeValue(i));
                }
            }
            return true; // Not our manifest - Enable workaround

        } catch (Exception e) {
            Log.w("ManifestThemeBug", "Probing failed");
            e.printStackTrace();
            return false; // Disable workaround, it'd probably also fail
        } finally {
            if (parser != null) {
                parser.close();
            }
        }
    }

    /**
     * Get manifest for given package
     */
    @TargetApi(Build.VERSION_CODES.FROYO) // We can fallback to non-workaround version
    public static XmlResourceParser getManifest(Context context, String packageName) throws IOException, PackageManager.NameNotFoundException {
        // "android" package is special, we will always use workaround for it
        final boolean isSystemPackage = "android".equals(packageName);

        // Check once if bug occurs on device
        if (!sThemeManifestBugProbed && !isSystemPackage) {
            sHasThemeManifestBug = probeManifestThemeBug(context);
            sThemeManifestBugProbed = true;
        }


        final Context packageContext = context.createPackageContext(packageName, 0);
        AssetManager assets = packageContext.getAssets();

        // Workaround bug if it exists
        if (sHasThemeManifestBug || isSystemPackage) {
            try {
                final Method getCookieName = AssetManager.class.getDeclaredMethod("getCookieName", int.class);
                String resourcesPath = packageContext.getPackageResourcePath();
                // Some devices will return from getPackageResourcePath for "android" null
                // or path for calling our own (IntentsLab) package
                if (isSystemPackage && (resourcesPath == null || resourcesPath.contains(context.getPackageName()))) {
                    resourcesPath = "/system/framework/framework-res.apk";
                }
                int cookie = 1;
                for (; cookie < 100; cookie++) { // Should throw exception before reaching value if something goes wrong
                    if (resourcesPath.equals(getCookieName.invoke(assets, cookie))) {
                        return assets.openXmlResourceParser(cookie, "AndroidManifest.xml");
                    }
                }
            } catch (Exception ignored) {
                Log.w("ManifestThemeBug", "Workaround failed");
                // fall through
            }
        }

        // Normal way if device don't have bug or workaround failed
        return assets.openXmlResourceParser("AndroidManifest.xml");
    }
}
