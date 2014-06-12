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

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.MenuItem;
import com.github.michalbednarski.intentslab.MasterDetailActivity;

public class BrowseComponentsActivity extends MasterDetailActivity {

    public static final String EXTRA_FETCHER = "BrowseComponentsActivity.fetcher";
    public static final String EXTRA_SERIALIZED_FETCHER = "BrowseComponentsActivity.fetcher.serialized";


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= 11) {
            onCreateAndroidSDK11AndUp();
        }
    }

    @Override
    protected Fragment createMasterFragment() {
        Bundle args = new Bundle();
        Fetcher fetcher;
        if (getIntent().hasExtra(EXTRA_SERIALIZED_FETCHER)) {
            fetcher = FetcherManager.unserializeFetcher(getIntent().getStringExtra(EXTRA_SERIALIZED_FETCHER));
        } else {
            fetcher = getIntent().getParcelableExtra(EXTRA_FETCHER);
        }
        args.putParcelable(BrowseComponentsFragment.ARG_FETCHER, fetcher);
        BrowseComponentsFragment fragment = new BrowseComponentsFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @TargetApi(11)
    private void onCreateAndroidSDK11AndUp() {
        getActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        switch (itemId) {
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
