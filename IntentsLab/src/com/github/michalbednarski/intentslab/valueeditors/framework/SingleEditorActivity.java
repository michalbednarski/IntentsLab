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
