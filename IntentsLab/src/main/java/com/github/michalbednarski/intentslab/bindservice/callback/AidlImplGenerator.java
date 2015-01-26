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

package com.github.michalbednarski.intentslab.bindservice.callback;

import android.os.Binder;
import android.os.IBinder;
import android.os.Parcel;

import com.github.michalbednarski.intentslab.Utils;
import com.google.dexmaker.Code;
import com.google.dexmaker.Comparison;
import com.google.dexmaker.DexMaker;
import com.google.dexmaker.FieldId;
import com.google.dexmaker.Label;
import com.google.dexmaker.Local;
import com.google.dexmaker.MethodId;
import com.google.dexmaker.TypeId;

import java.io.File;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Helper for generating aidl implementations
 */
class AidlImplGenerator {

    private static final TypeId<ThreadLocal> THREAD_LOCAL_TYPE_ID = TypeId.get(ThreadLocal.class);
    private static final TypeId<Parcel> PARCEL_TYPE_ID = TypeId.get(Parcel.class);
    private static final TypeId<InvocationHandler> INVOCATION_HANDLER_TYPE_ID = TypeId.get(InvocationHandler.class);


    private final File mCacheDir;



    public AidlImplGenerator(File cacheDir) {
        mCacheDir = cacheDir;
    }

    public IBinder makeAidlImpl(Class<? extends Binder> stubClass, BaseAidlInvocationHandler handler) {
        try {
            // "b" suffix is only to avoid mis-detection by SandboxedObject aidl wrapping
            final String implClassName = "generated/AidlImpl_" + stubClass.getName().replace('.', '/') + "b";
            return (IBinder)
                    generateImplClass(stubClass, implClassName)
                    .generateAndLoad(stubClass.getClassLoader(), mCacheDir)
                    .loadClass(implClassName)
                    .getConstructor(InvocationHandler.class)
                    .newInstance(handler);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     *
     * Generate implementation class for aidl generated stub
     *
     */
    private DexMaker generateImplClass(Class<? extends Binder> stubClass, String implClassName) {
        DexMaker dexMaker = new DexMaker();

        // Declare class
        // public class GeneratedImpl
        final TypeId<Object> implClass = TypeId.get("L" + implClassName + ";");
        dexMaker.declare(
                implClass,
                "generated-file",
                Modifier.PUBLIC | Modifier.FINAL,
                TypeId.get(stubClass)
        );


        // private final ThreadLocal<Boolean> mFlags;
        final FieldId<Object, ThreadLocal> mFlags = implClass.getField(THREAD_LOCAL_TYPE_ID, "mOneWay");
        dexMaker.declare(mFlags, Modifier.PRIVATE | Modifier.FINAL, null);

        // private final mInvocationHandler;
        final FieldId<Object, InvocationHandler> mInvocationHandler = implClass.getField(INVOCATION_HANDLER_TYPE_ID, "mInvocationHandler");
        dexMaker.declare(mInvocationHandler, Modifier.PRIVATE | Modifier.FINAL, null);

        // Constructor (<init>(Map invocationHandler))
        {
            final MethodId<Object, Void> ctor = implClass.getConstructor(INVOCATION_HANDLER_TYPE_ID);

            // Find superclass constructor
            MethodId<? extends Binder, Void> ctorWithoutArgs;
            try {
                stubClass.getConstructor(); // This might throw
                ctorWithoutArgs = TypeId.get(stubClass).getConstructor();
            } catch (NoSuchMethodException e) {
                ctorWithoutArgs = TypeId.get(Binder.class).getConstructor();
            }


            final Code code = dexMaker.declare(ctor, Modifier.PUBLIC);
            final Local aThis = code.getThis(implClass);

            // mFlags = new ThreadLocal();
            final Local<ThreadLocal> flagsTL = code.newLocal(THREAD_LOCAL_TYPE_ID);
            code.newInstance(flagsTL, THREAD_LOCAL_TYPE_ID.getConstructor());
            code.iput(mFlags, aThis, flagsTL);

            // mInvocationHandler = invocationHandler
            code.iput(mInvocationHandler, aThis, code.getParameter(0, INVOCATION_HANDLER_TYPE_ID));

            // super()
            code.invokeDirect((MethodId) ctorWithoutArgs, null, aThis);

            code.returnVoid();
        }


        // onTransact
        {
            final MethodId<Object, Boolean> onTransact = implClass.getMethod(
                    TypeId.BOOLEAN,
                    "onTransact",
                    TypeId.INT,
                    PARCEL_TYPE_ID,
                    PARCEL_TYPE_ID,
                    TypeId.INT
            );
            final Code code = dexMaker.declare(onTransact, Modifier.PUBLIC);

            // Declare locals
            final Local<ThreadLocal> flagsTL = code.newLocal(THREAD_LOCAL_TYPE_ID);
            final Local<Integer> flagsBoxed = code.newLocal(TypeId.get(Integer.class));
            final Local<Boolean> result = code.newLocal(TypeId.BOOLEAN);

            // ThreadLocal<Boolean> flagsTL = mFlags;
            code.iget(mFlags, flagsTL, code.getThis(implClass));

            // flagsTL.set(flags)
            final Local<Integer> flags = code.getParameter(3, TypeId.INT);
            code.invokeStatic(
                    TypeId.get(Integer.class).getMethod(
                            TypeId.get(Integer.class),
                            "valueOf",
                            TypeId.INT
                    ),
                    flagsBoxed,
                    flags
            );
            code.invokeVirtual(
                    THREAD_LOCAL_TYPE_ID.getMethod(
                            TypeId.VOID,
                            "set",
                            TypeId.OBJECT
                    ),
                    null,
                    flagsTL,
                    flagsBoxed
            );

            // boolean result = super.onTransact(...)
            code.invokeSuper(
                    onTransact,
                    result,
                    code.getThis(implClass),
                    code.getParameter(0, TypeId.INT),
                    code.getParameter(1, PARCEL_TYPE_ID),
                    code.getParameter(2, PARCEL_TYPE_ID),
                    code.getParameter(3, TypeId.INT)
            );

            // flagsTL.remove()
            code.invokeVirtual(
                    THREAD_LOCAL_TYPE_ID.getMethod(TypeId.VOID, "remove"),
                    null,
                    flagsTL
            );

            // return result;
            code.returnValue(result);
        }

        // Abstract methods impl
        for (Method baseMethod : stubClass.getMethods()) {
            if ((baseMethod.getModifiers() & Modifier.ABSTRACT) != 0) {
                // Convert parameters for DexMaker
                final Class<?>[] reflectionParameters = baseMethod.getParameterTypes();
                TypeId<?>[] parameters = new TypeId[reflectionParameters.length];
                for (int i = 0; i < reflectionParameters.length; i++) {
                    parameters[i] = TypeId.get(reflectionParameters[i]);
                }


                // Begin method impl
                final Class<?> returnClass = baseMethod.getReturnType();
                final TypeId<?> returnType = TypeId.get(returnClass);
                final MethodId<Object, ?> method1 = implClass.getMethod(returnType, baseMethod.getName(), parameters);
                final Code code = dexMaker.declare(method1, Modifier.PUBLIC);


                // Allocate locals
                final Local<Object> aThis = code.getThis(implClass);
                final Local<Integer> integerLocal = code.newLocal(TypeId.INT);
                final Local<Object[]> extras = code.newLocal(TypeId.get(Object[].class));
                final Local<Object> objectLocal = code.newLocal(TypeId.OBJECT);
                final Local<ThreadLocal> flagsTL = code.newLocal(THREAD_LOCAL_TYPE_ID);
                final Local<Object[]> args = code.newLocal(TypeId.get(Object[].class));
                final Local<InvocationHandler> invocationHandler = code.newLocal(INVOCATION_HANDLER_TYPE_ID);

                // Prepare to unbox result at end
                // result is allocated only if this isn't void method
                final boolean isVoidMethod = returnClass == Void.TYPE;
                Local boxedResultCasted = null;
                MethodId unboxResultMethod = null;
                Local unboxedResult = null;
                if (!isVoidMethod) {
                    // Return value exist, try unboxing it
                    Class<?> resultBoxedClass = Utils.toWrapperClass(returnClass);
                    if (resultBoxedClass != returnClass) {
                        unboxedResult = code.newLocal(returnType);
                        boxedResultCasted = code.newLocal(TypeId.get(resultBoxedClass));
                        // e.g. boxedResult.intValue()
                        unboxResultMethod = TypeId.get(resultBoxedClass).getMethod(returnType, Utils.afterLastDot(returnClass.getName()) + "Value");

                    }
                }

                // Prepare null local
                final Local<Object> aNull = code.newLocal(TypeId.OBJECT);
                code.loadConstant(aNull, null);

                // Object[] extras = new Object[EXTRAS_LENGTH]
                code.loadConstant(integerLocal, BaseAidlInvocationHandler.EXTRAS_LENGTH);
                code.newArray(extras, integerLocal);

                // extras[EXTRA_FLAGS] = mFlags.get()
                code.iget(mFlags, flagsTL, aThis);
                code.invokeVirtual(
                        THREAD_LOCAL_TYPE_ID.getMethod(TypeId.OBJECT, "get"),
                        objectLocal,
                        flagsTL
                );
                code.loadConstant(integerLocal, BaseAidlInvocationHandler.EXTRA_FLAGS);
                code.aput(extras, integerLocal, objectLocal);

                // extra[EXTRA_RETURN_TYPE] = baseMethod.getReturnType()
                code.loadConstant(objectLocal, returnClass == Void.TYPE ? null : returnClass);
                code.loadConstant(integerLocal, BaseAidlInvocationHandler.EXTRA_RETURN_TYPE);
                code.aput(extras, integerLocal, objectLocal);

                // extra[EXTRA_METHOD_NAME] = baseMethod.getName()
                code.loadConstant(objectLocal, baseMethod.getName());
                code.loadConstant(integerLocal, BaseAidlInvocationHandler.EXTRA_METHOD_NAME);
                code.aput(extras, integerLocal, objectLocal);

                // Object[] args = ...
                code.loadConstant(integerLocal, parameters.length);
                code.newArray(args, integerLocal);

                for (int i = 0; i < parameters.length; i++) {
                    final Class<?> paramClass = reflectionParameters[i];
                    final Class<?> wrapperClass = Utils.toWrapperClass(paramClass);

                    // Get parameter
                    Local<?> param = code.getParameter(i, parameters[i]);

                    // Box if needed
                    if (paramClass != wrapperClass) {
                        // BoxClass.valueOf() (eg. Integer.valueOf())
                        code.invokeStatic(
                                TypeId.get(wrapperClass).getMethod(
                                        TypeId.get(wrapperClass),
                                        "valueOf",
                                        parameters[i]
                                ),
                                objectLocal,
                                param
                        );

                        param = objectLocal;
                    }

                    // Put in array
                    code.loadConstant(integerLocal, i);
                    code.aput(args, integerLocal, param);
                }

                // Copy mInvocationHandler to local variable
                code.iget(mInvocationHandler, invocationHandler, aThis);

                // return mInvocationHandler.invoke(extras, null, args) // with unboxing result
                code.invokeInterface(
                        INVOCATION_HANDLER_TYPE_ID.getMethod(
                                TypeId.OBJECT,
                                "invoke",
                                TypeId.OBJECT,
                                TypeId.get(Method.class),
                                TypeId.get(Object[].class)
                        ),
                        isVoidMethod ? null : objectLocal,
                        invocationHandler,
                        extras,
                        aNull,
                        args
                );
                if (!isVoidMethod) {
                    // Forward return value
                    if (unboxResultMethod != null) {
                        // Unboxing needed

                        final Label boxedResultIsNullLabel = new Label();
                        code.compare(Comparison.EQ, boxedResultIsNullLabel, objectLocal, aNull);

                        // If boxed result returned by handler is not null
                        code.cast(boxedResultCasted, objectLocal);
                        code.invokeVirtual(unboxResultMethod, unboxedResult, boxedResultCasted);
                        code.returnValue(unboxedResult);

                        // Boxed result returned by handler is null, return default
                        code.mark(boxedResultIsNullLabel);
                        code.loadConstant(unboxedResult, Utils.getDefaultValueForPrimitveClass(returnClass));
                        code.returnValue(unboxedResult);
                    } else {
                        // No unboxing needed, just return value
                        code.returnValue(objectLocal);
                    }
                } else {
                    // Void method
                    code.returnVoid();
                }
            }
        }

        return dexMaker;
    }


}
