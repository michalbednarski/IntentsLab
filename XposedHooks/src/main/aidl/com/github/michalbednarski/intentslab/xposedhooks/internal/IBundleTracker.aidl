package com.github.michalbednarski.intentslab.xposedhooks.internal;

import com.github.michalbednarski.intentslab.xposedhooks.api.ReadBundleEntryInfo;

interface IBundleTracker {
    oneway void reportRead(in ReadBundleEntryInfo info);
}
