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

import android.content.IIntentReceiver;
import android.content.Intent;
import android.os.Bundle;

/**
 * Class that provides android-version-independent {@link android.content.IIntentReceiver}
 */
public abstract class RealIIntentReceiver extends IIntentReceiver.Stub {

    public final void performReceive(
            Intent intent,
            int resultCode,
            String data,
            Bundle extras,
            boolean ordered,
            boolean sticky,
            int sendingUser
    ) {
        performReceive(intent, resultCode, data, extras, ordered);
    }

    public final void performReceive(
            Intent intent,
            int resultCode,
            String data,
            Bundle extras,
            boolean ordered,
            boolean sticky
    ) {
        performReceive(intent, resultCode, data, extras, ordered);
    }

    /**
     * Actual cross-android-version method that should be overridden
     */
    public abstract void performReceive(
            Intent intent,
            int resultCode,
            String data,
            Bundle extras,
            boolean ordered
    );
}
