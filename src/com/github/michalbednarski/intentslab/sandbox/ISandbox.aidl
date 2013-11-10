package com.github.michalbednarski.intentslab.sandbox;

import com.github.michalbednarski.intentslab.sandbox.ClassLoaderDescriptor;
import com.github.michalbednarski.intentslab.sandbox.IAidlInterface;

/**
 * Created by mb on 30.09.13.
 */
interface ISandbox {
    IAidlInterface queryInterface(IBinder binder, in ClassLoaderDescriptor fromPackage);
}
