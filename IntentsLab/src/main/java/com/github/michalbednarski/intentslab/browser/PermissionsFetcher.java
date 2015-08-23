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
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PermissionInfo;
import android.os.Parcel;
import com.github.michalbednarski.intentslab.R;
import com.github.michalbednarski.intentslab.Utils;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Fetcher for permissions
 */
public class PermissionsFetcher extends AsyncTaskFetcher {
    private boolean mGrouped = true;
    private int mProtectionFilter =
            ComponentFetcher.PROTECTION_ANY_LEVEL;
    private String mNameSubstring;

    @Override
    Object getEntries(Context context) {
        PackageManager pm = context.getPackageManager();
        List<PackageInfo> installedPackages = pm.getInstalledPackages(PackageManager.GET_PERMISSIONS);
        ArrayList<Component> foundPermissions = new ArrayList<Component>();
        final boolean grouped = mGrouped; // Avoid race conditions
        ArrayList<Category> apps = grouped ? new ArrayList<Category>() : null;

        for (PackageInfo aPackage : installedPackages) {
            if (aPackage.permissions == null || aPackage.permissions.length == 0) {
                continue;
            }

            for (PermissionInfo permission : aPackage.permissions) {
                if (ComponentFetcher.checkProtectionLevelRaw(permission, mProtectionFilter) &&
                        (mNameSubstring == null || permission.name.toLowerCase().contains(mNameSubstring.toLowerCase()))
                    ) {
                    Component component = new Component();
                    component.title = permission.name;
                    component.subtitle = String.valueOf(permission.loadLabel(pm));
                    component.componentInfo = permission;
                    foundPermissions.add(component);
                }
            }

            if (grouped && !foundPermissions.isEmpty()) {
                Category category = new Category();
                category.title = String.valueOf(aPackage.applicationInfo.loadLabel(pm));
                category.subtitle = aPackage.packageName;
                category.components = foundPermissions.toArray(new Component[foundPermissions.size()]);
                apps.add(category);
                foundPermissions.clear();
            }
        }
        return grouped ?
                apps.toArray(new Category[apps.size()]) :
                foundPermissions.toArray(new Component[foundPermissions.size()]);
    }

    @Override
    int getConfigurationLayout() {
        return R.layout.permissions_filter;
    }

    @Override
    void initConfigurationForm(FetcherOptionsDialog dialog) {
        dialog.setBoxChecked(R.id.group_by_applications, mGrouped);
        dialog.setTextInField(R.id.substring, mNameSubstring);
        dialog.setBoxChecked(R.id.permission_filter_normal, (mProtectionFilter & ComponentFetcher.PROTECTION_NORMAL) != 0);
        dialog.setBoxChecked(R.id.permission_filter_dangerous, (mProtectionFilter & ComponentFetcher.PROTECTION_DANGEROUS) != 0);
        dialog.setBoxChecked(R.id.permission_filter_signature, (mProtectionFilter & ComponentFetcher.PROTECTION_SIGNATURE) != 0);
        dialog.setBoxChecked(R.id.permission_filter_system, (mProtectionFilter & ComponentFetcher.PROTECTION_SYSTEM) != 0);
        if (ComponentFetcher.DEVELOPMENT_PERMISSIONS_SUPPORTED) {
            dialog.setBoxChecked(R.id.permission_filter_development, (mProtectionFilter & ComponentFetcher.PROTECTION_DEVELOPMENT) != 0);
        } else {
            dialog.findView(R.id.permission_filter_development).setEnabled(false);
        }
    }

    @Override
    void updateFromConfigurationForm(FetcherOptionsDialog dialog) {
        mGrouped = dialog.isBoxChecked(R.id.group_by_applications);
        mNameSubstring = dialog.getTextFromField(R.id.substring);
        if (Utils.stringEmptyOrNull(mNameSubstring)) {
            mNameSubstring = null;
        }
        mProtectionFilter =
                (dialog.isBoxChecked(R.id.permission_filter_normal) ? ComponentFetcher.PROTECTION_NORMAL : 0) |
                (dialog.isBoxChecked(R.id.permission_filter_dangerous) ? ComponentFetcher.PROTECTION_DANGEROUS : 0) |
                (dialog.isBoxChecked(R.id.permission_filter_signature) ? ComponentFetcher.PROTECTION_SIGNATURE : 0) |
                (dialog.isBoxChecked(R.id.permission_filter_system) ? ComponentFetcher.PROTECTION_SYSTEM : 0) |
                (dialog.isBoxChecked(R.id.permission_filter_development) ? ComponentFetcher.PROTECTION_DEVELOPMENT : 0);
    }

    @Override
    boolean isExcludingEverything() {
        return mProtectionFilter == 0;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mGrouped ? 1 : 0);
        dest.writeString(mNameSubstring);
        dest.writeInt(mProtectionFilter);
    }

    public static final Creator<PermissionsFetcher> CREATOR = new Creator<PermissionsFetcher>() {
        @Override
        public PermissionsFetcher createFromParcel(Parcel source) {
            PermissionsFetcher fetcher = new PermissionsFetcher();
            fetcher.mGrouped = source.readInt() != 0;
            fetcher.mNameSubstring = source.readString();
            fetcher.mProtectionFilter = source.readInt();
            return fetcher;
        }

        @Override
        public PermissionsFetcher[] newArray(int size) {
            return new PermissionsFetcher[size];
        }
    };

    @Override
    JSONObject serializeToJSON() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("grouped", mGrouped);
        jsonObject.put("substring", mNameSubstring);
        jsonObject.put("protection", mProtectionFilter);
        return jsonObject;
    }

    static final Descriptor DESCRIPTOR = new Descriptor(PermissionsFetcher.class, "permissions", R.string.permissions) {
        @Override
        Fetcher unserializeFromJSON(JSONObject jsonObject) throws JSONException {
            PermissionsFetcher fetcher = new PermissionsFetcher();
            fetcher.mGrouped = jsonObject.getBoolean("grouped");
            fetcher.mNameSubstring = jsonObject.getString("substring");
            fetcher.mProtectionFilter = jsonObject.getInt("protection");
            return fetcher;
        }
    };
}
