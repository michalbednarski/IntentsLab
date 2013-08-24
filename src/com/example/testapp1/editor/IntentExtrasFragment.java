package com.example.testapp1.editor;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import com.example.testapp1.valueeditors.EditorLauncher;

public class IntentExtrasFragment extends IntentEditorPanel implements BundleAdapter.BundleAdapterAggregate {
    public IntentExtrasFragment() {}

	ListView mExtrasList;
	private BundleAdapter mBundleAdapter;

    @Override
    public BundleAdapter getBundleAdapter() {
        return mBundleAdapter;
    }

    @Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		super.onCreateView(inflater, container, savedInstanceState);

		mExtrasList = new ListView(inflater.getContext());

        mBundleAdapter = new BundleAdapter(getActivity(), getEditedIntent().getExtras(), new EditorLauncher(getActivity(), "IntentExtrasEditorLauncher"), this);
		mBundleAdapter.settleOnList(mExtrasList);
		return mExtrasList;
	}

	@Override
	public void updateEditedIntent(Intent editedIntent) {
		editedIntent.replaceExtras(mBundleAdapter.getBundle());
	}

	@Override
	public void onComponentTypeChanged(int newComponentType) {}
}
