package com.github.michalbednarski.intentslab;

import android.annotation.TargetApi;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.support.v4.util.ArrayMap;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.Toast;

import com.github.michalbednarski.intentslab.bindservice.manager.SystemServiceDescriptor;
import com.github.michalbednarski.intentslab.runas.IRemoteInterface;
import com.github.michalbednarski.intentslab.runas.RunAsInitReceiver;
import com.github.michalbednarski.intentslab.runas.RunAsManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
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

    public static Object getDefaultValueForPrimitveClass(Class<?> aClass) {
        if (aClass == Boolean.TYPE) {
            return false;
        } else if (aClass == Byte.TYPE) {
            return (byte) 0;
        } else if (aClass == Character.TYPE) {
            return 0;
        } else if (aClass == Short.TYPE) {
            return (short) 0;
        } else if (aClass == Integer.TYPE) {
            return 0;
        } else if (aClass == Long.TYPE) {
            return (long) 0;
        } else if (aClass == Float.TYPE) {
            return 0;
        } else if (aClass == Double.TYPE) {
            return 0;
        } else {
            throw new RuntimeException("Not primitive type");
        }
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

    public static Object[] deepCastArray(Object[] array, Class targetType) {
        assert targetType.isArray() && !targetType.getComponentType().isPrimitive();

        if (targetType.isInstance(array) || array == null) {
            return array;
        }

        Class componentType = targetType.getComponentType();
        Class nestedComponentType = componentType.getComponentType();
        Object[] newArray = (Object[]) Array.newInstance(componentType, array.length);
        if (nestedComponentType != null && !nestedComponentType.isPrimitive()) {
            for (int i = 0; i < array.length; i++) {
                newArray[i] = deepCastArray((Object[]) array[i], nestedComponentType);
            }
        } else {
            System.arraycopy(array, 0, newArray, 0, array.length);
        }
        return newArray;
    }

    public static void updateLegacyCheckedIcon(MenuItem menuItem) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            menuItem.setIcon(
                    menuItem.isChecked() ?
                            R.drawable.ic_menu_checked :
                            R.drawable.ic_menu_unchecked);
        }
    }

    public static int[] shrinkIntArray(int[] original, int toCount) {
        assert original.length >= toCount;
        if (original.length == toCount) {
            return original;
        }
        int[] newArray = new int[toCount];
        System.arraycopy(original, 0, newArray, 0, toCount);
        return newArray;
    }

    public static int[] toIntArray(List<Integer> list) {
        int[] array = new int[list.size()];
        int index = 0;
        for (Integer integer : list) {
            array[index++] = integer;
        }
        return array;
    }

    @TargetApi(13) // Function handles all supported api levels
    public static InputStream dumpSystemService(Context context, String serviceName, final String[] arguments) throws Exception {
        // Check if we have permission to invoke dump from our process
        final boolean canDumpLocally =
                context.getPackageManager().checkPermission(android.Manifest.permission.DUMP, context.getPackageName())
                == PackageManager.PERMISSION_GRANTED;

        // On versions without createPipe() just execute dumpsys
        if (android.os.Build.VERSION.SDK_INT < 9) {
            if (!canDumpLocally) {
                throw new Exception("Dumping is not supported on this system version");
            }
            String[] progArray = new String[arguments != null ? 2 + arguments.length : 2];
            progArray[0] = "dumpsys";
            progArray[1] = serviceName;
            if (arguments != null) {
                System.arraycopy(arguments, 0, progArray, 2, arguments.length);
            }
            return Runtime.getRuntime().exec(progArray).getInputStream();
        }

        // Get service
        final Class<?> serviceManager = Class.forName("android.os.ServiceManager");
        final IBinder service = (IBinder) serviceManager.getMethod("getService", String.class).invoke(null, serviceName);

        // Check permissions and get remote interface if needed
        IRemoteInterface remoteInterface = null;
        if (!canDumpLocally) {
            remoteInterface = RunAsManager.getRemoteInterfaceForSystemDebuggingCommands();
            if (remoteInterface == null) {
                throw new SecurityException("Process has no permission to dump services");
            }
        }

        // Create pipe, write(pipe[0]) -> read(pipe[1])
        final ParcelFileDescriptor[] pipe = ParcelFileDescriptor.createPipe();
        final ParcelFileDescriptor readablePipe = pipe[0];
        final ParcelFileDescriptor writablePipe = pipe[1];

        try {
            // Execute dump
            if (canDumpLocally) {
                if (android.os.Build.VERSION.SDK_INT >= 13) {
                    service.dumpAsync(writablePipe.getFileDescriptor(), arguments);
                    writablePipe.close();
                } else {
                    (new Thread() {
                        @Override
                        public void run() {
                            try {
                                service.dump(writablePipe.getFileDescriptor(), arguments);
                                writablePipe.close();
                            } catch (Exception e) {
                                // TODO: can we handle this?
                                e.printStackTrace();
                            }
                        }
                    }).start();
                }
            } else {
                remoteInterface.dumpServiceAsync(service, writablePipe, arguments);
                writablePipe.close();
            }
        // If anything went wrong, close pipe and rethrow
        } catch (Throwable e) {
            readablePipe.close();
            writablePipe.close();
            throwUnchecked(e);
            throw new Error(); // Unreachable
        }

        // Return stream that will ensure closing fd
        return new FileInputStream(readablePipe.getFileDescriptor()) {
            @Override
            public void close() throws IOException {
                super.close();
                readablePipe.close();
            }
        };
    }

    /**
     * Throw exception without declaring throws
     *
     * Use this only for rethrowing Throwable
     * in method declaring "throws Exception"!
     */
    private static void throwUnchecked(Throwable e) {
        // http://stackoverflow.com/a/12423831
        Utils.<RuntimeException>throwUnchecked0(e);
    }

    @SuppressWarnings("unchecked")
    private static <E extends Throwable> void throwUnchecked0(Throwable e) throws E {
        throw (E) e;
    }



    /**
     * Save reference to object in bundle so it can be later retrieved with
     * {@link #getLiveRefFromBundle(Bundle, String)}
     *
     * Note: such reference will become null if this process will be killed
     */
    public static void putLiveRefInBundle(Bundle bundle, String key, Object object) {
        RunAsInitReceiver.putBinderInBundle(bundle, key, new LiveRefInBundle(object));
    }

    public static <T> T getLiveRefFromBundle(Bundle bundle, String key) {
        final Object binder = bundle.get(key);
        if (binder instanceof LiveRefInBundle) {
            return (T) ((LiveRefInBundle) binder).target;
        }
        return null;
    }


    private final static class LiveRefInBundle extends Binder {
        LiveRefInBundle(Object target) {
            this.target = target;
        }

        final Object target;
    }



    // PROTECTED BROADCASTS
    private static ArrayMap<String, Boolean> sProtectedBroadcastsCache = new ArrayMap<String, Boolean>();

    public static boolean isProtectedBroadcast(String action) {
        // Try cache
        final Boolean cachedValue = sProtectedBroadcastsCache.get(action);
        if (cachedValue != null) {
            return cachedValue;
        }

        // Get value
        final boolean isProtectedBroadcast;

        try {
            final IBinder service = SystemServiceDescriptor.getSystemService("package");
            final Object asInterface =
                    Class.forName("android.content.pm.IPackageManager$Stub")
                            .getMethod("asInterface", IBinder.class)
                            .invoke(null, service);
            isProtectedBroadcast = (Boolean) asInterface.getClass()
                    .getMethod("isProtectedBroadcast", String.class)
                    .invoke(asInterface, action);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        // Add to cache
        sProtectedBroadcastsCache.put(action, isProtectedBroadcast);
        return isProtectedBroadcast;
    }
}
