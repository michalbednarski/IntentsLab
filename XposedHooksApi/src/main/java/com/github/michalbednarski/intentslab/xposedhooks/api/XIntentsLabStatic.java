package com.github.michalbednarski.intentslab.xposedhooks.api;

/**
 * Class used to obtain instance of XIntentsLab
 * and to indicate existence of [IntentsLab]XposedHooksApi library in package
 */
public class XIntentsLabStatic {
    /**
     * Get XposedHooks module api
     *
     * Instance is created once this method is called, subsequent calls will return same object
     *
     * Returns null if XposedHooks module is not active
     */
    public static XIntentsLab getInstance() {
        return null;
    }
}
