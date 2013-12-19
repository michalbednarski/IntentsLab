package com.github.michalbednarski.intentslab.sandbox;

/**
 * Created by mb on 10.11.13.
 */
interface ISandboxedBundle {
    String[] getKeySet();

    boolean containsKey(String key);

    String getAsString(String key);

    Bundle getWrapped(String key);

    void putWrapped(String key, in Bundle wrappedValue);

    void remove(String key);

    Bundle getBundle();
}
