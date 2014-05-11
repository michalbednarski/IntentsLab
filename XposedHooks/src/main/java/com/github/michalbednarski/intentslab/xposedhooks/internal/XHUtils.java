package com.github.michalbednarski.intentslab.xposedhooks.internal;

import android.os.IBinder;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;

/**
 * Various utilities that don't rely on XposedBridge library
 */
public class XHUtils {
    /**
     * Proxy interface so it can be used from different ClassLoader
     *
     * This doesn't rely on XposedBridge library
     */
    static Object castInterface(final Object object, Class<?> targetInterface) {
        if (object == null || targetInterface.isInstance(object)) {
            return object;
        } else {
            // Get class loaders
            final ClassLoader sourceClassLoader = object.getClass().getClassLoader();
            final ClassLoader targetClassLoader = targetInterface.getClassLoader();

            // Create proxy interface
            return Proxy.newProxyInstance(
                    targetClassLoader,
                    new Class<?>[]{targetInterface},
                    new InvocationHandler() {
                        @Override
                        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                            // Cast parameters
                            Class<?>[] parameterTypes = method.getParameterTypes();
                            for (int i = 0; i < parameterTypes.length; i++) {
                                Class<?> aClass = parameterTypes[i];
                                Class<?> paramTargetInterface = getTargetTypeIfMayCast(aClass, sourceClassLoader);
                                if (paramTargetInterface != null) {
                                    parameterTypes[i] = paramTargetInterface;
                                    args[i] = castInterface(args[i], paramTargetInterface);
                                }
                            }

                            try {
                                // Invoke method
                                Object result = object
                                        .getClass()
                                        .getMethod(method.getName(), parameterTypes)
                                        .invoke(object, args);

                                // Cast result if needed
                                Class<?> returnType = method.getReturnType();
                                //Class<?> targetReturnType = getTargetTypeIfMayCast(returnType, targetClassLoader);
                                //if (targetReturnType != null) {
                                    return convertObjectAcrossClassLoaders(result, returnType);
                                //} else {
                                //    return result;
                                //}
                            } catch (Throwable e) {
                                // If it failed, check if this is one of supports* methods and return false if so
                                if (
                                        method.getParameterTypes().length == 0 &&
                                                method.getReturnType() == boolean.class &&
                                                method.getName().startsWith("supports")
                                        ) {
                                    return false;
                                } else {
                                    // Otherwise rethrow
                                    throw e;
                                }
                            }
                        }
                    }
            );
        }
    }

    private static Object convertObjectAcrossClassLoaders(Object source, Class<?> targetClass) {
        if (source == null || targetClass.isInstance(source) || targetClass.isPrimitive()) {
            return source;
        } else if (targetClass.isInterface()) {
            return castInterface(source, targetClass);
        } else if (targetClass.isArray()) {
            final Object[] sourceArray = (Object[]) source;
            final int length = sourceArray.length;
            final Class<?> targetComponentType = targetClass.getComponentType();
            final Object[] targetArray = (Object[]) Array.newInstance(targetComponentType, length);
            for (int i = 0; i < length; i++) {
                targetArray[i] = convertObjectAcrossClassLoaders(sourceArray[i], targetComponentType);
            }
            return targetArray;
        } else {
            final Object targetObject;
            try {
                targetObject = targetClass.newInstance();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            Class<?> sourceClass = source.getClass();
            for (Field targetField : targetClass.getFields()) {
                // Only copy public fields that are not static nor final
                if ((targetField.getModifiers() & (Modifier.STATIC | Modifier.FINAL | Modifier.PUBLIC)) != Modifier.PUBLIC) {
                    continue;
                }

                try {
                    final Field sourceField = sourceClass.getField(targetField.getName());
                    final Class<?> sourceType = sourceField.getType();
                    final Class<?> targetType = targetField.getType();

                    final Object sourceValue = sourceField.get(source);
                    final Object targetValue;

                    if (sourceType == targetType) {
                        targetValue = sourceValue;
                    } else if (sourceType.getName().equals(targetType.getName())) {
                        targetValue = convertObjectAcrossClassLoaders(sourceValue, targetType);
                    } else {
                        throw new Exception("Unmatched types for field " + targetField.getName());
                    }
                    targetField.set(targetObject, targetValue);
                } catch (Exception e) {
                    // Might happen if versions are out-of-sync
                    e.printStackTrace();
                }
            }
            return targetObject;
        }
    }

    private static Class<?> getTargetTypeIfMayCast(Class<?> aClass, ClassLoader toClassLoader) {
        if (aClass.isInterface() &&
                (aClass.getName().startsWith("com.github.michalbednarski.intentslab.xposedhooks.api.") ||
                aClass.getName().startsWith("[Lcom/github/michalbednarski/intentslab/xposedhooks/api/"))
                ) {
            try {
                return toClassLoader.loadClass(aClass.getName());
            } catch (ClassNotFoundException e) {
                e.printStackTrace(); // Shouldn't happen
                // Fall through
            }
        }
        return null;
    }

    /**
     * Get SystemService interface
     *
     * This doesn't rely on XposedBridge library
     */
    public static IInternalInterface getSystemInterface() {
        try {
            return IInternalInterface.Stub.asInterface(
                    (IBinder) Class.forName("android.os.ServiceManager")
                            .getMethod("getService", String.class)
                            .invoke(null, ModuleInit.SYSTEM_SERVICE_NAME)
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Get stack trace of hooked method
     *
     * At index 0 is caller of hooked method
     */
    public static StackTraceElement[] getHookedMethodStackTrace() {
        StackTraceElement[] stackTrace = new Throwable().getStackTrace();

        // Find handleHookedMethod call index
        int i = 0;
        while (
                !"de.robv.android.xposed.XposedBridge".equals(stackTrace[i].getClassName()) ||
                !"handleHookedMethod".equals(stackTrace[i].getMethodName())
                ) {
            i++;
        }

        i++; // Method itself (replaced with native one by Xposed)

        // Get slice of stack trace
        StackTraceElement[] newStackTrace = new StackTraceElement[stackTrace.length - i];
        System.arraycopy(stackTrace, i, newStackTrace, 0, stackTrace.length - i);
        return newStackTrace;
    }

}
