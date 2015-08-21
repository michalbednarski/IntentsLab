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

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Parcel;
import android.view.Menu;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import com.github.michalbednarski.intentslab.R;
import com.github.michalbednarski.intentslab.appinfo.MyPackageInfo;
import com.github.michalbednarski.intentslab.appinfo.MyPackageManagerImpl;

import org.jdeferred.DoneFilter;
import org.jdeferred.Promise;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Fetcher for applications
 */
public class ApplicationFetcher extends Fetcher {
    private static final String TAG = "ApplicationFetcher";



    public static final int APP_TYPE_USER = 1;
    public static final int APP_TYPE_SYSTEM = 2;
    public int appType = APP_TYPE_USER;


    public String requireMetaDataSubstring = null;


    public ApplicationFetcher() {}

    // Fetching
    @Override
    Promise<Object, Throwable, Void> getEntriesAsync(final Context context) {
        return MyPackageManagerImpl
                .getInstance(context)
                .getPackages(false)
                .then(new DoneFilter<Collection<MyPackageInfo>, Object>() {
                    @Override
                    public Object filterDone(Collection<MyPackageInfo> result) {
                        PackageManager pm = context.getPackageManager();

                        ArrayList<Component> selectedApps = new ArrayList<>();

                        for (MyPackageInfo pack : result) {
                            // System app filter
                            if ((
                                    (pack.isSystemApplication() ?
                                            APP_TYPE_SYSTEM :
                                            APP_TYPE_USER)
                                    & appType) == 0) {
                                continue;
                            }

                            // Metadata filter
                            if (!checkMetaDataFilter(pack.getMetaData())) {
                                continue;
                            }

                            // Build and add app descriptor
                            Component app = new Component();
                            app.title = String.valueOf(pack.loadLabel(pm));
                            app.subtitle = pack.getPackageName();
                            app.componentInfo = pack;
                            selectedApps.add(app);
                        }
                        return selectedApps.toArray(new Component[selectedApps.size()]);
                    }
                });
    }

    private boolean checkMetaDataFilter(Bundle metaData) {
        if (requireMetaDataSubstring == null) {
            return true;
        }
        if (metaData == null || metaData.isEmpty()) {
            return false;
        }
        if (requireMetaDataSubstring.length() == 0) {
            return true;
        }
        for (String key : metaData.keySet()) {
            if (key.contains(requireMetaDataSubstring)) {
                return true;
            }
        }
        return false;
    }


    // Configuration UI
    @Override
    int getConfigurationLayout() {
        return R.layout.apps_filter;
    }

    @Override
    void initConfigurationForm(final FetcherOptionsDialog dialog) {
        // Fill form
        dialog.setBoxChecked(R.id.system_apps, (appType & APP_TYPE_SYSTEM) != 0);
        dialog.setBoxChecked(R.id.user_apps, (appType & APP_TYPE_USER) != 0);

        dialog.setBoxChecked(R.id.metadata, requireMetaDataSubstring != null);
        dialog.setTextInField(R.id.metadata_substring, requireMetaDataSubstring);

        // Hiding of metadata section
        dialog.findView(R.id.metadata_details).setVisibility(requireMetaDataSubstring != null ? View.VISIBLE : View.GONE);
        ((CheckBox) dialog.findView(R.id.metadata)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                dialog.findView(R.id.metadata_details).setVisibility(isChecked ? View.VISIBLE : View.GONE);
                if (!isChecked) {
                    dialog.setTextInField(R.id.metadata_substring, "");
                }
            }
        });
    }

    @Override
    void updateFromConfigurationForm(FetcherOptionsDialog dialog) {
        appType =
                (dialog.isBoxChecked(R.id.system_apps) ? APP_TYPE_SYSTEM : 0) |
                (dialog.isBoxChecked(R.id.user_apps) ? APP_TYPE_USER : 0);

        boolean requireMetaData = dialog.isBoxChecked(R.id.metadata);
        requireMetaDataSubstring =
                requireMetaData ?
                dialog.getTextFromField(R.id.metadata_substring) :
                null;
    }



    // Verification
    @Override
    boolean isExcludingEverything() {
        return
                appType == 0;
    }

    //
    // Parcelable
    //
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(appType);
        dest.writeString(requireMetaDataSubstring);
    }

    public static final Creator<ApplicationFetcher> CREATOR = new Creator<ApplicationFetcher>() {
        @Override
        public ApplicationFetcher createFromParcel(Parcel source) {
            ApplicationFetcher fetcher = new ApplicationFetcher();
            fetcher.appType = source.readInt();
            fetcher.requireMetaDataSubstring = source.readString();
            return fetcher;
        }

        @Override
        public ApplicationFetcher[] newArray(int size) {
            return new ApplicationFetcher[size];
        }
    };

    // Options menu
    @Override
    void onPrepareOptionsMenu(Menu menu) {
        if (appType == APP_TYPE_USER) {
            menu.findItem(R.id.system_apps).setVisible(true);
        } else if (appType == APP_TYPE_SYSTEM) {
            menu.findItem(R.id.user_apps).setVisible(true);
        }

        menu.findItem(R.id.applications).setChecked(true);
    }

    @Override
    boolean onOptionsItemSelected(int id) {
        switch (id) {
            case R.id.system_apps: appType = APP_TYPE_SYSTEM; return true;
            case R.id.user_apps:   appType = APP_TYPE_USER;   return true;
        }
        return false;
    }

    // JSON serialization & name
    static final Descriptor DESCRIPTOR = new Descriptor(ApplicationFetcher.class, "apps", R.string.applications) {
        @Override
        Fetcher unserializeFromJSON(JSONObject jsonObject) throws JSONException {
            ApplicationFetcher fetcher = new ApplicationFetcher();
            fetcher.appType = jsonObject.getInt("appType");
            fetcher.requireMetaDataSubstring = jsonObject.getString("metadataSubstring");
            return fetcher;
        }
    };

    @Override
    JSONObject serializeToJSON() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("appType", appType);
        jsonObject.put("metadataSubstring", requireMetaDataSubstring);
        return jsonObject;
    }
}
