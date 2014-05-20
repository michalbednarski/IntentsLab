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

package com.github.michalbednarski.intentslab.sandbox;

import android.os.Parcel;
import android.os.Parcelable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * Created by mb on 30.09.13.
 */
public class SandboxedMethod implements Parcelable {
    public final String name;
    public final String declaration;
    public final SandboxedType[] argumentTypes;


    public SandboxedMethod(Method method) {
        name = method.getName();
        declaration = method.toString();
        argumentTypes = SandboxedType.wrapClassesArray(method.getParameterTypes());
    }

    public SandboxedMethod(Constructor method) {
        name = method.getName();
        declaration = method.toString();
        argumentTypes = SandboxedType.wrapClassesArray(method.getParameterTypes());
    }

    private SandboxedMethod(Parcel source) {
        name = source.readString();
        declaration = source.readString();
        argumentTypes = source.createTypedArray(SandboxedType.CREATOR);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeString(declaration);
        dest.writeTypedArray(argumentTypes, 0);
    }

    public static final Creator<SandboxedMethod> CREATOR = new Creator<SandboxedMethod>() {
        @Override
        public SandboxedMethod createFromParcel(Parcel source) {
            return new SandboxedMethod(source);
        }

        @Override
        public SandboxedMethod[] newArray(int size) {
            return new SandboxedMethod[size];
        }
    };
}
