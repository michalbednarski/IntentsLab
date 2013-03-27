package com.example.testapp1;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.Activity;
import android.app.FragmentTransaction;
import android.os.Build;
import android.view.View;

public abstract class TabsHelper {
	
	public static TabsHelper makeTabsHelper(Activity activity, TabDef[] tabDefs) {
		if (tabDefs == null || tabDefs.length < 2) {
			return null;
		}
		if (Build.VERSION.SDK_INT >= 11) {
			return new TabsHelperActionBar(activity, tabDefs);
		} else {
			return null;
		}
	}

	public static class TabDef {
		private int[] viewIds;
		private String tabName;
		private Activity mActivity;
		private Object tabObject;

		public TabDef(String tabName, int... viewIds) {
			this.tabName = tabName;
			this.viewIds = viewIds;
		}

		private void findViewsOn(Activity activity) {

			mActivity = activity;
		}

		void leaveTab() {
			for (int v : viewIds) {
				mActivity.findViewById(v).setVisibility(View.GONE);
			}
		}

		void enterTab() {
			for (int v : viewIds) {
				mActivity.findViewById(v).setVisibility(View.VISIBLE);
			}
		}
	}



	protected Activity mActivity;
	protected int mCurrentViewId = 0;

	public int getCurrentView() {
		return mCurrentViewId;
	}
	
	public abstract void setCurrentView(int viewId);


	@TargetApi(11)
	private static class TabsHelperActionBar extends TabsHelper implements
			ActionBar.TabListener {

		ActionBar mActionBar;
		TabDef mTabs[];
		

		TabsHelperActionBar(Activity a, TabDef[] tabDefs) {
			mActivity = a;
			mTabs = tabDefs;
			mActionBar = a.getActionBar();
			mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
			
			for (TabDef tabDef : tabDefs) {
				tabDef.findViewsOn(a);
				
				Tab tab = mActionBar.newTab()
					.setText(tabDef.tabName)
					.setTabListener(this)
					.setTag(tabDef);
				mActionBar.addTab(tab);
				
				tabDef.tabObject = tab;
				
				if (tabDef != tabDefs[0]) {
					tabDef.leaveTab();
				}
			}
		}
		
		private TabDef findTabDefWithView(int viewId) {
			for (TabDef tabDef : mTabs) {
				for (int curViewId : tabDef.viewIds) {
					if (curViewId == viewId) {
						return tabDef;
					}
				}
			}
			return mTabs[0];
		}
		
		public void setCurrentView(int viewId) {
			TabDef newTab = findTabDefWithView(viewId);
			mActionBar.selectTab((Tab) newTab.tabObject);
		}
		
		
		
		public void onTabSelected(Tab tab, FragmentTransaction ft) {
			TabDef tabDef = (TabDef) tab.getTag();
			mCurrentViewId = tabDef.viewIds[0];
			tabDef.enterTab();
		}

		public void onTabUnselected(Tab tab, FragmentTransaction ft) {
			((TabDef) tab.getTag()).leaveTab();
		}

		public void onTabReselected(Tab tab, FragmentTransaction ft) {}
	}
}
