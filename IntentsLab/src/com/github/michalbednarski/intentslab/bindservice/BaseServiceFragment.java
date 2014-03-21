package com.github.michalbednarski.intentslab.bindservice;

import android.support.v4.app.Fragment;

import com.github.michalbednarski.intentslab.bindservice.manager.BindServiceManager;
import com.github.michalbednarski.intentslab.bindservice.manager.ServiceDescriptor;

/**
 * Created by mb on 21.03.14.
 */
public abstract class BaseServiceFragment extends Fragment {
    public static final String ARG_SERVICE_DESCRIPTOR = "intentslab.internal.service-descriptor.arg";
    private BindServiceManager.Helper mBoundService;

    protected BindServiceManager.Helper getServiceHelper() {
        if (mBoundService == null) {
            ServiceDescriptor descriptor = getArguments().getParcelable(ARG_SERVICE_DESCRIPTOR);
            mBoundService = BindServiceManager.getBoundService(descriptor);
        }
        return mBoundService;
    }
}
