package com.github.michalbednarski.intentslab.xposedhooks.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

public class QueryPermissionsReceiver extends BroadcastReceiver {
    public static final String RESULT_EXTRA_ALLOWED_UIDS = "XIntentsLab.allowedUids";

    @Override
    public void onReceive(Context context, Intent intent) {
        AllowedAppsDb allowedAppsDb = AllowedAppsDb.getInstance(context);
        Bundle resultExtras = new Bundle();
        resultExtras.putIntArray(
                RESULT_EXTRA_ALLOWED_UIDS,
                allowedAppsDb.getAllowedUids()
        );
        setResultExtras(resultExtras);
        allowedAppsDb.saveIfNeeded();
    }
}
