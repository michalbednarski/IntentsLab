package com.github.michalbednarski.intentslab.xposedhooks.api;

/**
 * Base interface for object trackers
 */
public interface BaseTracker {
    /**
     * Set listener for updates
     *
     * If tracker has pending update this will immediately invoke listener.
     * Tracker gets pending update when update tried to occur but listener wasn't set
     *
     * @param alwaysTriggerUpdate If true listener will be always called upon setting
     */
    void setUpdateListener(TrackerUpdateListener listener, boolean alwaysTriggerUpdate);

    void clearUpdateListener();
}
