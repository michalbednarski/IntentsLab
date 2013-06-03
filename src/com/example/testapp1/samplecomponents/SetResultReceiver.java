package com.example.testapp1.samplecomponents;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import com.example.testapp1.Utils;

/**
 * Sample BroadcastReceiver that uses setResult()
 */
public class SetResultReceiver extends BroadcastReceiver {
    public void onReceive(Context context, Intent intent) {
        try {
            setResult(42, "result data", new Bundle());
        } catch (Exception e) {
            Utils.toastException(context, e);
        }
    }
}
