package com.example.testapp1.editor;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.testapp1.R;

class BundleAdapter extends BaseAdapter implements OnClickListener {
	private Context mContext;
	private LayoutInflater mInflater;
	private Bundle mBundle;
	private String[] keys = new String[0];
	private int keysCount = 0;

	private boolean mShowAddNewOption = true;


	BundleAdapter(Context context, Bundle map) {
		mContext = context;
		mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		update(map);
	}

	void update(Bundle newMap) {
		if (newMap != null) {
			mBundle = new Bundle(newMap);
		} else {
			mBundle = new Bundle();
		}
		if (mBundle.keySet() != null) {
			keys = mBundle.keySet().toArray(keys);
			keysCount = mBundle.size();
		} else {
			keys = new String[0];
			keysCount = 0;
		}
	}

	void editItemAt(Context ctx, int position) {
		String key = keys[position];
		Object value = mBundle.get(key);
		if (value instanceof String) {
			// TODO
			Toast.makeText(ctx, "TODO", Toast.LENGTH_SHORT).show(); // TODO: Localize
		} else {
			Toast.makeText(ctx, "Type unsupported", Toast.LENGTH_SHORT).show(); // TODO: Localize
		}
	}

	public int getCount() {
		return keysCount + (mShowAddNewOption ? 1 : 0);
	}

	public Object getItem(int position) {
		return null;
	}

	public long getItemId(int position) {
		return position;
	}

	public View getView(int position, View convertView, ViewGroup parent) {
		boolean isNewButtonRequested = position == keysCount;
		boolean isNewButtonConverted = (convertView instanceof Button);

		if (isNewButtonRequested) {
			if (isNewButtonConverted) {
				return convertView;
			}
			Button btn = new Button(mContext);
			btn.setText(R.string.btn_add_extra);
			btn.setOnClickListener(this);
			return btn;
		}

		View view = convertView;
		if (view == null || isNewButtonConverted) {
			view = mInflater.inflate(android.R.layout.simple_list_item_2, parent, false);
		}
		((TextView)view.findViewById(android.R.id.text1)).setText(keys[position]);
		((TextView)view.findViewById(android.R.id.text2)).setText(mBundle.get(keys[position]).toString());
		return view;
	}

	public void onClick(View v) { // For add new button
		// TODO Auto-generated method stub

	}

}
