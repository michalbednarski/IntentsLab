package com.github.michalbednarski.intentslab.bindservice.manager;

import android.os.Parcelable;

/**
* Created by mb on 02.10.13.
*/
public abstract class ServiceDescriptor implements Parcelable {
    abstract ConnectionManager getConnectionManager();

    public abstract String getTitle();

    abstract static class ConnectionManager {

        BindServiceManager.Helper mHelper;

        abstract void bind();

        abstract void unbind();

    }
}
