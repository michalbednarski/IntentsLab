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

import android.content.pm.PackageManager;
import android.os.Bundle;
import com.github.michalbednarski.intentslab.uihelpers.FragmentTabsActivity;

/**
 * Application info
 */
public class AppInfoActivity extends FragmentTabsActivity implements AppComponentsFragment.AppInfoHost {
    public static final String EXTRA_PACKAGE_NAME = "intentsLab.packInfo.package";

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String packageName = getViewedPackageName();
        try {
            final PackageManager packageManager = getPackageManager();
            final CharSequence packageLabel = packageManager.getPackageInfo(packageName, 0).applicationInfo.loadLabel(packageManager);
            setTitle(packageLabel);
        } catch (Exception e) {
            setTitle(packageName);
        }

        addTab("Components", new AppComponentsFragment());
        addTab("Manifest", XMLViewerFragment.create(packageName, 0));
		allTabsAdded();
    }

    @Override
    public String getViewedPackageName() {
        return getIntent().getStringExtra(EXTRA_PACKAGE_NAME);
    }
}