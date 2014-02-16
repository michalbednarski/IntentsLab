package com.github.michalbednarski.intentslab.editor;

import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import com.github.michalbednarski.intentslab.R;
import com.github.michalbednarski.intentslab.SingleFragmentActivity;
import com.github.michalbednarski.intentslab.Utils;
import com.github.michalbednarski.intentslab.browser.ComponentInfoFragment;

import java.util.ArrayList;
import java.util.List;

public class ComponentPickerDialogFragment extends DialogFragment implements OnItemClickListener, OnItemLongClickListener {
    private static final String ARG_CHOICES = "ComPicker.choices";

    private ArrayList<ResolveInfo> mChoices;

	ComponentPickerDialogFragment(List<ResolveInfo> choices, IntentGeneralFragment targetFragment) {
        try {
            mChoices = (ArrayList<ResolveInfo>) choices;
        } catch (ClassCastException e) {
            mChoices = new ArrayList<ResolveInfo>(choices);
        }
        Bundle arguments = new Bundle();
        arguments.putParcelableArrayList(ARG_CHOICES, mChoices);
        setArguments(arguments);

        setTargetFragment(targetFragment, 0);
    }

    public ComponentPickerDialogFragment() {}

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        return dialog;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (mChoices == null) {
            mChoices = getArguments().getParcelableArrayList(ARG_CHOICES);
        }

        ListView lv = new ListView(getActivity(), null);
        lv.setId(android.R.id.list);
        lv.setAdapter(new Adapter());
        Utils.fixListViewInDialogBackground(lv);
        lv.setOnItemClickListener(this);
        lv.setOnItemLongClickListener(this);
        return lv;
    }

	private String getComponentName(ResolveInfo info) {
		boolean isService = info.activityInfo == null;
		return new ComponentName(
				isService ? info.serviceInfo.packageName : info.activityInfo.packageName,
				isService ? info.serviceInfo.name : info.activityInfo.name
			).flattenToShortString();
	}

    private class Adapter extends ArrayAdapter<ResolveInfo> {
        private LayoutInflater mInflater;
        private PackageManager mPm;

        Adapter() {
            super(getActivity(), 0, mChoices);
            FragmentActivity activity = getActivity();
            mInflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            mPm = activity.getPackageManager();
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
    }

    /**
     * Fill component name field on item click
     */
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        ((IntentGeneralFragment) getTargetFragment()).setComponentText(getComponentName(mChoices.get(position)));
		dismiss();
	}

    /**
     * Show item info on long click
     */
    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        ResolveInfo info = mChoices.get(position);
        boolean isService = info.activityInfo == null;
        startActivity(
                new Intent(getActivity(), SingleFragmentActivity.class)
                .putExtra(SingleFragmentActivity.EXTRA_FRAGMENT, ComponentInfoFragment.class.getName())
                .putExtra(ComponentInfoFragment.ARG_PACKAGE_NAME, isService ? info.serviceInfo.packageName : info.activityInfo.packageName)
                .putExtra(ComponentInfoFragment.ARG_COMPONENT_NAME, isService ? info.serviceInfo.name : info.activityInfo.name)
                .putExtra(ComponentInfoFragment.ARG_LAUNCHED_FROM_INTENT_EDITOR, true)
        );
        return true;
    }
}
