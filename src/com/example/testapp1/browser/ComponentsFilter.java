package com.example.testapp1.browser;

public class ComponentsFilter {

    public static final int TYPE_ACTIVITY = 1;
    public static final int TYPE_RECEIVER = 2;
    public static final int TYPE_SERVICE = 4;
    public static final int TYPE_CONTENT_PROVIDER = 8;
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
    public static final int PROTECTION_UNKNOWN = 128;

    public int protection = PROTECTION_WORLD_ACCESSIBLE;

    public static final int PROTECTION_ANY = 128 * 2 - 1;
    public static final int PROTECTION_ANY_PERMISSION =
            PROTECTION_NORMAL |
                    PROTECTION_DANGEROUS |
                    PROTECTION_SIGNATURE |
                    PROTECTION_SYSTEM |
                    PROTECTION_DEVELOPMENT |
                    PROTECTION_UNKNOWN;
    public static final int PROTECTION_ANY_OBTAINABLE =
            PROTECTION_WORLD_ACCESSIBLE |
                    PROTECTION_NORMAL |
                    PROTECTION_DANGEROUS;
    public static final int PROTECTION_ANY_EXPORTED =
            PROTECTION_ANY & ~PROTECTION_UNEXPORTED;


    public boolean requireMetaData = false;
    public String requireMetaDataSubstring = null;

    public boolean testWritePermissionForProviders = false; // TODO: UI
}
