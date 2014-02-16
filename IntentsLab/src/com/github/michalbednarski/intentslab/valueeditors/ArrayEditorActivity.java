package com.github.michalbednarski.intentslab.valueeditors;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import com.github.michalbednarski.intentslab.R;
import com.github.michalbednarski.intentslab.editor.BundleAdapter;
import com.github.michalbednarski.intentslab.valueeditors.framework.Editor;
import com.github.michalbednarski.intentslab.valueeditors.framework.EditorLauncher;

import java.lang.reflect.Array;

/**
 * Editor for primitive and object arrays
 */
public class ArrayEditorActivity extends FragmentActivity implements EditorLauncher.EditorLauncherCallback, AdapterView.OnItemClickListener {
    private static final String STATE_MODIFIED_ARRAY = "ArrEdAc.modified-array"; // mArray if was modified

    private Object mArray;
    private boolean mModified = false;
    private EditorLauncher mEditorLauncher;
    private final Adapter mAdapter = new Adapter();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        if (savedInstanceState != null && savedInstanceState.containsKey(STATE_MODIFIED_ARRAY)) {
            mModified = true;
            mArray = savedInstanceState.get(STATE_MODIFIED_ARRAY);
        } else {
            mArray = getIntent().getExtras().get(Editor.EXTRA_VALUE);
        }

        ListView listView = new ListView(this);
        listView.setAdapter(mAdapter);
        listView.setOnItemClickListener(this);
        setContentView(listView);


        mEditorLauncher = new EditorLauncher(this, "ArrEdiEdLa");
        mEditorLauncher.setCallback(this);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mModified) {
            BundleAdapter.putInBundle(outState, STATE_MODIFIED_ARRAY, mArray);
        }
    }

    @Override
    public void onEditorResult(String key, Object newValue) {
        Array.set(mArray, Integer.parseInt(key), newValue);
        if (!mModified) {
            mModified = true;
            ActivityCompat.invalidateOptionsMenu(this);
        }
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        mEditorLauncher.launchEditor(String.valueOf(position), Array.get(mArray, position));
    }

    @Override
    public void onBackPressed() {
        if (mModified) {
            Bundle extras = new Bundle();
            BundleAdapter.putInBundle(extras, Editor.EXTRA_VALUE, mArray);
            setResult(RESULT_OK, new Intent().replaceExtras(extras));
        }
        super.onBackPressed();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.array_editor, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.discard).setVisible(mModified);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.discard:
                new AlertDialog.Builder(this)
                        .setTitle(getString(R.string.discard_changes_confirm))
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                finish();
                            }
                        })
                        .setNegativeButton(android.R.string.no, null)
                        .show();
                return true;
            default:
                return false;
        }
    }

    private class Adapter extends BaseAdapter {

        @Override
        public int getCount() {
            return Array.getLength(mArray);
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(android.R.layout.simple_list_item_1, parent, false);
            }
            ((TextView) convertView).setText(position + ": " + String.valueOf(Array.get(mArray, position)));
            return convertView;
        }
    }

    public static class LaunchableEditor extends Editor.EditorActivity {
        @Override
        public boolean canEdit(Object value) {
            return value != null && value.getClass().isArray();
        }

        @Override
        public Intent getEditorIntent(Context context) {
            return new Intent(context, ArrayEditorActivity.class);
        }
    }
}
