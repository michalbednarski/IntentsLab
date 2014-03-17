package com.github.michalbednarski.intentslab.xposedhooks.internal;

import com.github.michalbednarski.intentslab.xposedhooks.internal.StackTraceWrapper;

interface IBundleTracker {
    oneway void reportRead(String itemName, String methodName, in StackTraceWrapper wrappedStackTrace);
}
