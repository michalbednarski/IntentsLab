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

package com.github.michalbednarski.intentslab.valueeditors.framework;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.widget.Toast;

import com.github.michalbednarski.intentslab.sandbox.SandboxedObject;

/**
 * An activity hosting {@link ValueEditorFragment}
 *
 * For internal use by {@link EditorLauncher}
 */
public class SingleEditorActivity extends FragmentActivity {

    final static String EXTRA_FRAGMENT_CLASS_NAME = "SingleEditorActivity.editorClassName";
    final static String EXTRA_ECHOED_KEY = "SingleEditorActivity.echoedKey";
    final static String EXTRA_RESULT = "SingleEditorActivity.result";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            Intent intent = getIntent();
            ValueEditorFragment fragment;
            try {

                fragment = (ValueEditorFragment)
                        Class.forName(intent.getStringExtra(EXTRA_FRAGMENT_CLASS_NAME))
                        .newInstance();
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Invalid fragment", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(android.R.id.content, fragment)
                    .commit();
        }
    }

    private ValueEditorFragment getEditorFragment() {
        return (ValueEditorFragment) getSupportFragmentManager().findFragmentById(android.R.id.content);
    }

    @Override
    public void onBackPressed() {
        ValueEditorFragment editorFragment = getEditorFragment();
        if (editorFragment.mModified) {
            Object editorResult = editorFragment.getEditorResult();
            SandboxedObject sandboxedResult;
            if (editorFragment.isEditorResultSandboxed()) {
                sandboxedResult = (SandboxedObject) editorResult;
            } else {
                sandboxedResult = new SandboxedObject(editorResult);
            }
            Intent resultIntent = new Intent();
            resultIntent.putExtra(EXTRA_ECHOED_KEY, getIntent().getStringExtra(EXTRA_ECHOED_KEY));
            resultIntent.putExtra(EXTRA_RESULT, sandboxedResult);
            setResult(RESULT_OK, resultIntent);
        } else {
            setResult(RESULT_CANCELED);
        }
        finish();
    }
}
