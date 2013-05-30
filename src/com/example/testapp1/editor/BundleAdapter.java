package com.example.testapp1.editor;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.testapp1.R;

class BundleAdapter extends BaseAdapter implements OnClickListener,
		OnItemClickListener, OnItemLongClickListener, EditorCallback {
	private static final String TAG = "BundleAdapter";
	private Activity mActivity;
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
			"Bundle", "IBinder" };

	static void putBundleValue(Bundle bundle, String key, Object value) {
		if (value instanceof Byte) {
			bundle.putByte(key, (Byte) value);
			// putChar is putString
		} else if (value instanceof Short) {
			bundle.putShort(key, (Short) value);
		} else if (value instanceof Integer) {
			bundle.putInt(key, (Integer) value);
		} else if (value instanceof Long) {
			bundle.putLong(key, (Long) value);
		} else if (value instanceof Float) {
			bundle.putFloat(key, (Float) value);
		} else if (value instanceof Double) {
			bundle.putDouble(key, (Double) value);
		} else if (value instanceof String) {
			bundle.putString(key, (String) value);
		} else if (value instanceof CharSequence) {
			bundle.putCharSequence(key, (CharSequence) value);
		} else if (value instanceof Parcelable) {
			bundle.putParcelable(key, (Parcelable) value);
		} else if (value instanceof Parcelable[]) {
			bundle.putParcelableArray(key, (Parcelable[]) value);
		} else if (value instanceof ArrayList<?>) {
			// } else if (value instanceof ArrayList<Parcelable>) {
			// bundle.putParcelableArrayList(key, (ArrayList<Parcelable>)
			// value);
			// } else if (value instanceof SparseArray<Parcelable>) {
			// bundle.putSparseParcelableArray(key, (SparseParcelable[]) value);
			// } else if (value instanceof ArrayList<Integer>) {
			// bundle.putIntegerArrayList(key, (ArrayList<Integer>) value);
			// } else if (value instanceof ArrayList<String>) {
			// bundle.putStringArrayList(key, (ArrayList<String>) value);
			// } else if (value instanceof ArrayList<CharSequence>) {
			// bundle.putCharSequenceArrayList(key, (ArrayList<CharSequence>)
			// value);
		} else if (value instanceof Serializable) {
			bundle.putSerializable(key, (Serializable) value);
		} else if (value instanceof boolean[]) {
			bundle.putBooleanArray(key, (boolean[]) value);
		} else if (value instanceof byte[]) {
			bundle.putByteArray(key, (byte[]) value);
		} else if (value instanceof short[]) {
			bundle.putShortArray(key, (short[]) value);
		} else if (value instanceof char[]) {
			bundle.putCharArray(key, (char[]) value);
		} else if (value instanceof int[]) {
			bundle.putIntArray(key, (int[]) value);
		} else if (value instanceof long[]) {
			bundle.putLongArray(key, (long[]) value);
		} else if (value instanceof float[]) {
			bundle.putFloatArray(key, (float[]) value);
		} else if (value instanceof double[]) {
			bundle.putDoubleArray(key, (double[]) value);
		} else if (value instanceof String[]) {
			bundle.putStringArray(key, (String[]) value);
		} else if (value instanceof CharSequence[]) {
			//bundle.putCharSequenceArray(key, (CharSequence[]) value);
		} else if (value instanceof Bundle) {
			bundle.putBundle(key, (Bundle) value);
		}
		// else if (value instanceof IBinder) {
		// bundle.putIBinder(key, (IBinder) value);
		// }
	}*/

	BundleAdapter(Activity activity, Bundle map) {
		mActivity = activity;
		mInflater = (LayoutInflater) activity
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		setBundle(map);
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

	Bundle getBundle() {
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
		// TODO Auto-generated method stub

	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		if (position != mKeysCount) {
			String key = mKeys[position];
			Object value = mBundle.get(key);
			if (value instanceof Boolean) {
				mBundle.putBoolean(key, !((Boolean) value));
			} else if(StringLikeItemEditor.editIfCan(mActivity, mBundle, key, value, this)) {
				// Do nothing, condition starts dialog
				return;
			} else if (value instanceof Integer) {
				TextView tv = new TextView(mActivity);
				Builder builder = (new AlertDialog.Builder(mActivity))
						.setMessage("null").setView(tv)
						.setPositiveButton("text", null);
				builder.create().show();
			} else {
				Toast.makeText(mActivity, R.string.type_unsupported,
						Toast.LENGTH_SHORT).show();
				return;
			}
			notifyDataSetChanged();
		}
	}

	@Override
	public boolean onItemLongClick(AdapterView<?> parent, View view,
			int position, long id) {
		// TODO Auto-generated method stub
		if (position != mKeysCount) {
			return true;
		}
		return false;
	}

	void settleOnList(ListView listView) {
		listView.setAdapter(this);
		listView.setOnItemClickListener(this);
		listView.setOnItemLongClickListener(this);
	}

	@Override
	public void afterEdit(boolean rename) {
		if(rename) {
			updateKeySet();
		}
		notifyDataSetChanged();
	}
}
