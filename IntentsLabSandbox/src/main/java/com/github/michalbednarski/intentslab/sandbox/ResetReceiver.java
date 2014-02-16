package com.github.michalbednarski.intentslab.sandbox;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by mb on 29.09.13.
 */
public class ResetReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        context.getCacheDir().delete();
        System.exit(0);
    }
}
