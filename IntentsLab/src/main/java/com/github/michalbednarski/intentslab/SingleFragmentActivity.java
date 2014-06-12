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

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.widget.Toast;

import com.github.michalbednarski.intentslab.bindservice.AidlControlsFragment;
import com.github.michalbednarski.intentslab.bindservice.InvokeAidlMethodFragment;
import com.github.michalbednarski.intentslab.bindservice.callback.CallbackCallsFragment;
import com.github.michalbednarski.intentslab.browser.ComponentInfoFragment;
import com.github.michalbednarski.intentslab.browser.RegisteredReceiverInfoFragment;
import com.github.michalbednarski.intentslab.providerlab.ProviderInfoFragment;

/**
 * Activity displaying single fragment
 */
public class SingleFragmentActivity extends FragmentActivity {

    public static final String EXTRA_FRAGMENT = "singleFragmentActivity.theFragmentClass";

    @SuppressWarnings("unchecked")
    private static final Class<? extends Fragment>[] WHITE_LIST = new Class[] {
            XMLViewerFragment.class,
            ComponentInfoFragment.class,
            ProviderInfoFragment.class,
            RegisteredReceiverInfoFragment.class,
            PermissionInfoFragment.class,
            AidlControlsFragment.class,
            InvokeAidlMethodFragment.class,
            CallbackCallsFragment.class
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            String fragmentName = getIntent().getStringExtra(EXTRA_FRAGMENT);
            for (Class<? extends Fragment> fragmentClass : WHITE_LIST) {
                if (fragmentClass.getName().equals(fragmentName)) {

                    Fragment fragment;
                    try {
                        fragment = fragmentClass.newInstance();
                    } catch (InstantiationException e) {
                        throw new RuntimeException(e);
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }

                    fragment.setArguments(getIntent().getExtras());
                    getSupportFragmentManager()
                            .beginTransaction()
                            .add(android.R.id.content, fragment)
                            .commit();

                    return;
                }
            }

            Toast.makeText(this, "Fragment not whitelisted", Toast.LENGTH_SHORT).show();
            finish();
        }
    }
}
