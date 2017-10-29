/*
 * IntentsLab - Android app for playing with Intents and Binder IPC
 * Copyright (C) 2014 Michał Bednarski
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.github.michalbednarski.intentslab.runas;

import com.github.michalbednarski.intentslab.BuildConfig;
import com.github.michalbednarski.intentslab.Utils;

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
    private HashMap<String, Integer> mArgNamesToIndexes;


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
                Object refArgument = typesNamesAndDefaults[i * 3];
                if (refArgument instanceof Class) {
                    refArguments[i] = (Class<?>) refArgument;
                } else {
                    refArguments[i] = Class.forName((String) refArgument);
                }

            }

            // Get method
            mMethod = mClass.getMethod(methodName, (Class<?>[]) refArguments);

            // If we're here - method exists
            mArgNamesToIndexes = new HashMap<>();
            mDefaultArgs = new Object[argCount];
            for (int i = 0; i < argCount; i++) {
                mArgNamesToIndexes.put((String) typesNamesAndDefaults[i * 3 + 1], i);
                mDefaultArgs[i] = typesNamesAndDefaults[i * 3 + 2];
            }
        } catch (NoSuchMethodException ignored) {
        } catch (ClassNotFoundException ignored) {}
        return this;
    }

    /**
     * Try finding method method variant in reflected class,
     * allowing method in class to have additional arguments between provided ones
     *
     * @param methodName Name of method to be found
     * @param typesNamesAndDefaults
     *          any amount of (in order, all required for each set)
     *           - Types (as class, used in reflection)
     *           - Names (used for {@link #invoke(Object, Object...)} call)
     *           - Default values
     */
    public CrossVersionReflectedMethod tryMethodVariantInexact(String methodName, Object... typesNamesAndDefaults) {
        // If we have already found an implementation, skip next checks
        if (mMethod != null) {
            return this;
        }

        int expectedArgCount = typesNamesAndDefaults.length / 3;

        for (Method method : mClass.getMethods()) {
            if (!methodName.equals(method.getName())) {
                continue;
            }

            // Matched name, try matching arguments
            // Get list of arguments for reflection call

            try {
                // These are for arguments provided to this method
                Class<?> expectedArgumentClass = null;
                int expectedArgumentI = 0;

                // This is for method arguments found through reflection
                int actualArgumentI = 0;

                // Parameters for method - we'll copy them to fields
                // when we're sure that this is right method
                HashMap<String, Integer> argNamesToIndexes = new HashMap<>();
                Object[] defaultArgs = new Object[method.getParameterTypes().length];

                // Iterate over actual method arguments
                for (Class<?> methodParam : method.getParameterTypes()) {
                    // Get expected argument if we haven't it cached
                    if (expectedArgumentClass == null && expectedArgumentI < expectedArgCount) {
                        Object refArgument = typesNamesAndDefaults[expectedArgumentI * 3];
                        if (refArgument instanceof Class) {
                            expectedArgumentClass = (Class<?>) refArgument;
                        } else {
                            expectedArgumentClass = Class.forName((String) refArgument);
                        }
                    }

                    // Check if this argument is expected one
                    if (methodParam == expectedArgumentClass) {
                        argNamesToIndexes.put((String) typesNamesAndDefaults[expectedArgumentI * 3 + 1], actualArgumentI);
                        defaultArgs[actualArgumentI] = typesNamesAndDefaults[expectedArgumentI * 3 + 2];

                        // Note this argument is passed
                        expectedArgumentI++;
                        expectedArgumentClass = null;
                    } else {
                        try {
                            defaultArgs[actualArgumentI] = Utils.getDefaultValueForPrimitveClass(methodParam);
                        } catch (Exception ignored) { /* Not primitive - leave null */ }
                    }

                    actualArgumentI++;
                }

                // Check if method has all requested arguments
                if (expectedArgumentI != expectedArgCount) {
                    continue;
                }

                // Export result if matched
                mMethod = method;
                mDefaultArgs = defaultArgs;
                mArgNamesToIndexes = argNamesToIndexes;
            } catch (ClassNotFoundException e) {
                // No such class on this system, probably okay
                if (BuildConfig.DEBUG) {
                    e.printStackTrace();
                }
            }
        }
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
