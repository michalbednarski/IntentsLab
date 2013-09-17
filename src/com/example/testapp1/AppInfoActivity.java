package com.example.testapp1;

import android.content.pm.PackageManager;
import android.os.Bundle;
import com.example.testapp1.editor.FragmentTabsActivity;

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
    }

    @Override
    public String getViewedPackageName() {
        return getIntent().getStringExtra(EXTRA_PACKAGE_NAME);
    }
}