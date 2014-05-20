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

package com.github.michalbednarski.intentslab.bindservice.manager;

import android.support.v4.app.Fragment;

/**
 * Created by mb on 21.03.14.
 */
public abstract class BaseServiceFragment extends Fragment {
    public static final String ARG_SERVICE_DESCRIPTOR = "intentslab.internal.service-descriptor.arg";

    private BindServiceManager.Helper mBoundService;
    private boolean mBoundServiceRefd;

    protected BindServiceManager.Helper getServiceHelper() {
        if (mBoundService == null) {
            ServiceDescriptor descriptor = getArguments().getParcelable(ARG_SERVICE_DESCRIPTOR);
            mBoundService = BindServiceManager.getBoundService(descriptor);
        }
        if (!mBoundServiceRefd) {
            mBoundService.userRef();
            mBoundServiceRefd = true;
        }
        return mBoundService;
    }

    @Override
    public void onDestroy() {
        if (mBoundServiceRefd) {
            mBoundService.userUnref();
        }
        super.onDestroy();
    }
}
