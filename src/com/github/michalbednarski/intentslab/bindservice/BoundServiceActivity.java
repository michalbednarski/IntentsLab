package com.github.michalbednarski.intentslab.bindservice;

import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;

/**
 * Created by mb on 01.10.13.
 */
public class BoundServiceActivity extends FragmentActivity {
    public static final String EXTRA_SERVICE = "intentslab.internal.service-descriptor";

    public static final String PREF_UNBIND_ON_FINISH = "unbind-on-finish";
    private BindServiceManager.Helper mBoundService;


    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        BindServiceDescriptor descriptor = getIntent().getParcelableExtra(EXTRA_SERVICE);
        mBoundService = BindServiceManager.getBoundService(descriptor);
        if (mBoundService == null) {
            finish();
            return;
        }

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(android.R.id.content, new AidlControlsFragment())
                    .commit();
        }
    }

    BindServiceManager.Helper getBoundService() {
        return mBoundService;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mBoundService == null) {
            return;
        }
        if (isFinishing() && PreferenceManager.getDefaultSharedPreferences(this).getBoolean(PREF_UNBIND_ON_FINISH, true)) {
            mBoundService.unbind();
        }
    }
}