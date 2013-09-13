package com.example.testapp1.providerlab.proxy;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

/**
 * Activity for viewing log of operations performed by {@link com.example.testapp1.providerlab.proxy.ProxyProvider}
 */
public class LogViewerActivity extends FragmentActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(android.R.id.content, new LogViewerFragment())
                    .commit();
        }
    }
}
