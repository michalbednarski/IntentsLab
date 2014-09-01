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

package com.github.michalbednarski.intentslab.valueeditors.framework;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.util.SparseArray;

/**
 * Helper class for delivering results into child fragments
 * since support library doesn't support this correctly
 *
 * TODO: remove this hack when moving to system fragments implementation
 */
public class ChildFragmentWorkaround extends Fragment {


    /**
     * Start an activity for result on fragment
     *
     * Use this instead of standard startActivityForResult on child fragments
     */
    public static void startActivityForResultFromFragment(
            Fragment fragment,
            Intent intent,
            int requestCode) {

        FragmentActivity activity = fragment.getActivity();
        FragmentManager topFragmentManager = activity.getSupportFragmentManager();
        FragmentManager fragmentManager = fragment.getFragmentManager();

        // No workaround needed?
        if (topFragmentManager == fragmentManager) {
            fragment.startActivityForResult(intent, requestCode);
            return;
        }

        DeliverResultInfo deliverInfo = new DeliverResultInfo(fragment, requestCode);

        // Create instance
        ChildFragmentWorkaround instance = (ChildFragmentWorkaround) topFragmentManager.findFragmentByTag(TAG_F);
        if (instance == null) {
            instance = new ChildFragmentWorkaround();
            instance.mNextSendIntent = intent;
            instance.mNextSendDeliverInfo = deliverInfo;
            topFragmentManager.beginTransaction().add(instance, TAG_F).commit();
        } else {
            instance.doStartActivity(intent, deliverInfo);
        }

    }

    // Fragment tag
    private static final String TAG_F = "DumbLibWorkaround";

    // Data about dispatching results
    private int mNextNewResultCode;
    private SparseArray<DeliverResultInfo> mDeliverInfos = new SparseArray<DeliverResultInfo>();

    // Pending send
    private DeliverResultInfo mNextSendDeliverInfo;
    private Intent mNextSendIntent;


    // Sending
    private void doStartActivity(Intent intent, DeliverResultInfo deliverInfo) {
        int localRequestCode = mNextNewResultCode++;
        mDeliverInfos.put(localRequestCode, deliverInfo);
        startActivityForResult(intent, localRequestCode);
    }

    // Deferred sending
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Execute pending send
        if (savedInstanceState == null && mNextSendIntent != null) {
            doStartActivity(mNextSendIntent, mNextSendDeliverInfo);
            mNextSendIntent = null;
            mNextSendDeliverInfo = null;
        }
    }

    // Dispatching
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        DeliverResultInfo deliverInfo = mDeliverInfos.get(requestCode);
        if (deliverInfo != null) {
            mDeliverInfos.remove(requestCode);
            deliverInfo.targetFragment.onActivityResult(deliverInfo.requestCode, resultCode, data);
        }
    }




    /*
    STATE SAVE/RESTORE
    */

    private static final String STATE_KEYS = "KS";
    private static final String STATE_NEXT_CODE = "NC";
    private static final String STATE_PREFIX_CODE = "RC";
    private static final String STATE_PREFIX_FRAGMENT = "TF";

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        int size = mDeliverInfos.size();
        int[] keys = new int[size];

        for (int i = 0; i < size; i++) {
            int key = mDeliverInfos.keyAt(i);
            DeliverResultInfo deliverInfo = mDeliverInfos.valueAt(i);

            keys[i] = key;
            outState.putInt(STATE_PREFIX_CODE + key, deliverInfo.requestCode);
            putChildFragmentInBundle(outState, STATE_PREFIX_FRAGMENT + key, deliverInfo.targetFragment);
        }

        outState.putIntArray(STATE_KEYS, keys);
        outState.putInt(STATE_NEXT_CODE, mNextNewResultCode);
    }


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (savedInstanceState != null) {
            FragmentManager topFragmentManager = getFragmentManager();
            for (int key : savedInstanceState.getIntArray(STATE_KEYS)) {
                mDeliverInfos.put(
                        key,
                        new DeliverResultInfo(
                                getChildFragmentFromBundle(
                                        topFragmentManager,
                                        savedInstanceState, STATE_PREFIX_FRAGMENT + key
                                ),
                                savedInstanceState.getInt(STATE_PREFIX_CODE + key)
                        )
                );
            }
            mNextNewResultCode = savedInstanceState.getInt(STATE_NEXT_CODE);
        }
    }

    /**
     * Child fragment aware {@link FragmentManager#putFragment(Bundle, String, Fragment)}
     */
    private static void putChildFragmentInBundle(Bundle bundle, String key, Fragment fragment) {
        int nl = 0;
        while (fragment != null) {
            fragment.getFragmentManager().putFragment(bundle, key + "__N" + nl, fragment);
            nl++;
            fragment = fragment.getParentFragment();
        }
        bundle.putInt(key + "__Z", nl);
    }

    private static Fragment getChildFragmentFromBundle(FragmentManager topFragmentManager, Bundle bundle, String key) {
        int nl = bundle.getInt(key + "__Z");
        for (;;) {
            Fragment fragment = topFragmentManager.getFragment(bundle, key + "__N" + (--nl));
            if (fragment == null || nl == 0) {
                return fragment;
            }
            topFragmentManager = fragment.getChildFragmentManager();
        }
    }

    /**
     * Data about result delivery
     */
    private static class DeliverResultInfo {
        DeliverResultInfo(Fragment targetFragment, int requestCode) {
            this.targetFragment = targetFragment;
            this.requestCode = requestCode;
        }

        Fragment targetFragment;
        int requestCode;
    }
}
