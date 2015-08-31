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

package com.github.michalbednarski.intentslab.uihelpers;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.view.View;

import com.github.michalbednarski.intentslab.R;
import com.github.michalbednarski.intentslab.SingleFragmentActivity;

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

                // Show or hide master using fragment manager
                Fragment masterFragment = getMaster();
                boolean masterVisible = !masterFragment.isHidden();
                if (masterVisible != usingTabletView()) {
                    FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                    if (usingTabletView()) {
                        transaction.show(masterFragment);
                    } else {
                        transaction.hide(masterFragment);
                    }
                    transaction.commit();
                }
            }
        }
        findViewById(R.id.detail).setVisibility(showDetail ? View.VISIBLE : View.GONE);
    }

    /**
     * Create fragment for use as master view
     */
    protected abstract Fragment createMasterFragment();

    private Fragment getMaster() {
        return getSupportFragmentManager().findFragmentById(R.id.master);
    }

    private Fragment getDetail() {
        return getSupportFragmentManager().findFragmentById(R.id.detail);
    }

    public boolean usingTabletView() {
        return getResources().getBoolean(R.bool.use_master_detail);
    }
    public boolean mayUseTabletView() {
        return getResources().getBoolean(R.bool.may_use_master_detail);
    }

    /**
     * Open given fragment in detail panel or new activity
     */
    public void openFragment(Class<? extends Fragment> fragmentClass, Bundle arguments) {
        if (mayUseTabletView()) {
            openFragmentInDetail(fragmentClass, arguments);
        } else {
            // Just open in new activity
            arguments.putString(SingleFragmentActivity.EXTRA_FRAGMENT, fragmentClass.getName());
            startActivity(new Intent(this, SingleFragmentActivity.class).replaceExtras(arguments));
        }
    }

    /**
     * Open given fragment in detail panel
     */
    public void openFragmentInDetail(Class<? extends Fragment> fragmentClass, Bundle arguments) {
        // Instantiate fragment
        Fragment fragment;
        try {
            fragment = fragmentClass.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        fragment.setArguments(arguments);

        // Perform fragment transaction
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.detail, fragment);
        if (!usingTabletView()) {
            // If not using tablet view right now, hide master
            transaction.hide(getMaster());
            findViewById(R.id.master).setVisibility(View.GONE);
        }
        transaction.commit();

        // Show detail view
        findViewById(R.id.detail).setVisibility(View.VISIBLE);
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
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.remove(getDetail());
        Fragment masterFragment = getMaster();
        if (masterFragment.isHidden()) {
            transaction.show(masterFragment);
        }
        transaction.commit();
        findViewById(R.id.detail).setVisibility(View.GONE);
        findViewById(R.id.master).setVisibility(View.VISIBLE);
    }
}
