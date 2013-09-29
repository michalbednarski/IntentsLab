package com.github.michalbednarski.intentslab;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

/**
 *
 */
public class XMLViewerActivity extends FragmentActivity {
    public static final String EXTRA_PACKAGE_NAME = "packageName__";
    public static final String EXTRA_RESOURCE_ID = "resId";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            String packageName = getIntent().getStringExtra(EXTRA_PACKAGE_NAME);
            int resourceId = getIntent().getIntExtra(EXTRA_RESOURCE_ID, 0);
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(android.R.id.content, XMLViewerFragment.create(packageName, resourceId))
                    .commit();
        }
    }
}
