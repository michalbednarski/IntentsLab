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

package com.github.michalbednarski.intentslab.editor;

/**
 * IntentEditorActivity that is used for interception
 *
 * This has following differences in behavior from normal IntentEditorActivity:
 * <ul>
 *     <li> Reads it's intent in it's raw form
 *     <li> Automatically detects componentType=ACTIVITY and method=startActivity(ForResult)
 *     <li> Allows returning result
 * </ul>
 */
public class IntentEditorInterceptedActivity extends IntentEditorActivity {
    @Override
    protected boolean isInterceptedIntent() {
        return true;
    }
}
