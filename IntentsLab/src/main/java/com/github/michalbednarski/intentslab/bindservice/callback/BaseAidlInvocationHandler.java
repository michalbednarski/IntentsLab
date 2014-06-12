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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * Helper class to bridge custom api used by generated code to not cross class loaders
 * to proper method arguments
 */
abstract class BaseAidlInvocationHandler implements InvocationHandler {

    static final int EXTRA_FLAGS = 0;
    static final int EXTRA_RETURN_TYPE = 1;
    static final int EXTRA_METHOD_NAME = 2;

    static final int EXTRAS_LENGTH = 3;


    /**
     * Helper class to bridge custom api used by generated code to not cross class loaders
     */
    @Override
    public final Object invoke(Object ucExtras, Method unused, Object[] args) throws Throwable {
        Object[] extras = (Object[]) ucExtras;
        return invokeAidl(
                args,
                (String) extras[EXTRA_METHOD_NAME],
                (Class) extras[EXTRA_RETURN_TYPE],
                (((Integer) extras[EXTRA_FLAGS]) & 1) != 0
        );
    }

    /**
     * Note: this method will be called from binder thread pool, not UI thread
     */
    protected abstract Object invokeAidl(Object[] args, String methodName, Class returnType, boolean oneWay);
}
