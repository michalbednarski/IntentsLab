package com.example.testapp1;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.Activity;
import android.app.FragmentTransaction;
import android.os.Build;

public abstract class TabsHelper {
	static TabsHelper makeTabsHelper(Activity activity) {
		if (Build.VERSION.SDK_INT >= 11) {
			return new TabsHelperActionBar(activity);
		} else {
			return null;
		}
	}

	public static class TabDef {
		public int viewId;
		public String tabName;

		TabDef(String tabName, int viewId) {
			this.tabName = tabName;
			this.viewId = viewId;
		}
	}

	public interface TabsListener {
		void onTabSelected(int tabId);
	}

	@TargetApi(11)
	private static class TabsHelperActionBar extends TabsHelper implements
			ActionBar.TabListener {

		ActionBar ab;
		TabsListener tl;

		TabsHelperActionBar(Activity a) {
			ab = a.getActionBar();
		}

		public void onTabSelected(Tab tab, FragmentTransaction ft) {
			tl.onTabSelected((Integer) tab.getTag());
		}

		public void onTabUnselected(Tab tab, FragmentTransaction ft) {
		}

		public void onTabReselected(Tab tab, FragmentTransaction ft) {
		}
	}
}
