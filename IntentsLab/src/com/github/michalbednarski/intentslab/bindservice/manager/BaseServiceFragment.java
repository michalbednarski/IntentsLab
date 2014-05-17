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
