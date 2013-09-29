package com.github.michalbednarski.intentslab.editor;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.view.*;
import android.view.View.OnClickListener;
import android.widget.*;
import android.widget.AdapterView.OnItemClickListener;
import com.github.michalbednarski.intentslab.R;
import com.github.michalbednarski.intentslab.Utils;
import com.github.michalbednarski.intentslab.valueeditors.EditorLauncher;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.regex.Pattern;

public class BundleAdapter extends BaseAdapter implements OnClickListener,
		OnItemClickListener, View.OnCreateContextMenuListener, EditorLauncher.EditorLauncherCallback {
	private static final String TAG = "BundleAdapter";
	private FragmentActivity mActivity;
	private LayoutInflater mInflater;
	private Bundle mBundle;
	private String[] mKeys = new String[0];
	private int mKeysCount = 0;

	private boolean mShowAddNewOption = true;

	/*static final String[] bundleContainableTypes = { "Byte", "Char", "Short",
			"Int", "Long", "Float", "Double", "String", "CharSequence",
			"Parcelable", "ParcelableArray", "ParcelableArrayList",
			"SparseParcelableArray", "IntegerArrayList", "StringArrayList",
			"CharSequenceArrayList", "Serializable", "BooleanArray",
			"ByteArray", "ShortArray", "CharArray", "IntArray", "LongArray",
			"FloatArray", "DoubleArray", "StringArray", "CharSequenceArray",
			"Bundle", "IBinder" };*/


    public static void putInBundle(Bundle bundle, String key, Object value) {
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
    private Fragment mOwnerFragment;

    public BundleAdapter(FragmentActivity activity, Bundle map, EditorLauncher editorLauncher, Fragment ownerFragment) {
		mActivity = activity;
		mInflater = (LayoutInflater) activity
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		setBundle(map);
        mEditorLauncher = editorLauncher;
        mEditorLauncher.setCallback(this);
        mOwnerFragment = ownerFragment;
    }

	private void updateKeySet() {
		if (mBundle.keySet() != null) {
			mKeys = mBundle.keySet().toArray(mKeys);
			mKeysCount = mBundle.size();
		} else {
			mKeys = new String[0];
			mKeysCount = 0;
		}
	}

	void setBundle(Bundle newMap) {
		if (newMap != null) {
			mBundle = new Bundle(newMap);
		} else {
			mBundle = new Bundle();
		}
		updateKeySet();
	}

	public Bundle getBundle() {
		return mBundle;
	}

	@Override
	public int getCount() {
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
		return position == mKeysCount ? 1 : 0;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if (position == mKeysCount) { // 'New' button
			if (convertView != null) {
				return convertView;
			}
			Button btn = new Button(mActivity);
			btn.setText(R.string.btn_add);
			btn.setOnClickListener(this);
			return btn;
		}

		View view = convertView;
		if (view == null) {
			view = mInflater.inflate(android.R.layout.simple_list_item_2,
					parent, false);
		}

		Object value = mBundle.get(mKeys[position]);

		((TextView) view.findViewById(android.R.id.text1))
				.setText(mKeys[position]);
		((TextView) view.findViewById(android.R.id.text2))
				.setText(value == null ? "null" : value.toString());
		return view;
	}

	@Override
	public void onClick(View v) { // For add new button
        FragmentManager topFragmentManager = mActivity.getSupportFragmentManager();

        if (mOwnerFragment instanceof IntentExtrasFragment && NewExtraPickerDialog.mayHaveSomethingForIntent(mActivity)) {
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
			Object value = mBundle.get(key);
            mEditorLauncher.launchEditor(key, value);
		}
	}


    @Override
    public void onEditorResult(String key, Object newValue) {
        boolean keySetChange = !mBundle.containsKey(key);
        putInBundle(mBundle, key, newValue);
        if (keySetChange) {
            updateKeySet();
        }
        notifyDataSetChanged();
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
        menu.add(mActivity.getString(R.string.delete)).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                mBundle.remove(mKeys[aMenuInfo.position]);
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
    }
}
