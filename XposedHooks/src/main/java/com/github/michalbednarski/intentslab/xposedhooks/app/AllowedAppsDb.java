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

package com.github.michalbednarski.intentslab.xposedhooks.app;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.util.JsonReader;
import android.util.JsonWriter;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

/**
 * Class maintaining list of allowed apps with uids and signatures
 */
class AllowedAppsDb {
    private static AllowedAppsDb sInstance = null;
    private final File mDbFile;
    private final PackageManager mPm;
    private final ArrayList<AppRecord> mAllowedApps = new ArrayList<AppRecord>();
    private boolean mChanged = false;

    private static class AppRecord {
        String packageName;
        int uid;
        Signature[] signatures;

        AppRecord(JsonReader reader) throws IOException {
            reader.beginObject();
            while (reader.hasNext()) {
                String name = reader.nextName();
                if ("packageName".equals(name)) {
                    packageName = reader.nextString();
                } else if ("uid".equals(name)) {
                    uid = reader.nextInt();
                } else if ("signatures".equals(name)) {
                    reader.beginArray();
                    ArrayList<Signature> signatureList = new ArrayList<Signature>();
                    while (reader.hasNext()) {
                        signatureList.add(new Signature(reader.nextString()));
                    }
                    signatures = signatureList.toArray(new Signature[signatureList.size()]);
                    reader.endArray();
                }
            }
            reader.endObject();
        }

        boolean isValid(PackageManager pm) {
            try {
                // Allow adb shell if remembered without further checks
                if (uid == 2000) {
                    return true;
                }

                // Get package info
                PackageInfo packageInfo = pm.getPackageInfo(packageName, PackageManager.GET_SIGNATURES);

                // Check uid
                if (packageInfo.applicationInfo.uid != uid || uid == 0) {
                    return false;
                }

                // Check signatures
                if (signatures == null || signatures.length == 0) {
                    return false;
                }
                List<Signature> packageSignatureList = Arrays.asList(packageInfo.signatures);
                if (!packageSignatureList.containsAll(Arrays.asList(signatures))) {
                    return false;
                }

                // Okay
                return true;
            } catch (Exception e) {
                return false;
            }
        }

        void writeToJson(JsonWriter writer) throws IOException {
            writer.beginObject();
            writer.name("packageName");
            writer.value(packageName);
            writer.name("uid");
            writer.value(uid);
            writer.name("signatures");
            writer.beginArray();
            for (Signature signature : signatures) {
                writer.value(signature.toCharsString());
            }
            writer.endArray();
            writer.endObject();
        }

        AppRecord(PackageInfo packageInfo) {
            packageName = packageInfo.packageName;
            uid = packageInfo.applicationInfo.uid;
            signatures = packageInfo.signatures;
        }
    }

    /**
     * Get instance of this class, at first call will load list of allowed apps
     */
    static AllowedAppsDb getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new AllowedAppsDb(
                    context.getApplicationContext()
            );
        }
        return sInstance;
    }

    private AllowedAppsDb(Context context) {
        mDbFile = context.getFileStreamPath("allowed-apps.json");
        mPm = context.getPackageManager();

        // Load allowed apps
        JsonReader reader = null;
        try {
            HashSet<Integer> uids = new HashSet<Integer>();
            reader = new JsonReader(new FileReader(mDbFile));
            reader.beginArray();
            while (reader.hasNext()) {
                AppRecord appRecord = new AppRecord(reader);
                if (appRecord.isValid(mPm) && !uids.contains(appRecord.uid)) {
                    mAllowedApps.add(appRecord);
                    uids.add(appRecord.uid);
                } else {
                    mChanged = true;
                }
            }
            reader.endArray();
        } catch (Exception e) {
            mAllowedApps.clear();
            e.printStackTrace();
        } finally {
            try {
                reader.close();
            } catch (Exception ignored) {}
        }
    }

    /**
     * Clean db removing invalid packages and duplicate uids
     */
    private void cleanDb() {
        HashSet<Integer> uids = new HashSet<Integer>();
        Iterator<AppRecord> iterator = mAllowedApps.iterator();
        while (iterator.hasNext()) {
            AppRecord appRecord = iterator.next();
            if (!appRecord.isValid(mPm) || uids.contains(appRecord.uid)) {
                iterator.remove();
                mChanged = true;
            } else {
                uids.add(appRecord.uid);
            }
        }
    }

    void allowApp(PackageInfo packageInfo) {
        mAllowedApps.add(new AppRecord(packageInfo));
        mChanged = true;
    }

    /**
     * Get array of allowed uids, returned array will be sorted and won't contain duplicates
     */
    int[] getAllowedUids() {
        cleanDb();
        int[] allowedUids = new int[mAllowedApps.size()];
        int index = 0;
        for (AppRecord allowedApp : mAllowedApps) {
            allowedUids[index++] = allowedApp.uid;
        }
        Arrays.sort(allowedUids);
        return allowedUids;
    }

    void saveIfNeeded() {
        cleanDb();
        if (mChanged) {
            JsonWriter writer = null;
            try {
                writer = new JsonWriter(new FileWriter(mDbFile));
                writer.beginArray();
                for (AppRecord allowedApp : mAllowedApps) {
                    allowedApp.writeToJson(writer);
                }
                writer.endArray();
                mChanged = false;
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    writer.close();
                } catch (Exception ignored) {}
            }

        }
    }
}
