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

public class IntentEditorConstants {

	public static final int ACTIVITY = 0;
	public static final int BROADCAST = 1;
	public static final int SERVICE = 2;
    public static final int RESULT = 3;

	public static final int ACTIVITY_METHOD_STARTACTIVITY = 0;
	public static final int ACTIVITY_METHOD_STARTACTIVITYFORRESULT = 1;

	public static final int BROADCAST_METHOD_SENDBROADCAST = 0;
	public static final int BROADCAST_METHOD_SENDORDEREDBROADCAST = 1;
	public static final int BROADCAST_METHOD_SENDSTICKYBROADCAST = 2;

	public static final int SERVICE_METHOD_STARTSERVICE = 0;
	public static final int SERVICE_METHOD_BINDSERVICE = 1;
}
