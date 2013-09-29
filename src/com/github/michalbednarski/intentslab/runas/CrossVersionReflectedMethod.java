package com.github.michalbednarski.intentslab.runas;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;

/**
 * Class wrapping reflection method and using named arguments for invocation
 *
 * Can have multiple variants of method and find one that actually exists
 */
public class CrossVersionReflectedMethod {

    private final Class<?> mClass;
    private Method mMethod = null;
    private Object[] mDefaultArgs;
    private final HashMap<String, Integer> mArgNamesToIndexes = new HashMap<String, Integer>();


    public CrossVersionReflectedMethod(Class<?> aClass) {
        mClass = aClass;
    }


    /**
     * Try finding method method variant in reflected class
     *
     * @param methodName Name of method to be found
     * @param typesNamesAndDefaults
     *          any amount of (in order, all required for each set)
     *           - Types (as class, used in reflection)
     *           - Names (used for {@link #invoke(Object, Object...)} call)
     *           - Default values
     */
    public CrossVersionReflectedMethod tryMethodVariant(String methodName, Object... typesNamesAndDefaults) {
        // If we have already found an implementation, skip next checks
        if (mMethod != null) {
            return this;
        }

        try {
            // Get list of arguments for reflection call
            int argCount = typesNamesAndDefaults.length / 3;
            Class<?>[] refArguments = new Class<?>[argCount];
            for (int i = 0; i < argCount; i++) {
                refArguments[i] = (Class<?>) typesNamesAndDefaults[i * 3];
            }

            // Get method
            mMethod = mClass.getMethod(methodName, (Class<?>[]) refArguments);

            // If we're here - method exists
            mDefaultArgs = new Object[argCount];
            for (int i = 0; i < argCount; i++) {
                mArgNamesToIndexes.put((String) typesNamesAndDefaults[i * 3 + 1], i);
                mDefaultArgs[i] = typesNamesAndDefaults[i * 3 + 2];
            }
        } catch (NoSuchMethodException ignored) {}
        return this;
    }

    /**
     * Invoke method
     *
     * @param receiver Object on which we call {@link java.lang.reflect.Method#invoke(Object, Object...)}
     * @param namesAndValues
     *          Any amount of argument name (as used in {@link #tryMethodVariant(String, Object...)} and value pairs
     */
    public Object invoke(Object receiver, Object ...namesAndValues) throws InvocationTargetException {
        if (mMethod == null) {
            throw new RuntimeException("Couldn't find method with matching signature");
        }
        Object[] args = mDefaultArgs.clone();
        for (int i = 0; i < namesAndValues.length; i += 2) {
            @SuppressWarnings("SuspiciousMethodCalls")
            Integer namedArgIndex = mArgNamesToIndexes.get(namesAndValues[i]);
            if (namedArgIndex != null) {
                args[namedArgIndex] = namesAndValues[i + 1];
            }
        }
        try {
            return mMethod.invoke(receiver, args);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
