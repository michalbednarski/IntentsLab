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

package com.github.michalbednarski.intentslab.xposedhooks.internal.apiimpl;

import android.os.Handler;
import android.os.Message;

import com.github.michalbednarski.intentslab.xposedhooks.api.BaseTracker;
import com.github.michalbednarski.intentslab.xposedhooks.api.TrackerUpdateListener;

/**
 * Created by mb on 06.03.14.
 */
abstract class BaseTrackerImpl implements BaseTracker {

    private static final int MSG_UPDATE = 0;

    private TrackerUpdateListener mUpdateListener = null;
    private Object mUpdateLock = new Object();
    private boolean mUpdatePending = false;
    private Handler mUpdateHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_UPDATE:
                    if (mUpdateListener != null) {
                        mUpdateListener.onTrackerUpdate();
                    }
                    mUpdatePending = false;
                    break;
            }
        }
    };

    @Override
    public void setUpdateListener(TrackerUpdateListener listener, boolean alwaysTriggerUpdate) {
        mUpdateListener = listener;
        if (alwaysTriggerUpdate) {
            listener.onTrackerUpdate();
        }
    }

    @Override
    public void clearUpdateListener() {
        mUpdateListener = null;
    }

    void dispatchUpdate() {
        synchronized (mUpdateLock) {
            if (!mUpdatePending) {
                mUpdateHandler.sendEmptyMessageDelayed(MSG_UPDATE, 500);
                mUpdatePending = true;
            }
        }
    }
}
