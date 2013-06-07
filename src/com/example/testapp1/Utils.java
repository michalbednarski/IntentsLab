package com.example.testapp1;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.widget.Toast;

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

    public static void toastException(Context context, Exception exception) {
        String exceptionName = exception.getClass().getName();
        exceptionName = afterLastDot(exceptionName);
        Toast.makeText(context, exceptionName + ": " + exception.getMessage(), Toast.LENGTH_LONG).show();
    }

    public static String afterLastDot(String s) {
        if (s == null) {
            return "null";
        }
        return s.substring(s.lastIndexOf('.') + 1);
    }

    /**
     * Check if object has overridden equals(Object) method
     */
    public static boolean hasOverriddenEqualsMethod(Object o) {
        try {
            return o.getClass().getMethod("equals", Object.class).getDeclaringClass() != Object.class;
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Something is really wrong with type hierarchy, equals method not found", e);
        }
    }
}
