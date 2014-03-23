package com.github.michalbednarski.intentslab.sandbox;

import com.github.michalbednarski.intentslab.sandbox.InvokeMethodResult;
import com.github.michalbednarski.intentslab.sandbox.SandboxedObject;
import com.github.michalbednarski.intentslab.sandbox.SandboxedMethod;

/**
 * Created by mb on 30.09.13.
 */
interface IAidlInterface {
    String getInterfaceName();

    SandboxedMethod[] getMethods();

    InvokeMethodResult invokeMethod(int methodNumber, in SandboxedObject[] arguments);


}
