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

package com.github.michalbednarski.intentslab.valueeditors.object;

import android.content.Context;
import com.github.michalbednarski.intentslab.Utils;
import com.github.michalbednarski.intentslab.valueeditors.framework.EditorLauncher;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;

/**
 * Created by mb on 16.11.13.
 */
class LocalObjectEditorHelper implements EditorLauncher.EditorLauncherCallback, ObjectEditorHelper {

    final private Object mObject;
    private final ObjectEditorHelperCallback mObjectEditorHelperCallback;
    private HashMap<String, InlineValueEditor> mValueEditors = new HashMap<String, InlineValueEditor>();
    private boolean mHasNonPublicFields = false;
    final private GettersInvoker mGettersInvoker;

    LocalObjectEditorHelper(Context context, Object editedObject, EditorLauncher editorLauncher, ObjectEditorHelperCallback objectEditorHelperCallback) {
        mContext = context;
        mEditorLauncher = editorLauncher;
        mObject = editedObject;
        mObjectEditorHelperCallback = objectEditorHelperCallback;
        mGettersInvoker = new GettersInvoker(editedObject);

        // Get class
        Class<?> aClass = editedObject.getClass();

        // Prepare field editors
        for (Class fieldsClass = aClass; fieldsClass != null; fieldsClass = fieldsClass.getSuperclass()) {
            for (final Field field : fieldsClass.getDeclaredFields()) {
                int modifiers = field.getModifiers();


                if (
                        !Modifier.isStatic(modifiers) && // Not static field
                                !mValueEditors.containsKey(field.getName()) // Not scanned already
                        ) {




                    // Set flag if there are non-public fields
                    boolean isPublic = Modifier.isPublic(modifiers);
                    if (!isPublic) {
                        mHasNonPublicFields = true;
                        field.setAccessible(true);
                    }

                    // Create an editor object
                    InlineValueEditor editor = new InlineValueEditor(
                            field.getType(),
                            field.getName(),
                            !isPublic,
                            new InlineValueEditor.ValueAccessors() {
                                @Override
                                public Object getValue() {
                                    try {
                                        return field.get(mObject);
                                    } catch (IllegalAccessException e) {
                                        throw new RuntimeException(e);
                                    }
                                }

                                @Override
                                public void setValue(Object newValue) {
                                    try {
                                        field.set(mObject, newValue);
                                    } catch (IllegalAccessException e) {
                                        throw new RuntimeException(e);
                                    }
                                    if (mObjectEditorHelperCallback != null) {
                                        mObjectEditorHelperCallback.onModified();
                                    }
                                }

                                @Override
                                public void startEditor() {
                                    mEditorLauncher.launchEditor(field.getName(), field.getName(), getValue(), field.getType());
                                }
                            }
                    );

                    // Register editor
                    mValueEditors.put(field.getName(), editor);


                }
            }
        }
    }

    private final Context mContext;
    // Editing fields
    EditorLauncher mEditorLauncher;

    @Override
    public void onEditorResult(String key, Object newValue) {
        try {
            final InlineValueEditor valueEditor = mValueEditors.get(key);
            valueEditor.getAccessors().setValue(newValue);
            valueEditor.updateTextOnButton();
        } catch (Exception e) {
            e.printStackTrace();
            Utils.toastException(mContext, "Field.set", e);
        }
    }

    //
    @Override
    public InlineValueEditor[] getInlineValueEditors() {
        return mValueEditors.values().toArray(new InlineValueEditor[mValueEditors.size()]);
    }

    @Override
    public boolean hasNonPublicFields() {
        return mHasNonPublicFields;
    }

    @Override
    public Object getObject() {
        return mObject;
    }

    @Override
    public CharSequence getGetterValues() {
        return mGettersInvoker.getGettersValues();
    }
}
