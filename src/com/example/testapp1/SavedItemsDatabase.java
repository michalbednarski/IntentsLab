package com.example.testapp1;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.example.testapp1.editor.IntentEditorActivity;

import java.net.URISyntaxException;

/**
 * Created by mb on 10.06.13.
 */
public class SavedItemsDatabase {
    private static final int DB_VERSION = 1;
    private static SavedItemsDatabase sInstance = null;
    private SQLiteDatabase mDatabase;

    private SavedItemsDatabase(Context context) {
        mDatabase = context.openOrCreateDatabase("saved_items", Context.MODE_PRIVATE, null);
        int oldVersion = mDatabase.getVersion();
        if (oldVersion < DB_VERSION) {
            mDatabase.execSQL(
                "CREATE TABLE `intents` (" +
                    "_id INTEGER PRIMARY KEY, " +
                    "name TEXT, " +
                    "intent TEXT" +
                ")",
                new Object[0]
            );
            mDatabase.setVersion(DB_VERSION);
        }
    }

    public static SavedItemsDatabase getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new SavedItemsDatabase(context);
        }
        return sInstance;
    }

    public void lazyAttachListAdapter(final ListView listView) {
        new AsyncTask<Object, Object, Cursor[]>() {

            @Override
            protected Cursor[] doInBackground(Object... params) {
                return new Cursor[] {mDatabase.query("intents", new String[] {"name", "_id", "intent"}, null, null, null, null, null)};
            }

            @Override
            protected void onPostExecute(final Cursor[] cursors) {
                final Context context = listView.getContext();

                new MultipleCursorAdapter.Builder()
                        .addCursor(cursors[0], "intents", new MultipleCursorAdapter.OnCursorAdapterItemClickListener() {
                            @Override
                            public void onCursorAdapterItemClick(Cursor cursor) {
                                try {
                                    context.startActivity(
                                        new Intent(context, IntentEditorActivity.class)
                                            .putExtra("intent", Intent.parseUri(cursor.getString(2), 0))
                                    );
                                } catch (URISyntaxException e) {
                                    Log.e("", "Malformed intent");
                                }
                            }

                            @Override
                            public void onCursorAdapterCreateContextMenu(ContextMenu menu, Cursor cursor) {
                                final int _id = cursor.getInt(1);
                                menu.add(R.string.delete).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                                    @Override
                                    public boolean onMenuItemClick(MenuItem item) {
                                        mDatabase.delete("intents", "_id = ?", new String[]{String.valueOf(_id)});
                                        lazyAttachListAdapter(listView);
                                        return true;
                                    }
                                });
                            }
                        })
                        .buildAndAttach(listView);
            }
        }.execute();
    }

    private interface PromptForNameCallback {
        void onConfirmSave(String name);
    }

    private void promptForName(Context context, String title, final PromptForNameCallback callback) {
        View view = ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                .inflate(R.layout.save_as, null);
        final TextView textView = (TextView) view.findViewById(R.id.name);
        new AlertDialog.Builder(context)
            .setTitle(title)
            .setView(view)
            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    callback.onConfirmSave(textView.getText().toString());
                }
            })
            .show();
    }

    public void saveIntent(final Context context, final Intent intent) {
        promptForName(context, context.getString(R.string.save), new PromptForNameCallback() {
            @Override
            public void onConfirmSave(String name) {
                ContentValues values = new ContentValues();
                values.put("name", name);
                values.put("intent", intent.toUri(0));
                mDatabase.insert("intents", null, values);
                Toast.makeText(context, R.string.saved, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
