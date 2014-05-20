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

package com.github.michalbednarski.intentslab.xposedhooks.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

public class QueryPermissionsReceiver extends BroadcastReceiver {
    public static final String RESULT_EXTRA_ALLOWED_UIDS = "XIntentsLab.allowedUids";

    @Override
    public void onReceive(Context context, Intent intent) {
        AllowedAppsDb allowedAppsDb = AllowedAppsDb.getInstance(context);
        Bundle resultExtras = new Bundle();
        resultExtras.putIntArray(
                RESULT_EXTRA_ALLOWED_UIDS,
                allowedAppsDb.getAllowedUids()
        );
        setResultExtras(resultExtras);
        allowedAppsDb.saveIfNeeded();
    }
}
