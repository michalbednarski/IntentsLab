package com.github.michalbednarski.intentslab.sandbox;

import com.github.michalbednarski.intentslab.sandbox.SandboxedClassField;
import com.github.michalbednarski.intentslab.sandbox.SandboxedObject;

/**
 * Created by mb on 30.09.13.
 */
interface ISandboxedObject {
    SandboxedObject getWrappedObject();

    SandboxedClassField[] getNonStaticFields();

    SandboxedObject getFieldValue(String fieldName);

    String getFieldValueAsString(String fieldName);

    void setFieldValue(String fieldName, in SandboxedObject value);

    boolean gettersExits();

    CharSequence getGetterValues();
}
