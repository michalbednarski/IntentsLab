package com.github.michalbednarski.intentslab;

import android.annotation.TargetApi;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.widget.ListView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class Utils {
    private Utils() {
    }

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    public static void applyOrCommitPrefs(SharedPreferences.Editor prefsEditor) {
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

    public static String describeException(Throwable exception) {
        String exceptionName = exception.getClass().getName();
        exceptionName = afterLastDot(exceptionName);
        return exceptionName + ": " + exception.getMessage();
    }

    public static void toastException(Context context, Throwable exception) {
        toastException(context, null, exception);
    }

    public static void toastException(Context context, String methodName, Throwable exception) {
        Toast.makeText(
            context,
            (methodName != null ? methodName + ": " : "") + describeException(exception),
            Toast.LENGTH_LONG
        ).show();
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

    /**
     * contentValues.keySet() with fallback for older platform versions
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static Set<String> getKeySet(ContentValues contentValues) {
        try {
            return contentValues.keySet();
        } catch (NoSuchMethodError error) {
            HashSet<String> set = new HashSet<String>();
            for (Map.Entry<String, Object> entry : contentValues.valueSet()) {
                set.add(entry.getKey());
            }
            return set;
        }
    }

    /**
     * Map used by toWrapperClass()
     */
    private static HashMap<Class<?>, Class<?>> PRIMITIVE_TO_WRAPPER_CLASS_MAP = new  HashMap<Class<?>, Class<?>>();
    static {
        PRIMITIVE_TO_WRAPPER_CLASS_MAP.put(Boolean.TYPE, Boolean.class);
        PRIMITIVE_TO_WRAPPER_CLASS_MAP.put(Byte.TYPE, Byte.class);
        PRIMITIVE_TO_WRAPPER_CLASS_MAP.put(Character.TYPE, Character.class);
        PRIMITIVE_TO_WRAPPER_CLASS_MAP.put(Short.TYPE, Short.class);
        PRIMITIVE_TO_WRAPPER_CLASS_MAP.put(Integer.TYPE, Integer.class);
        PRIMITIVE_TO_WRAPPER_CLASS_MAP.put(Long.TYPE, Long.class);
        PRIMITIVE_TO_WRAPPER_CLASS_MAP.put(Float.TYPE, Float.class);
        PRIMITIVE_TO_WRAPPER_CLASS_MAP.put(Double.TYPE, Double.class);
        PRIMITIVE_TO_WRAPPER_CLASS_MAP.put(Void.TYPE, Void.class);
    }

    /**
     * Convert primitive class to it's wrapper class
     * Leaves other classes intact
     */
    public static Class<?> toWrapperClass(Class<?> aClass) {
        Class<?> wrapperClass = PRIMITIVE_TO_WRAPPER_CLASS_MAP.get(aClass);
        return wrapperClass != null ? wrapperClass : aClass;
    }

    public static void fixListViewInDialogBackground(ListView listView) {
        if (Build.VERSION.SDK_INT < 11) {
            listView.setBackgroundColor(listView.getResources().getColor(android.R.color.background_light));
        }
    }

    public static JSONArray toJsonArray(String[] javaArray) {
        return javaArray == null ? null : new JSONArray(Arrays.asList(javaArray));
    }

    public static JSONObject contentValuesToJsonObject(ContentValues contentValues) throws JSONException {
        if (contentValues == null) {
            return null;
        }
        JSONObject jsonObject = new JSONObject();
        for (String key : getKeySet(contentValues)) {
            jsonObject.put(key, contentValues.getAsString(key));
        }
        return jsonObject;
    }

    public static ContentValues jsonObjectToContentValues(JSONObject jsonObject) throws JSONException {
        if (jsonObject == null) {
            return null;
        }
        ContentValues contentValues = new ContentValues();
        @SuppressWarnings("unchecked")
        final Iterator<String> iterator = jsonObject.keys();
        while (iterator.hasNext()) {
            String key = iterator.next();
            contentValues.put(key, jsonObject.getString(key));
        }
        return contentValues;
    }
}