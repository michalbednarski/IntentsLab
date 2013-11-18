package com.github.michalbednarski.intentslab.sandbox.remote;

import android.os.Bundle;
import android.os.RemoteException;
import com.github.michalbednarski.intentslab.sandbox.ISandboxedObject;
import com.github.michalbednarski.intentslab.sandbox.SandboxManager;
import com.github.michalbednarski.intentslab.sandbox.SandboxedClassField;
import com.github.michalbednarski.intentslab.sandbox.SandboxedType;
import com.github.michalbednarski.intentslab.valueeditors.object.GettersInvoker;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;

/**
 * Created by mb on 08.10.13.
 */
class SandboxedObjectImpl extends ISandboxedObject.Stub {


    private final ClassLoader mClassLoader;
    private Object mObject;
    private GettersInvoker mGettersInvoker;

    SandboxedObjectImpl(Object object, ClassLoader classLoader) {
        mClassLoader = classLoader;
        mObject = object;
        mGettersInvoker = new GettersInvoker(mObject);
    }

    @Override
    public Bundle getWrappedObject() throws RemoteException {
        return SandboxManager.wrapObject(mObject);
    }

    @Override
    public SandboxedClassField[] getNonStaticFields() throws RemoteException {
        ArrayList<SandboxedClassField> fieldsList = new ArrayList<SandboxedClassField>();
        final Class<?> aClass = mObject.getClass();

        filterFieldsAndAddToList(fieldsList, aClass.getFields(), 0);
        filterFieldsAndAddToList(fieldsList, aClass.getDeclaredFields(), Modifier.PRIVATE);

        return fieldsList.toArray(new SandboxedClassField[fieldsList.size()]);
    }


    /**
     * @param fieldsList Output list
     * @param fields Array of fields to filter
     * @param expectedModifiers Values of PRIVATE and STATIC modifiers matching filter ({@link java.lang.reflect.Modifier})
     */
    private static void filterFieldsAndAddToList(ArrayList<SandboxedClassField> fieldsList, Field[] fields, int expectedModifiers) {
        for (Field field : fields) {
            final int modifiers = field.getModifiers();
            if ((modifiers & (Modifier.STATIC | Modifier.PRIVATE)) == expectedModifiers) { // Match modifiers
                fieldsList.add(new SandboxedClassField(
                        new SandboxedType(field.getType()),
                        field.getName(),
                        modifiers
                ));
            }
        }
    }

    /**
     * Get field by name and make it accessible
     */
    private Field getField(String name) {
        final Class<?> aClass = mObject.getClass();
        try {
            return aClass.getField(name);
        } catch (NoSuchFieldException e) {
            try {
                final Field field = aClass.getDeclaredField(name);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException e1) {
                throw new RuntimeException(e1);
            }
        }
    }

    @Override
    public Bundle getFieldValue(String fieldName) throws RemoteException {
        try {
            return SandboxManager.wrapObject(getField(fieldName).get(mObject));
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getFieldValueAsString(String fieldName) throws RemoteException {
        try {
            return String.valueOf(getField(fieldName).get(mObject));
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setFieldValue(String fieldName, Bundle value) throws RemoteException {
        value.setClassLoader(mClassLoader);
        try {
            getField(fieldName).set(mObject, SandboxManager.unwrapObject(value));
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean gettersExits() throws RemoteException {
        return mGettersInvoker.gettersExist();
    }

    @Override
    public CharSequence getGetterValues() throws RemoteException {
        return mGettersInvoker.getGettersValues();
    }
}
