package com.github.michalbednarski.intentslab.appinfo;

import android.content.IntentFilter;
import android.content.UriPermission;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

/**
 * Information about activity/receiver/service/provider in application
 */
public interface MyComponentInfo {
    String getName();

    boolean isExported();

    String getPermission();

    String getWritePermission();

    CharSequence loadLabel(PackageManager packageManager); // TODO: Context?, strip formatting?

    Drawable loadIcon(PackageManager packageManager); // TODO: Context?

    boolean isEnabled(PackageManager packageManager); // TODO: drop argument?

    IntentFilter[] getIntentFilters();

    Bundle getMetaData();

    ProviderInfo getProviderInfo(); // TODO: split to multiple methods for getting relevant info
}
