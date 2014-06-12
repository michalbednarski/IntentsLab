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

/**
 * Sandboxed equivalent of {@link java.lang.reflect.Field}
 */
public class SandboxedClassField implements Parcelable {
    public SandboxedType type;
    public String name;
    public int modifiers;


    public SandboxedClassField(SandboxedType type, String name, int modifiers) {
        this.type = type;
        this.name = name;
        this.modifiers = modifiers;
    }


    /*
     *
     * Parcelable
     *
     */

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        type.writeToParcel(dest, flags);
        dest.writeString(name);
        dest.writeInt(modifiers);
    }

    public static final Creator<SandboxedClassField> CREATOR = new Creator<SandboxedClassField>() {
        @Override
        public SandboxedClassField createFromParcel(Parcel source) {
            return new SandboxedClassField(
                    SandboxedType.CREATOR.createFromParcel(source),
                    source.readString(),
                    source.readInt()
            );
        }

        @Override
        public SandboxedClassField[] newArray(int size) {
            return new SandboxedClassField[size];
        }
    };
}
