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

import android.os.BadParcelableException;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.github.michalbednarski.intentslab.R;
import com.github.michalbednarski.intentslab.Utils;
import com.github.michalbednarski.intentslab.sandbox.ClassLoaderDescriptor;
import com.github.michalbednarski.intentslab.sandbox.ISandboxedBundle;
import com.github.michalbednarski.intentslab.sandbox.SandboxManager;
import com.github.michalbednarski.intentslab.sandbox.SandboxedObject;
import com.github.michalbednarski.intentslab.valueeditors.framework.EditorLauncher;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.regex.Pattern;

public class BundleAdapter<OwnerFragment extends Fragment & BundleAdapter.BundleAdapterAggregate> extends BaseAdapter implements OnClickListener,
		OnItemClickListener, View.OnCreateContextMenuListener, EditorLauncher.EditorLauncherWithSandboxCallback {
	private static final String TAG = "BundleAdapter";
	private Bundle mBundle;
	private String[] mKeys = new String[0];
	private int mKeysCount = 0;

	private boolean mShowAddNewOption = true;

    private boolean mUseSandbox = false;
    private ISandboxedBundle mSandboxedBundle = null;
    private ArrayList<Runnable> mSandboxedBundleReadyCallbacks = null;

	/*static final String[] bundleContainableTypes = { "Byte", "Char", "Short",
			"Int", "Long", "Float", "Double", "String", "CharSequence",
			"Parcelable", "ParcelableArray", "ParcelableArrayList",
			"SparseParcelableArray", "IntegerArrayList", "StringArrayList",
			"CharSequenceArrayList", "Serializable", "BooleanArray",
			"ByteArray", "ShortArray", "CharArray", "IntArray", "LongArray",
			"FloatArray", "DoubleArray", "StringArray", "CharSequenceArray",
			"Bundle", "IBinder" };*/


    public static void putInBundle(Bundle bundle, String key, Object value) {
        if (value == null) {
            bundle.putString(key, null);
            return;
        }

        Pattern putMethodName = Pattern.compile("put[A-Z][A-Za-z]+");
        for (Method method : Bundle.class.getMethods()) {
            if (
                    putMethodName.matcher(method.getName()).matches() &&
                    !method.isVarArgs()) {
                final Class<?>[] parameterTypes = method.getParameterTypes();
                if (
                        parameterTypes.length == 2 &&
                        parameterTypes[0] == String.class &&
                        Utils.toWrapperClass(parameterTypes[1]).isInstance(value)) {
                    try {
                        method.invoke(bundle, key, value);
                        return;
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                        // continue
                    } catch (InvocationTargetException e) {
                        throw new RuntimeException("Method " + method.getName() + " of bundle thrown exception", e);
                    }
                }
            }
        }
        throw new RuntimeException("No put* method found");
    }

    private final EditorLauncher mEditorLauncher;
    private OwnerFragment mOwnerFragment;

    public BundleAdapter(Bundle map, EditorLauncher editorLauncher, OwnerFragment ownerFragment) {
        mEditorLauncher = editorLauncher;
        mOwnerFragment = ownerFragment;
        setBundle(map);
    }

	private void updateKeySet() {
        // Try to unpack bundle without sandbox
        if (!mUseSandbox) {
            try {
                if (mBundle.keySet() != null) {
                    mKeys = mBundle.keySet().toArray(mKeys);
                    mKeysCount = mBundle.size();
                } else {
                    mKeys = new String[0];
                    mKeysCount = 0;
                }
                return;
            } catch (BadParcelableException ignored) {}
        }

        // If it failed or was disabled
        sandboxBundle(true, null);
	}

    private void sandboxBundle(boolean alwaysUpdateKeySet, final Runnable whenDone) {
        if (mUseSandbox) {
            // Sandbox already enabled, return if it's still alive, that is, there's no need to reinitialize
            if (mSandboxedBundle != null && mSandboxedBundle.asBinder().isBinderAlive()) {
                if (alwaysUpdateKeySet) {
                    try {
                        mKeys = mSandboxedBundle.getKeySet();
                        mKeysCount = mKeys.length;
                        notifyDataSetChanged();
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
                if (whenDone != null) {
                    whenDone.run();
                }
                return;
            }
        } else {
            // Start using sandbox
            mUseSandbox = true;
            SandboxManager.refSandbox();
        }

        // If it's being prepared, add to ready callbacks
        if (mSandboxedBundleReadyCallbacks != null) {
            if (whenDone != null) {
                mSandboxedBundleReadyCallbacks.add(whenDone);
            }
            return;
        }

        // Set flag we're working and add ready callback
        mSandboxedBundleReadyCallbacks = new ArrayList<Runnable>();
        if (whenDone != null) {
            mSandboxedBundleReadyCallbacks.add(whenDone);
        }

        // Initialize sandbox
        SandboxManager.initSandboxAndRunWhenReady(mOwnerFragment.getActivity(), new Runnable() {
            @Override
            public void run() {

                try {
                    // Wrap bundle
                    // TODO: don't hardcode package name
                    mSandboxedBundle = SandboxManager.getSandbox().sandboxBundle(mBundle, new ClassLoaderDescriptor("com.github.michalbednarski.intentslab.samples"));

                    // Update key set
                    mKeys = mSandboxedBundle.getKeySet();
                    mKeysCount = mKeys.length;

                    // Notify all callbacks that we're ready
                    for (Runnable readyCallback : mSandboxedBundleReadyCallbacks) {
                        readyCallback.run();
                    }
                    mSandboxedBundleReadyCallbacks = null;

                    notifyDataSetChanged();
                } catch (Exception e) {
                    mSandboxedBundle = null;
                }

            }
        });
    }

    public void shutdown() {
        if (mUseSandbox) {
            SandboxManager.unrefSandbox();
        }
    }

	void setBundle(Bundle newMap) {
		if (newMap != null) {
			mBundle = new Bundle(newMap);
		} else {
			mBundle = new Bundle();
		}
        mUseSandbox = false;
        mSandboxedBundle = null;
		updateKeySet();
	}

	public Bundle getBundle() {
        if (mUseSandbox && mSandboxedBundle != null) {
            try {
                mBundle = mSandboxedBundle.getBundle();
            } catch (RemoteException e) {
                // There was some problem with sandbox,
                // but we can't do anything about that
                e.printStackTrace();
            }
        }
		return mBundle;
	}

	@Override
	public int getCount() {
        if (mUseSandbox && mSandboxedBundle == null) {
            return 0;
        }
		return mKeysCount + (mShowAddNewOption ? 1 : 0);
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
	public int getViewTypeCount() {
		return mShowAddNewOption ? 2 : 1;
	}
	@Override
	public int getItemViewType(int position) {
        if (mUseSandbox && mSandboxedBundle == null) {
            return IGNORE_ITEM_VIEW_TYPE;
        }
		return position == mKeysCount ? 1 : 0;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if (position == mKeysCount) { // 'New' button
			if (convertView != null) {
				return convertView;
			}
			Button btn = new Button(parent.getContext());
			btn.setText(R.string.btn_add);
			btn.setOnClickListener(this);
			return btn;
		}

		View view = convertView;
		if (view == null) {
			view = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_2,
                    parent, false);
		}

        final String key = mKeys[position];
        String valueAsString;

        if (mUseSandbox) {
            try {
                valueAsString = mSandboxedBundle.getAsString(key);
            } catch (RemoteException e) {
                // TODO: resandbox bundle
                valueAsString = "[Sandbox error]";
            }
        } else {
            valueAsString = String.valueOf(mBundle.get(key));
        }

        ((TextView) view.findViewById(android.R.id.text1))
				.setText(key);
		((TextView) view.findViewById(android.R.id.text2))
				.setText(valueAsString);
		return view;
	}

	@Override
	public void onClick(View v) { // For add new button
        FragmentManager topFragmentManager = mOwnerFragment.getFragmentManager();

        if (mOwnerFragment instanceof IntentExtrasFragment && NewExtraPickerDialog.mayHaveSomethingForIntent(mOwnerFragment.getActivity())) {
            // We're in intent editor and may guess some extras from it
            // Launch suggestion picker
            NewExtraPickerDialog newExtraPickerDialog = new NewExtraPickerDialog((IntentExtrasFragment) mOwnerFragment);
            newExtraPickerDialog.show(topFragmentManager, "newExtraPicker");
        } else {
            // Launch generic add extra dialog
            NewBundleEntryDialog newBundleEntryDialog = new NewBundleEntryDialog(mOwnerFragment);
            newBundleEntryDialog.show(topFragmentManager, "newBundleEntryDialog");
        }
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		if (position != mKeysCount) {
            String key = mKeys[position];
            if (mUseSandbox) {
                SandboxedObject wrappedValue;
                try {
                    wrappedValue = mSandboxedBundle.getWrapped(key);
                } catch (RemoteException e) {
                    return;
                }
                try {
                    mEditorLauncher.launchEditor(key, wrappedValue.unwrap(null));
                } catch (BadParcelableException e) {
                    mEditorLauncher.launchEditorForSandboxedObject(key, key, wrappedValue);
                }
            } else {
                mEditorLauncher.launchEditor(key, mBundle.get(key));
            }
		}
	}


    @Override
    public void onEditorResult(final String key, final Object newValue) {

        if (mUseSandbox) {
            // Async wrap in sandbox
            sandboxBundle(false, new Runnable() {
                @Override
                public void run() {
                    boolean keySetChange = false;
                    try {
                        keySetChange = !mSandboxedBundle.containsKey(key);
                        mSandboxedBundle.putWrapped(key, new SandboxedObject(newValue));
                    } catch (Exception e) {
                        e.printStackTrace(); // Cannot recover
                    }
                    if (keySetChange) {
                        updateKeySet();
                    }
                    notifyDataSetChanged();
                    dispatchModified();
                }
            });
        } else {
            // No sandbox
            boolean keySetChange;
            keySetChange = !mBundle.containsKey(key);
            putInBundle(mBundle, key, newValue);
            if (keySetChange) {
                updateKeySet();
            }
            notifyDataSetChanged();
            dispatchModified();
        }
    }

    @Override
    public void onSandboxedEditorResult(final String key, final SandboxedObject newWrappedValue) {
        // Check if sandbox isn't needed
skipSandbox: {
            if (!mUseSandbox) {
                Object unwrapped = null;
                try {
                    unwrapped = newWrappedValue.unwrap(null);
                } catch (Exception e) {
                    break skipSandbox;
                }
                onEditorResult(key, unwrapped);
                return;
            }
        }

        // Really use sandbox
        sandboxBundle(false, new Runnable() {
            @Override
            public void run() {
                boolean keySetChange = false;
                try {
                    keySetChange = !mSandboxedBundle.containsKey(key);
                    mSandboxedBundle.putWrapped(key, newWrappedValue);
                } catch (Exception e) {
                    e.printStackTrace(); // Cannot recover
                }
                if (keySetChange) {
                    updateKeySet();
                }
                notifyDataSetChanged();
                dispatchModified();
            }
        });
    }

    private void dispatchModified() {
        mOwnerFragment.onBundleModified();
    }

	public void settleOnList(ListView listView) {
		listView.setAdapter(this);
		listView.setOnItemClickListener(this);
        listView.setOnCreateContextMenuListener(this);
	}

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        final AdapterView.AdapterContextMenuInfo aMenuInfo = (AdapterView.AdapterContextMenuInfo) menuInfo;
        if (aMenuInfo.position == mKeysCount) {
            return;
        }
        menu.add(mOwnerFragment.getString(R.string.delete)).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                final String key = mKeys[aMenuInfo.position];
                if (mUseSandbox) {
                    try {
                        mSandboxedBundle.remove(key);
                    } catch (RemoteException e) {
                        e.printStackTrace(); // TODO: resandbox bundle
                    }
                } else {
                    mBundle.remove(key);
                }
                updateKeySet();
                notifyDataSetChanged();
                return true;
            }
        });
    }

    public void launchEditorForNewEntry(String key, Object initialValue) {
        mEditorLauncher.launchEditor(key, initialValue);
    }

    public interface BundleAdapterAggregate {
        public BundleAdapter getBundleAdapter();

        void onBundleModified();
    }
}
