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

import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.util.ArrayMap;
import android.support.v4.view.PagerAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.github.michalbednarski.intentslab.BuildConfig;
import com.github.michalbednarski.intentslab.Utils;

import java.util.ArrayList;

/**
 * Implementation of {@link android.support.v4.view.PagerAdapter} that
 * represents each page as a {@link android.support.v4.app.Fragment} that is persistently
 * kept in the fragment manager as long as the user can return to the page.
 *
 * <p>This version of the pager is best for use when there are a handful of
 * typically more static fragments to be paged through, such as a set of tabs.
 * The fragment of each page the user visits will be kept in memory, though its
 * view hierarchy may be destroyed when not visible.  This can result in using
 * a significant amount of memory since fragment instances can hold on to an
 * arbitrary amount of state.  For larger sets of pages, consider
 * {@link android.support.v4.app.FragmentStatePagerAdapter}.
 *
 * <p>When using FragmentTabMergingPagerAdapter the host ViewPager must have a
 * valid ID set.</p>
 */
class FragmentTabMergingPagerAdapter extends PagerAdapter {
    private static final String TAG = "FragmentTabMergingPagerAdapter";
    private static final boolean DEBUG = false;

    private final FragmentManager mFragmentManager;
    private FragmentTransaction mCurTransaction = null;
    private ArrayList<PendingAddFragmentAfterMove> mPendingAddFragmentOps = null;

    private Object mCurrentPrimaryItem;
    private int mCurrentPrimaryPage = -1;


    private final Fragment[] mFragments;
    private final int[] mPageToFirstFragmentMap;
    private final int[] mFragmentToPageMap;
    private final MultiFragmentPageInfo[] mPagesInfoMap;


    public FragmentTabMergingPagerAdapter(
            FragmentManager fm,
            Fragment[] fragments,
            ArrayMap<Integer, MultiFragmentPageInfo> tabMappings
    ) {
        mFragmentManager = fm;
        mFragments = fragments;

        int count = fragments.length;

        int[] fragmentToPageMap = new int[count];
        int[] pageToFirstFragmentMap = new int[count];
        int pageCount = 0;

        for (int fragmentIndex = 0; fragmentIndex < count;) {
            final MultiFragmentPageInfo info = tabMappings.get(fragmentIndex);
            final int page = pageCount++;

            // Get count of fragments on page
            final int fragmentsOnPage;
            if (info != null) {
                fragmentsOnPage = info.fillInIds.length;
            } else {
                fragmentsOnPage = 1;
            }

            // Ensure valid fragmentsOnPage value
            if (BuildConfig.DEBUG && fragmentsOnPage <= 0) {
                throw new AssertionError("Page must have at least one fragment");
            }

            // Create mappings for page
            for (int i = 0; i < fragmentsOnPage; i++) {
                fragmentToPageMap[fragmentIndex + i] = page;
            }
            pageToFirstFragmentMap[page] = fragmentIndex;
            fragmentIndex += fragmentsOnPage;
        }

        // Ensure that pageCount >= count
        if (BuildConfig.DEBUG && (count < pageCount || pageCount <= 0)) {
            throw new AssertionError("Invalid count of pages");
        }


        // Shrink and export to fields
        mFragmentToPageMap = fragmentToPageMap;
        mPageToFirstFragmentMap = Utils.shrinkIntArray(pageToFirstFragmentMap, pageCount);
        mPagesInfoMap = new MultiFragmentPageInfo[pageCount];
        for (int i = 0; i < tabMappings.size(); i++) {
            mPagesInfoMap[tabMappings.keyAt(i)] = tabMappings.valueAt(i);
        }
    }


    @Override
    public int getCount() {
        return mPageToFirstFragmentMap.length;
    }

    @Override
    public void startUpdate(ViewGroup container) {
    }

    @Override
    public Object instantiateItem(ViewGroup container, int page) {
        if (mCurTransaction == null) {
            mCurTransaction = mFragmentManager.beginTransaction();
        }

        // Get info about page
        final MultiFragmentPageInfo pageInfo = mPagesInfoMap[page];
        final int fragmentsOnPage = pageInfo != null ? pageInfo.fillInIds.length : 1;
        final int firstFragmentOnPage = mPageToFirstFragmentMap[page];

        // Prepare wrapper layout
        final View view;
        final Fragment[] addedFragments;
        if (pageInfo != null) {
            view = LayoutInflater.from(container.getContext()).inflate(pageInfo.containerLayout, container, false);
            container.addView(view);
            addedFragments = new Fragment[fragmentsOnPage];
        } else {
            view = null;
            addedFragments = null;
        }

        // Prepare all fragments on page
        final int pagerId = container.getId();
        for (int i = 0; i < fragmentsOnPage; i++) {
            final int fragmentIndex = firstFragmentOnPage + i;
            final int containerId =
                    pageInfo == null ?
                            pagerId :
                            pageInfo.fillInIds[i];

            // Do we already have this fragment?
            String name = makeFragmentName(pagerId, fragmentIndex);
            Fragment fragment = mFragmentManager.findFragmentByTag(name);
            if (fragment != null) {
                if (DEBUG) Log.v(TAG, "Attaching item #" + fragmentIndex + ": f=" + fragment);
                if (fragment.getId() != containerId) {
                    // Added to another container
                    moveFragment(fragment, containerId);
                } else {
                    // Already added, possibly detached
                    if (pageInfo != null) {
                        // When activity was created this tab wasn't ready
                        // Detach and re-attach this fragment
                        mCurTransaction.detach(fragment);
                    }

                    mCurTransaction.attach(fragment);
                }
            } else {
                // Not added
                fragment = mFragments[fragmentIndex];

                if (DEBUG) Log.v(TAG, "Adding item #" + fragmentIndex + ": f=" + fragment);

                mCurTransaction.add(containerId, fragment, name);
            }

            // Deactivate menu if not on current page
            if (page != mCurrentPrimaryPage) {
                fragment.setMenuVisibility(false);
                fragment.setUserVisibleHint(false);
            }

            // For single fragment page return fragment
            if (pageInfo == null) {
                return fragment;
            }

            // On multi fragment page add to page
            addedFragments[i] = fragment;
        }

        // Return descriptor of multi fragment page
        return new AddedPageInfo(view, addedFragments);
    }

    private void moveFragment(Fragment fragment, int newContainerId) {
        // We must remove, commit, executePending and add again to move
        // http://stackoverflow.com/a/17775067

        // Schedule fragment to be re-added
        if (mPendingAddFragmentOps == null) {
            mPendingAddFragmentOps = new ArrayList<PendingAddFragmentAfterMove>();
        }
        mPendingAddFragmentOps.add(new PendingAddFragmentAfterMove(
                newContainerId,
                fragment,
                fragment.getTag()
        ));

        // Remove it now
        mCurTransaction.remove(fragment);
    }

    @Override
    public void destroyItem(ViewGroup container, int page, Object object) {
        if (mCurTransaction == null) {
            mCurTransaction = mFragmentManager.beginTransaction();
        }

        if (DEBUG) Log.v(TAG, "Detaching items from page #" + page);

        if (object instanceof AddedPageInfo) {
            for (Fragment fragment : ((AddedPageInfo) object).fragments) {
                mCurTransaction.detach(fragment);
            }
            container.removeView(((AddedPageInfo) object).view);
        } else {
            mCurTransaction.detach((Fragment) object);
        }
    }

    @Override
    public void setPrimaryItem(ViewGroup container, int page, Object object) {
        if (object != mCurrentPrimaryItem) {
            if (mCurrentPrimaryItem != null) {
                activateOrDeactivatePage(mCurrentPrimaryItem, false);
            }
            if (object != null) {
                activateOrDeactivatePage(object, true);
            }
            mCurrentPrimaryItem = object;
            mCurrentPrimaryPage = page;
        }
    }

    private void activateOrDeactivatePage(Object object, boolean activate) {
        if (object instanceof AddedPageInfo) {
            for (Fragment fragment : ((AddedPageInfo) object).fragments) {
                fragment.setMenuVisibility(activate);
                fragment.setUserVisibleHint(activate);
            }
        } else {
            Fragment fragment = (Fragment) object;
            fragment.setMenuVisibility(activate);
            fragment.setUserVisibleHint(activate);
        }
    }

    @Override
    public void finishUpdate(ViewGroup container) {
        if (mCurTransaction != null) {
            mCurTransaction.commitAllowingStateLoss();
            mCurTransaction = null;
            mFragmentManager.executePendingTransactions();

            // Execute scheduled re-additions after remove
            if (mPendingAddFragmentOps != null) {
                final FragmentTransaction transaction = mFragmentManager.beginTransaction();
                for (PendingAddFragmentAfterMove op : mPendingAddFragmentOps) {
                    op.addToTransaction(transaction);
                }
                mPendingAddFragmentOps = null;
                transaction.commitAllowingStateLoss();
                mFragmentManager.executePendingTransactions();
            }
        }
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        if (object instanceof AddedPageInfo) {
            return ((AddedPageInfo) object).view == view;
        }
        return ((Fragment)object).getView() == view;
    }

    @Override
    public Parcelable saveState() {
        return null;
    }

    @Override
    public void restoreState(Parcelable state, ClassLoader loader) {
    }

    private static String makeFragmentName(int viewId, long id) {
        return "android:switcher:" + viewId + ":" + id;
    }

    // Save/restore state
    private int mSavedPageNumber = -1;
    private int mSavedFragmentNumber;

    int fragmentNumberToPageNumber(int fragmentNumber) {
        if (fragmentNumber > 0) {
            mSavedFragmentNumber = fragmentNumber;
            mSavedPageNumber = mFragmentToPageMap[fragmentNumber];
            return mSavedPageNumber;
        }
        return 0;
    }

    int getCurrentFragmentNumber() {
        if (mCurrentPrimaryPage == mSavedPageNumber) {
            return mSavedFragmentNumber;
        }
        return mPageToFirstFragmentMap[mCurrentPrimaryPage];
    }

    // Info about tabs to merge
    static final class MultiFragmentPageInfo {
        MultiFragmentPageInfo(int containerLayout, int[] fillInIds) {
            this.containerLayout = containerLayout;
            this.fillInIds = fillInIds;
        }

        final int containerLayout;
        final int[] fillInIds;
    }

    // Created page view and fragment holder
    private static final class AddedPageInfo {
        AddedPageInfo(View view, Fragment[] fragments) {
            this.view = view;
            this.fragments = fragments;
        }

        final View view;
        final Fragment[] fragments;
    }

    private static final class PendingAddFragmentAfterMove {
        private int mContainerViewId;
        private Fragment mFragment;
        private String mTag;

        PendingAddFragmentAfterMove(int containerViewId, Fragment fragment, String tag) {
            mContainerViewId = containerViewId;
            mFragment = fragment;
            mTag = tag;
        }

        void addToTransaction(FragmentTransaction transaction) {
            transaction.add(mContainerViewId, mFragment, mTag);
        }
    }
}
