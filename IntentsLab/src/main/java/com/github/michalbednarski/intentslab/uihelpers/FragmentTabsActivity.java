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

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.FragmentTransaction;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.util.ArrayMap;
import android.support.v4.view.ViewPager;
import android.view.inputmethod.InputMethodManager;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;

import com.github.michalbednarski.intentslab.BuildConfig;
import com.github.michalbednarski.intentslab.R;

import java.util.ArrayList;
import java.util.HashMap;

import static android.preference.PreferenceManager.getDefaultSharedPreferences;

@SuppressLint("Registered")
public abstract class FragmentTabsActivity extends FragmentActivity {

    private static final String STATE_CURRENT_FRAGMENT_NUMBER = "FragmentTabsActivity.current";

    private ViewPager mViewPager;

    private ArrayList<Fragment> mFragmentsList = new ArrayList<Fragment>();
    private int mTabsCount = 0;
    private int mTabsToSkip = 0;
    private final ArrayMap<Integer, FragmentTabMergingPagerAdapter.MultiFragmentPageInfo> mTabMergingMappings = new ArrayMap<Integer, FragmentTabMergingPagerAdapter.MultiFragmentPageInfo>();

    private int mInitialFragmentNumber = 0; // For state restoring

    private TabsHelper mTabsHelper;
    private FragmentTabMergingPagerAdapter mAdapter;

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        // Create tabs helper and view pager
        boolean useActionBarForTabs = Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB
                && !getDefaultSharedPreferences(this).getBoolean("forcelegacytabs", false);

		if (useActionBarForTabs) {
			mTabsHelper = new TabsHelperActionBar();
		} else {
            mTabsHelper = new TabsHelperTabWidget();
		}

        // We save state ourselves
        mViewPager.setSaveEnabled(false);
        if (savedInstanceState != null) {
            mInitialFragmentNumber = savedInstanceState.getInt(STATE_CURRENT_FRAGMENT_NUMBER);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_CURRENT_FRAGMENT_NUMBER, mAdapter.getCurrentFragmentNumber());
    }

    protected void allTabsAdded() {
        mAdapter = new FragmentTabMergingPagerAdapter(
                getSupportFragmentManager(),
                mFragmentsList.toArray(new Fragment[mFragmentsList.size()]),
                mTabMergingMappings
        );
        final int page = mAdapter.fragmentNumberToPageNumber(mInitialFragmentNumber);
        mViewPager.setAdapter(mAdapter);
        mViewPager.setCurrentItem(page);
		mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
			@Override
			public void onPageSelected(int position) {
				mTabsHelper.selectTab(position);
			}
		});
        mTabsHelper.selectTab(page);
	}

	/**
	 * Add new tab with associated fragment
	 *
	 * @param text Displayed title of tab
	 * @param fragment Fragment class that has to be instantiated and put in tab
	 */
    protected void addTab(CharSequence text, Fragment fragment) {
        if (mTabsToSkip == 0) {
            mTabsHelper.addTab(text, mTabsCount++);
        } else {
            mTabsToSkip--;
        }
        mFragmentsList.add(fragment);
	}

    protected void mergeFollowingTabs(CharSequence text, int containerLayout, int ...fillInViewIds) {
        if (BuildConfig.DEBUG) {
            if (mTabsToSkip != 0) {
                throw new AssertionError("Overlapping tab merge");
            }
            if (fillInViewIds.length <= 0) {
                throw new AssertionError("No fill-in view ids");
            }
        }

        mTabMergingMappings.put(
                mFragmentsList.size(),
                new FragmentTabMergingPagerAdapter.MultiFragmentPageInfo(
                        containerLayout,
                        fillInViewIds
                )
        );
        mTabsHelper.addTab(text, mTabsCount++);
        mTabsToSkip = fillInViewIds.length;
    }

    /**
     * Callback from TabsHelper implementation
     * Notifies us about tab selection and triggers page selection in ViewPage
     */
    private void onTabSelected(int index) {
        mViewPager.setCurrentItem(index);
    }

    private interface TabsHelper {
        /**
         * Add a tab
         *
         * @param text Text on tab
         * @param index Index of new tab,
         *              This will be always count of tabs added so far
         *              This is to help implementation and cannot be used to insert tab before another
         */
        void addTab(CharSequence text, int index);

        /**
         * Select tab with given index
         */
        void selectTab(int index);
    }

    private class TabsHelperTabWidget implements TabsHelper {

        private HashMap<String, Integer> mTabIdToIndexMap = new HashMap<String, Integer>();
        private final TabHost mTabHost;

        private TabsHelperTabWidget() {
            // Prepare TabHost tabs
            setContentView(R.layout.tabhost_fragment);

            mTabHost = (TabHost) findViewById(android.R.id.tabhost);
            mTabHost.setup();

            mTabHost.setOnTabChangedListener(new TabHost.OnTabChangeListener() {
                @Override
                public void onTabChanged(String tabId) {
                    // Notify FragmentTabsActivity
                    onTabSelected(mTabIdToIndexMap.get(tabId));

                    // Hide keyboard
                    // http://stackoverflow.com/questions/1109022/close-hide-the-android-soft-keyboard
                    InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(mTabHost.getApplicationWindowToken(), 0);
                }
            });
            mViewPager = (ViewPager) findViewById(R.id.pager);
        }

        @Override
        public void addTab(CharSequence text, int index) {
            String tabId = text.toString();
            mTabIdToIndexMap.put(tabId, index);
            TabHost th = (TabHost) findViewById(android.R.id.tabhost);
            TabSpec tabSpec = th.newTabSpec(tabId).setIndicator(text).setContent(android.R.id.tabcontent);
            th.addTab(tabSpec);
        }

        @Override
        public void selectTab(int index) {
            mTabHost.setCurrentTab(index);
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private class TabsHelperActionBar implements TabsHelper {
        private ActionBar.TabListener mActionBarTabListener;
        private ArrayList<ActionBar.Tab> mTabs = new ArrayList<ActionBar.Tab>();

        private TabsHelperActionBar() {
            // Prepare tabs on ActionBar
            ActionBar actionBar = getActionBar();
            actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
            mActionBarTabListener = new ActionBar.TabListener() {

                @Override
                public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction ft) {
                }

                @SuppressWarnings("unchecked")
                @Override
                public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {
                    // hide keyboard (might cause flickering, but I don't know solution)
                    InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(mViewPager.getApplicationWindowToken(), 0);

                    // Notify FragmentTabsActivity
                    FragmentTabsActivity.this.onTabSelected((Integer) tab.getTag());
                }

                @Override
                public void onTabReselected(ActionBar.Tab tab, FragmentTransaction ft) {
                }
            };
            mViewPager = new ViewPager(FragmentTabsActivity.this);
            mViewPager.setId(R.id.pager);
            setContentView(mViewPager);
        }

        @Override
        public void addTab(CharSequence text, int index) {
            ActionBar actionBar = getActionBar();
            ActionBar.Tab tab = actionBar.newTab() //
                    .setText(text) //
                    .setTag(mFragmentsList.size()) // Position
                    .setTabListener(mActionBarTabListener);
            mTabs.add(tab);
            actionBar.addTab(tab);
        }

        @Override
        public void selectTab(int index) {
            getActionBar().selectTab(mTabs.get(index));
        }
    }

}
