package com.github.michalbednarski.intentslab;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.View;

/**
 * Base activity implementing master-detail pattern
 */
public abstract class MasterDetailActivity extends FragmentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.master_detail_base);

        boolean showDetail = false;

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().add(R.id.master, createMasterFragment()).commit();
        } else {
            Fragment detail = getDetail();
            if (detail != null) {
                if (!usingTabletView()) {
                    // Device changed orientation and is not using master-detail anymore
                    // Hide master
                    findViewById(R.id.master).setVisibility(View.GONE);
                }
                showDetail = true;
            }
        }
        findViewById(R.id.detail).setVisibility(showDetail ? View.VISIBLE : View.GONE);
    }

    /**
     * Create fragment for use as master view
     */
    protected abstract Fragment createMasterFragment();

    private Fragment getDetail() {
        return getSupportFragmentManager().findFragmentById(R.id.detail);
    }

    public boolean usingTabletView() {
        return getResources().getBoolean(R.bool.use_master_detail);
    }

    /**
     * Open given fragment in detail panel or new activity
     */
    public void openFragment(Class<? extends Fragment> fragmentClass, Bundle arguments) {
        if (usingTabletView()) {
            // Put in detail fragment
            Fragment fragment;
            try {
                fragment = fragmentClass.newInstance();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            fragment.setArguments(arguments);
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.detail, fragment)
                    .commit();
            findViewById(R.id.detail).setVisibility(View.VISIBLE);
        } else {
            // Just open in new activity
            arguments.putString(SingleFragmentActivity.EXTRA_FRAGMENT, fragmentClass.getName());
            startActivity(new Intent(this, SingleFragmentActivity.class).replaceExtras(arguments));
        }
    }

    @Override
    public void onBackPressed() {
        if (!usingTabletView() && getDetail() != null) {
            closeDetail();
        } else {
            super.onBackPressed();
        }
    }

    private void closeDetail() {
        getSupportFragmentManager()
                .beginTransaction()
                .remove(getDetail())
                .commit();
        findViewById(R.id.detail).setVisibility(View.GONE);
        findViewById(R.id.master).setVisibility(View.VISIBLE);
    }
}
