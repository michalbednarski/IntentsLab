package com.example.testapp1.samplecomponents;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Sample BroadcastReceiver that uses abortBroadcast()
 */
public class AbortBroadcastReceiver extends BroadcastReceiver {
    public void onReceive(Context context, Intent intent) {
        abortBroadcast();
    }
}
