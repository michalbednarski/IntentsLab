package com.example.testapp1.samplecomponents;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

/**
 * Sample BroadcastReceiver that uses setResult()
 */
public class SetResultReceiver extends BroadcastReceiver {
    public void onReceive(Context context, Intent intent) {
        setResult(42, "result data", new Bundle());
    }
}
