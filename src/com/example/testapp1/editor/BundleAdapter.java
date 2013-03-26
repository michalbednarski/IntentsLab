package com.example.testapp1.editor;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import android.widget.Toast;

class BundleAdapter extends BaseAdapter {
	private LayoutInflater mInflater;
	private Bundle mBundle;
	private String[] keys = new String[0];
	private int keysCount = 0;
	

	BundleAdapter(Context context, Bundle map) {
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
		return keysCount;
	}

	public Object getItem(int position) {
		return null;
	}

	public long getItemId(int position) {
		return position;
	}

	public View getView(int position, View convertView, ViewGroup parent) {
		View view = convertView;
		if (view == null) {
			view = mInflater.inflate(android.R.layout.simple_list_item_2, parent, false);
		}
		((TextView)view.findViewById(android.R.id.text1)).setText(keys[position]);
		((TextView)view.findViewById(android.R.id.text2)).setText(mBundle.get(keys[position]).toString());
		return view;
	}

}
