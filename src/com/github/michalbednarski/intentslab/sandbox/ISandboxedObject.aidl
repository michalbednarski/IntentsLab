package com.github.michalbednarski.intentslab.sandbox;

import com.github.michalbednarski.intentslab.sandbox.SandboxedClassField;

/**
 * Created by mb on 30.09.13.
 */
interface ISandboxedObject {
    Bundle getWrappedObject();

    SandboxedClassField[] getNonStaticFields();

    Bundle getFieldValue(String fieldName);

    String getFieldValueAsString(String fieldName);

    void setFieldValue(String fieldName, in Bundle value);

    boolean gettersExits();

    CharSequence getGetterValues();
}
