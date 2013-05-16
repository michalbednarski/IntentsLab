package com.example.testapp1.browser;

public class AppsBrowserFilter {

	public static final int TYPE_ACTIVITY = 1;
	public static final int TYPE_RECEIVER = 2;
	public static final int TYPE_SERVICE = 4;
	public int type = TYPE_ACTIVITY;


	public static final int APP_TYPE_USER = 1;
	public static final int APP_TYPE_SYSTEM = 2;
	public int appType = APP_TYPE_USER;


	public static final int PROTECTION_WORLD_ACCESSIBLE = 1;
	public static final int PROTECTION_NORMAL = 2;
	public static final int PROTECTION_DANGEROUS = 4;
	public static final int PROTECTION_SIGNATURE = 8;
	public static final int PROTECTION_SYSTEM = 16;
	public static final int PROTECTION_DEVELOPMENT = 32;
	public static final int PROTECTION_UNEXPORTED = 64;
	public int protection = PROTECTION_WORLD_ACCESSIBLE;
}
