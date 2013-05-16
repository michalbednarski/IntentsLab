package com.example.testapp1.editor;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.FragmentTransaction;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTabHost;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;

import com.example.testapp1.R;

@SuppressLint("Registered")
@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public abstract class FragmentTabsActivity extends FragmentActivity {
	private boolean mUseActionBarForTabs = false;
	private ActionBar.TabListener mActionBarTabListener;
	private FrameLayout mTabContent;
	//private boolean mFirstTabAdded = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mUseActionBarForTabs = Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB
				&& !PreferenceManager.getDefaultSharedPreferences(this).getBoolean("forcelegacytabs", false);

		if (mUseActionBarForTabs) {
			// Prepare tabs on actionbar
			ActionBar actionBar = getActionBar();
			actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
			mActionBarTabListener = new ActionBar.TabListener() {

				@Override
				public void onTabUnselected(Tab tab, FragmentTransaction ft) {
				}

				@SuppressWarnings("unchecked")
				@Override
				public void onTabSelected(Tab tab, FragmentTransaction ft) {
					// hide keyboard (might cause flickering, but I don't know solution)
					InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
					imm.hideSoftInputFromWindow(mTabContent.getApplicationWindowToken(), 0);

					// swap displayed fragment
					Fragment fragment;
					try {
						fragment = ((Class<? extends Fragment>) tab.getTag()).newInstance();
						//fragment = (Fragment) tab.getTag();
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
					getSupportFragmentManager().beginTransaction()
						.replace(android.R.id.tabcontent, fragment)
						.commit();

				}

				@Override
				public void onTabReselected(Tab tab, FragmentTransaction ft) {
				}
			};
			mTabContent = new FrameLayout(this);
			mTabContent.setId(android.R.id.tabcontent);
			setContentView(mTabContent);
		} else {
			// Prepare tabhost tabs
			setContentView(R.layout.tabhost_fragment);
			mTabContent = (FrameLayout) findViewById(android.R.id.tabcontent);

			final FragmentTabHost fth = (FragmentTabHost) findViewById(android.R.id.tabhost);
			fth.setup(this, getSupportFragmentManager(), R.id.realtabcontent);
			fth.setOnTabChangedListener(new TabHost.OnTabChangeListener() {
				@Override
				public void onTabChanged(String tabId) {
					// hide keyboard
					// http://stackoverflow.com/questions/1109022/close-hide-the-android-soft-keyboard
					InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
					imm.hideSoftInputFromWindow(fth.getApplicationWindowToken(), 0);
				}
			});

		}

	}

	/**
	 * Add new tab with associated fragment
	 *
	 * @param text Displayed title of tab
	 * @param fragment Fragment class that has to be instantiated and put in tab
	 */
	protected void addTab(CharSequence text, Class<? extends Fragment> fragment) {
		try {
			if (mUseActionBarForTabs) {
				ActionBar actionBar = getActionBar();
				actionBar.addTab(actionBar.newTab() //
					.setText(text) //
					.setTag(fragment/*.newInstance()*/) //
					.setTabListener(mActionBarTabListener) //
					);
			} else {
				FragmentTabHost th = (FragmentTabHost) findViewById(android.R.id.tabhost);
				TabSpec tabSpec = th.newTabSpec(text.toString()).setIndicator(text);
				th.addTab(tabSpec, fragment, null);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
