package com.example.testapp1.samplecomponents;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.example.testapp1.Utils;

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
