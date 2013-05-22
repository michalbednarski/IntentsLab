package com.example.testapp1;

import android.annotation.TargetApi;
import android.content.SharedPreferences;
import android.os.Build;

public class Utils {
    private Utils() {
    }

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    static void applyOrCommitPrefs(SharedPreferences.Editor prefsEditor) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            prefsEditor.apply();
        } else {
            prefsEditor.commit();
        }
    }

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    public static boolean stringEmptyOrNull(String str) {
        if (str == null) {
            return true;
        }
        try {
            return str.isEmpty();
        } catch (NoSuchMethodError error) {
            return "".equals(str);
        }
    }
}
