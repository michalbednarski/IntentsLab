package com.github.michalbednarski.intentslab.sandbox;

import com.github.michalbednarski.intentslab.sandbox.ISandboxedObject;
import com.github.michalbednarski.intentslab.sandbox.SandboxedMethod;
import com.github.michalbednarski.intentslab.sandbox.SandboxedMethodArguments;

/**
 * Created by mb on 30.09.13.
 */
interface IAidlInterface {
    String getInterfaceName();

    SandboxedMethod[] getMethods();

    Bundle invokeMethod(int methodNumber, in SandboxedMethodArguments remoteObjects, out Bundle outExtras);


}
