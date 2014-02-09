package com.github.michalbednarski.intentslab.sandbox;

import com.github.michalbednarski.intentslab.sandbox.SandboxedObject;

/**
 * Created by mb on 10.11.13.
 */
interface ISandboxedBundle {
    String[] getKeySet();

    boolean containsKey(String key);

    String getAsString(String key);

    SandboxedObject getWrapped(String key);

    void putWrapped(String key, in SandboxedObject wrappedValue);

    void remove(String key);

    Bundle getBundle();
}
