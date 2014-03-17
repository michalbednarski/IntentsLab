package com.github.michalbednarski.intentslab.xposedhooks.api;

import android.content.Intent;

/**
 * Created by mb on 06.03.14.
 */
public interface IntentTracker extends BaseTracker {
    Intent tagIntent(Intent intent);

    boolean actionRead();

    BundleTracker getExtrasTracker();

    //
    //
}
