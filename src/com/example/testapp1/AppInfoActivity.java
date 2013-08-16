package com.example.testapp1;

import android.os.Bundle;
import com.example.testapp1.editor.FragmentTabsActivity;

/**
 * Application info
 */
public class AppInfoActivity extends FragmentTabsActivity implements AppComponentsFragment.AppInfoHost {
    public static final String EXTRA_PACKAGE_NAME = "intentsLab.packInfo.package";

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String packageName = getIntent().getStringExtra(EXTRA_PACKAGE_NAME);
        addTab("Components", new AppComponentsFragment());
        addTab("Manifest", XMLViewerFragment.create(packageName, 0));
    }

    @Override
    public String getViewedPackageName() {
        return getIntent().getStringExtra(EXTRA_PACKAGE_NAME);
    }
}