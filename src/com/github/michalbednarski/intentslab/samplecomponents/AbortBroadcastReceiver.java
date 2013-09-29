package com.github.michalbednarski.intentslab.samplecomponents;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.github.michalbednarski.intentslab.Utils;

/**
 * Sample BroadcastReceiver that uses abortBroadcast()
 */
public class AbortBroadcastReceiver extends BroadcastReceiver {
    public void onReceive(Context context, Intent intent) {
        try {
            abortBroadcast();
        } catch (Exception e) {
            Utils.toastException(context, e);
        }
    }
}
