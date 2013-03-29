package com.example.testapp1;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.Activity;
import android.app.FragmentTransaction;
import android.os.Build;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;

public abstract class TabsHelper {
	public static Builder getBuilder(Activity actiivity) {
		return new Builder(actiivity);
	}

	public static class Builder {
		Activity mActivity;
		TabDef mTabs[] = null;
		int mLayoutWithoutTabHost;
		boolean mUseActionbar;

		private Builder(Activity activity) {
			mActivity = activity;
			mUseActionbar = getTabsMode();
		}

		public Builder setLayout(int layoutWithTabHost, int layoutWithoutTabHost) {
			mLayoutWithoutTabHost = layoutWithoutTabHost;

			mActivity.setContentView(
					!mUseActionbar
					? layoutWithTabHost
							: layoutWithoutTabHost
					);

			return this;
		}

		public Builder tryTabsConfiguration(Object ...tabsConfiguration) {
			if (mTabs == null) {
				TabDef[] tmpTabs = new TabDef[tabsConfiguration.length / 2];
				for (int i = 0, z = 0; i < tabsConfiguration.length; i += 2, z++) {
					if (mActivity.findViewById((Integer) tabsConfiguration[i + 1]) == null) {
						return this;
					}
					tmpTabs[z] = new TabDef((String) tabsConfiguration[i], (Integer) tabsConfiguration[i + 1]);
				}
				mTabs = tmpTabs;
			}
			return this;
		}

		public TabsHelper build() {
			if (mTabs == null || mTabs.length < 2) {
				return null;
			}
			if (mUseActionbar) {
				return new TabsHelperActionBar(mActivity, mTabs);
			} else {
				return new TabsHelperTabWidget(mActivity, mTabs);
			}
		}

		private boolean getTabsMode() {
			boolean useLegacyTabs =
							PreferenceManager
							.getDefaultSharedPreferences(mActivity)
							.getBoolean("forcelegacytabs", false);

			return (Build.VERSION.SDK_INT >= 11 && !useLegacyTabs);
		}
	}

	public static class TabDef {
		private int viewId;
		private String tabName;
		private Object tabObject;

		public TabDef(String tabName, int viewId) {
			this.tabName = tabName;
			this.viewId = viewId;
		}
	}



	protected Activity mActivity;
	protected TabDef mTabs[];
	protected int mCurrentViewId = 0;

	protected abstract void initTabs();
	protected abstract void createTab(TabDef tabDef, int tabDefId);

	protected TabsHelper(Activity activity, TabDef[] tabs) {
		mActivity = activity;
		mTabs = tabs;
		initTabs();
		for (int i = 0; i < tabs.length; i++) {
			createTab(tabs[i], i);
			if (i != 0) {
				activity.findViewById(tabs[i].viewId).setVisibility(View.GONE);
			}
		}
	}

	public int getCurrentView() {
		return mCurrentViewId;
	}

	private static class TabsHelperTabWidget extends TabsHelper implements TabHost.TabContentFactory {
		TabHost mTabHost;

		TabsHelperTabWidget(Activity a, TabDef[] b) {
			super(a, b);
		};

		@Override
		protected void initTabs() {
			mTabHost = (TabHost) mActivity.findViewById(android.R.id.tabhost);
			mTabHost.setup();
		}

		@Override
		protected void createTab(TabDef tabDef, int tabDefId) {
			TabSpec tab = mTabHost.newTabSpec(String.valueOf(tabDef.viewId))
					.setIndicator(tabDef.tabName)
					.setContent(this);

			mTabHost.addTab(tab);
			tabDef.tabObject = tab;
		}

		public View createTabContent(String tag) {
			return mActivity.findViewById(Integer.valueOf(tag));
		}


	}

	@TargetApi(11)
	private static class TabsHelperActionBar extends TabsHelper implements
	ActionBar.TabListener {

		TabsHelperActionBar(Activity a, TabDef[] b) {
			super(a, b);
		};

		ActionBar mActionBar;

		@Override
		protected void initTabs() {
			mActionBar = mActivity.getActionBar();
			mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		}

		@Override
		protected void createTab(TabDef tabDef, int tabDefId) {
			Tab tab = mActionBar.newTab()
					.setText(tabDef.tabName)
					.setTabListener(this)
					.setTag(tabDef);
			mActionBar.addTab(tab);

			tabDef.tabObject = tab;
		}

		public void onTabSelected(Tab tab, FragmentTransaction ft) {
			mActivity.findViewById(((TabDef) tab.getTag()).viewId).setVisibility(View.VISIBLE);
		}

		public void onTabUnselected(Tab tab, FragmentTransaction ft) {
			mActivity.findViewById(((TabDef) tab.getTag()).viewId).setVisibility(View.GONE);
		}

		public void onTabReselected(Tab tab, FragmentTransaction ft) {}
	}
}
