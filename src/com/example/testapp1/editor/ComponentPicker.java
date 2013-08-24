package com.example.testapp1.editor;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import android.widget.AdapterView.OnItemClickListener;
import com.example.testapp1.R;
import com.example.testapp1.Utils;

import java.util.List;

class ComponentPicker extends ArrayAdapter<ResolveInfo> implements OnItemClickListener {
	private LayoutInflater mInflater;
	private PackageManager mPm;
	private AlertDialog mDialog;
	private TextView mComponentTextView;

	ComponentPicker(Context context, List<ResolveInfo> choices, TextView componentTextView) {
		super(context, 0, choices);
		if (choices.isEmpty()) {
			Toast.makeText(context, R.string.no_matching_components_found, Toast.LENGTH_SHORT).show();
			return;
		}
		mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mPm = context.getPackageManager();
		mComponentTextView = componentTextView;
		ListView lv = new ListView(context, null);
		lv.setAdapter(this);
        Utils.fixListViewInDialogBackground(lv);
        lv.setOnItemClickListener(this);

		mDialog =
				new AlertDialog.Builder(context)
				.setView(lv)
				.show();
	}

	void show() {}

	private String getComponentName(ResolveInfo info) {
		boolean isService = info.activityInfo == null;
		return new ComponentName(
				isService ? info.serviceInfo.packageName : info.activityInfo.packageName,
				isService ? info.serviceInfo.name : info.activityInfo.name
			).flattenToShortString();
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if (convertView == null) {
			convertView = mInflater.inflate(R.layout.simple_list_item_2_with_icon, parent, false);
		}

		ResolveInfo info = getItem(position);
		((TextView)convertView.findViewById(android.R.id.text1))
			.setText(info.loadLabel(mPm));
		((TextView)convertView.findViewById(android.R.id.text2))
			.setText(getComponentName(info));

		((ImageView)convertView.findViewById(R.id.app_icon)).setImageDrawable(info.loadIcon(mPm));
		return convertView;
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		mComponentTextView.setText(getComponentName(getItem(position)));
		mDialog.dismiss();
	}
}
