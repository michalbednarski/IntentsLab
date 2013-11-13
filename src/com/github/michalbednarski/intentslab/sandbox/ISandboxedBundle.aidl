package com.github.michalbednarski.intentslab.sandbox;

import com.github.michalbednarski.intentslab.sandbox.ParcelableValue;

/**
 * Created by mb on 10.11.13.
 */
interface ISandboxedBundle {
    String[] getKeySet();

    boolean containsKey(String key);

    ParcelableValue get(String key);

    String getAsString(String key);

    Bundle getWrapped(String key);

    void put(String key, in ParcelableValue value);

    void putWrapped(String key, in Bundle wrappedValue);

    void remove(String key);

    Bundle getBundle();
}
